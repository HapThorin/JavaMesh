/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.extagent.entity;

import java.io.File;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;

import com.huawei.apm.bootstrap.config.BaseConfig;
import com.huawei.apm.bootstrap.config.ConfigTypeKey;

/**
 * 额外agent的配置，配置前缀为{@code ext.agent}
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
@ConfigTypeKey("ext.agent")
public class ExtAgentConfig implements BaseConfig {
    /**
     * 是否加载额外的agent
     */
    private boolean loadExtAgent = false;
    private String mappingName = "mapping.yaml";
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
