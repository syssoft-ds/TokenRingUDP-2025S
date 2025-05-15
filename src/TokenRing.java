import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.nio.charset.StandardCharsets;


public class TokenRing {

    private static final int TIMEOUT_MS = 15000;

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                Token.ReceivedToken received = Token.receiveWithSender(socket);
                Token rc = received.token;

                if (rc.isAck()) {
                    continue; // ACKs ignorieren
                }

                // ACK senden
                Token.sendAck(socket, received.address, received.port);

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
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();
                Token.Endpoint next = rc.poll();
                rc.append(next);
                rc.incrementSequence();
                if (next.ip().equals(ip) && next.port() == port) {
                    System.out.println("Skipping sending token to self.");
                    rc.append(next); // selbst wieder an Ring anhängen und nächsten Knoten zum Senden suchen
                    next = rc.poll();
                }
                Thread.sleep(1000);
                rc.send(socket, next);

                // Nach send() in Zustand waitForAck() gehen um auf eine Bestätigung zu warten, welche nach einem receive() verschickt wird
                // wenn Timeout ohne ACK durch ist, dann soll Knoten entfernt werden
                boolean ackReceived = waitForAck(socket, next.ip(), next.port());
                if (!ackReceived) {
                    System.out.println("Removing unreachable node: " + next.ip() + ":" + next.port());
                }
            }
            catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static boolean waitForAck(DatagramSocket socket, String expectedIp, int expectedPort) throws IOException {
        try {
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] buf = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            Token token = Token.fromJSON(json);

            if (token.isAck() &&
                    packet.getAddress().getHostAddress().equals(expectedIp) &&
                    packet.getPort() == expectedPort) {
                System.out.println("Received ACK from " + expectedIp + ":" + expectedPort);
                return true;
            } else {
                System.out.println("Received something unexpected while waiting for ACK.");
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout waiting for ACK from " + expectedIp + ":" + expectedPort);
        } finally {
            socket.setSoTimeout(0);
        }
        return false;
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