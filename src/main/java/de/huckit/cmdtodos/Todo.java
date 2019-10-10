package de.huckit.cmdtodos;

import java.io.Serializable;
import java.time.LocalDate;

public class Todo implements Serializable {
    private String title, description;
    private boolean ticked = false;
    private LocalDate date = LocalDate.now();

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

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

    public String toString() {
        return " - Title: " + title + "\n" +
               "   Description: " + description + "\n" +
               "   Date created: " + date.toString() + "\n" +
               "   Done: " + (ticked ? "yes" : "no") + "\n";
    }

    public String forList() {
        return "> " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " " + title;
    }
}
