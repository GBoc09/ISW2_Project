package org.example.models;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
/** With this class our intent is to create the object VERSION, whit its attributes
 * Each version has an ID, a name and a creation date. */
public class Version {

    private String id;
    private int index;
    private String name;
    private LocalDate date;

    public Version(String id, String name, int index, @NotNull LocalDate date){

        this.id = id;
        this.name = name;
        this.index = index;
        this.date = date;
    }

    public Version(String id, String name, @NotNull LocalDate date){
        this.id = id;
        this.name = name;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
