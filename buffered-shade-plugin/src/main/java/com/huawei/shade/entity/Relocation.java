/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.shade.entity;

import java.util.List;
import java.util.Set;

/**
 * Relocation对象
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
public class Relocation {
    private String sourceDirectory;
    private String targetDirectory;
    private List<RelocatePattern> relocatePatterns;
    private Set<String> excludes;

    public String getSourceDirectory() {
        return sourceDirectory;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public List<RelocatePattern> getRelocatePatterns() {
        return relocatePatterns;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

}
