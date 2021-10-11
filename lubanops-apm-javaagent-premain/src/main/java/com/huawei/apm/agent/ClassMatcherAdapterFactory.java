/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.agent;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.IndirectMatch;
import org.apache.skywalking.apm.agent.core.plugin.match.NameMatch;

import com.huawei.apm.bootstrap.matcher.ClassMatcher;
import com.huawei.apm.bootstrap.matcher.NameMatcher;
import com.huawei.apm.bootstrap.matcher.NonNameMatcher;

/**
 * <p>提供一个静态方法{@link ClassMatcherAdapterFactory#adapter(ClassMatch)},用于将skyWalking中的 {@link ClassMatch} 转换成 {@link ClassMatcher}</p>
 *
 * @author y30010171
 * @since 2021-09-28
 **/
public class ClassMatcherAdapterFactory {

    /**
     * 将{@link ClassMatch} 转换为 {@link ClassMatcher}
     *
     * @param classMatch 待适配的{@link ClassMatch}
     * @return {@link ClassMatcher}
     */
    public static ClassMatcher adapter(ClassMatch classMatch) {
        if (classMatch instanceof NameMatch) {
            NameMatch nameMatch = (NameMatch) classMatch;
            return new NameMatcher(nameMatch.getClassName());
        }
        if (classMatch instanceof IndirectMatch) {
            return new NonNameMatcherAdapter((IndirectMatch) classMatch);
        }
        return new NameMatcher("");
    }

    static class NonNameMatcherAdapter implements NonNameMatcher {

        private final IndirectMatch indirectMatch;

        private NonNameMatcherAdapter(IndirectMatch indirectMatch) {
            this.indirectMatch = indirectMatch;
        }

        @Override
        public ElementMatcher.Junction<TypeDescription> buildJunction() {
            return indirectMatch.buildJunction();
        }

        @Override
        public boolean isMatch(TypeDescription typeDescription) {
            return indirectMatch.isMatch(typeDescription);
        }
    }
}
