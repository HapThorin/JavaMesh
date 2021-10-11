/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.bootstrap.agent;

import java.util.Collections;
import java.util.List;

import net.bytebuddy.dynamic.DynamicType;

import com.huawei.apm.bootstrap.definition.EnhanceDefinition;

/**
 * 额外agent的转换结果，调用{@link ExtAgentLoader#transform}的返回值
 *
 * @author h30007557
 * @version 1.0.0
 * @since 2021/10/11
 */
public class ExtAgentTransResp {
    /**
     * 增强器定义集合
     */
    private final List<EnhanceDefinition> definitions;
    /**
     * builder
     */
    private final DynamicType.Builder<?> builder;

    public ExtAgentTransResp(List<EnhanceDefinition> definitions, DynamicType.Builder<?> builder) {
        this.definitions = definitions;
        this.builder = builder;
    }

    public List<EnhanceDefinition> getDefinitions() {
        return definitions;
    }

    public DynamicType.Builder<?> getBuilder() {
        return builder;
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    public static ExtAgentTransResp empty(DynamicType.Builder<?> builder) {
        return new ExtAgentTransResp(Collections.<EnhanceDefinition>emptyList(), builder);
    }
}
