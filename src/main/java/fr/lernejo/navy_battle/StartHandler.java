package fr.lernejo.navy_battle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class StartHandler implements HttpHandler {

    private final String url;
    private final JSONObject response = new JSONObject("{\n" +
        "  \"id\": \"string\",\n" +
        "  \"url\": \"string\",\n" +
        "  \"message\": \"string\",\n" +
        "}");

    public StartHandler(String url) {
        this.url = url;
    }

    public boolean verifyBody(JSONObject jsonData) {
        try {
            File schemaFile = new File("src/main/resources/startSchema.json");
            JSONTokener schemaData = new JSONTokener(new FileInputStream(schemaFile));
            JSONObject jsonSchema = new JSONObject(schemaData);
            Schema schemaValidator = SchemaLoader.load(jsonSchema);
            schemaValidator.validate(jsonData);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public void setResponse(int code, String url) {
        response.put("id", UUID.randomUUID().toString());
        response.put("url", url);
        switch(code) {
            case 1:
                response.put("message", "OK!");
                break;
            case 2:
                response.put("message", "Bad request !");
                break;
            default:
                response.put("message", "Not found !");
                break;
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String url = "http://localhost:" + exchange.getLocalAddress().getPort() + exchange.getRequestURI().toString();
        if ("POST".contentEquals(exchange.getRequestMethod())) {
            JSONObject jsonData = new JSONObject(new JSONTokener(new InputStreamReader(exchange.getRequestBody())));
            int code = verifyBody(jsonData) ? 1 : 2;
            setResponse(code, url);
            exchange.sendResponseHeaders(code == 1 ? HttpURLConnection.HTTP_ACCEPTED : HttpURLConnection.HTTP_BAD_REQUEST, response.toString().length());
            exchange.getResponseBody().write(response.toString().getBytes());
            try { this.sendFire(jsonData.get("url").toString(), "A2"); }
            catch (InterruptedException e) { e.printStackTrace(); }
        } else {
            setResponse(3, url);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.toString().length());
            exchange.getResponseBody().write(response.toString().getBytes());
        }
        exchange.close();
    }

    public String sendFire(String adversaryURL, String cell) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest fireRequest = HttpRequest.newBuilder()
            .uri(URI.create(adversaryURL + "/api/game/fire?cell=" + cell))
            .setHeader("Accept", "application/json")
            .setHeader("Content-Type", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = client.send(fireRequest, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
