package modelos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class usuario {

    private usuario() {
    }

    public static boolean autenticar(String user, String passwd) throws ClassNotFoundException, SQLException {
        String sql = "SELECT nombre_usuario FROM usuario WHERE nombre_usuario = ? AND passwd = ?";
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user);
            statement.setString(2, passwd);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
