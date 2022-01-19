# Chat desktop client (Windows, Linux)

**Build**

mvn package

**Running**

java -jar ChatServer.jar [port]

Folder /lib should be nearby in the same folder as the chat.jar itself.

---
## User commands
1. /register [userName] [userPass]
2. /login [userName] [userPass]
3. /logout
4. /help
5. /list
6. userName@privateMessage

---
All user save in file users.ser.

