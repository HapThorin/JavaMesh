/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;

import com.huawei.apm.core.lubanops.bootstrap.log.LogFactory;
import com.huawei.apm.core.util.SpiLoadUtil;

/**
 * <Code>CoreService<Code/>管理器，加载、启动所有定义的服务实例。
 *
 * <p><Code>CoreService<Code/>定义在SPI声明文件当中。</p>
 */
public class ServiceManager {
    private static final Map<String, BaseService> services = new HashMap<String, BaseService>();

    public static void initServices() {
        for (final BaseService service : ServiceLoader.load(BaseService.class)) {
            loadService(service, service.getClass(), BaseService.class);
            service.start();
        }
        // 加载完所有服务再启动服务
        addStopHook();
    }

    public static <T> T getService(Class<T> serviceClass) {
        final BaseService baseService = services.get(serviceClass.getName());
        if (baseService != null && serviceClass.isAssignableFrom(baseService.getClass())) {
            return (T) baseService;
        }
        throw new IllegalArgumentException("Service instance of [" +serviceClass+ "] is not found. ");
    }

    protected static void loadService(BaseService service, Class<?> serviceCls,
            Class<? extends BaseService> baseCls) {
        if (serviceCls == null || serviceCls == baseCls || !baseCls.isAssignableFrom(serviceCls)) {
            return;
        }
        loadService(service, serviceCls.getSuperclass(), baseCls);
        for (Class<?> interfaceCls : serviceCls.getInterfaces()) {
            loadService(service, interfaceCls, baseCls);
        }
        String serviceName = serviceCls.getName();
        if (SpiLoadUtil.compare(services.get(serviceName), service)) {
            services.put(serviceName, service);
        }
    }

    private static void addStopHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                for (BaseService baseService : new HashSet<>(services.values())) {
                    baseService.stop();
                }
            }
        }));
    }
}
