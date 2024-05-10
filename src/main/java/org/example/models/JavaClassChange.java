package org.example.models;

import org.eclipse.jgit.diff.DiffEntry;

public class JavaClassChange {
    private JavaClass javaClass;
    private DiffEntry.ChangeType changeType;

    public JavaClassChange(JavaClass javaClass, DiffEntry.ChangeType changeType) {
        this.javaClass = javaClass;
        this.changeType = changeType;
    }
}
