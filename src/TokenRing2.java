import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing2 {

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) throws InterruptedException {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }


        try {
            socket.setSoTimeout(4000); // timeout set for 4 secs
        } catch (SocketException e) {
            System.out.println("Fehler beim Setzen des Socket-Timeouts: " + e.getMessage());
        }

        while (true) {
            Token rc = null;

            // receive of token with timeout
            try {
                rc = Token.receive(socket);
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout beim Warten auf Token. Vorheriger Knoten eventuell ausgefallen.");
                continue; // failed Node -> wait to try next Endpoint
            } catch (IOException e) {
                System.out.println("Fehler beim Empfang: " + e.getMessage());
                continue;
            }

            if (rc.length() == 0) {
                System.out.println("Ring ist leer. Keine verfügbaren Knoten mehr im Ring.");
                Thread.sleep(1000);
                continue;
            }

            if (rc.length() == 1) {
                candidates.add(rc.poll());
                if (!first) {
                    continue;
                }
            }
            first = false;

            /*
            System.out.print("Anzeige - Aktueller Ring: ");
            for (Token.Endpoint endpoint : rc.getRing()) {
                System.out.printf("(%s:%d) ", endpoint.ip(), endpoint.port());
            } */
            System.out.println();

            for (Token.Endpoint candidate : candidates) {
                rc.append(candidate);
            }
            candidates.clear();

            boolean tokenSent = false;
            int attempts = rc.length();

            while (attempts-- > 0 && !tokenSent) {
                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();
                Thread.sleep(2000);
                try {
                    rc.send(socket, next);
                    tokenSent = true;
                } catch (IOException e) {
                    System.out.printf("Knoten %s:%d nicht erreichbar -> Knoten wird entfernt\n", next.ip(), next.port());
                    rc.removeNode(next.ip(), next.port());
                }
            }

            if (!tokenSent) {
                System.out.println("Kein verfügbarer Knoten -> Warte ein wenig...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}