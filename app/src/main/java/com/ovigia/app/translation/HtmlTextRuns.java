package com.ovigia.app.translation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Os textos de uma biografia HTML da Comic Vine, para traduzir sem quebrar a
 * marcação.
 *
 * Um trecho é uma sequência de nós irmãos que ficam na mesma linha de leitura
 * (texto solto, links, negrito, itálico) — na prática, o parágrafo, título,
 * item de lista ou legenda inteiros, para o tradutor ver a frase completa.
 * Imagens, tabelas e o resto da estrutura não são tocados.
 *
 * Links e ênfases voltam para a tradução quando o texto deles aparece igual
 * nela — o caso comum, porque quase sempre são nomes próprios. Quando o
 * tradutor muda esse texto, o trecho continua traduzido e o link vira texto
 * simples: melhor perder o link do que a frase.
 */
public final class HtmlTextRuns {

    /** Marcação que acompanha o texto dentro da mesma frase. */
    private static final Set<String> INLINE = new HashSet<>(Arrays.asList(
            "a", "abbr", "b", "big", "cite", "del", "em", "font", "i", "ins", "mark", "q",
            "s", "small", "span", "strong", "sub", "sup", "u"));

    /** Conteúdo que não é texto de leitura (ou que o WebView nem executa). */
    private static final Set<String> SKIP = new HashSet<>(Arrays.asList(
            "script", "style", "noscript", "iframe", "svg", "math", "code", "pre", "textarea"));

