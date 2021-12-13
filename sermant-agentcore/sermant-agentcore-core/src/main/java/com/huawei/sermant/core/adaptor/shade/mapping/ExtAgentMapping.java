/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.sermant.core.adaptor.shade.mapping;

import java.util.List;
import java.util.Set;

/**
 * 外部agent的配置mapping
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/18
 */
public class ExtAgentMapping {
    /**
     * 兼容包
     */
    private String adaptorJar;
    /**
     * agent启动包
     */
    private String agentJar;
    /**
     * 插件目录名
     */
    private String pluginsDir;
    /**
     * 配置目录名
     */
    private String configsDir;
    /**
     * lib目录的mapping
     */
    private List<LibJarMapping> libJarMappings;
    /**
     * 全限定名修正的mapping
     */
    private List<ShadeMapping> shadeMappings;
    /**
     * 排除不进行全限定名修正的jar包
     */
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
