import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRingDelete {

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) throws SocketException {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        Token.Endpoint next = null;
        Token rc = null;
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {

                rc = receive(rc, socket, next);

                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();
                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) {
                        continue;
                    }
                }
                first = false;

                // Set the timeout dynamically based on the number of participants
                int timeout = rc.length() * 1500; // 1,5 seconds per participant
                socket.setSoTimeout(timeout);
                System.out.printf("Set socket timeout to %d ms.\n", timeout);

                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                Token.Endpoint copyEndpoint = rc.poll();
                if (copyEndpoint != null) {
                    next = new Token.Endpoint(copyEndpoint.ip(), copyEndpoint.port());
                } else {
                    next = null; // Handle the case where the ring is empty
                }
                rc.append(copyEndpoint);
                rc.incrementSequence();
                Thread.sleep(1000);
                rc.send(socket, copyEndpoint);
            }
            catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static Token receive(Token rc, DatagramSocket socket, Token.Endpoint next) throws IOException {
        Token rccopy;
        if(rc != null) {
            // Kopie des Tokens erstellen für Rekursion!
            rccopy = new Token();
            rccopy.setSequence(rc.getSequence()); // Copy the sequence value

            // Copy the ring (queue) contents
            for (Token.Endpoint endpoint : rc.getRing()) {
                rccopy.append(new Token.Endpoint(endpoint.ip(), endpoint.port()));
            }
        }

        try {
            rccopy = Token.receive(socket);
            if (rccopy == null) {
                System.out.println("Received Ping. Return to receive.");
                return receive(rc, socket, next);
            }
            return rccopy;
        } catch (SocketTimeoutException e) {
            confirmDeadNode(socket, rc, next);
            return receive(rc, socket, next);
        }
    }

    private static void confirmDeadNode(DatagramSocket socket, Token rc, Token.Endpoint next) throws SocketException {
        if (next == null) {
            System.out.println("No next node to confirm.");
            return;
        }
        // Use the sendPing method from Token
        if (!rc.sendPing(socket, next)) {
            // Remove the dead node from the ring
            rc.remove(next.ip(), next.port());
            System.out.printf("Removed dead node %s:%d from the ring.\n", next.ip(), next.port());

            // Send the token to the next available node
            Token.Endpoint newNext = rc.poll();
            if (newNext != null) {
                try {
                    rc.append(newNext); // Re-add the new next node to the ring
                    rc.send(socket, newNext);
                    System.out.printf("Token sent to new next node %s:%d\n", newNext.ip(), newNext.port());
                } catch (IOException e) {
                    System.out.println("Error sending token to new next node: " + e.getMessage());
                }
            } else {
                System.out.println("No more nodes in the ring. Terminating program.");
                System.exit(1);
            }
        }else{
            // The Knoten lebt, also Timeout zurücksetzen, damit nicht noch mal Pingt.
            socket.setSoTimeout(999999);
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