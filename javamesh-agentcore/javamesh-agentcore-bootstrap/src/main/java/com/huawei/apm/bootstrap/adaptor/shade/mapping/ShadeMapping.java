/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.adaptor.shade.mapping;

/**
 * 全限定名修正的mapping
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/18
 */
public class ShadeMapping {
    /**
     * 源全限定名格式
     */
    private String sourcePattern;
    /**
     * 目标全限定名格式
     */
    private String targetPattern;
    /**
     * 源路径格式
     */
    private String sourcePathPattern;
    /**
     * 目标路径格式
     */
    private String targetPathPattern;

    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    public void setTargetPattern(String targetPattern) {
        this.targetPattern = targetPattern;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public String getTargetPattern() {
        return targetPattern;
    }

    public String getSourcePathPattern() {
        if (sourcePathPattern == null) {
            sourcePathPattern = sourcePattern.replace('.', '/');
        }
        return sourcePathPattern;
    }

    public String getTargetPathPattern() {
        if (targetPathPattern == null) {
            targetPathPattern = targetPattern.replace('.', '/');
        }
        return targetPathPattern;
    }

    public String relocateClass(String className) {
        if (className.startsWith(sourcePattern)) {
            return className.replaceFirst(sourcePattern, targetPattern);
        }
        return className;
    }

    public String relocatePath(String classPath) {
        if (classPath.startsWith(getSourcePathPattern())) {
            return classPath.replaceFirst(getSourcePathPattern(), getTargetPathPattern());
        }
        return classPath;
    }
}
