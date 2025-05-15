import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {


    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }

        boolean wasEverInRing = first;

        while (true) {
            try {
                socket.setSoTimeout(10000);

                Token rc = Token.receive(socket);

                wasEverInRing = true;

                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                // Eigene Adresse hinzufügen, wenn nur ein Knoten da ist
                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) continue;
                }

                first = false;

                // Neue Kandidaten hinzufügen
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                boolean sent = false;
                int attempts = rc.length(); // max. so viele Versuche wie Mitglieder
                while (!sent && attempts > 0) {
                    Token.Endpoint next = rc.poll();
                    try {
                        rc.append(next); // wieder hinten anhängen
                        rc.incrementSequence();
                        Thread.sleep(1000);
                        rc.send(socket, next);
                        sent = true;
                    } catch (IOException e) {
                        System.out.println("Reaching member impossible: " + next.ip() + ":" + next.port() + ", will be removed.");
                        // nicht wieder anhängen => wird entfernt
                    }
                    attempts--;
                }

                if (!sent) {
                    System.out.println("No accessible member in ring!");
                    Thread.sleep(2000); // Warten und hoffen, dass jemand zurückkommt
                }
            }  catch (SocketTimeoutException e) {
                System.out.println("Timeout – Token may got lost.");
                // Bedingung: Nur Token neu erzeugen, wenn nichts kam
                if (first || wasEverInRing) {
                    System.out.println("Creating new token and restarting ring.");
                    Token newToken = new Token().append(ip, port);
                    for (Token.Endpoint candidate : candidates) {
                        newToken.append(candidate);
                    }
                    candidates.clear();
                    newToken.incrementSequence();
                    if (newToken.length() > 0) {
                        Token.Endpoint next = newToken.poll();
                        newToken.append(next);
                        try {
                            newToken.send(socket, next);
                        } catch (IOException ex) {
                            System.out.println("Sending token to " + next + " was impossible.");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s, %d)\n", ip, port);
            if (args.length == 0) {
                loop(socket,ip,port,true);
            }
            else if (args.length == 2) {
                Token rc = new Token().append(ip,port);
                rc.send(socket,args[0],Integer.parseInt(args[1]));
                loop(socket,ip,port,false);
            }
            else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        }
        catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        }
        catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        }
        catch (IOException e) {
            System.out.println("IO error: " + e.getMessage());
            System.out.println(e.getStackTrace());
        }
    }
}