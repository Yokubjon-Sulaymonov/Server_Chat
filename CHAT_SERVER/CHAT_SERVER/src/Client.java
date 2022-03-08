import java.net.*;
import java.io.*;
import java.util.*;

public class Client  {
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;                  // socket of the chat
    private String server;                  // server of the chat
    private String username;                // username of client
    private int port;					    // port number

    //getting username
    public String getUsername() {
        return username;
    }

    //setting username
    public void setUsername(String username) {
        this.username = username;
    }

    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    //starting the chat
    public boolean start() {
        try {
            socket = new Socket(server, port);
        }
        catch(Exception ec) {
            display("" + ec);
            return false;
        }

        try
        {
            // creating both data stream
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("" + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();

        //All messages are ChatMessage Object but username
        try
        {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            display("" + eIO);
            close();
            return false;
        }
        return true;
    }

    // printing the message
    private void display(String msg) {
        System.out.println(msg);
    }

    // send message
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            display("" + e);
        }
    }

    //close everything
    private void close() {
        try {
            if(sInput != null) sInput.close();
            if(sOutput != null) sOutput.close();
            if(socket != null) socket.close();
        }
        catch(Exception e) {
            display("" + e);
        }
    }

    public static void main(String[] args) {
        int portNumber = 4561;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        Scanner keyboard = new Scanner(System.in);
        System.out.print("Enter the username: ");
        userName = keyboard.nextLine();

        // different case according to the length of the arguments.


        Client client = new Client(serverAddress, portNumber, userName);

        // if not connected returns
        if(!client.start()) {
            return;
        }

        // print instruction
        instruction();

        // infinite loop to get the input from the user
        while(true) {
            System.out.print("> ");
            String msg = keyboard.nextLine();

            // if message is EXIT keyword
            if(msg.equalsIgnoreCase("EXIT")) {
                client.sendMessage(new ChatMessage(ChatMessage.EXIT, ""));
                break;
            }
            // else if message is ACTIVEUSERS keyword
            else if(msg.equalsIgnoreCase("ACTIVEUSERS")) {
                client.sendMessage(new ChatMessage(ChatMessage.ACTIVEUSERS, ""));
            }

            // else it is simple message
            else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        keyboard.close();
        client.close();
    }

    protected static void instruction() {
        System.out.println("Hello Hello");
        System.out.println("Welcome to the chatroom!");
        System.out.println("Instruction for usage:");
        System.out.println("1. Enter the message to send broadcast message to all clients");
        System.out.println("2. Enter '@username<space>yourMessage' without quotes to send private message");
        System.out.println("3. Enter 'ACTIVEUSERS' without quotes to see list of active clients");
        System.out.println("4. ENTER 'EXIT' without quotes to exit the server\n\n");
    }

    // to get the message from the server
    class ListenFromServer extends Thread {
        public void run() {
            while(true) {
                try {
                    // read the message
                    String msg = (String) sInput.readObject();
                    // print the message
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    display("Server has closed the connection: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    System.out.println(e2);
                }
            }
        }
    }
}