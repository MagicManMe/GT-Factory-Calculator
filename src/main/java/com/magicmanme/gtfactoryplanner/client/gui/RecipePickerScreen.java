package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.magicmanme.gtfactoryplanner.GTFactoryPlanner;
import com.magicmanme.gtfactoryplanner.client.PlannerState;
import com.magicmanme.gtfactoryplanner.data.PlannerRecipe;
import com.magicmanme.gtfactoryplanner.data.RecipeIndex;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;
import com.magicmanme.gtfactoryplanner.model.MachineConfig;
import com.magicmanme.gtfactoryplanner.model.PlanLine;

import gregtech.api.enums.GTValues;

/**
 * NEI-inspired recipe picker. Like NEI's recipe view, recipes are grouped by
 * machine (left column = NEI's handler tabs) and each recipe is shown visually:
 * input icons {@code >} output icons with amount badges and rich item tooltips.
 * A search box filters by ingredient, product or machine name.
 *
 * Note: extends ModularScreen with a panel-builder lambda capturing constructor
 * parameters. Do NOT use CustomModularScreen with instance fields here — the
 * super constructor invokes buildUI before subclass fields are assigned.
 */
public class RecipePickerScreen extends ModularScreen {

    /** Hard cap on built recipe rows per filter state, to keep the UI snappy. */
    private static final int MAX_ROWS = 150;
    private static final int MAX_INPUT_ICONS = 4;
    private static final int MAX_OUTPUT_ICONS = 3;

    public RecipePickerScreen(ResourceKey target) {
        super(GTFactoryPlanner.MODID, context -> buildPanel(target));
    }

    /** One pickable recipe plus its precomputed lowercase search haystack. */
    private static final class Entry {

        final PlannerRecipe recipe;
        final String searchText;

        Entry(PlannerRecipe recipe) {
            this.recipe = recipe;
            StringBuilder sb = new StringBuilder(UiHelpers.machineName(recipe.mapName));
            for (PlannerRecipe.RStack stack : recipe.allInputs()) {
                sb.append(' ')
                    .append(stack.key.displayName());
            }
            for (PlannerRecipe.RStack stack : recipe.allOutputs()) {
                sb.append(' ')
                    .append(stack.key.displayName());
            }
            this.searchText = sb.toString()
                .toLowerCase(Locale.ROOT);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static ModularPanel buildPanel(ResourceKey target) {
        List<PlannerRecipe> producing = RecipeIndex.get()
            .producing(target);

        // Group by machine, most recipes first; sort within a group by EU/t.
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        for (PlannerRecipe recipe : producing) {
            groups.computeIfAbsent(recipe.mapName, k -> new ArrayList<>())
                .add(new Entry(recipe));
        }
        List<Map.Entry<String, List<Entry>>> orderedGroups = new ArrayList<>(groups.entrySet());
        orderedGroups.sort(
            Comparator.comparingInt(
                (Map.Entry<String, List<Entry>> e) -> e.getValue()
                    .size())
                .reversed());
        for (List<Entry> group : groups.values()) {
            group.sort(Comparator.comparingInt(e -> e.recipe.euT));
        }

        // Mutable filter state, captured by lambdas (not screen fields, see note).
        String[] query = { "" };
        String[] selectedMap = { null };

        ListWidget recipeList = (ListWidget) new ListWidget<>().expanded()
            .heightRel(1f);

        Runnable rebuildRecipes = () -> {
            recipeList.removeAll();
            int added = 0;
            for (Map.Entry<String, List<Entry>> group : orderedGroups) {
                if (selectedMap[0] != null && !selectedMap[0].equals(group.getKey())) continue;
                for (Entry entry : group.getValue()) {
                    if (!query[0].isEmpty() && !entry.searchText.contains(query[0])) continue;
                    if (added >= MAX_ROWS) break;
                    recipeList.child(recipeRow(entry.recipe));
                    added++;
                }
                if (added >= MAX_ROWS) break;
            }
            if (added == 0) {
                recipeList.child(
                    IKey.str("§7No recipes match.")
                        .asWidget());
            } else if (added >= MAX_ROWS) {
                recipeList.child(
                    IKey.str("§7... more hidden; refine the search.")
                        .asWidget());
            }
        };
        rebuildRecipes.run();

        ListWidget machineList = (ListWidget) new ListWidget<>().width(112)
            .heightRel(1f);
        machineList.child(machineRow(null, producing.size(), selectedMap, rebuildRecipes));
        for (Map.Entry<String, List<Entry>> group : orderedGroups) {
            machineList.child(
                machineRow(
                    group.getKey(),
                    group.getValue()
                        .size(),
                    selectedMap,
                    rebuildRecipes));
        }

        return ModularPanel.defaultPanel("recipe_picker", 380, 250)
            .padding(7)
            .child(
                Flow.column()
                    .sizeRel(1f)
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.START)
                    .child(
                        Flow.row()
                            .widthRel(1f)
                            .height(16)
                            .childPadding(4)
                            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                            .child(UiHelpers.icon(target))
                            .child(
                                new ScrollingTextWidget(
                                    IKey.str("§lRecipes producing§r %s §7(%d)", target.displayName(), producing.size()))
                                        .expanded()
                                        .height(12)))
                    .child(new TextFieldWidget().value(new StringValue.Dynamic(() -> query[0], text -> {
                        String lower = text.trim()
                            .toLowerCase(Locale.ROOT);
                        if (!lower.equals(query[0])) {
                            query[0] = lower;
                            rebuildRecipes.run();
                        }
                    }))
                        .autoUpdateOnChange(true)
                        .widthRel(1f)
                        .height(14))
                    .child(
                        Flow.row()
                            .widthRel(1f)
                            .expanded()
                            .childPadding(4)
                            .child(machineList)
                            .child(recipeList))
                    .child(
                        new ButtonWidget<>().width(50)
                            .height(14)
                            .overlay(IKey.str("Back"))
                            .onMousePressed(b -> {
                                PlannerScreen.reopen();
                                return true;
                            })));
    }

