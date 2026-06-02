package com.eurowall.gtfactoryplanner.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.eurowall.gtfactoryplanner.Config;
import com.eurowall.gtfactoryplanner.GTFactoryPlanner;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * Read-only index of every GregTech machine recipe, built client-side from
 * {@link RecipeMap#ALL_RECIPE_MAPS}. This is the planner's entire data layer —
 * no transcription, no JSON dumps; whatever recipes the running pack has, we have.
 *
 * Built lazily on first use (all recipes are registered well before any world is
 * joined) and cached for the lifetime of the game session.
 */
public final class RecipeIndex {

    private static volatile RecipeIndex instance;

    public final List<PlannerRecipe> recipes;
    /** Recipes keyed by what they produce — the drill-down lookup ("what makes X?"). */
    public final Map<ResourceKey, List<PlannerRecipe>> byOutput;
    public final int mapCount;
    public final long buildMillis;

    public static RecipeIndex get() {
        RecipeIndex local = instance;
        if (local == null) {
            synchronized (RecipeIndex.class) {
                local = instance;
                if (local == null) {
                    instance = local = build();
                }
            }
        }
        return local;
    }

    /** Drop the cached index (e.g. if a config change alters filtering). */
    public static void invalidate() {
        instance = null;
    }

    private RecipeIndex(List<PlannerRecipe> recipes, Map<ResourceKey, List<PlannerRecipe>> byOutput, int mapCount,
        long buildMillis) {
        this.recipes = recipes;
        this.byOutput = byOutput;
        this.mapCount = mapCount;
        this.buildMillis = buildMillis;
    }

    /** Recipes producing the given resource, or an empty list. */
    public List<PlannerRecipe> producing(ResourceKey key) {
        List<PlannerRecipe> result = byOutput.get(key);
        return result != null ? result : java.util.Collections.emptyList();
    }

    private static RecipeIndex build() {
        long start = System.currentTimeMillis();
        List<PlannerRecipe> recipes = new ArrayList<>(300_000);
        Map<ResourceKey, List<PlannerRecipe>> byOutput = new HashMap<>(200_000);
        int mapCount = 0;

        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            mapCount++;
            for (GTRecipe r : map.getAllRecipes()) {
                if (r.mFakeRecipe || !r.mEnabled) continue;
                if (r.mHidden && !Config.indexHiddenRecipes) continue;
                PlannerRecipe normalized = normalize(map.unlocalizedName, r);
                if (normalized == null) continue;
                recipes.add(normalized);
                for (PlannerRecipe.RStack out : normalized.allOutputs()) {
                    byOutput.computeIfAbsent(out.key, k -> new ArrayList<>(2))
                        .add(normalized);
                }
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        GTFactoryPlanner.LOG
            .info("Recipe index built: {} recipes across {} maps in {} ms", recipes.size(), mapCount, elapsed);
        return new RecipeIndex(recipes, byOutput, mapCount, elapsed);
    }

    /** Convert one GT recipe to the normalized planner form. Returns null for degenerate recipes. */
    private static PlannerRecipe normalize(String mapName, GTRecipe r) {
        List<PlannerRecipe.RStack> itemInputs = new ArrayList<>(4);
        List<PlannerRecipe.RStack> itemOutputs = new ArrayList<>(4);
        List<PlannerRecipe.RStack> fluidInputs = new ArrayList<>(2);
        List<PlannerRecipe.RStack> fluidOutputs = new ArrayList<>(2);

        if (r.mInputs != null) {
            for (ItemStack stack : r.mInputs) {
                if (stack == null || stack.getItem() == null) continue;
                // stackSize 0 = non-consumed input (programmed circuit, catalyst)
                itemInputs.add(new PlannerRecipe.RStack(ItemKey.of(stack), stack.stackSize));
            }
        }
        if (r.mOutputs != null) {
            for (int i = 0; i < r.mOutputs.length; i++) {
                ItemStack stack = r.mOutputs[i];
                if (stack == null || stack.getItem() == null) continue;
                // Chanced outputs: use the long-run expected amount.
                double chance = chanceAt(r.mOutputChances, i);
                itemOutputs.add(new PlannerRecipe.RStack(ItemKey.of(stack), stack.stackSize * chance));
            }
        }
        if (r.mFluidInputs != null) {
            for (FluidStack stack : r.mFluidInputs) {
                if (stack == null || stack.getFluid() == null) continue;
                fluidInputs.add(new PlannerRecipe.RStack(FluidKey.of(stack), stack.amount));
            }
        }
        if (r.mFluidOutputs != null) {
            for (FluidStack stack : r.mFluidOutputs) {
                if (stack == null || stack.getFluid() == null) continue;
                fluidOutputs.add(new PlannerRecipe.RStack(FluidKey.of(stack), stack.amount));
            }
        }

        if (itemOutputs.isEmpty() && fluidOutputs.isEmpty()) return null;

        return new PlannerRecipe(
            mapName,
            r,
            itemInputs,
            itemOutputs,
            fluidInputs,
            fluidOutputs,
            r.mEUt,
            r.mDuration,
            r.mSpecialValue);
    }

    /** Output chance in [0,1]; GT stores chances out of 10000, missing array = guaranteed. */
    private static double chanceAt(int[] chances, int index) {
        if (chances == null || index >= chances.length) return 1.0;
        return chances[index] / 10000.0;
    }
}
