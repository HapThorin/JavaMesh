package com.huawei.apm.bootstrap.agent;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

public interface ExtAgentLoader {
    ExtAgentType getType();

    void doPremain(String agentArgs, Instrumentation instrumentation);

    void uniteNamedEnhancers(Map<String, List<EnhanceDefinition>> nameDefinitions);

    void getNonNamedEnhancers(List<EnhanceDefinition> nonNameDefinitions);

    Interceptor createInterceptor(Class<?> cls);
}
