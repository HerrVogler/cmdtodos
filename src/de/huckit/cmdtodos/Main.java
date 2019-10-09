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
    private static String appdata = System.getenv("APPDATA");

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"help"};
        }

        String command = args[0];
        final var arguments = Arrays.copyOfRange(args, 1, args.length);
        File todosDir = new File(appdata + "\\cmdtodos\\todos");



        if (!todosDir.exists()) {
           todosDir.mkdir();
        }
        else {
            try {
                todos = readTodos();
            }
            catch (Exception e) {
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

                break;
            case "help":
                System.out.println("HiLfE");
                break;
            case "ls":
                commandLs(arguments);
                break;
            case "lsarchive":
                commandLsarchive(arguments);
                break;

            default:
                throw new RuntimeException("unknown command");
        }

        writeTodos(todos.toArray(new Todo[0]));
    }

    private static void commandLsarchive(String[] args) {
        //gg
    }

    private static void commandLs(String[] args) {
        System.out.println("works");
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
                throw new RuntimeException("command should be: \"todo new <titel> <description>\"");
        }
    }

    private static void writeTodos(Serializable[] todos) {
        try {
            deleteFileOrFolder(Paths.get(String.valueOf(new File(appdata + "\\cmdtodos\\todos"))));
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't delete files");
        }
        for (int i = 0; i < todos.length; i++) {
            File file = new File(appdata + "\\cmdtodos\\todos\\" + i + ".todo");

            ObjectOutputStream oos = null;

            try {
                oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(todos);
            }
            catch (Exception e) {
                System.out.println("Todo not saved");
            }
            finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static ArrayList<Todo> readTodos() throws IOException, ClassNotFoundException {
        ArrayList<Todo> todos = new ArrayList<Todo>();
        int length = new File(appdata + "\\cmdtodos\\todos").listFiles().length;

        for (int i = 0; i < length; i++) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(appdata + "\\cmdtodos\\todos\\" + i + ".todo")));
            Object obj = ois.readObject();
            ois.close();

            todos.add((Todo) obj);
        }

        return todos;
    }

    private static void deleteFileOrFolder(final Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>(){
            @Override public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                return handleException(e);
            }

            private FileVisitResult handleException(final IOException e) {
                e.printStackTrace(); // replace with more robust error handling
                return TERMINATE;
            }

            @Override public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                    throws IOException {
                if(e!=null)return handleException(e);
                Files.delete(dir);
                return CONTINUE;
            }
        });
    };
}
