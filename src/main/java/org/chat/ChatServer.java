package org.chat;

import org.chat.security.User;
import org.chat.security.UserService;

import java.io.*;
import java.util.*;
import java.net.*;

// Server class
public class ChatServer implements Log {
    public static final String USERS_SER = "users.ser";
    // Vector to store active clients
    private Map<String, ClientHandler> clientHandlers = new HashMap<>();
    private Map<String, String> ipNames = new HashMap<>();
    private Map<String, User> nameUsers = new HashMap<>();
    // counter for clients
    static int i = 0;
    private int port = 1234;

    public Map<String, ClientHandler> getClientHandlers() {
        return clientHandlers;
    }

    public Map<String, String> getIpNames() {return ipNames;}
    public Map<String, User> getNameUsers() {return nameUsers;}

    public void start(String[] args) throws IOException {
        // server is listening on port 1234
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        ServerSocket ss = new ServerSocket(port);
        // running infinite loop for getting
        // client request
        loadUsers();

        while (true) {
            // Accept the incoming request
            Socket clientSocket = ss.accept();
            //log("New client request received : " + clientSocket);
            //log("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            //ClientHandler clientHandler = new ClientHandler(clientSocket, "client " + i, dis, dos);
            String ip = clientSocket.getInetAddress().getHostAddress();
            String newName = "user" + i;
            if (ipNames.containsKey(ip)){
                newName = ipNames.get(ip);
            } else {
                ipNames.put(clientSocket.getInetAddress().getHostAddress(), newName);
            }

            ClientHandler clientHandler = new ClientHandler(this, clientSocket, newName);
            // Create a new Thread with this object.

            //log("Adding " + clientHandler.getName() + " client to active client list");
            // add this client to active clients list
            System.out.println();
            clientHandlers.put(clientHandler.getName(), clientHandler);
            // start the thread.
            Thread client = new Thread(clientHandler);
            client.start();
            i++;

        }

    }

    public void loadUsers() {
        File usersFile = new File(USERS_SER);
        if (usersFile.isFile()){
            nameUsers = UserService.loadUsers(usersFile);
        }
    }

    public void saveUsers() {
        File usersFile = new File(USERS_SER);
        UserService.saveUsers(usersFile, nameUsers);

    }

    public static void main(String[] args) throws IOException {
        ChatServer chatServer = new ChatServer();
        chatServer.start(args);
    }

}

