package de.huckit.cmdtodos;

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
    private static final File todoDir = new File(System.getProperty("user.home") + "/.cmdtodos/todos");

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";


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
        System.out.println();

        try {
            run(args);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
    } //ideas: switch to json, categories for todos, reminder/priority/deadline, id starting at 1, consolen handle, java native, enum, file.delete()

    private static void run(String[] args) {
        if (!new File(todoDir.getPath() + "/todos.todo").exists()) {
            //noinspection ResultOfMethodCallIgnored
            todoDir.mkdirs();

            if (args.length == 0) {
                args = new String[]{"help"};
            }
        } else {
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
            case "help":
            default:
                commandHelp(arguments);
                break;
        }
    }

    //////////////////// COMMANDS ////////////////////////

    private static void commandNew(String[] args) {
        switch (args.length) {
            case 1:
                todos.add(new Todo(createID(), args[0], "no description"));
                break;
            case 2:
                todos.add(new Todo(createID(), args[0], args[1]));
                break;
            default:
                throw new RuntimeException("> command should be: todo new <title> <description>");
        }

        writeTodos(todos);
        System.out.println("> Todo added successfully");
    }

    private static void commandTickAndUntick(String[] args, boolean archive) {
        ArrayList<Todo> values = new ArrayList<>(getTickedOrUnticked(archive));
        ArrayList<Todo> results;

        if (args.length != 1) {
            throw new RuntimeException("> command should be: todo " + (archive ? "untick" : "tick") + " <title>");
        }

        if (values.size() == 0) {
            throw new RuntimeException("> there are no todos to be " + (archive ? "unticked" : "ticked"));
        }

        results = new ArrayList<>(findTodoByTitle(values, args[0]));

        switch (results.size()) {
            case 0:
                int size = findTodoByTitle(getTickedOrUnticked(!archive), args[0]).size();

                if (size > 0) {
                    throw new RuntimeException("> " + ((size > 1) ? "todos" : "a todo") + " with the same name " + ((size > 1) ? "are" : "is") + " already " + (archive ? "unticked" : "ticked"));
                }
                throw new RuntimeException("> could not find Todo (Hint: it's case sensitive)\n" +
                        "> to see which todo can be " + (archive ? "unticked" : "ticked") + " type: todo ls" + (archive ? " ticked" : "") + " [filter]");
            case 1:
                tickAndUntick(results.get(0).getId(), archive);
                break;
            default:
                tickAndUntick(selectFromMultipleTodosDialog(results), archive);
                break;
        }

        writeTodos(todos);
    }

    private static void tickAndUntick(long id, boolean archive) {
        String ticked = ANSI_RED + "X" + ANSI_RESET + " -> " + ANSI_GREEN + "O" + ANSI_RESET;
        String unticked = ANSI_GREEN + "O" + ANSI_RESET + " -> " + ANSI_RED + "X" + ANSI_RESET;

        todos.get(getIndexOfTodo(id)).setTicked(!archive);
        AnsiConsole.out.println("> " + (archive ? unticked : ticked));
    }

    private static void commandShow(String[] args) {
        ArrayList<Todo> values;
        StringBuilder output = new StringBuilder();

        if (args.length != 1) {
            throw new RuntimeException("> command should be: todo show <title>");
        }

        values = new ArrayList<>(findTodoByTitle(todos, args[0]));

        for (Todo value : values) {
            output.append(value.toString()).append("\n");
        }

        if (values.isEmpty()) {
            output.append("> nothing here\n");
        }

        AnsiConsole.out.print(output);
    }

    private static void commandDelete(String[] args) {
        ArrayList<Todo> values;

        if (args.length != 1) {
            throw new RuntimeException("> command should be: todo delete <title>");
        }

        if (todos.isEmpty()) {
            throw new RuntimeException("> there are no todos to be deleted");
        }

        values = new ArrayList<>(findTodoByTitle(todos, args[0]));

        if (values.isEmpty()) {
            throw new RuntimeException("> could not find Todo (Hint: it's case sensitive)");
        } else {
            todos.remove(getIndexOfTodo(selectFromMultipleTodosDialog(values)));
            System.out.println("> successfully deleted");
        }


        writeTodos(todos);
    }

    private static void commandDeleteAll(String[] args) {
        if (args.length > 1) {
            throw new RuntimeException("> command should be: todo deleteAll [category]");
        }

        if (args.length != 0) {
            switch (args[0].toLowerCase()) {
                case "all":
                    askToDelete("all");
                    deleteAll();
                    break;
                case "ticked":
                    askToDelete("all ticked");
                    deleteTickedOrUnticked(true);
                    break;
                case "unticked":
                    askToDelete("all unticked");
                    deleteTickedOrUnticked(false);
                    break;
                default:
                    throw new RuntimeException("> unexpected argument, for help type: todo help");
            }
        } else {
            askToDelete("");
            deleteAll();
        }
    }

    private static void askToDelete(String category) {
        Scanner sc = new Scanner(System.in);

        System.out.println("> do you really want to delete " + category + " todos?\n");

        System.out.println("> type \"DELETE\" to confirm");
        AnsiConsole.out.print("> " + ANSI_RED);

        String input = sc.nextLine();

        AnsiConsole.out.print(ANSI_RESET);

        System.out.println();

        if (!input.equals("DELETE")) {
            throw new RuntimeException("> \"DELETE\" was not entered correctly");
        }
    }

    private static void deleteAll() {
        try {
            deleteFileOrFolder(Paths.get(todoDir.getPath()));

            System.out.println("> everything deleted");
        } catch (IOException e) {
            throw new RuntimeException("> could not delete everything");
        }
    }

    private static void deleteTickedOrUnticked(boolean ticked) {
        todos = new ArrayList<>(getTickedOrUnticked(!ticked));

        writeTodos(todos);

        System.out.println("> all " + (ticked ? "ticked" : "unticked") + " todos deleted");
    }

    private static void commandSort(String[] args) {
        String exceptionMessage = "> command should be: todo sort [category] <filter>";
        String unknownArgument = "> unexpected argument, for help type: todo help";

        switch (args.length) {
            case 1:
                todos = filterSelection(todos, args[0].toLowerCase(), unknownArgument);
                break;
            case 2:
                switch (args[0].toLowerCase()) {
                    case "unticked":
                        List<Todo> ticked = getTickedOrUnticked(true);
                        todos = filterSelection(getTickedOrUnticked(false), args[1].toLowerCase(), unknownArgument);
                        todos.addAll(ticked);
                        break;
                    case "ticked":
                        List<Todo> unticked = getTickedOrUnticked(false);
                        todos = filterSelection(getTickedOrUnticked(true), args[1].toLowerCase(), unknownArgument);
                        todos.addAll(unticked);
                        break;
                    case "all":
                        todos = filterSelection(todos, args[1].toLowerCase(), unknownArgument);
                        break;
                    default:
                        throw new RuntimeException(unknownArgument);
                }
                break;
            default:
                throw new RuntimeException(exceptionMessage);
        }

        System.out.println("> sorted");

        writeTodos(todos);
    }

    private static void commandEdit(String[] args) {
        String message = "> command should be: todo edit <\"title\"|\"description\"> <title> [edit]";
        List<Todo> values;
        int index;

        if (args.length < 2 || args.length > 3) {
            throw new RuntimeException(message);
        }

        values = findTodoByTitle(todos, args[1]);

        if (values.size() == 0) {
            throw new RuntimeException("> could not find todo");
        }

        switch (args[0].toLowerCase()) {
            case "title":
                switch (args.length) {
                    case 2:
                        index = getIndexOfTodo(selectFromMultipleTodosDialog(values));
                        todos.get(index).setTitle(userEdit("title", todos.get(index).getTitle()));
                        break;
                    case 3:
                        index = getIndexOfTodo(selectFromMultipleTodosDialog(values));
                        todos.get(index).setTitle(args[2]);
                        break;
                }
                break;
            case "description":
                switch (args.length) {
                    case 2:
                        index = getIndexOfTodo(selectFromMultipleTodosDialog(values));
                        todos.get(index).setDescription(userEdit("description", todos.get(index).getDescription()));
                        break;
                    case 3:
                        index = getIndexOfTodo(selectFromMultipleTodosDialog(values));
                        todos.get(index).setDescription(args[2]);
                        break;
                }
                break;
            default:
                throw new RuntimeException(message);
        }

        System.out.println("> edited");

        writeTodos(todos);
    }

    private static String userEdit(String part, String old) {
        Scanner sc = new Scanner(System.in);

        System.out.println("> old " + part + ": " + old);
        System.out.print("> new " + part + ": ");

        String input = sc.nextLine();
        System.out.println();

        return input;
    }

    private static void commandHelp(String[] args) {
        switch (args.length) {
            case 0:
                System.out.println("for command specific information type: todo help [command]" + "\n" +
                        "commands, categories, and filters are not case sensitive" + "\n\n" +
                        "\t\t" + "Commands" + "\n\n" +
                        "  " + String.format("%-20s%s", "new", "creates a new todo") + "\n" +
                        "  " + String.format("%-20s%s", "ls", "lists your todos") + "\n" +
                        "  " + String.format("%-20s%s", "tick/untick", "ticks/unticks a todo") + "\n" +
                        "  " + String.format("%-20s%s", "show", "shows details for a todo") + "\n" +
                        "  " + String.format("%-20s%s", "delete", "deletes a todo") + "\n" +
                        "  " + String.format("%-20s%s", "edit", "edits a todo") + "\n" +
                        "  " + String.format("%-20s%s", "sort", "sorts your todos") + "\n" +
                        "  " + String.format("%-20s%s", "deleteAll", "deletes all data") + "\n\n" +
                        "\t\t" + "Categories" + "\n\n" +
                        "  " + String.format("%-20s%s", "ticked", "includes all ticked todos") + "\n" +
                        "  " + String.format("%-20s%s", "unticked", "includes all unticked todos") + "\n" +
                        "  " + String.format("%-20s%s", "all", "includes all todos") + "\n\n" +
                        "\t\t" + "Filters" + "\n\n" +
                        "  " + String.format("%-20s%s", "NewtoOld", "sorts todos from new to old") + "\n" +
                        "  " + String.format("%-20s%s", "OldtoNew", "sorts todos from old to new") + "\n" +
                        "  " + String.format("%-20s%s", "AtoZ", "sorts todos alphabetically from A to Z") + "\n" +
                        "  " + String.format("%-20s%s", "ZtoA", "sorts todos alphabetically from Z to A") + "\n" +
                        "  " + String.format("%-20s%s", "TickedtoUnticked", "sorts todos from ticked to unticked") + "\n" +
                        "  " + String.format("%-20s%s", "UntickedtoTicked", "sorts todos from unticked to ticked"));
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "new":
                    case "add":
                        System.out.println("> todo new <title> [description]");
                        break;
                    case "ls":
                        System.out.println("> todo ls [category] [filter]");
                        break;
                    case "tick":
                        System.out.println("> todo tick <title>");
                        break;
                    case "untick":
                        System.out.println("> todo untick <title>");
                        break;
                    case "show":
                        System.out.println("> todo show <title>");
                        break;
                    case "delete":
                        System.out.println("> todo delete <title>");
                        break;
                    case "edit":
                        System.out.println("> todo edit <\"title\"|\"description\"> <title> [edit]");
                        break;
                    case "sort":
                        System.out.println("> todo sort [category] <filter>");
                        break;
                    case "deleteall":
                        System.out.println("> todo deleteAll [category]");
                        break;
                    default:
                        throw new RuntimeException("> unexpected argument, for help type: todo help");
                }
                break;
        default:
            throw new RuntimeException("> command should be: todo help [command]");
        }
    }

    private static void commandLs(String[] args) {
        String unexpectedArgument = "> unexpected argument, for help type: todo help";

        switch (args.length) {
            case 0:
                list(getTickedOrUnticked(false));
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "unticked":
                        list(getTickedOrUnticked(false));
                        break;
                    case "ticked":
                        list(getTickedOrUnticked(true));
                        break;
                    case "all":
                        list(todos);
                        break;
                    default:
                        list(filterSelection(getTickedOrUnticked(false), args[0], unexpectedArgument));
                }
                break;
            case 2:
                switch (args[0]) {
                    case "unticked":
                        list(filterSelection(getTickedOrUnticked(false), args[1], unexpectedArgument));
                        break;
                    case "ticked":
                        list(filterSelection(getTickedOrUnticked(true), args[1], unexpectedArgument));
                        break;
                    case "all":
                        list(filterSelection(todos, args[1], unexpectedArgument));
                        break;
                    default:
                        throw new RuntimeException(unexpectedArgument);
                }
                break;
            default:
                throw new RuntimeException("> command should be: todo ls [category] [filter]");
        }
    }

    private static void list(List<Todo> values) {
        StringBuilder output = new StringBuilder();

        for (Todo todo : values) {
            output.append(todo.forList()).append("\n");
        }

        if (values.isEmpty()) {
            output.append("> nothing here\n");
        }

        AnsiConsole.out.print(output);
    }

    ////////////////////// LOGIC ///////////////////////////

    private static List<Todo> getTickedOrUnticked(boolean ticked) {
        List<Todo> values = new ArrayList<>();

        for (Todo todo : todos) {
            if (ticked) {
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

    private static long selectFromMultipleTodosDialog(List<Todo> results) {
    Scanner sc = new Scanner(System.in);

    long number = 0;
    boolean run = true;

    if (results.size() == 1) {
        return results.get(0).getId();
    }

    while (run) {
        System.out.println("> there is more than one todo with the same name");
        System.out.println("> please choose:\n");

        for (Todo todo : results) {
            AnsiConsole.out.println(todo + "\n");
        }

        System.out.println("> enter ID");
        System.out.print("> ");
        String input = sc.nextLine();

        System.out.println();

        if (input.equals("exit") || input.equals("quit") || input.equals("cancel") || input.equals("stop")) {
            throw new RuntimeException("> exited");
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
            AnsiConsole.out.println("> " + ANSI_RED + "no valid entry" + ANSI_RESET + "\n");
        }
    }

    return number;
} // Only used in case more than one t0do is available

    private static List<Todo> filterSelection(List<Todo> values, String argument, String defaultMessage) {
        switch (argument.toLowerCase()) {
            case "newtoold":
                newtoold(values);
                break;
            case "oldtonew":
                oldtonew(values);
                break;
            case "atoz":
                atoz(values);
                break;
            case "ztoa":
                ztoa(values);
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

        return values;
    }

    ////////////////// READ / WRITE //////////////////////

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

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(todoDir.getPath() + "/todos.todo"))) {
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
                throw new RuntimeException("> could not create ID file");
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                number = Long.parseLong(reader.readLine()) + 1;
            } catch (IOException e) {
                throw new RuntimeException("> could not read ID file");
            }
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(Long.toString(number));
        } catch (IOException e) {
            throw new RuntimeException("> could not write ID file");
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