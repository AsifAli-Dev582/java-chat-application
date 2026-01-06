import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class Server {
    private static int uniqueId;
    private final ArrayList<ClientThread> clients;
    private final int port;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    public Server(int port) {
        this.port = port;
        clients = new ArrayList<>();
    }
    public void start() {
        System.out.println("[" + sdf.format(new Date()) + "] Server starting on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientThread t = new ClientThread(socket);
                clients.add(t);
                t.start();
            }
        } catch (IOException e) {
            System.out.println("[" + sdf.format(new Date()) + "] Server error: " + e);
        }
    }
    private synchronized void broadcast(ChatMessage cm, String sender) {
        for (ClientThread ct : clients) {
            try {
                if (!ct.username.equals(sender)) {
                    ct.out.writeObject(cm);
                    ct.out.flush();
                }
            } catch (IOException e) {
                clients.remove(ct);
                System.out.println("[" + sdf.format(new Date()) + "] Removed client " + ct.username);
            }
        }
    }
    private synchronized boolean sendPrivate(String toUser, ChatMessage cm) throws IOException {
        for (ClientThread ct : clients) {
            if (ct.username.equalsIgnoreCase(toUser)) {
                ct.out.writeObject(cm);
                ct.out.flush();
                return true;
            }
        }
        return false;
    }
    public static void main(String[] args) {
        int port = 1500;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception e) {}
        }
        new Server(port).start();
    }
    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;
        String username;
        int id;
        ClientThread(Socket socket) {
            this.socket = socket;
            this.id = ++uniqueId;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                Object o = in.readObject();
                username = (o instanceof String) ? (String) o : "Guest" + id;
                String time = sdf.format(new Date());
                System.out.println("[" + time + "] " + username + " joined the chat");
                broadcast(new ChatMessage(ChatMessage.MESSAGE, "[" + time + "] *** " + username + " joined the chat ***"), username);
            } catch (Exception e) {
                System.out.println("[" + sdf.format(new Date()) + "] Error creating client streams: " + e);
            }
        }
        public void run() {
            try {
                while (true) {
                    Object o = in.readObject();
                    if (o instanceof ChatMessage) {
                        ChatMessage cm = (ChatMessage) o;
                        String timestamp = sdf.format(new Date());

                        switch (cm.getType()) {
                            case ChatMessage.MESSAGE:
                                String msg = cm.getMessage();
                                if (msg.startsWith("@")) { // Private message
                                    int space = msg.indexOf(' ');
                                    if (space != -1) {
                                        String toUser = msg.substring(1, space);
                                        String text = msg.substring(space + 1);
                                        boolean success = sendPrivate(toUser,
                                                new ChatMessage(ChatMessage.MESSAGE,
                                                        "[" + timestamp + "] [Private] " + username + ": " + text));
                                        if (!success)
                                            out.writeObject(new ChatMessage(ChatMessage.MESSAGE,
                                                    "[" + timestamp + "] User " + toUser + " not found."));
                                    }
                                } else {
                                    ChatMessage broadcastMsg = new ChatMessage(ChatMessage.MESSAGE,
                                            "[" + timestamp + "] " + username + ": " + msg);
                                    broadcast(broadcastMsg, username);
                                    out.writeObject(broadcastMsg); // show sender's message too
                                    out.flush();
                                    System.out.println("[" + timestamp + "] " + username + ": " + msg); // server log
                                }
                                break;

                            case ChatMessage.FILE:
                                broadcast(new ChatMessage(ChatMessage.FILE, cm.getFileData(), cm.getFileName()), username);
                                ChatMessage fileMsg = new ChatMessage(ChatMessage.MESSAGE,
                                        "[" + timestamp + "] " + username + " sent file: " + cm.getFileName());
                                broadcast(fileMsg, username);
                                out.writeObject(fileMsg);
                                out.flush();
                                System.out.println("[" + timestamp + "] " + username + " sent file: " + cm.getFileName());
                                break;

                            case ChatMessage.LOGOUT:
                                ChatMessage logoutMsg = new ChatMessage(ChatMessage.MESSAGE,
                                        "[" + timestamp + "] *** " + username + " has left ***");
                                broadcast(logoutMsg, username);
                                out.writeObject(logoutMsg);
                                cleanup();
                                return;

                            case ChatMessage.WHOISIN:
                                StringBuilder sb = new StringBuilder("Connected users:");
                                for (ClientThread ct : clients) sb.append("\n - ").append(ct.username);
                                out.writeObject(new ChatMessage(ChatMessage.MESSAGE, sb.toString()));
                                out.flush();
                                break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("[" + sdf.format(new Date()) + "] Connection lost with " + username);
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
            try { if (socket != null) socket.close(); } catch (Exception e) {}
            clients.remove(this);
            System.out.println("[" + sdf.format(new Date()) + "] Client " + username + " disconnected.");
        }
    }
}
