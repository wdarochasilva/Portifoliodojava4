package br.com.arthurhenrique.dao;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import br.com.arthurhenrique.model.Projeto;

public class ProjetoCSV {

    private final Path caminho;

    public ProjetoCSV() {
        Path direto = Path.of("data/projetos.csv");
        Path aninhado = Path.of("portfolio-academico-java/data/projetos.csv");

        if (Files.exists(direto)) {
            this.caminho = direto;
        } else if (Files.exists(aninhado)) {
            this.caminho = aninhado;
        } else {
            this.caminho = aninhado;
        }
    }

    public void salvar(List<Projeto> projetos) throws Exception {
        Path pai = caminho.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }

        List<String> linhas = new ArrayList<>();
        linhas.add("id;nome;descricao;categoria;status;prazo");

        if (projetos != null) {
            for (Projeto projeto : projetos) {
                if (projeto == null) {
                    continue;
                }
                String linha = projeto.getId() + ";"
                        + sanitizar(projeto.getNome()) + ";"
                        + sanitizar(projeto.getDescricao()) + ";"
                        + sanitizar(projeto.getCategoria()) + ";"
                        + sanitizar(projeto.getStatus()) + ";"
                        + sanitizar(projeto.getPrazo());
                linhas.add(linha);
            }
        }

        Files.write(caminho, linhas, StandardCharsets.UTF_8);
    }

    public List<Projeto> listar() throws Exception {
        List<Projeto> projetos = new ArrayList<>();

        if (!Files.exists(caminho)) {
            return projetos;
        }

        List<String> linhas = Files.readAllLines(caminho, StandardCharsets.UTF_8);
        if (linhas.isEmpty()) {
            return projetos;
        }

        for (int i = 1; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (linha == null || linha.isBlank()) {
                continue;
            }

            String[] dados = linha.split(";", -1);
            if (dados.length < 5) {
                continue;
            }

            int id = Integer.parseInt(dados[0].trim());
            String nome = dados[1].trim();
            String descricao = dados[2].trim();
            String categoria = dados[3].trim();
            String status = dados[4].trim();
            String prazo = dados.length >= 6 ? dados[5].trim() : "";

            Projeto projeto = new Projeto(id, nome, descricao, categoria, status, prazo);
            projetos.add(projeto);
        }

        return projetos;
    }

    private String sanitizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace(";", ",")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
} 