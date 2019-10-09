package de.huckit.cmdtodos;


import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {
    private static List<Todo> todos = new ArrayList<Todo>();
    private static String appdata = System.getenv("APPDATA");
    private static File todoDir = new File(appdata + "\\cmdtodos\\todos");

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"ls"};
        }

        String command = args[0];
        final var arguments = Arrays.copyOfRange(args, 1, args.length);


        if (!todoDir.exists()) {
            todoDir.mkdirs();
        } else {
            try {
                todos = readTodos();
            } catch (Exception e) {
                System.out.println("error");
                return;
            }
        }

        switch (command.toLowerCase()) {
            case "new":
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
                throw new RuntimeException("unexpected command; type \"todo help\" for help");
        }

        writeTodos(todos.toArray(new Todo[0]));
    }

    private static void commandUntick(String[] arguments) {
    }

    private static void commandHelp(String[] arguments) {
    }

    private static void commandLs(String[] args) {
        switch (args.length) {
            case 0:
                list(false, todos);
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "archive":
                        list(true, todos);
                        break;
                    case "oldtonew":                                 //May not be efficient to sort the list and then to decide whether to use the archive or not
                        list(false, oldtonew());
                        break;
                    case "newtoold":
                        list(false, newtoold());
                        break;
                    case "atoz":
                        list(false, atoz());
                        break;
                    case "ztoa":
                        list(false, ztoa());
                        break;
                    default:
                        throw new RuntimeException("unexpected argument; type \"todo help\" for help");
                }

                break;
            case 2:
                if (!args[0].toLowerCase().equals("archive")) {
                    throw new RuntimeException("unexpected argument; type \"todo help\" for help");
                }

                switch (args[1].toLowerCase()) {
                    case "oldtonew":
                        list(true, oldtonew());
                        break;
                    case "newtoold":
                        list(true, newtoold());
                        break;
                    case "atoz":
                        list(true, atoz());
                        break;
                    case "ztoa":
                        list(true, ztoa());
                        break;
                    default:
                        throw new RuntimeException("unexpected argument; type \"todo help\" for help");
                }

                break;
        }
    }

    private static void list(boolean archive, List<Todo> values) {
        String output = "";

        for (Todo todo : values) {
            if (archive ^ !todo.isTicked()) {
                output += todo.toString() + "\n";
            }
        }

        System.out.println(output);
    }

    private static void commandTick(String[] args) {
        //// TODO: 08.10.2019
    }

    private static void commandNew(String[] args) {
        switch (args.length) {
            case 1:
                todos.add(new Todo(args[0], "no description"));
                break;
            case 2:
                todos.add(new Todo(args[0], args[1]));
                break;
            default:
                throw new RuntimeException("command should be: \"todo new <title> <description>\"");
        }
    }

    private static void writeTodos(Serializable[] todos) {
        try {
            deleteFileOrFolder(Paths.get(todoDir.getPath()));
            todoDir.mkdirs();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't delete files");
        }

        for (int i = 0; i < todos.length; i++) {
            File file = new File(todoDir.getPath() + "\\" + i + ".todo");

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))){
                oos.writeObject(todos[i]);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Todo not saved");
            }
        }
    }

    private static ArrayList<Todo> readTodos() throws IOException, ClassNotFoundException {
        ArrayList<Todo> todos = new ArrayList<Todo>();
        int length = Objects.requireNonNull(new File(appdata + "\\cmdtodos\\todos").listFiles()).length;

        for (int i = 0; i < length; i++) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(appdata + "\\cmdtodos\\todos\\" + i + ".todo")))) {
                Object obj = ois.readObject();
                todos.add((Todo) obj);
            } catch (Exception e) {
                System.out.println("catch bei readTodos()");
                e.printStackTrace();
            }
        }

        return todos;
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

    ;

    private static List<Todo> newtoold() {
        //
        return todos;
    }

    private static List<Todo> oldtonew() {
        //
        return todos;
    }

    private static List<Todo> atoz() {
        List<Todo> values = new ArrayList<>(todos);
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

    private static List<Todo> ztoa() {
        //
        return todos;
    }
}