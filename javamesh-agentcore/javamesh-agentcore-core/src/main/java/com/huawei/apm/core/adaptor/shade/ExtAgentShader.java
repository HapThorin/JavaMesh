/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.adaptor.shade;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import com.huawei.apm.core.adaptor.shade.mapping.ShadeMapping;
import com.huawei.apm.core.adaptor.utils.IOUtil;

/**
 * 修正jar包中所有类全限定名的shader
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/18
 */
public class ExtAgentShader {
    /**
     * 修正目录下所有jar包中所有类全限定名
     *
     * @param sourcePath    源目录
     * @param targetPath    输出目标目录
     * @param shadeMappings 修正mapping
     * @param excludes      无需修正的jar包
     */
    public static void shade(String sourcePath, String targetPath, List<ShadeMapping> shadeMappings,
            Set<String> excludes) {
        IOUtil.deleteDirs(new File(targetPath));
        final File sourceDir = new File(sourcePath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }
        final CopyConsumer copyConsumer = CopyConsumer.build(sourcePath, targetPath);
        final ShadeConsumer shadeConsumer = ShadeConsumer.build(sourcePath, targetPath, shadeMappings, copyConsumer);
        if (copyConsumer == null || shadeConsumer == null) {
            return;
        }
        foreachFile(sourceDir, excludes, shadeConsumer, copyConsumer);
    }