    /** Left-column machine tab (null mapName = "All machines"). */
    private static IWidget machineRow(String mapName, int count, String[] selectedMap, Runnable rebuildRecipes) {
        String name = mapName == null ? "All machines" : UiHelpers.machineName(mapName);
        String label = name + " §7(" + count + ")";
        return new ButtonWidget<>().widthRel(1f)
            .height(13)
            .overlay(IKey.dynamicKey(() -> IKey.str((Objects.equals(selectedMap[0], mapName) ? "§e> " : "") + label)))
            .addTooltipLine(name)
            .onMousePressed(b -> {
                selectedMap[0] = mapName;
                rebuildRecipes.run();
                return true;
            });
    }

    /** One recipe as an NEI-style visual row: inputs > outputs, cost, add button. */
    private static IWidget recipeRow(PlannerRecipe recipe) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(20)
            .childPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER);

        addStackIcons(row, recipe.allInputs(), MAX_INPUT_ICONS);
        row.child(
            IKey.str("§7>")
                .asWidget());
        addStackIcons(row, recipe.allOutputs(), MAX_OUTPUT_ICONS);

        String cost = "§e" + GTValues.VN[PlannerState.minTierFor(recipe.euT)]
            + "§7 | §c"
            + recipe.euT
            + "§7 EU/t | "
            + UiHelpers.formatAmount(recipe.durationTicks / 20.0)
            + "s";
        row.child(
            new ScrollingTextWidget(IKey.str(cost)).expanded()
                .height(12));

        row.child(
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
        return row;
    }

    private static void addStackIcons(Flow row, Iterable<PlannerRecipe.RStack> stacks, int max) {
        int shown = 0;
        int extra = 0;
        for (PlannerRecipe.RStack stack : stacks) {
            if (shown < max) {
                row.child(UiHelpers.stackIcon(stack));
                shown++;
            } else {
                extra++;
            }
        }
        if (extra > 0) {
            row.child(
                IKey.str("§7+" + extra)
                    .asWidget());
        }
    }
}
