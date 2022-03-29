/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.huawei.flowcontrol;

import com.huawei.sermant.core.plugin.agent.declarer.AbstractPluginDeclarer;
import com.huawei.sermant.core.plugin.agent.declarer.InterceptDeclarer;
import com.huawei.sermant.core.plugin.agent.matcher.ClassMatcher;
import com.huawei.sermant.core.plugin.agent.matcher.MethodMatcher;

/**
 * dubbo增强 apache dubbo alibaba dubbo
 *
 * @author zhouss
 * @since 2022-02-10
 */
public abstract class DubboDeclarer extends AbstractPluginDeclarer {
    private final String enhanceClass;

    private final String interceptorClass;

    /**
     * dubbo声明器
     *
     * @param enhanceClass 增强方法
     * @param interceptorClass 拦截器权限定名
     */
    protected DubboDeclarer(String enhanceClass, String interceptorClass) {
        this.enhanceClass = enhanceClass;
        this.interceptorClass = interceptorClass;
    }

    @Override
    public ClassMatcher getClassMatcher() {
        return ClassMatcher.nameEquals(enhanceClass);
    }

    @Override
    public InterceptDeclarer[] getInterceptDeclarers(ClassLoader classLoader) {
        return new InterceptDeclarer[]{
            InterceptDeclarer.build(MethodMatcher.nameEquals("invoke"), interceptorClass)
        };
    }
}
