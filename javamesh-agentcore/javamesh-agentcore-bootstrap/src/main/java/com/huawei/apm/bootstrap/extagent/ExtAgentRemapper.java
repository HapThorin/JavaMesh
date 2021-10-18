package com.huawei.apm.bootstrap.extagent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.commons.Remapper;

import com.huawei.apm.bootstrap.extagent.entity.ShadeMapping;

public class ExtAgentRemapper extends Remapper {
    private final Pattern classPattern = Pattern.compile("(\\[*)?L(.+);");
    private final List<ShadeMapping> shadeMappings;

    public ExtAgentRemapper(List<ShadeMapping> shadeMappings) {
        this.shadeMappings = shadeMappings;
    }

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
