package de.huckit;

import java.util.Arrays;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String command = args[0];

        switch (command) {
            case "new":
                commandNew(args);
            break;
            default:
                throw new RuntimeException("Unexpected command");
        }
    }

    private static void commandNew(String[] args) {

    }
}
