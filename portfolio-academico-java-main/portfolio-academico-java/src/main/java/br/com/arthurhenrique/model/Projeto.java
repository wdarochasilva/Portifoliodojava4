package br.com.arthurhenrique.model;

public class Projeto {

    private int id;
    private String nome;
    private String descricao;
    private String categoria;
    private String status;
    private String prazo;

    public Projeto() {
    }

    public Projeto(int id, String nome, String descricao, String categoria, String status) {
        this(id, nome, descricao, categoria, status, "");
    }

    public Projeto(int id, String nome, String descricao, String categoria, String status, String prazo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.status = status;
        this.prazo = prazo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrazo() {
        return prazo;
    }

    public void setPrazo(String prazo) {
        this.prazo = prazo;
    }

    public void exibirDados() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'exibirDados'");
    }
}