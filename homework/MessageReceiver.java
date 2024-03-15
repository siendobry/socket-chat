package homework;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static homework.StandardOutput.outputMessage;

public class MessageReceiver {

    public static Thread getTcpListener(
            Socket socket,
            BufferedReader in,
            PrintWriter out,
            String username,
            Client client
    ) {
        Thread shutdown = new Thread(() -> {
            synchronized (socket) {
                if (!socket.isClosed()) {
                    out.close();
                }
            }
        });

        return new Thread(() -> {
            Runtime.getRuntime().addShutdownHook(shutdown);

            try {
                while (true) {
                    String message = in.readLine();
                    if (message == null) {
                        client.handleServerShutdown();
                        break;
                    }

                    outputMessage(message, username);
                }
            } catch (SocketException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static Thread getUdpListener(
            DatagramSocket socket,
            String username
    ) {

        Thread shutdown = new Thread(() -> {
            synchronized (socket) {
                if (!socket.isClosed()) {
                    socket.close();
                }
            }
        });

        return new Thread(() -> {
            Runtime.getRuntime().addShutdownHook(shutdown);

            packetReceiver(socket, username);
        });
    }

    public static Thread getMulticastListener(
            MulticastSocket socket,
            String username,
            InetAddress multicastAddress
    ) {
        Thread shutdown = new Thread(() -> {
            try {
                synchronized (socket) {
                    if (!socket.isClosed()) {
                        socket.leaveGroup(multicastAddress);
                        socket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return new Thread(() -> {
            Runtime.getRuntime().addShutdownHook(shutdown);

            packetReceiver(socket, username);
        });
    }

    private static void packetReceiver(DatagramSocket socket, String username) {
        try {
            byte[] buffer = new byte[1024];

            while (true) {
                Arrays.fill(buffer, (byte) '\0');
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), StandardCharsets.UTF_8);

                outputMessage(message, username);
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
