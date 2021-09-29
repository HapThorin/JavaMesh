package com.lubanops.apm.premain;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lubanops.apm.bootstrap.commons.LubanApmConstants;
import com.lubanops.apm.bootstrap.log.LogFactory;
import com.lubanops.apm.bootstrap.log.LogPathUtils;
import com.lubanops.apm.premain.agent.AgentStatus;
import com.lubanops.apm.premain.agent.ArgumentBuilder;
import com.lubanops.apm.premain.log.CollectorLogFactory;
import com.lubanops.apm.premain.utils.LibPathUtils;

import com.huawei.apm.bootstrap.agent.ExtAgentManager;
import com.huawei.apm.bootstrap.config.ConfigLoader;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.serialize.SerializerHolder;
import com.huawei.apm.classloader.ClassLoaderManager;
import com.huawei.apm.classloader.PluginClassLoader;
import com.huawei.apm.premain.BootstrapEnhance;
import com.huawei.apm.premain.ByteBuddyAgentBuilder;
import com.huawei.apm.premain.NoneNamedListenerBuilder;

public class AgentPremain {

    private static final String AGENT_JAR_FILE_NAME = "apm-javaagent.jar";

    private static AgentStatus agentStatus = AgentStatus.STOPPED;

    private static Logger logger;

    private static ArgumentBuilder argumentBuilder = new ArgumentBuilder();

    //~~ premain method

    @SuppressWarnings("rawtypes")
    public static void premain(String agentArgs, Instrumentation instrumentation) {
//        try {
//            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            final Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//            addURL.setAccessible(true);
//            addURL.invoke(classLoader, new File(
//                    "D:\\workplace\\other\\skywalking\\dist\\apache-skywalking-apm-bin\\agent\\skywalking-agent.jar")
//                    .toURI().toURL());
//            final Class<?> skyWalkingAgent = classLoader.loadClass("org.apache.skywalking.apm.agent.SkyWalkingAgent");
//            final Method premain = skyWalkingAgent.getDeclaredMethod("premain", String.class, Instrumentation.class);
//            premain.invoke(null, agentArgs, instrumentation);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        try {
            if (AgentStatus.STOPPED.equals(agentStatus)) {
                agentStatus = AgentStatus.LOADING;
                // 添加bootstrap包到bootstrap classloader中
                loadBootstrap(instrumentation);
                Map argsMap = argumentBuilder.build(agentArgs, new LogInitCallback());
                // 获取javaagent依赖的包
                final List<URL> urls = LibPathUtils.getLibUrl();
                urls.add(AgentPremain.class.getProtectionDomain().getCodeSource().getLocation());
                logger.info("[APM PREMAIN]loading javaagent.");
                addAgentPath(argsMap);
                // 初始化序列化器
                SerializerHolder.initialize(PluginClassLoader.getDefault());
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                ClassLoader spiLoader = ClassLoaderManager.getTargetClassLoader(contextClassLoader);
                // 配置初始化
                ConfigLoader.initialize(agentArgs, spiLoader);
                ExtAgentManager.init(contextClassLoader, spiLoader, agentArgs, instrumentation);
                // 针对NoneNamedListener初始化增强
                NoneNamedListenerBuilder.initialize(instrumentation);
                // 初始化byte buddy
                ByteBuddyAgentBuilder.initialize(instrumentation);
                // 重定义, 使之可被bytebuddy增强
                BootstrapEnhance.reTransformClasses(instrumentation);
            } else {
                logger.log(Level.SEVERE, "[APM PREMAIN]The JavaAgent is loaded repeatedly.");
            }
            AgentPremain.agentStatus = AgentStatus.STARTED;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[APM PREMAIN]Loading javaagent failed", e);
        }

    }

    //~~internal methods

    private static void addAgentPath(Map argsMap) {
        String agentPath = LibPathUtils.getAgentPath();
        String bootPath = LibPathUtils.getBootstrapJarPath();
        String pluginsPath = LibPathUtils.getPluginsJarPath();
        argsMap.put(LubanApmConstants.AGENT_PATH_COMMONS, agentPath);
        argsMap.put(LubanApmConstants.BOOT_PATH_COMMONS, bootPath);
        argsMap.put(LubanApmConstants.PLUGINS_PATH_COMMONS, pluginsPath);
    }

    private static void loadBootstrap(Instrumentation instrumentation) throws IOException {
        ProtectionDomain pd = AgentPremain.class.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        String jarPath = cs.getLocation().getPath();
        String agentPath = jarPath.substring(0, jarPath.lastIndexOf(AGENT_JAR_FILE_NAME));
        String bootPath = agentPath + File.separator + "boot";
        List<JarFile> bootstrapJar = new ArrayList<JarFile>();
        File libDir = new File(bootPath);
        File[] files = libDir.listFiles();
        if (files != null) {
            for (File file : files) {
                bootstrapJar.add(new JarFile(file));
            }
        }
        for (JarFile jar : bootstrapJar) {
            instrumentation.appendToBootstrapClassLoaderSearch(jar);
        }
    }

    public static class LogInitCallback {
        public void initLog(String appName, String instanceName) {
            LogPathUtils.build(appName, instanceName);
            Logger apmLogger = CollectorLogFactory.getLogger("luban.apm");
            AgentPremain.logger = apmLogger;
            LogFactory.setLogger(apmLogger);
        }
    }
}
