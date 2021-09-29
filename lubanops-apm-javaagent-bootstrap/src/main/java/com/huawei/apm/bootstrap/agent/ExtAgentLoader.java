package com.huawei.apm.bootstrap.agent;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Map;

import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

public interface ExtAgentLoader {
    ExtAgentType getType();

    void doPremain(String agentArgs, Instrumentation instrumentation);

    Map<String, List<EnhanceDefinition>> getNamedEnhancers();

    List<EnhanceDefinition> getNonNamedEnhancers();

    Interceptor createInterceptor(Class<?> cls);
}
