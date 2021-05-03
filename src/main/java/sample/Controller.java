package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    @FXML
    TextArea chatArea;

    @FXML
    TextField textField;

    @FXML
    HBox bottomPanel;

    @FXML
    HBox upperPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    ListView<String> clientList;

    @FXML
    private Label userName;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    static final String ADDRESS = "localhost";
    static final int PORT = 6001;

    private boolean isAuthorized;

    List<TextArea> textAreas;

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);

            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);

            clientList.setVisible(false);
            clientList.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);

            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);

            clientList.setVisible(true);
            clientList.setManaged(true);
        }
    }

    @FXML
    void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void connect() {
        try {
            userName.setText("test");
            socket = new Socket(ADDRESS, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            setAuthorized(false);

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if ("/auth-OK".equals(str)) {
                            setAuthorized(true);
                            ////2. После загрузки клиента показывать ему последние 100 строк чата.
                            try {
                                loadHistory();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            break;
                        } else {
                            for (TextArea ta : textAreas) {
                                ta.appendText(str + "\n");
                            }
                        }
                    }

                    while (true) {
                        String str = in.readUTF();
                        if ("/serverClosed".equals(str)) {
                            break;
                        }
                        if (str.startsWith("/clientList ")) {
                            String[] tokens = str.split(" ");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientList.getItems().add(tokens[i]);
                                    }
                                }
                            });
                        } else {
                            chatArea.appendText(str + "\n");
                            ////1. Добавить в сетевой чат запись локальной истории в текстовый файл на клиенте.
                            saveHistory();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthorized(false);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Connection refused)\nServer is not available");
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            //loginField.clear();
            //passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) {
            if (!socket.isClosed()) {
                try {
                    out.writeUTF("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void selectClient(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            MiniStage ms = new MiniStage(clientList.getSelectionModel().getSelectedItem(), out, textAreas);
            ms.show();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        textAreas = new ArrayList<>();
        textAreas.add(chatArea);
    }

    public void logUp(ActionEvent actionEvent) {
        RegistrationStage rs = new RegistrationStage(out);
        rs.show();
    }

    //1. Добавить в сетевой чат запись локальной истории в текстовый файл на клиенте.
    // Для каждой учетной записи файл
    // с историей должен называться history_[login].txt. (Например, history_login1.txt,
    // history_user111.txt)
    String getFileName(){
        return String.format("history_[%s]", loginField.getText());
    }

    private void saveHistory() throws IOException {
        try {
            File history = new File("src/"+getFileName()+".txt");
            if (!history.exists()) {
                history.createNewFile();
            }
            PrintWriter fileWriter = new PrintWriter(new FileWriter(history, true));

            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(chatArea.getText());
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //2. После загрузки клиента показывать ему последние 100 строк чата.
    private void loadHistory() throws IOException {
        int posHistory = 100;
        File history = new File("src/"+getFileName()+".txt");
        ArrayList<String> historyList = new ArrayList<>();
        FileInputStream in = new FileInputStream(history);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));

        String temp;
        while ((temp = bufferedReader.readLine()) != null) {
            historyList.add(temp);
        }

        if (historyList.size() > posHistory) {
            for (int i = historyList.size() - posHistory; i <= (historyList.size() - 1); i++) {
                chatArea.appendText(historyList.get(i) + "\n");
            }
        } else {
            for (int i = 0; i < historyList.size(); i++) {
                chatArea.appendText(historyList.get(i) + "\n");
            }
        }
    }


}
