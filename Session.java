import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/* TODO
* (optional) add help status messages
* handle timeouts while trying to connect to server (should be in the api)
* deal with request timeouts (i.e. if user idles for a while the server will close, we need to re-open or prompt them to do so)
* add in ALL remaining assignment-specific error codes to raise (might do it as an exception library)
* (optional) error handling/test suite
* deal with debug on/off
* */
//And uhm this new reader on each command call should also give us a means to handle timeouts!
//deal with setting up 150 to be ignored
public class Session {


    private Boolean programLive;
    private Boolean connectionOpen;
    private Boolean debugOn;
    private final int MAX_LEN;
    private String currDict;

    private Socket dictSocket;
    private PrintWriter message;
    BufferedReader response;


    public Session(Boolean debugOn, int max_length) {
        this.programLive = true;
        this.connectionOpen = false;
        this.debugOn = debugOn;
        this.MAX_LEN = max_length;
    }

    private boolean generalResponseError(String replyMessage) throws IOException {
        boolean res = true;
        String[] reply = replyMessage.split(" ");
        switch (reply[0]) {
            case "500":
                System.out.println("Syntax error, command not recognized");
                break;
            case "501":
                System.out.println("Syntax error, illegal parameters");
                break;
            case "502":
                System.out.println("Command not implemented");
                break;
            case "503":
                System.out.println("Command parameter not implemented");
                break;
            case "420":
                System.out.println("Server temporarily unavailable");
                close();
                break;
            case "421":
                System.out.println("Server shutting down at operator request");
                close();
                break;
            default:
                res = false;
                break;
        }
        return res;
    }

    private void open(String server, String port) throws IOException {
        //TODO connection timeout handling, i.e. cannot establish connection within 30s - piazza this one

        try {
            Integer portNumber = Integer.decode(port);
            System.out.printf("Establishing connection to: %s on port %d\n", server, portNumber);

            dictSocket = new Socket(server, portNumber);
            message = new PrintWriter(dictSocket.getOutputStream(), true);
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
            if (generalResponseError(response.readLine()))
                return;
            connectionOpen = true;
        } catch (IOException exception) {
            String errorMessage = "920 Control connection to " + server +  " on port " + port +" failed to open.";
            System.out.format("\u001B[1m%s\u001B[0m Invalid host or socket input.\n", errorMessage);
            return;
        }
        currDict = "*";
        System.out.println("Successfully connected to server");
    }

    private void close() throws IOException {
        if (dictSocket != null) {
            message.println("quit");
            message.println("\r\n");

            dictSocket.close();
        }
        System.out.println("Closing connection");
        connectionOpen = false;
    }

    private void quit() throws IOException {

        if (connectionOpen)
            close();

        System.out.println("bye bye");
        programLive = false;
    }

    private void dict() throws IOException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            throw new RequestTimeoutException();
        }
        System.out.println("ENTERING loop");
        message.println("SHOW DB");
        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                break;
            } else if (reply.matches("(554).*")) {
                System.out.println("No dictionaries present on this server.");
                break;
            } else if (reply.matches("(110).*") || reply.matches("(\\.)")) {
                continue;
            } else {
                System.out.println(reply);
            }
        }
    }

    private void set(String dictionary) {
        currDict = dictionary;
    }

    private void define(String word) throws IOException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            throw new RequestTimeoutException();
        }
        String mssg = "DEFINE " + currDict + " " + word;
        message.println(mssg);
        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                break;
            } else if (reply.matches("(550).*")) {
                System.out.println("Invalid database. change set dict");
                break;
            } else if (reply.matches("(552).*")) {
                //MATCH * word . == all matches using server default strat.
                String temp = currDict;
                set("*");
                match(word, ".");
                set(temp);
                break;
            } else if (reply.matches("(151).*")) {
                String line[] = reply.split(" ");
                System.out.println(line[2]);
            } else if (reply.matches("(\\.)")) {
                System.out.println("");
            } else {
                System.out.println(reply);
            }

        }
    }

    private void match(String word, String strat) throws IOException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            throw new RequestTimeoutException();
        }
        String mssg = "MATCH " + currDict + " " + strat + " " + word;
        //message.println(""); //Hopefully detect via exception a close connection
        message.println(mssg);
        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                break;
            } else if (reply.matches("(550).*")) {
                System.out.println("Invalid database. change set dict");
                break;
            } else if (reply.matches("(552).*")) {
                String out = (strat.equals(".")) ? "****No matches found****" : "*****No matching word(s) found*****";
                System.out.println(out);
                break;
            } else if (reply.matches("(152).*") || reply.matches("(150).*") || reply.matches("(\\.)")) {
                continue;
            } else {
                System.out.println(reply);
            }
        }
    }


    private void runCommand(String cmd, String[] args) throws IOException {
        switch (cmd) {
            case "open":
                open(args[0], args[1]);
                break;
            case "dict":
                dict();
                break;
            case "set":
                set(args[0]);
                break;
            case "define":
                define(args[0]);
                break;
            case "match":
                match(args[0], "exact");
                break;
            case "prefixmatch":
                match(args[0], "prefix");
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


    private Boolean checkCommand(String cmd, String[] args) {
        Boolean res = true;
        switch (cmd) {
            case "open":
                if (args.length != 2) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (!args[1].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (connectionOpen) {
                    System.out.println("\u001B[1m 903 Supplied command not expected at this time.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                }

                break;
            case "dict":
            case "close":
                if (args.length != 0) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (!connectionOpen) {
                    System.out.println("\u001B[1m 903 Supplied command not expected at this time.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                }
                break;
            case "set":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (!connectionOpen) {
                    System.out.println("\u001B[1m 903 Supplied command not expected at this time.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                }
                break;
            case "define":
            case "match":
            case "prefixmatch":
                if (args.length != 1) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (args[0].matches("(\\d+)")) {
                    System.out.println("\u001B[1m 902 Invalid argument.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                } else if (!connectionOpen) {
                    System.out.println("\u001B[1m 903 Supplied command not expected at this time.\u001B[0m For description of commands and their usage see help -commands");
                    res = false;
                }
                break;
            case "quit":
                if (args.length != 0) {
                    System.out.println("\u001B[1m 901 Incorrect number of arguments.\u001B[0m For description of valid commands see help -commands");
                    res = false;
                }
                break;
            default:
                //unknown command
                System.out.println("\u001B[1m 900 Invalid command.\u001B[0m For description of valid commands see help -commands");
                res = false;
                break;
        }
        return res;
    }

    public void listen() {
        byte cmdString[];
        String command = null;
        String[] arguments = null;
        int len;
        while (programLive) {
            cmdString = new byte[MAX_LEN];
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

            } catch (IOException exception) {
                System.err.println("998 Input error while reading commands, terminating.");
                System.exit(-1);
            }

            //handle command
            try {
                Boolean cmdStatus = checkCommand(command, arguments);
                if (cmdStatus) {
                    //Run the cmd
                    runCommand(command, arguments);
                }
            } catch (IOException exception) {
                //TODO the rest of the exception handling
                System.err.println("999 Processing error. Unknown error, terminating.");
                System.exit(-1);
            }
        }
        System.out.println("Program will now exit.");
    }

}
