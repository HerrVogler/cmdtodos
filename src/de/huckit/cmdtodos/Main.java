package de.huckit.cmdtodos;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {
    private static List<Todo> todos = new ArrayList<Todo>();
    private static File todoDir = new File(System.getProperty("user.home") + "/.cmdtodos/todos");

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }

        String command = args[0];
        final var arguments = Arrays.copyOfRange(args, 1, args.length);


        if (!new File(todoDir.getPath() + "\\todos.todo").exists()) {
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
        System.out.println("> Todo added successfully");
    }

    private static void commandTick(String[] args) {
        //// TODO: 08.10.2019
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
                list(false, todos);
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "archive":
                        list(true, todos);
                        break;
                    case "oldtonew":                                 //May not be efficient to sort the list and then to decide whether to use the archive or not
                        list(false, oldtonew(todos));
                        break;
                    case "newtoold":
                        list(false, newtoold(todos));
                        break;
                    case "atoz":
                        list(false, atoz(todos));
                        break;
                    case "ztoa":
                        list(false, ztoa(todos));
                        break;
                    default:
                        throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                break;
            case 2:
                if (!args[0].toLowerCase().equals("archive")) {
                    throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                switch (args[1].toLowerCase()) {
                    case "oldtonew":
                        list(true, oldtonew(todos));
                        break;
                    case "newtoold":
                        list(true, newtoold(todos));
                        break;
                    case "atoz":
                        list(true, atoz(todos));
                        break;
                    case "ztoa":
                        list(true, ztoa(todos));
                        break;
                    default:
                        throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
                }

                break;
            default:
                throw new RuntimeException("> too many arguments; type \"todo help\" for help");
        }
    }

    private static void list(boolean archive, List<Todo> values) {
        String output = "\n";

        for (Todo todo : values) {
            if (archive ^ !todo.isTicked()) {
                output += todo.toString() + "\n";
            }
        }

        System.out.println(output);
    }

    ////////////////// WRITE / READ //////////////////////

    private static void writeTodos(List<Todo> values) {
        File file = new File(todoDir.getPath() + "\\todos.todo");

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

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(todoDir.getPath() + "\\todos.todo")))) {
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