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

import com.huawei.sermant.plugins.skywalking.adaptor.collector.SkyWalkingPluginCollector;

import net.bytebuddy.asm.Advice;

import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.SnifferConfigInitializer;
import org.apache.skywalking.apm.agent.core.plugin.PluginBootstrap;
import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;

/**
 * 用于非侵入式地修改skywalking代码的adviser，将其增强逻辑屏蔽，同时取出插件定义列表
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-29
 */
public class SkyWalkingAgentAdviser {
    private SkyWalkingAgentAdviser() {
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onMethodEnter(@Advice.Argument(0) String agentArgs) {
        try {
            SnifferConfigInitializer.initializeCoreConfig(agentArgs);
            SkyWalkingPluginCollector.initialize(new PluginFinder(new PluginBootstrap().loadPlugins()));
            ServiceManager.INSTANCE.boot();
            Runtime.getRuntime().addShutdownHook(
                    new Thread(ServiceManager.INSTANCE::shutdown, "skywalking service shutdown thread"));
        } catch (Throwable ignored) {
            // Cannot find any medium to write out the exception log.
        }
        return true;
    }
}
