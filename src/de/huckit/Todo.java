package de.huckit;

import java.io.Serializable;
import java.time.LocalDate;

public class Todo implements Serializable {
    private String title, description;
    private boolean ticked = false;
    private LocalDate date = LocalDate.now();

    public Todo(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public boolean isTicked() {
        return ticked;
    }

    public void setTicked(boolean ticked) {
        this.ticked = ticked;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }
}