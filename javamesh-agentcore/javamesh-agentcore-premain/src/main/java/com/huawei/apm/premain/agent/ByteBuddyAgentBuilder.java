package com.huawei.apm.premain.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * 插件增强Builder
 */
public class ByteBuddyAgentBuilder {
    public static void initialize(Instrumentation instrumentation) {
        EnhanceDefinitionLoader loader = EnhanceDefinitionLoader.INSTANCE;
        new AgentBuilder.Default(new ByteBuddy())
            .ignore(BuilderHelpers.ignoreClass())
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .type(loader.buildMatch())
            .transform(new Transformer(loader))
            .with(new LoadListener())
            .installOn(instrumentation);
    }
}
