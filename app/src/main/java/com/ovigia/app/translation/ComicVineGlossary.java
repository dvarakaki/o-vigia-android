package com.ovigia.app.translation;

import android.content.res.Resources;

import com.ovigia.app.R;
import com.ovigia.app.model.ApiRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vocabulário fechado da Comic Vine — os poderes, as origens e as galerias de
 * imagem — traduzido à mão, como o resto dos textos do app.
 *
 * São listas pequenas e quase imutáveis (a API tem 128 poderes e 10 origens),
 * então a tradução curada sai na hora, sem tradutor nenhum, e evita os erros
 * que o automático cometeria fora de contexto ("Power Suit", "Feral", "Stamina").
 * Termo novo na API aparece em inglês até alguém traduzi-lo aqui.
 *
 * O mapa é explícito (em vez de {@code Resources.getIdentifier}) para o R8
 * enxergar as referências e não apagar as strings ao encolher os recursos;
 * {@code ComicVineGlossaryTest} confere se toda entrada tem texto em todos os idiomas.
 */
public final class ComicVineGlossary {

    private static final Map<Integer, Integer> ORIGINS = origins();
    private static final Map<Integer, Integer> POWERS = powers();

    /** Nome da origem no idioma do app; o da API quando ela for nova por lá. */
    public static String origin(Resources res, ApiRef origin) {
        return origin == null ? null : name(res, ORIGINS.get(origin.id), origin.name);
    }

    /** Nome do poder no idioma do app; o da API quando ele for novo por lá. */
    public static String power(Resources res, ApiRef power) {
        return power == null ? null : name(res, POWERS.get(power.id), power.name);
    }

    /** A lista de poderes com os nomes traduzidos, na ordem em que a API mandou. */
    public static List<ApiRef> powers(Resources res, List<ApiRef> powers) {
        if (powers == null) return null;
        List<ApiRef> translated = new ArrayList<>(powers.size());
        for (ApiRef power : powers) {
            translated.add(power == null ? null : renamed(power, power(res, power)));
        }
        return translated;
    }

    /** Nome da galeria de imagens; {@code null} quando o app não conhece essa galeria. */
    public static String gallery(Resources res, String gallery) {
        return ComicVineTexts.isKnownGallery(gallery) ? res.getString(R.string.cv_gallery_all_images) : null;
    }

    private static String name(Resources res, Integer text, String fallback) {
        return text == null ? fallback : res.getString(text);
    }

    /** Cópia da referência só com o nome trocado: o resto (links, id) continua valendo. */
    private static ApiRef renamed(ApiRef ref, String name) {
        ApiRef copy = new ApiRef();
        copy.id = ref.id;
        copy.name = name;
        copy.apiDetailUrl = ref.apiDetailUrl;
        copy.siteDetailUrl = ref.siteDetailUrl;
        copy.issueNumber = ref.issueNumber;
        return copy;
    }

    private static Map<Integer, Integer> origins() {
        Map<Integer, Integer> origins = new HashMap<>();
        origins.put(1, R.string.cv_origin_mutant); // Mutant
        origins.put(2, R.string.cv_origin_cyborg); // Cyborg
        origins.put(3, R.string.cv_origin_alien); // Alien
        origins.put(4, R.string.cv_origin_human); // Human
        origins.put(5, R.string.cv_origin_robot); // Robot
        origins.put(6, R.string.cv_origin_radiation); // Radiation
        origins.put(7, R.string.cv_origin_god_eternal); // God/Eternal
        origins.put(8, R.string.cv_origin_animal); // Animal
        origins.put(9, R.string.cv_origin_other); // Other
        origins.put(10, R.string.cv_origin_infection); // Infection
        return origins;
    }

