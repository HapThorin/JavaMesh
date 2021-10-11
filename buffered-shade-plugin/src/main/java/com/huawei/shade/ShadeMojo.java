/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.shade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.shade.ShadeRequest;
import org.apache.maven.plugins.shade.Shader;
import org.apache.maven.plugins.shade.filter.Filter;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.relocation.SimpleRelocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.codehaus.plexus.util.IOUtil;

import com.huawei.shade.entity.RelocatePattern;
import com.huawei.shade.entity.Relocation;

/**
 * ShadeMojo，主要逻辑对象
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
@Mojo(name = "shade", defaultPhase = LifecyclePhase.PACKAGE)
public class ShadeMojo extends AbstractMojo {
    /**
     * shade插件的轮子{@link Shader}
     */
    @Component(hint = "default", role = org.apache.maven.plugins.shade.Shader.class)
    private Shader shader;

    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "relocations")
    private Relocation[] relocations;

    /**
     * 文件消费者，jdk1.6写法
     */
    private interface FileConsumer {
        void consume(File file);
    }

    /**
     * 遍历所有文件
     *
     * @param file            文件或目录
     * @param excludes        排除的文件
     * @param jarConsumer     jar包消费者
     * @param defaultConsumer 普通文件消费者
     */
    private void doForeachFile(File file, Set<String> excludes, FileConsumer jarConsumer,
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
                    doForeachFile(subFile, excludes, jarConsumer, defaultConsumer);
                }
            }
        }
    }

    /**
     * 判断源目录是否存在，并执行{@link ShadeMojo#doForeachFile}
     *
     * @param relocation Relocation参数
     */
    private void foreachFile(final Relocation relocation) {
        final File sourceDir = new File(relocation.getSourceDirectory());
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            return;
        }
        doForeachFile(sourceDir, relocation.getExcludes(),
                new FileConsumer() {
                    @Override
                    public void consume(File file) {
                        try {
                            shader.shade(buildShadeRequest(file, relocation));
                        } catch (IOException e) {
                            getLog().warn(e);
                        } catch (MojoExecutionException e) {
                            getLog().warn(e);
                        }
                    }
                }, new FileConsumer() {
                    @Override
                    public void consume(File file) {
                        final String sourcePath = file.getPath();
                        final File targetFile = new File(sourcePath.replace(
                                relocation.getSourceDirectory(), relocation.getTargetDirectory()));
                        if (createParentDir(targetFile)) {
                            copyFile(file, targetFile);
                        }
                    }
                });
    }

    /**
     * 创建父目录
     *
     * @param file 文件
     * @return 是否创建成功
     */
    private boolean createParentDir(File file) {
        final File parentDir = file.getParentFile();
        return parentDir.exists() || parentDir.mkdirs();
    }

    /**
     * 复制文件
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     */
    private void copyFile(File sourceFile, File targetFile) {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            IOUtil.copy(inputStream, outputStream);
        } catch (IOException ignored) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 构建ShadeRequest
     *
     * @param jar        jar包
     * @param relocation Relocation参数
     * @return ShadeRequest
     */
    private ShadeRequest buildShadeRequest(File jar, Relocation relocation) {
        final ShadeRequest shadeRequest = new ShadeRequest();
        shadeRequest.setJars(Collections.singleton(jar));
        shadeRequest.setUberJar(new File(jar.getPath().replace(
                relocation.getSourceDirectory(), relocation.getTargetDirectory())));
        shadeRequest.setFilters(Collections.<Filter>emptyList());
        final ArrayList<Relocator> relocators = new ArrayList<Relocator>();
        for (RelocatePattern relocatePattern : relocation.getRelocatePatterns()) {
            relocators.add(new SimpleRelocator(
                    relocatePattern.getSourcePattern(),
                    relocatePattern.getTargetPattern(),
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList()));
        }
        shadeRequest.setRelocators(relocators);
        shadeRequest.setResourceTransformers(Collections.<ResourceTransformer>emptyList());
        return shadeRequest;
    }

    /**
     * 遍历Relocation，并执行{@link ShadeMojo#foreachFile}
     */
    private void foreachRelocation() {
        for (Relocation relocation : relocations) {
            foreachFile(relocation);
        }
    }

    /**
     * 主要逻辑，判断是否执行业务
     */
    @Override
    public void execute() {
        if (skip || relocations == null) {
            return;
        }
        foreachRelocation();
    }
}
