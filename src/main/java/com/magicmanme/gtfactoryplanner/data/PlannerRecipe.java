package com.magicmanme.gtfactoryplanner.data;

import java.util.Collections;
import java.util.List;

import gregtech.api.util.GTRecipe;

/**
 * Immutable, normalized view of one GregTech recipe: the solver and UI work with
 * this instead of {@link GTRecipe} directly so chanced outputs, null slots and
 * machine-map quirks are dealt with exactly once (in {@link RecipeIndex}).
 */
public final class PlannerRecipe {

    /** One side of a recipe slot: a resource and the (expected) amount per run. */
    public static final class RStack {

        public final ResourceKey key;
        /**
         * Expected amount per recipe run. For chanced outputs this is
         * {@code amount * chance / 10000} — the long-run average, which is what
         * throughput planning needs.
         */
        public final double amountPerRun;

        public RStack(ResourceKey key, double amountPerRun) {
            this.key = key;
            this.amountPerRun = amountPerRun;
        }

        @Override
        public String toString() {
            return amountPerRun + "x " + key;
        }
    }

    /** Unlocalized name of the owning recipe map, e.g. "gt.recipe.blastfurnace". */
    public final String mapName;
    /** Original GT recipe, kept for NEI integration / debugging. */
    public final GTRecipe source;

    /** Item inputs with amount 0 are non-consumed (programmed circuits, catalysts). */
    public final List<RStack> itemInputs;
    public final List<RStack> itemOutputs;
    public final List<RStack> fluidInputs;
    public final List<RStack> fluidOutputs;

    /** Base EU/t of the recipe (before overclocking). */
    public final int euT;
    /** Base duration in ticks (before overclocking). */
    public final int durationTicks;
    /** Machine-specific value; for the EBF family this is required coil heat (K). */
    public final int specialValue;

    public PlannerRecipe(String mapName, GTRecipe source, List<RStack> itemInputs, List<RStack> itemOutputs,
        List<RStack> fluidInputs, List<RStack> fluidOutputs, int euT, int durationTicks, int specialValue) {
        this.mapName = mapName;
        this.source = source;
        this.itemInputs = Collections.unmodifiableList(itemInputs);
        this.itemOutputs = Collections.unmodifiableList(itemOutputs);
        this.fluidInputs = Collections.unmodifiableList(fluidInputs);
        this.fluidOutputs = Collections.unmodifiableList(fluidOutputs);
        this.euT = euT;
        this.durationTicks = durationTicks;
        this.specialValue = specialValue;
    }

    /** All outputs, items first then fluids. */
    public Iterable<RStack> allOutputs() {
        return concat(itemOutputs, fluidOutputs);
    }

    /** All inputs, items first then fluids. */
    public Iterable<RStack> allInputs() {
        return concat(itemInputs, fluidInputs);
    }

    private static Iterable<RStack> concat(List<RStack> a, List<RStack> b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        java.util.ArrayList<RStack> out = new java.util.ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }
}
