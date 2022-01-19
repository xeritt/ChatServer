package org.chat.security;

import java.io.*;
import java.util.Map;

public class UserService {

    static public void saveUsers(File file, Map<String, User> nameUsers){
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))) {
            os.writeObject(nameUsers);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public Map<String, User> loadUsers(File file){
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, User> nameUsers = (Map<String, User>) is.readObject();
            return nameUsers;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
