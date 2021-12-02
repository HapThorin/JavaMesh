/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.javamesh.core.common;

import com.huawei.javamesh.core.lubanops.bootstrap.commons.LubanApmConstants;
import com.huawei.javamesh.core.lubanops.bootstrap.config.AgentConfigManager;

/**
 * 通用常量管理类，
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/11/27
 */
public class CommonConstant {
    /**
     * 启动配置文件名
     */
    public static final String BOOTSTRAP_CONFIG_FILE_NAME = LubanApmConstants.CONFIG_FILENAME;

    /**
     * 核心配置文件名
     */
    public static final String CORE_CONFIG_FILE_NAME = "config.properties";

    /**
     * 插件设定文件名
     */
    public static final String PLUGIN_SETTING_FILE_NAME = "plugins.yaml";

    /**
     * logback配置文件名
     */
    public static final String LOG_SETTING_FILE_NAME = "logback.xml";

    /**
     * 核心包版本键
     */
    public static final String CORE_VERSION_KEY = "Java-mesh-Version";

    /**
     * 启动配置中应用名称的键
     */
    public static final String APP_NAME_KEY = LubanApmConstants.APP_NAME_COMMONS;

    /**
     * 启动配置中实例名称的键
     */
    public static final String INSTANCE_NAME_KEY = LubanApmConstants.INSTANCE_NAME_COMMONS;

    /**
     * 启动配置中应用类型的键
     */
    public static final String APP_TYPE_KEY = LubanApmConstants.APP_TYPE_COMMON;

    /**
     * 启动配置中的env
     */
    public static final String ENV_KEY = LubanApmConstants.ENV_COMMONS;

    /**
     * 启动配置中的envTag
     */
    public static final String ENV_TAG_KEY = LubanApmConstants.ENV_TAG_COMMONS;

    /**
     * 启动配置中的business
     */
    public static final String BIZ_PATH_KEY = LubanApmConstants.BIZ_PATH_COMMONS;

    /**
     * 启动配置中的subBusiness
     */
    public static final String SUB_BUSINESS_KEY = LubanApmConstants.SUB_BUSINESS_COMMONS;

    /**
     * 启动配置中的envSecret
     */
    public static final String ENV_SECRET_KEY = LubanApmConstants.ENV_SECRET_COMMONS;

    /**
     * 启动配置中的access.key
     */
    public static final String MASTER_ACCESS_KEY = AgentConfigManager.MASTER_ACCESS_KEY;

    /**
     * 启动配置中的secret.key
     */
    public static final String MASTER_SECRET_KEY = AgentConfigManager.MASTER_SECRET_KEY;

    /**
     * 启动配置中的master.address
     */
    public static final String MASTER_ADDRESS_KEY = AgentConfigManager.MASTER_ADDRESS;

    /**
     * 启动配置中的agentPath
     */
    public static final String AGENT_ROOT_DIR_KEY = LubanApmConstants.AGENT_PATH_COMMONS;

    /**
     * 启动配置中的bootPath
     */
    @Deprecated
    public static final String LUBAN_BOOT_PATH_KEY = LubanApmConstants.BOOT_PATH_COMMONS;

    /**
     * 启动配置中的pluginsPath
     */
    @Deprecated
    public static final String LUBAN_PLUGINS_PATH_KEY = LubanApmConstants.PLUGINS_PATH_COMMONS;

    /**
     * javamesh的配置文件名的键
     */
    public static final String CORE_CONFIG_FILE_KEY = "core.config.file";

    /**
     * 插件的设置配置名的键
     */
    public static final String PLUGIN_SETTING_FILE_KEY = "plugin.setting.file";

    /**
     * pluginPackage插件包的键
     */
    public static final String PLUGIN_PACKAGE_DIR_KEY = "plugin.package.dir";

    /**
     * logback配置的键
     */
    public static final String LOG_SETTING_FILE_KEY = "log.setting.file";
}
