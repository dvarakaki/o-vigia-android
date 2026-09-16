package com.ovigia.app.translation;

import com.ovigia.app.model.CharacterDetail;
import com.ovigia.app.model.ImageData;

import org.junit.Test;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** O que dá para resolver sem tradutor e o que precisa dele. */
public class ComicVineTextsTest {

    @Test
    public void nascimentoEscritoComoDataViraData() {
        assertEquals(LocalDate.of(1962, 10, 14), ComicVineTexts.birthDate("Oct 14, 1962"));
        assertEquals(LocalDate.of(1962, 10, 14), ComicVineTexts.birthDate("October 14, 1962"));
        assertEquals(MonthDay.of(10, 14), ComicVineTexts.birthDate("October 14"));
    }

    @Test
    public void nascimentoEmTextoLivreNaoEData() {
        assertNull(ComicVineTexts.birthDate("Unknown"));
        assertNull(ComicVineTexts.birthDate("Before the dawn of Asgard"));
        assertNull(ComicVineTexts.birthDate(null));
    }

    @Test
    public void galeriasVemSeparadasPorVirgula() {
        assertEquals(List.of("All Images", "Spider-Man Art"),
                ComicVineTexts.galleries("All Images, Spider-Man Art,"));
        assertTrue(ComicVineTexts.isKnownGallery("all images"));
        assertTrue(ComicVineTexts.isKnownGallery("All Images"));
    }

    @Test
    public void soVaoParaOTradutorOsTextosQueEleResolve() {
        CharacterDetail detail = new CharacterDetail();
        detail.deck = " Peter Parker was bitten by a spider. ";
        detail.birth = "Oct 14, 1962";
        detail.image = new ImageData();
        detail.image.imageTags = "All Images, Fan Art";

        // O nascimento é data (vira formato do idioma) e "All Images" tem nome próprio no app.
        assertEquals(List.of("Peter Parker was bitten by a spider.", "Fan Art"),
                ComicVineTexts.shortTexts(detail));
    }

    @Test
    public void nascimentoEmTextoLivreVaiParaOTradutor() {
        CharacterDetail detail = new CharacterDetail();
        detail.birth = "Unknown";

        assertEquals(List.of("Unknown"), ComicVineTexts.shortTexts(detail));
    }

    @Test
    public void fichaVaziaNaoTemOQueTraduzir() {
        assertEquals(List.of(), ComicVineTexts.shortTexts(new CharacterDetail()));
        assertEquals(List.of(), ComicVineTexts.shortTexts(null));
    }

    @Test
    public void oIdiomaDaApiNaoPrecisaDeTraducao() {
        assertTrue(ComicVineTexts.isSourceLanguage("en"));
        assertTrue(!ComicVineTexts.isSourceLanguage("pt"));
    }
}
