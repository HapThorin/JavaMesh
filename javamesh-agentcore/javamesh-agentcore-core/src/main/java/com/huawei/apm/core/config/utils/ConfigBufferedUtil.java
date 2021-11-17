/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.config.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import com.huawei.apm.core.config.common.BaseConfig;
import com.huawei.apm.core.config.common.ConfigFieldKey;
import com.huawei.apm.core.config.common.ConfigTypeKey;
import com.huawei.apm.core.lubanops.bootstrap.log.LogFactory;
import com.huawei.apm.core.serialize.SerializerHolder;

/**
 * 统一配置部分的包装工具，不对外使用
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/11/12
 */
public class ConfigBufferedUtil {
    /**
     * 日志
     */
    private static final Logger LOGGER = LogFactory.getLogger();

    /**
     * 序列化转化，修正ClassLoader不同导致的赋值失败
     *
     * @param config 配置对象
     * @param cls    配置类型
     * @return 修正后的配置对象
     */
    public static BaseConfig getSerialize(BaseConfig config, Class<? extends BaseConfig> cls) {
        return SerializerHolder.deserialize(SerializerHolder.serialize(config), cls);
    }

    /**
     * 获取配置对象的byte-buddy代理，禁用set方法
     *
     * @param config      源配置对象
     * @param classLoader 用于加载代理类的classloader
     * @return 配置对象的byte-buddy代理
     */
    public static BaseConfig getProxy(final BaseConfig config, ClassLoader classLoader) {
        if (config == null) {
            return null;
        }
        final Class<? extends BaseConfig> configClass = config.getClass();
        final InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                final String methodName = method.getName();
                if (methodName.startsWith("set")) {
                    LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                            "Calling method [%s#%s] is not supported.", configClass, methodName));
                    return null;
                }
                return method.invoke(config, args);
            }
        };
        try {
            return new ByteBuddy().subclass(configClass)
                    .implement(BaseConfig.class)
                    .method(ElementMatchers.<MethodDescription>any())
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make()
                    .load(classLoader)
                    .getLoaded()
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception ignored) {
            LOGGER.log(Level.WARNING, String.format(Locale.ROOT, "Create proxy of [%s] failed.", configClass));
            return config;
        }
    }

}
