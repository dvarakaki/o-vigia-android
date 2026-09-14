package com.ovigia.app.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Poderes curados manualmente por personagem (chave = id Comic Vine).
 *
 * A Comic Vine só expõe o campo "powers" no endpoint de detalhe de cada
 * personagem (não no de lista, que é o que usamos para buscar o elenco
 * inteiro em 1 request), e a taxonomia dela é genérica/inconsistente demais
 * para gerar boas perguntas de Akinator (ex.: o Homem-Aranha vem com "Siphon
 * Abilities", "Wall Clinger", "Feral"). Por isso os poderes usados no jogo
 * são atribuídos manualmente aqui, com um vocabulário pequeno e consistente.
 */
public final class Traits {

    private static final Set<Integer> AVENGERS_IDS = parseIds(Rosters.AVENGERS);
    private static final Set<Integer> XMEN_IDS = parseIds(Rosters.XMEN);
    private static final Set<Integer> GUARDIANS_IDS = parseIds(Rosters.GUARDIANS);
    private static final Set<Integer> F4_IDS = parseIds(Rosters.FANTASTIC4);
    private static final Set<Integer> INHUMANS_IDS = parseIds(Rosters.INHUMANS);
    private static final Set<Integer> ETERNALS_IDS = parseIds(Rosters.ETERNALS);

    public static boolean isAvenger(int id) { return AVENGERS_IDS.contains(id); }
    public static boolean isXMen(int id) { return XMEN_IDS.contains(id); }
    public static boolean isGuardian(int id) { return GUARDIANS_IDS.contains(id); }
    public static boolean isFantasticFour(int id) { return F4_IDS.contains(id); }
    public static boolean isInhuman(int id) { return INHUMANS_IDS.contains(id); }
    public static boolean isEternal(int id) { return ETERNALS_IDS.contains(id); }

    private static final Map<Integer, Set<String>> POWERS = new HashMap<>();
    static {
        tag(1440, "forca", "cura", "armas", "sentidos");           // Wolverine
        tag(1441, "energia", "genio");                             // Magneto
        tag(1442, "forca", "armas", "velocidade", "cura");         // Captain America — fórmula do super-soldado inclui regeneração acelerada
        tag(1443, "forca", "teia", "sentidos", "velocidade");      // Spider-Man
        tag(1444, "voo", "energia");                                // Storm
        tag(1446, "forca", "voo", "absorver");                     // Rogue
        tag(1449, "forca", "cura");                                 // She-Hulk
        tag(1450, "forca", "invulneravel");                        // Luke Cage
        tag(1451, "voo", "tecnologia");                             // Sam Wilson
        tag(1455, "voo", "tecnologia", "energia", "genio");        // Iron Man
        tag(1456, "magia", "teletransporte", "voo");               // Doctor Strange — voa com a Capa da Levitação
        tag(1457, "telepatia", "invulneravel");                    // Emma Frost
        tag(1459, "energia");                                       // Cyclops
        tag(1460, "forca", "invulneravel");                        // Colossus
        tag(1461, "teletransporte");                                // Nightcrawler
        tag(1462, "forca", "genio", "sentidos");                   // Beast
        tag(1464, "energia", "invulneravel");                       // Iceman — vira o corpo inteiro em gelo, resistente a dano
        tag(1466, "magia", "telecinese", "realidade");             // Scarlet Witch
        tag(1467, "velocidade");                                    // Quicksilver
        tag(1468, "magia", "tecnologia", "genio");                 // Doctor Doom
        tag(1472, "voo", "forca", "energia");                      // Captain Marvel
        tag(1475, "armas");                                         // Hawkeye
        tag(1476, "voo", "forca");                                  // Namor
        tag(1477, "forca", "sentidos", "tecnologia");              // Black Panther
        tag(1483, "forca", "genio");                                // Kingpin — chefão do crime tão gênio quanto forte (vs. Sandman)
        tag(1485, "genio", "tecnologia");                          // Doctor Octopus
        tag(1486, "forca", "cura", "teia");                         // Venom — simbionte gera teias como o Homem-Aranha
        tag(1492, "armas", "energia");                              // Iron Fist — soco de chi concentrado (o "Punho de Ferro")
        tag(1493, "armas", "forca");                                // Moon Knight — força sobre-humana concedida por Khonshu (vs. Gavião Arqueiro)
        tag(1499, "energia", "armas");                             // Gambit
        tag(1502, "voo", "tamanho", "energia");                    // Wasp
        tag(1504, "voo", "energia", "tecnologia", "tamanho");      // Vision
        tag(1505, "telepatia", "genio");                           // Professor X
        tag(1525, "armas");                                         // Punisher
        tag(1802, "armas", "sentidos");                             // Elektra — sentidos aguçados pelo treinamento ninja (A Seita)
        tag(1926, "voo", "tecnologia", "armas");                   // War Machine
        tag(2105, "voo", "forca", "energia");                      // Nova
        tag(2114, "forca", "invulneravel");                        // Thing
        tag(2120, "voo", "energia");                                // Human Torch
        tag(2151, "elasticidade", "genio");                        // Mr. Fantastic
        tag(2157, "telepatia", "telecinese", "armas");             // Cable
        tag(2170, "armas");                                         // Yondu
        tag(2190, "invisibilidade");                                // Invisible Woman
        tag(2265, "forca");                                         // Jessica Jones
        tag(2267, "forca", "cura", "invulneravel");                 // Hulk — pele verde resistente a quase todo dano físico
        tag(2268, "voo", "forca", "energia", "imortal", "armas");  // Thor — deus asgardiano milenar, arma-símbolo (Mjolnir / Stormbreaker)
        tag(2502, "voo", "energia", "realidade");                  // Silver Surfer
        tag(3176, "telepatia", "armas");                           // Psylocke
        tag(3200, "armas");                                         // Black Widow
        tag(3324, "telepatia", "cura");                            // Mantis
        tag(3548, "atravessar");                                    // Kitty Pryde
        tag(3552, "telepatia", "telecinese", "realidade");         // Jean Grey
        tag(4324, "magia", "teletransporte");                      // Loki
        tag(4562, "energia", "jovem");                              // Jubilee — entrou pra Nova Geração dos X-Men ainda adolescente
        tag(6108, "magia", "imortal");                             // Ghost Rider
        tag(6139, "voo", "forca", "energia", "armas");             // Beta Ray Bill
        tag(6805, "voo", "energia", "realidade");                  // Adam Warlock
        tag(6806, "armas", "forca");                                // Gamora — "mulher mais mortal da galáxia", força alienígena aumentada por Thanos
        tag(6807, "forca");                                         // Drax
        tag(7570, "cura", "armas");                                 // Blade
        tag(7606, "forca", "cura", "armas");                        // Deadpool
        tag(7607, "forca", "energia", "realidade", "genio");       // Thanos
        tag(10957, "armas", "tecnologia");                          // Star-Lord
        tag(14558, "tecnologia", "armas");                          // Nebula
        tag(20577, "tamanho");                                      // Ant-Man
        tag(24341, "forca", "cura", "tamanho");                    // Groot
        tag(24694, "sentidos", "armas");                            // Daredevil
        tag(32814, "armas", "genio", "sentidos");                  // Rocket Raccoon — sentidos animais aguçados (guaxinim geneticamente modificado)
        tag(40470, "armas", "tecnologia");                          // Bucky Barnes

        // --- Elenco expandido ---
        tag(1445, "forca", "invulneravel", "magia");                // Juggernaut — força vem da gema mística de Cyttorak (vs. Rhino, tecnológico)
        tag(1453, "voo", "energia", "sentidos");                    // Spider-Woman
        tag(1454, "forca", "voo", "energia", "realidade");          // Sentry — o Vazio consegue desfazer/apagar a realidade (vs. Thor)
        tag(1465, "forca", "voo");                                  // Wonder Man
        tag(1469, "armas", "metamorfose");                          // Mystique — muda de forma pra se passar por qualquer pessoa (vs. Typhoid Mary)
        tag(1473, "energia", "absorver");                           // Polaris — absorve e redireciona campos magnéticos (filha do Magneto)
        tag(1474, "teletransporte");                                // Cloak
        tag(1488, "forca", "cura", "sentidos");                     // Lizard — sentidos reptilianos aguçados (olfato, visão noturna)
        tag(1490, "forca", "cura", "elasticidade");                 // Carnage — simbionte mais fluido/elástico que o do Venom
        tag(1496, "voo", "invulneravel");                           // Cannonball
        tag(1503, "absorver", "energia", "armas");                  // Bishop — conhecido pela arma futurística de grande porte
        tag(1536, "armas");                                         // Typhoid Mary
        tag(1935, "energia");                                       // Dagger
        tag(2101, "voo", "energia");                                // Firestar
        tag(2104, "energia", "invulneravel");                       // Speedball
        tag(2111, "voo", "armas", "energia");                       // Darkhawk
        tag(2112, "voo");                                           // Angel
        tag(2113, "voo", "forca");                                  // Namorita
        tag(2115, "energia");                                       // Crystal
        tag(2118, "magia", "imortal", "realidade");                 // Sersi
        tag(2126, "forca", "invulneravel", "tecnologia");           // Rhino — força vem da armadura/exosqueleto, não é inata
        tag(2149, "voo", "energia", "realidade", "imortal");        // Galactus
        tag(2158, "forca", "sentidos");                             // Warpath
        tag(2161, "armas");                                         // Domino
        tag(2172, "forca", "invulneravel");                         // Devil Dinosaur — dinossauro gigante, resistente a dano físico
        tag(2205, "magia", "imortal", "realidade", "absorver");     // Dormammu — devora dimensões/energia inteiras para crescer
        tag(2242, "voo", "energia", "tecnologia", "invulneravel");  // Ultron
        tag(2247, "tamanho", "genio");                              // Hank Pym
        tag(2250, "armas", "genio");                                // Red Skull
        tag(2258, "armas", "jovem");                                // Patriot — Jovens Vingadores
        tag(2259, "forca", "voo", "jovem");                         // Hulkling — Jovens Vingadores (vs. Namor, mesmos 2 poderes mas milenar)
        tag(2262, "armas", "jovem");                                // Kate Bishop — virou Gaviã Arqueira ainda adolescente
        tag(2264, "tecnologia", "genio", "viagem_no_tempo");        // Kang — "o Conquistador", viajante do tempo (vs. Dr. Octopus/Homem-Toupeira)
        tag(2469, "realidade", "genio");                            // Franklin Richards
        tag(2470, "genio");                                         // Valeria Richards
        tag(2474, "tecnologia");                                    // Stilt-Man
        tag(2475, "forca", "sentidos");                             // Kraven the Hunter
        tag(2481, "genio", "tecnologia", "telepatia");              // Mole Man — comanda mentalmente seu exército de monstros subterrâneos
        tag(2488, "forca");                                         // Squirrel Girl
        tag(2497, "armas", "genio");                                // Mockingbird — Dr. Bobbi Morse, PhD em bioquímica (vs. Black Widow)
        tag(2503, "forca", "imortal");                              // Hercules
        tag(2635, "magia", "imortal", "realidade", "telepatia");    // Mephisto — lê mentes e manipula desejos p/ barganhar almas
        tag(3202, "armas", "genio");                                // Nick Fury
        tag(3225, "forca", "armas");                                // Crossbones
        tag(3228, "energia");                                       // Electro
        tag(3281, "voo", "energia");                                // Songbird
        tag(3489, "forca", "cura");                                 // Abomination — mutação gama permanente, sem outro traço extra (vs. Venom/Lizard/Carnage)
        tag(3530, "magia", "tecnologia");                           // Mandarin
        tag(3534, "teletransporte");                                // Lockjaw
        tag(3544, "forca");                                         // Sandman
        tag(3546, "energia", "tecnologia");                         // Havok — usa um traje de contenção pra controlar/focar as rajadas de plasma
        tag(3560, "forca", "cura", "armas", "sentidos", "jovem");   // X-23 — criada/experimentada quando ainda criança
        tag(3799, "telecinese");                                    // Puppet Master
        tag(4279, "genio", "tecnologia");                           // Forge
        tag(4329, "voo", "energia");                                // Black Bolt
        tag(4330, "forca", "energia");                              // Gorgon
        tag(4333, "tecnologia", "ilusao");                          // Mysterio — mestre das ilusões (vs. Homem-Palito, só a tecnologia)
        tag(4342, "magia", "imortal", "realidade");                 // Hela
        tag(4459, "voo", "tecnologia");                             // Vulture
        tag(4484, "forca", "tecnologia");                           // Scorpion
        tag(4557, "forca", "sentidos");                             // Wolfsbane
        tag(4563, "forca", "cura", "sentidos");                     // Sabretooth
        tag(4578, "armas", "mimetismo");                            // Taskmaster — reflexos fotográficos, copia qualquer luta que vê
        tag(4644, "forca", "voo", "energia");                       // Sunspot
        tag(4647, "armas", "sentidos");                             // Bullseye — precisão/coordenação sobre-humana (acerta qualquer alvo com qualquer objeto)
        tag(4811, "telecinese", "voo");                             // Justice
        tag(4825, "energia", "tecnologia");                         // Shocker — poder 100% da manopla, sem nada biológico (vs. Electro)
        tag(7214, "telepatia", "telecinese");                       // Moondragon
        tag(7225, "magia");                                         // Enchantress
        tag(7258, "forca", "tecnologia");                           // Deathlok
        tag(7605, "voo", "tecnologia", "armas", "forca");           // Hobgoblin — fórmula duende também dá força sobre-humana (vs. Jack O'Lantern, só tecnologia)
        tag(7612, "forca", "imortal", "genio", "tecnologia");       // Apocalypse
        tag(8303, "magia", "teletransporte", "armas");              // Magik
        tag(9708, "energia", "sentidos");                           // Banshee — audição sobre-humana ligada ao grito sônico
        tag(10242, "magia");                                        // Diablo
        tag(10964, "energia", "imortal");                           // Annihilus
        tag(10975, "sentidos");                                     // Karnak
        tag(12709, "energia", "tecnologia", "armas");               // Whiplash — usa chicotes de energia como arma (vs. Shocker)
        tag(12716, "armas", "magia");                               // Shang-Chi — Dez Anéis e a mística de Ta Lo
        tag(13822, "energia", "imortal");                           // Thena
        tag(13945, "velocidade", "imortal");                        // Makkari
        tag(13967, "voo", "energia", "forca", "imortal");           // Ikaris
        tag(20591, "tecnologia", "genio", "imortal");                // Phastos
        tag(21188, "forca", "sentidos");                            // Tigra
        tag(21332, "forca", "voo", "energia", "invisibilidade", "elasticidade"); // Super-Skrull
        tag(29157, "forca", "imortal");                             // Gilgamesh
        tag(34207, "forca", "sentidos", "armas");                   // White Tiger
        tag(34946, "imortal", "cura");                              // Ajak
        tag(34947, "telepatia", "imortal");                         // Druig
        tag(40505, "magia", "realidade", "jovem");                  // Wiccan — Jovens Vingadores
        tag(40516, "tamanho", "jovem");                             // Cassie Lang — vira heroína ainda adolescente
        tag(40901, "voo", "tecnologia", "armas");                   // Jack O'Lantern
        tag(42604, "forca", "energia");                             // Korath the Pursuer
        tag(56510, "energia", "imortal");                           // Kingo Sunen
        tag(79420, "forca", "teia", "invisibilidade", "jovem");     // Miles Morales — mordido pela aranha ainda no colégio
        tag(82858, "voo", "forca", "energia", "jovem");             // Sam Alexander — era um adolescente ao herdar o capacete Nova (vs. Richard Rider, adulto)
        tag(86297, "imortal");                                      // Sprite (Eternals)
        tag(124409, "genio");                                       // Moon Girl
        tag(129909, "voo", "tecnologia", "genio", "energia");       // Ironheart

        // --- Cobertura complementar (personagens do elenco ainda sem poderes tagueados) ---
        tag(1479, "armas", "sentidos");                             // Black Cat — ladra ninja com agilidade felina sobre-humana e gadgets/garras
        tag(4327, "telecinese", "armas");                           // Medusa — controla o próprio cabelo psicocineticamente e usa como chicote/armas
        tag(4458, "metamorfose", "tecnologia");                     // Chameleon — muda de aparência via máscaras/tecnologia (vs. Mystique, biológica)
        tag(6757, "armas", "sentidos", "realidade");                // Longshot — acrobata com facas, agilidade extradimensional e "boa sorte" (manipula probabilidade)
        tag(70860, "armas", "tecnologia");                          // Phil Coulson — agente SHIELD, humano normal com armamento tático
    }

    public static Set<String> powersOf(int characterId) {
        Set<String> powers = POWERS.get(characterId);
        return powers == null ? Collections.emptySet() : powers;
    }

    private static void tag(int characterId, String... powers) {
        POWERS.put(characterId, new HashSet<>(Arrays.asList(powers)));
    }

    private static Set<Integer> parseIds(String pipeSeparated) {
        Set<Integer> ids = new HashSet<>();
        for (String s : pipeSeparated.split("\\|")) {
            ids.add(Integer.parseInt(s));
        }
        return ids;
    }

    private Traits() { }
}
