package com.huawei.apm.bootstrap.extagent;

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

import com.huawei.apm.bootstrap.extagent.entity.ShadeMapping;

public class ExtAgentShader {
    public static void shade(String sourcePath, String targetPath, List<ShadeMapping> shadeMappings,
            Set<String> excludes) {
        ExtAgentUtils.deleteDirs(new File(targetPath));
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

    private interface FileConsumer {
        void consume(File file);
    }

    private static class ShadeConsumer implements FileConsumer {
        private final String sourcePath;
        private final String targetPath;
        private final ExtAgentRemapper extAgentRemapper;
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
                    if (!ExtAgentUtils.createParentDir(targetFile)) {
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

        private void shadeEntry(JarEntry entry, InputStream inputStream, JarOutputStream outputStream,
                Set<String> duplicateSet) throws IOException {
            final String entryName = entry.getName();
            final long entryTime = entry.getTime();
            String remappedName = extAgentRemapper.map(entryName);
            checkParentDir(remappedName, entryTime, outputStream, duplicateSet);
            if (entryName.endsWith(".class")) {
                renameClass(extAgentRemapper, entryName, entryTime, inputStream, outputStream);
            } else {
                copyEntry(entryName, entryTime, inputStream, outputStream);
            }
        }

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

        private void renameClass(ExtAgentRemapper remapper, String classPath, long entryTime, InputStream inputStream,
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

        private void checkParentDir(String classPath, long entryTime, JarOutputStream outputStream,
                Set<String> duplicateSet) throws IOException {
            final int index = classPath.lastIndexOf('/');
            if (index != -1) {
                final String dir = classPath.substring(0, index);
                if (!duplicateSet.contains(dir)) {
                    mkdirs(dir, entryTime, outputStream, duplicateSet);
                }
            }
        }

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

    private static class CopyConsumer implements FileConsumer {
        private final String sourcePath;
        private final String targetPath;

        private CopyConsumer(String sourcePath, String targetPath) {
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
        }

        @Override
        public void consume(File file) {
            try {
                final File targetFile = new File(file.getCanonicalPath().replace(sourcePath, targetPath));
                if (ExtAgentUtils.createParentDir(targetFile)) {
                    ExtAgentUtils.copyFile(file, targetFile);
                }
            } catch (IOException ignored) {
            }
        }

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
