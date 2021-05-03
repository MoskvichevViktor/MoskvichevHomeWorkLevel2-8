package server;

import java.sql.*;

public class AuthService {
    private static Connection connection;
    private static Statement statement;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static int addUser(String login, String pass, String nickname) {
        try {
            String query = "INSERT INTO users (login, password, nickname) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nickname);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int addBlacklist(String nickname) {
        try {
            String queryBlacklist = "INSERT INTO blacklist (nickname) VALUES (?);";
            PreparedStatement ps = connection.prepareStatement(queryBlacklist);
            ps.setString(1, nickname);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static int addMsg(String Msg) {
        try {
            String queryMsg = "INSERT INTO Msg (Msg) VALUES (?);";
            PreparedStatement ps = connection.prepareStatement(queryMsg);
            ps.setString(1, Msg);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }




    public static String getNicknameByLoginAndPass(String login, String pass) {
        String query = String.format("select nickname, password from users where login='%s'", login);
        try {
            ResultSet rs = statement.executeQuery(query); // возвращает выборку через select
            int myHash = pass.hashCode();
            // кеш числа 12345
            // изменим пароли в ДБ на хеш от строки pass1

            if (rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if (myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static String getNicknameByBlacklist(String nickname) {
        String queryBlacklist = String.format("select nickname from blacklist where nickname='%s'", nickname);
        try {
            ResultSet rs = statement.executeQuery(queryBlacklist); // возвращает выборку через select

            if (rs.next()) {
                String nick = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
