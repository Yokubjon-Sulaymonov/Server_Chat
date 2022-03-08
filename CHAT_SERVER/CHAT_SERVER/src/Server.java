import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static int uniqueId;                // id for each client
    private ArrayList<ClientThread> clients;    // list of Client
    private SimpleDateFormat sdf;               // to get current time
    private int port;                           // port number
    private boolean connected;                  // to check if server is connected

    public Server(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        clients = new ArrayList<ClientThread>();
    }

    // starting the server
    public void start() {
        connected = true;
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for Clients connection
            while(connected)
            {
                Socket socket = serverSocket.accept();              // accept clients
                if(!connected)
                    break;                                          // if client is not connected, loop breaks
                ClientThread t = new ClientThread(socket);          // thread for each Client
                clients.add(t);                                     // adding client to arraylist

                t.start();
            }
            try {
                serverSocket.close();
                for(int i = 0; i < clients.size(); ++i) {
                    ClientThread tc = clients.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                    }
                }
            }
            catch(Exception e) {
                display("" + e);
            }
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    // Printing out with current time
    public void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    // broadcast message to Clients
    public synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());

        String[] myMessage = message.split(" ",3);               //checking if it is private message

        boolean itIsPrivate = false;
        if(myMessage[1].charAt(0)=='@') {
            itIsPrivate = true;
        }

        // sending private message to correct Client
        if(itIsPrivate)
        {
            String txt = myMessage[1].substring(1, myMessage[1].length());

            message = myMessage[0] + myMessage[2];
            String messageLf = time + " " + message;
            boolean found = false;

            // looking for username
            for(int y = clients.size(); --y >= 0;)
            {
                ClientThread ct1 = clients.get(y);
                String check = ct1.getUsername();
                if(check.equals(txt))
                {
                    // if it fails remove it from the list
                    if(!ct1.writeMsg(messageLf)) {
                        clients.remove(y);
                        display("Disconnected Client " + ct1.username + " removed from list.\n");
                    }
                    // else deliver the message
                    found = true;
                    break;
                }
            }

            // if such username is not found
            if(!found) {
                return false;
            }
        }

        // if it is not private message then broadcast it
        else
        {
            String messageLf = time + " " + message;
            System.out.print(messageLf);

            // checking if client is disconnected
            for(int i = clients.size(); --i >= 0;) {
                ClientThread ct = clients.get(i);
                if(!ct.writeMsg(messageLf)) {
                    clients.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.\n");
                }
            }
        }
        return true;
    }

    // if client sent EXIT message to exit
    synchronized public void remove(int id) {

        String disconnectedClient = "";
        // scan the arraylist until we found the Id
        for(int i = 0; i < clients.size(); ++i) {
            ClientThread ct = clients.get(i);
            // if found remove it
            if(ct.id == id) {
                disconnectedClient = ct.getUsername();
                clients.remove(i);
                break;
            }
        }
        broadcast(disconnectedClient + " has left the chat room.\n");
    }


    public static void main(String[] args) {
        int portNumber = 4561;
        Server server = new Server(portNumber);
        server.start();
    }

    // Thread for each client
    class ClientThread extends Thread {
        Socket socket;                                      // to get the message from client
        ObjectInputStream sInput;                           // to write the message to socket
        ObjectOutputStream sOutput;                         // to read the message from socket
        int id;                                             // unique id of client
        String username;                                    // username of the client
        ChatMessage cm;                                     // to check if it is simple message or keyword
        String date;                                        // current time

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try
            {
                //creating both data stream
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());

                username = (String) sInput.readObject();                                //getting username of client
                broadcast(username + " has joined the chat room.\n");
            }
            catch (IOException e) {
                display(" " + e);
                return;
            }
            catch (ClassNotFoundException e) {
                System.out.println(e);
            }
            date = new Date().toString();
        }

        //getting username
        public String getUsername() {
            return username;
        }

        public void run() {
            boolean connected = true;
            // it will loop until client is connected
            while(connected) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    System.out.println(e2);
                    break;
                }

                String message = cm.getMessage();                   //getting the message

                // different actions based on type message
                switch(cm.getType()) {
                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message + "\n");
                        if(!confirmation){
                            String msg = "Sorry. No such user exists.";
                            writeMsg(msg);
                        }
                        break;
                    case ChatMessage.EXIT:
                        display(username + " disconnected with a EXIT keyword.");
                        connected = false;
                        break;
                    case ChatMessage.ACTIVEUSERS:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // send list of active clients
                        for(int i = 0; i < clients.size(); ++i) {
                            ClientThread ct = clients.get(i);
                            writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            remove(id);                 // if disconnected remove from list
            close();                    // close everything
        }

        // close everything
        public void close() {
            try {
                // checks if it is not null then close manually
                if(sOutput != null) sOutput.close();
                if(sInput != null) sInput.close();
                if(socket != null) socket.close();
            }
            catch(Exception e) {
                System.out.println("" + e);
            }
        }

        public boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}
