package modelos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final String JDBC_URL = "jdbc:derby://localhost:1527/pr2";
    private static final String JDBC_USER = "pr2";
    private static final String JDBC_PASSWORD = "pr2";
    private static final String JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName(JDBC_DRIVER);
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }
}
