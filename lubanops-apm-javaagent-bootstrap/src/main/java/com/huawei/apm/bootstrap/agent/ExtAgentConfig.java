/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.agent;

import java.util.Map;

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
    /**
     * 额外agent的jar包路径
     * <p>键为agent类型，通过该类型绑定加载策略
     * <p>值为额外agent存放目录下的相对目录，默认在apm agent目录下的agent目录中存放额外的agent
     */
    private Map<ExtAgentType, String> extAgentJarPaths;

    public boolean isLoadExtAgent() {
        return loadExtAgent;
    }

    public void setLoadExtAgent(boolean loadExtAgent) {
        this.loadExtAgent = loadExtAgent;
    }

    public Map<ExtAgentType, String> getExtAgentJarPaths() {
        return extAgentJarPaths;
    }

    public void setExtAgentJarPaths(Map<ExtAgentType, String> extAgentJarPaths) {
        this.extAgentJarPaths = extAgentJarPaths;
    }
}
