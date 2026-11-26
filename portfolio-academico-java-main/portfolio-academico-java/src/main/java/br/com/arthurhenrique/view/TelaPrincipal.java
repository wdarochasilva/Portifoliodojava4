package br.com.arthurhenrique.view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import br.com.arthurhenrique.api.Api;
import br.com.arthurhenrique.client.ApiClient;
import br.com.arthurhenrique.log.LogManager;
import br.com.arthurhenrique.model.Projeto;
import br.com.arthurhenrique.service.ProjetoService;

public class TelaPrincipal extends JFrame {

    // =====================================================================
    // CONSTANTES
    // =====================================================================

    private static final Color COR_FUNDO = new Color(236, 239, 241);
    private static final Color COR_SIDEBAR = new Color(38, 52, 58);
    private static final Color COR_SIDEBAR_ATIVO = new Color(55, 71, 79);
    private static final Color COR_TOPO = new Color(38, 50, 56);
    private static final Color COR_CARD = Color.WHITE;
    private static final Color COR_BORDA = new Color(220, 226, 232);
    private static final Color COR_TEXTO = new Color(33, 37, 41);
    private static final Color COR_MUTED = new Color(84, 110, 122);

    private static final Color COR_AZUL = new Color(25, 118, 210);
    private static final Color COR_VERDE = new Color(46, 125, 50);
    private static final Color COR_LARANJA = new Color(239, 108, 0);
    private static final Color COR_VERMELHO = new Color(198, 40, 40);

    private static final Font FONTE_TITULO = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONTE_SUBTITULO = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONTE_SECAO = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONTE_PADRAO = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONTE_BOTAO = new Font("Segoe UI", Font.BOLD, 13);

    private static final String[] ROTAS = { "DASHBOARD", "PROJETOS", "CATEGORIAS", "RELATORIOS" };
    private static final String[] ROTULOS_MENU = { "Dashboard", "Projetos", "Categorias", "Relatórios" };
    private static final String[] CATEGORIAS = { "Software", "Mobile", "Web", "Desktop" };
    private static final String[] STATUS = { "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado" };
    private static final String[] OPCOES_CATEGORIA = comOpcaoTodos("Todas", CATEGORIAS);
    private static final String[] OPCOES_STATUS = comOpcaoTodos("Todos", STATUS);
    private static final String[] COLUNAS = { "ID", "Nome", "Categoria", "Status", "Prazo" };