    /**
     * 对所有文件进行操作
     *
     * @param file            文件
     * @param excludes        无需修正的jar包
     * @param jarConsumer     对jar包进行操作
     * @param defaultConsumer 对其他文件或无需修正的jar包进行的操作
     */
    private static void foreachFile(File file, Set<String> excludes, FileConsumer jarConsumer,
            FileConsumer defaultConsumer) {
        if (file.isFile()) {
            final String fileName = file.getName();
            if (fileName.endsWith(".jar") && (excludes == null || !excludes.contains(fileName))) {
                jarConsumer.consume(file);
            } else {
                defaultConsumer.consume(file);
            }
        } else {
            final File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    foreachFile(subFile, excludes, jarConsumer, defaultConsumer);
                }
            }
        }
    }

    /**
     * 文件消费者，为兼容1.6
     */
    private interface FileConsumer {
        void consume(File file);
    }

    /**
     * shade操作消费者
     */
    private static class ShadeConsumer implements FileConsumer {
        /**
         * 源路径
         */
        private final String sourcePath;
        /**
         * 目标路径
         */
        private final String targetPath;
        /**
         * 自定义Remapper，用于修正全限定名和路径
         */
        private final ExtAgentRemapper extAgentRemapper;
        /**
         * 默认消费者，修正失败时将执行默认操作
         */
        private final FileConsumer defaultConsumer;

        private ShadeConsumer(String sourcePath, String targetPath, ExtAgentRemapper extAgentRemapper,
                FileConsumer defaultConsumer) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
            this.extAgentRemapper = extAgentRemapper;
            this.defaultConsumer = defaultConsumer;
        }

        @Override
        public void consume(File file) {
            try {
                JarOutputStream outputStream = null;
                try {
                    final File targetFile = new File(file.getCanonicalPath().replace(sourcePath, targetPath));
                    if (!IOUtil.createParentDir(targetFile)) {
                        return;
                    }
                    outputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(targetFile)));
                    shadeJar(file, outputStream);
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            } catch (IOException ignored) {
                defaultConsumer.consume(file);
            }
        }

        /**
         * 修正jar包
         *
         * @param file         jar包
         * @param outputStream 目标输出流
         * @throws IOException 修正jar包失败
         */
        private void shadeJar(File file, JarOutputStream outputStream) throws IOException {
            final JarFile jarFile = new JarFile(file);
            final Set<String> duplicateSet = new HashSet<String>();
            for (Enumeration<JarEntry> enumeration = jarFile.entries(); enumeration.hasMoreElements(); ) {
                JarEntry entry = enumeration.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                InputStream inputStream = null;
                try {
                    inputStream = jarFile.getInputStream(entry);
                    shadeEntry(entry, inputStream, outputStream, duplicateSet);
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            }
        }

        /**
         * 修正jar包中文件
         *
         * @param entry        jar包中文件
         * @param inputStream  该文件的输入流
         * @param outputStream 目标jar包输出流
         * @param duplicateSet 目录去重集
         * @throws IOException 修正文件失败
         */
        private void shadeEntry(JarEntry entry, InputStream inputStream, JarOutputStream outputStream,
                Set<String> duplicateSet) throws IOException {
            final String entryName = entry.getName();
            final long entryTime = entry.getTime();
            String remappedName = extAgentRemapper.map(entryName);
            checkParentDir(remappedName, entryTime, outputStream, duplicateSet);
            if (entryName.endsWith(".class")) {
                remapClass(extAgentRemapper, entryName, entryTime, inputStream, outputStream);
            } else {
                copyEntry(entryName, entryTime, inputStream, outputStream);
            }
        }

        /**
         * 拷贝jar包中普通文件
         *
         * @param entryPath    文件路径
         * @param entryTime    文件创建时间
         * @param inputStream  文件输入流
         * @param outputStream 目标jar包输出流
         * @throws IOException 拷贝文件失败
         */
        private void copyEntry(String entryPath, long entryTime, InputStream inputStream, JarOutputStream outputStream)
                throws IOException {
            final JarEntry newEntry = new JarEntry(entryPath);
            newEntry.setTime(entryTime);
            outputStream.putNextEntry(newEntry);
            final byte[] buffer = new byte[1024 * 16];
            int n;
            while (0 <= (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
        }

        /**
         * 对class文件进行重新修正
         *
         * @param remapper     自定义Remapper，用于修正全限定名和路径
         * @param classPath    class文件路径
         * @param entryTime    文件创建时间
         * @param inputStream  文件的输入流
         * @param outputStream 目标jar包输出流
         * @throws IOException 修正class文件失败
         */
        private void remapClass(ExtAgentRemapper remapper, String classPath, long entryTime, InputStream inputStream,
                JarOutputStream outputStream) throws IOException {
            final ClassReader classReader = new ClassReader(inputStream);
            final ClassWriter classWriter = new ClassWriter(0);
            final String packagePath = classPath.substring(0, classPath.lastIndexOf('/') + 1);
            final ClassVisitor classVisitor = new ClassRemapper(classWriter, remapper) {
                @Override
                public void visitSource(final String source, final String debug) {
                    if (source == null) {
                        super.visitSource(null, debug);
                    } else {
                        final String remappedSource = remapper.map(packagePath + source);
                        super.visitSource(remappedSource.substring(remappedSource.lastIndexOf('/') + 1), debug);
                    }
                }
            };
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
            final byte[] renamedClass = classWriter.toByteArray();
            final String remappedName = remapper.map(classPath.substring(0, classPath.indexOf('.')));
            final JarEntry entry = new JarEntry(remappedName + ".class");
            entry.setTime(entryTime);
            outputStream.putNextEntry(entry);
            outputStream.write(renamedClass);
        }

        /**
         * 验证并创建父目录
         *
         * @param filePath     文件路径
         * @param entryTime    文件创建时间
         * @param outputStream 目标jar包输出流
         * @param duplicateSet 目录去重集
         * @throws IOException 创建目录失败
         */
        private void checkParentDir(String filePath, long entryTime, JarOutputStream outputStream,
                Set<String> duplicateSet) throws IOException {
            final int index = filePath.lastIndexOf('/');
            if (index != -1) {
                final String dir = filePath.substring(0, index);
                if (!duplicateSet.contains(dir)) {
                    mkdirs(dir, entryTime, outputStream, duplicateSet);
                }
            }
        }

        /**
         * 在jar包中创建完整的一个包路径
         *
         * @param packagePath  包路径
         * @param entryTime    创建时间
         * @param outputStream 目标jar包输出流
         * @param duplicateSet 目录去重集
         * @throws IOException 创建目录失败
         */
        private void mkdirs(String packagePath, long entryTime, JarOutputStream outputStream, Set<String> duplicateSet)
                throws IOException {
            if (packagePath.lastIndexOf('/') > 0) {
                final String parent = packagePath.substring(0, packagePath.lastIndexOf('/'));
                if (!duplicateSet.contains(parent)) {
                    mkdirs(parent, entryTime, outputStream, duplicateSet);
                }
            }
            final JarEntry entry = new JarEntry(packagePath + '/');
            entry.setTime(entryTime);
            outputStream.putNextEntry(entry);
            duplicateSet.add(packagePath);
        }

        /**
         * 校验路径并构建ShadeConsumer
         *
         * @param sourcePath      源jar包路径
         * @param targetPath      目标jar包路径
         * @param shadeMappings   修正mapping
         * @param defaultConsumer 默认的文件处理器
         * @return ShadeConsumer对象
         */
        static ShadeConsumer build(String sourcePath, String targetPath, List<ShadeMapping> shadeMappings,
                FileConsumer defaultConsumer) {
            try {
                return new ShadeConsumer(new File(sourcePath).getCanonicalPath(),
                        new File(targetPath).getCanonicalPath(),
                        new ExtAgentRemapper(shadeMappings), defaultConsumer);
            } catch (IOException ignored) {
                return null;
            }
        }
    }

    /**
     * 用于复制文件的处理器
     */
    private static class CopyConsumer implements FileConsumer {
        /**
         * 源文件路径
         */
        private final String sourcePath;
        /**
         * 目标文件路径
         */
        private final String targetPath;

        private CopyConsumer(String sourcePath, String targetPath) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
        }

        @Override
        public void consume(File file) {
            try {
                final File targetFile = new File(file.getCanonicalPath().replace(sourcePath, targetPath));
                if (IOUtil.createParentDir(targetFile)) {
                    IOUtil.copyFile(file, targetFile);
                }
            } catch (IOException ignored) {
            }
        }

        /**
         * 构建CopyConsumer
         *
         * @param sourcePath 源文件路径
         * @param targetPath 目标文件路径
         * @return CopyConsumer对象
         */
        static CopyConsumer build(String sourcePath, String targetPath) {
            try {
                return new CopyConsumer(new File(sourcePath).getCanonicalPath(),
                        new File(targetPath).getCanonicalPath());
            } catch (IOException ignored) {
                return null;
            }
        }
    }
}
