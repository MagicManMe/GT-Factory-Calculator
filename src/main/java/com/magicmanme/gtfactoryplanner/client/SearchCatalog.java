package com.magicmanme.gtfactoryplanner.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.magicmanme.gtfactoryplanner.GTFactoryPlanner;
import com.magicmanme.gtfactoryplanner.data.RecipeIndex;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;

/**
 * Searchable catalog of every product the recipe index knows about, with display
 * names precomputed once (computing them per-keystroke over ~100k entries would
 * be far too slow).
 */
public final class SearchCatalog {

    public static final class Entry {

        public final ResourceKey key;
        public final String displayName;
        final String lowerName;

        Entry(ResourceKey key, String displayName) {
            this.key = key;
            this.displayName = displayName;
            this.lowerName = displayName.toLowerCase(Locale.ROOT);
        }
    }

    private static volatile List<Entry> entries;

    private SearchCatalog() {}

    public static List<Entry> all() {
        List<Entry> local = entries;
        if (local == null) {
            synchronized (SearchCatalog.class) {
                local = entries;
                if (local == null) {
                    entries = local = build();
                }
            }
        }
        return local;
    }

    /**
     * NEI-style search over product names, capped at {@code limit}: the query is
     * split on whitespace and every token must match as a substring, so
     * "naq ingot" finds "Naquadah Ingot".
     */
    public static List<Entry> search(String query, int limit) {
        String[] tokens = tokenize(query);
        List<Entry> matches = new ArrayList<>(limit);
        for (Entry entry : all()) {
            if (matchesAll(entry.lowerName, tokens)) {
                matches.add(entry);
                if (matches.size() >= limit) break;
            }
        }
        return matches;
    }

    /** Split a query into lowercase whitespace-separated tokens ("" -> none). */
    public static String[] tokenize(String query) {
        String trimmed = query == null ? ""
            : query.trim()
                .toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? EMPTY_TOKENS : trimmed.split("\\s+");
    }

    /** True if every token is a substring of the (lowercase) haystack. */
    public static boolean matchesAll(String lowerHaystack, String[] tokens) {
        for (String token : tokens) {
            if (!lowerHaystack.contains(token)) return false;
        }
        return true;
    }

    private static final String[] EMPTY_TOKENS = new String[0];

    private static List<Entry> build() {
        long start = System.currentTimeMillis();
        List<Entry> list = new ArrayList<>(RecipeIndex.get().byOutput.size());
        for (ResourceKey key : RecipeIndex.get().byOutput.keySet()) {
            list.add(new Entry(key, key.displayName()));
        }
        list.sort(Comparator.comparing(e -> e.lowerName));
        GTFactoryPlanner.LOG
            .info("Search catalog built: {} products in {} ms", list.size(), System.currentTimeMillis() - start);
        return list;
    }
}
