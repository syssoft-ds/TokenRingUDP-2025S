package ueb_03.UDP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Main {

    public static ArrayList<String> Kontakte = new ArrayList<>();
    public static ArrayList<Integer> Ports = new ArrayList<>();

    private static void fatal ( String comment ) {
        System.out.println(comment);
        System.exit(-1);
    }

    // ************************************************************************
    // MAIN
    // ************************************************************************
    public static void main(String[] args) throws IOException {
        if (args.length != 3 && args.length != 2)
            fatal("Usage: \"<netcat> -l <port> \" or \"netcat <ip> <port> <name>\"");
        int port = Integer.parseInt(args[1]);
        String name = null;
        if (args.length == 3){
            name = args[2];
        }
        if (args[0].equalsIgnoreCase("-l"))
            listenAndTalk(port);
        else
            connectAndTalk(args[0],port, name);
    }

    private static final int packetSize = 4096;

    // ************************************************************************
    // listenAndTalk
    // ************************************************************************
    private static void listenAndTalk ( int port ) throws IOException  {
        DatagramSocket s = new DatagramSocket(port);
        byte[] buffer = new byte[packetSize];
        String line;
        do {
            DatagramPacket p = new DatagramPacket(buffer,buffer.length);
            s.receive(p);
            line = new String(buffer,0,p.getLength(),"UTF-8");
            System.out.println(line);
        } while (!line.equalsIgnoreCase("stop"));
        s.close();
    }

    // ************************************************************************
    // connectAndTalk
    // ************************************************************************
    private static void connectAndTalk ( String other_host, int other_port , String name) throws IOException {
        InetAddress other_address = InetAddress.getByName(other_host);
        DatagramSocket s = new DatagramSocket();
        byte[] buffer = new byte[packetSize];
        String line;
        //Vorstellung eines neuen Teilnehmers
        line = "Hallo hier ist " + name + ", meine IP-Adresse ist 127.0.0.1 und du kannst mich unter Port-Nummer " + s.getLocalPort() + " erreichen.";
        Kontakte.add(name);
        Ports.add(s.getLocalPort());
        buffer = line.getBytes("UTF-8");
        DatagramPacket hello = new DatagramPacket(buffer,buffer.length,other_address,other_port);
        s.send(hello);
        //-----------------------------------
        do {
            line = name + ": " +readString();
            //falls eine Nachricht gesendet werden soll
            if(line.startsWith(name + ": send")){
                line = line.substring(name.length()+5);
                String partner = line.substring(0,line.indexOf(" "));
                String message = line.substring(line.indexOf(" ")+1);
                if(Kontakte.contains(partner)){
                    buffer = message.getBytes("UTF-8");
                    DatagramPacket p = new DatagramPacket(buffer,buffer.length,other_address,Ports.get(Kontakte.indexOf(partner)));
                    s.send(p);
                }
            }
            //------------------------------------------
            else{
                buffer = line.getBytes("UTF-8");
                DatagramPacket p = new DatagramPacket(buffer,buffer.length,other_address,other_port);
                s.send(p);
            }
        } while (!line.equalsIgnoreCase(name + ": stop"));
        //Person verläst den Chat
        line = name + " has left";
        buffer = line.getBytes("UTF-8");
        DatagramPacket p = new DatagramPacket(buffer,buffer.length,other_address,other_port);
        s.send(p);
        //-----------------------
        s.close();
    }

    private static String readString () {
        BufferedReader br = null;
        boolean again = false;
        String input = null;
        do {
            // System.out.print("Input: ");
            try {
                if (br == null)
                    br = new BufferedReader(new InputStreamReader(System.in));
                input = br.readLine();
            }
            catch (Exception e) {
                System.out.printf("Exception: %s\n",e.getMessage());
                again = true;
            }
        } while (again);
        return input;
    }

    private BufferedReader br = null;
}
