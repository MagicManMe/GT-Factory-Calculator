package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.magicmanme.gtfactoryplanner.GTFactoryPlanner;
import com.magicmanme.gtfactoryplanner.client.PlannerState;
import com.magicmanme.gtfactoryplanner.data.RecipeIndex;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;
import com.magicmanme.gtfactoryplanner.model.Plan;
import com.magicmanme.gtfactoryplanner.model.PlanLine;
import com.magicmanme.gtfactoryplanner.solver.Overclocks;
import com.magicmanme.gtfactoryplanner.solver.PlanResult;

import gregtech.api.enums.GTValues;

/**
 * The main planner window: target product + rate, the production-line table
 * (recipe | tier | machines | EU/t), and the raw-input / surplus balance.
 *
 * Structure changes (add/remove lines, change target) rebuild the screen by
 * reopening it; numeric changes (rate, tier) update live through dynamic keys.
 */
public class PlannerScreen extends CustomModularScreen {

    public PlannerScreen() {
        super(GTFactoryPlanner.MODID);
    }

    /** Reopen the planner, rebuilding the whole widget tree from current state. */
    static void reopen() {
        ClientGUI.open(new PlannerScreen());
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        PlannerState.resolve();
        Plan plan = PlannerState.plan;

        Flow root = Flow.column()
            .sizeRel(1f)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.START);

        root.child(
            IKey.str("§lGregTech Factory Planner§r §7(%,d recipes indexed)", RecipeIndex.get().recipes.size())
                .asWidget());

        root.child(targetRow(plan));

        root.child(
            IKey.str("§nProduction lines")
                .asWidget());
        List<IWidget> lineRows = new ArrayList<>();
        for (PlanLine line : plan.lines) {
            lineRows.add(lineRow(plan, line));
        }
        if (lineRows.isEmpty()) {
            lineRows.add(
                IKey.str("§7Add recipes via [+] next to a needed ingredient below.")
                    .asWidget());
        }
        root.child(
            new ListWidget<>().widthRel(1f)
                .expanded()
                .children(lineRows, w -> w));

        root.child(
            IKey.str("§nNeeded inputs / surplus")
                .asWidget());
        root.child(
            new ListWidget<>().widthRel(1f)
                .height(64)
                .children(balanceRows(), w -> w));

        root.child(
            IKey.dynamicKey(
                () -> IKey
                    .str("§6Total average power: %s EU/t", UiHelpers.formatAmount(PlannerState.result.totalAverageEuT)))
                .asWidget());

