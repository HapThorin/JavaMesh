/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.adaptor.shade.mapping;

/**
 * lib目录的mapping
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/18
 */
public class LibJarMapping {
    /**
     * 源路径
     */
    private String sourcePath;
    /**
     * 目标文件夹
     */
    private String targetDir;

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }
}
