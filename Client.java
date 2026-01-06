import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private String username;
    private final Scanner sc = new Scanner(System.in);

    public Client(String serverIp, int port, String username) throws Exception {
        this.username = username;
        socket = new Socket(serverIp, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        out.writeObject(username); // send username
        new ListenFromServer().start();
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Connected to server " + serverIp + ":" + port);
    }

    public void sendText(String text) throws IOException {
        out.writeObject(new ChatMessage(ChatMessage.MESSAGE, text));
        out.flush();
    }

    public void sendFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + path);
            return;
        }
        byte[] data = Files.readAllBytes(file.toPath());
        out.writeObject(new ChatMessage(ChatMessage.FILE, data, file.getName()));
        out.flush();
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] File sent: " + file.getName());
    }

    public void whoIsIn() throws IOException {
        out.writeObject(new ChatMessage(ChatMessage.WHOISIN, ""));
        out.flush();
    }

    public void logout() throws IOException {
        out.writeObject(new ChatMessage(ChatMessage.LOGOUT, ""));
        out.flush();
        socket.close();
    }

    class ListenFromServer extends Thread {
        public void run() {
            try {
                while (true) {
                    Object o = in.readObject();
                    if (o instanceof ChatMessage) {
                        ChatMessage cm = (ChatMessage) o;
                        if (cm.getType() == ChatMessage.MESSAGE) {
                            System.out.println(cm.getMessage());
                        } else if (cm.getType() == ChatMessage.FILE) {
                            Path p = Paths.get("received_" + cm.getFileName());
                            Files.write(p, cm.getFileData());
                            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
                            System.out.println("[" + time + "] Received file: " + cm.getFileName());
                            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(p.toFile()); } 
                            catch (IOException e) { System.out.println("Could not open file automatically."); }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Connection closed.");
            }
        }
    }

    public void consoleLoop() throws Exception {
        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1) Send text message (use @username for private)");
            System.out.println("2) Send file (audio/video)");
            System.out.println("3) WHOISIN");
            System.out.println("4) LOGOUT and exit");
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    System.out.print("Enter message: ");
                    sendText(sc.nextLine());
                    break;
                case "2":
                    System.out.print("Enter full file path: ");
                    sendFile(sc.nextLine());
                    break;
                case "3":
                    whoIsIn();
                    break;
                case "4":
                    logout();
                    System.exit(0);
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    public static void main(String[] args) {
        try {
            Scanner s = new Scanner(System.in);
            System.out.print("Server IP (enter for localhost): ");
            String ip = s.nextLine().trim();
            if (ip.isEmpty()) ip = "localhost";

            System.out.print("Port (enter for 1500): ");
            String portStr = s.nextLine().trim();
            int port = portStr.isEmpty() ? 1500 : Integer.parseInt(portStr);

            System.out.print("Username (no spaces): ");
            String user = s.nextLine().trim();
            if (user.isEmpty()) user = "Anonymous";

            Client c = new Client(ip, port, user);
            c.consoleLoop();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
