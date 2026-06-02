package com.eurowall.gtfactoryplanner.solver;

import com.eurowall.gtfactoryplanner.data.PlannerRecipe;
import com.eurowall.gtfactoryplanner.model.MachineConfig;

import gregtech.api.enums.GTValues;
import gregtech.api.util.OverclockCalculator;

/**
 * Thin wrapper around GregTech's own {@link OverclockCalculator} so the planner's
 * overclock math is, by construction, identical to what machines actually do
 * (regular 2/4 OCs, perfect OCs, EBF coil heat discounts and heat overclocks).
 */
public final class Overclocks {

    /** Result of applying a machine configuration to a recipe. */
    public static final class OcResult {

        /** Actual EU/t consumed while running. */
        public final long euT;
        /** Actual duration of one cycle, in ticks (>= 1). */
        public final double durationTicks;

        public OcResult(long euT, double durationTicks) {
            this.euT = euT;
            this.durationTicks = durationTicks;
        }
    }

    private Overclocks() {}

    public static OcResult apply(PlannerRecipe recipe, MachineConfig machine) {
        OverclockCalculator calc = new OverclockCalculator().setRecipeEUt(recipe.euT)
            .setDuration(recipe.durationTicks)
            .setEUt(GTValues.V[machine.voltageTier])
            .setParallel(machine.parallels)
            .setDurationModifier(machine.speedModifier)
            .setEUtDiscount(machine.euModifier);

        if (machine.perfectOC) {
            calc.enablePerfectOC();
        }
        if (machine.machineHeat > 0 && recipe.specialValue > 0) {
            calc.setHeatOC(true)
                .setHeatDiscount(true)
                .setMachineHeat(machine.machineHeat)
                .setRecipeHeat(recipe.specialValue);
        }

        calc.calculate();
        return new OcResult(calc.getConsumption(), Math.max(1, calc.getDuration()));
    }
}
