/*
 * Copyright (C) 2021-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.sermant.plugins.skywalking.adaptor.service;

import com.huawei.sermant.core.plugin.adaptor.service.AdaptorService;
import com.huawei.sermant.core.plugin.classloader.PluginClassLoader;
import com.huawei.sermant.core.utils.FileUtils;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import org.apache.skywalking.apm.agent.SkyWalkingAgent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * skywalking的适配器服务
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-29
 */
public class SkyWalkingAdaptorService implements AdaptorService {
    @Override
    public boolean start(String agentMainArg, File execEnvDir, ClassLoader classLoader,
            Instrumentation instrumentation) {
        if (!loadAgentJars(execEnvDir, classLoader, instrumentation)) {
            return false;
        }
        invokePremain(agentMainArg, instrumentation);
        return true;
    }

    /**
     * 加载skywalking入口包
     *
     * @param execEnvDir      运行环境目录
     * @param classLoader     加载适配包的类加载器
     * @param instrumentation Instrumentation对象
     * @return 是否加载入口包成功
     */
    private boolean loadAgentJars(File execEnvDir, ClassLoader classLoader, Instrumentation instrumentation) {
        final File[] agentJars = FileUtils.getChildrenByWildcard(execEnvDir, "apm-agent-*.jar");
        for (File agentJar : agentJars) {
            try {
                loadAgentJar(agentJar, classLoader, instrumentation);
            } catch (IOException ignored) {
                return false;
            }
        }
        return true;
    }

    /**
     * 加载agent入口包
     *
     * @param agentJar        agent入口
     * @param classLoader     加载适配包的类加载器
     * @param instrumentation Instrumentation对象
     * @throws IOException 加载失败
     */
    private void loadAgentJar(File agentJar, ClassLoader classLoader, Instrumentation instrumentation)
            throws IOException {
        if (classLoader instanceof PluginClassLoader) {
            ((PluginClassLoader) classLoader).addURL(agentJar.toURI().toURL());
        } else {
            JarFile jarfile = null;
            try {
                jarfile = new JarFile(agentJar);
                instrumentation.appendToSystemClassLoaderSearch(jarfile);
            } finally {
                if (jarfile != null) {
                    jarfile.close();
                }
            }
        }
    }

    /**
     * 调用入口包的入口方法
     * <p>调用之前，使用byte-buddy修改skywalking代码，将执行增强的逻辑屏蔽，并将其插件定义抽取出来，适配为插件描述器
     *
     * @param agentMainArg    luban agent启动参数
     * @param instrumentation Instrumentation对象
     */
    private void invokePremain(String agentMainArg, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.<TypeDescription>named(
                        "org.apache.skywalking.apm.agent.SkyWalkingAgent"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                            TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {
                        return builder.method(ElementMatchers.<MethodDescription>named("premain"))
                                .intercept(Advice.to(SkyWalkingAgentAdviser.class));
                    }
                })
                .installOn(instrumentation);
        SkyWalkingAgent.premain(agentMainArg, instrumentation);
    }

    @Override
    public void stop() {
    }
}
