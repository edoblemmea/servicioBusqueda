package isdcm.serviciobusqueda.resources;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.sql.SQLException;
import java.util.List;
import java.nio.charset.StandardCharsets;
import modelos.usuario;
import modelos.video;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

/**
 *
 * @author 
 */
@Path("videos")
@Produces(MediaType.APPLICATION_JSON)
public class JakartaEE91Resource {
    private static final String JWT_SECRET = "isdcm-entrega-3-clave-firma-hs512-2026-segura-larga-64bytes-minimo-ok";
    private static final long JWT_EXPIRACION_MS = 15 * 60 * 1000L;

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping() {
        return Response
                .ok("ping Jakarta EE")
                .build();
    }

    @GET
    public Response buscarVideos(@QueryParam("titulo") String titulo,
            @QueryParam("autor") String autor,
            @QueryParam("anio") Integer anio,
            @QueryParam("mes") Integer mes,
            @QueryParam("dia") Integer dia) {
        return ejecutarBusqueda(titulo, autor, anio, mes, dia);
    }

    @POST
    @Path("buscar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response buscarVideosPost(JsonObject filtros) {
        if (filtros == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Debes enviar un JSON con los filtros de busqueda"))
                    .build();
        }

        String titulo = obtenerTexto(filtros, "titulo");
        String autor = obtenerTexto(filtros, "autor");
        Integer anio = obtenerEntero(filtros, "anio");
        Integer mes = obtenerEntero(filtros, "mes");
        Integer dia = obtenerEntero(filtros, "dia");

        return ejecutarBusqueda(titulo, autor, anio, mes, dia);
    }

    /**
     * POST method to login in the application
     *
     * @param username
     * @param password
     * @return
     */
    @Path("login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username,
            @FormParam("password") String password) {

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Usuario y contrasena son obligatorios"))
                    .build();
        }

