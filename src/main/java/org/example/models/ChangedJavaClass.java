package org.example.models;

public class ChangedJavaClass {
    private final String javaClassName;
    private final String changeType;


    public ChangedJavaClass(String javaClass, String changeType) {
        this.javaClassName = javaClass;
        this.changeType = changeType;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public String getChangeType() {
        return changeType;
    }
}
