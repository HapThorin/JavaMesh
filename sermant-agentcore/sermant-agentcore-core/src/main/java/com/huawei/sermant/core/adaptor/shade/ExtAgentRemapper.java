/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 */

package com.huawei.sermant.core.adaptor.shade;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.commons.Remapper;

import com.huawei.sermant.core.adaptor.shade.mapping.ShadeMapping;

/**
 * 自定义Remapper，用于修正全限定名和路径
 *
 * @author HapThorin
 * @version 1.0.0
 * @since 2021/10/18
 */
public class ExtAgentRemapper extends Remapper {
    /**
     * 类的匹配格式
     */
    private final Pattern classPattern = Pattern.compile("(\\[*)?L(.+);");
    /**
     * 全限定名修正的mapping集
     */
    private final List<ShadeMapping> shadeMappings;

    public ExtAgentRemapper(List<ShadeMapping> shadeMappings) {
        this.shadeMappings = shadeMappings;
    }

    /**
     * 修正字符串型字段，需要修正全限定名和路径名
     *
     * @param value 字段值
     * @return 修正后结果
     */
    @Override
    public Object mapValue(Object value) {
        if (value instanceof String) {
            String prefix = "";
            String suffix = "";
            String fieldValStr = (String) value;
            final Matcher matcher = classPattern.matcher((String) value);
            if (matcher.matches()) {
                prefix = matcher.group(1) + "L";
                suffix = ";";
                fieldValStr = matcher.group(2);
            }
            for (ShadeMapping shadeMapping : shadeMappings) {
                if (fieldValStr.startsWith(shadeMapping.getSourcePattern())) {
                    return prefix + shadeMapping.relocateClass(fieldValStr) + suffix;
                } else if (fieldValStr.startsWith(shadeMapping.getSourcePathPattern())) {
                    return prefix + shadeMapping.relocatePath(fieldValStr) + suffix;
                }
            }
            return value;
        }
        return super.mapValue(value);
    }

    /**
     * 修正类型
     *
     * @param internalName 类型名称
     * @return 修正后的类型
     */
    @Override
    public String map(String internalName) {
        String prefix = "";
        String suffix = "";
        String jarStr = internalName;
        final Matcher matcher = classPattern.matcher(internalName);
        if (matcher.matches()) {
            prefix = matcher.group(1) + "L";
            suffix = ";";
            jarStr = matcher.group(2);
        }
        for (ShadeMapping shadeMapping : shadeMappings) {
            if (jarStr.startsWith(shadeMapping.getSourcePathPattern())) {
                return prefix + shadeMapping.relocatePath(jarStr) + suffix;
            }
        }
        return internalName;
    }
}
