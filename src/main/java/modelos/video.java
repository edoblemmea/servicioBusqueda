package modelos;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class video {
    private String identificador;
    private String titulo;
    private String autor;
    private String fechaCreacion;
    private String duracion;
    private int numReproducciones;
    private String descripcion;
    private String formato;
    private String rutaVideo;
    private String usuarioRegistro;

    public video() {
    }

    public video(String identificador, String titulo, String autor, String fechaCreacion,
            String duracion, int numReproducciones, String descripcion, String formato,
            String rutaVideo, String usuarioRegistro) {
        this.identificador = identificador;
        this.titulo = titulo;
        this.autor = autor;
        this.fechaCreacion = fechaCreacion;
        this.duracion = duracion;
        this.numReproducciones = numReproducciones;
        this.descripcion = descripcion;
        this.formato = formato;
        this.rutaVideo = rutaVideo;
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getDuracion() {
        return duracion;
    }

    public void setDuracion(String duracion) {
        this.duracion = duracion;
    }

    public int getNumReproducciones() {
        return numReproducciones;
    }

    public void setNumReproducciones(int numReproducciones) {
        this.numReproducciones = numReproducciones;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getRutaVideo() {
        return rutaVideo;
    }

    public void setRutaVideo(String rutaVideo) {
        this.rutaVideo = rutaVideo;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public static video buscarPorIdentificador(String identificador) throws ClassNotFoundException, SQLException {
        String sql = "SELECT identificador, titulo, autor, fecha_creacion, duracion, num_reproducciones, "
                + "descripcion, formato, ruta_video, usuario_registro "
                + "FROM video WHERE identificador = ?";
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identificador);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return construirVideo(resultSet);
                }
            }
        }
        return null;
    }

    public static boolean incrementarReproducciones(String identificador) throws ClassNotFoundException, SQLException {
        String sql = "UPDATE video SET num_reproducciones = num_reproducciones + 1 WHERE identificador = ?";
        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identificador);
            return statement.executeUpdate() > 0;
        }
    }

    public static List<video> buscar(String titulo, String autor, Integer anio, Integer mes, Integer dia)
            throws ClassNotFoundException, SQLException {
        List<video> videos = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT identificador, titulo, autor, fecha_creacion, duracion, num_reproducciones, "
                + "descripcion, formato, ruta_video, usuario_registro "
                + "FROM video WHERE 1=1");

        if (titulo != null && !titulo.isBlank()) {
            sql.append(" AND UPPER(titulo) LIKE UPPER(?)");
            parametros.add("%" + titulo.trim() + "%");
        }
        if (autor != null && !autor.isBlank()) {
            sql.append(" AND UPPER(autor) LIKE UPPER(?)");
            parametros.add("%" + autor.trim() + "%");
        }
        if (anio != null) {
            sql.append(" AND SUBSTR(fecha_creacion, 1, 4) = ?");
            parametros.add(String.format("%04d", anio));
        }
        if (mes != null) {
            sql.append(" AND SUBSTR(fecha_creacion, 6, 2) = ?");
            parametros.add(String.format("%02d", mes));
        }
        if (dia != null) {
            sql.append(" AND SUBSTR(fecha_creacion, 9, 2) = ?");
            parametros.add(String.format("%02d", dia));
        }

        sql.append(" ORDER BY fecha_creacion DESC, titulo ASC");

        try (Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                Object parametro = parametros.get(i);
                if (parametro instanceof String) {
                    statement.setString(i + 1, (String) parametro);
                } else if (parametro instanceof Integer) {
                    statement.setInt(i + 1, (Integer) parametro);
                }
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    videos.add(construirVideo(resultSet));
                }
            }
        }
        return videos;
    }

    private static video construirVideo(ResultSet resultSet) throws SQLException {
        return new video(
                resultSet.getString("identificador"),
                resultSet.getString("titulo"),
                resultSet.getString("autor"),
                resultSet.getString("fecha_creacion"),
                resultSet.getString("duracion"),
                resultSet.getInt("num_reproducciones"),
                resultSet.getString("descripcion"),
                resultSet.getString("formato"),
                resultSet.getString("ruta_video"),
                resultSet.getString("usuario_registro")
        );
    }
}
