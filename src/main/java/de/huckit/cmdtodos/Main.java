package de.huckit.cmdtodos;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {
    private static List<Todo> todos = new ArrayList<>();
    private static File todoDir = new File(System.getProperty("user.home") + "/.cmdtodos/todos");

    private static String ANSI_RED = "\u001B[31m";
    private static String ANSI_GREEN = "\u001B[32m";
    private static String ANSI_RESET = "\u001B[0m";


    /*

    public static void main(String[] args) throws JsonProcessingException {
        final var mapper = new ObjectMapper();
        System.out.println(mapper
                .valueToTree(new Todo("Hello", "Hello", 1)));
        final var json = "{\"id\":1,\"title\":\"Hello\",\"ticked\":false,\"date\":{\"year\":2019,\"month\":\"OCTOBER\",\"chronology\":{\"id\":\"ISO\",\"calendarType\":\"iso8601\"},\"era\":\"CE\",\"leapYear\":false,\"dayOfWeek\":\"FRIDAY\",\"dayOfYear\":284,\"monthValue\":10,\"dayOfMonth\":11}}";
        final var jsonNode = mapper.readTree(json);
        final var todo = mapper.treeToValue(jsonNode, Todo.class);


        System.out.println(todo.getTitle());
    }

     */

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.out.println("\n" + e.getMessage());
            System.exit(1);
        }
    } //todo      switch to json     categoriese for todos            reminder/priority/deadline             id starting at 1

    private static void run(String[] args) {
        if (!new File(todoDir.getPath() + "/todos.todo").exists()) {
            //noinspection ResultOfMethodCallIgnored
            todoDir.mkdirs();

            if (args.length == 0) {
                args = new String[]{"help"};
            }
        } else {
            if (args.length > 0) {
                if (args[0].equals("aabbllrr")) {
                    try {
                        deleteFileOrFolder(Paths.get(todoDir.getPath()));
                        System.out.println("\n> reseted");
                    } catch (IOException e) {
                        throw new RuntimeException("> could not reset cmdtodos");
                    }
                }
            }

            try {
                todos = readTodos();
            } catch (Exception e) {
                throw new RuntimeException("> Could not read Todos");
            }
        }

        if (args.length == 0) {
            args = new String[]{"ls"};
        }

        String command = args[0];
        final String[] arguments = Arrays.copyOfRange(args, 1, args.length);

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
                commandDelete(arguments);
                break;
            case "deleteall":
                commandDeleteAll(arguments);
                break;
            case "sort":
                commandSort(arguments);
                break;
            case "edit":
                commandEdit(arguments);
                break;
            default:
                throw new RuntimeException("> unexpected command; type \"todo help\" for help");
        }
    } // todo consolen handle // java native           // enum     // file.delete()

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
        ArrayList<Todo> values = new ArrayList<>(archive(archive));
        ArrayList<Todo> results;

        if (args.length != 1) {
            throw new RuntimeException("> command should be: \"todo " + (archive ? "untick" : "tick") + " <title>\"");
        }

        if (values.size() == 0) {
            throw new RuntimeException("> there are no todos to be " + (archive ? "unticked" : "ticked"));
        }

        results = new ArrayList<>(findTodoByTitle(values, args[0]));

        switch (results.size()) {
            case 0:
                int size = findTodoByTitle(archive(!archive), args[0]).size();

                if (size > 0) {
                    throw new RuntimeException("> " + ((size > 1) ? "todos" : "a todo") + " with the same name " + ((size > 1) ? "are" : "is") + " already " + (archive ? "unticked" : "ticked"));
                }
                throw new RuntimeException("> could not find Todo (Hint: it's case sensitive)\n" +
                        "> to see which todo can be " + (archive ? "unticked" : "ticked") + " type: \"todo ls" + (archive ? " ticked" : "") + " [filter]\"");
            case 1:
                tickAndUntick(results.get(0).getId(), archive);
                break;
            default:
                tickAndUntick(getTodoFromUser(results), archive);
                break;
        }

        writeTodos(todos);
    }

    private static void tickAndUntick(long id, boolean archive) {
        String ticked = ANSI_RED + "X" + ANSI_RESET + " -> " + ANSI_GREEN + "O" + ANSI_RESET, unticked = ANSI_GREEN + "O" + ANSI_RESET + " -> " + ANSI_RED + "X" + ANSI_RESET;

        todos.get(getIndexOfTodo(id)).setTicked(!archive);
        AnsiConsole.out.println("\n> " + (archive ? unticked : ticked));
    }

    private static void commandShow(String[] args) {
        ArrayList<Todo> values;
        StringBuilder output = new StringBuilder("\n");

        if (args.length != 1) {
            throw new RuntimeException("> command should be: todo show <title>");
        }

        values = new ArrayList<>(findTodoByTitle(todos, args[0]));

        for (Todo value : values) {
            output.append(value.toString()).append("\n");
        }

        AnsiConsole.out.print((output.length() > 1) ? output.substring(0, output.length() - 1) : output);
    }

    private static void commandDelete(String[] args) {
        ArrayList<Todo> values;

        if (args.length != 1) {
            throw new RuntimeException("> command should be: todo delete <title>");
        }

        if (todos.size() == 0) {
            throw new RuntimeException("> there are no todos to be deleted");
        }

        values = new ArrayList<>(findTodoByTitle(todos, args[0]));

        switch (values.size()) {
            case 0:
                throw new RuntimeException("> could not find Todo (Hint: it's case sensitive)");
            case 1:
                todos.remove(values.get(0));
                System.out.println("\n> successfully deleted");
                break;
            default:
                todos.remove(getIndexOfTodo(getTodoFromUser(values)));
                System.out.println("\n> successfully deleted");
        }


        writeTodos(todos);
    }

    private static void commandDeleteAll(String[] args) {
        if (args.length > 1) {
            throw new RuntimeException("> command should be: \"todo deleteAll [filter]\"");
        }

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "all":
                    askToDelete("\n> do you really want to delete all todos?");
                    deleteAll();
                    break;
                case "ticked":
                    askToDelete("\n> do you really want to delete all ticked todos?");
                    deleteArguments(true);
                    break;
                case "unticked":
                    askToDelete("\n> do you really want to delete all unticked todos?");
                    deleteArguments(false);
                    break;
                default:
                    throw new RuntimeException("> unexpected argument; type \"todo help\" for help");
            }
        } else {
            askToDelete("\n> do you really want to delete all todos?");
            deleteAll();
        }
    }

    private static void deleteAll() {
        try {
            deleteFileOrFolder(Paths.get(todoDir.getPath()));

            System.out.println("\n> everything deleted");
        } catch (IOException e) {
            throw new RuntimeException("> could not delete everything");
        }
    }

    private static void deleteArguments(boolean archive) {
        todos = new ArrayList<>(archive(!archive));

        writeTodos(todos);
    }

    private static void askToDelete(String question) {
        Scanner sc = new Scanner(System.in);

        System.out.println(question);

        System.out.println("\n> type \"DELETE\" to confirm");
        AnsiConsole.out.print("> " + ANSI_RED);

        String input = sc.nextLine();

        AnsiConsole.out.print(ANSI_RESET);

        if (!input.equals("DELETE")) {
            throw new RuntimeException("> \"DELETE\" was not entered correctly");
        }
    }

    private static void commandSort(String[] args) {
        String exceptionMessage = "> command should be: \"todo sort [category] <filter>\"";
        String unknownArgument = "> unexpected argument; type \"todo help\" for help";

        switch (args.length) {
            case 1:
                todos = filterSelection(todos, args[0].toLowerCase(), true, unknownArgument);
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "unticked":
                        todos = filterSelection(archive(false), args[1].toLowerCase(), false, unknownArgument);
                        break;
                    case "ticked":
                        todos = filterSelection(archive(true), args[1].toLowerCase(), false, unknownArgument);
                        break;
                    case "all":
                        todos = filterSelection(todos, args[1].toLowerCase(), true, unknownArgument);
                        break;
                    default:
                        throw new RuntimeException(unknownArgument);
                }
                break;
            default:
                throw new RuntimeException(exceptionMessage);
        }

        System.out.println();
        System.out.println("> sorted");

        writeTodos(todos);
    }

    private static void commandEdit(String[] args) {
        String message = "> command should be: \"todo edit <\"titleOf\"/\"descriptionOf\"> <title> [edit]\"";
        List<Todo> values;
        int index;

        if (args.length < 2) {
            throw new RuntimeException(message);
        }

        values = findTodoByTitle(todos, args[1]);

        if (values.size() == 0) {
            throw new RuntimeException("> could not find todo");
        }

        switch (args.length) {
            case 2:
                switch (args[0].toLowerCase()) {
                    case "titleof":
                        index = getIndexOfTodo(getTodoFromUser(values)); // line has multiples because of getTodoFromUser which has user interaction
                        todos.get(index).setTitle(userEdit(todos.get(index).getTitle()));
                        break;
                    case "descriptionof":
                        index = getIndexOfTodo(getTodoFromUser(values));
                        todos.get(index).setDescription(userEdit(todos.get(index).getDescription()));
                        break;
                    default:
                        throw new RuntimeException(message);
                }
                break;
            case 3:
                switch (args[0].toLowerCase()) {
                    case "titleof":
                        index = getIndexOfTodo(getTodoFromUser(values));
                        todos.get(index).setTitle(args[2]);
                        break;
                    case "descriptionof":
                        index = getIndexOfTodo(getTodoFromUser(values));
                        todos.get(index).setDescription(args[2]);
                        break;
                    default:
                        throw new RuntimeException(message);
                }

                break;
            default:
                throw new RuntimeException(message);
        }

        System.out.println();
        System.out.println("> edited");

        writeTodos(todos);
    } // TODO: 28.10.2019 make more efficient

    private static String userEdit(String userEdit) {
        //gg
        return "";
    } // TODO: 30.10.2019 finish

    private static void commandHelp(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
                case "new":
                case "add":

                    break;
                case "tick":

                    break;
                default:
                    throw new RuntimeException("unexpected atribute");
            }
        } else {
            throw new RuntimeException("> command should be: todo help [command]");
        }
    } // TODO: 11.10.2019

    private static void commandLs(String[] args) {
        String unexpectedArgument = "> unexpected argument; type \"todo help\" for help";
        switch (args.length) {
            case 0:
                list(archive(false));
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "unticked":
                        list(archive(false));
                        break;
                    case "ticked":
                        list(archive(true));
                        break;
                    case "all":
                        list(todos);
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
                        throw new RuntimeException(unexpectedArgument);
                }

                break;
            case 2:
                switch (args[0]) {
                    case "ticked":
                        list(filterSelection(archive(true), args[1].toLowerCase(), false, unexpectedArgument));
                        break;
                    case "all":
                        list(filterSelection(todos, args[1].toLowerCase(), true, unexpectedArgument));
                        break;
                    case "unticked":
                        list(filterSelection(archive(false), args[1].toLowerCase(), false, unexpectedArgument));
                        break;
                    default:
                        throw new RuntimeException(unexpectedArgument);
                }

                break;
            default:
                throw new RuntimeException("> command should be: \"todo ls [category] [filter]\"");
        }
    }

    private static void list(List<Todo> values) {
        StringBuilder output = new StringBuilder("\n");

        for (Todo todo : values) {
            output.append(todo.forList()).append("\n");
        }

        AnsiConsole.out.print(output);
    }

    ////////////////////// LOGIC ///////////////////////////

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

    private static int getIndexOfTodo(long id) {
        for (int i = 0; i < todos.size(); i++) {
            if (todos.get(i).getId() == id) {
                return i;
            }
        }

        return -1;
    }

    private static List<Todo> findTodoByTitle(List<Todo> input, String title) {
        List<Todo> values = new ArrayList<>();

        for (Todo todo : input) {
            if (todo.getTitle().equals(title)) {
                values.add(todo);
            }
        }

        return values;
    }

    private static long getTodoFromUser(List<Todo> results) {
    Scanner sc = new Scanner(System.in);

    long number = 0;
    boolean run = true;

        if (results.size() == 1) {
        return results.get(0).getId();
    }

        while (run) {
        System.out.println("\n> there are more than one todo with the same name");
        System.out.println("> please choose:\n");

        for (Todo todo : results) {
            AnsiConsole.out.println(todo);
        }

        System.out.println("> enter ID");
        System.out.print("> ");
        String input = (sc.nextLine());

        if (input.equals("exit")) {
            throw new RuntimeException("\n> exited\n");
        }

        try {
            number = Long.parseLong(input);
        } catch (Exception e) {
            number = 0;
        }

        for (Todo result : results) {
            if (number == result.getId()) {
                run = false;
                break;
            }
        }

        if (run) {
            System.out.println();
            AnsiConsole.out.println("> " + ANSI_RED + "no valid entry" + ANSI_RESET);
        }
    }

        return number;
} // Only used in case more than one t0do is available

    private static List<Todo> filterSelection(List<Todo> values, String argument, boolean includeTickedFilters, String defaultMessage) {
        if (includeTickedFilters) {
            switch (argument.toLowerCase()) {
                case "newtoold":
                    values = newtoold(values);
                    break;
                case "oldtonew":
                    values = oldtonew(values);
                    break;
                case "atoz":
                    values = atoz(values);
                    break;
                case "ztoa":
                    values = ztoa(values);
                    break;
                case "tickedtounticked":
                    values = tickedtounticked(values);
                    break;
                case "untickedtoticked":
                    values = untickedtoticked(values);
                    break;
                default:
                    throw new RuntimeException(defaultMessage);
            }
        } else {
            switch (argument.toLowerCase()) {
                case "newtoold":
                    values = newtoold(values);
                    break;
                case "oldtonew":
                    values = oldtonew(values);
                    break;
                case "atoz":
                    values = atoz(values);
                    break;
                case "ztoa":
                    values = ztoa(values);
                    break;
                default:
                    throw new RuntimeException(defaultMessage);
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

    private static ArrayList<Todo> readTodos() {
        ArrayList<Todo> values;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(todoDir.getPath() + "/todos.todo")))) {
            Object obj = ois.readObject();
            //noinspection unchecked
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
        } else {
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
    }

    private static void deleteFileOrFolder(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
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

    ///////////////////// FILTERS ////////////////////////

    private static List<Todo> newtoold(List<Todo> values) {
        boolean run = true;

        while (run) {
            run = false;

            for (int i = 0; i < values.size() - 1; i++) {
                if (values.get(i + 1).getDate() > values.get(i).getDate()) {
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

                if (values.get(i + 1).getDate() < values.get(i).getDate()) {
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
                if (values.get(i + 1).getTitle().toLowerCase().compareTo(values.get(i).getTitle().toLowerCase()) < 0) {
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
                if (values.get(i + 1).getTitle().toLowerCase().compareTo(values.get(i).getTitle().toLowerCase()) > 0) {
                    Todo helper = values.get(i + 1);
                    values.set(i + 1, values.get(i));
                    values.set(i, helper);

                    run = true;
                }
            }
        }

        return values;
    }

    private static List<Todo> tickedtounticked(List<Todo> values) {
        List<Todo> ticked = new ArrayList<>();
        List<Todo> unticked = new ArrayList<>();

        for (Todo todo : values) {
            if (todo.isTicked()) {
                ticked.add(todo);
            }

            if (!todo.isTicked()) {
                unticked.add(todo);
            }

        }

        ticked.addAll(unticked);

        return ticked;
    }

    private static List<Todo> untickedtoticked(List<Todo> values) {
        List<Todo> ticked = new ArrayList<>();
        List<Todo> unticked = new ArrayList<>();

        for (Todo todo : values) {
            if (todo.isTicked()) {
                ticked.add(todo);
            }

            if (!todo.isTicked()) {
                unticked.add(todo);
            }

        }

        unticked.addAll(ticked);

        return unticked;
    }
}