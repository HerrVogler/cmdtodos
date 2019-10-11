package de.huckit.cmdtodos;

import java.io.Serializable;
import java.time.LocalDate;

public class Todo implements Serializable {
    private String title, description;
    private boolean ticked = false;
    private LocalDate date = LocalDate.now();
    private final long id;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public Todo(String title, String description, long id) {
        this.title = title;
        this.description = description;
        this.id = id;
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
        return " - Title: " + title + " (" + id + ")" + "\n" +
               "   Description: " + description + "\n" +
               "   Date created: " + date.toString() + "\n" +
               "   Done: " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + "\n";
    }

    public String forList() {
        return "> " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + title;
    }

    public String forListID() {
        return "> " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + id + " - " + title;
    }

    public long getID() {
        return id;
    }
}
