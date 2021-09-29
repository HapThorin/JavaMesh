package com.huawei.apm.bootstrap.agent;

import java.util.Map;

import com.huawei.apm.bootstrap.config.BaseConfig;
import com.huawei.apm.bootstrap.config.ConfigTypeKey;

@ConfigTypeKey("ext.agent")
public class ExtAgentConfig implements BaseConfig {
    private boolean loadExtAgent;
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
