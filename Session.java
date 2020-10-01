import java.io.IOException;
import java.util.Arrays;

public class Session {


    private static Session instance = null;
    private static Boolean programLive;
    private static Boolean connectionOpen;
    public static Boolean debugOn;
    public static int MAX_LEN;

    private static void open(String server, String port) {
        //TODO API stuff
        Integer portNumber = Integer.decode(port);
        System.out.printf("Esablishing connection to: %s on port %d\n", server, portNumber);
        connectionOpen = true;
    }

    private static void close() {
        //TODO API stuff
        System.out.println("Closing connection");
        connectionOpen = false;
    }

    private static void quit() {
        //TODO Java Socket API stuff
        System.out.println("bye bye");
        connectionOpen = false;
        programLive = false;
    }

    private static void runCommand(String cmd, String[] args) {
        switch (cmd) {
            case "open":
                open(args[0], args[1]);
                break;
            case "dict":
                //
                break;
            case "set":
                //
                break;
            case "define":

                break;
            case "match":

                break;
            case "prefixmatch":

                break;
            case "close":
                close();
                break;
            case "quit":
                quit();
                break;
            default:
                //invalid command
                System.out.println("error");
                break;
        }
    }


    private static Boolean checkCommand(String cmd, String[] args) {
        Boolean res;
        switch (cmd) {
            case "open":
                //TODO: we need one more block to handle when user tries to open connection while one is already open.. might needa do this all in a diff class!
                //State variable to keep track of if a connection is open!
                if (args.length != 2) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else if (!args[1].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }

                break;
            case "dict":
                //TODO: we need one more block to handle when user tries to use this when no connection is open (basically true of all commands here).
                //State variable to keep track of if a connection is open!
                if (args.length != 0) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "set":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "define":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else if (args[1].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "match":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else if (args[1].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "prefixmatch":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else if (args[1].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "close":
                if (args.length != 0) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    //Command is valid
                    res = true;
                }
                break;
            case "quit":
                if (args.length != 0) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                } else {
                    res = true;
                }
                break;
            default:
                //invalid command
                System.out.println("\u001B[1m 900 Invalid command.\u001B[0m For description of valid commands see help -commands");
                res = false;
                break;
        }
        return res;
    }


    private Session() {
        programLive = true;
        connectionOpen = false;
    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public static void listen() {
        byte cmdString[] = new byte[MAX_LEN];
        String command;
        String[] arguments;
        int len;
        while (programLive) {
            try {
                System.out.print("csdict> ");
                System.in.read(cmdString);

                // Convert the command string to ASCII
                String inputString = new String(cmdString, "ASCII");

                // Split the string into words
                String[] inputs = inputString.trim().split("( |\t)+");
                // Set the command
                command = inputs[0].toLowerCase().trim();
                // Remainder of the inputs is the arguments.
                arguments = Arrays.copyOfRange(inputs, 1, inputs.length);

                System.out.println("The command is: " + command);
                len = arguments.length;
                System.out.println("The arguments are: ");
                for (int i = 0; i < len; i++) {
                    System.out.println("    " + arguments[i]);
                }
                System.out.println("Done.");

                //handle command
                Boolean cmdStatus = checkCommand(command, arguments);
                if (cmdStatus) {
                    //Run the cmd
                    runCommand(command, arguments);

                }

            } catch (IOException exception) {
                System.err.println("998 Input error while reading commands, terminating.");
                System.exit(-1);
            }
        }
        System.out.println("Program will now exit.");
    }

}
