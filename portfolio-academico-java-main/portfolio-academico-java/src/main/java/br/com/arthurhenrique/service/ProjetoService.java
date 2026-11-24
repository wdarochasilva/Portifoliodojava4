package br.com.arthurhenrique.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.arthurhenrique.dao.ProjetoCSV;
import br.com.arthurhenrique.model.Projeto;

// regras de negócio
public class ProjetoService {

    private List<Projeto> projetos;
    private final ProjetoCSV dao;

    public ProjetoService() {
        this.projetos = new ArrayList<>();
        this.dao = new ProjetoCSV();
    }

    public void carregar() throws Exception {
        List<Projeto> carregados = dao.listar();
        this.projetos = (carregados != null) ? new ArrayList<>(carregados) : new ArrayList<>();
    }

    public void salvar() throws Exception {
        dao.salvar(projetos);
    }

    public boolean adicionar(Projeto projeto) {
        if (projeto == null) {
            return false;
        }

        String nome = projeto.getNome() != null ? projeto.getNome().trim() : "";
        if (nome.isBlank()) {
            return false;
        }

        if (buscaPorId(projeto.getId()) != null) {
            return false;
        }

        for (Projeto existente : projetos) {
            if (existente.getNome() != null && existente.getNome().trim().equalsIgnoreCase(nome)) {
                return false;
            }
        }

        projeto.setNome(nome);
        if (projeto.getDescricao() != null) {
            projeto.setDescricao(projeto.getDescricao().trim());
        }
        if (projeto.getPrazo() != null) {
            projeto.setPrazo(projeto.getPrazo().trim());
        }

        projetos.add(projeto);
        return true;
    }

    public List<Projeto> listar() {
        return List.copyOf(projetos);
    }

    // buscas
    public Projeto buscaPorId(int id) {
        for (Projeto projeto : projetos) {
            if (projeto.getId() == id) {
                return projeto;
            }
        }
        return null;
    }

    public List<Projeto> buscarPorCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return List.copyOf(projetos);
        }
        String alvo = categoria.trim();
        return projetos.stream()
                .filter(p -> p.getCategoria() != null && p.getCategoria().trim().equalsIgnoreCase(alvo))
                .toList();
    }

    public List<Projeto> buscarPorStatus(String status) {
        if (status == null || status.isBlank()) {
            return List.copyOf(projetos);
        }
        String alvo = status.trim();
        return projetos.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().trim().equalsIgnoreCase(alvo))
                .toList();
    }

    // remoções
    public boolean removerPorId(int id) {
        return projetos.removeIf(projeto -> projeto.getId() == id);
    }

    // alterações
    public boolean alterar(Projeto projetoAtualizado) {
        if (projetoAtualizado == null) {
            return false;
        }

        Projeto projeto = buscaPorId(projetoAtualizado.getId());
        if (projeto == null) {
            return false;
        }

        String nome = projetoAtualizado.getNome() != null ? projetoAtualizado.getNome().trim() : "";
        if (nome.isBlank()) {
            return false;
        }

        for (Projeto outro : projetos) {
            if (outro.getId() != projeto.getId()
                    && outro.getNome() != null
                    && outro.getNome().trim().equalsIgnoreCase(nome)) {
                return false;
            }
        }

        projeto.setNome(nome);
        projeto.setDescricao(projetoAtualizado.getDescricao() != null
                ? projetoAtualizado.getDescricao().trim() : "");
        projeto.setCategoria(projetoAtualizado.getCategoria());
        projeto.setStatus(projetoAtualizado.getStatus());
        projeto.setPrazo(projetoAtualizado.getPrazo() != null
                ? projetoAtualizado.getPrazo().trim() : "");

        return true;
    }

    public boolean alterar(int id, Projeto projetoAtualizado) {
        if (projetoAtualizado == null) {
            return false;
        }
        projetoAtualizado.setId(id);
        return alterar(projetoAtualizado);
    }

    public int gerarId() {
        int greatest = 0;
        for (Projeto projeto : projetos) {
            if (projeto.getId() > greatest) {
                greatest = projeto.getId();
            }
        }
        return greatest + 1;
    }

    @Override
    public String toString() {
        return "ProjetoService{totalProjetos=" + projetos.size() + ", dao=" + Objects.toString(dao) + "}";
    }
} 