    private static final int COL_ID = 0;
    private static final int COL_NOME = 1;
    private static final int COL_CATEGORIA = 2;
    private static final int COL_STATUS = 3;
    private static final int COL_PRAZO = 4;

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] TITULOS_PAGINA = {
            "Sistema de Projetos",
            "Sistema de Projetos",
            "Categorias",
            "Relatórios"
    };

    private static final String[] SUBTITULOS = {
            "Visão geral e indicadores do portfólio",
            "Gerenciamento de projetos e portfólio",
            "Exploração e filtragem de projetos por categoria",
            "Relatórios detalhados, filtros e exportação de dados"
    };

    // =====================================================================
    // CAMPOS
    // =====================================================================

    private final ProjetoService service;
    private final ApiClient apiClient;
    private final String usuarioLogado;
    private final String nomeUsuarioLogado;
    private final String perfilUsuarioLogado;

    private List<Projeto> projetosCache = new ArrayList<>();
    private boolean atualizandoTabela = false;
    private boolean erroCargaExibido = false;

    // Navegação lateral + conteúdo central
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel painelConteudo = new JPanel(cardLayout);
    private final Map<String, JButton> botoesMenu = new LinkedHashMap<>();
    private String rotaAtiva = "DASHBOARD";
    private JLabel tituloCabecalho;
    private JLabel subtituloCabecalho;
    private JButton botaoSair;

    // Aba Projetos
    private JTextField campoNome;
    private JTextArea campoDescricao;
    private JTextField campoPrazo;
    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboStatus;
    private JButton botaoCadastrar;
    private JButton botaoLimpar;
    private JButton botaoExcluir;
    private JButton botaoAlterar;
    private JTextField campoBuscaId;
    private TableRowSorter<TableModel> sorterProjetos;
    private DefaultTableModel modelo;
    private JTable tabela;

    // Aba Categorias
    private String categoriaAtiva = "Todas";
    private Map<String, JButton> botoesCategoria;
    private DefaultTableModel modeloCategorias;
    private JLabel lblBadgeProjetosCat;

    // Dashboard (2 colunas + 4 KPIs + 4 gráficos + últimos projetos)
    private JLabel lblKpiTotal;
    private JLabel lblKpiConcluidos;
    private JLabel lblKpiAtivos;
    private JLabel lblKpiCancelados;
    private JLabel lblResumoPrazos;
    private JPanel listaUltimosProjetos;

    private final PainelGraficoStatus graficoStatus = new PainelGraficoStatus();
    private final PainelGraficoCategoria graficoCategoria = new PainelGraficoCategoria();
    private final PainelGraficoMensal graficoMensal = new PainelGraficoMensal();
    private final PainelGraficoPrazos graficoPrazos = new PainelGraficoPrazos();

    // Aba Relatórios
    private JTextField campoBuscaRelatorio;
    private JComboBox<String> comboFiltroCategoria;
    private JComboBox<String> comboFiltroStatus;
    private JLabel lblQuantidadeRegistros;
    private JTable tabelaRelatorio;
    private TableRowSorter<TableModel> sorterRelatorios;

    // =====================================================================
    // CONSTRUTORES
    // =====================================================================

    public TelaPrincipal() throws Exception {
        this(System.getProperty("user.name"), System.getProperty("user.name"), "USER");
    }

    public TelaPrincipal(String usuarioLogado, String nomeUsuarioLogado, String perfilUsuarioLogado) throws Exception {
        this.usuarioLogado = (usuarioLogado != null && !usuarioLogado.isBlank())
                ? usuarioLogado.trim()
                : System.getProperty("user.name");
        this.nomeUsuarioLogado = (nomeUsuarioLogado != null && !nomeUsuarioLogado.isBlank())
                ? nomeUsuarioLogado.trim()
                : this.usuarioLogado;
        this.perfilUsuarioLogado = (perfilUsuarioLogado != null && !perfilUsuarioLogado.isBlank())
                ? perfilUsuarioLogado.trim()
                : "USER";

        setTitle("Sistema de Projetos - " + this.nomeUsuarioLogado);
        setSize(1240, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().setBackground(COR_FUNDO);
        setLayout(new BorderLayout());

        service = new ProjetoService();
        apiClient = new ApiClient(this.usuarioLogado);

        criarComponentes();
        criarEventos();

        try {
            service.carregar();
        } catch (Exception e) {
            LogManager.error("DESKTOP_CARGA", e.getMessage());
            JOptionPane.showMessageDialog(this, "Erro ao carregar: " + e.getMessage());
        }
        carregarTabela();
    }

    // =====================================================================
    // MONTAGEM DA INTERFACE - MENU LATERAL + CARDLAYOUT CENTRAL
    // =====================================================================

    public void criarComponentes() {
        criarFormulario();
        criarTabelaProjetos();

        painelConteudo.setBackground(COR_FUNDO);
        painelConteudo.add(criarPainelDashboard(), "DASHBOARD");
        painelConteudo.add(criarAbaProjetos(), "PROJETOS");
        painelConteudo.add(criarAbaCategorias(), "CATEGORIAS");
        painelConteudo.add(criarAbaRelatorios(), "RELATORIOS");

        add(criarMenuLateral(), BorderLayout.WEST);
        add(painelConteudo, BorderLayout.CENTER);
        selecionarRota("DASHBOARD");

        revalidate();
        repaint();
    }

    private JPanel criarMenuLateral() {
        JPanel menu = new JPanel();
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setPreferredSize(new Dimension(225, 0));
        menu.setBackground(COR_SIDEBAR);
        menu.setBorder(new EmptyBorder(22, 16, 20, 16));

        JLabel marca = new JLabel("<html>Sistema de<br>Projetos</html>");
        marca.setFont(new Font("Segoe UI", Font.BOLD, 20));
        marca.setForeground(Color.WHITE);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divisorTopo = new JPanel();
        divisorTopo.setBackground(new Color(255, 255, 255, 30));
        divisorTopo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divisorTopo.setPreferredSize(new Dimension(0, 1));
        divisorTopo.setAlignmentX(Component.LEFT_ALIGNMENT);

        menu.add(marca);
        menu.add(Box.createVerticalStrut(16));
        menu.add(divisorTopo);
        menu.add(Box.createVerticalStrut(16));

        for (int i = 0; i < ROTAS.length; i++) {
            String rota = ROTAS[i];
            String rotulo = ROTULOS_MENU[i];
            JButton btn = new JButton(rotulo);
            btn.setFont(FONTE_BOTAO);
            btn.setForeground(Color.WHITE);
            btn.setBackground(COR_SIDEBAR);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            btn.setBorder(new EmptyBorder(9, 14, 9, 14));
            btn.addActionListener(e -> selecionarRota(rota));
            botoesMenu.put(rota, btn);

            menu.add(btn);
            menu.add(Box.createVerticalStrut(6));
        }

        menu.add(Box.createVerticalGlue());

        JPanel divisorRodape = new JPanel();
        divisorRodape.setBackground(new Color(255, 255, 255, 30));
        divisorRodape.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divisorRodape.setPreferredSize(new Dimension(0, 1));
        divisorRodape.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblUsuario = new JLabel(nomeUsuarioLogado + " (" + perfilUsuarioLogado + ")");
        lblUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUsuario.setForeground(new Color(176, 190, 197));
        lblUsuario.setAlignmentX(Component.LEFT_ALIGNMENT);

        botaoSair = new JButton("Sair");
        botaoSair.setFont(FONTE_BOTAO);
        botaoSair.setForeground(Color.WHITE);
        botaoSair.setBackground(new Color(47, 62, 70));
        botaoSair.setFocusPainted(false);
        botaoSair.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botaoSair.setAlignmentX(Component.LEFT_ALIGNMENT);
        botaoSair.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        botaoSair.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 55), 1, true),
                new EmptyBorder(6, 12, 6, 12)));

        menu.add(divisorRodape);
        menu.add(Box.createVerticalStrut(14));
        menu.add(lblUsuario);
        menu.add(Box.createVerticalStrut(10));
        menu.add(botaoSair);

        return menu;
    }

    private void selecionarRota(String rota) {
        rotaAtiva = rota;
        cardLayout.show(painelConteudo, rota);

        int idx = 0;
        for (int i = 0; i < ROTAS.length; i++) {
            if (ROTAS[i].equals(rota)) {
                idx = i;
                break;
            }
        }

        if (tituloCabecalho != null) {
            tituloCabecalho.setText(TITULOS_PAGINA[idx]);
        }
        if (subtituloCabecalho != null) {
            subtituloCabecalho.setText(SUBTITULOS[idx]);
        }

        for (Map.Entry<String, JButton> entry : botoesMenu.entrySet()) {
            boolean ativa = entry.getKey().equals(rota);
            entry.getValue().setBackground(ativa ? COR_SIDEBAR_ATIVO : COR_SIDEBAR);
        }

        if ("DASHBOARD".equals(rota)) {
            atualizarDashboard();
        }
    }

    public void criarEventos() {
        botaoCadastrar.addActionListener(e -> cadastrar());
        botaoLimpar.addActionListener(e -> limparFormulario());
        botaoExcluir.addActionListener(e -> excluirProjeto());
        botaoAlterar.addActionListener(e -> alterarProjeto());
        botaoSair.addActionListener(e -> encerrarSessao());
    }

    private void encerrarSessao() {
        LogManager.access(usuarioLogado, "LOGOUT_SWING");
        dispose();

        SwingUtilities.invokeLater(() -> {
            TelaLogin login = new TelaLogin(null);
            login.setVisible(true);
            if (!login.isAutenticado()) {
                System.exit(0);
                return;
            }
            try {
                TelaPrincipal novaTela = new TelaPrincipal(
                        login.getUsuarioLogado(),
                        login.getNomeUsuarioLogado(),
                        login.getPerfilUsuarioLogado()
                );
                novaTela.setVisible(true);
            } catch (Exception ex) {
                LogManager.error("SWING_RELOGIN", ex.getMessage());
                JOptionPane.showMessageDialog(null, "Erro ao abrir janela: " + ex.getMessage());
            }
        });
    }

    // =====================================================================
    // DASHBOARD - 4 KPIs NO TOPO + 2 COLUNAS (GRÁFICOS | ÚLTIMOS PROJETOS)
    // =====================================================================

    private JPanel criarPainelDashboard() {
        lblKpiTotal = new JLabel("0");
        lblKpiConcluidos = new JLabel("0");
        lblKpiAtivos = new JLabel("0");
        lblKpiCancelados = new JLabel("0");
        lblResumoPrazos = new JLabel("No prazo: 0 (0%) | Atrasados: 0 (0%)");

        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.setOpaque(false);
        cabecalho.setAlignmentX(Component.LEFT_ALIGNMENT);

        tituloCabecalho = new JLabel(TITULOS_PAGINA[0]);
        tituloCabecalho.setFont(FONTE_TITULO);
        tituloCabecalho.setForeground(COR_TEXTO);
        tituloCabecalho.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtituloCabecalho = new JLabel(SUBTITULOS[0]);
        subtituloCabecalho.setFont(FONTE_SUBTITULO);
        subtituloCabecalho.setForeground(COR_MUTED);
        subtituloCabecalho.setAlignmentX(Component.LEFT_ALIGNMENT);

        cabecalho.add(tituloCabecalho);
        cabecalho.add(Box.createVerticalStrut(3));
        cabecalho.add(subtituloCabecalho);

        JPanel linhaKpis = new JPanel(new GridLayout(1, 4, 16, 16));
        linhaKpis.setOpaque(false);
        linhaKpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        linhaKpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        linhaKpis.add(criarCartaoKpi("Total de Projetos", lblKpiTotal, COR_AZUL));
        linhaKpis.add(criarCartaoKpi("Projetos Concluídos", lblKpiConcluidos, COR_VERDE));
        linhaKpis.add(criarCartaoKpi("Projetos Ativos", lblKpiAtivos, COR_LARANJA));
        linhaKpis.add(criarCartaoKpi("Projetos Cancelados", lblKpiCancelados, COR_VERMELHO));

        JPanel colunaEsquerda = new JPanel();
        colunaEsquerda.setLayout(new BoxLayout(colunaEsquerda, BoxLayout.Y_AXIS));
        colunaEsquerda.setOpaque(false);
        colunaEsquerda.add(criarCardGrafico("Status dos Projetos", graficoStatus, null));
        colunaEsquerda.add(Box.createVerticalStrut(16));
        colunaEsquerda.add(criarCardGrafico("Projetos por Categoria", graficoCategoria, null));
        colunaEsquerda.add(Box.createVerticalStrut(16));
        colunaEsquerda.add(criarCardGrafico("Evolução Mensal", graficoMensal, null));
        colunaEsquerda.add(Box.createVerticalStrut(16));
        colunaEsquerda.add(criarCardGrafico("Cumprimento de Prazos", graficoPrazos, lblResumoPrazos));

        listaUltimosProjetos = new JPanel();
        listaUltimosProjetos.setLayout(new BoxLayout(listaUltimosProjetos, BoxLayout.Y_AXIS));
        listaUltimosProjetos.setOpaque(false);

        JPanel cardUltimos = criarCard();
        cardUltimos.setLayout(new BorderLayout(0, 12));
        cardUltimos.add(criarTituloSecao("Últimos Projetos Cadastrados"), BorderLayout.NORTH);
        cardUltimos.add(listaUltimosProjetos, BorderLayout.CENTER);

        JPanel gradeInferior = new JPanel(new GridBagLayout());
        gradeInferior.setOpaque(false);
        gradeInferior.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbcEsquerda = new GridBagConstraints();
        gbcEsquerda.gridx = 0;
        gbcEsquerda.gridy = 0;
        gbcEsquerda.weightx = 0.64;
        gbcEsquerda.weighty = 1.0;
        gbcEsquerda.fill = GridBagConstraints.BOTH;
        gbcEsquerda.insets = new Insets(0, 0, 0, 18);

        GridBagConstraints gbcDireita = new GridBagConstraints();
        gbcDireita.gridx = 1;
        gbcDireita.gridy = 0;
        gbcDireita.weightx = 0.36;
        gbcDireita.weighty = 1.0;
        gbcDireita.fill = GridBagConstraints.BOTH;

        gradeInferior.add(colunaEsquerda, gbcEsquerda);
        gradeInferior.add(cardUltimos, gbcDireita);

        JPanel corpo = new JPanel();
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        corpo.setBackground(COR_FUNDO);
        corpo.setBorder(new EmptyBorder(24, 30, 24, 30));
        corpo.add(cabecalho);
        corpo.add(Box.createVerticalStrut(18));
        corpo.add(linhaKpis);
        corpo.add(Box.createVerticalStrut(18));
        corpo.add(gradeInferior);

        JScrollPane scroll = new JScrollPane(corpo);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(COR_FUNDO);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(COR_FUNDO);
        painel.add(scroll, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarCartaoKpi(String titulo, JLabel lblValor, Color corDestaque) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, corDestaque),
                new EmptyBorder(14, 18, 14, 18)));

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblValor.setForeground(COR_TEXTO);
        lblValor.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitulo.setForeground(COR_MUTED);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblValor);
        card.add(Box.createVerticalStrut(3));
        card.add(lblTitulo);
        return card;
    }

    private JPanel criarCardGrafico(String titulo, JComponent grafico, JLabel rodape) {
        JPanel card = criarCard();
        card.setLayout(new BorderLayout(0, 8));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(0, 180));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 195));

        card.add(criarTituloSecao(titulo), BorderLayout.NORTH);
        card.add(grafico, BorderLayout.CENTER);

        if (rodape != null) {
            rodape.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            rodape.setForeground(COR_MUTED);
            rodape.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(rodape, BorderLayout.SOUTH);
        }
        return card;
    }

    private void atualizarDashboard() {
        if (lblKpiTotal == null) {
            return;
        }

        int total = projetosCache.size();
        int concluidos = 0;
        int ativos = 0;
        int cancelados = 0;
        int emDesenvolvimento = 0;
        int planejado = 0;
        int noPrazo = 0;
        int atrasados = 0;

        int[] porCategoria = new int[CATEGORIAS.length];
        int[] porMes = new int[12];
        LocalDate hoje = LocalDate.now();

        for (Projeto p : projetosCache) {
            String status = p.getStatus() != null ? p.getStatus().trim().toUpperCase() : "";
            boolean isConcluido = status.contains("CONCLU");
            boolean isCancelado = status.contains("CANCEL");

            if (isConcluido) {
                concluidos++;
            } else if (isCancelado) {
                cancelados++;
            } else {
                ativos++;
            }

            if (status.contains("DESENV")) {
                emDesenvolvimento++;
            }
            if (status.contains("PLANEJ")) {
                planejado++;
            }

            String cat = p.getCategoria() != null ? p.getCategoria().trim() : "";
            for (int i = 0; i < CATEGORIAS.length; i++) {
                if (CATEGORIAS[i].equalsIgnoreCase(cat)) {
                    porCategoria[i]++;
                    break;
                }
            }

            LocalDate dataRef = p.getDataCriacao() != null ? p.getDataCriacao() : p.getPrazo();
            if (dataRef != null) {
                porMes[dataRef.getMonthValue() - 1]++;
            }

            if (p.getPrazo() != null) {
                if (isConcluido || !p.getPrazo().isBefore(hoje)) {
                    noPrazo++;
                } else {
                    atrasados++;
                }
            }
        }

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiConcluidos.setText(String.valueOf(concluidos));
        lblKpiAtivos.setText(String.valueOf(ativos));
        lblKpiCancelados.setText(String.valueOf(cancelados));

        int totalPrazos = noPrazo + atrasados;
        int pctNoPrazo = totalPrazos > 0 ? Math.round((noPrazo * 100f) / totalPrazos) : 0;
        int pctAtrasados = totalPrazos > 0 ? Math.round((atrasados * 100f) / totalPrazos) : 0;
        lblResumoPrazos.setText("No prazo: " + noPrazo + " (" + pctNoPrazo + "%) | Atrasados: "
                + atrasados + " (" + pctAtrasados + "%)");

        graficoStatus.setDados(concluidos, emDesenvolvimento, planejado);
        graficoCategoria.setDados(porCategoria);
        graficoMensal.setDados(porMes);
        graficoPrazos.setDados(noPrazo, atrasados);

        atualizarListaUltimosProjetos();
    }

    private void atualizarListaUltimosProjetos() {
        listaUltimosProjetos.removeAll();

        if (projetosCache.isEmpty()) {
            JLabel vazio = new JLabel("Nenhum projeto cadastrado.");
            vazio.setFont(FONTE_PADRAO);
            vazio.setForeground(COR_MUTED);
            vazio.setAlignmentX(Component.LEFT_ALIGNMENT);
            listaUltimosProjetos.add(vazio);
        } else {
            int inicio = Math.max(0, projetosCache.size() - 5);
            for (int i = projetosCache.size() - 1; i >= inicio; i--) {
                Projeto p = projetosCache.get(i);
                String prazoFmt = formatarPrazo(p);
                String infoPrazo = prazoFmt.isEmpty() ? "" : " • Prazo: " + prazoFmt;

                JPanel itemCard = new JPanel();
                itemCard.setLayout(new BoxLayout(itemCard, BoxLayout.Y_AXIS));
                itemCard.setBackground(new Color(248, 250, 252));
                itemCard.setAlignmentX(Component.LEFT_ALIGNMENT);
                itemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
                itemCard.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COR_BORDA, 1, true),
                        new EmptyBorder(8, 12, 8, 12)));

                JLabel lblNome = new JLabel("#" + p.getId() + " • " + (p.getNome() != null ? p.getNome() : ""));
                lblNome.setFont(FONTE_BOTAO);
                lblNome.setForeground(COR_TEXTO);
                lblNome.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel lblMeta = new JLabel((p.getCategoria() != null ? p.getCategoria() : "")
                        + " — " + (p.getStatus() != null ? p.getStatus() : "") + infoPrazo);
                lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lblMeta.setForeground(COR_MUTED);
                lblMeta.setAlignmentX(Component.LEFT_ALIGNMENT);

                itemCard.add(lblNome);
                itemCard.add(Box.createVerticalStrut(2));
                itemCard.add(lblMeta);

                listaUltimosProjetos.add(itemCard);
                listaUltimosProjetos.add(Box.createVerticalStrut(8));
            }
        }

        listaUltimosProjetos.revalidate();
        listaUltimosProjetos.repaint();
    }

    // =====================================================================
    // ABA PROJETOS - 2 COLUNAS (CADASTRAR PROJETO | PROJETOS CADASTRADOS)
    // =====================================================================

    private void criarFormulario() {
        campoNome = new JTextField();
        campoDescricao = new JTextArea(4, 20);
        campoDescricao.setLineWrap(true);
        campoDescricao.setWrapStyleWord(true);
        campoPrazo = new JTextField();
        campoPrazo.setToolTipText("AAAA-MM-DD ou DD/MM/AAAA");

        comboCategoria = new JComboBox<>(CATEGORIAS);
        comboStatus = new JComboBox<>(STATUS);

        botaoCadastrar = new JButton("Cadastrar projeto");
        botaoAlterar = new JButton("Alterar");
        botaoExcluir = new JButton("Excluir");
        botaoLimpar = new JButton("Limpar");

        estilizarCampo(campoNome);
        campoDescricao.setFont(FONTE_PADRAO);
        campoDescricao.setForeground(COR_TEXTO);
        campoDescricao.setBackground(Color.WHITE);
        campoDescricao.setBorder(new EmptyBorder(6, 10, 6, 10));
        estilizarCampo(campoPrazo);
        estilizarCombo(comboCategoria);
        estilizarCombo(comboStatus);

        estilizarBotao(botaoCadastrar, new Color(241, 243, 245), COR_TEXTO);
        estilizarBotao(botaoAlterar, new Color(255, 152, 0), Color.WHITE);
        estilizarBotao(botaoExcluir, COR_VERMELHO, Color.WHITE);
        estilizarBotao(botaoLimpar, new Color(108, 117, 125), Color.WHITE);
    }

    private void criarTabelaProjetos() {
        modelo = criarModeloTabela();
        sorterProjetos = new TableRowSorter<>(modelo);
        tabela = new JTable(modelo);
        tabela.setRowSorter(sorterProjetos);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || atualizandoTabela) {
                return;
            }
            int linhaView = tabela.getSelectedRow();
            if (linhaView != -1) {
                preencherFormulario(idDaLinha(linhaView));
            }
        });
    }

    private JPanel criarAbaProjetos() {
        JPanel topoBranco = new JPanel();
        topoBranco.setLayout(new BoxLayout(topoBranco, BoxLayout.Y_AXIS));
        topoBranco.setBackground(COR_CARD);
        topoBranco.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COR_BORDA),
                new EmptyBorder(18, 28, 18, 28)));

        JLabel h1 = new JLabel("Sistema de Projetos");
        h1.setFont(FONTE_TITULO);
        h1.setForeground(COR_TEXTO);
        h1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Gerenciamento de projetos e portfólio");
        sub.setFont(FONTE_SUBTITULO);
        sub.setForeground(COR_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        topoBranco.add(h1);
        topoBranco.add(Box.createVerticalStrut(3));
        topoBranco.add(sub);

        // Coluna esquerda: formulário vertical
        JPanel formVertical = new JPanel();
        formVertical.setLayout(new BoxLayout(formVertical, BoxLayout.Y_AXIS));
        formVertical.setBackground(COR_CARD);

        JLabel tituloForm = new JLabel("Cadastrar projeto");
        tituloForm.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tituloForm.setForeground(COR_TEXTO);
        tituloForm.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane scrollDescricao = new JScrollPane(campoDescricao);
        scrollDescricao.setBorder(BorderFactory.createLineBorder(COR_BORDA, 1, true));
        scrollDescricao.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollDescricao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        ajustarLarguraTotal(campoNome, 36);
        ajustarLarguraTotal(comboCategoria, 36);
        ajustarLarguraTotal(comboStatus, 36);
        ajustarLarguraTotal(campoPrazo, 36);

        botaoCadastrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        botaoCadastrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel linhaAcoesSecundarias = new JPanel(new GridLayout(1, 3, 8, 0));
        linhaAcoesSecundarias.setBackground(COR_CARD);
        linhaAcoesSecundarias.setAlignmentX(Component.LEFT_ALIGNMENT);
        linhaAcoesSecundarias.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        linhaAcoesSecundarias.add(botaoAlterar);
        linhaAcoesSecundarias.add(botaoExcluir);
        linhaAcoesSecundarias.add(botaoLimpar);

        formVertical.add(tituloForm);
        formVertical.add(Box.createVerticalStrut(14));
        formVertical.add(criarRotuloBloco("Nome:"));
        formVertical.add(Box.createVerticalStrut(4));
        formVertical.add(campoNome);
        formVertical.add(Box.createVerticalStrut(10));
        formVertical.add(criarRotuloBloco("Descrição:"));
        formVertical.add(Box.createVerticalStrut(4));
        formVertical.add(scrollDescricao);
        formVertical.add(Box.createVerticalStrut(10));
        formVertical.add(criarRotuloBloco("Categoria:"));
        formVertical.add(Box.createVerticalStrut(4));
        formVertical.add(comboCategoria);
        formVertical.add(Box.createVerticalStrut(10));
        formVertical.add(criarRotuloBloco("Status:"));
        formVertical.add(Box.createVerticalStrut(4));
        formVertical.add(comboStatus);
        formVertical.add(Box.createVerticalStrut(10));
        formVertical.add(criarRotuloBloco("Prazo (opcional):"));
        formVertical.add(Box.createVerticalStrut(4));
        formVertical.add(campoPrazo);
        formVertical.add(Box.createVerticalStrut(14));
        formVertical.add(botaoCadastrar);
        formVertical.add(Box.createVerticalStrut(8));
        formVertical.add(linhaAcoesSecundarias);

        JScrollPane scrollForm = new JScrollPane(formVertical);
        scrollForm.setBorder(null);
        scrollForm.getViewport().setBackground(COR_CARD);
        scrollForm.getVerticalScrollBar().setUnitIncrement(14);

        JPanel cardEsquerdo = criarCard();
        cardEsquerdo.setLayout(new BorderLayout());
        cardEsquerdo.add(scrollForm, BorderLayout.CENTER);

        // Coluna direita: projetos cadastrados + busca por ID
        JLabel tituloLista = new JLabel("Projetos cadastrados");
        tituloLista.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tituloLista.setForeground(COR_TEXTO);
        tituloLista.setAlignmentX(Component.LEFT_ALIGNMENT);

        campoBuscaId = new JTextField(12);
        estilizarCampo(campoBuscaId);

        JButton btnBuscarId = new JButton("Buscar");
        JButton btnMostrarTodos = new JButton("Mostrar todos");
        estilizarBotao(btnBuscarId, new Color(241, 243, 245), COR_TEXTO);
        estilizarBotao(btnMostrarTodos, new Color(241, 243, 245), COR_TEXTO);

        btnBuscarId.addActionListener(e -> filtrarProjetoPorId());
        btnMostrarTodos.addActionListener(e -> {
            campoBuscaId.setText("");
            sorterProjetos.setRowFilter(null);
        });

        JPanel linhaBusca = criarLinha(4);
        linhaBusca.add(criarRotulo("Buscar por ID:"));
        linhaBusca.add(campoBuscaId);

        JPanel linhaBotoesBusca = criarLinha(4);
        linhaBotoesBusca.add(btnBuscarId);
        linhaBotoesBusca.add(btnMostrarTodos);

        JPanel topoDireito = new JPanel();
        topoDireito.setLayout(new BoxLayout(topoDireito, BoxLayout.Y_AXIS));
        topoDireito.setBackground(COR_CARD);
        topoDireito.add(tituloLista);
        topoDireito.add(Box.createVerticalStrut(10));
        topoDireito.add(linhaBusca);
        topoDireito.add(linhaBotoesBusca);
        topoDireito.add(Box.createVerticalStrut(10));

        JScrollPane scrollTabela = new JScrollPane(tabela);
        estilizarTabela(tabela, scrollTabela);

        JPanel cardDireito = criarCard();
        cardDireito.setLayout(new BorderLayout(0, 8));
        cardDireito.add(topoDireito, BorderLayout.NORTH);
        cardDireito.add(scrollTabela, BorderLayout.CENTER);

        JPanel grade = new JPanel(new GridLayout(1, 2, 18, 0));
        grade.setBackground(COR_FUNDO);
        grade.setBorder(new EmptyBorder(20, 24, 20, 24));
        grade.add(cardEsquerdo);
        grade.add(cardDireito);

        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setBackground(COR_FUNDO);
        pagina.add(topoBranco, BorderLayout.NORTH);
        pagina.add(grade, BorderLayout.CENTER);
        return pagina;
    }

    private void filtrarProjetoPorId() {
        String idTexto = campoBuscaId.getText().trim();
        if (idTexto.isEmpty()) {
            sorterProjetos.setRowFilter(null);
            return;
        }
        sorterProjetos.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                return entry.getStringValue(COL_ID).trim().equals(idTexto);
            }
        });
    }

    private JLabel criarRotuloBloco(String texto) {
        JLabel lbl = criarRotulo(texto);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void ajustarLarguraTotal(JComponent comp, int altura) {
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, altura));
        comp.setPreferredSize(new Dimension(0, altura));
    }

    // =====================================================================
    // ABA CATEGORIAS
    // =====================================================================

    private JPanel criarAbaCategorias() {
        categoriaAtiva = OPCOES_CATEGORIA[0];
        botoesCategoria = new LinkedHashMap<>();
        modeloCategorias = criarModeloTabela();

        JPanel painelBotoes = criarLinha(8);
        for (String opcao : OPCOES_CATEGORIA) {
            JButton btn = new JButton(opcao);
            estilizarBotao(btn, new Color(241, 243, 245), COR_TEXTO);
            btn.addActionListener(e -> {
                categoriaAtiva = opcao;
                atualizarCategorias();
            });
            botoesCategoria.put(opcao, btn);
            painelBotoes.add(btn);
        }

        JPanel cardFiltro = criarCard();
        cardFiltro.setLayout(new BoxLayout(cardFiltro, BoxLayout.Y_AXIS));
        cardFiltro.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardFiltro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        cardFiltro.add(criarTituloSecao("Filtrar por Categoria"));
        cardFiltro.add(painelBotoes);

        JTable tabelaCategorias = new JTable(modeloCategorias);
        tabelaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollCategorias = new JScrollPane(tabelaCategorias);
        estilizarTabela(tabelaCategorias, scrollCategorias);

        lblBadgeProjetosCat = new JLabel("0 projeto(s)");
        lblBadgeProjetosCat.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBadgeProjetosCat.setForeground(COR_TEXTO);
        lblBadgeProjetosCat.setOpaque(true);
        lblBadgeProjetosCat.setBackground(new Color(238, 241, 244));
        lblBadgeProjetosCat.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setBackground(COR_CARD);
        cabecalho.add(criarTituloSecao("Todos os Projetos"), BorderLayout.WEST);
        cabecalho.add(lblBadgeProjetosCat, BorderLayout.EAST);

        JPanel cardTabela = criarCard();
        cardTabela.setLayout(new BorderLayout(0, 10));
        cardTabela.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardTabela.add(cabecalho, BorderLayout.NORTH);
        cardTabela.add(scrollCategorias, BorderLayout.CENTER);

        JPanel aba = criarPainelAba();
        aba.add(cardFiltro);
        aba.add(Box.createVerticalStrut(16));
        aba.add(cardTabela);
        return aba;
    }

    private void atualizarCategorias() {
        int totalGeral = modelo.getRowCount();
        Map<String, Integer> contagem = contarPorCategoria();

        modeloCategorias.setRowCount(0);
        for (int i = 0; i < totalGeral; i++) {
            String categoria = valorCelula(i, COL_CATEGORIA);
            if (categoriaAtiva.equals(OPCOES_CATEGORIA[0]) || categoria.equalsIgnoreCase(categoriaAtiva)) {
                modeloCategorias.addRow(new Object[] {
                        modelo.getValueAt(i, COL_ID),
                        modelo.getValueAt(i, COL_NOME),
                        modelo.getValueAt(i, COL_CATEGORIA),
                        modelo.getValueAt(i, COL_STATUS),
                        modelo.getValueAt(i, COL_PRAZO)
                });
            }
        }
        lblBadgeProjetosCat.setText(modeloCategorias.getRowCount() + " projeto(s)");

        for (String opcao : OPCOES_CATEGORIA) {
            JButton btn = botoesCategoria.get(opcao);
            int qtd = opcao.equals(OPCOES_CATEGORIA[0]) ? totalGeral : contagem.get(opcao);
            btn.setText(opcao + " (" + qtd + ")");

            if (opcao.equalsIgnoreCase(categoriaAtiva)) {
                estilizarBotao(btn, COR_TOPO, Color.WHITE);
            } else {
                estilizarBotao(btn, new Color(241, 243, 245), COR_TEXTO);
            }
        }
    }

    // =====================================================================
    // ABA RELATÓRIOS
    // =====================================================================

    private JPanel criarAbaRelatorios() {
        campoBuscaRelatorio = new JTextField(20);
        estilizarCampo(campoBuscaRelatorio);

        comboFiltroCategoria = new JComboBox<>(OPCOES_CATEGORIA);
        comboFiltroStatus = new JComboBox<>(OPCOES_STATUS);
        estilizarCombo(comboFiltroCategoria);
        estilizarCombo(comboFiltroStatus);

        JButton botaoLimparFiltros = new JButton("Limpar filtros");
        JButton botaoExportarCsv = new JButton("Exportar CSV");
        JButton botaoImprimirPdf = new JButton("Imprimir / Salvar PDF");
        estilizarBotao(botaoLimparFiltros, new Color(108, 117, 125), Color.WHITE);
        estilizarBotao(botaoExportarCsv, COR_VERDE, Color.WHITE);
        estilizarBotao(botaoImprimirPdf, COR_TOPO, Color.WHITE);

        JPanel linhaFiltros = criarLinha(6);
        linhaFiltros.add(criarRotulo("Buscar:"));
        linhaFiltros.add(campoBuscaRelatorio);
        linhaFiltros.add(criarRotulo("Categoria:"));
        linhaFiltros.add(comboFiltroCategoria);
        linhaFiltros.add(criarRotulo("Status:"));
        linhaFiltros.add(comboFiltroStatus);

        JPanel linhaAcoes = criarLinha(6);
        linhaAcoes.add(botaoLimparFiltros);
        linhaAcoes.add(botaoExportarCsv);
        linhaAcoes.add(botaoImprimirPdf);

        JPanel cardFiltros = criarCard();
        cardFiltros.setLayout(new BoxLayout(cardFiltros, BoxLayout.Y_AXIS));
        cardFiltros.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        cardFiltros.add(criarTituloSecao("Filtros do Relatório"));
        cardFiltros.add(Box.createVerticalStrut(6));
        cardFiltros.add(linhaFiltros);
        cardFiltros.add(linhaAcoes);

        sorterRelatorios = new TableRowSorter<>(modelo);
        tabelaRelatorio = new JTable(modelo);
        tabelaRelatorio.setRowSorter(sorterRelatorios);
        tabelaRelatorio.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollRelatorio = new JScrollPane(tabelaRelatorio);
        estilizarTabela(tabelaRelatorio, scrollRelatorio);

        lblQuantidadeRegistros = new JLabel("0 registro(s)");
        lblQuantidadeRegistros.setFont(FONTE_PADRAO);
        lblQuantidadeRegistros.setForeground(COR_MUTED);

        JPanel cabecalhoListagem = new JPanel(new BorderLayout());
        cabecalhoListagem.setBackground(COR_CARD);
        cabecalhoListagem.add(criarTituloSecao("Listagem Analítica"), BorderLayout.WEST);
        cabecalhoListagem.add(lblQuantidadeRegistros, BorderLayout.EAST);

        JPanel cardListagem = criarCard();
        cardListagem.setLayout(new BorderLayout(0, 10));
        cardListagem.add(cabecalhoListagem, BorderLayout.NORTH);
        cardListagem.add(scrollRelatorio, BorderLayout.CENTER);

        campoBuscaRelatorio.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltrosRelatorio();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltrosRelatorio();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltrosRelatorio();
            }
        });
        comboFiltroCategoria.addActionListener(e -> aplicarFiltrosRelatorio());
        comboFiltroStatus.addActionListener(e -> aplicarFiltrosRelatorio());
        botaoLimparFiltros.addActionListener(e -> limparFiltrosRelatorio());
        botaoExportarCsv.addActionListener(e -> exportarCsv());
        botaoImprimirPdf.addActionListener(e -> imprimirRelatorio());

        JPanel aba = criarPainelAba();
        aba.add(cardFiltros);
        aba.add(Box.createVerticalStrut(16));
        aba.add(cardListagem);
        return aba;
    }

    private void aplicarFiltrosRelatorio() {
        String termoBusca = campoBuscaRelatorio.getText().trim().toLowerCase();
        String catSelecionada = comboFiltroCategoria.getSelectedItem().toString();
        String statusSelecionado = comboFiltroStatus.getSelectedItem().toString();

        sorterRelatorios.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                String nome = entry.getStringValue(COL_NOME).toLowerCase();
                String categoria = entry.getStringValue(COL_CATEGORIA).trim();
                String status = entry.getStringValue(COL_STATUS).trim();
                String prazo = entry.getStringValue(COL_PRAZO).toLowerCase();

                boolean matchTexto = termoBusca.isEmpty() || nome.contains(termoBusca) || prazo.contains(termoBusca);
                boolean matchCategoria = OPCOES_CATEGORIA[0].equalsIgnoreCase(catSelecionada)
                        || categoria.equalsIgnoreCase(catSelecionada);
                boolean matchStatus = OPCOES_STATUS[0].equalsIgnoreCase(statusSelecionado)
                        || status.equalsIgnoreCase(statusSelecionado);

                return matchTexto && matchCategoria && matchStatus;
            }
        });

        lblQuantidadeRegistros.setText(tabelaRelatorio.getRowCount() + " registro(s)");
    }

    private void limparFiltrosRelatorio() {
        campoBuscaRelatorio.setText("");
        comboFiltroCategoria.setSelectedIndex(0);
        comboFiltroStatus.setSelectedIndex(0);
        aplicarFiltrosRelatorio();
    }

    private void exportarCsv() {
        JFileChooser seletor = new JFileChooser();
        seletor.setSelectedFile(new File("relatorio_projetos.csv"));

        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File arquivo = seletor.getSelectedFile();
        if (!arquivo.getName().toLowerCase().endsWith(".csv")) {
            arquivo = new File(arquivo.getAbsolutePath() + ".csv");
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {

            for (int col = 0; col < modelo.getColumnCount(); col++) {
                if (col > 0) {
                    pw.print(";");
                }
                pw.print(escaparCsv(modelo.getColumnName(col)));
            }
            pw.println();

            for (int viewRow = 0; viewRow < tabelaRelatorio.getRowCount(); viewRow++) {
                int modelRow = tabelaRelatorio.convertRowIndexToModel(viewRow);
                for (int col = 0; col < modelo.getColumnCount(); col++) {
                    if (col > 0) {
                        pw.print(";");
                    }
                    pw.print(escaparCsv(valorCelula(modelRow, col)));
                }
                pw.println();
            }

            JOptionPane.showMessageDialog(this,
                    "Relatório exportado com sucesso para:\n" + arquivo.getAbsolutePath(),
                    "Exportação concluída", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LogManager.error("DESKTOP_EXPORTAR_CSV", ex.getMessage());
            JOptionPane.showMessageDialog(this, "Erro ao exportar CSV: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void imprimirRelatorio() {
        try {
            boolean concluido = tabelaRelatorio.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("Relatório de Projetos"),
                    new MessageFormat("Página {0}"));

            if (concluido) {
                JOptionPane.showMessageDialog(this, "Impressão / exportação enviada com sucesso.",
                        "Relatório", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            LogManager.error("DESKTOP_IMPRESSAO", ex.getMessage());
            JOptionPane.showMessageDialog(this, "Erro ao imprimir / salvar PDF: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escaparCsv(String texto) {
        if (texto == null) {
            return "";
        }
        String escapado = texto.replace("\"", "\"\"");
        if (escapado.contains(";") || escapado.contains("\"") || escapado.contains("\n") || escapado.contains("\r")) {
            return "\"" + escapado + "\"";
        }
        return escapado;
    }

    // =====================================================================
    // OPERAÇÕES DO PROJETO (cadastrar / alterar / excluir / limpar)
    // =====================================================================

    public void cadastrar() {
        try {
            service.carregar();
            int id = service.gerarId();
            Projeto projeto = montarEValidarProjeto(id, LocalDate.now());
            projeto.setUsuarioLogin(usuarioLogado);

            apiClient.cadastrarProjeto(projeto);
            JOptionPane.showMessageDialog(this, "Projeto Cadastrado: " + projeto.getNome());

            limparFormulario();
            carregarTabela();
        } catch (Exception e) {
            LogManager.error("DESKTOP_CADASTRO", e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro de Validação", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void alterarProjeto() {
        int linhaSelecionada = tabela.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione um projeto", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int id = idDaLinha(linhaSelecionada);

            Projeto original = buscarNoCache(id);
            LocalDate dataCriacao = original != null ? original.getDataCriacao() : null;
            Projeto projetoAtualizado = montarEValidarProjeto(id, dataCriacao);
            projetoAtualizado.setUsuarioLogin(usuarioLogado);

            int option = JOptionPane.showConfirmDialog(this,
                    "Um projeto será alterado.\nEstá certo disso?",
                    "Aviso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                apiClient.alterarProjeto(id, projetoAtualizado);
                carregarTabela();
                limparFormulario();
                JOptionPane.showMessageDialog(this, "Projeto alterado com sucesso", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            LogManager.error("DESKTOP_ALTERACAO", e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void excluirProjeto() {
        int linhaSelecionada = tabela.getSelectedRow();
        if (linhaSelecionada == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecione um projeto", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int id = idDaLinha(linhaSelecionada);

            int option = JOptionPane.showConfirmDialog(this,
                    "Um projeto será deletado.\nEstá certo disso?",
                    "Aviso", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                apiClient.excluirProjeto(id);
                limparFormulario();
                carregarTabela();
                JOptionPane.showMessageDialog(this, "Projeto excluído com sucesso", "Sucesso",
                        JOptionPane.PLAIN_MESSAGE);
            }
        } catch (Exception e) {
            LogManager.error("DESKTOP_EXCLUSAO", e.getMessage());
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void limparFormulario() {
        campoNome.setText("");
        campoDescricao.setText("");
        campoPrazo.setText("");
        comboCategoria.setSelectedIndex(0);
        comboStatus.setSelectedIndex(0);
        tabela.clearSelection();
        campoNome.requestFocus();
    }

    private Projeto montarEValidarProjeto(int id, LocalDate dataCriacao) {
        String nome = campoNome.getText().trim();
        String descricao = campoDescricao.getText().trim();
        String categoria = comboCategoria.getSelectedItem().toString();
        String status = comboStatus.getSelectedItem().toString();
        LocalDate prazo = ProjetoService.parseDataFlexivel(campoPrazo.getText().trim());

        Projeto projeto = new Projeto(id, nome, descricao, categoria, status, dataCriacao, prazo);
        service.validarProjeto(projeto, false, null);
        return projeto;
    }

    // =====================================================================
    // DADOS E TABELA (carga via API, atualização e seleção)
    // =====================================================================

    public void carregarTabela() {
        new SwingWorker<List<Projeto>, Void>() {
            @Override
            protected List<Projeto> doInBackground() throws Exception {
                return apiClient.listarProjetos();
            }

            @Override
            protected void done() {
                try {
                    projetosCache = get();
                    erroCargaExibido = false;
                    atualizarTabela(projetosCache);
                } catch (Exception e) {
                    LogManager.error("DESKTOP_CARGA", e.getMessage());
                    if (!erroCargaExibido) {
                        erroCargaExibido = true;
                        JOptionPane.showMessageDialog(TelaPrincipal.this, "Erro ao carregar: " + e.getMessage(),
                                "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    public void atualizarTabela(List<Projeto> projetos) {
        Integer idSelecionado = null;
        int linhaView = tabela.getSelectedRow();
        if (linhaView != -1) {
            idSelecionado = idDaLinha(linhaView);
        }

        atualizandoTabela = true;
        try {
            modelo.setRowCount(0);
            for (Projeto projeto : projetos) {
                modelo.addRow(new Object[] {
                        projeto.getId(),
                        projeto.getNome(),
                        projeto.getCategoria(),
                        projeto.getStatus(),
                        formatarPrazo(projeto)
                });
            }

            if (idSelecionado != null) {
                for (int i = 0; i < modelo.getRowCount(); i++) {
                    if (idSelecionado.equals(modelo.getValueAt(i, COL_ID))) {
                        int viewRow = tabela.convertRowIndexToView(i);
                        if (viewRow != -1) {
                            tabela.setRowSelectionInterval(viewRow, viewRow);
                        }
                        break;
                    }
                }
            }
        } finally {
            atualizandoTabela = false;
        }

        atualizarAbasDerivadas();
    }

    private void atualizarAbasDerivadas() {
        atualizarCategorias();
        atualizarDashboard();
        aplicarFiltrosRelatorio();
    }

    private void preencherFormulario(int id) {
        Projeto p = buscarNoCache(id);
        if (p == null) {
            return;
        }

        campoNome.setText(p.getNome() != null ? p.getNome() : "");
        campoDescricao.setText(p.getDescricao() != null ? p.getDescricao() : "");
        campoPrazo.setText(formatarPrazo(p));
        if (p.getCategoria() != null) {
            comboCategoria.setSelectedItem(p.getCategoria());
        }
        if (p.getStatus() != null) {
            comboStatus.setSelectedItem(p.getStatus());
        }
    }

    private Projeto buscarNoCache(int id) {
        for (Projeto p : projetosCache) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    private int idDaLinha(int linhaView) {
        int linhaModel = tabela.convertRowIndexToModel(linhaView);
        return Integer.parseInt(modelo.getValueAt(linhaModel, COL_ID).toString());
    }

    private String valorCelula(int linhaModel, int coluna) {
        Object valor = modelo.getValueAt(linhaModel, coluna);
        return valor != null ? valor.toString().trim() : "";
    }

    private String formatarPrazo(Projeto projeto) {
        return projeto.getPrazo() != null ? projeto.getPrazo().format(FORMATO_DATA) : "";
    }

    private Map<String, Integer> contarPorCategoria() {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (String categoria : CATEGORIAS) {
            contagem.put(categoria, 0);
        }

        for (int i = 0; i < modelo.getRowCount(); i++) {
            String categoria = valorCelula(i, COL_CATEGORIA);
            for (String conhecida : CATEGORIAS) {
                if (conhecida.equalsIgnoreCase(categoria)) {
                    contagem.merge(conhecida, 1, Integer::sum);
                    break;
                }
            }
        }
        return contagem;
    }

    // =====================================================================
    // FÁBRICA DE COMPONENTES E ESTILO
    // =====================================================================

    private static String[] comOpcaoTodos(String rotulo, String[] opcoes) {
        String[] resultado = new String[opcoes.length + 1];
        resultado[0] = rotulo;
        System.arraycopy(opcoes, 0, resultado, 1, opcoes.length);
        return resultado;
    }

    private DefaultTableModel criarModeloTabela() {
        DefaultTableModel m = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (String coluna : COLUNAS) {
            m.addColumn(coluna);
        }
        return m;
    }

    private JPanel criarPainelAba() {
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBackground(COR_FUNDO);
        painel.setBorder(new EmptyBorder(18, 22, 18, 22));
        return painel;
    }

    private JPanel criarCard() {
        JPanel card = new JPanel();
        card.setBackground(COR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_BORDA, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return card;
    }

    private JPanel criarLinha(int espacamentoVertical) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, espacamentoVertical));
        linha.setBackground(COR_CARD);
        linha.setAlignmentX(Component.LEFT_ALIGNMENT);
        return linha;
    }

    private JLabel criarTituloSecao(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(FONTE_SECAO);
        label.setForeground(COR_TEXTO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel criarRotulo(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(FONTE_PADRAO);
        label.setForeground(COR_TEXTO);
        return label;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setFont(FONTE_PADRAO);
        campo.setForeground(COR_TEXTO);
        campo.setBackground(Color.WHITE);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_BORDA, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private void estilizarCombo(JComboBox<?> combo) {
        combo.setFont(FONTE_PADRAO);
        combo.setForeground(COR_TEXTO);
        combo.setBackground(Color.WHITE);
    }

    private void estilizarBotao(JButton botao, Color fundo, Color texto) {
        botao.setFont(FONTE_BOTAO);
        botao.setBackground(fundo);
        botao.setForeground(texto);
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fundo.darker(), 1, true),
                new EmptyBorder(8, 16, 8, 16)));
    }

    private void estilizarTabela(JTable tabela, JScrollPane scroll) {
        tabela.setFont(FONTE_PADRAO);
        tabela.setForeground(COR_TEXTO);
        tabela.setBackground(Color.WHITE);
        tabela.setRowHeight(28);
        tabela.setGridColor(COR_BORDA);
        tabela.setSelectionBackground(new Color(227, 242, 253));
        tabela.setSelectionForeground(COR_TEXTO);

        if (tabela.getTableHeader() != null) {
            tabela.getTableHeader().setFont(FONTE_BOTAO);
            tabela.getTableHeader().setBackground(new Color(248, 249, 250));
            tabela.getTableHeader().setForeground(COR_TEXTO);
        }

        if (scroll != null) {
            scroll.getViewport().setBackground(Color.WHITE);
            scroll.setBorder(BorderFactory.createLineBorder(COR_BORDA, 1, true));
        }
    }

    // =====================================================================
    // GRÁFICOS COM GRAPHICS2D
    // =====================================================================

    private static class PainelGraficoStatus extends JPanel {
        private int concluido;
        private int emDesenvolvimento;
        private int planejado;

        PainelGraficoStatus() {
            setOpaque(false);
        }

        void setDados(int concluido, int emDesenvolvimento, int planejado) {
            this.concluido = concluido;
            this.emDesenvolvimento = emDesenvolvimento;
            this.planejado = planejado;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = Math.max(1, Math.max(concluido, Math.max(emDesenvolvimento, planejado)));
            int larguraTotal = Math.max(120, getWidth() - 24);
            int y = 10;

            desenharLinha(g2, "Concluído (" + concluido + ")", concluido, max, 12, y, larguraTotal, COR_VERDE);
            desenharLinha(g2, "Em Desenvolvimento (" + emDesenvolvimento + ")", emDesenvolvimento, max, 12, y + 28, larguraTotal, COR_LARANJA);
            desenharLinha(g2, "Planejado (" + planejado + ")", planejado, max, 12, y + 56, larguraTotal, COR_AZUL);

            g2.dispose();
        }

        private void desenharLinha(Graphics2D g2, String rotulo, int valor, int max, int x, int y, int larguraMax, Color cor) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(COR_MUTED);
            g2.drawString(rotulo, x, y + 12);

            int barraX = x + 170;
            int disponivel = Math.max(40, larguraMax - 170);
            int larguraBarra = (int) ((valor / (double) max) * disponivel);

            g2.setColor(new Color(236, 239, 241));
            g2.fillRoundRect(barraX, y + 1, disponivel, 14, 6, 6);

            if (valor > 0) {
                g2.setColor(cor);
                g2.fillRoundRect(barraX, y + 1, Math.max(larguraBarra, 8), 14, 6, 6);
            }
        }
    }

    private static class PainelGraficoCategoria extends JPanel {
        private final String[] categorias = { "Web", "Software", "Mobile", "Desktop" };
        private int[] valores = new int[4];

        PainelGraficoCategoria() {
            setOpaque(false);
        }

        void setDados(int[] valores) {
            this.valores = valores != null ? valores : new int[4];
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = 1;
            for (int v : valores) {
                max = Math.max(max, v);
            }

            int largura = Math.max(120, getWidth());
            int baseY = Math.max(40, getHeight() - 26);
            int topoY = 16;
            int alturaDisponivel = Math.max(16, baseY - topoY);
            int larguraColuna = Math.max(36, (largura - 64) / categorias.length);

            g2.setColor(new Color(207, 216, 220));
            g2.drawLine(16, baseY, largura - 16, baseY);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(COR_MUTED);
            g2.drawString("0", 6, baseY + 4);

            for (int i = 0; i < categorias.length; i++) {
                int x = 28 + i * larguraColuna;
                int h = (int) ((valores[i] / (double) max) * alturaDisponivel);

                if (valores[i] > 0) {
                    g2.setColor(COR_AZUL);
                    g2.fillRoundRect(x, baseY - h, 32, Math.max(h, 6), 5, 5);
                }

                g2.setColor(COR_MUTED);
                g2.drawString(categorias[i], x - 2, baseY + 16);
            }

            g2.dispose();
        }
    }

    private static class PainelGraficoMensal extends JPanel {
        private final String[] meses = { "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez" };
        private int[] porMes = new int[12];

        PainelGraficoMensal() {
            setOpaque(false);
        }

        void setDados(int[] porMes) {
            this.porMes = porMes != null ? porMes : new int[12];
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = 1;
            for (int v : porMes) {
                max = Math.max(max, v);
            }

            int largura = Math.max(160, getWidth());
            int baseY = Math.max(40, getHeight() - 28);
            int topoY = 16;
            int alturaDisponivel = Math.max(16, baseY - topoY);
            int passoX = Math.max(24, (largura - 56) / 11);

            g2.setColor(new Color(207, 216, 220));
            g2.drawLine(24, baseY, largura - 16, baseY);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(COR_MUTED);
            g2.drawString("0", 8, baseY + 4);

            int prevX = -1;
            int prevY = -1;
            for (int i = 0; i < meses.length; i++) {
                int x = 32 + i * passoX;
                int y = baseY - (int) ((porMes[i] / (double) max) * alturaDisponivel);

                if (prevX != -1) {
                    g2.setColor(COR_AZUL);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(prevX, prevY, x, y);
                }

                g2.setColor(COR_AZUL);
                g2.fillOval(x - 3, y - 3, 6, 6);

                prevX = x;
                prevY = y;

                g2.setColor(COR_MUTED);
                g2.drawString(meses[i], x - 9, baseY + 16);
            }

            g2.setComposite(AlphaComposite.SrcOver);
            g2.dispose();
        }
    }

    private static class PainelGraficoPrazos extends JPanel {
        private int noPrazo;
        private int atrasados;

        PainelGraficoPrazos() {
            setOpaque(false);
        }

        void setDados(int noPrazo, int atrasados) {
            this.noPrazo = noPrazo;
            this.atrasados = atrasados;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int max = Math.max(1, Math.max(noPrazo, atrasados));
            int larguraTotal = Math.max(120, getWidth() - 24);

            desenharBarra(g2, "No prazo (" + noPrazo + ")", noPrazo, max, 12, 12, larguraTotal, COR_VERDE);
            desenharBarra(g2, "Atrasados (" + atrasados + ")", atrasados, max, 12, 42, larguraTotal, COR_VERMELHO);

            g2.dispose();
        }

        private void desenharBarra(Graphics2D g2, String rotulo, int valor, int max, int x, int y, int larguraMax, Color cor) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(COR_MUTED);
            g2.drawString(rotulo, x, y + 12);

            int barraX = x + 130;
            int disponivel = Math.max(40, larguraMax - 130);
            int larguraBarra = (int) ((valor / (double) max) * disponivel);

            g2.setColor(new Color(236, 239, 241));
            g2.fillRoundRect(barraX, y + 1, disponivel, 14, 6, 6);

            if (valor > 0) {
                g2.setColor(cor);
                g2.fillRoundRect(barraX, y + 1, Math.max(larguraBarra, 8), 14, 6, 6);
            }
        }
    }

    // =====================================================================
    // PONTO DE ENTRADA
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TelaLogin telaLogin = new TelaLogin(null);
            telaLogin.setVisible(true);

            if (!telaLogin.isAutenticado()) {
                System.exit(0);
                return;
            }

            String usuario = telaLogin.getUsuarioLogado();
            String nome = telaLogin.getNomeUsuarioLogado();
            String perfil = telaLogin.getPerfilUsuarioLogado();

            try {
                Api.iniciar(usuario);
            } catch (Exception ex) {
                System.out.println("Aviso ao iniciar API: " + ex.getMessage());
            }

            try {
                TelaPrincipal tela = new TelaPrincipal(usuario, nome, perfil);
                tela.setVisible(true);

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "atualizacao-tabela");
                    t.setDaemon(true);
                    return t;
                });
                scheduler.scheduleAtFixedRate(tela::carregarTabela, 5, 5, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}