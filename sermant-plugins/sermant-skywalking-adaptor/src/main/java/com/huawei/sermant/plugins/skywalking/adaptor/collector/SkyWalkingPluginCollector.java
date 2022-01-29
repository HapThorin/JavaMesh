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

package com.huawei.sermant.plugins.skywalking.adaptor.collector;

import com.huawei.sermant.core.plugin.adaptor.collector.AdaptorCollector;
import com.huawei.sermant.core.plugin.agent.collector.AbstractPluginCollector;
import com.huawei.sermant.core.plugin.agent.declarer.PluginDeclarer;
import com.huawei.sermant.core.plugin.agent.declarer.PluginDescription;
import com.huawei.sermant.plugins.skywalking.adaptor.declarer.SkyWalkingPluginDescription;

import org.apache.skywalking.apm.agent.core.plugin.PluginFinder;
import org.apache.skywalking.apm.agent.core.plugin.loader.AgentClassLoader;

import java.util.Collections;

/**
 * skywalking的插件收集器
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2022-01-29
 */
public class SkyWalkingPluginCollector extends AbstractPluginCollector implements AdaptorCollector {
    /**
     * skywalking的插件查找器
     */
    private static PluginFinder pluginFinder;

    /**
     * 初始化
     *
     * @param originPluginFinder skywalking的插件查找器
     */
    public static void initialize(PluginFinder originPluginFinder) {
        pluginFinder = originPluginFinder;
    }

    @Override
    public Iterable<? extends PluginDeclarer> getDeclarers() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<? extends PluginDescription> getDescriptions() {
        if (pluginFinder == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new SkyWalkingPluginDescription(pluginFinder, AgentClassLoader.getDefault()));
    }
}
