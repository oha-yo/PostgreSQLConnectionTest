package postgresConnection;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgreSQLConnectionTest {

    public static void main(String[] args) {
        Connection connection = null;
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            String formattedDateTime = now.format(formatter);
            System.out.println("---------------------------------------------------");
            System.out.println("Recording start time: " + formattedDateTime);

            Properties props = loadProperties("database.properties");

            String url = props.getProperty("db.url");
            System.out.println("---------------------------------------------------");
            System.out.println("url: " + url);

            Pattern pattern = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/.*");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String phost = matcher.group(1);
                int defaultPort = 5432;
                int pport;
                if (matcher.group(2) != null) {
                    pport = Integer.parseInt(matcher.group(2));
                    if (pport <= 0 || pport > 65535) {
                        throw new IllegalArgumentException("Port number out of range: " + pport);
                    }
                } else {
                    pport = defaultPort;
                }

                System.out.println("Host: " + phost);
                System.out.println("Port: " + pport);

                if (!isHostReachable(phost)) {
                    throw new IOException("Host " + phost + " is not reachable.");
                }

                if (!isPortOpen(phost, pport)) {
                    throw new IOException("Port " + pport + " on host " + phost + " is not open.");
                }
            } else {
                throw new IllegalArgumentException("URL parsing failed");
            }

            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");
            String query = props.getProperty("sql.query");

            connection = DriverManager.getConnection(url, user, password);

            ResultSet resultSet = executeQuery(connection, query);
            printResultSet(resultSet);

            System.out.println("---------------------------------------------------");
            System.out.println("Database connection established successfully.");

        } catch (IOException | SQLException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            closeConnection(connection);
        }
    }

    private static Properties loadProperties(String fileName) throws IOException {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(fileName)) {
            props.load(in);
        }
        return props;
    }

    private static boolean isHostReachable(String host) throws IOException {
        return InetAddress.getByName(host).isReachable(2000); // 2秒間のタイムアウト
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // 2秒間接続を試みる
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static ResultSet executeQuery(Connection connection, String query) throws SQLException {
        Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        return statement.executeQuery(query);
    }

    private static void printResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        int[] columnWidths = new int[columnCount];

        while (resultSet.next()) {
            for (int i = 0; i < columnCount; i++) {
                String value = resultSet.getString(i + 1);
                if (value != null) {
                    columnWidths[i] = Math.max(columnWidths[i], value.length());
                } else {
                    columnWidths[i] = Math.max(columnWidths[i], metaData.getColumnLabel(i + 1).length());
                }
            }
        }

        resultSet.beforeFirst();
        while (resultSet.next()) {
            for (int i = 0; i < columnCount; i++) {
                String value = resultSet.getString(i + 1);
                if (value != null) {
                    System.out.print(String.format("%-" + columnWidths[i] + "s", value) + " ");
                } else {
                    System.out.print(String.format("%-" + columnWidths[i] + "s", "") + " ");
                }
            }
            System.out.println();
        }
    }

    private static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error while closing connection: " + e.getMessage());
            }
        }
    }
}
