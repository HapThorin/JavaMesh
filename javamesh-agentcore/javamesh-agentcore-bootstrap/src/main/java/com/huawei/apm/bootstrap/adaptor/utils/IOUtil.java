/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.adaptor.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * io工具类
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/18
 */
public class IOUtil {
    /**
     * 缓冲区大小
     */
    private static final int bufferSize = 1024 * 16;

    /**
     * 确保一个文件或文件夹的父目录存在
     *
     * @param file 文件或文件夹
     * @return 是否存在或创建成功
     */
    public static boolean createParentDir(File file) {
        final File parentDir = file.getParentFile();
        return parentDir.exists() || parentDir.mkdirs();
    }

    /**
     * 将源文件拷贝到目标路径
     *
     * @param sourceFile 源文件
     * @param targetFile 目标文件
     * @throws IOException 拷贝失败
     */
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            final byte[] buffer = new byte[bufferSize];
            int n;
            while (0 <= (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
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
     * 拷贝文件夹下所有文件
     *
     * @param sourceFile 源文件夹
     * @param targetPath 目标文件夹
     * @throws IOException 拷贝失败
     */
    public static void copyAllFiles(File sourceFile, String targetPath) throws IOException {
        if (sourceFile.isFile()) {
            final File targetFile = new File(targetPath);
            if (createParentDir(targetFile)) {
                copyFile(sourceFile, targetFile);
            }
        } else {
            final File[] subFiles = sourceFile.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    copyAllFiles(subFile, targetPath + File.separatorChar + subFile.getName());
                }
            }
        }
    }

    /**
     * 删除文件夹及其内部所有文件
     *
     * @param file 文件或文件夹
     * @return 是否全部删除成功
     */
    public static boolean deleteDirs(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            if (subFiles != null) {
                boolean result = true;
                for (File subFile : subFiles) {
                    result &= deleteDirs(subFile);
                }
                return result && file.delete();
            }
        }
        return file.delete();
    }
}
