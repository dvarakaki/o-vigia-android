package com.ovigia.app.engine;

import com.ovigia.app.api.Rosters;
import com.ovigia.app.api.Traits;
import com.ovigia.app.model.Character;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converte um {@link Character} da Comic Vine em um {@link CharacterProfile}
 * com atributos [0,1] e registra o texto (em português) da pergunta
 * correspondente a cada atributo, para o {@link GameEngine} usar.
 *
 * Gênero e origem vêm direto da Comic Vine (campos confiáveis no endpoint de
 * lista). Times e poderes vêm de {@link Traits}, curados manualmente — ver
 * o motivo lá.
 */
public final class CharacterMapper {

    // Crenças "quase certas" em vez de 1/0 puros — dá folga ao motor bayesiano
    // para lidar com respostas imprecisas do jogador ("Talvez").
    private static final double YES = 0.92;
    private static final double NO = 0.08;

    // Só os valores de "origin" que a Comic Vine de fato retorna neste elenco.
    // "Other" foi deixado de fora de propósito: é um bucket genérico demais
    // para virar uma pergunta de sim/não útil.
    // Perguntas curtas e diretas, uma ideia por pergunta — sem exemplos, sem "ou".
    private static final Map<String, String> ORIGIN_QUESTIONS = new HashMap<>();
    static {
        ORIGIN_QUESTIONS.put("Human", "Seu personagem nasceu humano e sem poderes?");
        ORIGIN_QUESTIONS.put("Mutant", "Seu personagem é um mutante?");
        ORIGIN_QUESTIONS.put("Alien", "Seu personagem veio de outro planeta?");
        ORIGIN_QUESTIONS.put("God/Eternal", "Seu personagem é um deus ou um Eterno?");
        ORIGIN_QUESTIONS.put("Robot", "Seu personagem é um robô ou androide?");
        ORIGIN_QUESTIONS.put("Radiation", "Seu personagem ganhou seus poderes por causa de radiação?");
        ORIGIN_QUESTIONS.put("Cyborg", "Seu personagem é ciborgue?");
        ORIGIN_QUESTIONS.put("Animal", "Seu personagem é um animal?");
    }

    private static final Map<String, String> POWER_QUESTIONS = new HashMap<>();
    static {
        POWER_QUESTIONS.put("forca", "Seu personagem tem força sobre-humana?");
        POWER_QUESTIONS.put("voo", "Seu personagem consegue voar?");
        POWER_QUESTIONS.put("cura", "Seu personagem se cura rápido de ferimentos?");
        POWER_QUESTIONS.put("genio", "Seu personagem é um gênio?");
        POWER_QUESTIONS.put("invisibilidade", "Seu personagem consegue ficar invisível?");
        POWER_QUESTIONS.put("telepatia", "Seu personagem consegue ler mentes?");
        POWER_QUESTIONS.put("telecinese", "Seu personagem consegue mover objetos com a mente?");
        POWER_QUESTIONS.put("magia", "Seu personagem usa magia?");
        POWER_QUESTIONS.put("armas", "Seu personagem luta com armas ou artes marciais, sem poderes?");
        POWER_QUESTIONS.put("teia", "Seu personagem lança teias?");
        POWER_QUESTIONS.put("elasticidade", "Seu personagem consegue esticar o corpo?");
        POWER_QUESTIONS.put("imortal", "Seu personagem é imortal?");
        POWER_QUESTIONS.put("tamanho", "Seu personagem consegue mudar de tamanho?");
        POWER_QUESTIONS.put("teletransporte", "Seu personagem consegue se teletransportar?");
        POWER_QUESTIONS.put("energia", "Seu personagem dispara rajadas de energia pelo corpo?");
        POWER_QUESTIONS.put("sentidos", "Seu personagem tem sentidos aguçados?");
        POWER_QUESTIONS.put("tecnologia", "Os poderes do seu personagem vêm de uma armadura ou equipamento?");
        POWER_QUESTIONS.put("invulneravel", "A pele do seu personagem para balas?");
        POWER_QUESTIONS.put("velocidade", "Seu personagem tem supervelocidade?");
        POWER_QUESTIONS.put("absorver", "Seu personagem consegue absorver poderes de outras pessoas?");
        POWER_QUESTIONS.put("realidade", "Seu personagem consegue alterar a realidade?");
        POWER_QUESTIONS.put("atravessar", "Seu personagem consegue atravessar paredes?");
        POWER_QUESTIONS.put("mimetismo", "Seu personagem consegue copiar lutas ao observar?");
        POWER_QUESTIONS.put("metamorfose", "Seu personagem consegue mudar de aparência?");
        POWER_QUESTIONS.put("viagem_no_tempo", "Seu personagem consegue viajar no tempo?");
        POWER_QUESTIONS.put("ilusao", "Seu personagem cria ilusões?");
        POWER_QUESTIONS.put("jovem", "Seu personagem começou a ser herói adolescente?");
    }

