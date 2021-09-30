package com.huawei.apm.bootstrap.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.huawei.apm.bootstrap.config.ConfigLoader;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

public abstract class ExtAgentManager {
    private static volatile boolean isInit = false;

    private static final List<ExtAgentLoader> extAgentLoaders = new ArrayList<ExtAgentLoader>();

    public static void init(ClassLoader spiLoader, String agentArgs, Instrumentation instrumentation) {
        final ExtAgentConfig config = ConfigLoader.getConfig(ExtAgentConfig.class);
        if (!config.isLoadExtAgent()) {
            return;
        }
        if (!isInit) {
            synchronized (extAgentLoaders) {
                if (!isInit) {
                    doInit(config, spiLoader, agentArgs, instrumentation);
                    isInit = true;
                }
            }
        }
    }

    private static void doInit(ExtAgentConfig config, ClassLoader spiLoader, String agentArgs,
            Instrumentation instrumentation) {
        final ClassLoader agentClassLoader = Thread.currentThread().getContextClassLoader();
        final Method addURL;
        try {
            addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException e) {
            // todo
            return;
        }
        addURL.setAccessible(true);
        final Map<ExtAgentType, String> extAgentJarPaths = config.getExtAgentJarPaths();
        for (ExtAgentLoader loader : ServiceLoader.load(ExtAgentLoader.class, spiLoader)) {
            final String jarPath = extAgentJarPaths.get(loader.getType());
            if (jarPath == null) {
                // todo
                continue;
            }
            final File jarFile = new File(jarPath);
            if (!jarFile.exists() || !jarFile.isFile()) {
                // todo
                continue;
            }
            try {
                addURL.invoke(agentClassLoader, jarFile.toURI().toURL());
                loader.doPremain(agentArgs, instrumentation);
                extAgentLoaders.add(loader);
            } catch (Exception e) {
                // todo
                e.printStackTrace();
            }
        }
    }

    public static void uniteNamedEnhancers(Map<String, List<EnhanceDefinition>> nameDefinitions) {
        if (!isInit) {
            // todo
            return;
        }
        for (ExtAgentLoader loader : extAgentLoaders) {
            loader.uniteNamedEnhancers(nameDefinitions);
        }
    }

    public static void uniteNonNamedEnhancers(List<EnhanceDefinition> nonNameDefinitions) {
        if (!isInit) {
            // todo
            return;
        }
        for (ExtAgentLoader loader : extAgentLoaders) {
            loader.getNonNamedEnhancers(nonNameDefinitions);
        }
    }

    public static Interceptor createInterceptor(Class<?> cls) {
        if (!isInit) {
            // todo
            return null;
        }
        for (ExtAgentLoader extAgentLoader : extAgentLoaders) {
            final Interceptor interceptor = extAgentLoader.createInterceptor(cls);
            if (interceptor != null) {
                return interceptor;
            }
        }
        // todo
        return null;
    }
}
