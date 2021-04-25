package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static server.AuthService.*;

public class ClientHandler {

    private ConsoleServer server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String nickname;



    // черный список у пользователя, а не у сервера
    //List<String> blackList;

    public ClientHandler(ConsoleServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            //this.blackList = new ArrayList<>();

            server.getClientsExecutorService().execute(() -> {
                boolean isExit = false;
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/auth")){
                            String[] tokens = str.split(" ");
                            String nick = AuthService.getNicknameByLoginAndPass(tokens[1], tokens[2]);
                            if (nick != null) {
                                if (!server.isNickBusy(nick)) {
                                    sendMsg("/auth-OK");
                                    setNickname(nick);
                                    server.subscribe(ClientHandler.this);
                                    break;
                                } else {
                                    sendMsg("Учетная запись уже используется");
                                }
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        }
                        // регистрация
                        if (str.startsWith("/signup ")) {
                            //timeout
                            //long start= System.currentTimeMillis();

                            String[] tokens = str.split(" ");
                            int result = AuthService.addUser(tokens[1], tokens[2], tokens[3]);


                            //long timeoutInMillis = 1000L * 120;
                           // long finish = System.currentTimeMillis();
                            /*if((finish-start) < timeoutInMillis){
                                sendMsg("Время на регистрацию истекло!");

                                socket.isClosed();
                                break;
                                }*/

                            if (result > 0) {
                                sendMsg("Successful registration");
                            } else {
                                sendMsg("Registration failed");
                            }
                        }
                        // выход
                        if ("/end".equals(str)) {
                            isExit = true;
                            break;
                        }
                    }

                    if (!isExit) {
                        while (true) {
                            String str = in.readUTF();
                            // для всех служебных команд и личных сообщений
                            if (str.startsWith("/") || str.startsWith("@")) {
                                if ("/end".equalsIgnoreCase(str)){
                                    // для оповещения клиента, т.к. без сервера клиент работать не должен
                                    out.writeUTF("/serverClosed");
                                    System.out.println("Client (" + socket.getInetAddress() + ") exited");
                                    break;
                                }
                                // вторая часть ДЗ. выполнение
                                if (str.startsWith("@") ) {
                                    String[] tokens = str.split(" ", 2);
                                    server.sendPrivateMsg(this, tokens[0].substring(1), tokens[1]);
                                }
                                // черный список для пользователя.
                                if (str.startsWith("/blacklist ")) {
                                    String[] tokens = str.split(" ");
                                    addBlacklist(tokens[1]);
                                    sendMsg("You added " + tokens[1] + " to blacklist");
                                }
                            } else {
                                server.broadcastMessage(this, nickname +": " + str);
                            }
                            System.out.println("Client (" + socket.getInetAddress() + "): " + str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            addMsg(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean checkBlackList(String nickname) {
        if(getNicknameByBlacklist(nickname) != null){
            return true;
        }else return false;
    }
}
