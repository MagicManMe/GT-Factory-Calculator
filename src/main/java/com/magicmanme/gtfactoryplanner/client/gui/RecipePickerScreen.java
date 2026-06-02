package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.List;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.magicmanme.gtfactoryplanner.GTFactoryPlanner;
import com.magicmanme.gtfactoryplanner.client.PlannerState;
import com.magicmanme.gtfactoryplanner.data.PlannerRecipe;
import com.magicmanme.gtfactoryplanner.data.RecipeIndex;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;
import com.magicmanme.gtfactoryplanner.model.MachineConfig;
import com.magicmanme.gtfactoryplanner.model.PlanLine;

/**
 * Lists every recipe that produces a given resource; clicking one adds it as a
 * plan line (machine tier auto-set to the lowest that can run the recipe).
 * Recipes are always chosen by the user — never auto-picked.
 *
 * Note: extends ModularScreen with a panel-builder lambda capturing constructor
 * parameters. Do NOT use CustomModularScreen with instance fields here — the
 * super constructor invokes buildUI before subclass fields are assigned.
 */
public class RecipePickerScreen extends ModularScreen {

    public RecipePickerScreen(ResourceKey target) {
        super(GTFactoryPlanner.MODID, context -> buildPanel(target));
    }

    private static ModularPanel buildPanel(ResourceKey target) {
        List<PlannerRecipe> recipes = RecipeIndex.get()
            .producing(target);

        return ModularPanel.defaultPanel("recipe_picker", 300, 230)
            .padding(7)
            .child(
                Flow.column()
                    .sizeRel(1f)
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.START)
                    .child(
                        IKey.str("§lRecipes producing§r %s §7(%d)", target.displayName(), recipes.size())
                            .asWidget())
                    .child(
                        new ListWidget<>().widthRel(1f)
                            .expanded()
                            .children(recipes, RecipePickerScreen::recipeRow))
                    .child(
                        new ButtonWidget<>().width(50)
                            .height(14)
                            .overlay(IKey.str("Back"))
                            .onMousePressed(b -> {
                                PlannerScreen.reopen();
                                return true;
                            })));
    }

    private static IWidget recipeRow(PlannerRecipe recipe) {
        String label = "§b" + UiHelpers.machineName(recipe.mapName) + "§r - " + UiHelpers.recipeSummary(recipe);
        return Flow.row()
            .widthRel(1f)
            .height(18)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(UiHelpers.primaryOutput(recipe)))
            .child(
                new ScrollingTextWidget(IKey.str(label)).expanded()
                    .height(12))
            .child(
                new ButtonWidget<>().width(14)
                    .height(14)
                    .overlay(IKey.str("§2+"))
                    .addTooltipLine("Add this recipe to the plan")
                    .onMousePressed(b -> {
                        PlannerState.plan.lines
                            .add(new PlanLine(recipe, new MachineConfig(PlannerState.minTierFor(recipe.euT))));
                        PlannerScreen.reopen();
                        return true;
                    }));
    }
}
