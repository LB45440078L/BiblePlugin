package org.bibleplugin.spigot.gui;

import java.util.List;

/** Page arithmetic for menus. Pure, so it is unit-tested without a server. */
public record Paging<T>(List<T> items, int page, int pageCount) {

    public static <T> Paging<T> of(List<T> all, int requestedPage, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        int pages = Math.max(1, (all.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int from = Math.min(all.size(), page * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        return new Paging<>(all.subList(from, to), page, pages);
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page < pageCount - 1;
    }
}
