package homework;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Server {

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private final int portNumber = 4040;
    private ServerSocket tcpSocket = null;
    private DatagramSocket udpSocket = null;
    private Thread udpListener = null;
    private final Set<TcpServerThread> clientHandlers = Collections.synchronizedSet(new HashSet<>());


    private void start() throws IOException {
        Runtime.getRuntime().addShutdownHook(shutdown());

        tcpSocket = new ServerSocket(portNumber);
        udpSocket = new DatagramSocket(portNumber);

        udpListener = new UdpServerThread(udpSocket, this);
        udpListener.start();

        System.out.println("SERVER STARTED");

        try {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                System.out.println("Client connected");

                TcpServerThread clientHandler = new TcpServerThread(clientSocket, this);
                synchronized (clientHandlers) {clientHandlers.add(clientHandler);}
                clientHandler.start();
            }
        } catch (SocketException ignored) {
        }

        System.out.println("SERVER SHUT DOWN");
    }

    protected Set<TcpServerThread> getClientHandlers() {
        return Collections.unmodifiableSet(clientHandlers);
    }

    protected synchronized void removeThread(TcpServerThread thread) {
        clientHandlers.remove(thread);
    }

    private Thread shutdown() {
        Thread currentThread = Thread.currentThread();

        return new Thread(() -> {

            try {
                if (tcpSocket != null) {
                    tcpSocket.close();
                }

                synchronized (clientHandlers) {
                    for (Thread clientHandler : clientHandlers) {
                        clientHandler.join();
                    }
                }
                if (udpListener != null) {
                    udpListener.join();
                }
                currentThread.join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println();
            }
        });
    }
}