    private static Map<Integer, Integer> powers() {
        Map<Integer, Integer> powers = new HashMap<>();
        powers.put(1, R.string.cv_power_flight); // Flight
        powers.put(2, R.string.cv_power_super_strength); // Super Strength
        powers.put(3, R.string.cv_power_super_speed); // Super Speed
        powers.put(4, R.string.cv_power_agility); // Agility
        powers.put(5, R.string.cv_power_stamina); // Stamina
        powers.put(6, R.string.cv_power_invulnerability); // Invulnerability
        powers.put(7, R.string.cv_power_telepathy); // Telepathy
        powers.put(8, R.string.cv_power_telekinesis); // Telekinesis
        powers.put(9, R.string.cv_power_intellect); // Intellect
        powers.put(10, R.string.cv_power_teleport); // Teleport
        powers.put(11, R.string.cv_power_radar_sense); // Radar Sense
        powers.put(12, R.string.cv_power_psychic); // Psychic
        powers.put(13, R.string.cv_power_force_field); // Force Field
        powers.put(14, R.string.cv_power_blast_power); // Blast Power
        powers.put(15, R.string.cv_power_healing); // Healing
        powers.put(16, R.string.cv_power_magic); // Magic
        powers.put(17, R.string.cv_power_weapon_master); // Weapon Master
        powers.put(18, R.string.cv_power_super_sight); // Super Sight
        powers.put(19, R.string.cv_power_super_smell); // Super Smell
        powers.put(20, R.string.cv_power_super_hearing); // Super Hearing
        powers.put(21, R.string.cv_power_invisibility); // Invisibility
        powers.put(22, R.string.cv_power_phasing); // Phasing / Ghost
        powers.put(23, R.string.cv_power_chameleon); // Chameleon
        powers.put(24, R.string.cv_power_shape_shifter); // Shape Shifter
        powers.put(25, R.string.cv_power_magnetism); // Magnetism
        powers.put(26, R.string.cv_power_fire_control); // Fire Control
        powers.put(27, R.string.cv_power_implants); // Implants
        powers.put(28, R.string.cv_power_feral); // Feral
        powers.put(29, R.string.cv_power_psionic); // Psionic
        powers.put(30, R.string.cv_power_insanely_rich); // Insanely Rich
        powers.put(31, R.string.cv_power_power_suit); // Power Suit
        powers.put(32, R.string.cv_power_radiation); // Radiation
        powers.put(33, R.string.cv_power_elasticity); // Elasticity
        powers.put(34, R.string.cv_power_unarmed_combat); // Unarmed Combat
        powers.put(35, R.string.cv_power_gadgets); // Gadgets
        powers.put(36, R.string.cv_power_weather_control); // Weather Control
        powers.put(37, R.string.cv_power_divine_powers); // Divine Powers
        powers.put(38, R.string.cv_power_siphon_abilities); // Siphon Abilities
        powers.put(39, R.string.cv_power_size_manipulation); // Size Manipulation
        powers.put(40, R.string.cv_power_immortal); // Immortal
        powers.put(49, R.string.cv_power_necromancy); // Necromancy
        powers.put(50, R.string.cv_power_vampirism); // Vampirism
        powers.put(51, R.string.cv_power_electricity_control); // Electricity Control
        powers.put(52, R.string.cv_power_ice_control); // Ice Control
        powers.put(53, R.string.cv_power_sub_mariner); // Sub-Mariner
        powers.put(54, R.string.cv_power_wall_clinger); // Wall Clinger
        powers.put(55, R.string.cv_power_mesmerize); // Mesmerize
        powers.put(56, R.string.cv_power_penance_stare); // Penance Stare
        powers.put(57, R.string.cv_power_sonic_scream); // Sonic Scream
        powers.put(58, R.string.cv_power_escape_artist); // Escape Artist
        powers.put(59, R.string.cv_power_tracking); // Tracking
        powers.put(60, R.string.cv_power_astral_projection); // Astral Projection
        powers.put(61, R.string.cv_power_danger_sense); // Danger Sense
        powers.put(64, R.string.cv_power_possession); // Possession
        powers.put(65, R.string.cv_power_sand_manipulation); // Sand Manipulation
        powers.put(66, R.string.cv_power_adaptive); // Adaptive
        powers.put(67, R.string.cv_power_electronic_interaction); // Electronic Interaction
        powers.put(68, R.string.cv_power_animation); // Animation
        powers.put(69, R.string.cv_power_probability_manipulation); // Probability Manipulation
        powers.put(70, R.string.cv_power_super_eating); // Super Eating
        powers.put(71, R.string.cv_power_precognition); // Precognition
        powers.put(72, R.string.cv_power_energy_enhanced_strike); // Energy-Enhanced Strike
        powers.put(73, R.string.cv_power_technopathy); // Technopathy
        powers.put(74, R.string.cv_power_inertia_absorption); // Inertia Absorption
        powers.put(75, R.string.cv_power_light_projection); // Light Projection
        powers.put(76, R.string.cv_power_holographic_projection); // Holographic Projection
        powers.put(77, R.string.cv_power_reality_manipulation); // Reality Manipulation
        powers.put(78, R.string.cv_power_swordsmanship); // Swordsmanship
        powers.put(79, R.string.cv_power_electronic_disruption); // Electronic Disruption
        powers.put(80, R.string.cv_power_levitation); // Levitation
        powers.put(81, R.string.cv_power_soul_absorption); // Soul Absorption
        powers.put(82, R.string.cv_power_duplication); // Duplication
        powers.put(83, R.string.cv_power_emotion_control); // Emotion Control
        powers.put(84, R.string.cv_power_matter_absorption); // Matter Absorption
        powers.put(85, R.string.cv_power_dimensional_manipulation); // Dimensional Manipulation
        powers.put(87, R.string.cv_power_animal_control); // Animal Control
        powers.put(88, R.string.cv_power_enhance_mutation); // Enhance Mutation
        powers.put(89, R.string.cv_power_omni_lingual); // Omni-lingual
        powers.put(90, R.string.cv_power_postcognition); // Postcognition
        powers.put(91, R.string.cv_power_shadowmeld); // Shadowmeld
        powers.put(92, R.string.cv_power_energy_shield); // Energy Shield
        powers.put(93, R.string.cv_power_time_travel); // Time Travel
        powers.put(94, R.string.cv_power_siphon_lifeforce); // Siphon Lifeforce
        powers.put(95, R.string.cv_power_sense_death); // Sense Death
        powers.put(96, R.string.cv_power_darkforce_manipulation); // Darkforce Manipulation
        powers.put(97, R.string.cv_power_vibration_wave); // Vibration Wave
        powers.put(98, R.string.cv_power_power_mimicry); // Power Mimicry
        powers.put(99, R.string.cv_power_empathy); // Empathy
        powers.put(100, R.string.cv_power_gravity_control); // Gravity Control
        powers.put(101, R.string.cv_power_darkness_manipulation); // Darkness Manipulation
        powers.put(102, R.string.cv_power_illusion_casting); // Illusion Casting
        powers.put(103, R.string.cv_power_heat_vision); // Heat Vision
        powers.put(104, R.string.cv_power_psychometry); // Psychometry
        powers.put(105, R.string.cv_power_wind_bursts); // Wind Bursts
        powers.put(106, R.string.cv_power_density_control); // Density Control
        powers.put(107, R.string.cv_power_earth_manipulation); // Earth Manipulation
        powers.put(108, R.string.cv_power_marksmanship); // Marksmanship
        powers.put(109, R.string.cv_power_genetic_manipulation); // Genetic Manipulation
        powers.put(110, R.string.cv_power_time_manipulation); // Time Manipulation
        powers.put(111, R.string.cv_power_power_item); // Power Item
        powers.put(112, R.string.cv_power_plant_control); // Plant Control
        powers.put(113, R.string.cv_power_claws); // Claws
        powers.put(114, R.string.cv_power_death_touch); // Death Touch
        powers.put(115, R.string.cv_power_hypnosis); // Hypnosis
        powers.put(116, R.string.cv_power_poisonous); // Poisonous
        powers.put(117, R.string.cv_power_pheromone_control); // Pheromone Control
        powers.put(118, R.string.cv_power_water_control); // Water Control
        powers.put(119, R.string.cv_power_chemical_secretion); // Chemical Secretion
        powers.put(120, R.string.cv_power_energy_absorption); // Energy Absorption
        powers.put(123, R.string.cv_power_energy_manipulation); // Energy Manipulation
        powers.put(124, R.string.cv_power_cosmic_awareness); // Cosmic Awareness
        powers.put(125, R.string.cv_power_willpower_constructs); // Willpower-Based Constructs
        powers.put(126, R.string.cv_power_heat_generation); // Heat Generation
        powers.put(128, R.string.cv_power_energy_constructs); // Energy Based Constructs
        powers.put(129, R.string.cv_power_synaesthesia); // Synaesthesia
        powers.put(130, R.string.cv_power_voice_manipulation); // Voice-induced Manipulation
        powers.put(131, R.string.cv_power_chemical_absorption); // Chemical Absorption
        powers.put(132, R.string.cv_power_stealth); // Stealth
        powers.put(133, R.string.cv_power_berserker_strength); // Berserker Strength
        powers.put(135, R.string.cv_power_leadership); // Leadership
        powers.put(136, R.string.cv_power_longevity); // Longevity
        powers.put(137, R.string.cv_power_bone_growth); // Controlled Bone Growth
        powers.put(138, R.string.cv_power_webslinger); // Webslinger
        powers.put(139, R.string.cv_power_ice_breath); // Ice Breath
        powers.put(140, R.string.cv_power_flame_breath); // Flame Breath
        powers.put(141, R.string.cv_power_hellfire_control); // Hellfire Control
        powers.put(142, R.string.cv_power_prehensile_hair); // Prehensile Hair
        powers.put(143, R.string.cv_power_blood_control); // Blood Control
        return powers;
    }

    private ComicVineGlossary() { }
}
