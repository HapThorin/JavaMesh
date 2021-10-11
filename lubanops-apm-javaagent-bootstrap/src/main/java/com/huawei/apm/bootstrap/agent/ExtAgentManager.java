/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lubanops.apm.bootstrap.log.LogFactory;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import com.huawei.apm.bootstrap.config.ConfigLoader;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

/**
 * 额外agent的管理器
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
public abstract class ExtAgentManager {
    /**
     * 日志
     */
    private static final Logger LOGGER = LogFactory.getLogger();

    /**
     * 额外的agent加载策略，只有addUrl成功的才会添加
     */
    private static final List<ExtAgentLoader> EXT_AGENT_LOADERS = new ArrayList<ExtAgentLoader>();

    /**
     * 初始化标记
     */
    private static volatile boolean isInit = false;

    public static boolean isInit() {
        return isInit;
    }

    /**
     * 初始化，判断是否需要进行初始化操作，如果需要，则执行{@link #doInit}
     *
     * @param spiLoader       用作spi的classLoader
     * @param agentArgs       agent启动参数
     * @param instrumentation Instrumentation
     * @param extAgentDir     额外agent的存放目录
     */
    public static void init(ClassLoader spiLoader, String agentArgs, Instrumentation instrumentation,
            String extAgentDir) {
        final ExtAgentConfig config = ConfigLoader.getConfig(ExtAgentConfig.class);
        if (!config.isLoadExtAgent()) {
            return;
        }
        if (!isInit) {
            synchronized (EXT_AGENT_LOADERS) {
                if (!isInit) {
                    isInit = doInit(config, spiLoader, agentArgs, instrumentation, extAgentDir);
                }
            }
        }
    }

    /**
     * 初始化操作
     * <p>尝试拿到配置的agent路径和spi设置的额外agent策略的交集
     * <p>调用addUrl方法添加额外的agent包
     * <p>再调用额外agent策略的{@link ExtAgentLoader#init}方法进行初始化
     *
     * @param config          额外agent配置
     * @param spiLoader       用作spi的classLoader
     * @param agentArgs       agent启动参数
     * @param instrumentation Instrumentation
     * @param extAgentDir     额外agent的存放目录
     * @return 是否初始化成功
     */
    private static boolean doInit(ExtAgentConfig config, ClassLoader spiLoader, String agentArgs,
            Instrumentation instrumentation, String extAgentDir) {
        final ClassLoader agentClassLoader = Thread.currentThread().getContextClassLoader();
        final Method addURL;
        try {
            addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException ignored) {
            LOGGER.log(Level.SEVERE, String.format(Locale.ROOT,
                    "Cannot find 'addURL' method, [%s] initialize failed.", ExtAgentManager.class.getSimpleName()));
            return false;
        }
        addURL.setAccessible(true);
        final Map<ExtAgentType, String> extAgentJarPaths = config.getExtAgentJarPaths();
        for (ExtAgentLoader loader : ServiceLoader.load(ExtAgentLoader.class, spiLoader)) {
            final ExtAgentType extAgentType = loader.getType();
            final String jarPath = extAgentJarPaths.get(extAgentType);
            if (jarPath == null) {
                LOGGER.log(Level.WARNING, String.format(Locale.ROOT, "Missing agent jar path of [%s].", extAgentType));
                continue;
            }
            final File jarFile = new File(extAgentDir + jarPath);
            if (!jarFile.exists() || !jarFile.isFile()) {
                LOGGER.log(Level.WARNING, String.format(Locale.ROOT, "Cannot find agent jar of [%s].", extAgentType));
                continue;
            }
            try {
                addURL.invoke(agentClassLoader, jarFile.toURI().toURL());
            } catch (Exception ignored) {
                LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                        "Add agent jar of [%s] to class loader failed.", extAgentType));
                continue;
            }
            if (loader.init(agentArgs, instrumentation)) {
                LOGGER.log(Level.INFO, String.format(Locale.ROOT,
                        "Add agent jar of [%s] to class loader.", extAgentType));
                EXT_AGENT_LOADERS.add(loader);
            }
        }
        return true;
    }

    /**
     * 构架匹配器
     * <p>对所有初始化成功的额外agent策略，调用{@link ExtAgentLoader#buildMatch}方法
     * <p>再将他们统合为一个{@link ElementMatcher}返回
     *
     * @return 所有额外agent统合后的ElementMatcher
     */
    public static ElementMatcher<TypeDescription> buildMatch() {
        if (!isInit) {
            LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                    "[%s] hasn't been initialized yet, or initializes failed.",
                    ExtAgentManager.class.getSimpleName()));
            return null;
        }
        ElementMatcher.Junction<TypeDescription> junction = ElementMatchers.none();
        for (ExtAgentLoader loader : EXT_AGENT_LOADERS) {
            final ElementMatcher<TypeDescription> matcher = loader.buildMatch();
            if (matcher != null) {
                junction = junction.or(matcher);
            }
        }
        return junction;
    }

    /**
     * 转换方法
     * <p>对所有初始化成功的额外agent策略，调用{@link ExtAgentLoader#transform}方法
     * <p>再将他们统合为一个{@link ExtAgentTransResp}返回
     *
     * @param builder         构建器
     * @param typeDescription 类型描述
     * @param classLoader     类加载器
     * @return 所有额外agent统合后的ExtAgentTransResp
     */
    public static ExtAgentTransResp transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
            ClassLoader classLoader) {
        if (!isInit) {
            LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                    "[%s] hasn't been initialized yet, or initializes failed.",
                    ExtAgentManager.class.getSimpleName()));
            return ExtAgentTransResp.empty(builder);
        }
        final List<EnhanceDefinition> result = new ArrayList<EnhanceDefinition>();
        DynamicType.Builder<?> newBuilder = builder;
        for (ExtAgentLoader loader : EXT_AGENT_LOADERS) {
            final ExtAgentTransResp resp = loader.transform(newBuilder, typeDescription, classLoader);
            if (!resp.isEmpty()) {
                result.addAll(resp.getDefinitions());
                newBuilder = resp.getBuilder();
            }
        }
        return new ExtAgentTransResp(result, newBuilder);
    }

    /**
     * 创建额外agent相关的拦截器
     * <p>对所有初始化成功的额外agent策略，调用{@link ExtAgentLoader#newInterceptor}方法，一旦成功则返回
     *
     * @param className 拦截器名称
     * @return 拦截器对象
     */
    public static Interceptor newInterceptor(String className) {
        if (!isInit) {
            LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                    "[%s] hasn't been initialized yet, or initializes failed.",
                    ExtAgentManager.class.getSimpleName()));
            return null;
        }
        for (ExtAgentLoader extAgentLoader : EXT_AGENT_LOADERS) {
            final Interceptor interceptor = extAgentLoader.newInterceptor(className);
            if (interceptor != null) {
                return interceptor;
            }
        }
        LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                "There is no extra agent can create interceptor of [%s].", className));
        return null;
    }
}
