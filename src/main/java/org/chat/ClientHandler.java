package org.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

// ClientHandler class
class ClientHandler implements Runnable, Log {
    public static final String DELIM = "@";
    private static final String DELIM_COMMAND = " ";
    private Scanner scn = new Scanner(System.in);
    private String name;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private boolean isloggedin;
    private ChatServer chatServer;

    public String getName() {
        return name;
    }

    public ClientHandler(Socket socket, String name) throws IOException {
        // obtain input and output streams
        this.name = name;
        this.socket = socket;
        this.isloggedin = true;
    }

    public ClientHandler(ChatServer chatServer, Socket socket, String name) throws IOException {
        // obtain input and output streams
        this.name = name;
        this.socket = socket;
        this.chatServer = chatServer;
        this.isloggedin = true;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            this.dos = dos;
            this.dis = dis;
            String received;
            helpCommand("/help");
            while (true) {
                try {
                    // receive the string
                    received = dis.readUTF();
                    log(received);
                    if (logoutCommand(received)) break;
                    if (listCommand(received)) continue;
                    if (helpCommand(received)) continue;
                    if (setNameCommand(received)) continue;
                    // break the string into message and recipient part
                    if (sendPrivateCommand(received)) continue;

                } catch (NoSuchElementException e){
                    log("Error input!");
                } catch (IOException e) {
                    e.printStackTrace();
                    isloggedin = false;
                    socket.close();
                    chatServer.getClientHandlers().remove(getName(), this);
                    log("Client: " + getName() + " disconected.");
                    break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean sendPrivateCommand(String received) throws IOException {
        StringTokenizer st = new StringTokenizer(received, DELIM);
        if (st.countTokens() < 2) return false;
        String recipient;
        String MsgToSend;
        recipient = st.nextToken();
        MsgToSend = st.nextToken();

        //if (setNameCommand(MsgToSend, recipient)) continue;

        // search for the recipient in the connected devices list.
        // ar is the vector storing client of active users
        ClientHandler clientHandler = chatServer.getClientHandlers().get(recipient);
        if (clientHandler == null) return true;
        if (clientHandler.isloggedin == false) return true;

        clientHandler.dos.writeUTF(this.name + " : " + MsgToSend);
        return false;
    }

    private boolean setNameCommand(String received) {

        StringTokenizer commands = new StringTokenizer(received, DELIM_COMMAND);
        if (commands.countTokens() < 2) return false;
            String command = commands.nextToken();
            String newName = commands.nextToken();
            //if (setNameCommand(command, arg1)) continue;


        if (command.equals("/setname")) {
            String oldName = name;

            Map<String, ClientHandler> clients = chatServer.getClientHandlers();
            synchronized(clients){
                clients.remove(oldName, this);
                name = newName;
                clients.put(newName, this);
            }

            String msg = "User " + oldName + " change name to " + getName();
            log(msg);
            try {
                dos.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private boolean listCommand(String received) {
        if (received.equals("/list")) {
            Map<String, ClientHandler> clients = chatServer.getClientHandlers();
            StringBuilder sbl = new StringBuilder();
            sbl.append("Online " + clients.size()+ " users \n");
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                ClientHandler user = entry.getValue();
                sbl.append(user.getName() + ", ");
                //System.out.println(entry.getKey() + ":" + entry.getValue());
            }
            try {
                dos.writeUTF(sbl.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private boolean logoutCommand(String received) throws IOException {
        if (received.equals("/logout")) {
            isloggedin = false;
            socket.close();
            Map<String, ClientHandler> clients = chatServer.getClientHandlers();
            synchronized(clients) {
                clients.remove(name, this);
            }
            return true;
        }
        return false;
    }

    private boolean helpCommand(String received) {
        if (received.equals("/help")) {
            StringBuilder sbl = new StringBuilder();
            sbl.append("Wellcome " + name+ " to free chat!\n");
            sbl.append("Available commands: /help, /list, /logout, /setname [newname], userName@message");
            try {
                dos.writeUTF(sbl.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

}
