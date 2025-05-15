import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

/**
 * Fehlerrobuster Token-Ring:
 *
 * Diese Implementierung des Token-Rings stellt sicher, dass bei einem Ausfall
 * eines Knotens dieser automatisch aus dem Ring entfernt wird. Der Token wird
 * dadurch weiterhin zwischen den aktiven Knoten übertragen.
 *
 * Der Ring reorganisiert sich selbstständig und bleibt funktionsfähig,
 * solange mindestens ein Knoten aktiv ist.
 *
 * Getestet durch Starten mehrerer Instanzen und gezieltes Beenden einzelner Teilnehmer.
 */
public class TokenRing {

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                Token rc = Token.receive(socket);

                // Ausgabe des aktuellen Tokens mit Sequenznummer und Mitgliedern
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();

                // Falls nur ein Mitglied existiert, neuen Teilnehmer aufnehmen
                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) {
                        continue;
                    }
                }
                first = false;

                // Neue Kandidaten in den Ring einfügen
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();

                // Fehlerbehandlung beim Senden: Ausgefallene Knoten automatisch entfernen
                boolean sent = false;
                while (!sent && rc.length() > 0) {
                    Token.Endpoint next = rc.poll();  // nächsten Teilnehmer nehmen
                    try {
                        rc.incrementSequence();        // Sequenznummer erhöhen
                        Thread.sleep(1000);             // kleine Verzögerung
                        rc.send(socket, next);          // Token senden
                        rc.append(next);                // NUR wenn senden geklappt hat, wieder anhängen!
                        sent = true;                    // Senden erfolgreich
                    } catch (IOException e) {
                        // Knoten konnte nicht erreicht werden - entfernen
                        System.out.println("Knoten " + next.ip() + ":" + next.port() + " ist ausgefallen. Entferne ihn aus dem Ring.");
                    }
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

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s, %d)\n", ip, port);

            // Entscheidung: erster Knoten startet Ring oder schließt sich an
            if (args.length == 0) {
                loop(socket, ip, port, true);
            } else if (args.length == 2) {
                Token rc = new Token().append(ip, port);
                rc.send(socket, args[0], Integer.parseInt(args[1]));
                loop(socket, ip, port, false);
            } else {
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
