package com.ovigia.app.api;

import java.util.HashSet;
import java.util.Set;

/**
 * Elenco jogável (IDs Comic Vine) e classificações curadas manualmente —
 * heróis/vilões/times — que a API não expõe de forma confiável (o filtro
 * `publisher:` no endpoint de lista simplesmente não funciona nesta API, e
 * `teams`/`powers` só existem no endpoint de detalhe, um personagem por vez;
 * ver {@link Traits}). IDs verificados manualmente contra a Comic Vine,
 * com publisher = Marvel, ordenados pelos mais relevantes (mais aparições
 * em quadrinhos), pra garantir que o jogador reconheça o elenco.
 */
public final class Rosters {

    /** Times "principais" usados como perguntas do jogo — ver {@link Traits}. */
    public static final String AVENGERS =
            "1455|1442|2268|2267|3200|1475|1504|1466|1467|1926|1451|40470|20577|1502|2247"
                    + "|1472|1443|1477|1456|1449|1450|2265|1493|2503|1465|1454|2497|2101|4811|129909";

    public static final String XMEN =
            "1440|1459|3552|1444|1462|1464|2112|1446|1499|1461|1460|3548|1457|3176|4562|1441"
                    + "|1503|2157|9708|3546|1473|1496|4644|2158|2161|3560|4279|4557|6757|8303|4563|7612";

    public static final String GUARDIANS =
            "10957|6806|6807|32814|24341|3324|14558|2170|6805|7214";

    public static final String FANTASTIC4 =
            "2151|2190|2120|2114|2469|2470";

    public static final String INHUMANS =
            "4329|4327|2115|4330|10975|3534";

    public static final String ETERNALS =
            "13967|2118|13945|13822|34946|34947|20591|29157|56510|86297";

    /** Vilões — de todos os grupos acima e fora deles. */
    public static final String VILLAINS =
            "7607|4324|1468|1485|1483|1486|1441"
                    + "|2250|2242|2264|1469|4563|7612|2635|2205|4342|3530|4578|4333|1490|4458|3228"
                    + "|2126|4459|1488|4484|2475|4647|3225|2474|10242|3799|2481|12709|40901|7605|4825"
                    + "|3489|1445|7225|1536|3544|10964|21332";

    /** Elenco completo jogável. */
    public static final String ICONS =
            AVENGERS + "|" + XMEN + "|" + GUARDIANS + "|" + FANTASTIC4 + "|" + INHUMANS + "|" + ETERNALS + "|" + VILLAINS
                    // "extras": não se encaixam num time fixo, mas são reconhecíveis o bastante
                    + "|1453|1802|1525|24694|7570|7606|6108|6139|2502|2105|1476|1935|2104|2111"
                    + "|2113|2149|3202|3281|2258|2259|2262|21188|34207|12716|82858|79420|124409"
                    + "|2172|2488|70860|1479|1492|40505|40516|1474|1505|7258|42604";

    /**
     * Personagens que o público casual REALMENTE reconhece — nível MCU + os
     * únicos "de quadrinho" que atravessaram (Wolverine, Homem-Aranha,
     * Deadpool, X-Men clássicos, Thanos). O motor bayesiano ({@code GameEngine})
     * usa isso pra empurrar o prior desses candidatos pra cima, resolvendo o
     * caso em que personagens obscuros com muitas aparições em quadrinhos
     * (Luke Cage, Songbird, Speedball, Justice, Sunspot, Cannonball, etc.)
     * dominam por causa do {@code count_of_issue_appearances} alto sem que
     * ninguém tenha pensado neles.
     *
     * Critério pra entrar: aparece em filme/série MCU importante OU é um
     * X-Man dos anos 90 (linha clássica reconhecível) OU é vilão-símbolo
     * (Doutor Destino, Thanos, Duende Verde, Venom).
     */
    public static final String MAINSTREAM =
            // MCU Vingadores núcleo
            "1455|1442|2268|2267|3200|1475|1504|1466|1467|1926|1451|40470|20577|1502"
            + "|1472|1443|1477|1456|1454|1493|129909"
            // MCU expandido
            + "|10957|6806|6807|32814|24341|3324|14558|2170|7607|4324"
            + "|2151|2190|2120|2114|1476"
            + "|13967|2118|13945|13822|34946|34947|20591"
            + "|12716|40505|40516|2262|79420|82858"
            // X-Men clássicos (linha animada dos anos 90)
            + "|1440|1459|3552|1444|1462|1464|1446|1499|1461|1460|3548|1441"
            // Sobrenatural & Marvel Knights com filme
            + "|6108|7570|24694|1802|7606"
            // Vilões-símbolo
            + "|1468|1485|1486|1490|1441|2126|4459|1488|2475|4342|2205|2635";

    private static final Set<Integer> VILLAIN_IDS = parseIds(VILLAINS);
    private static final Set<Integer> AVENGER_IDS = parseIds(AVENGERS);
    private static final Set<Integer> XMEN_IDS = parseIds(XMEN);
    private static final Set<Integer> GUARDIAN_IDS = parseIds(GUARDIANS);
    private static final Set<Integer> F4_IDS = parseIds(FANTASTIC4);
    private static final Set<Integer> INHUMAN_IDS = parseIds(INHUMANS);
    private static final Set<Integer> ETERNAL_IDS = parseIds(ETERNALS);
    private static final Set<Integer> MAINSTREAM_IDS = parseIds(MAINSTREAM);

    public static boolean isVillain(int characterId) { return VILLAIN_IDS.contains(characterId); }
    public static boolean isAvenger(int characterId) { return AVENGER_IDS.contains(characterId); }
    public static boolean isXMen(int characterId) { return XMEN_IDS.contains(characterId); }
    public static boolean isGuardian(int characterId) { return GUARDIAN_IDS.contains(characterId); }
    public static boolean isFantasticFour(int characterId) { return F4_IDS.contains(characterId); }
    public static boolean isInhuman(int characterId) { return INHUMAN_IDS.contains(characterId); }
    public static boolean isEternal(int characterId) { return ETERNAL_IDS.contains(characterId); }
    public static boolean isMainstream(int characterId) { return MAINSTREAM_IDS.contains(characterId); }

    private static Set<Integer> parseIds(String pipeSeparated) {
        Set<Integer> ids = new HashSet<>();
        for (String s : pipeSeparated.split("\\|")) {
            ids.add(Integer.parseInt(s));
        }
        return ids;
    }

    private Rosters() { }
}
