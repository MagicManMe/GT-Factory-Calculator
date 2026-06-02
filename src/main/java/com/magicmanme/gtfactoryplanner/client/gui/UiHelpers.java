package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.FluidDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.magicmanme.gtfactoryplanner.data.FluidKey;
import com.magicmanme.gtfactoryplanner.data.ItemKey;
import com.magicmanme.gtfactoryplanner.data.PlannerRecipe;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;

/** Small shared UI building blocks. */
final class UiHelpers {

    private UiHelpers() {}

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
