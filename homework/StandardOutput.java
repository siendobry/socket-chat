package homework;

public class StandardOutput {

    public static synchronized void logMessage(String message) {
        clearLine();
        System.out.println(message);
    }

    public static synchronized void outputMessage(String message, String username) {
        logMessage(message);
        outputUsername(username);
    }

    public static synchronized void outputUsername(String username) {
        clearLine();
        System.out.print(username + ": ");
    }

    private static synchronized void clearLine() {
        System.out.print("\033[2K\033[1G");
        System.out.flush();
    }
}
