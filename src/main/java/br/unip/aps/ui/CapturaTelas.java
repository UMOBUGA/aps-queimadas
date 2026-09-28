package br.unip.aps.ui;

import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

/** Captura as telas do dashboard em PNG nos temas claro e escuro. */
final class CapturaTelas {
    private static final Logger LOG = Logger.getLogger(CapturaTelas.class.getName());

    private final UiContexto ctx;
    private final MainController main;
    private final Stage stage;
    private final Path destino;
    private final Deque<Runnable> passos = new ArrayDeque<>();

    CapturaTelas(UiContexto ctx, MainController main, Stage stage, Path destino) {
        this.ctx = ctx;
        this.main = main;
        this.stage = stage;
        this.destino = destino;
    }

    void executar() {
        try {
            Files.createDirectories(destino);
        } catch (IOException e) {
            LOG.severe("Nao foi possivel criar " + destino + ": " + e.getMessage());
            return;
        }
        boolean animacoes = GerenciadorTema.get().animacoesProperty().get();
        GerenciadorTema.Tema temaOriginal = GerenciadorTema.get().temaProperty().get();
        GerenciadorTema.get().animacoesProperty().set(false);
        stage.setWidth(1440);
        stage.setHeight(900);
        int n = 1;
        for (GerenciadorTema.Tema tema : GerenciadorTema.Tema.values()) {
            for (Pagina p : Pagina.values()) {
                final int num = n++;
                passos.add(() -> {
                    GerenciadorTema.get().temaProperty().set(tema);
                    ctx.navegar(p);
                    Object c = main.controllerDe(p);
                    Runnable capturar = () -> esperar(p == Pagina.MAPA ? 8000 : p == Pagina.ESTRUTURAS ? 14000 : 900, () -> {
                        String base = String.format("%02d-%s-%s", num, p.name().toLowerCase().replace('_', '-'),
                                tema == GerenciadorTema.Tema.CLARO ? "claro" : "escuro");
                        salvar(base + ".png");
                        if (main.noDe(p) instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
                            salvarNo(sp.getContent(), "completa/" + base + ".png");
                        }
                        proximo();
                    });
                    if (tema == GerenciadorTema.Tema.CLARO && c instanceof Pagina.Controlador pc) {
                        pc.demonstrar(() -> aguardarOcioso(capturar));
                    } else {
                        capturar.run();
                    }
                });
            }
        }
        adicionarExtras(n);
        passos.add(() -> {
            GerenciadorTema.get().temaProperty().set(temaOriginal);
            GerenciadorTema.get().animacoesProperty().set(animacoes);
            LOG.info("Capturas concluidas em " + destino.toAbsolutePath());
            if (Boolean.getBoolean("aps.capturas.sair")) Platform.exit();
        });
        esperar(600, this::proximo);
    }