    private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0]+");
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    /** Textos a traduzir, na ordem do documento e sem repetição. */
    public static List<String> sources(String html) {
        Set<String> sources = new LinkedHashSet<>();
        for (Run run : runs(parse(html))) sources.add(run.source);
        return new ArrayList<>(sources);
    }

    /**
     * O HTML com cada trecho trocado pelo que {@code translations} devolver para
     * ele; trecho sem tradução ({@code null} ou vazio) fica no original.
     */
    public static String apply(String html, UnaryOperator<String> translations) {
        Document document = parse(html);
        for (Run run : runs(document)) {
            String translated = translations.apply(run.source);
            if (translated != null && !translated.trim().isEmpty()) run.replaceWith(translated.trim());
        }
        return document.body().html();
    }

    private static Document parse(String html) {
        Document document = Jsoup.parseBodyFragment(html == null ? "" : html);
        // O HTML sai como entrou: a Comic Vine já cuida do espaçamento dela.
        document.outputSettings().prettyPrint(false);
        return document;
    }

    private static List<Run> runs(Document document) {
        List<Run> runs = new ArrayList<>();
        collect(document.body(), runs);
        return runs;
    }

    private static void collect(Element parent, List<Run> runs) {
        List<Node> pending = new ArrayList<>();
        for (Node child : parent.childNodes()) {
            if (isInline(child)) {
                pending.add(child);
                continue;
            }
            flush(pending, runs);
            if (child instanceof Element && !SKIP.contains(((Element) child).normalName())) {
                collect((Element) child, runs);
            }
        }
        flush(pending, runs);
    }

    /** Nó que faz parte da frase: texto, comentário ou marcação de linha só com texto dentro. */
    private static boolean isInline(Node node) {
        if (node instanceof TextNode || node instanceof Comment) return true;
        if (!(node instanceof Element)) return false;
        Element element = (Element) node;
        // Uma imagem dentro de um link, por exemplo, faz o link virar fronteira: ele
        // não pode ser recriado a partir do texto traduzido.
        if (!INLINE.contains(element.normalName())) return false;
        for (Node child : element.childNodes()) {
            if (!isInline(child)) return false;
        }
        return true;
    }

    private static void flush(List<Node> pending, List<Run> runs) {
        if (pending.isEmpty()) return;
        Run run = Run.of(pending);
        if (run != null) runs.add(run);
        pending.clear();
    }

    private static String normalize(String text) {
        return SPACES.matcher(text).replaceAll(" ").trim();
    }

    /**
     * Primeira ocorrência de {@code needle} a partir de {@code from} que não
     * esteja no meio de outra palavra.
     */
    private static int indexOfWord(String text, String needle, int from) {
        for (int at = text.indexOf(needle, from); at >= 0; at = text.indexOf(needle, at + 1)) {
            int end = at + needle.length();
            boolean startsWord = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
            boolean endsWord = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (startsWord && endsWord) return at;
        }
        return -1;
    }

    /** Um trecho: os nós que formam a frase e o texto que o tradutor recebe. */
    private static final class Run {

        private final List<Node> nodes;
        final String source;
        /** O trecho era separado dos vizinhos por espaço (ex.: "Texto &lt;ul&gt;…"). */
        private final boolean spaceBefore;
        private final boolean spaceAfter;

        private Run(List<Node> nodes, String source, boolean spaceBefore, boolean spaceAfter) {
            this.nodes = nodes;
            this.source = source;
            this.spaceBefore = spaceBefore;
            this.spaceAfter = spaceAfter;
        }

        /** {@code null} quando não há nada para traduzir (só espaços, números ou pontuação). */
        static Run of(List<Node> nodes) {
            StringBuilder raw = new StringBuilder();
            for (Node node : nodes) raw.append(textOf(node));
            String text = SPACES.matcher(raw).replaceAll(" ");
            if (!LETTER.matcher(text).find()) return null;
            return new Run(new ArrayList<>(nodes), text.trim(), text.startsWith(" "), text.endsWith(" "));
        }

        private static String textOf(Node node) {
            if (node instanceof TextNode) return ((TextNode) node).getWholeText();
            if (node instanceof Element) return ((Element) node).wholeText();
            return "";
        }

        void replaceWith(String translated) {
            Element wrapper = wrapper();
            List<Element> markup = wrapper != null ? wrapper.children() : elements();
            List<Node> content = rebuild(translated, markup);
            if (wrapper != null) {
                // O trecho inteiro estava dentro de um <em>, <b>…: a tradução fica lá dentro.
                Element clone = wrapper.shallowClone();
                for (Node node : content) clone.appendChild(node);
                content = new ArrayList<>(Collections.singletonList(clone));
            }
            if (spaceBefore) content.add(0, new TextNode(" "));
            if (spaceAfter) content.add(new TextNode(" "));

            Node anchor = nodes.get(0);
            for (Node node : content) anchor.before(node);
            for (Node node : nodes) node.remove();
        }

        /**
         * Texto traduzido com a marcação de volta: cada elemento é procurado pelo
         * próprio texto, sempre depois do anterior (a tradução mantém a ordem dos
         * nomes). O que não for encontrado fica como texto simples.
         */
        private List<Node> rebuild(String translated, List<Element> markup) {
            List<Node> content = new ArrayList<>();
            int cursor = 0;
            for (Element element : markup) {
                String text = normalize(element.wholeText());
                if (text.isEmpty()) continue;
                int at = indexOfWord(translated, text, cursor);
                if (at < 0) continue;
                if (at > cursor) content.add(new TextNode(translated.substring(cursor, at)));
                content.add(element.clone());
                cursor = at + text.length();
            }
            if (cursor < translated.length()) content.add(new TextNode(translated.substring(cursor)));
            return content;
        }

        /** O único elemento que cobre o trecho inteiro, se houver. */
        private Element wrapper() {
            List<Element> elements = elements();
            if (elements.size() != 1) return null;
            Element only = elements.get(0);
            return normalize(only.wholeText()).equals(source) ? only : null;
        }

        private List<Element> elements() {
            List<Element> elements = new ArrayList<>();
            for (Node node : nodes) {
                if (node instanceof Element) elements.add((Element) node);
            }
            return elements;
        }
    }

    private HtmlTextRuns() { }
}
