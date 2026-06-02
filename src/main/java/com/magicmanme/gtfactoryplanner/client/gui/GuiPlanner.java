package com.magicmanme.gtfactoryplanner.client.gui;

import net.minecraft.client.gui.GuiScreen;

import com.magicmanme.gtfactoryplanner.data.RecipeIndex;

/**
 * v0 planner screen: proves the recipe index works by showing its stats.
 *
 * TODO: replace with a ModularUI2 panel — production-line table (Recipe | Machines
 * | EU/t | Products | Ingredients), target-rate entry, click-ingredient drill-down,
 * totals tab. See the project plan.
 */
public class GuiPlanner extends GuiScreen {

    private RecipeIndex index;

    @Override
    public void initGui() {
        // Built lazily on first open; cached for the session afterwards.
        this.index = RecipeIndex.get();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int centerX = width / 2;
        drawCenteredString(fontRendererObj, "GregTech Factory Planner", centerX, 30, 0xFFFFFF);
        drawCenteredString(
            fontRendererObj,
            String.format(
                "Recipe index: %,d recipes across %d machine maps (built in %d ms)",
                index.recipes.size(),
                index.mapCount,
                index.buildMillis),
            centerX,
            55,
            0xAAAAAA);
        drawCenteredString(
            fontRendererObj,
            String.format("%,d distinct products indexed", index.byOutput.size()),
            centerX,
            70,
            0xAAAAAA);
        drawCenteredString(fontRendererObj, "Planner UI under construction...", centerX, 95, 0x777777);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
