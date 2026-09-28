package br.unip.aps.ui;

import br.unip.aps.Focos;
import br.unip.aps.app.Sessao;
import br.unip.aps.config.AppConfig;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Interacao real com a interface (teclado e mouse simulados pelo TestFX, numa tela virtual Monocle). */
@DisplayName("Interface: interacoes com teclado e mouse")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InteracaoTest {
    private static FxRobot ROBO;
    private static Stage stage;
    private static UiContexto ctx;
    private static MainController main;
    private static boolean daltonicoAntes;
    private static int escalaAntes;

    @BeforeAll
    static void abrir() throws Exception {
        FxToolkit.registerPrimaryStage();
        FxToolkit.setupFixture(() -> {
            GerenciadorTema tema = GerenciadorTema.get();
            tema.animacoesProperty().set(false);
            daltonicoAntes = tema.daltonicoProperty().get();
            escalaAntes = tema.escalaTextoProperty().get();
            try {
                stage = new Stage();
                ctx = new UiContexto(new Sessao(AppConfig.carregar()), stage);
                FXMLLoader loader = new FXMLLoader(InteracaoTest.class.getResource("/br/unip/aps/ui/main.fxml"));
                loader.setControllerFactory(ctx::criarController);
                Parent root = loader.load();
                main = loader.getController();
                Scene scene = new Scene(root, 1280, 800);
                tema.registrar(scene);
                stage.setScene(scene);
                ctx.baseProperty().set(new BaseDeFocos(Focos.aleatorios(600, 1), new RelatorioCarga(), List.of(Path.of("t.csv"))));
                stage.show();
                stage.toFront();
                stage.requestFocus();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        ROBO = new FxRobot();
    }

    @AfterAll
    static void fechar() throws Exception {
        FxToolkit.setupFixture(() -> {
            GerenciadorTema.get().daltonicoProperty().set(daltonicoAntes);
            GerenciadorTema.get().escalaTextoProperty().set(escalaAntes);
            ctx.encerrar();
            stage.close();
        });
    }

    private static void esperar() {
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Rola a pagina para deixar o controle no meio da area visivel, como a pessoa faria antes de clicar. */
    private static void rolarAte(String seletor) {
        ROBO.interact(() -> {
            Node alvo = ROBO.lookup(seletor).query();
            for (Node x = alvo; x != null; x = x.getParent()) {
                if (x instanceof javafx.scene.control.ScrollPane sp) {
                    Node conteudo = sp.getContent();
                    double y = conteudo.sceneToLocal(alvo.localToScene(alvo.getBoundsInLocal())).getMinY();
                    double livre = conteudo.getBoundsInLocal().getHeight() - sp.getViewportBounds().getHeight();
                    if (livre > 0) sp.setVvalue(Math.max(0, Math.min(1, (y - sp.getViewportBounds().getHeight() / 2) / livre)));
                }
            }
        });
        esperar();
    }

    @Test
    @Order(1)
    @DisplayName("Ctrl+K abre a paleta; digitar 'mapa' e Enter leva ao Mapa")
    void paletaDeComandos() {
        ROBO.press(KeyCode.CONTROL, KeyCode.K).release(KeyCode.K, KeyCode.CONTROL);
        esperar();
        assertTrue(main.paleta().aberta(), "a paleta deveria abrir com Ctrl+K");
        ROBO.write("mapa");
        ROBO.type(KeyCode.ENTER);
        esperar();
        assertFalse(main.paleta().aberta());
        assertEquals(Pagina.MAPA, ctx.paginaProperty().get());
    }

    @Test
    @Order(2)
    @DisplayName("Ctrl+1 volta à Visão geral e Ctrl+T alterna o tema")
    void atalhosDeTeclado() {
        ROBO.press(KeyCode.CONTROL, KeyCode.DIGIT1).release(KeyCode.DIGIT1, KeyCode.CONTROL);
        esperar();
        assertEquals(Pagina.VISAO_GERAL, ctx.paginaProperty().get());
        GerenciadorTema.Tema antes = GerenciadorTema.get().temaProperty().get();
        ROBO.press(KeyCode.CONTROL, KeyCode.T).release(KeyCode.T, KeyCode.CONTROL);
        esperar();
        assertNotEquals(antes, GerenciadorTema.get().temaProperty().get());
        ROBO.press(KeyCode.CONTROL, KeyCode.T).release(KeyCode.T, KeyCode.CONTROL);
        esperar();
        assertEquals(antes, GerenciadorTema.get().temaProperty().get());
    }

    @Test
    @Order(3)
    @DisplayName("F5 entra no modo apresentação e Esc sai")
    void modoApresentacao() {
        ROBO.type(KeyCode.F5);
        esperar();
        assertTrue(main.apresentando());
        ROBO.type(KeyCode.ESCAPE);
        esperar();
        assertFalse(main.apresentando());
    }

    @Test
    @Order(4)
    @DisplayName("digitar um município na busca e Enter filtra todas as telas")
    void filtroPorMunicipio() {
        Node busca = ROBO.lookup(".busca-municipio").query();
        ROBO.clickOn(busca).write("BAURU").type(KeyCode.ENTER);
        esperar();
        FiltroGlobal f = ctx.filtroProperty().get();
        assertNotNull(f.municipio(), "o filtro por município deveria estar ativo");
        int n = ctx.focosFiltradosProperty().get().size();
        assertTrue(n > 0 && n < 600, "focos filtrados: " + n);
        ROBO.interact(() -> ctx.filtroProperty().set(FiltroGlobal.VAZIO));
        esperar();
    }

    @Test
    @Order(5)
    @DisplayName("nas Configurações, os controles de acessibilidade mudam o tema na hora")
    void acessibilidade() {
        ROBO.interact(() -> ctx.navegar(Pagina.CONFIGURACOES));
        esperar();
        boolean antes = GerenciadorTema.get().daltonicoProperty().get();
        ROBO.clickOn("#swDaltonico");
        esperar();
        assertNotEquals(antes, GerenciadorTema.get().daltonicoProperty().get());
        assertTrue(stage.getScene().getStylesheets().stream().anyMatch(u -> u.contains("daltonico")) != antes);
        rolarAte("#tgTexto130");
        ROBO.clickOn("#tgTexto130");
        esperar();
        assertEquals(130, GerenciadorTema.get().escalaTextoProperty().get());
        assertTrue(stage.getScene().getStylesheets().stream().anyMatch(u -> u.contains("-130-")), "folhas ampliadas aplicadas");
        rolarAte("#tgTexto100");
        ROBO.clickOn("#tgTexto100");
        esperar();
        assertEquals(100, GerenciadorTema.get().escalaTextoProperty().get());
    }

    @Test
    @Order(6)
    @DisplayName("no Mapa, o seletor de mapa-base abre, troca o estilo, fecha com Esc e bloqueia estilos online sem internet")
    void seletorDeMapaBase() {
        ROBO.interact(() -> ctx.navegar(Pagina.MAPA));
        esperar();
        MapaController mapa = (MapaController) main.controllerDe(Pagina.MAPA);
        br.unip.aps.ui.componentes.SeletorMapaBase seletor = mapa.seletorBase();
        String original = seletor.selecionadaProperty().get();
        ROBO.interact(() -> seletor.setOffline(false));

        ROBO.clickOn("#btnMapaBase");
        esperar();
        assertTrue(seletor.aberto(), "o painel deveria abrir");
        ROBO.clickOn("#base-relevo");
        esperar();
        assertEquals("relevo", seletor.selecionadaProperty().get());
        assertFalse(seletor.aberto(), "o painel fecha depois da escolha");

        ROBO.clickOn("#btnMapaBase");
        esperar();
        ROBO.type(KeyCode.ESCAPE);
        esperar();
        assertFalse(seletor.aberto(), "Esc fecha o painel");

        ROBO.interact(() -> seletor.setOffline(true));
        assertTrue(ROBO.lookup("#base-satelite").query().isDisabled(), "sem internet, o satélite fica indisponível");
        assertFalse(ROBO.lookup("#base-neutro").query().isDisabled(), "o Neutro continua disponível");
        ROBO.interact(() -> {
            seletor.setOffline(false);
            seletor.escolher(original);
        });
        esperar();
        assertEquals(original, seletor.selecionadaProperty().get());
    }

    @Test
    @Order(7)
    @DisplayName("a linha do tempo toca e pausa, e escolher um mês muda a contagem do mapa")
    void linhaDoTempo() {
        ROBO.interact(() -> ctx.navegar(Pagina.MAPA));
        esperar();
        MapaController mapa = (MapaController) main.controllerDe(Pagina.MAPA);
        br.unip.aps.ui.componentes.LinhaDoTempo linha = mapa.linhaDoTempo();

        ROBO.clickOn("#btnTocarMeses");
        esperar();
        assertTrue(linha.tocando(), "o botão deveria começar a tocar os meses");
        assertEquals(YearMonth.of(2023, 1), linha.inicio());
        ROBO.clickOn("#btnTocarMeses");
        esperar();
        assertFalse(linha.tocando(), "o segundo clique pausa");

        YearMonth agosto = YearMonth.of(2024, 8);
        ROBO.interact(() -> linha.setPeriodo(agosto, agosto));
        esperar();
        long esperado = ctx.focosFiltradosProperty().get().stream().filter(f -> f.getAno() == 2024 && f.getMes() == 8).count();
        assertEquals(br.unip.aps.util.Formatos.inteiro(esperado), ROBO.lookup("#lblContagem").queryAs(Label.class).getText());
        assertEquals("focos em ago/2024", ROBO.lookup("#lblContagemTexto").queryAs(Label.class).getText());

        ROBO.clickOn("#btnTodosMeses");
        esperar();
        assertNull(linha.inicio());
        assertEquals("600", ROBO.lookup("#lblContagem").queryAs(Label.class).getText());
    }

    @Test
    @Order(8)
    @DisplayName("a ficha do município abre pela paleta (Ctrl+K) e fecha no X")
    void fichaDoMunicipio() {
        ROBO.interact(() -> ctx.navegar(Pagina.VISAO_GERAL));
        esperar();
        ROBO.press(KeyCode.CONTROL, KeyCode.K).release(KeyCode.K, KeyCode.CONTROL);
        esperar();
        ROBO.write("ficha de bauru");
        ROBO.type(KeyCode.ENTER);
        esperar();
        assertEquals(Pagina.MAPA, ctx.paginaProperty().get());
        MapaController mapa = (MapaController) main.controllerDe(Pagina.MAPA);
        assertTrue(mapa.ficha().aberta(), "a ficha deveria abrir");
        long bauru = ctx.baseProperty().get().getFocos().stream().filter(f -> f.getMunicipio().equals("BAURU")).count();
        assertEquals(bauru, mapa.ficha().perfil().total());
        assertEquals(24, mapa.ficha().perfil().porMes().size());

        ROBO.clickOn("#btnFecharFicha");
        esperar();
        assertFalse(mapa.ficha().aberta());

        ROBO.interact(() -> mapa.abrirFicha("Cidade Inexistente"));
        esperar();
        assertFalse(mapa.ficha().aberta(), "nome desconhecido não abre a ficha");
    }

    @Test
    @Order(9)
    @DisplayName("o mapa é exportado em PNG com o dobro da resolução e sem os controles")
    void exportarPng(@TempDir Path pasta) throws Exception {
        ROBO.interact(() -> ctx.navegar(Pagina.MAPA));
        esperar();
        MapaController mapa = (MapaController) main.controllerDe(Pagina.MAPA);
        Path destino = pasta.resolve("sub").resolve("mapa.png");
        Exception[] erro = new Exception[1];
        ROBO.interact(() -> {
            try {
                mapa.exportarPng(destino);
            } catch (java.io.IOException e) {
                erro[0] = e;
            }
        });
        esperar();
        assertNull(erro[0]);
        assertTrue(java.nio.file.Files.size(destino) > 0);
        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(destino.toFile());
        double largura = ROBO.lookup("#moldura").query().getLayoutBounds().getWidth();
        assertEquals(Math.round(largura * MapaController.ESCALA_PNG), img.getWidth(), 2);
        assertTrue(ROBO.lookup("#linhaDoTempo").query().isVisible(), "os controles voltam depois da exportação");
    }
}
