package br.com.arthurhenrique.api;

import java.util.List;
import java.util.Map;

import br.com.arthurhenrique.model.Projeto;
import br.com.arthurhenrique.service.ProjetoService;
import io.javalin.Javalin;

public class Api {

    private static Javalin app;

    public static void main(String[] args) throws Exception {
        if (app != null) {
            return;
        }

        ProjetoService service = new ProjetoService();
        service.carregar();

        app = Javalin.create(config -> {
            config.staticFiles.add("/public");

            config.routes.get("/", ctx -> ctx.redirect("/projetos.html"));

            // -- GET / READ --
            config.routes.get("/api/projetos", ctx -> {
                service.carregar();
                List<Projeto> projetos = service.listar();
                ctx.json(projetos);
            });

            config.routes.get("/api/projetos/{id}", ctx -> {
                try {
                    int id = Integer.parseInt(ctx.pathParam("id"));
                    service.carregar();
                    Projeto projeto = service.buscaPorId(id);

                    if (projeto == null) {
                        ctx.status(404).json(Map.of("erro", "Projeto não encontrado"));
                        return;
                    }

                    ctx.json(projeto);
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("erro", "ID inválido"));
                }
            });

            // -- POST / CREATE --
            config.routes.post("/api/projetos", ctx -> {
                service.carregar();
                Projeto projeto = ctx.bodyAsClass(Projeto.class);

                if (projeto == null || projeto.getNome() == null || projeto.getNome().isBlank()) {
                    ctx.status(400).json(Map.of("erro", "Nome é obrigatório"));
                    return;
                }

                if (projeto.getId() <= 0) {
                    projeto.setId(service.gerarId());
                }

                boolean adicionou = service.adicionar(projeto);
                if (!adicionou) {
                    ctx.status(409).json(Map.of("erro", "Não foi possível cadastrar o projeto (nome ou ID já existe)"));
                    return;
                }

                service.salvar();
                ctx.status(201).json(projeto);
            });

            // -- PUT / UPDATE --
            config.routes.put("/api/projetos/{id}", ctx -> {
                try {
                    int id = Integer.parseInt(ctx.pathParam("id"));
                    service.carregar();

                    Projeto projeto = ctx.bodyAsClass(Projeto.class);
                    if (projeto == null || projeto.getNome() == null || projeto.getNome().isBlank()) {
                        ctx.status(400).json(Map.of("erro", "Nome é obrigatório"));
                        return;
                    }

                    projeto.setId(id);
                    boolean alterou = service.alterar(projeto);

                    if (!alterou) {
                        ctx.status(404).json(Map.of("erro", "Projeto não encontrado ou nome duplicado"));
                        return;
                    }

                    service.salvar();
                    ctx.json(projeto);
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("erro", "ID inválido"));
                }
            });

            // -- DELETE --
            config.routes.delete("/api/projetos/{id}", ctx -> {
                try {
                    int id = Integer.parseInt(ctx.pathParam("id"));
                    service.carregar();

                    Projeto projeto = service.buscaPorId(id);
                    if (projeto == null) {
                        ctx.status(404).json(Map.of("erro", "Projeto não encontrado"));
                        return;
                    }

                    service.removerPorId(id);
                    service.salvar();
                    ctx.status(204);
                } catch (NumberFormatException e) {
                    ctx.status(400).json(Map.of("erro", "ID inválido"));
                }
            });
        });

        try {
            app.start(7070);
        } catch (Exception e) {
            app = null;
            throw e;
        }
    }
}