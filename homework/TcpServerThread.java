package homework;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class TcpServerThread extends Thread {

    private String username = null;

    private final Socket clientSocket;
    private final Server server;
    private PrintWriter out;
    private BufferedReader in;

    public TcpServerThread(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(shutdown());

        try {
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            String message;
            int delimPos;
            String messageContents;

            while (true) {
                message = in.readLine();
                if (message == null) {
                    System.out.println("User " + username + " disconnected from the chat");
                    username = null;
                    break;
                }

                delimPos = message.indexOf(":");

                if (delimPos == -1) {
                    username = message;
                    System.out.println("User " + username + " connected to the chat");
                    broadcastMessage("User " + username + " connected to the chat");
                } else {
                    messageContents = message.substring(delimPos + 2);
                    System.out.println("TCP | " + username + " sent a message: '" + messageContents + "'");
                    broadcastMessage(message);
                }


            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (server.getClientHandlers()) {
                server.removeThread(this);
            }

            if (!clientSocket.isClosed()) {
                out.close();
            }
        }

    }

    public InetAddress getInetAddress() {
        return clientSocket.getInetAddress();
    }

    public int getPortNumber() {
        return clientSocket.getPort();
    }

    protected synchronized void sendToClient(String message) {
        out.println(message);
    }

    private synchronized void broadcastMessage(String message) {
        for (TcpServerThread clientHandler : server.getClientHandlers()) {
            if (clientHandler != this) {
                clientHandler.out.println(message);
            }
        }
    }

    private Thread shutdown() {
        return new Thread(() -> {
            if (username != null) {
                System.out.println("User " + username + " disconnected from the chat");
            }

            if (!clientSocket.isClosed()) {
                out.close();
            }
        });
    }
}
