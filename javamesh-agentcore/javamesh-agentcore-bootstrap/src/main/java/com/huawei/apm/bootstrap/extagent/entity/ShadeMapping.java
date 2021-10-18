package com.huawei.apm.bootstrap.extagent.entity;

public class ShadeMapping {
    private String sourcePattern;
    private String targetPattern;
    private String sourcePathPattern;
    private String targetPathPattern;

    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    public void setTargetPattern(String targetPattern) {
        this.targetPattern = targetPattern;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public String getTargetPattern() {
        return targetPattern;
    }

    public String getSourcePathPattern() {
        if (sourcePathPattern == null) {
            sourcePathPattern = sourcePattern.replace('.', '/');
        }
        return sourcePathPattern;
    }

    public String getTargetPathPattern() {
        if (targetPathPattern == null) {
            targetPathPattern = targetPattern.replace('.', '/');
        }
        return targetPathPattern;
    }

    public String relocateClass(String className) {
        if (className.startsWith(sourcePattern)) {
            return className.replaceFirst(sourcePattern, targetPattern);
        }
        return className;
    }

    public String relocatePath(String classPath) {
        if (classPath.startsWith(getSourcePathPattern())) {
            return classPath.replaceFirst(getSourcePathPattern(), getTargetPathPattern());
        }
        return classPath;
    }
}
