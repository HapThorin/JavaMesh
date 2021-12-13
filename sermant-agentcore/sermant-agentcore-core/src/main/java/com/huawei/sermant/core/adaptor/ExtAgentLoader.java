/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.sermant.core.adaptor;

import java.lang.instrument.Instrumentation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;

import com.huawei.sermant.core.adaptor.entity.ExtAgentTransResp;
import com.huawei.sermant.core.agent.interceptor.Interceptor;

/**
 * 额外agent的加载策略
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/11
 */
public interface ExtAgentLoader {
    /**
     * 初始化额外agent，提供调用premain方法的参数
     *
     * @param agentArgs       agent参数
     * @param instrumentation Instrumentation
     * @return 是否初始化成功
     */
    boolean init(String agentArgs, Instrumentation instrumentation);

    /**
     * 在构建matcher时调用，将应用于{@link net.bytebuddy.agent.builder.AgentBuilder#type}
     *
     * @return 元素匹配器
     */
    ElementMatcher<TypeDescription> buildMatch();

    /**
     * 转换方法，将应用于{@link net.bytebuddy.agent.builder.AgentBuilder.Identified#transform}
     *
     * @param builder         构建器
     * @param typeDescription 类型描述
     * @param classLoader     类加载器
     * @return 转换相应
     */
    ExtAgentTransResp transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
            ClassLoader classLoader);

    /**
     * 尝试依据拦截器类名创建拦截器
     *
     * @param className 拦截器类名
     * @return 拦截器实例
     */
    Interceptor newInterceptor(String className, Class<?> interceptorType);
}
