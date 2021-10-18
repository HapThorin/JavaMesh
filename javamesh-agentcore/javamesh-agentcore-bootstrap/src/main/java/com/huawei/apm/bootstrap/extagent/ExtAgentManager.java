/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.extagent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import org.yaml.snakeyaml.Yaml;

import com.huawei.apm.bootstrap.config.ConfigLoader;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.extagent.entity.ExtAgentConfig;
import com.huawei.apm.bootstrap.extagent.entity.ExtAgentMapping;
import com.huawei.apm.bootstrap.extagent.entity.ExtAgentTransResp;
import com.huawei.apm.bootstrap.extagent.entity.LibJarMapping;
import com.huawei.apm.bootstrap.interceptors.Interceptor;
import com.huawei.apm.bootstrap.lubanops.log.LogFactory;

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

    private static final Set<String> EXT_AGENT_TYPES = Collections.unmodifiableSet(new HashSet<String>(
            Collections.singletonList("skywalking")));

    /**
     * 额外的agent加载策略，只有addUrl成功的才会添加
     */
    private static final List<ExtAgentLoader> EXT_AGENT_LOADERS = new ArrayList<ExtAgentLoader>();

    /**
     * 初始化标记
     */
    private static volatile boolean isInit = false;

    public static boolean isNotExtAgentType(String name) {
        return !EXT_AGENT_TYPES.contains(name);
    }

    public static boolean isInit() {
        return isInit;
    }

    /**
     * 初始化，判断是否需要进行初始化操作，如果需要，则执行{@link #doInit}
     *
     * @param agentArgs       agent启动参数
     * @param instrumentation Instrumentation
     */
    public static void init(String agentArgs, Instrumentation instrumentation, String agentPath, String pluginsPath) {
        if (!isInit) {
            synchronized (EXT_AGENT_LOADERS) {
                if (!isInit) {
                    isInit = doInit(agentArgs, instrumentation, agentPath, pluginsPath);
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
     * @param agentArgs       agent启动参数
     * @param instrumentation Instrumentation
     * @return 是否初始化成功
     */
    private static boolean doInit(String agentArgs, Instrumentation instrumentation, String agentPath,
            String pluginsPath) {
        final ExtAgentConfig config = ConfigLoader.getConfig(ExtAgentConfig.class);
        if (!config.isLoadExtAgent()) {
            return false;
        }
        final URLAppender urlAppender = URLAppender.build();
        if (urlAppender == null) {
            return false;
        }
        for (String extAgentType : EXT_AGENT_TYPES) {
            final String extAgentPath = pluginsPath + File.separatorChar + extAgentType;
            final ExtAgentMapping extAgentMapping =
                    readExtAgentMapping(extAgentPath + File.separatorChar + config.getMappingName());
            if (extAgentMapping == null) {
                // todo log
                continue;
            }
            if (!addAdaptorJar(extAgentPath, urlAppender, extAgentMapping)) {
                // todo log
                continue;
            }
            final String cacheExtAgentPath = cacheStructure(extAgentPath, config.getTempCacheDir(agentPath),
                    extAgentType, extAgentMapping);
            if (cacheExtAgentPath == null) {
                // todo log
                continue;
            }
            final String executeExtAgentPath;
            if (extAgentMapping.needShade()) {
                executeExtAgentPath = cacheExtAgentPath + "-shade";
                ExtAgentShader.shade(cacheExtAgentPath, executeExtAgentPath, extAgentMapping.getShadeMappings(),
                        extAgentMapping.getShadeExcludes());
            } else {
                executeExtAgentPath = cacheExtAgentPath;
            }
            if (!addAgentJar(executeExtAgentPath, urlAppender, extAgentMapping)) {
                // todo log
            }
        }
        for (ExtAgentLoader loader : urlAppender.getExtAgentLoaders()) {
            if (loader.init(agentArgs, instrumentation)) {
                EXT_AGENT_LOADERS.add(loader);
            }
        }
        return true;
    }

    private static boolean addAdaptorJar(String extAgentPath, URLAppender urlAppender,
            final ExtAgentMapping extAgentMapping) {
        final File extAgentDir = new File(extAgentPath);
        if (!extAgentDir.exists() || !extAgentDir.isDirectory()) {
            return false;
        }
        final File[] adaptorJars = extAgentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") &&
                        ExtAgentUtils.isWildcardMatch(name, extAgentMapping.getAdaptorJar());
            }
        });
        if (adaptorJars == null || adaptorJars.length <= 0) {
            return false;
        }
        urlAppender.addURL(adaptorJars[0]);
        return true;
    }

    private static boolean addAgentJar(String shadedExtAgentPath, URLAppender urlAppender,
            final ExtAgentMapping extAgentMapping) {
        final File shadeExtAgentDir = new File(shadedExtAgentPath);
        if (!shadeExtAgentDir.exists() || !shadeExtAgentDir.isDirectory()) {
            return false;
        }
        final File[] agentJars = shadeExtAgentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") &&
                        ExtAgentUtils.isWildcardMatch(name, extAgentMapping.getAgentJar());
            }
        });
        if (agentJars == null || agentJars.length <= 0) {
            return false;
        }
        urlAppender.addURL(agentJars[0]);
        return true;
    }

    private static void cachePlugins(String extAgentPath, String cacheExtAgentPath,
            final ExtAgentMapping extAgentMapping) throws IOException {
        final File extAgentDir = new File(extAgentPath);
        if (!extAgentDir.exists() || !extAgentDir.isDirectory()) {
            return;
        }
        final File[] pluginJars = extAgentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar") && !ExtAgentUtils.isWildcardMatch(name, extAgentMapping.getAdaptorJar());
            }
        });
        if (pluginJars == null) {
            return;
        }
        for (File pluginJar : pluginJars) {
            final File targetFile = new File(cacheExtAgentPath + File.separatorChar + extAgentMapping.getPluginsDir() +
                    File.separatorChar + pluginJar.getName());
            if (!ExtAgentUtils.createParentDir(targetFile)) {
                return;
            }
            ExtAgentUtils.copyFile(pluginJar, targetFile);
        }
    }

    private static void cacheConfigs(String extAgentPath, String cacheExtAgentPath, ExtAgentMapping extAgentMapping)
            throws IOException {
        ExtAgentUtils.copyAllFiles(new File(extAgentPath + File.separatorChar + "config"),
                cacheExtAgentPath + File.separatorChar + extAgentMapping.getConfigsDir());
    }

    private static void cacheLibJars(String extAgentPath, String cacheExtAgentPath, ExtAgentMapping extAgentMapping)
            throws IOException {
        final List<LibJarMapping> libJarMappings = extAgentMapping.getLibJarMappings();
        if (libJarMappings == null) {
            return;
        }
        final String libPath = extAgentPath + File.separatorChar + "lib";
        for (final LibJarMapping mapping : libJarMappings) {
            final String targetDirPath = cacheExtAgentPath + File.separatorChar + mapping.getTargetDir();
            final File targetDir = new File(targetDirPath);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                continue;
            }
            final File wildcardFile = new File(libPath + File.separatorChar + mapping.getSourcePath());
            final String wildcardName = wildcardFile.getName();
            final File resourceDir = wildcardFile.getParentFile();
            if (!resourceDir.exists() || !resourceDir.isDirectory()) {
                continue;
            }
            final File[] matchFiles = resourceDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return ExtAgentUtils.isWildcardMatch(name, wildcardName);
                }
            });
            if (matchFiles == null) {
                continue;
            }
            for (File matchFile : matchFiles) {
                ExtAgentUtils.copyFile(matchFile, new File(targetDirPath + File.separatorChar + matchFile.getName()));
            }
        }
    }

    private static void cacheResources(String extAgentPath, String cacheExtAgentPath) throws IOException {
        final File resourceDir = new File(extAgentPath + File.separatorChar + "resources");
        if (!resourceDir.exists() && !resourceDir.mkdirs()) {
            return;
        }
        ExtAgentUtils.copyAllFiles(resourceDir, cacheExtAgentPath);
    }

    private static String cacheStructure(String extAgentPath, String cacheCacheDirPath, String extAgentType,
            ExtAgentMapping extAgentMapping) {
        final String cacheExtAgentPath = cacheCacheDirPath + File.separatorChar + extAgentType;
        ExtAgentUtils.deleteDirs(new File(cacheExtAgentPath));
        try {
            cachePlugins(extAgentPath, cacheExtAgentPath, extAgentMapping);
            cacheConfigs(extAgentPath, cacheExtAgentPath, extAgentMapping);
            cacheLibJars(extAgentPath, cacheExtAgentPath, extAgentMapping);
            cacheResources(extAgentPath, cacheExtAgentPath);
        } catch (IOException ignored) {
            return null;
        }
        return cacheExtAgentPath;
    }

    private static ExtAgentMapping readExtAgentMapping(String mappingPath) {
        final File extAgentMappingFile = new File(mappingPath);
        if (!extAgentMappingFile.exists() || !extAgentMappingFile.isFile()) {
            return null;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(extAgentMappingFile), Charset.forName("UTF-8"));
            return new Yaml().loadAs(reader, ExtAgentMapping.class);
        } catch (IOException ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
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
     * @param className       拦截器名称
     * @param interceptorType 拦截器类型
     * @return 拦截器对象
     */
    public static Interceptor newInterceptor(String className, Class<?> interceptorType) {
        if (!isInit) {
            LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                    "[%s] hasn't been initialized yet, or initializes failed.",
                    ExtAgentManager.class.getSimpleName()));
            return null;
        }
        for (ExtAgentLoader extAgentLoader : EXT_AGENT_LOADERS) {
            final Interceptor interceptor = extAgentLoader.newInterceptor(className, interceptorType);
            if (interceptor != null) {
                return interceptor;
            }
        }
        LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                "There is no extra agent can create interceptor of [%s].", className));
        return null;
    }

    private static class URLAppender {
        private final ClassLoader classLoader;
        private final Method addURL;

        private URLAppender(ClassLoader classLoader, Method addURL) {
            this.classLoader = classLoader;
            this.addURL = addURL;
        }

        void addURL(File jarFile) {
            try {
                addURL.invoke(classLoader, jarFile.toURI().toURL());
            } catch (Exception ignored) {
                LOGGER.log(Level.WARNING, String.format(Locale.ROOT,
                        "Add agent jar [%s] to class loader failed.", jarFile));
            }
        }

        Iterable<ExtAgentLoader> getExtAgentLoaders() {
            return ServiceLoader.load(ExtAgentLoader.class, classLoader);
        }

        static URLAppender build() {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                return new URLAppender(classLoader, addURL);
            } catch (NoSuchMethodException ignored) {
                LOGGER.log(Level.SEVERE, String.format(Locale.ROOT,
                        "Cannot find 'addURL' method, [%s] initialize failed.", ExtAgentManager.class.getSimpleName()));
                return null;
            }
        }
    }
}
