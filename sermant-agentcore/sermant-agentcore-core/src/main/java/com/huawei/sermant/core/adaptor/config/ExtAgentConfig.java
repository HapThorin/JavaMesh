/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.sermant.core.adaptor.config;

import java.io.File;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;

import com.huawei.sermant.core.config.common.BaseConfig;
import com.huawei.sermant.core.config.common.ConfigTypeKey;

/**
 * 额外agent的配置，配置前缀为{@code ext.agent}
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/11
 */
@ConfigTypeKey("ext.agent")
public class ExtAgentConfig implements BaseConfig {
    /**
     * 是否加载额外的agent
     */
    private boolean loadExtAgent = false;
    /**
     * 外部agent加载相关配置的mapping
     */
    private String mappingName = "mapping.yaml";
    /**
     * 临时缓存目录，外部agent将在这里修正目录结构和全限定名，默认为系统的临时文件夹
     */
    private String tempCacheDir;

    public boolean isLoadExtAgent() {
        return loadExtAgent;
    }

    public void setLoadExtAgent(boolean loadExtAgent) {
        this.loadExtAgent = loadExtAgent;
    }

    public String getMappingName() {
        return mappingName;
    }

    public void setMappingName(String mappingName) {
        this.mappingName = mappingName;
    }

    public String getTempCacheDir(String baseDir) {
        if (tempCacheDir == null || tempCacheDir.isEmpty()) {
            return AccessController.doPrivileged(new GetPropertyAction("java.io.tmpdir")) +
                    File.separatorChar + "javamesh";
        }
        return baseDir + File.separatorChar + tempCacheDir;
    }

    public void setTempCacheDir(String tempCacheDir) {
        this.tempCacheDir = tempCacheDir;
    }
}
