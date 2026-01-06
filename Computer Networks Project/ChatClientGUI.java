import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
public class ChatClientGUI extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, sendFileButton, whoIsInButton, logoutButton;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private String username;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private FileWriter logWriter;

    public ChatClientGUI(String serverIp, int port, String username) {
        this.username = username;
        setTitle("Chat Client - " + username);
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);

        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendFileButton = new JButton("Send File");
        whoIsInButton = new JButton("WHOISIN");
        logoutButton = new JButton("Logout");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);
        buttonPanel.add(whoIsInButton);
        buttonPanel.add(logoutButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(scroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Connect to server
        try {
            socket = new Socket(serverIp, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(username); // send username

            new ListenFromServer().start();

            // Log file
            logWriter = new FileWriter("chatlog_" + username + ".txt", true);

            appendMessage("[" + sdf.format(new Date()) + "] Connected to server.");
        } catch (Exception e) {
            appendMessage("Error connecting to server: " + e.getMessage());
        }

        // Button actions
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        sendFileButton.addActionListener(e -> sendFile());
        whoIsInButton.addActionListener(e -> sendWhoIsIn());
        logoutButton.addActionListener(e -> logout());

        setVisible(true);
    }

    private void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        try { logWriter.write(msg + "\n"); logWriter.flush(); } catch (IOException ignored) {}
    }

    private void sendMessage() {
        String msg = inputField.getText().trim();
        if (!msg.isEmpty()) {
            try {
                out.writeObject(new ChatMessage(ChatMessage.MESSAGE, msg));
                out.flush();
                inputField.setText("");
            } catch (IOException e) {
                appendMessage("Error sending message: " + e.getMessage());
            }
        }
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                out.writeObject(new ChatMessage(ChatMessage.FILE, data, file.getName()));
                out.flush();
                appendMessage("[" + sdf.format(new Date()) + "] File sent: " + file.getName());
            } catch (IOException e) {
                appendMessage("Error sending file: " + e.getMessage());
            }
        }
    }

    private void sendWhoIsIn() {
        try {
            out.writeObject(new ChatMessage(ChatMessage.WHOISIN, ""));
            out.flush();
        } catch (IOException e) {
            appendMessage("Error requesting user list: " + e.getMessage());
        }
    }

    private void logout() {
        try {
            out.writeObject(new ChatMessage(ChatMessage.LOGOUT, ""));
            out.flush();
            socket.close();
            appendMessage("Logged out.");
            System.exit(0);
        } catch (IOException e) {
            appendMessage("Error during logout: " + e.getMessage());
        }
    }

    // Thread to listen from server
    class ListenFromServer extends Thread {
        public void run() {
            try {
                while (true) {
                    Object o = in.readObject();
                    if (o instanceof ChatMessage) {
                        ChatMessage cm = (ChatMessage) o;
                        if (cm.getType() == ChatMessage.MESSAGE) {
                            appendMessage(cm.getMessage());
                        } else if (cm.getType() == ChatMessage.FILE) {
                            Path p = Paths.get("received_" + cm.getFileName());
                            Files.write(p, cm.getFileData());
                            appendMessage("[" + sdf.format(new Date()) + "] Received file: " + cm.getFileName());
                            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(p.toFile());
                        }
                    }
                }
            } catch (Exception e) {
                appendMessage("Connection closed.");
            }
        }
    }

    public static void main(String[] args) {
        String serverIp = JOptionPane.showInputDialog("Enter server IP (localhost):");
        if (serverIp == null || serverIp.isEmpty()) serverIp = "localhost";
        String portStr = JOptionPane.showInputDialog("Enter port (1500):");
        int port = portStr == null || portStr.isEmpty() ? 1500 : Integer.parseInt(portStr);
        String username = JOptionPane.showInputDialog("Enter username:");
        if (username == null || username.isEmpty()) username = "Anonymous";

        new ChatClientGUI(serverIp, port, username);
    }
}
