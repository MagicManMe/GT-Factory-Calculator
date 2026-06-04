package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.magicmanme.gtfactoryplanner.data.FluidKey;
import com.magicmanme.gtfactoryplanner.data.ItemKey;
import com.magicmanme.gtfactoryplanner.data.PlannerRecipe;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;

import gregtech.api.enums.HeatingCoilLevel;

/** Small shared UI building blocks and shared sizing/colour constants. */
final class UiHelpers {

    private UiHelpers() {}

    // --------------------------------------------------------------- constants

    /** Standard table row height. */
    static final int ROW_H = 18;
    /** Standard square action-button size (matches the 16px ingredient icons). */
    static final int BTN = 16;
    /** Subtle divider colour for section separators. */
    private static final int DIVIDER_COLOR = 0xFF505050;

    // ------------------------------------------------------------------ layout

    /** A thin full-width horizontal line for separating sections. */
    static IWidget divider() {
        return new Rectangle().color(DIVIDER_COLOR)
            .asWidget()
            .widthRel(1f)
            .height(1)
            .margin(0, 2);
    }

    /** A bold section-header label. */
    static IWidget sectionHeader(String text) {
        return IKey.str("§l" + text)
            .asWidget()
            .height(10);
    }

    /** Truncate to a max length with an ellipsis, for single-line labels. */
    static String truncate(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(1, max - 1)) + "…";
    }

    // ------------------------------------------------------------------- coils

    /** Selectable coil levels from GT's own enum (skips None and unimplemented ULV). */
    private static final java.util.List<HeatingCoilLevel> COILS = buildCoils();

    private static java.util.List<HeatingCoilLevel> buildCoils() {
        java.util.List<HeatingCoilLevel> coils = new java.util.ArrayList<>();
        for (HeatingCoilLevel level : HeatingCoilLevel.values()) {
            if (level == HeatingCoilLevel.None || level == HeatingCoilLevel.ULV) continue;
            coils.add(level);
        }
        return coils;
    }

    /** Whether this recipe's machine uses heating coils (EBF family, plasma forge). */
    static boolean usesCoils(PlannerRecipe recipe) {
        return recipe.specialValue > 0
            && (recipe.mapName.contains("blastfurnace") || recipe.mapName.contains("plasmaforge"));
    }

    /** Lowest coil base heat that lets the recipe run at the given voltage tier. */
    static int minCoilHeatFor(PlannerRecipe recipe, int voltageTier) {
        int tierBonus = 100 * Math.max(0, voltageTier - 2);
        for (HeatingCoilLevel level : COILS) {
            if (level.getHeat() + tierBonus >= recipe.specialValue) return (int) level.getHeat();
        }
        return (int) COILS.get(COILS.size() - 1)
            .getHeat();
    }

    /** Display name of the coil with the given base heat (e.g. "Nichrome"). */
    static String coilName(int coilHeat) {
        for (HeatingCoilLevel level : COILS) {
            if ((int) level.getHeat() == coilHeat) return level.getName();
        }
        return coilHeat + "K";
    }

    /**
     * Next/previous selectable coil, wrapping within [lowest viable, best].
     * {@code minHeat} is the floor (the recipe must still be able to run).
     */
    static int cycleCoilHeat(int currentHeat, int delta, int minHeat) {
        int currentIndex = coilIndex(currentHeat);
        int minIndex = coilIndex(minHeat);
        int index = currentIndex + delta;
        if (index < minIndex) index = COILS.size() - 1;
        if (index >= COILS.size()) index = minIndex;
        return (int) COILS.get(index)
            .getHeat();
    }

    private static int coilIndex(int coilHeat) {
        for (int i = 0; i < COILS.size(); i++) {
            if ((int) COILS.get(i)
                .getHeat() == coilHeat) return i;
        }
        return 0;
    }

    /** 16x16 icon widget for a resource (item icon, or a text placeholder for fluids). */
    static IWidget icon(ResourceKey key) {
        if (key instanceof ItemKey) {
            return new ItemDrawable(((ItemKey) key).toStack(1)).asWidget()
                .size(16);
        }
        return IKey.str("§b~")
            .asWidget()
            .size(16);
    }

    /**
     * NEI-style 16x16 ingredient icon with amount badge and rich tooltip: real item
     * tooltip (or fluid name) plus the per-run amount.
     */
    static IWidget stackIcon(PlannerRecipe.RStack rstack) {
        if (rstack.key instanceof ItemKey) {
            int badge = (int) Math.max(1, Math.round(rstack.amountPerRun));
            ItemStack stack = ((ItemKey) rstack.key).toStack(badge);
            return new ItemDisplayWidget().item(stack)
                .displayAmount(rstack.amountPerRun > 1)
                .size(16)
                .tooltipBuilder(tooltip -> {
                    tooltip.addFromItem(stack);
                    tooltip.addLine(amountTooltip(rstack));
                });
        }
        FluidStack fluidStack = ((FluidKey) rstack.key).toFluidStack(1);
        if (fluidStack != null) {
            return new FluidDrawable(fluidStack).asWidget()
                .size(16)
                .tooltipBuilder(tooltip -> {
                    tooltip.addLine("§b" + rstack.key.displayName());
                    tooltip.addLine(amountTooltip(rstack));
                });
        }
        return IKey.str("§b~")
            .asWidget()
            .size(16)
            .addTooltipLine(rstack.key.displayName());
    }

    private static String amountTooltip(PlannerRecipe.RStack rstack) {
        if (rstack.amountPerRun <= 0) {
            return "§7Not consumed (circuit/catalyst)";
        }
        String unit = rstack.key instanceof FluidKey ? " L" : "x";
        return "§7~" + formatAmount(rstack.amountPerRun) + unit + " per run";
    }

    /** Human-readable machine name for a recipe map's unlocalized name. */
    static String machineName(String mapName) {
        String localized = StatCollector.translateToLocal(mapName);
        if (localized != null && !localized.equals(mapName)) return localized;
        return mapName.replace("gt.recipe.", "");
    }

    /** The "main" output used to represent a recipe in lists. */
    static ResourceKey primaryOutput(PlannerRecipe recipe) {
        if (!recipe.itemOutputs.isEmpty()) return recipe.itemOutputs.get(0).key;
        return recipe.fluidOutputs.get(0).key;
    }

    /** Short one-line summary of a recipe's outputs + cost, for picker rows. */
    static String recipeSummary(PlannerRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (PlannerRecipe.RStack out : recipe.allOutputs()) {
            if (shown >= 3) {
                sb.append(", ...");
                break;
            }
            if (shown > 0) sb.append(", ");
            sb.append(formatAmount(out.amountPerRun))
                .append("x ")
                .append(out.key.displayName());
            shown++;
        }
        sb.append("  (")
            .append(recipe.euT)
            .append(" EU/t, ")
            .append(formatAmount(recipe.durationTicks / 20.0))
            .append("s)");
        return sb.toString();
    }

    static String formatAmount(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1e9) {
            return String.format(Locale.ROOT, "%,.0f", value);
        }
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}
