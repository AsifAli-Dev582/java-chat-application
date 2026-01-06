import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1112122200L;

    public static final int MESSAGE = 1;
    public static final int LOGOUT  = 2;
    public static final int WHOISIN = 3;
    public static final int FILE    = 4; // audio/video

    private int type;
    private String message;
    private byte[] fileData;
    private String fileName;

    // Text constructor
    public ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // File constructor
    public ChatMessage(int type, byte[] fileData, String fileName) {
        this.type = type;
        this.fileData = fileData;
        this.fileName = fileName;
    }

    public int getType() { return type; }
    public String getMessage() { return message; }
    public byte[] getFileData() { return fileData; }
    public String getFileName() { return fileName; }
}
