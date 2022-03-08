import java.io.*;
public class ChatMessage implements Serializable {

    static final int ACTIVEUSERS = 0, MESSAGE = 1, EXIT = 2;
    private int type;
    private String message;

    ChatMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // get the type of message
    int getType() {
        return type;
    }

    // get the message
    String getMessage() {
        return message;
    }
}