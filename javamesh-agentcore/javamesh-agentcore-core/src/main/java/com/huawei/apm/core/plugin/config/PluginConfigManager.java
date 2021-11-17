/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.plugin.config;

import java.io.File;

import com.huawei.apm.core.agent.interceptor.InterceptorChainManager;
import com.huawei.apm.core.config.ConfigManager;
import com.huawei.apm.core.config.common.BaseConfig;
import com.huawei.apm.core.util.FileUtil;

/**
 * 插件配置管理器，${ConfigManager}统一配置管理器的特化，专门用来加载插件包配置和插件服务包配置
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/11/12
 */
public class PluginConfigManager extends ConfigManager {
    private static final String CONFIG_FILE_NAME = "config.yaml";

    /**
     * 加载插件服务包配置
     *
     * @param configDir   配置文件夹
     * @param classLoader 加载插件服务包的类加载器
     */
    public static void loadServiceConfig(File configDir, ClassLoader classLoader) {
        final String configDirStr = FileUtil.getCanonicalPath(configDir);
        if (configDirStr == null) {
            return;
        }
        final File configFile = new File(configDirStr + File.separatorChar + CONFIG_FILE_NAME);
        if (!configFile.exists() || !configFile.isFile()) {
            return;
        }
        loadConfig(configFile, PluginConfig.class, classLoader,
                new ConfigConsumer() {
                    @Override
                    public void accept(BaseConfig config) {
                        if (config instanceof AliaConfig) {
                            InterceptorChainManager.addAlia((AliaConfig) config);
                        }
                    }
                });
    }
}
