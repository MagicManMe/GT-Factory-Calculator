package com.eurowall.gtfactoryplanner.data;

/**
 * Identity of a plannable resource — an item (+meta) or a fluid. Used as the node
 * key in the recipe graph and as the key of the solver's demand map.
 */
public interface ResourceKey {

    /** Human-readable name for UI display. */
    String displayName();
}
