package br.com.arthurhenrique;

import java.time.LocalDate;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import br.com.arthurhenrique.model.Projeto;
import br.com.arthurhenrique.model.Usuario;
import br.com.arthurhenrique.service.AuthService;
import br.com.arthurhenrique.service.ProjetoService;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        AuthService authService = new AuthService();
        authService.inicializar();

        String usuarioLogado = realizarLoginConsole(sc, authService);
        if (usuarioLogado == null) {
            System.out.println("Encerrando programa...");
            sc.close();
            return;
        }

        ProjetoService projetoService = new ProjetoService();
        projetoService.carregar();

        int opcao = -1;

        do {
            try {
                System.out.println("==========================\n   SISTEMA DE PROJETOS   \n==========================");
                System.out.println("Usuário: " + usuarioLogado);
                System.out.println("Total de projetos: " + projetoService.listar().size());

                System.out.println(
                        "1 - Listar\n2 - Buscar projeto\n3 - Cadastrar projeto\n4 - Alterar projeto\n5 - Excluir projeto\n6 - Busca por categoria\n7 - Busca por status\n0 - Sair\n");
                System.out.print("Escolha: ");
                opcao = sc.nextInt();
                sc.nextLine();
                System.out.println();
            } catch (InputMismatchException e) {
                System.out.println("Erro: Insira apenas números");
                sc.nextLine();
                continue;
            }

            switch (opcao) {
                case 1 -> {
                    System.out.println("=== LISTA DE PROJETOS ===");
                    for (Projeto projeto : projetoService.listar()) {
                        projeto.exibirDados();
                    }
                }
                case 2 -> {
                    System.out.println("=== BUSCAR POR ID ===");
                    System.out.print("Digite o ID: ");
                    int idBusca = sc.nextInt();
                    sc.nextLine();
                    Projeto projetoId = projetoService.buscaPorId(idBusca);
                    if (projetoId != null) {
                        projetoId.exibirDados();
                    } else {
                        System.out.println("\nProjeto não encontrado");
                    }
                }
                case 3 -> {
                    System.out.println("=== CADASTRAR PROJETO ===");
                    try {
                        int id = projetoService.gerarId();

                        System.out.print("Nome: ");
                        String nome = sc.nextLine();

                        System.out.print("Descrição: ");
                        String descricao = sc.nextLine();

                        System.out.print("Categoria (Software, Mobile, Web, Desktop): ");
                        String categoria = sc.nextLine();

                        System.out.print("Status (Planejado, Em Desenvolvimento, Concluído, Cancelado): ");
                        String status = sc.nextLine();

                        System.out.print("Prazo (dd/MM/yyyy ou yyyy-MM-dd, Enter para vazio): ");
                        String prazoTexto = sc.nextLine();
                        LocalDate prazo = ProjetoService.parseDataFlexivel(prazoTexto);

                        Projeto projeto = new Projeto(id, nome, descricao, categoria, status, LocalDate.now(), prazo);
                        projeto.setUsuarioLogin(usuarioLogado);

                        projetoService.adicionar(projeto);
                        projetoService.salvar();
                        System.out.println("\nProjeto cadastrado com sucesso");
                    } catch (IllegalArgumentException e) {
                        System.out.println("\nErro de validação: " + e.getMessage());
                    }
                }  
                case 4 -> {
                    System.out.println("=== ATUALIZAR PROJETO ===");
                    System.out.print("Digite o ID do projeto: ");
                    int idAlterar = sc.nextInt();
                    sc.nextLine();

                    Projeto existente = projetoService.buscaPorId(idAlterar);
                    if (existente == null) {
                        System.out.println("Projeto não encontrado");
                        break;
                    }

                    System.out.println("\nProjeto atual:");
                    existente.exibirDados();

                    try {
                        System.out.print("Novo nome: ");
                        String novoNome = sc.nextLine();

                        System.out.print("Nova descrição: ");
                        String novaDescricao = sc.nextLine();

                        System.out.print("Nova categoria (Software, Mobile, Web, Desktop): ");
                        String novaCategoria = sc.nextLine();

                        System.out.print("Novo status (Planejado, Em Desenvolvimento, Concluído, Cancelado): ");
                        String novoStatus = sc.nextLine();

                        System.out.print("Novo prazo (dd/MM/yyyy ou yyyy-MM-dd, Enter para vazio): ");
                        String novoPrazoTexto = sc.nextLine();
                        LocalDate novoPrazo = ProjetoService.parseDataFlexivel(novoPrazoTexto);

                        Projeto projetoAtualizado = new Projeto(
                                idAlterar,
                                novoNome,
                                novaDescricao,
                                novaCategoria,
                                novoStatus,
                                existente.getDataCriacao(),
                                novoPrazo
                        );
                        projetoAtualizado.setUsuarioLogin(existente.getUsuarioLogin());

                        if (projetoService.alterar(projetoAtualizado)) {
                            projetoService.salvar();
                            System.out.printf("\nProjeto %d alterado com sucesso\n", idAlterar);
                        } else {
                            System.out.println("\nNão foi possível alterar o projeto");
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("\nErro de validação: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.println("=== REMOVER PROJETO ===");
                    System.out.print("Digite o ID do projeto: ");
                    int idRemover = sc.nextInt();
                    sc.nextLine();

                    Projeto existenteRemover = projetoService.buscaPorId(idRemover);
                    if (existenteRemover == null) {
                        System.out.println("\nProjeto não encontrado");
                        break;
                    }

                    System.out.print("Confirmar a exclusão? (S/N): ");
                    String confirmacao = sc.nextLine();

                    if (confirmacao.equalsIgnoreCase("s")) {
                        if (projetoService.removerPorId(idRemover)) {
                            projetoService.salvar();
                            System.out.println("\nProjeto excluído com sucesso");
                        } else {
                            System.out.println("Exclusão cancelada");
                        }
                    }
                }
                case 6 -> {
                    System.out.println("=== BUSCA POR CATEGORIA ===");
                    System.out.print("Digite a categoria: ");
                    String buscaCategoria = sc.nextLine();

                    List<Projeto> categoriaList = projetoService.buscarPorCategoria(buscaCategoria);
                    System.out.println();

                    if (categoriaList.isEmpty()) {
                        System.out.println("Nenhum projeto encontrado");
                        break;
                    }

                    for (Projeto projeto : categoriaList) {
                        projeto.exibirDados();
                    }
                }
                case 7 -> {
                    System.out.println("=== BUSCA POR STATUS ===");
                    System.out.print("Digite o status (Concluído, Em Desenvolvimento, Planejado, Cancelado): ");
                    String buscaStatus = sc.nextLine();

                    List<Projeto> statusList = projetoService.buscarPorStatus(buscaStatus);
                    System.out.println();

                    if (statusList.isEmpty()) {
                        System.out.println("Nenhum projeto encontrado");
                        break;
                    }

                    for (Projeto projeto : statusList) {
                        projeto.exibirDados();
                    }
                }
                case 0 -> {
                    System.out.println("Encerrando programa...");
                    sc.close();
                    System.exit(0);
                }
            }
        } while (true);
    }

    private static String realizarLoginConsole(Scanner sc, AuthService authService) throws Exception {
        while (true) {
            System.out.println("==========================\n          LOGIN           \n==========================");
            System.out.println("1 - Entrar\n2 - Criar conta\n0 - Sair");
            System.out.print("Escolha: ");
            String escolha = sc.nextLine().trim();

            if ("0".equals(escolha)) {
                return null;
            }

            if ("2".equals(escolha)) {
                try {
                    System.out.print("Nome completo: ");
                    String nome = sc.nextLine();
                    System.out.print("Usuário: ");
                    String usuario = sc.nextLine();
                    System.out.print("Senha: ");
                    String senha = sc.nextLine();

                    authService.cadastrar(nome, usuario, senha);
                    System.out.println("Conta criada com sucesso! Faça login.\n");
                } catch (IllegalArgumentException e) {
                    System.out.println("Erro: " + e.getMessage() + "\n");
                }
                continue;
            }

            if ("1".equals(escolha)) {
                System.out.print("Usuário: ");
                String login = sc.nextLine().trim();
                System.out.print("Senha: ");
                String senha = sc.nextLine();

                @SuppressWarnings("unchecked")
                Optional<Usuario> resultado = authService.autenticar(login, senha);
                if (resultado.isPresent()) {
                    Usuario usuario = resultado.get();
                    String nomeUsuario = usuario.getUsuario();
                    // mantém aqui o código que continua após o login
                } else {
                    System.out.println("Usuário ou senha inválidos.\n");
                }
            }
        }
    }
}