    private void adicionarExtras(int inicio) {
        int[] n = {inicio};
        passos.add(() -> {
            GerenciadorTema.get().temaProperty().set(GerenciadorTema.Tema.CLARO);
            ctx.navegar(Pagina.VISAO_GERAL);
            main.filterBar().selecionar(java.util.Set.of("Cerrado"), java.util.Set.of(2024));
            esperar(1200, () -> {
                salvar(String.format("%02d-visao-geral-filtro-cerrado-2024.png", n[0]++));
                main.filterBar().limpar();
                proximo();
            });
        });
        passos.add(() -> {
            ctx.navegar(Pagina.ORDENACAO);
            if (main.controllerDe(Pagina.ORDENACAO) instanceof OrdenacaoController oc) {
                oc.demonstrarComparativo(() -> aguardarOcioso(() -> esperar(900, () -> {
                    salvar(String.format("%02d-ordenacao-comparativo.png", n[0]++));
                    oc.demonstrarVisualizacao();
                    esperar(2500, () -> {
                        salvar(String.format("%02d-ordenacao-visualizacao.png", n[0]++));
                        quadrosGif(oc.visualizador(), 0, this::proximo);
                    });
                })));
            } else {
                proximo();
            }
        });
        passos.add(() -> {
            ctx.navegar(Pagina.MAPA);
            if (main.controllerDe(Pagina.MAPA) instanceof MapaController mc) {
                mc.modo("calor");
                esperar(4000, () -> {
                    salvar(String.format("%02d-mapa-calor.png", n[0]++));
                    mc.modo("cluster");
                    esperar(4000, () -> {
                        salvar(String.format("%02d-mapa-agrupado.png", n[0]++));
                        mc.modo("municipios");
                        esperar(4000, () -> {
                            salvar(String.format("%02d-mapa-municipios.png", n[0]++));
                            mc.modo("pontos");
                            String baseOriginal = mc.seletorBase().selecionadaProperty().get();
                            mc.seletorBase().abrir();
                            esperar(3000, () -> {
                                salvar(String.format("%02d-mapa-base-seletor.png", n[0]++));
                                mc.seletorBase().escolher("hibrido");
                                esperar(5000, () -> {
                                    salvar(String.format("%02d-mapa-satelite.png", n[0]++));
                                    mc.seletorBase().escolher("relevo");
                                    esperar(5000, () -> {
                                        salvar(String.format("%02d-mapa-relevo.png", n[0]++));
                                        mc.seletorBase().escolher(baseOriginal);
                                        mc.linhaDoTempo().setPeriodo(java.time.YearMonth.of(2024, 8), java.time.YearMonth.of(2024, 8));
                                        esperar(4000, () -> {
                                            salvar(String.format("%02d-mapa-agosto-2024.png", n[0]++));
                                            mc.linhaDoTempo().setPeriodo(null, null);
                                            mc.abrirFicha(municipioComMaisFocos());
                                            esperar(3000, () -> {
                                                salvar(String.format("%02d-mapa-ficha-municipio.png", n[0]++));
                                                br.unip.aps.geo.PerfilMunicipio p = mc.ficha().perfil();
                                                if (p != null && !p.vizinhosComFocos().isEmpty()) mc.comparar(p.nomeInpe(), p.vizinhosComFocos().get(0).chave());
                                                esperar(3000, () -> {
                                                    salvar(String.format("%02d-mapa-comparacao.png", n[0]++));
                                                    mc.comparacao().fechar();
                                                    mc.ficha().fechar();
                                                    mc.forcarOffline();
                                                    esperar(4000, () -> {
                                                        salvar(String.format("%02d-mapa-offline-mundo.png", n[0]++));
                                                        proximo();
                                                    });
                                                });
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            } else {
                proximo();
            }
        });
    }

    /** Grava quadros da animacao da ordenacao (gif/quadro-NN.png) para montar o GIF de demonstracao. */
    private void quadrosGif(javafx.scene.Node no, int i, Runnable depois) {
        if (i >= 70) {
            depois.run();
            return;
        }
        salvarNo(no, String.format("gif/quadro-%02d.png", i));
        esperar(110, () -> quadrosGif(no, i + 1, depois));
    }

    private String municipioComMaisFocos() {
        java.util.Map<String, Integer> n = new java.util.HashMap<>();
        String melhor = "";
        for (br.unip.aps.model.FocoIncendio f : ctx.baseProperty().get().getFocos()) {
            int c = n.merge(f.getMunicipio(), 1, Integer::sum);
            if (c > n.getOrDefault(melhor, 0)) melhor = f.getMunicipio();
        }
        return melhor;
    }

    private void proximo() {
        Runnable r = passos.poll();
        if (r != null) r.run();
    }

    private void aguardarOcioso(Runnable depois) {
        if (!ctx.ocupadoProperty().get()) {
            depois.run();
            return;
        }
        esperar(300, () -> aguardarOcioso(depois));
    }

    private static void esperar(long ms, Runnable r) {
        PauseTransition p = new PauseTransition(Duration.millis(ms));
        p.setOnFinished(e -> r.run());
        p.play();
    }

    private void salvarNo(javafx.scene.Node no, String nome) {
        try {
            Files.createDirectories(destino.resolve(nome).getParent());
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(javafx.scene.paint.Color.web(GerenciadorTema.get().escuro() ? "#0D0A09" : "#F3F2F0"));
            WritableImage img = no.snapshot(sp, null);
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", destino.resolve(nome).toFile());
        } catch (IOException | RuntimeException e) {
            LOG.warning("Falha ao capturar " + nome + ": " + e.getMessage());
        }
    }

    private void salvar(String nome) {
        try {
            WritableImage img = stage.getScene().getRoot().snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", destino.resolve(nome).toFile());
        } catch (IOException | RuntimeException e) {
            LOG.warning("Falha ao capturar " + nome + ": " + e.getMessage());
        }
    }
}
