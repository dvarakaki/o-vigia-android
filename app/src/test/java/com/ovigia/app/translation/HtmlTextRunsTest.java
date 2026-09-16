package com.ovigia.app.translation;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Recorte e remontagem da biografia HTML da Comic Vine. */
public class HtmlTextRunsTest {

    /** Tradutor de mentira: deixa o texto reconhecível sem mexer nas palavras. */
    private static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    @Test
    public void cadaParagrafoTituloELegendaViraUmTrecho() {
        List<String> sources = HtmlTextRuns.sources(
                "<h2>Origin</h2><p>He was born in <a href=\"/canada/\">Canada</a>.</p>"
                        + "<ul><li>First</li><li>Second</li></ul>"
                        + "<figure><a href=\"/img.jpg\"><img src=\"/img.jpg\"></a><figcaption>He arrives.</figcaption></figure>");

        assertEquals(List.of("Origin", "He was born in Canada.", "First", "Second", "He arrives."), sources);
    }

    @Test
    public void naoTraduzOQueNaoTemPalavra() {
        assertEquals(List.of(), HtmlTextRuns.sources("<p>1963</p><p> · </p><table><tr><td>#42</td></tr></table>"));
    }

    @Test
    public void ignoraScriptStyleENoscript() {
        String html = "<script>var a = 'Hidden';</script><style>p{color:red}</style>"
                + "<noscript><img src=\"/real.jpg\" alt=\"Real image\"></noscript><p>Visible text.</p>";

        assertEquals(List.of("Visible text."), HtmlTextRuns.sources(html));
        // A imagem do noscript é a que o WebView mostra (ele roda sem JavaScript): continua inteira.
        assertTrue(HtmlTextRuns.apply(html, HtmlTextRunsTest::upper).contains("<img src=\"/real.jpg\""));
    }

    @Test
    public void trocaOTextoMantendoAEstrutura() {
        String translated = HtmlTextRuns.apply(
                "<h2>Origin</h2><figure><img src=\"/img.jpg\"><figcaption>Wolverine.</figcaption></figure>"
                        + "<p>He was born.</p>",
                HtmlTextRunsTest::upper);

        assertEquals("<h2>ORIGIN</h2><figure><img src=\"/img.jpg\"><figcaption>WOLVERINE.</figcaption></figure>"
                + "<p>HE WAS BORN.</p>", translated);
    }

    @Test
    public void linkVoltaParaATraducaoQuandoONomeSobrevive() {
        // O tradutor devolve a frase em outra ordem, com os nomes próprios intactos.
        String translated = HtmlTextRuns.apply(
                "<p>He met <a href=\"/rose/\" data-ref-id=\"29-1\">Rose</a> and <b>Dog</b> in Alberta.</p>",
                source -> "Em Alberta ele conheceu Rose e Dog.");

        assertEquals("<p>Em Alberta ele conheceu <a href=\"/rose/\" data-ref-id=\"29-1\">Rose</a> e <b>Dog</b>.</p>",
                translated);
    }

    @Test
    public void semOTextoOriginalOLinkViraTextoSimples() {
        String translated = HtmlTextRuns.apply(
                "<p>He is <a href=\"/spider-man/\">Spider-Man</a> now.</p>",
                source -> "Ele agora é o Homem-Aranha.");

        assertEquals("<p>Ele agora é o Homem-Aranha.</p>", translated);
    }

    @Test
    public void trechoInteiroDentroDeUmaEnfaseContinuaEnfatizado() {
        assertEquals("<p><em>COM GRANDES PODERES.</em></p>",
                HtmlTextRuns.apply("<p><em>Com grandes poderes.</em></p>", HtmlTextRunsTest::upper));
    }

    @Test
    public void textoSoltoAoLadoDeUmaListaNaoGrudaNoProximo() {
        String translated = HtmlTextRuns.apply("<li>Teams: <ul><li>Avengers</li></ul></li>",
                HtmlTextRunsTest::upper);

        assertTrue(translated, translated.contains("TEAMS: <ul>"));
        assertTrue(translated, translated.contains("<li>AVENGERS</li>"));
    }

    @Test
    public void trechoSemTraducaoFicaNoOriginal() {
        String html = "<p>Already known.</p><p>Not yet.</p>";

        String translated = HtmlTextRuns.apply(html,
                source -> source.equals("Already known.") ? "Já conhecido." : null);

        assertEquals("<p>Já conhecido.</p><p>Not yet.</p>", translated);
    }

    @Test
    public void traducaoComHtmlDentroNaoVazaMarcacao() {
        String translated = HtmlTextRuns.apply("<p>Hello.</p>", source -> "<img src=x onerror=alert(1)>");

        assertFalse(translated, translated.contains("<img"));
        assertTrue(translated, translated.contains("&lt;img"));
    }

    @Test
    public void biografiaVaziaOuNulaNaoQuebra() {
        assertEquals(List.of(), HtmlTextRuns.sources(null));
        assertEquals("", HtmlTextRuns.apply(null, HtmlTextRunsTest::upper));
    }
}
