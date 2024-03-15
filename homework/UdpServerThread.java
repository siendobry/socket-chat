package homework;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class UdpServerThread extends Thread {

    private final DatagramSocket socket;
    private final Server server;

    public UdpServerThread(DatagramSocket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(shutdown());

        try {
            byte[] buffer = new byte[1024];

            String message;
            int delimPos;
            String username;
            String messageContents;

            while (true) {
                Arrays.fill(buffer, (byte)'\0');
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
                message = new String(packet.getData(), StandardCharsets.UTF_8);

                delimPos = message.indexOf(":");
                username = message.substring(0, delimPos);
                messageContents = message.substring(delimPos + 2);
                System.out.println("UDP | " + username + " sent a message: '" + messageContents + "'");

                broadcastMessage(message, packet.getAddress(), packet.getPort());
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected synchronized void broadcastMessage(String message, InetAddress senderAddress, int senderPort) throws IOException {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

        for (TcpServerThread clientHandler : server.getClientHandlers()) {
            InetAddress clientInetAddress = clientHandler.getInetAddress();
            int clientPortNumber = clientHandler.getPortNumber();

            if (!Objects.equals(senderAddress, clientInetAddress) || senderPort != clientPortNumber) {
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        clientInetAddress, clientPortNumber
                );
                socket.send(packet);
            }
        }
    }

    private Thread shutdown() {
        return new Thread(socket::close);
    }
}
