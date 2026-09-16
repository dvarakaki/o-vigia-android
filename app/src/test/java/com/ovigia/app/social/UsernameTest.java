package com.ovigia.app.social;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UsernameTest {

    @Test
    public void normalize_dropsAtSignSpacesAndCase() {
        assertEquals("davi_s", Username.normalize("  @Davi_S "));
        assertEquals("davi", Username.normalize("@@davi"));
        assertEquals("", Username.normalize(null));
    }

    @Test
    public void validNames() {
        assertNull(Username.problemWith("davi"));
        assertNull(Username.problemWith("x_men_99"));
        assertTrue(Username.isValid("abc"));
        assertTrue(Username.isValid("a2345678901234567890"));
    }

    @Test
    public void invalidNames() {
        assertEquals(Username.Problem.TOO_SHORT, Username.problemWith("ab"));
        assertEquals(Username.Problem.TOO_LONG, Username.problemWith("a23456789012345678901"));
        assertEquals(Username.Problem.INVALID_CHARACTERS, Username.problemWith("joão"));
        assertEquals(Username.Problem.INVALID_CHARACTERS, Username.problemWith("ana lima"));
        assertEquals(Username.Problem.INVALID_CHARACTERS, Username.problemWith("Davi"));
        assertEquals(Username.Problem.ONLY_UNDERSCORES, Username.problemWith("___"));
    }

    @Test
    public void suggestion_comesFromTheAccountName() {
        assertEquals("davisouza", Username.suggestFrom("Davi Souza"));
        assertEquals("joaosilva", Username.suggestFrom("João Silva!"));
        assertEquals("", Username.suggestFrom("Jo"));
        assertEquals("", Username.suggestFrom(null));
        assertEquals(20, Username.suggestFrom("Pedro de Alcântara Francisco Antônio").length());
    }
}
