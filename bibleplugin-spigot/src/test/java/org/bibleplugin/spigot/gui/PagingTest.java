package org.bibleplugin.spigot.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PagingTest {

    private static final List<Integer> NUMBERS = java.util.stream.IntStream.rangeClosed(1, 10).boxed().toList();

    @Test
    void slicesTheRequestedPage() {
        Paging<Integer> page = Paging.of(NUMBERS, 1, 4);
        assertEquals(List.of(5, 6, 7, 8), page.items());
        assertEquals(3, page.pageCount());
        assertTrue(page.hasPrevious());
        assertTrue(page.hasNext());
    }

    @Test
    void clampsPagesOutOfRange() {
        assertEquals(2, Paging.of(NUMBERS, 99, 4).page());
        assertEquals(0, Paging.of(NUMBERS, -5, 4).page());
        assertEquals(List.of(9, 10), Paging.of(NUMBERS, 99, 4).items());
    }

    @Test
    void emptyListIsOnePage() {
        Paging<Integer> empty = Paging.of(List.of(), 3, 10);
        assertEquals(1, empty.pageCount());
        assertEquals(List.of(), empty.items());
        assertFalse(empty.hasNext());
        assertFalse(empty.hasPrevious());
    }

    @Test
    void rejectsNonsensicalPageSizes() {
        assertThrows(IllegalArgumentException.class, () -> Paging.of(NUMBERS, 0, 0));
    }
}
