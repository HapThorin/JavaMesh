/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lubanops.apm.bootstrap.log.LogFactory;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import org.apache.skywalking.apm.agent.SkyWalkingAgent;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.loader.AgentClassLoader;

import com.huawei.apm.bootstrap.agent.ExtAgentLoader;
import com.huawei.apm.bootstrap.agent.ExtAgentManager;
import com.huawei.apm.bootstrap.agent.ExtAgentTransResp;
import com.huawei.apm.bootstrap.agent.ExtAgentType;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

/**
 * SkyWalking的agent加载策略
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
public class SkyWalkingAgentLoader implements ExtAgentLoader {
    /**
     * 日志
     */
    private static final Logger LOGGER = LogFactory.getLogger();

    /**
     * 获取加载策略类型，返回{@link ExtAgentType#SKY_WALKING}
     *
     * @return 加载策略类型
     */
    @Override
    public ExtAgentType getType() {
        return ExtAgentType.SKY_WALKING;
    }

    /**
     * 初始化SkyWalking的agent
     * <p>增强{@code PluginFinder}类的{@code buildMatch}方法，从中保留{@code PluginFinder}对象
     * <p>为避免多次调用{@link AgentBuilder#installOn}，在{@code buildMatch}方法中植入{@link SkyWalkingInterruptException}异常，在{@link
     * SkyWalkingAgent#premain}中初次调用{@code buildMatch}时抛出该异常，以达到终止调用{@link AgentBuilder#installOn}的目的
     *
     * @param agentArgs       agent参数
     * @param instrumentation Instrumentation
     * @return 是否初始化成功
     */
    @Override
    public boolean init(String agentArgs, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.<TypeDescription>named(
                        "org.apache.skywalking.apm.agent.core.plugin.PluginFinder"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                            TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {
                        return builder.method(ElementMatchers.<MethodDescription>named("buildMatch"))
                                .intercept(Advice.to(PluginFinderAdvice.class));
                    }
                })
                .installOn(instrumentation);
        try {
            SkyWalkingAgent.premain(agentArgs, instrumentation);
        } catch (SkyWalkingInterruptException ignored) {
            return true;
        }
        LOGGER.log(Level.SEVERE, "SkyWalking premain procession hasn't been interrupt.");
        return false;
    }

    @Override
    public ElementMatcher<TypeDescription> buildMatch() {
        return PluginFinderAdvice.buildMatch();
    }

    @Override
    public ExtAgentTransResp transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
            ClassLoader classLoader) {
        final List<EnhanceDefinition> result = PluginFinderAdvice.find(typeDescription);
        if (result.isEmpty()) {
            return ExtAgentTransResp.empty(builder);
        }
        return new ExtAgentTransResp(result,
                builder.defineField("_$EnhancedClassField_ws", Object.class, Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE)
                        .implement(EnhancedInstance.class)
                        .intercept(FieldAccessor.ofField("_$EnhancedClassField_ws")));
    }

    @Override
    public Interceptor newInterceptor(String className) {
        try {
            final Class<?> cls = AgentClassLoader.getDefault().loadClass(className);
            if (StaticMethodsAroundInterceptor.class.isAssignableFrom(cls)) {
                return InterceptorAdapterFactory.adapter((StaticMethodsAroundInterceptor) cls.newInstance());
            } else if (InstanceConstructorInterceptor.class.isAssignableFrom(cls)) {
                return InterceptorAdapterFactory.adapter((InstanceConstructorInterceptor) cls.newInstance());
            } else if (InstanceMethodsAroundInterceptor.class.isAssignableFrom(cls)) {
                return InterceptorAdapterFactory.adapter((InstanceMethodsAroundInterceptor) cls.newInstance());
            }
        } catch (InstantiationException ignored) {
        } catch (IllegalAccessException ignored) {
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * 用于中断{@link SkyWalkingAgent#premain}的异常
     */
    public static class SkyWalkingInterruptException extends RuntimeException {
    }

    /**
     * {@code PluginFinder}的建议器
     */
    public static class PluginFinderAdvice {
        /**
         * {@code PluginFinder}
         */
        private static Object pluginFinder;
        /**
         * {@code PluginFinder}的{@code buildMatch}方法
         */
        private static Method buildMatchMethod;
        /**
         * {@code PluginFinder}的{@code find}方法
         */
        private static Method findMethod;

        /**
         * 增强{@code PluginFinder}的{@code buildMatch}方法，如果正在初始化，则调用{@link PluginFinderAdvice#init}
         *
         * @param originPluginFinder {@code PluginFinder}实例
         */
        @Advice.OnMethodEnter
        public static void methodEnter(@Advice.This Object originPluginFinder) {
            if (!ExtAgentManager.isInit()) {
                PluginFinderAdvice.init(originPluginFinder);
            }
        }

        /**
         * 初始化，获取相关资源，并抛出{@link SkyWalkingInterruptException}异常中断{@link SkyWalkingAgent#premain}
         *
         * @param originPluginFinder {@code PluginFinder}实例
         */
        public static void init(Object originPluginFinder) {
            pluginFinder = originPluginFinder;
            try {
                final Class<?> pluginFinderClass = pluginFinder.getClass();
                buildMatchMethod = pluginFinderClass.getDeclaredMethod("buildMatch");
                findMethod = pluginFinderClass.getDeclaredMethod("find", TypeDescription.class);
            } catch (NoSuchMethodException ignored) {
            }
            throw new SkyWalkingInterruptException();
        }

        /**
         * 调用{@code PluginFinder}的{@code buildMatch}方法
         *
         * @return 元素匹配器
         */
        public static ElementMatcher<TypeDescription> buildMatch() {
            Object matcher = null;
            try {
                matcher = buildMatchMethod.invoke(pluginFinder);
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }
            if (!(matcher instanceof ElementMatcher)) {
                LOGGER.log(Level.WARNING, "Execute PluginFinder#buildMatch failed.");
                return null;
            }
            return (ElementMatcher<TypeDescription>) matcher;
        }

        /**
         * 调用{@code PluginFinder}的{@code find}方法，并将结果类型转换为{@link EnhanceDefinition}
         *
         * @param typeDescription 类型描述
         * @return EnhanceDefinition集合
         */
        public static List<EnhanceDefinition> find(TypeDescription typeDescription) {
            Object pluginDefines = null;
            try {
                pluginDefines = findMethod.invoke(pluginFinder, typeDescription);
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }
            if (!(pluginDefines instanceof List)) {
                LOGGER.log(Level.WARNING, "Execute PluginFinder#find failed.");
                return Collections.emptyList();
            }
            final List<EnhanceDefinition> result = new ArrayList<EnhanceDefinition>();
            for (Object pluginDefine : (List<?>) pluginDefines) {
                if (pluginDefine instanceof AbstractClassEnhancePluginDefine) {
                    result.add(EnhanceDefinitionAdapterFactory.adapter(
                            (AbstractClassEnhancePluginDefine) pluginDefine));
                }
            }
            return result;
        }
    }
}
