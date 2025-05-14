import java.io.IOException;
import java.net.*;
import java.util.LinkedList;


public class TokenRing {

    // if a a node failed to send a receipt, resend flag is set to true
    private static boolean RESEND_FLAG = false;
    private static final int TIMEOUT = 5000;

    private static void loop(DatagramSocket socket, String ip, int port, boolean first){
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();

        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        try {

            // added to code
            socket.setSoTimeout(TIMEOUT);

            Token rc = null;

            while (true) {
                try {
                    if (!RESEND_FLAG){

                        rc = Token.receive(socket);

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
                    }
                    Token.Endpoint next = rc.poll();
                    rc.append(next);
                    rc.incrementSequence();
                    Thread.sleep(1000);

                    if(rc.length()>1)
                        rc.sendReceipt(socket, Token.last);

                    rc.send(socket, next);

                    if(rc.length() > 1){
                        if(!waitForReceipt(socket, next)){
                            if(rc != null && rc.length() > 1){
                                RESEND_FLAG = true;
                                rc.drop();
                            }
                        else
                            RESEND_FLAG = false;
                        }
                    } else
                        RESEND_FLAG = false;
                }
                catch (SocketTimeoutException e){
                    // pass
                }
                catch (IOException e) {
                    System.out.println("Error receiving packet: " + e.getMessage());
                }
                catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch(SocketException e){
            System.out.println("Error creating Socker: " + e.getMessage());
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


    private static boolean waitForReceipt(DatagramSocket socket, Token.Endpoint next){

        /** waits for a receipt as proof a package arrived */

        try{
            Token.receiveReceipt(socket);
            return true;
        } catch(SocketTimeoutException e){
            return false;
        } catch(IOException e){
            return false;
        }
    }
    
}