package ueb_03.TCP;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static void fatal(String comment) {
        System.out.println(comment);
        System.exit(-1);
    }

    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length != 2 && args.length != 3)
            fatal("Usage: \"<netcat> -l <port>\" or \"netcat <ip> <port> <name>\"");
        int port = Integer.parseInt(args[1]);
        String name = null;
        if (args.length == 3) {
            name = args[2];
        }
        if (args[0].equalsIgnoreCase("-l"))
            Server(port);
        else
            Client(args[0], port, name);
    }

    // ************************************************************************
    // Server
    // ************************************************************************
    private static void Server(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            Socket client = serverSocket.accept();
            Thread t = new Thread(() -> serveClient(client));
            t.start();
        }
    }

    private static void serveClient(Socket clientConnection) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
            PrintWriter w = new PrintWriter(clientConnection.getOutputStream(), true);

            String name = r.readLine();
            if (name == null || name.isBlank()) {
                w.println("Ungültiger Name.");
                clientConnection.close();
                return;
            }

            if (clients.containsKey(name)) {
                w.println("Name bereits vergeben.");
                clientConnection.close();
                return;
            }

            clients.put(name, w);
            System.out.println("Neuer Client registriert: " + name);

            String line;
            while ((line = r.readLine()) != null && !line.equalsIgnoreCase("stop")) {
                if (line.startsWith("send ")) {
                    String[] parts = line.split(" ", 3);
                    if (parts.length == 3) {
                        String empfaenger = parts[1];
                        String message = parts[2];
                        PrintWriter empfaengerWriter = clients.get(empfaenger);
                        if (empfaengerWriter != null) {
                            empfaengerWriter.println(name + ": " + message);
                        } else {
                            w.println("Teilnehmer \"" + empfaenger + "\" nicht gefunden.");
                        }
                    } else {
                        w.println("Ungültiges Format. Benutze: send name message");
                    }
                } else {
                    w.println("Unbekannter Befehl.");
                }
            }

            System.out.println("Client getrennt: " + name);
            clients.remove(name);
            clientConnection.close();

        } catch (IOException e) {
            System.out.println("Fehler beim Verarbeiten eines Clients: " + e.getMessage());
        }
    }

    // ************************************************************************
    // Client
    // ************************************************************************
    private static void Client(String serverHost, int serverPort, String name) throws IOException {
        InetAddress serverAddress = InetAddress.getByName(serverHost);
        Socket socket = new Socket(serverAddress, serverPort);
        PrintWriter w = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        w.println(name);

        new Thread(() -> {
            String msg;
            try {
                while ((msg = r.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (IOException e) {
                System.out.println("Verbindung beendet.");
            }
        }).start();

        String line;
        do {
            line = readString();
            w.println(line);
        } while (!line.equalsIgnoreCase("stop"));

        socket.close();
    }

    private static String readString() {
        boolean again = false;
        String input = null;
        do {
            try {
                if (br == null)
                    br = new BufferedReader(new InputStreamReader(System.in));
                input = br.readLine();
            } catch (Exception e) {
                System.out.printf("Exception: %s\n", e.getMessage());
                again = true;
            }
        } while (again);
        return input;
    }

    private static BufferedReader br = null;
}
