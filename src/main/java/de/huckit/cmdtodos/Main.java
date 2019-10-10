package de.huckit.cmdtodos;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {
    private static List<Todo> todos = new ArrayList<Todo>();
    private static File todoDir = new File(System.getProperty("user.home") + "/.cmdtodos/todos");

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.out.println("\n" + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length == 0) {
            args = new String[]{"ls"};
        }

        String command = args[0];
        final var arguments = Arrays.copyOfRange(args, 1, args.length);


        if (!new File(todoDir.getPath() + "/todos.todo").exists()) {
            //noinspection ResultOfMethodCallIgnored
            todoDir.mkdirs();
        } else {
            try {
                todos = readTodos();
            } catch (Exception e) {
                throw new RuntimeException("> Could not read Todos");
            }
        }

        switch (command.toLowerCase()) {
            case "new":
            case "add":
                commandNew(arguments);
                break;
            case "tick":
                commandTick(arguments);
                break;
            case "untick":
                commandUntick(arguments);
                break;
            case "help":
                commandHelp(arguments);
                break;
            case "ls":
                commandLs(arguments);
                break;
            default:
                throw new RuntimeException("> unexpected command; type \"todo help\" for help");
        }
    }

    //////////////////// COMMANDS ////////////////////////

    private static void commandNew(String[] args) {
        switch (args.length) {
            case 1:
                todos.add(new Todo(args[0], "no description"));
                break;
            case 2:
                todos.add(new Todo(args[0], args[1]));
                break;
            default:
                throw new RuntimeException("> command should be: \"todo new <title> <description>\"");
        }

        writeTodos(todos);
        System.out.println("\n> Todo added successfully");
    }

    private static void commandTick(String[] args) {
        Scanner sc = new Scanner(System.in);

        ArrayList<Todo> values = new ArrayList<>(archive(false));
        ArrayList<Todo> results = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();

        if (args.length == 1) {
            for (int i = 0; i < values.size(); i++) {
                if (args[0].equals(values.get(i).getTitle())) {
                    results.add(values.get(i));
                    indexes.add(i);
                }
            }

            switch (results.size()) {
                case 0:
                    throw new RuntimeException("> could not find Todo (Hint: it's case sensitive\n" +
                                               "> to see which todo can be ticked type: \"todo ls [filter]\"");
                case 1:
                    //tick // untick;
                    break;
                default:
                    boolean run = true;

                    while (run) {
                        run = false;

                        System.out.println("\n> There are more than one Todo with the same name");
                        System.out.println("> Please choose:\n");

                        for (int i = 0; i < results.size(); i++) {
                            System.out.print((i + 1) + "." + results.get(i) + "\n"); // TODO: 10.10.2019 (toString)
                        }

                        System.out.print("> ");
                        int input = Integer.parseInt(sc.nextLine());
                        System.out.println("\n"); // TODO: 10.10.2019  next line or not

                        if ((0 < input) && (input <= results.size())) {
                            //gg
                        }
                        else {                             // RED                             // RESET
                            AnsiConsole.out.println("> " + "\u001B[31m" + "no valid entry" + "\u001B[0m" + "\n");
                            run = true;
                        }
                    }

                    break;
            }
        }
        else {
            throw new RuntimeException("> command should be: \"todo tick <title>\"");
        }

        writeTodos(todos); // TODO: 10.10.2019
    }

    private static void commandUntick(String[] arguments) {
        // TODO: 10.10.2019
    }

    private static void commandHelp(String[] arguments) {
        System.out.println("help");
        // TODO: 10.10.2019
    }

    private static void commandLs(String[] args) {
        switch (args.length) {
            case 0:
                list(archive(false));
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "archive":
                        list(archive(true));
                        break;
                    case "all":
                        list(todos);
                    case "oldtonew":
                        list(oldtonew(archive(false)));
                        break;
                    case "newtoold":
                        list(newtoold(archive(false)));
                        break;
                    case "atoz":
                        list(atoz(archive(false)));
                        break;
                    case "ztoa":
                        list(ztoa(archive(false)));
                        break;
                    default:
                        throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                break;
            case 2:
                if (!args[0].toLowerCase().equals("archive")) {
                    throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                switch (args[0]) {
                    case "archive":
                        switch (args[1].toLowerCase()) {
                            case "oldtonew":
                                list(oldtonew(archive(true)));
                                break;
                            case "newtoold":
                                list(newtoold(archive(true)));
                                break;
                            case "atoz":
                                list(atoz(archive(true)));
                                break;
                            case "ztoa":
                                list(ztoa(archive(true)));
                                break;
                            default:
                                throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                        }

                        break;
                    case "all":
                        switch (args[1].toLowerCase()) {
                            case "oldtonew":
                                list(oldtonew(todos));
                                break;
                            case "newtoold":
                                list(newtoold(todos));
                                break;
                            case "atoz":
                                list(atoz(todos));
                                break;
                            case "ztoa":
                                list(ztoa(todos));
                                break;
                            default:
                                throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                        }

                        break;
                    default:
                        throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                break;
            default:
                throw new RuntimeException("> too many arguments; type \"todo help\" for help");
        }
    }

    private static void list(List<Todo> values) {
        StringBuilder output = new StringBuilder("\n");

        for (Todo todo : values) {
            output.append(todo.forList()).append("\n");
        }

        AnsiConsole.out.print(output);
    }

    private static List<Todo> archive(boolean archive) {
        List<Todo> values = new ArrayList<>();

        for (Todo todo : todos) {
            if (archive) {
                if (todo.isTicked()) {
                    values.add(todo);
                }
            } else {
                if (!todo.isTicked()) {
                    values.add(todo);
                }
            }
        }

        return values;
    }

    ////////////////// WRITE / READ //////////////////////

    private static void writeTodos(List<Todo> values) {
        File file = new File(todoDir.getPath() + "/todos.todo");

        if (file.exists()) {
            try {
                deleteFileOrFolder(Paths.get(file.getPath()));
            } catch (IOException e) {
                throw new RuntimeException("> Could not overwrite files");
            }
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(values);
        } catch (Exception e) {
            throw new RuntimeException("> Todos could not be saved");
        }
    }

    private static ArrayList<Todo> readTodos() throws IOException, ClassNotFoundException {
        ArrayList<Todo> values;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(todoDir.getPath() + "/todos.todo")))) {
            Object obj = ois.readObject();
            values = (ArrayList<Todo>) obj;
        } catch (Exception e) {
            throw new RuntimeException();
        }

        return values;
    }

    private static void deleteFileOrFolder(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return handleException(e);
            }

            private FileVisitResult handleException(final IOException e) {
                e.printStackTrace(); // replace with more robust error handling
                return TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                    throws IOException {
                if (e != null) return handleException(e);
                Files.delete(dir);
                return CONTINUE;
            }
        });
    }

    ///////////////////// ARGUMENTS //////////////////////// todo (sorting doesnt work)

    private static List<Todo> newtoold(List<Todo> values) {
        boolean run = true;

        while (run) {
            run = false;

            for (int i = 0; i < values.size() - 1; i++) {
                if (values.get(i + 1).getDate().isBefore(values.get(i).getDate())) {
                    Todo helper = values.get(i + 1);
                    values.set(i + 1, values.get(i));
                    values.set(i, helper);

                    run = true;
                }
            }
        }

        return values;
    }

    private static List<Todo> oldtonew(List<Todo> values) {
        boolean run = true;

        while (run) {
            run = false;

            for (int i = 0; i < values.size() - 1; i++) {
                if (values.get(i + 1).getDate().isAfter(values.get(i).getDate())) {
                    Todo helper = values.get(i + 1);
                    values.set(i + 1, values.get(i));
                    values.set(i, helper);

                    run = true;
                }
            }
        }

        return values;
    }

    private static List<Todo> atoz(List<Todo> values) {
        boolean run = true;

        while (run) {
            run = false;

            for (int i = 0; i < values.size() - 1; i++) {
                if (values.get(i + 1).getTitle().compareTo(values.get(i).getTitle()) < 0) {
                    Todo helper = values.get(i + 1);
                    values.set(i + 1, values.get(i));
                    values.set(i, helper);

                    run = true;
                }
            }
        }

        return values;
    }

    private static List<Todo> ztoa(List<Todo> values) {
        boolean run = true;

        while (run) {
            run = false;

            for (int i = 0; i < values.size() - 1; i++) {
                if (values.get(i + 1).getTitle().compareTo(values.get(i).getTitle()) > 0) {
                    Todo helper = values.get(i + 1);
                    values.set(i + 1, values.get(i));
                    values.set(i, helper);

                    run = true;
                }
            }
        }

        return values;
    }
}