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
import com.huawei.sermant.core.plugin.agent.entity.ExecuteContext;
import com.huawei.sermant.core.plugin.agent.interceptor.Interceptor;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * 拦截器转换器，提供将skywalking拦截器转换为sermant拦截器的方法
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-29
 */
public class InterceptorTransformer {
    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger();

    private InterceptorTransformer() {
    }

    /**
     * 创建sermant的拦截器
     *
     * @param interceptor skywalking拦截器名
     * @param classLoader 加载skywalking插件的类加载器
     * @return sermant的拦截器
     */
    public static Interceptor createInterceptor(String interceptor, ClassLoader classLoader) {
        try {
            final Object interceptorObj = classLoader.loadClass(interceptor).newInstance();
            if (interceptorObj instanceof StaticMethodsAroundInterceptor) {
                return interceptorTransform((StaticMethodsAroundInterceptor) interceptorObj);
            } else if (interceptorObj instanceof InstanceConstructorInterceptor) {
                return interceptorTransform((InstanceConstructorInterceptor) interceptorObj);
            } else if (interceptorObj instanceof InstanceMethodsAroundInterceptor) {
                return interceptorTransform((InstanceMethodsAroundInterceptor) interceptorObj);
            } else {
                LOGGER.warning(String.format(Locale.ROOT, "Unknown type of interceptor %s. ", interceptor));
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ignored) {
            LOGGER.warning(String.format(Locale.ROOT, "Create instance of interceptor %s, failed. ", interceptor));
        }
        return null;
    }

    /**
     * 获取入参的类型数组
     *
     * @param args 入参数组
     * @return 入参的类型数组
     */
    private static Class<?>[] getArgClasses(Object[] args) {
        if (args == null || args.length == 0) {
            return new Class<?>[0];
        }
        final Class<?>[] allClasses = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            allClasses[i] = args[i].getClass();
        }
        return allClasses;
    }

    /**
     * 将skywalking静态拦截器转换为sermant拦截器
     *
     * @param interceptor skywalking静态拦截器
     * @return sermant拦截器
     */
    private static Interceptor interceptorTransform(final StaticMethodsAroundInterceptor interceptor) {
        return new Interceptor() {
            @Override
            public ExecuteContext before(ExecuteContext context) {
                final MethodInterceptResult result = new MethodInterceptResult();
                interceptor.beforeMethod(context.getRawCls(), context.getMethod(), context.getArguments(),
                        getArgClasses(context.getArguments()), result);
                return result.isContinue() ? context : context.skip(result._ret());
            }

            @Override
            public ExecuteContext after(ExecuteContext context) {
                return context.changeResult(interceptor.afterMethod(context.getRawCls(), context.getMethod(),
                        context.getArguments(), getArgClasses(context.getArguments()), context.getResult()));
            }

            @Override
            public ExecuteContext onThrow(ExecuteContext context) {
                interceptor.handleMethodException(context.getRawCls(), context.getMethod(), context.getArguments(),
                        getArgClasses(context.getArguments()), context.getThrowable());
                return context;
            }
        };
    }

    /**
     * 将skywalking构造拦截器转换为sermant拦截器
     *
     * @param interceptor skywalking构造拦截器
     * @return sermant拦截器
     */
    private static Interceptor interceptorTransform(final InstanceConstructorInterceptor interceptor) {
        return new Interceptor() {
            @Override
            public ExecuteContext before(ExecuteContext context) {
                return context;
            }

            @Override
            public ExecuteContext after(ExecuteContext context) {
                final Object rawObj = context.getObject();
                if (rawObj instanceof EnhancedInstance) {
                    interceptor.onConstruct((EnhancedInstance) rawObj, context.getArguments());
                }
                return context;
            }

            @Override
            public ExecuteContext onThrow(ExecuteContext context) {
                return context;
            }
        };
    }

    /**
     * 将skywalking实例拦截器转换为sermant拦截器
     *
     * @param interceptor skywalking实例拦截器
     * @return sermant拦截器
     */
    private static Interceptor interceptorTransform(final InstanceMethodsAroundInterceptor interceptor) {
        return new Interceptor() {
            @SuppressWarnings("checkstyle:IllegalCatch")
            @Override
            public ExecuteContext before(ExecuteContext context) throws Exception {
                final Object rawObj = context.getObject();
                if (rawObj instanceof EnhancedInstance) {
                    try {
                        final MethodInterceptResult result = new MethodInterceptResult();
                        interceptor.beforeMethod((EnhancedInstance) rawObj, context.getMethod(), context.getArguments(),
                                getArgClasses(context.getArguments()), result);
                        return result.isContinue() ? context : context.skip(result._ret());
                    } catch (Throwable e) {
                        throw new Exception(e);
                    }
                }
                return context;
            }

            @SuppressWarnings("checkstyle:IllegalCatch")
            @Override
            public ExecuteContext after(ExecuteContext context) throws Exception {
                final Object rawObj = context.getObject();
                if (rawObj instanceof EnhancedInstance) {
                    try {
                        return context.changeResult(
                                interceptor.afterMethod((EnhancedInstance) rawObj, context.getMethod(),
                                        context.getArguments(), getArgClasses(context.getArguments()),
                                        context.getResult()));
                    } catch (Throwable e) {
                        throw new Exception(e);
                    }
                }
                return context;
            }

            @Override
            public ExecuteContext onThrow(ExecuteContext context) {
                final Object rawObj = context.getObject();
                if (rawObj instanceof EnhancedInstance) {
                    interceptor.handleMethodException((EnhancedInstance) rawObj, context.getMethod(),
                            context.getArguments(), getArgClasses(context.getArguments()), context.getThrowable());
                }
                return context;
            }
        };
    }
}
