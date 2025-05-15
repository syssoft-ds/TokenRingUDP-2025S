import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class Token {

    private static final int max_buffer_size = 4096;

    public record Endpoint(String ip, int port) {}

    public Token append(String ip, int port) {
        ring.offer(new Endpoint(ip, port));
        return this;
    }

    public Token append(Endpoint endpoint) {
        ring.offer(endpoint);
        return this;
    }

    public Endpoint first() {
        return ring.peek();
    }

    public Endpoint poll() {
        return ring.poll();
    }

    public int length () {
        return ring.size();
    }

    private int sequence = 0;

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void incrementSequence() {
        sequence++;
    }

    public void send (DatagramSocket s, String ip_address, int port ) throws IOException {
        String rc_json = toJSON();
        byte[] rc_json_bytes = rc_json.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(ip_address);
        DatagramPacket packet = new DatagramPacket(rc_json_bytes, rc_json_bytes.length, address, port);
        System.out.printf("Sending %s to %s:%d\n", rc_json, ip_address, port);
        s.send(packet);
    }

    public void send (DatagramSocket s, Endpoint endpoint) throws IOException {
        send(s, endpoint.ip(), endpoint.port());
    }


    public static Token receive(DatagramSocket s) throws IOException {
        byte[] buf = new byte[max_buffer_size];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        s.receive(packet);
        String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

        // Check if the received packet is a "ping"
        if ("ping".equals(receivedData)) {
            System.out.printf("Received ping from %s:%d\n", packet.getAddress().getHostAddress(), packet.getPort());
            // Respond with "pong"
            String pongMessage = "pong";
            byte[] pongBytes = pongMessage.getBytes(StandardCharsets.UTF_8);
            DatagramPacket pongPacket = new DatagramPacket(pongBytes, pongBytes.length, packet.getAddress(), packet.getPort());
            s.send(pongPacket);
            System.out.printf("Return Pong to Sender\n");
            return null; // No token to process
        }

        // Otherwise, process the packet as a normal token
        System.out.printf("Received %s from %s:%d\n", receivedData, packet.getAddress().getHostAddress(), packet.getPort());
        return fromJSON(receivedData);
    }

    public boolean sendPing(DatagramSocket socket, Endpoint endpoint) {
        try {
            // Send a "ping" message
            String pingMessage = "ping";
            byte[] pingBytes = pingMessage.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(endpoint.ip());
            DatagramPacket pingPacket = new DatagramPacket(pingBytes, pingBytes.length, address, endpoint.port());
            socket.send(pingPacket);
            System.out.printf("Sent ping to %s:%d\n", endpoint.ip(), endpoint.port());

            // Wait for a "pong" response
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.setSoTimeout(2000); // 2-second timeout for the response
            socket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
            if ("pong".equals(response)) {
                System.out.printf("Received pong from next Node\n", responsePacket.getAddress().getHostAddress(), responsePacket.getPort());
                return true; // Node is alive
            } else {
                System.out.printf("Unexpected response: %s from %s:%d\n", response, responsePacket.getAddress().getHostAddress(), responsePacket.getPort());
            }
        } catch (SocketTimeoutException e) {
            System.out.printf("No response from %s:%d. Node is considered dead.\n", endpoint.ip(), endpoint.port());
        } catch (IOException e) {
            System.out.println("Error during ping: " + e.getMessage());
        }
        return false; // Node is considered dead
    }

    //Entfernt ein Element aus dem Ring wenn ip und port übereinstimmen
    public void remove(String ip, int port) {
        for(int i = 0; i < ring.size(); i++) {
            Endpoint endpoint = ring.poll();
            if (Objects.equals(endpoint.ip(), ip) && endpoint.port() == port) {
                System.out.printf("Removing %s:%d\n", ip, port);
                continue;
            }
            ring.add(endpoint);
        }
    }

    @JsonProperty
    private final Queue<Endpoint> ring = new LinkedList<>();

    public Queue<Endpoint> getRing() {
        return ring;
    }

    private static final ObjectMapper serializer = new ObjectMapper();

    public String toJSON() throws JsonProcessingException {
        return serializer.writeValueAsString(this);
    }

    public static Token fromJSON(String json) throws IOException {
        return serializer.readValue(json, Token.class);
    }
}
