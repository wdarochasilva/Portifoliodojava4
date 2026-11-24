package br.com.arthurhenrique.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.arthurhenrique.model.Projeto;

public class ApiClient {

    private final String baseUrl = "http://localhost:7070";
    private final HttpClient client;
    private final ObjectMapper mapper;

    public ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
    }

    // GET
    public List<Projeto> listarProjetos() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/projetos"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Erro ao listar projetos (" + response.statusCode() + "): " + response.body());
        }

        return mapper.readValue(response.body(), new TypeReference<List<Projeto>>() {
        });
    }

    // POST
    public Projeto cadastrarProjeto(Projeto projeto) throws Exception {
        String json = mapper.writeValueAsString(projeto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/projetos"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Erro ao cadastrar projeto (" + response.statusCode() + "): " + response.body());
        }

        return mapper.readValue(response.body(), Projeto.class);
    }

    // DELETE
    public void excluirProjeto(int id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/projetos/" + id))
                .timeout(Duration.ofSeconds(5))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new RuntimeException("Erro ao excluir projeto (" + response.statusCode() + "): " + response.body());
        }
    }

    // PUT
    public Projeto alterarProjeto(int id, Projeto projeto) throws Exception {
        String json = mapper.writeValueAsString(projeto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/projetos/" + id))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Erro ao alterar projeto (" + response.statusCode() + "): " + response.body());
        }

        return mapper.readValue(response.body(), Projeto.class);
    }
} 