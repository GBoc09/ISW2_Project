package org.example.models;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

/** With this class our intent is to create the object VERSION, whit its attributes
 * Each version has an ID, a name and a creation date. */
public class Version {
    String id;
    int index;
    String name;
    LocalDate date;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Version(String id, String name, @NotNull LocalDate date) {
        this.id = id;
        this.name = name;
        this.date = date;
    }
}