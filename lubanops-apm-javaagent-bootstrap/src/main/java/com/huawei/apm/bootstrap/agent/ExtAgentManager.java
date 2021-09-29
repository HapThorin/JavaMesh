package com.huawei.apm.bootstrap.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import com.huawei.apm.bootstrap.config.ConfigLoader;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

public abstract class ExtAgentManager {
    private static volatile boolean isInit = false;

    private static final List<ExtAgentLoader> extAgentLoaders = new ArrayList<ExtAgentLoader>();

    public static void init(ClassLoader contextClassLoader, ClassLoader spiLoader, String agentArgs,
            Instrumentation instrumentation) {
        if (!(contextClassLoader instanceof URLClassLoader)) {
            // todo
            return;
        }
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

    public static Map<String, List<EnhanceDefinition>> getNamedEnhancers() {
        if (!isInit) {
            // todo
            return Collections.emptyMap();
        }
        final Map<String, List<EnhanceDefinition>> result = new HashMap<String, List<EnhanceDefinition>>();
        for (ExtAgentLoader loader : extAgentLoaders) {
            for (Map.Entry<String, List<EnhanceDefinition>> entry : loader.getNamedEnhancers().entrySet()) {
                final String key = entry.getKey();
                final List<EnhanceDefinition> parsedList = result.get(key);
                if (parsedList == null) {
                    result.put(key, entry.getValue());
                } else {
                    parsedList.addAll(entry.getValue());
                }
            }
        }
        return result;
    }

    public static List<EnhanceDefinition> getNonNamedEnhancers() {
        if (!isInit) {
            // todo
            return Collections.emptyList();
        }
        final List<EnhanceDefinition> result = new ArrayList<EnhanceDefinition>();
        for (ExtAgentLoader loader : extAgentLoaders) {
            result.addAll(loader.getNonNamedEnhancers());
        }
        return result;
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
