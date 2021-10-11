/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.agent;

import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.definition.MethodInterceptPoint;
import com.huawei.apm.bootstrap.matcher.ClassMatcher;
import com.huawei.apm.bootstrap.matcher.NameMatcher;
import com.lubanops.apm.bootstrap.log.LogFactory;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * <p>提供一个静态方法{@link EnhanceDefinitionAdapterFactory#adapter(AbstractClassEnhancePluginDefine)},
 * 用于将skyWalking的插件定义{@link AbstractClassEnhancePluginDefine} 转换成 {@link EnhanceDefinition}</p>
 *
 * @author y30010171
 * @since 2021-09-27
 **/
public class EnhanceDefinitionAdapterFactory {

    /**
     * 将SkyWalking中的插件定义{@link AbstractClassEnhancePluginDefine} 转换成 {@link EnhanceDefinition}
     *
     * @param abstractClassEnhancePluginDefine 待转换的SkyWalking插件定义{@link AbstractClassEnhancePluginDefine}
     * @return {@link EnhanceDefinition}
     */
    public static EnhanceDefinition adapter(AbstractClassEnhancePluginDefine abstractClassEnhancePluginDefine) {
        return new EnhanceDefinitionAdapter(abstractClassEnhancePluginDefine);
    }

    static class EnhanceDefinitionAdapter implements EnhanceDefinition {
        private static final Logger LOGGER = LogFactory.getLogger();
        private final AbstractClassEnhancePluginDefine abstractClassEnhancePluginDefine;
        private final ClassMatcher classMatcher;

        private EnhanceDefinitionAdapter(AbstractClassEnhancePluginDefine abstractClassEnhancePluginDefine) {
            this.abstractClassEnhancePluginDefine = abstractClassEnhancePluginDefine;
            ClassMatcher tempClassMatcher;
            try {
                Method enhanceClass = abstractClassEnhancePluginDefine.getClass().getDeclaredMethod("enhanceClass");
                enhanceClass.setAccessible(true);
                tempClassMatcher = ClassMatcherAdapterFactory.adapter((ClassMatch) enhanceClass.invoke(abstractClassEnhancePluginDefine));
            } catch (Exception e) {
                LOGGER.warning(String.format(Locale.ROOT, "Can't get ClassMatch which should be provided by AbstractClassEnhancePluginDefine. %s", e.getMessage()));
                tempClassMatcher = new NameMatcher("");
            }
            this.classMatcher = tempClassMatcher;
        }

        @Override
        public ClassMatcher enhanceClass() {
            return classMatcher;
        }

        @Override
        public MethodInterceptPoint[] getMethodInterceptPoints() {
            try {
                int staticMethodSize = 0;
                if (abstractClassEnhancePluginDefine.getStaticMethodsInterceptPoints() != null) {
                    staticMethodSize = abstractClassEnhancePluginDefine.getStaticMethodsInterceptPoints().length;
                }
                int constructMethodSize = 0;
                if (abstractClassEnhancePluginDefine.getConstructorsInterceptPoints() != null) {
                    constructMethodSize = abstractClassEnhancePluginDefine.getConstructorsInterceptPoints().length;
                }
                int instanceMethodSize = 0;
                if (abstractClassEnhancePluginDefine.getInstanceMethodsInterceptPoints() != null) {
                    instanceMethodSize = abstractClassEnhancePluginDefine.getInstanceMethodsInterceptPoints().length;
                }
                MethodInterceptPoint[] methodInterceptPoints = new MethodInterceptPoint[constructMethodSize + staticMethodSize + instanceMethodSize];

                int index = 0;

                // 转换静态方法拦截点
                if (staticMethodSize > 0) {
                    for (StaticMethodsInterceptPoint interceptPoint : abstractClassEnhancePluginDefine.getStaticMethodsInterceptPoints()) {
                        methodInterceptPoints[index++] = MethodInterceptPoint.newStaticMethodInterceptPoint(interceptPoint.getMethodsInterceptor(), interceptPoint.getMethodsMatcher());
                    }
                }

                // 转换构造方法拦截点
                if (constructMethodSize > 0) {
                    for (ConstructorInterceptPoint interceptPoint : abstractClassEnhancePluginDefine.getConstructorsInterceptPoints()) {
                        methodInterceptPoints[index++] = MethodInterceptPoint.newConstructorInterceptPoint(interceptPoint.getConstructorInterceptor(), interceptPoint.getConstructorMatcher());
                    }
                }

                // 转换实例方法拦截点
                if (abstractClassEnhancePluginDefine.getInstanceMethodsInterceptPoints() != null) {
                    for (InstanceMethodsInterceptPoint interceptPoint : abstractClassEnhancePluginDefine.getInstanceMethodsInterceptPoints()) {
                        methodInterceptPoints[index++] = MethodInterceptPoint.newInstMethodInterceptPoint(interceptPoint.getMethodsInterceptor(), interceptPoint.getMethodsMatcher());
                    }
                }
                return methodInterceptPoints;
            } catch (Throwable e) {
                LOGGER.warning(String.format(Locale.ROOT, "Can't get MethodInterceptPoints which should be provided by AbstractClassEnhancePluginDefine. %s", e.getMessage()));
            }
            return new MethodInterceptPoint[0];
        }
    }
}
