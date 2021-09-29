package com.huawei.apm.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import org.apache.skywalking.apm.agent.SkyWalkingAgent;
import org.apache.skywalking.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;

import com.huawei.apm.bootstrap.agent.ExtAgentLoader;
import com.huawei.apm.bootstrap.agent.ExtAgentType;
import com.huawei.apm.bootstrap.definition.EnhanceDefinition;
import com.huawei.apm.bootstrap.interceptors.Interceptor;

public class SkyWalkingAgentLoader implements ExtAgentLoader {
    public static Object pluginFinder;

    @Override
    public ExtAgentType getType() {
        return ExtAgentType.SKY_WALKING;
    }

    @Override
    public void doPremain(String agentArgs, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.<TypeDescription>named(
                        "org.apache.skywalking.apm.agent.core.plugin.PluginFinder"))
                .transform(new AgentBuilder.Transformer() {
                    @Override
                    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                            TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) {
                        return builder.method(ElementMatchers.<MethodDescription>named("buildMatch"))
                                .intercept(Advice.to(PluginFinderAdvice.class));
                    }
                })
                .installOn(instrumentation);
        SkyWalkingAgent.premain(agentArgs, instrumentation);
    }

    @Override
    public Map<String, List<EnhanceDefinition>> getNamedEnhancers() {
        if (SkyWalkingAgentLoader.pluginFinder == null) {
            return Collections.emptyMap();
        }
        final Map<?, ?> nameMatchDefineMap = getNameMatchDefineMap();
        if (nameMatchDefineMap.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, List<EnhanceDefinition>> result = new HashMap<String, List<EnhanceDefinition>>();
        for (Map.Entry<?, ?> entry : nameMatchDefineMap.entrySet()) {
            final Object key = entry.getKey();
            if (!(key instanceof String)) {
                continue;
            }
            final Object nameMatchDefineList = entry.getValue();
            if (!(nameMatchDefineList instanceof List)) {
                continue;
            }
            final List<EnhanceDefinition> enhancerList = new ArrayList<EnhanceDefinition>();
            for (Object nameMatchDefine : (List<?>) nameMatchDefineList) {
                if (nameMatchDefine instanceof AbstractClassEnhancePluginDefine) {
                    enhancerList.add(parseEnhancer((AbstractClassEnhancePluginDefine) nameMatchDefine));
                }
            }
            result.put((String) key, enhancerList);
        }
        return result;
    }

    private Map<?, ?> getNameMatchDefineMap() {
        Object result = null;
        try {
            final Class<?> pluginFinder = SkyWalkingAgentLoader.pluginFinder.getClass();
            final Field nameMatchDefine = pluginFinder.getDeclaredField("nameMatchDefine");
            nameMatchDefine.setAccessible(true);
            result = nameMatchDefine.get(SkyWalkingAgentLoader.pluginFinder);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (result instanceof Map) {
            return (Map<?, ?>) result;
        }
        return Collections.emptyMap();
    }

    @Override
    public List<EnhanceDefinition> getNonNamedEnhancers() {
        if (SkyWalkingAgentLoader.pluginFinder == null) {
            return Collections.emptyList();
        }
        final List<?> signatureMatchDefineList = getSignatureMatchDefineList();
        if (signatureMatchDefineList.isEmpty()) {
            return Collections.emptyList();
        }
        final List<EnhanceDefinition> result = new ArrayList<EnhanceDefinition>();
        for (Object signatureMatchDefine : signatureMatchDefineList) {
            if (signatureMatchDefine instanceof AbstractClassEnhancePluginDefine) {
                result.add(parseEnhancer((AbstractClassEnhancePluginDefine) signatureMatchDefine));
            }
        }
        return result;
    }

    @Override
    public Interceptor createInterceptor(Class<?> cls) {
        return null;
    }

    private List<?> getSignatureMatchDefineList() {
        Object result = null;
        try {
            final Class<?> pluginFinder = SkyWalkingAgentLoader.pluginFinder.getClass();
            final Field signatureMatchDefine = pluginFinder.getDeclaredField("signatureMatchDefine");
            signatureMatchDefine.setAccessible(true);
            result = signatureMatchDefine.get(SkyWalkingAgentLoader.pluginFinder);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (result instanceof List) {
            return (List<?>) result;
        }
        return Collections.emptyList();
    }

    private EnhanceDefinition parseEnhancer(AbstractClassEnhancePluginDefine define) {
        return EnhanceDefinitionAdapter.adapter(define);
    }

    public static class PluginFinderAdvice {
        @Advice.OnMethodExit
        public static void methodExit(@Advice.This Object pluginFinder) {
            SkyWalkingAgentLoader.pluginFinder = pluginFinder;
        }
    }
}
