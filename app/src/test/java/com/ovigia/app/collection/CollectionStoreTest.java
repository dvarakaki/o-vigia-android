package com.ovigia.app.collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionStoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() {
        file = new File(tmp.getRoot(), "collection.json");
    }

    @Test
    public void save_isIdempotentPerCharacter() {
        CollectionStore store = new CollectionStore(() -> file);
        assertTrue(store.save("ana", 7, "Wolverine", "img"));
        assertFalse("o mesmo personagem não entra duas vezes", store.save("ana", 7, "Wolverine", "img"));

        assertTrue(store.contains("ana", 7));
        assertEquals(1, store.list("ana").size());
    }

    @Test
    public void seenInCatalog_isPerAccountAndPersists() {
        CollectionStore store = new CollectionStore(() -> file);
        store.save("ana", 7, "Wolverine", "img");
        store.save("bia", 7, "Wolverine", "img");
        assertFalse(store.list("ana").get(0).seenInCatalog);

        store.markSeenInCatalog("ana", Collections.singletonList(7));

        CollectionStore reopened = new CollectionStore(() -> file);
        assertTrue(reopened.list("ana").get(0).seenInCatalog);
        assertFalse("outra conta não é afetada", reopened.list("bia").get(0).seenInCatalog);
        assertEquals("data original mantida", store.list("ana").get(0).savedAt, reopened.list("ana").get(0).savedAt);
    }

    @Test
    public void collections_areSeparatedByAccount() {
        CollectionStore store = new CollectionStore(() -> file);
        store.save("ana", 7, "Wolverine", "img");

        assertFalse(store.contains("bia", 7));
        assertTrue(store.list("bia").isEmpty());
        assertTrue(store.save("bia", 7, "Wolverine", "img"));
    }

    @Test
    public void list_isNewestFirst_andSurvivesReopening() throws Exception {
        CollectionStore store = new CollectionStore(() -> file);
        store.save("ana", 1, "Thor", "img-1");
        Thread.sleep(5);
        store.save("ana", 2, "Hulk", null);

        List<CollectionStore.Entry> entries = new CollectionStore(() -> file).list("ana");
        assertEquals(2, entries.size());
        assertEquals("Hulk", entries.get(0).name);
        assertEquals("img-1", entries.get(1).imageUrl);
    }
}
