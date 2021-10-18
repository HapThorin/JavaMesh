package com.huawei.apm.bootstrap.extagent.entity;

import java.util.List;
import java.util.Set;

public class ExtAgentMapping {
    private String adaptorJar;
    private String agentJar;
    private String pluginsDir;
    private String configsDir;
    private List<LibJarMapping> libJarMappings;
    private List<ShadeMapping> shadeMappings;
    private Set<String> shadeExcludes;

    public String getAdaptorJar() {
        return adaptorJar;
    }

    public void setAdaptorJar(String adaptorJar) {
        this.adaptorJar = adaptorJar;
    }

    public String getAgentJar() {
        return agentJar;
    }

    public void setAgentJar(String agentJar) {
        this.agentJar = agentJar;
    }

    public String getPluginsDir() {
        return pluginsDir;
    }

    public void setPluginsDir(String pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    public String getConfigsDir() {
        return configsDir;
    }

    public void setConfigsDir(String configsDir) {
        this.configsDir = configsDir;
    }

    public List<LibJarMapping> getLibJarMappings() {
        return libJarMappings;
    }

    public void setLibJarMappings(List<LibJarMapping> libJarMapping) {
        this.libJarMappings = libJarMapping;
    }

    public List<ShadeMapping> getShadeMappings() {
        return shadeMappings;
    }

    public void setShadeMappings(List<ShadeMapping> shadeMappings) {
        this.shadeMappings = shadeMappings;
    }

    public Set<String> getShadeExcludes() {
        return shadeExcludes;
    }

    public void setShadeExcludes(Set<String> shadeExcludes) {
        this.shadeExcludes = shadeExcludes;
    }

    public boolean needShade() {
        return shadeMappings != null && !shadeMappings.isEmpty();
    }
}