        return ModularPanel.defaultPanel("planner", 370, 240)
            .padding(7)
            .child(root);
    }

    // ------------------------------------------------------------------ target

    private IWidget targetRow(Plan plan) {
        if (plan.targets.isEmpty()) {
            return new ButtonWidget<>().width(140)
                .height(16)
                .overlay(IKey.str("Set target product..."))
                .onMousePressed(b -> {
                    ClientGUI.open(new ItemPickerScreen(PlannerScreen::setTarget));
                    return true;
                });
        }

        Plan.Target target = plan.targets.get(0);
        return Flow.row()
            .widthRel(1f)
            .height(18)
            .childPadding(4)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(target.key))
            .child(
                new ScrollingTextWidget(IKey.str(target.key.displayName())).expanded()
                    .height(12))
            .child(new TextFieldWidget().value(new DoubleValue.Dynamic(() -> target.amountPerSecond, value -> {
                target.amountPerSecond = Math.max(0, value);
                PlannerState.resolve();
            }))
                .numbersDouble(0, 1_000_000_000)
                .width(56)
                .height(14))
            .child(
                IKey.str("/s")
                    .asWidget())
            .child(
                new ButtonWidget<>().width(46)
                    .height(14)
                    .overlay(IKey.str("Change"))
                    .onMousePressed(b -> {
                        ClientGUI.open(new ItemPickerScreen(PlannerScreen::setTarget));
                        return true;
                    }));
    }

    private static void setTarget(ResourceKey key) {
        double rate = PlannerState.plan.targets.isEmpty() ? 1.0 : PlannerState.plan.targets.get(0).amountPerSecond;
        PlannerState.plan.targets.clear();
        PlannerState.plan.targets.add(new Plan.Target(key, rate));
        reopen();
    }

    // ------------------------------------------------------------------- lines

    private IWidget lineRow(Plan plan, PlanLine line) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(18)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(UiHelpers.primaryOutput(line.recipe)))
            .child(
                new ScrollingTextWidget(IKey.str(UiHelpers.machineName(line.recipe.mapName))).expanded()
                    .height(12))
            .child(
                new ButtonWidget<>().width(32)
                    .height(14)
                    .overlay(IKey.dynamicKey(() -> IKey.str(GTValues.VN[line.machine.voltageTier])))
                    .addTooltipLine("Click: higher tier. Right-click: lower tier.")
                    .onMousePressed(mouseButton -> {
                        cycleTier(line, mouseButton == 0 ? 1 : -1);
                        return true;
                    }));

        if (UiHelpers.usesCoils(line.recipe)) {
            row.child(
                new ButtonWidget<>().width(54)
                    .height(14)
                    .overlay(IKey.dynamicKey(() -> IKey.str(UiHelpers.coilName(line.machine.coilHeat))))
                    .tooltipAutoUpdate(true)
                    .tooltipBuilder(tooltip -> {
                        tooltip.addLine(
                            "Coils: " + UiHelpers.coilName(line.machine.coilHeat)
                                + " ("
                                + Overclocks.totalHeatCapacity(line.machine)
                                + "K total)");
                        tooltip.addLine("Recipe needs " + line.recipe.specialValue + "K");
                        tooltip.addLine("§7Click: better coils. Right-click: worse.");
                        tooltip.addLine("§7+5% EU discount per 900K over; perfect OC per 1800K over.");
                    })
                    .onMousePressed(mouseButton -> {
                        int minHeat = UiHelpers.minCoilHeatFor(line.recipe, line.machine.voltageTier);
                        line.machine.coilHeat = UiHelpers
                            .cycleCoilHeat(line.machine.coilHeat, mouseButton == 0 ? 1 : -1, minHeat);
                        PlannerState.resolve();
                        return true;
                    }));
        }

        return row.child(
            IKey.dynamicKey(() -> IKey.str("§e%s§7 mach.", UiHelpers.formatAmount(machinesOf(line))))
                .asWidget()
                .width(58))
            .child(
                IKey.dynamicKey(() -> IKey.str("§c%s§7 EU/t", UiHelpers.formatAmount(eutOf(line))))
                    .asWidget()
                    .width(68))
            .child(
                new ButtonWidget<>().width(14)
                    .height(14)
                    .overlay(IKey.str("§4x"))
                    .addTooltipLine("Remove this line")
                    .onMousePressed(b -> {
                        plan.lines.remove(line);
                        reopen();
                        return true;
                    }));
    }

    private static void cycleTier(PlanLine line, int delta) {
        int min = PlannerState.minTierFor(line.recipe.euT);
        int max = GTValues.V.length - 1;
        int tier = line.machine.voltageTier + delta;
        if (tier < min) tier = max;
        if (tier > max) tier = min;
        line.machine.voltageTier = tier;
        // Lowering the tier shrinks the +100K/tier heat bonus — keep the recipe runnable.
        if (UiHelpers.usesCoils(line.recipe)) {
            int minHeat = UiHelpers.minCoilHeatFor(line.recipe, line.machine.voltageTier);
            if (line.machine.coilHeat < minHeat) line.machine.coilHeat = minHeat;
        }
        PlannerState.resolve();
    }

    private static double machinesOf(PlanLine line) {
        PlanResult.LineResult result = resultOf(line);
        return result != null ? result.machines : 0;
    }

    private static double eutOf(PlanLine line) {
        PlanResult.LineResult result = resultOf(line);
        return result != null ? result.averageEuT : 0;
    }

    private static PlanResult.LineResult resultOf(PlanLine line) {
        for (PlanResult.LineResult result : PlannerState.result.lines) {
            if (result.line == line) return result;
        }
        return null;
    }

    // ----------------------------------------------------------------- balance

    private List<IWidget> balanceRows() {
        List<IWidget> rows = new ArrayList<>();
        for (Map.Entry<ResourceKey, Double> entry : PlannerState.result.rawInputs.entrySet()) {
            rows.add(balanceRow(entry.getKey(), false));
        }
        for (Map.Entry<ResourceKey, Double> entry : PlannerState.result.surplus.entrySet()) {
            rows.add(balanceRow(entry.getKey(), true));
        }
        if (rows.isEmpty()) {
            rows.add(
                IKey.str("§7Set a target product to see required inputs.")
                    .asWidget());
        }
        return rows;
    }

    private IWidget balanceRow(ResourceKey key, boolean isSurplus) {
        Flow row = Flow.row()
            .widthRel(1f)
            .height(16)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(key))
            .child(
                new ScrollingTextWidget(IKey.str(key.displayName())).expanded()
                    .height(12))
            .child(IKey.dynamicKey(() -> {
                Map<ResourceKey, Double> source = isSurplus ? PlannerState.result.surplus
                    : PlannerState.result.rawInputs;
                Double value = source.get(key);
                String amount = value == null ? "-" : UiHelpers.formatAmount(value);
                return isSurplus ? IKey.str("§a+%s/s", amount) : IKey.str("§f%s/s", amount);
            })
                .asWidget()
                .width(64));

        if (!isSurplus && !RecipeIndex.get()
            .producing(key)
            .isEmpty()) {
            row.child(
                new ButtonWidget<>().width(14)
                    .height(14)
                    .overlay(IKey.str("§2+"))
                    .addTooltipLine(
                        "Add a recipe producing " + key.displayName()
                            .toLowerCase(Locale.ROOT))
                    .onMousePressed(b -> {
                        ClientGUI.open(new RecipePickerScreen(key));
                        return true;
                    }));
        }
        return row;
    }
}
