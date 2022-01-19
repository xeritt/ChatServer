package org.chat;

import org.chat.security.RSAUtil;
import org.chat.security.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

// ClientHandler class
class ClientHandler implements Runnable, Log {
    public static final String DELIM = "@";
    private static final String DELIM_COMMAND = " ";
    public static final String USER_EXISTS = "A user with this name exists";
    public static final String REGISTERED = "user registered";
    public static final String LOGIN_SUCCESS = "User login success";
    public static final String BAD_PASSWORD = "Bad password";
    public static final String NO_USER = "No user with name";
    public static final String PASSWORD_REGISTRATION_ERROR = "Password registration error";
    public static final String ALREADY_LOGGED_IN = "The user [%s] is already logged in";
    public static final String SETNAME = "/setname ";
    public static final String NOT_LOGGED_IN_PLEASE_LOGIN = "The user is not logged in. Please login.";
    //private Scanner scn = new Scanner(System.in);
    private String name;
    //private String ip;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private boolean isloggedin;
    private ChatServer chatServer;

    private boolean encrypt = false;
    private KeyPair writeKeys;
    private PublicKey readKey;
    private User user;

    public String getName() {
        return name;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ClientHandler(Socket socket, String name) throws IOException {
        // obtain input and output streams
        this.name = name;
        this.socket = socket;
        this.isloggedin = true;

    }

    public ClientHandler(ChatServer chatServer, Socket socket, User user) throws IOException {
        // obtain input and output streams
        setUser(user);
        this.socket = socket;
        this.isloggedin = true;
        this.chatServer = chatServer;
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
            //helpCommand("/help");
            loginMsg();
            sendAllCommand("has joined the chat.");
            while (true) {
                try {
                    // receive the string
                    received = dis.readUTF();
                    //log(received);
                    if (encrypt){
                        received = RSAUtil.decrypt(received, this.writeKeys.getPrivate());
                    }
                    //log(received);
                    if (keyCommand(received)) continue;
                    if (authCommand(received)) continue;
                    if (checkUnLogin()) continue;
                    if (logoutCommand(received)) break;
                    if (listCommand(received)) continue;
                    if (helpCommand(received)) continue;
                    if (setNameCommand(received)) continue;

                    // break the string into message and recipient part
                    if (sendPrivateCommand(received)) continue;
                    if (sendAllCommand(received)) continue;

                } catch (NoSuchElementException e){
                    log("Error input!");
                } catch (IOException e) {
                    e.printStackTrace();
                    isloggedin = false;
                    socket.close();
                    chatServer.getClientHandlers().remove(getName(), this);
                    log("Client: " + getName() + " disconected.");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                    log("Encrypt error.");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean keyCommand(String received) {
        if (!received.equals("/key")) return false;
        try {
            String keyEncode = dis.readUTF();
            this.readKey = RSAUtil.getPublicKey(keyEncode);

            this.writeKeys = RSAUtil.generateKeyPair();
            dos.writeUTF("/key");
            String publicKey = RSAUtil.convertPublicKeyToString(writeKeys.getPublic());
            dos.writeUTF(publicKey);
            this.encrypt = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void send(String msg){
        if (encrypt){
            try {
                msg = RSAUtil.encrypt(msg , readKey);
            } catch (Exception e) {
                e.printStackTrace();
                log("Error encrypt");
            }
        }
        try {
            dos.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            log("Error write");
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

        clientHandler.send(this.name + ": " + MsgToSend);
        return false;
    }

    private boolean setNameCommand(String received) {

        StringTokenizer commands = new StringTokenizer(received, DELIM_COMMAND);
        if (commands.countTokens() < 2) return false;
            String command = commands.nextToken();
            String newName = commands.nextToken();
            //if (setNameCommand(command, arg1)) continue;


        if (command.equals(SETNAME.trim())) {
            String oldName = name;

            Map<String, ClientHandler> clients = chatServer.getClientHandlers();
            synchronized(clients){
                clients.remove(oldName, this);
                name = newName;
                clients.put(newName, this);
            }

            Map<String, String> ipNames = chatServer.getIpNames();
            String ip = socket.getInetAddress().getHostAddress();
            ipNames.replace(ip, oldName, newName);
            String msg = "User [" + oldName + "] change name to [" + getName() + "]";
            log(msg);
            send(msg);
            return true;
        }
        return false;
    }

    private boolean sendAllCommand(String text) throws IOException {
        //if (!received.equals("/list")) return false;
        Map<String, ClientHandler> clients = chatServer.getClientHandlers();
        StringBuilder sbl = new StringBuilder();
        sbl.append("Online " + clients.size()+ " users \n");
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            ClientHandler clientHandler = entry.getValue();
            if (clientHandler != this)
                clientHandler.send(getName() + ": " + text);
        }
        return true;
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
            send(sbl.toString());
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

    private boolean authCommand(String received) throws IOException {
        StringTokenizer commands = new StringTokenizer(received, DELIM_COMMAND);
        if (commands.countTokens() < 3) return false;
        String command = commands.nextToken();
        String userName = commands.nextToken();
        String userPass = commands.nextToken();

        Map<String, User> nameUsers = chatServer.getNameUsers();
        if (command.equals("/login") || command.equals("/l")) {
            return loginCommand(userName, userPass, nameUsers);
        } else if (command.equals("/register")){
            return registerCommand(userName, userPass, nameUsers);
        }
        return false;
    }

    private boolean registerCommand(String userName, String userPass, Map<String, User> nameUsers) {
        if (checkLogin()) return true;
        if (nameUsers.containsKey(userName)){
            send(USER_EXISTS);
            log(USER_EXISTS);
            return true;
        }
        try {
            userPass = RSAUtil.getMD5(userPass);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            send(PASSWORD_REGISTRATION_ERROR);
            log(PASSWORD_REGISTRATION_ERROR);
            return true;
        }
        User user = new User(userName, userPass);
        user.setIsloggedin(true);
        setUser(user);
        setNameCommand(SETNAME + userName);
        nameUsers.put(userName, user);
        String reg = "[ " + userName + " ] " + REGISTERED;
        chatServer.saveUsers();
        send(reg);
        log(reg);
        return true;
    }

    private boolean checkUnLogin() {
        if (user!=null){
            if (user.isIsloggedin()){
                return false;
            }
        }
        send(NOT_LOGGED_IN_PLEASE_LOGIN);
        log(NOT_LOGGED_IN_PLEASE_LOGIN);
        return true;
    }

    private boolean checkLogin() {
        if (user!=null){
            if (user.isIsloggedin()){
                String msg = String.format(ALREADY_LOGGED_IN, user.getUsername());
                send(msg);
                log(msg);
                return true;
            }
        }
        return false;
    }

    private boolean loginCommand(String userName, String userPass, Map<String, User> nameUsers) {
        if (checkLogin()) return true;
        if (nameUsers.containsKey(userName)){
            User user = nameUsers.get(userName);
            try {
                userPass = RSAUtil.getMD5(userPass);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                String msg = BAD_PASSWORD + "Erorr MD5";
                send(msg);
                log(msg);
            }
            if (user.getPassword().equals(userPass)){
                send(LOGIN_SUCCESS);
                if (!userName.equals(this.getName())){
                    setNameCommand(SETNAME + userName);
                }
                setUser(user);
                helpCommand("/help");
                log(LOGIN_SUCCESS);
            } else {
                send(BAD_PASSWORD);
                log(BAD_PASSWORD);
            }
        } else {
            String msg = "[ " + userName + " ] " + NO_USER;
            send(msg);
            log(msg);
        }
        return true;
    }


    private boolean helpCommand(String received) {
        if (received.equals("/help")) {
            StringBuilder sbl = new StringBuilder();
            sbl.append("Wellcome " + name+ " to free chat!\n");
            sbl.append("Available commands: /help, /list, /logout, /setname [newname], userName@message");
            send(sbl.toString());
            return true;
        }
        return false;
    }

    private void loginMsg() {
            StringBuilder sbl = new StringBuilder();
            sbl.append("Wellcome " + name+ " to free chat!\n");
            sbl.append("Available commands: /register [userName] [userPass], /login [userName] [userPass]\n");
            sbl.append("Please login or register.");
            send(sbl.toString());
    }

}
