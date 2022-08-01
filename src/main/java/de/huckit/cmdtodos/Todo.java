package de.huckit.cmdtodos;

import java.io.Serializable;
import java.util.Date;

public class Todo implements Serializable {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    private long id;
    private String title, description;
    private boolean ticked;
    private long date;

    public Todo(long id, String title, String description, boolean ticked, long date) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.ticked = ticked;
        this.date = date;
    }

    public Todo(long id, String title, String description) {
        this(id, title, description, false, System.currentTimeMillis());
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
                "   Status: " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET));
    }

    public String forList() {
        return " " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + title + shortenDescription();
    }

    public String forListID() {
        return " " + (ticked ? (ANSI_GREEN + "O" + ANSI_RESET) : (ANSI_RED + "X" + ANSI_RESET)) + " - " + id + " - " + title + shortenDescription();
    }

    private String shortenDescription() {
        if (description.equals("no description")) {
            return "";
        }

        if (description.length() > 50) {
            return " - " + description.substring(0, 50).stripTrailing() + "...";
        }

        return " - " + description;
    }
}
