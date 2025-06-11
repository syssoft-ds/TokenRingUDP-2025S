package ueb_04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

public class TCP_Chat_Client {
    private static String name;
    private static String serverIP;
    private static int serverPort;

    //permittings
    private static boolean globalClient = false;
    private static boolean globalList = false;

    private static void fatal(String input) {
        System.err.println(input);
        System.exit(-1);
    }

    public static boolean isIP(String ip) { // Checks if String is valid IPv4 address
        return UDP_Chat.isIP(ip);
    }

    public static boolean isPort(String port) {
        return UDP_Chat.isPort(port);
    }

    public static void main(String[] args) {

        // Handling arguments, checking validity
        if (args.length != 3) {
            fatal("Arguments: \"<server ip address> <server port number> <client name>\"");
        }
        if (!isIP(args[0])) {
            fatal("Invalid IP address");
        } else {
            serverIP = args[0];
        }
        if (!isPort(args[1])) {
            fatal("Invalid port number");
        } else {
            serverPort = Integer.parseInt(args[1]);
        }
        name = args[2];



        try (Socket socket = new Socket(serverIP, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {
            // closes automatically

            new Thread(() -> {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null) {
                        if (fromServer.startsWith("RULE")) {
                            if (fromServer.contains("LIST") && fromServer.contains("TRUE")) {
                                System.out.println("SERVER: List allowed");
                                globalList = true;
                            }
                            else if (fromServer.contains("LIST") && fromServer.contains("FALSE")) {
                                System.out.println("SERVER: List denied");
                                globalList = false;
                            }
                            else if (fromServer.contains("GLOBAL") && fromServer.contains("TRUE")) {
                                System.out.println("SERVER: Global allowed");
                                globalClient = true;
                            }
                            else if (fromServer.contains("GLOBAL") && fromServer.contains("FALSE")) {
                                System.out.println("SERVER: Global denied");
                                globalClient = false;
                            }
                        }else{
                            System.out.println(fromServer);
                        }
                        Map<String, String> predefinedResponses = Map.of(
                                "Was ist deine MAC-Adresse?", "Diese Information ist geheim.",
                                "Sind Kartoffeln eine richtige Mahlzeit?", "Natürlich, mit Soße und allem!"
                        );

                        if (predefinedResponses.containsKey(fromServer)) {
                            System.out.println("Auto-Response: " + predefinedResponses.get(fromServer));
                        }

                    }
                } catch (IOException e) {
                    fatal("Unable to get message from Server.");
                }
            }).start();

            // Register the client with the server
            out.println("register " + name);

            System.out.println(name + " is connected to Server at IP " + serverIP + " on port " + serverPort + ".\nUse \"send <client name> <message>\" to send a message to a client.");
            System.out.println("use \"sendAll <message>\" to send a message to all clients.");
            System.out.println("use \"list\" to recieve a list of all Clients");
            System.out.println("use \"request\" <*> to submit a request, you can request:");
            System.out.println("\"global\" to request sending global messages,");
            System.out.println("\"list\" to request recieving all clients.");

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                String[] parts = userInput.split(" ", 3);
                if (parts[0].equalsIgnoreCase("send") && parts.length == 3) {
                    out.println(userInput);
                    System.out.println("Message sent.");
                } else if (parts[0].equalsIgnoreCase("request") && parts.length == 2) {
                    if(parts[1].equalsIgnoreCase("global")) {
                        out.println(userInput);
                        System.out.println("request sent.");
                    }
                    else if(parts[1].equalsIgnoreCase("list")) {
                        out.println(userInput);
                        System.out.println("request sent.");
                    }
                    else{
                        System.out.println("unknown command.");
                    }
                }

                else if (parts[0].equalsIgnoreCase("sendAll") && parts.length == 2 && globalClient == true) {
                    out.println(userInput);
                    System.out.println("Message sent to all clients.");
                }
                else if (parts[0].equalsIgnoreCase("list") && parts.length == 1 && globalList == true) {
                    out.println(userInput);
                    System.out.println("List requested.");
                }
                else {
                    System.err.println("Unknown command, or missing permission.");
                }
            }
        } catch (UnknownHostException e) {
            fatal("Unknown Server with IP " + serverIP);
        } catch (IOException e) {
            fatal("Unable to send message.");
        }
        System.exit(0);
    }
}