        try {
            boolean credencialesValidas = usuario.autenticar(username.trim(), password);
            if (!credencialesValidas) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("Credenciales incorrectas"))
                        .build();
            }

            Date ahora = new Date();
            Date expiracion = new Date(ahora.getTime() + JWT_EXPIRACION_MS);
            String token = Jwts.builder()
                    .setSubject(username.trim())
                    .setIssuedAt(ahora)
                    .setExpiration(expiracion)
                    .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                    .compact();

            JsonObject respuesta = Json.createObjectBuilder()
                    .add("JWT", token)
                    .build();
            return Response.ok(respuesta).build();

        } catch (ClassNotFoundException | SQLException ex) {
            return Response.serverError()
                    .entity(new ErrorResponse("No se ha podido generar el token JWT"))
                    .build();
        }
    }

    @POST
    @Path("validar-jwt")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response validarJwt(JsonObject body) {
        if (body == null || !body.containsKey("jwt") || body.isNull("jwt")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Debes enviar un JSON con el campo jwt"))
                    .build();
        }

        String token = body.getString("jwt", "").trim();
        if (token.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El campo jwt no puede estar vacio"))
                    .build();
        }

        try {
            byte[] keyBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(token);
            jws.setKey(new HmacKey(keyBytes));
            jws.setAlgorithmConstraints(
                    new org.jose4j.jwa.AlgorithmConstraints(
                            org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST,
                            AlgorithmIdentifiers.HMAC_SHA512
                    )
            );

            boolean firmaValida = jws.verifySignature();
            if (!firmaValida) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse("JWT no valido: la firma no es correcta"))
                        .build();
            }

            JwtConsumer consumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setRequireIssuedAt()
                    .setVerificationKey(new HmacKey(keyBytes))
                    .setJwsAlgorithmConstraints(
                            new org.jose4j.jwa.AlgorithmConstraints(
                                    org.jose4j.jwa.AlgorithmConstraints.ConstraintType.WHITELIST,
                                    AlgorithmIdentifiers.HMAC_SHA512
                            )
                    )
                    .build();

            JwtClaims claims = consumer.processToClaims(token);
            long nowSeconds = System.currentTimeMillis() / 1000L;
            long expSeconds = claims.getExpirationTime().getValue();
            long segundosRestantes = expSeconds - nowSeconds;
            boolean expirado = segundosRestantes <= 0;
            Long issuedAt = claims.getIssuedAt() == null ? null : claims.getIssuedAt().getValue();

            JsonObject respuesta = Json.createObjectBuilder()
                    .add("firmaValida", firmaValida)
                    .add("expirado", expirado)
                    .add("subject", claims.getSubject() == null ? "" : claims.getSubject())
                    .add("issuedAtEpoch", issuedAt == null ? 0 : issuedAt)
                    .add("expirationEpoch", expSeconds)
                    .add("segundosRestantes", expirado ? 0 : segundosRestantes)
                    .build();
            return Response.ok(respuesta).build();

        } catch (JoseException ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("JWT no valido: " + ex.getMessage()))
                    .build();
        } catch (InvalidJwtException ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("JWT no valido: " + ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.serverError()
                    .entity(new ErrorResponse("No se ha podido validar el JWT (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")"))
                    .build();
        }
    }

    private Response ejecutarBusqueda(String titulo, String autor, Integer anio, Integer mes, Integer dia) {
        String errorValidacion = validarFecha(anio, mes, dia);
        if (errorValidacion != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(errorValidacion))
                    .build();
        }

        try {
            List<video> resultados = video.buscar(titulo, autor, anio, mes, dia);
            return Response.ok(construirJsonVideos(resultados)).build();
        } catch (ClassNotFoundException | SQLException ex) {
            return Response.serverError()
                    .entity(new ErrorResponse("No se ha podido realizar la busqueda de videos"))
                    .build();
        }
    }

    private String obtenerTexto(JsonObject json, String clave) {
        if (!json.containsKey(clave) || json.isNull(clave)) {
            return null;
        }

        String valor = json.getString(clave, "").trim();
        return valor.isEmpty() ? null : valor;
    }

    private Integer obtenerEntero(JsonObject json, String clave) {
        if (!json.containsKey(clave) || json.isNull(clave)) {
            return null;
        }

        try {
            return json.getInt(clave);
        } catch (Exception ex) {
            return null;
        }
    }

    @PUT
    @Path("{identificador}/reproducciones")
    @Consumes(MediaType.WILDCARD)
    public Response incrementarReproducciones(@PathParam("identificador") String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("El identificador del video es obligatorio"))
                    .build();
        }

        try {
            boolean actualizado = video.incrementarReproducciones(identificador.trim());
            if (!actualizado) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No existe ningun video con el identificador indicado"))
                        .build();
            }

            video videoActualizado = video.buscarPorIdentificador(identificador.trim());
            return Response.ok(videoActualizado == null ? Json.createObjectBuilder().build() : construirJsonVideo(videoActualizado)).build();
        } catch (ClassNotFoundException | SQLException ex) {
            return Response.serverError()
                    .entity(new ErrorResponse("No se ha podido actualizar el numero de reproducciones"))
                    .build();
        }
    }

    private String validarFecha(Integer anio, Integer mes, Integer dia) {
        if (dia != null && (mes == null || anio == null)) {
            return "Para buscar por dia debes indicar tambien mes y año";
        }
        if (mes != null && anio == null) {
            return "Para buscar por mes debes indicar tambien año";
        }
        if (anio != null && anio < 0) {
            return "El año no es valido";
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            return "El mes debe estar entre 1 y 12";
        }
        if (dia != null && (dia < 1 || dia > 31)) {
            return "El dia debe estar entre 1 y 31";
        }
        return null;
    }

    private JsonArray construirJsonVideos(List<video> videos) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        if (videos == null) {
            return arrayBuilder.build();
        }

        for (video item : videos) {
            arrayBuilder.add(construirJsonVideo(item));
        }
        return arrayBuilder.build();
    }

    private JsonObject construirJsonVideo(video item) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("identificador", valor(item.getIdentificador()))
                .add("titulo", valor(item.getTitulo()))
                .add("autor", valor(item.getAutor()))
                .add("fechaCreacion", valor(item.getFechaCreacion()))
                .add("duracion", valor(item.getDuracion()))
                .add("numReproducciones", item.getNumReproducciones())
                .add("descripcion", valor(item.getDescripcion()))
                .add("formato", valor(item.getFormato()))
                .add("rutaVideo", valor(item.getRutaVideo()))
                .add("usuarioRegistro", valor(item.getUsuarioRegistro()));

        return builder.build();
    }

    private String valor(String value) {
        return value == null ? "" : value;
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
