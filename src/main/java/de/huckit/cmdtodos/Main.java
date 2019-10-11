package de.huckit.cmdtodos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.fusesource.jansi.AnsiConsole;

import javax.management.relation.RoleUnresolved;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {
    private static List<Todo> todos = new ArrayList<Todo>();
    private static File todoDir = new File(System.getProperty("user.home") + "/.cmdtodos/todos");

    private static String ANSI_RED = "\u001B[31m";
    private static String ANSI_GREEN = "\u001B[32m";
    private static String ANSI_RESET = "\u001B[0m";

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
                commandTickAndUntick(arguments, false);
                break;
            case "untick":
                commandTickAndUntick(arguments, true);
                break;
            case "help":
                commandHelp(arguments);
                break;
            case "ls":
                commandLs(arguments);
                break;
            case "show":
                commandShow(arguments);
                break;
            case "delete":

                break;
            case "deleteAll":
                commandDeleteAll();
                break;
            default:
                throw new RuntimeException("> unexpected command; type \"todo help\" for help");
        }
    }

    //////////////////// COMMANDS ////////////////////////

    private static void commandNew(String[] args) {
        switch (args.length) {
            case 1:
                todos.add(new Todo(args[0], "no description", createID()));
                break;
            case 2:
                todos.add(new Todo(args[0], args[1], createID()));
                break;
            default:
                throw new RuntimeException("> command should be: \"todo new <title> <description>\"");
        }

        writeTodos(todos);
        System.out.println("\n> Todo added successfully");
    }

    private static void commandTickAndUntick(String[] args, boolean archive) {
        Scanner sc = new Scanner(System.in);

        ArrayList<Todo> values = new ArrayList<>(archive(archive));
        ArrayList<Todo> results = new ArrayList<>();

        if (values.size() == 0) {
            throw new RuntimeException("> there are no Todos to be " + (archive ? "unticked" : "ticked"));
        }

        if (args.length == 1) {
            for (Todo value : values) {
                if (args[0].equals(value.getTitle())) {
                    results.add(value);
                }
            }

            switch (results.size()) {
                case 0:
                    throw new RuntimeException("> could not find Todo (Hint: it's case sensitive)\n" +
                                               "> to see which todo can be " + (archive ? "unticked" : "ticked") + " type: \"todo ls" + (archive ? " archive" : "") + " [filter]\"");
                case 1:
                    tickAndUntick(results.get(0).getID(), archive);

                    break;
                default:
                    boolean run = true;

                    while (run) {
                        run = false;

                        System.out.println("\n> there are more than one Todo with the same name");
                        System.out.println("> please choose:\n");

                        for (Todo todo : results) {
                            AnsiConsole.out.println(todo.toString());
                        }

                        System.out.println("> enter ID");
                        System.out.print("> ");
                        String input = (sc.nextLine());

                        if (input.equals("exit")) {
                            throw new RuntimeException("");
                        }

                        long number = Long.parseLong(input);

                        boolean wrong = true;
                        for (Todo result : results) {
                            if (number == result.getID()) {
                                tickAndUntick(number, archive);
                                wrong = false;
                            }
                        }

                        if (wrong) {
                            System.out.println();
                            AnsiConsole.out.println("> " + ANSI_RED + "no valid entry" + ANSI_RESET);
                            run = true;
                        }
                    }

                    break;
            }
        }
        else {
            throw new RuntimeException("> command should be: \"todo " + (archive ? "untick" : "tick") + " <title>\"");
        }

        writeTodos(todos);
    }

    private static void commandShow(String[] args) {
        ArrayList<Todo> values;

        if (args.length == 1) {
            values = new ArrayList<>(findTodoByTitle(todos, args[0]));

            System.out.println();

            for (Todo value : values) {
                AnsiConsole.out.println(value.toString());
            }
        } else {
            throw new RuntimeException("> command should be: todo show <title>");
        }
    }

    private static void commandDelete() {
        // TODO: 11.10.2019
    }

    private static void commandDeleteAll(String[] args) {
        Scanner sc = new Scanner(System.in);

        if (args.length > 0) {
            throw new RuntimeException("> command should be: \"todo deleteAll\"");
        }

        System.out.println("> do you really want to delete all Todos?");
        System.out.println("> type \"DELETE\" to confirm");
        System.out.print("> ");

        if (sc.nextLine().equals("DELETE")) {
            try {
                deleteFileOrFolder(Paths.get(todoDir.getPath()));
            } catch (IOException e) {
                throw new RuntimeException("> could not delete everything");
            }
        }
        else {
            throw new RuntimeException("> \"DELETE\" was not entered correctly");
        }
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
                        list(all());
                        break;
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
                                list(oldtonew(all()));
                                break;
                            case "newtoold":
                                list(newtoold(all()));
                                break;
                            case "atoz":
                                list(atoz(all()));
                                break;
                            case "ztoa":
                                list(ztoa(all()));
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

    private static List<Todo> all() {
        ArrayList<Todo> values = new ArrayList<>(archive(false));

        values.addAll(archive(true));

        return values;
    }

    private static int getIndexOfTodo(long id) {
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).getID() == id) {
                return i;
            }
        }

        return -1;
    }

    private static List<Todo> findTodoByTitle(List<Todo> input, String title) {
        ArrayList<Todo> values = new ArrayList<>();

        for (Todo todo : input) {
            if (todo.getTitle().equals(title)) {
                values.add(todo);
            }
        }

        return values;
    }

    private static void tickAndUntick(long id, boolean archive) {
        String ticked = "(" + ANSI_RED + "X" + ANSI_RESET + " -> " + ANSI_GREEN + "O" + ANSI_RESET + ")", unticked = "(" + ANSI_GREEN + "O" + ANSI_RESET + " -> " + ANSI_RED + "X" + ANSI_RESET + ")";

        todos.get(getIndexOfTodo(id)).setTicked(!archive);
        AnsiConsole.out.println("\n> " + (archive ? unticked : ticked));
    }

    private static void thereAreMoreTodos() {
        // TODO: 11.10.2019
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

    private static long createID() {
        long number;
        File file = new File(todoDir + "/id.todo");

        if (!file.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();

                number = 1000;
            } catch (IOException e) {
                throw new RuntimeException("error");
            }
        }
        else {
             try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                 number = Long.parseLong(reader.readLine()) + 1;
             } catch (IOException e) {
                throw new RuntimeException("error at else bufferedReader create ID");
            }
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write("" + number);
        } catch (IOException e) {
            throw new RuntimeException("error at bufferedWriter create ID");
        }

        return number;
        // TODO: 11.10.2019
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

    ///////////////////// ARGUMENTS ////////////////////////

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
    } // TODO: 11.10.2019 should include time

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