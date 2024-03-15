package homework;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static homework.MessageReceiver.*;
import static homework.StandardOutput.*;

public class Client {

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private String username = null;

    private final String hostName = "localhost";
    private final InetAddress hostAddress;
    private final int hostPortNumber = 4040;
    private final InetAddress multicastAddress;
    private final int multicastPortNumber = 5050;
    private Socket tcpSocket = null;
    private DatagramSocket udpSocket = null;
    private MulticastSocket multicastSocket = null;

    PrintWriter out = null;
    BufferedReader in = null;
    BufferedReader stdIn = null;
    private Thread tcpListener = null;
    private Thread udpListener = null;
    private Thread multicastListener = null;

    public Client() throws UnknownHostException {
        this.hostAddress = InetAddress.getByName(hostName);
        this.multicastAddress = InetAddress.getByName("224.0.0.1");
    }

    private void start() throws IOException, InterruptedException {
        Runtime.getRuntime().addShutdownHook(shutdown());

        stdIn = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        System.out.print("Enter your username: ");
        username = stdIn.readLine();
        if (username == null) {
            return;
        }

        connectToServer();

        multicastSocket = new MulticastSocket(multicastPortNumber);
        multicastSocket.joinGroup(multicastAddress);
        multicastListener = getMulticastListener(multicastSocket, username, multicastAddress);
        multicastListener.start();

        String userInput;
        boolean continueExecution;

        try {
            do {
                outputUsername(username);
                userInput = stdIn.readLine();
                continueExecution = handleUserInput(userInput);
            } while (continueExecution);
        } catch (IOException ignore) {}
    }

    private void connectToServer() throws IOException {
        while (true) {
            try {
                tcpSocket = new Socket(hostName, hostPortNumber);
                udpSocket = new DatagramSocket(tcpSocket.getLocalPort());
                break;
            } catch (BindException ignore) {
                tcpSocket.close();
            } catch (ConnectException e) {
                logMessage("Connection with the server cannot be established");
                logMessage("Only multicast communication is available");
                break;
            }
        }

        if (tcpSocket != null && !tcpSocket.isClosed() && tcpSocket.isConnected()) {
            out = new PrintWriter(new OutputStreamWriter(tcpSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream(), StandardCharsets.UTF_8));
            tcpListener = getTcpListener(tcpSocket, in, out, username, this);
            tcpListener.start();

            udpListener = getUdpListener(udpSocket, username);
            udpListener.start();

            out.println(username);
            System.out.println("Connected to chat");
        }
    }

    private boolean handleUserInput(String userInput) throws IOException {
        if (userInput == null) {
            shutdownSequence();
            return false;
        }

        switch (userInput) {
            case "-Q" -> {
                shutdownSequence();
                return false;
            }
            case "-C" -> {
                if (tcpSocket == null || tcpSocket.isClosed() || tcpSocket.isConnected()) {
                    connectToServer();
                } else {
                    logMessage("Already connected");
                }

                return true;
            }

            default -> {
                CommunicationType protocol;
                if (userInput.length() > 3) {
                    protocol = getCommunicationType(userInput.substring(0, 3));
                } else {
                    protocol = CommunicationType.TCP;
                }

                String message = switch (protocol) {
                    case TCP -> username + ": " + userInput;
                    case UDP, MULTICAST -> username + ": " + userInput.substring(3);
                };

                sendMessageWithProtocol(message, protocol);

                return true;
            }
        }
    }

    private void sendMessageWithProtocol(String message, CommunicationType protocol) throws IOException {
        switch (protocol) {
            case TCP -> {
                if (tcpSocket == null || tcpSocket.isClosed()) {
                    logMessage("Message not sent - TCP communication unavailable");
                    break;
                }

                out.println(message);
            }
            case UDP -> {
                if (udpSocket == null || udpSocket.isClosed()) {
                    logMessage("Message not sent - UDP communication unavailable");
                    break;
                }

                byte[] byteArr = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        byteArr, byteArr.length,
                        hostAddress, hostPortNumber
                );
                udpSocket.send(packet);
            }
            case MULTICAST -> {
                if (multicastSocket == null || multicastSocket.isClosed()) {
                    logMessage("Message not sent - multicast communication unavailable");
                    break;
                }

                byte[] byteArr = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(
                        byteArr, byteArr.length,
                        multicastAddress, multicastPortNumber
                );
                multicastSocket.send(packet);
            }
        }
    }

    private CommunicationType getCommunicationType(String protocolIdentifier) {
        CommunicationType communicationType;

        switch (protocolIdentifier) {
            case "-U " -> communicationType = CommunicationType.UDP;
            case "-M " -> communicationType = CommunicationType.MULTICAST;
            default -> communicationType = CommunicationType.TCP;
        }

        return communicationType;
    }

    protected void handleServerShutdown() {
        logMessage("Server has been shut down");

        if (tcpSocket != null && !tcpSocket.isClosed()) {
            out.close();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        logMessage("Only multicast communication is now available");
        outputUsername(username);
    }

    private void shutdownSequence() {
        logMessage("Quitting");

        try {
            stdIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (tcpSocket != null && !tcpSocket.isClosed()) {
            out.close();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            multicastSocket.close();
        }
    }

    private Thread shutdown() {
        Thread currentThread = Thread.currentThread();

        return new Thread(() -> {
            try {
                if (stdIn != null) {
                    stdIn.close();
                }

                for (Thread thread : Arrays.asList(tcpListener, udpListener, multicastListener, currentThread)) {
                    if (thread != null) {
                        thread.join();
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println();
            }
        });
    }
}
