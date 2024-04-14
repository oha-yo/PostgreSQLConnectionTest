package postgresConnection;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PostgreSQLConnectionTest {


	public static void main(String[] args) {
        Connection connection = null;
        try {
            // プロパティファイルから接続情報とSQLクエリを読み込む
            Properties props = new Properties();
            FileInputStream in = new FileInputStream("database.properties");
            props.load(in);
            in.close();

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");
            String query = props.getProperty("sql.query");

            // PostgreSQLに接続する
            connection = DriverManager.getConnection(url, user, password);
            // JDBCドライバーの情報を取得
            java.sql.DatabaseMetaData conmetaData = connection.getMetaData();
            String driverName = conmetaData.getDriverName();
            String driverVersion = conmetaData.getDriverVersion();
            

            // SQLクエリを実行し、結果セットを取得する
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement.executeQuery(query);

            // 結果セットのメタデータを取得し、各列のデータ型を表示

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            /*
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String columnType = metaData.getColumnTypeName(i);
                System.out.println(i + " " + columnName + ", Type: " + columnType);
            }
            System.out.println();
            */
            // 各列の最大幅を計算
            int[] columnWidths = new int[columnCount];
            resultSet.beforeFirst();
            while (resultSet.next()) {
                for (int i = 0; i < columnCount; i++) {
                    String value = resultSet.getString(i + 1);
                    if (value != null) {
                        columnWidths[i] = Math.max(columnWidths[i], value.length());
                    } else {
                        // null値の場合のデフォルトの幅を設定
                        columnWidths[i] = Math.max(columnWidths[i], metaData.getColumnLabel(i + 1).length());
                    }
                }
            }

            // 結果セットを最初に戻す
            resultSet.beforeFirst();

            // 結果を出力
            while (resultSet.next()) {
                for (int i = 0; i < columnCount; i++) {
                    String value = resultSet.getString(i + 1);
                    if (value != null) {
                        // 各列の幅に基づいて左詰めで表示し、隣の列と1桁のスペースを挿入
                        System.out.print(String.format("%-" + columnWidths[i] + "s", value) + " ");
                    } else {
                        // null値の場合はデフォルトの幅で表示し、隣の列と1桁のスペースを挿入
                        System.out.print(String.format("%-" + columnWidths[i] + "s", "") + " ");
                    }
                }
                System.out.println();
            }
            // 新しいSQLクエリを取得して実行し、結果を表示する
            System.out.println("---------------------------------------------------");
            System.out.println("OS Name             : " + System.getProperty("os.name"));
            System.out.println("OS Version          : " + System.getProperty("os.version"));
            System.out.println("JDBC Driver Name    : " + driverName);
            System.out.println("JDBC Driver Version : " + driverVersion);
            
            String newQuery = props.getProperty("sql.query2");
            ResultSet newResultSet = statement.executeQuery(newQuery);
            if (newResultSet.next()) {
                System.out.println("Current Timestamp   : " + newResultSet.getString(1));
            }
            
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}