    public static CharacterProfile toProfile(Character c, Map<String, String> questionTextByKey) {
        Map<String, Double> attrs = new HashMap<>();

        if (c.gender == 1) {
            put(attrs, questionTextByKey, "gender_m", "Seu personagem é do gênero masculino?", YES);
        } else if (c.gender == 2) {
            put(attrs, questionTextByKey, "gender_m", "Seu personagem é do gênero masculino?", NO);
        }

        // Origem é mutuamente exclusiva — um personagem só pode ter UMA. Se
        // sabemos qual é (e ela está mapeada), gravamos YES pra ela e NO
        // explícito pra todas as outras: sem isso, as chaves não-correspondentes
        // ficariam ausentes e o motor as trataria como "não sei fraco" (0.1),
        // enfraquecendo o ganho de informação das perguntas de origem — que
        // costumam ser das mais discriminantes do jogo.
        //
        // Se origin é null OU cai em "Other" (não mapeado), não gravamos nada:
        // o motor mantém o comportamento antigo de tratar como desconhecido.
        String correctOriginKey = null;
        if (c.origin != null && c.origin.name != null) {
            String question = ORIGIN_QUESTIONS.get(c.origin.name);
            if (question != null) {
                correctOriginKey = "origin_" + slug(c.origin.name);
                put(attrs, questionTextByKey, correctOriginKey, question, YES);
            }
        }
        if (correctOriginKey != null) {
            for (Map.Entry<String, String> entry : ORIGIN_QUESTIONS.entrySet()) {
                String key = "origin_" + slug(entry.getKey());
                if (!key.equals(correctOriginKey)) {
                    put(attrs, questionTextByKey, key, entry.getValue(), NO);
                }
            }
        }

        put(attrs, questionTextByKey, "team_avengers", "Seu personagem já foi um Vingador?",
                Traits.isAvenger(c.id) ? YES : NO);
        put(attrs, questionTextByKey, "team_xmen", "Seu personagem já foi um X-Men?",
                Traits.isXMen(c.id) ? YES : NO);
        put(attrs, questionTextByKey, "team_guardians", "Seu personagem já foi um Guardião da Galáxia?",
                Traits.isGuardian(c.id) ? YES : NO);
        put(attrs, questionTextByKey, "team_f4", "Seu personagem faz parte do Quarteto Fantástico?",
                Traits.isFantasticFour(c.id) ? YES : NO);
        put(attrs, questionTextByKey, "team_inhumans", "Seu personagem é um Inumano?",
                Traits.isInhuman(c.id) ? YES : NO);
        put(attrs, questionTextByKey, "team_eternals", "Seu personagem é um Eterno?",
                Traits.isEternal(c.id) ? YES : NO);

        for (String power : Traits.powersOf(c.id)) {
            String question = POWER_QUESTIONS.get(power);
            if (question != null) {
                put(attrs, questionTextByKey, "power_" + power, question, YES);
            }
        }

        boolean villain = Rosters.isVillain(c.id);
        put(attrs, questionTextByKey, "is_villain", "Seu personagem é um vilão?", villain ? YES : NO);

        String imageUrl = c.image != null ? c.image.bestForHero() : null;
        return new CharacterProfile(c.id, c.name, imageUrl, attrs, c.issueCount, Rosters.isMainstream(c.id));
    }

    private static void put(Map<String, Double> attrs, Map<String, String> questionTextByKey,
                             String key, String questionText, double belief) {
        attrs.put(key, belief);
        questionTextByKey.putIfAbsent(key, questionText);
    }

    private static String slug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private CharacterMapper() { }
}
