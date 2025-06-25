package ueb_05;

import java.io.*;
import java.net.*;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Spieler {

    private static DatagramSocket socket;
    private static Scanner scanner = new Scanner(System.in);
    private static int kapital = 1000;

    private static final int PORT = socket.getLocalPort();
    private static final String IP = socket.getLocalAddress().getHostAddress();
    private static final int CROUPIER_PORT = 5000;
    private static final String CROUPIER_IP = "127.0.0.1";
    private static final int BUFFER_SIZE = 4096;

    public static void main(String[] args) throws Exception {
        socket = new DatagramSocket(PORT);
        double trueCount;
        sendMessage(IP, CROUPIER_IP + " " + String.valueOf(CROUPIER_PORT), "register", "Spieler " + IP + " " + String.valueOf(PORT));

        while (true) {
            String [] hand1 = new String[15];
            String [] hand2 = new String[15];
            String [] croupier = new String[15];
            /*
            die theoretisch maximale spielbare Hand bei 8 Decks besteht aus 8 Assen und 6x der 2, also insgesamt 14 Karten,
            sollte eine 15 Karte gezogen werden, ist man sowieso überkauft.
            * */
            String antwort;
            sendMessage(IP, "Kartenzähler", "get", "trueCount");
            antwort = receiveMessage();
            trueCount = Double.parseDouble(antwort);
            System.out.println(trueCount);
            System.out.print("Setze Einsatz: ");
            int einsatz = Integer.parseInt(scanner.nextLine());
            sendMessage(IP, "Croupier", "Einsatz", String.valueOf(einsatz));
            antwort = receiveMessage();
            if(antwort.endsWith("denied")) {
                System.out.println("Nicht Spielberechtigt");
                break;
                // falls der Croupier den Spieler rausgeschmissen hat, wird das hier zurückgegeben, der Spieler kann dann nicht weiterspielen
            }
            else if(antwort.endsWith("accepted")){
                // Spielberechtigung wurde erteilt, der Einsatz wurde angenommen, das Spiel kann starten
                kapital -= einsatz;
            }
            String command = "";
            while(command != "Surrender"){
                sendMessage(IP, "Croupier", "getHand", "first");
                // first = erste Hand, second = zweite Hand, both = beide Hände. Es werden zusätzlich auch immer die offenen Karten des Croupiers mitgegeben
                antwort = receiveMessage();
                int hand1_pos = 1;
                int hand2_pos = -1;
                String [] temp = antwort.split(";");
                // Croupier gibt Hand im folgenden Format aus: A 3 4;B B;3 4 6 oder A 3 4;B B;null
                if (!temp[2].equals(null)){
                    String [] tempHand = temp[0].split(" ");
                    System.out.println("Croupier: " + temp[0]);
                    for(int i = 0; i < hand1_pos; i++){
                        croupier[i] = tempHand[i];
                    }
                    System.out.println("Hand 1: " + temp[1]);
                    tempHand = temp[1].split(" ");
                    for(int i = 0; i < hand1_pos; i++){
                        hand1[i] = tempHand[i];
                    }
                    System.out.println("Hand 2: " + temp[2]);
                    tempHand = temp[2].split(" ");
                    for(int i = 0; i < hand1_pos; i++){
                        hand1[i] = tempHand[i];
                    }
                }
                else{
                    String [] tempHand = new String[15];
                    System.out.println("Croupier: " + temp[0]);
                    tempHand = temp[0].split(" ");
                    for(int i = 0; i < hand1_pos; i++){
                        croupier[i] = tempHand[i];
                    }
                    System.out.println("Hand: " + temp[1]);
                    tempHand = temp[1].split(" ");
                    for(int i = 0; i < hand1_pos; i++){
                        hand1[i] = tempHand[i];
                    }
                }
                System.out.println();
                System.out.println("Wähle deine Aktion:");
                System.out.println("[1] Hand 1");
                System.out.println("[2] Hand 2");
                System.out.println("[3] commands");
                System.out.print("Deine Wahl: ");
                int eingabe = scanner.nextInt();
                switch (eingabe) {
                    case 1:
                        System.out.println("Wähle deine Aktion:");
                        System.out.println("[1] Hit");
                        System.out.println("[2] Stand");
                        System.out.println("[3] Double Down");
                        System.out.println("[4] Split");
                        eingabe = scanner.nextInt();
                        int handwert;
                        switch (eingabe) {

                            case 1:
                                handwert = 0;
                                for (int i = 0; i < hand1_pos; i++) {
                                    handwert += Integer.parseInt(hand1[i]);
                                }
                                sendMessage(IP, "Croupier", "draw", String.valueOf(handwert));
                                //der Croupier prüft mit dem aktuellen Handwert, ob der Spieler überkauft ist
                                antwort = receiveMessage();
                                hand1_pos++;
                                hand1[hand1_pos]  = antwort.substring(antwort.length() - 2);
                                break;
                            case 2:
                                handwert = 0;
                                for (int i = 0; i < hand1_pos; i++) {
                                    handwert += Integer.parseInt(hand1[i]);
                                }
                                sendMessage(IP, "Croupier", "Stand", String.valueOf(handwert));
                                antwort = receiveMessage();
                                antwort = antwort.substring(antwort.lastIndexOf(" "));
                                if(antwort.startsWith("+")) {
                                    antwort = antwort.substring(1);
                                    kapital += Integer.parseInt(antwort);
                                }
                                else if(antwort.startsWith("-")) {
                                    antwort = antwort.substring(1);
                                    kapital -= Integer.parseInt(antwort);
                                }
                                break;
                            case 3:
                                kapital -= einsatz;
                                sendMessage(IP, "Croupier", " Hand1 Double Down", String.valueOf(kapital*2));
                                // der Croupier erhält den verdoppelten Wert des Einsatzes und überschreibt den alten Wert
                                break;
                            case 4:
                                if(hand2[0] != null){
                                    System.out.println("bereits gesplittet");
                                    break;
                                }
                                handwert = 0;
                                sendMessage(IP, "Croupier", "split", String.valueOf(handwert));
                                //der Croupier prüft mit dem aktuellen Handwert, ob der Spieler überkauft ist
                                antwort = receiveMessage();
                                hand2_pos++;
                                antwort  = antwort.substring(antwort.length() - 4);
                                String [] tempSplit = antwort.split(" ");
                                String tempKarte = hand1[1];
                                hand2[hand2_pos] = tempKarte;
                                hand2_pos++;
                                hand1[1] = tempSplit[0];
                                hand2[hand2_pos] = tempSplit[1];
                                //wir splitten die Hand 3 5 auf zu: 3 x und 5 y
                                break;


                        }

                        ; break;
                    case 2:
                        System.out.println("Wähle deine Aktion:");
                        System.out.println("[1] Hit");
                        System.out.println("[2] Stand");
                        System.out.println("[3] Double Down");
                        eingabe = scanner.nextInt();
                        int handwert2 = 0;
                        switch (eingabe) {

                            case 1:
                                handwert2 = 0;
                                for (int i = 0; i < hand2_pos; i++) {
                                    handwert2 += Integer.parseInt(hand2[i]);
                                }
                                sendMessage(IP, "Croupier", "draw", String.valueOf(handwert2));
                                //der Croupier prüft mit dem aktuellen Handwert, ob der Spieler überkauft ist
                                antwort = receiveMessage();
                                hand2_pos++;
                                hand2[hand2_pos]  = antwort.substring(antwort.length() - 2);
                                break;
                            case 2:
                                handwert2 = 0;
                                for (int i = 0; i < hand2_pos; i++) {
                                    handwert2 += Integer.parseInt(hand2[i]);
                                }
                                sendMessage(IP, "Croupier", "Stand", String.valueOf(handwert2));
                                antwort = receiveMessage();
                                antwort = antwort.substring(antwort.lastIndexOf(" "));
                                if(antwort.startsWith("+")) {
                                    antwort = antwort.substring(1);
                                    kapital += Integer.parseInt(antwort);
                                }
                                else if(antwort.startsWith("-")) {
                                    antwort = antwort.substring(1);
                                    kapital -= Integer.parseInt(antwort);
                                }
                                break;
                            case 3:
                                kapital -= einsatz;
                                sendMessage(IP, "Croupier", " Hand2 Double Down", String.valueOf(kapital*2));
                                // der Croupier erhält den verdoppelten Wert des Einsatzes und überschreibt den alten Wert
                                break;
                            }

                        break;
                    case 3:
                        System.out.println("Wähle deine Aktion:");
                        System.out.println("[1] Surrender");
                        System.out.println("[2] return");
                        eingabe = scanner.nextInt();
                        switch (eingabe) {
                            case 1:
                                command = "Surrender"; break;
                            case 2:
                                System.out.println("going back"); break;

                        }
                }
            }




        }

        socket.close();
    }

    private static void sendMessage(String sender, String empfänger, String command, String value) throws IOException {

        // 1) Nachricht zusammenbauen(⇒ "Alice;Croupier;bet;20")
        String payload = String.join(";", sender, empfänger, command, value);

        // 2) In Bytes kodieren (UTF-8)
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);

        // 3) Datagramm an den Croupier schicken
        InetAddress address = InetAddress.getByName(CROUPIER_IP);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, CROUPIER_PORT);

        socket.send(packet);
    }

    private static String receiveMessage() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return new String(packet.getData(), 0, packet.getLength());
    }

}
