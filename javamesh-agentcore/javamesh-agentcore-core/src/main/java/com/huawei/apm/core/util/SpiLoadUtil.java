/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.apm.core.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ServiceLoader;

/**
 * 插件spi加载工具
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/11/16
 */
public class SpiLoadUtil {
    /**
     * 依权重获取最优实现
     *
     * @param clazz spi目标类
     * @param <T>   目标类型
     * @return 最优实现
     */
    public static <T> T getImpl(Class<T> clazz) {
        return getImpl(clazz, ClassLoader.getSystemClassLoader());
    }

    /**
     * 依权重获取最优实现 、
     *
     * @param clazz       spi目标类
     * @param classLoader 查找的ClassLoader
     * @return 最优实现
     * @return 最优实现
     */
    public static <T> T getImpl(Class<T> clazz, ClassLoader classLoader) {
        T impl = null;
        for (T newImpl : ServiceLoader.load(clazz, classLoader)) {
            impl = compare(impl, newImpl) ? newImpl : impl;
        }
        return impl;
    }

    /**
     * 比较权重，返回真时取后者，否则取前者
     *
     * @param source 比较源
     * @param target 比较目标
     * @param <T>    类型
     * @return 返回真时取后者，否则取前者
     */
    public static <T> boolean compare(T source, T target) {
        if (target == null) {
            return false;
        } else if (source == null) {
            return true;
        } else {
            final SpiWeight sourceWeight = source.getClass().getAnnotation(SpiWeight.class);
            final SpiWeight targetWeight = target.getClass().getAnnotation(SpiWeight.class);
            if (targetWeight == null) {
                return false;
            } else if (sourceWeight == null) {
                return true;
            } else {
                return sourceWeight.value() < targetWeight.value();
            }
        }
    }

    /**
     * spi权重
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SpiWeight {
        int value() default 0;
    }
}
