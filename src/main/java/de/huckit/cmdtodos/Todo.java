package de.huckit.cmdtodos;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

public class Todo implements Serializable {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    private long id;
    private String title, description;
    private boolean ticked = false;
    private long date = System.currentTimeMillis();


    public Todo(String title, String description, long id) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public Todo(long id, String title, String description, boolean ticked, long date) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ticked = ticked;
        this.date = date;
    }

    public Todo() {
    }

    public long getId() {
        return id;
    }

    public Todo setId(long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Todo setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Todo setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isTicked() {
        return ticked;
    }

    public Todo setTicked(boolean ticked) {
        this.ticked = ticked;
        return this;
    }

    public long getDate() {
        return date;
    }

    public Todo setDate(long date) {
        this.date = date;
        return this;
    }

    public String toString() {
        return " - Title: " + title + " (" + id + ")" + "\n" +
                "   Description: " + description + "\n" +
                "   Date created: " + new Date(this.date) + "\n" +
                "   Done: " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + "\n";
    }

    public String forList() {
        return "> " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + title;
    }

    public String forListID() {
        return "> " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + id + " - " + title;
    }
}
