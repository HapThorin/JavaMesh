/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2022. All rights reserved.
 */

package com.huawei.flowcontrol.core.datasource;

import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.huawei.javamesh.core.plugin.config.PluginConfigManager;
import com.huawei.javamesh.core.service.ServiceManager;
import com.huawei.javamesh.core.service.dynamicconfig.utils.LabelGroupUtils;
import com.huawei.javamesh.core.service.dynamicconfig.service.ConfigChangedEvent;
import com.huawei.javamesh.core.service.dynamicconfig.service.ConfigurationListener;
import com.huawei.javamesh.core.service.dynamicconfig.service.DynamicConfigurationFactoryService;
import com.huawei.flowcontrol.core.config.FlowControlConfig;
import com.huawei.flowcontrol.core.datasource.kie.rule.RuleCenter;
import com.huawei.flowcontrol.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流控规则初始化
 * <h3>同时兼容zk配置中心</h3>
 *
 * @author hanpeng
 * @since 2020-11-12
 */
public class DefaultDataSourceManager implements DataSourceManager {
    /**
     * 数据源map
     */
    private final Map<String, DefaultDataSource<?>> sourceMap = new ConcurrentHashMap<String, DefaultDataSource<?>>();

    /**
     * 规则中心
     */
    private final RuleCenter ruleCenter = new RuleCenter();

    public DefaultDataSourceManager() {

    }

    @Override
    public void start() {
        // 初始化数据源
        initDataSources();

        // 注册规则管理器
        registerRuleManager();

        // 初始化监听
        initConfigListener();
    }

    @Override
    public void stop() {

    }

    private void initDataSources() {
        for (String ruleType : ruleCenter.getRuleTypes()) {
            Class<?> ruleClass = ruleCenter.getRuleClass(ruleType);
            DefaultDataSource<?> defaultDataSource = getDataSource(ruleType, ruleClass);
            sourceMap.put(ruleType, defaultDataSource);
        }
    }

    private <T> DefaultDataSource<T> getDataSource(String ruleKey, Class<T> ruleClass) {
        return new DefaultDataSource<T>(ruleClass, ruleKey);
    }

    private void registerRuleManager() {
        for (Map.Entry<String, DefaultDataSource<?>> entry : sourceMap.entrySet()) {
            ruleCenter.registerRuleManager(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 初始化配置监听
     */
    private void initConfigListener() {
        final FlowControlConfig pluginConfig = PluginConfigManager.getPluginConfig(FlowControlConfig.class);
        String serviceName = AppNameUtil.getAppName();
        if (!StringUtils.isEmpty(pluginConfig.getConfigServiceName())) {
            serviceName = pluginConfig.getConfigServiceName();
        }
        final String groupLabel = LabelGroupUtils.createLabelGroup(Collections.singletonMap("service", serviceName));
        final DynamicConfigurationFactoryService service = ServiceManager.getService(DynamicConfigurationFactoryService.class);
        service.getDynamicConfigurationService().addGroupListener(groupLabel, new ConfigurationListener() {
            @Override
            public void process(ConfigChangedEvent event) {
                for (DefaultDataSource<?> defaultDataSource : sourceMap.values()) {
                    defaultDataSource.update(event);
                }
            }
        });
    }
}
