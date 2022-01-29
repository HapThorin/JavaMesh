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

package com.huawei.sermant.plugins.skywalking.adaptor.declarer;

import com.huawei.sermant.core.common.LoggerFactory;
import com.huawei.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import com.huawei.sermant.core.plugin.agent.declarer.PluginDescription;
import com.huawei.sermant.core.plugin.agent.declarer.SuperTypeDeclarer;
import com.huawei.sermant.core.plugin.agent.interceptor.Interceptor;
import com.huawei.sermant.core.plugin.agent.matcher.MethodMatcher;
import com.huawei.sermant.core.plugin.agent.transformer.AdviceTransformer;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * skywalking插件描述器，主要功能为将skywalking的插件定义转换为sermant插件描述器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-29
 */
public class SkyWalkingPluginDescription implements PluginDescription {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    /**
     * skywalking的插件查找器
     */
    private final PluginFinder pluginFinder;

    /**
     * 加载skywalking插件的类加载器
     */
    private final ClassLoader pluginClassLoader;

    /**
     * 类相关的插件定义
     */
    private List<AbstractClassEnhancePluginDefine> pluginDefineList;

    public SkyWalkingPluginDescription(PluginFinder pluginFinder, ClassLoader pluginClassLoader) {
        this.pluginFinder = pluginFinder;
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    public boolean matches(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
        if (pluginFinder.buildMatch().matches(typeDescription)) {
            pluginDefineList = pluginFinder.find(typeDescription);
            return true;
        }
        return false;
    }

    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
            ClassLoader classLoader, JavaModule module) {
        if (classLoader == null) {
            LOGGER.warning("Enhancing classes loaded by bootstrap class loader is unsupported for the time being. ");
            return builder;
        }
        if (pluginDefineList.isEmpty()) {
            return builder;
        }
        final List<InterceptDeclarer> interceptDeclarers =
                getInterceptDeclarers(typeDescription, pluginDefineList, pluginClassLoader);
        if (interceptDeclarers.isEmpty()) {
            return builder;
        }
        final DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<?> newBuilder = builder
                .defineField("_$EnhancedClassField_ws", Object.class, Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE)
                .implement(EnhancedInstance.class)
                .intercept(FieldAccessor.ofField("_$EnhancedClassField_ws"));
        return new AdviceTransformer(
                interceptDeclarers.toArray(new InterceptDeclarer[0]), new SuperTypeDeclarer[0]
        ).transform(newBuilder, typeDescription, classLoader, module);
    }

    /**
     * 获取拦截声明器集
     *
     * @param typeDesc      类型定义
     * @param pluginDefines skywalking插件定义集
     * @param pluginLoader  插件类加载器
     * @return 拦截声明器集
     */
    private List<InterceptDeclarer> getInterceptDeclarers(TypeDescription typeDesc,
            List<AbstractClassEnhancePluginDefine> pluginDefines, ClassLoader pluginLoader) {
        final List<InterceptDeclarer> interceptDeclarers = new ArrayList<>();
        for (MethodDescription.InDefinedShape methodDesc : typeDesc.getDeclaredMethods()) {
            if (methodDesc.isNative() || methodDesc.isAbstract() || methodDesc.isBridge()
                    || MethodMatcher.isDeclaredByObject().matches(methodDesc)) {
                continue;
            }
            final List<Interceptor> interceptors = getInterceptors(methodDesc, pluginDefines, pluginLoader);
            if (interceptors.isEmpty()) {
                continue;
            }
            interceptDeclarers.add(InterceptDeclarer.build(MethodMatcher.build(ElementMatchers.is(methodDesc)),
                    interceptors.toArray(new Interceptor[0])));
        }
        return interceptDeclarers;
    }

    /**
     * 获取单个方法的sermant拦截器集
     *
     * @param methodDesc    方法定义
     * @param pluginDefines skywalking插件定义集
     * @param pluginLoader  插件类加载器
     * @return sermant拦截器集
     */
    private List<Interceptor> getInterceptors(MethodDescription.InDefinedShape methodDesc,
            List<AbstractClassEnhancePluginDefine> pluginDefines, ClassLoader pluginLoader) {
        final List<Interceptor> interceptors = new ArrayList<>();
        for (AbstractClassEnhancePluginDefine pluginDefine : pluginDefines) {
            final Interceptor interceptor = getInterceptor(methodDesc, pluginDefine, pluginLoader);
            if (interceptor != null) {
                interceptors.add(interceptor);
            }
        }
        return interceptors;
    }

    /**
     * 获取sermant拦截器
     *
     * @param methodDesc   方法定义
     * @param pluginDefine skywalking插件定义
     * @param pluginLoader 插件类加载器
     * @return sermant拦截器
     */
    private Interceptor getInterceptor(MethodDescription.InDefinedShape methodDesc,
            AbstractClassEnhancePluginDefine pluginDefine, ClassLoader pluginLoader) {
        if (methodDesc.isConstructor()) {
            for (ConstructorInterceptPoint point : pluginDefine.getConstructorsInterceptPoints()) {
                if (point.getConstructorMatcher().matches(methodDesc)) {
                    return InterceptorTransformer.createInterceptor(point.getConstructorInterceptor(), pluginLoader);
                }
            }
        } else if (methodDesc.isStatic()) {
            for (StaticMethodsInterceptPoint point : pluginDefine.getStaticMethodsInterceptPoints()) {
                if (point.getMethodsMatcher().matches(methodDesc)) {
                    return InterceptorTransformer.createInterceptor(point.getMethodsInterceptor(), pluginLoader);
                }
            }
        } else {
            for (InstanceMethodsInterceptPoint point : pluginDefine.getInstanceMethodsInterceptPoints()) {
                if (point.getMethodsMatcher().matches(methodDesc)) {
                    return InterceptorTransformer.createInterceptor(point.getMethodsInterceptor(), pluginLoader);
                }
            }
        }
        return null;
    }
}
