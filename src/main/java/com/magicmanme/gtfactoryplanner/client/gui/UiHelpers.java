package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.Locale;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
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
