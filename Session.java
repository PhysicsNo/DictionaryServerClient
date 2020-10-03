import Exceptions.RequestTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/* TODO
* debug
* final cleanup
* format for remote server functionality
* DONE
* */

/*
For doing timeout stuff!
Socket socket = new Socket();
// This limits the time allowed to establish a connection in the case
// that the connection is refused or server doesn't exist.
socket.connect(new InetSocketAddress(host, port), timeout);
// This stops the request from dragging on after connection succeeds.
socket.setSoTimeout(timeout);

* */
public class Session {


    private Boolean programLive;
    private Boolean connectionOpen;
    private final Boolean debugOn;
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
        String debugMssg;

        switch (reply[0]) {
            case "500":
            case "501":
            case "502":
            case "503":
                debugMssg = "<-- " + replyMessage;
                if (debugOn)
                    System.out.println(debugMssg);
                break;
            case "420":
            case "421":
                debugMssg = "<-- " + replyMessage;
                if (debugOn)
                    System.out.println(debugMssg);
                close();
                break;
            default:
                res = false;
                break;
        }
        return res;
    }

    private void open(String server, String port) {
        try {
            Integer portNumber = Integer.decode(port);
            System.out.printf("Establishing connection to: %s on port %d\n", server, portNumber);

            dictSocket = new Socket();
            int timeout = 5 * 1000;
            dictSocket.connect(new InetSocketAddress(server, portNumber), timeout);
            dictSocket.setSoTimeout(timeout);
            message = new PrintWriter(dictSocket.getOutputStream(), true);
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));


            String initialReply = response.readLine();

            String debugMssg = "<-- " + initialReply;
            if (debugOn && !generalResponseError(initialReply))
                System.out.println(debugMssg);

            connectionOpen = true;
        } catch (SocketTimeoutException ste) {
            String errorMessage = "999 Processing error.";
            System.out.format("\u001B[1m%s\u001B[0m Timed out while waiting for response.\n", errorMessage);
            return;
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

    private void dict() throws IOException, RequestTimeoutException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            close();
            throw new RequestTimeoutException();
        }
        message.println("SHOW DB");
        String debugMsg = "> " + "SHOW DB";
        if (debugOn)
            System.out.println(debugMsg);

        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                break;
            } else if (reply.matches("(554).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                break;
            } else if (reply.matches("(110).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);
            } else {
                //String out = "@ " + reply;
                System.out.println(reply);
            }
        }
    }

    private void set(String dictionary) {
        currDict = dictionary;
    }

    private void define(String word) throws IOException, RequestTimeoutException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            close();
            throw new RequestTimeoutException();
        }
        String mssg = "DEFINE " + currDict + " " + word;
        message.println(mssg);
        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                break;
            } else if (reply.matches("(550).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                System.out.println("Invalid database. change set dict");
                break;
            } else if (reply.matches("(552).*")) {
                //MATCH * word . == all matches using server default strat.
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);
                System.out.println("***No definition found***");
                String temp = currDict;
                set("*");
                match(word, ".");
                set(temp);
                break;
            } else if (reply.matches("(151).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                String line[] = reply.split(" ");
                System.out.println(line[2]);
            } else if (reply.matches("(150).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);
            } else {
                String out = "@ " + reply;
                System.out.println(out);
            }

        }
    }

    private void match(String word, String strat) throws IOException, RequestTimeoutException {
        try {
            response = new BufferedReader(new InputStreamReader(dictSocket.getInputStream()));
        } catch (IOException exception) {
            close();
            throw new RequestTimeoutException();
        }
        String mssg = "MATCH " + currDict + " " + strat + " " + word;
        message.println(mssg);

        String debugMsg = "> " + mssg;
        if (debugOn)
            System.out.println(debugMsg);

        String reply;
        while ((reply = response.readLine()) != null) {
            if (generalResponseError(reply)) {
                break;
            } else if (reply.matches("(250).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                break;
            } else if (reply.matches("(550).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                break;
            } else if (reply.matches("(552).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

                String out = (strat.equals(".")) ? "****No matches found****" : "*****No matching word(s) found*****";
                System.out.println(out);
                break;
            } else if (reply.matches("(152).*") || reply.matches("(150).*")) {
                String debugMssg = "<-- " + reply;
                if (debugOn)
                    System.out.println(debugMssg);

            } else {
                System.out.println(reply);
            }
        }
    }

    private void help(String flag) {
        if (!flag.equals("command"))
            return;
        String open = "open SERVER PORT\n- where PORT must be a numeric value and both parameters must be supplied." +
                "\n- unexpected if connection is already open.";
        String close = "close\n- closes established connection.\n- unexpected if there are no open connections.";
        String quit = "quit\n- closes any established connection and exits the program.";
        String dict = "dict\n- retrieve and prints list of dictionaries.\n- unexpected if no open connection.";
        String set = "set DICTIONARY\n- where dictionary must be one returned by dict or one of the required virtual " +
                "databases (*, !).\n- Set the dictionary to retrieve subsequent definitions and/or matches from." +
                "\n- unexpected if no open connection.";
        String define = "define WORD\n- where WORD is any character string.\n- Retrieve and print all the definitions " +
                "for WORD from dictionary last set using the set command.\n- unexpected if no open connection.";
        String match = "match WORD\n- where WORD is any character string.\n- Retrieve and print all the exact matches found" +
                "for WORD from dictionary last set using the set command.\n- unexpected if no open connection.";
        String prefixmatch = "prefixmatch WORD\n- where WORD is any character string.\n- Retrieve and print all the " +
                "matches found using the prefix specified by WORD." +
                "for WORD from dictionary last set using the set command.\n- unexpected if no open connection.";
        System.out.format("%s\n, %s\n, %s\n, %s\n, %s\n, %s\n, %s\n, %s\n", open, close, quit, dict, set, define, match, prefixmatch);
    }


    private void runCommand(String cmd, String[] args) throws IOException, RequestTimeoutException {

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
            case "help":
                help("cf");
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
            case "help":
                if (!args[1].equals("-commands"))
                    res = false;
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
            } catch(RequestTimeoutException rte) {
                String errorMessage = "925 Control connection I/O error, closing control connection";
                System.out.format("\u001B[1m%s\u001B[0m \n", errorMessage);
            } catch (IOException exception) {
                System.err.println("999 Processing error. Unknown error, terminating.");
                System.exit(-1);
            }
        }
        System.out.println("Program will now exit.");
    }

}
