package com.eurowall.gtfactoryplanner.solver;

import java.util.LinkedHashMap;
import java.util.Map;

import com.eurowall.gtfactoryplanner.data.PlannerRecipe;
import com.eurowall.gtfactoryplanner.data.ResourceKey;
import com.eurowall.gtfactoryplanner.model.Plan;
import com.eurowall.gtfactoryplanner.model.PlanLine;

/**
 * Default solver: top-down demand propagation, modeled on Factory Planner's
 * sequential engine. Targets seed a demand map; each line in order satisfies as
 * much demand for its outputs as it can, adding its own inputs as new demand.
 *
 * Whatever demand is left at the end is the plan's raw ingredient list; any
 * negative demand is surplus/byproduct.
 *
 * Limitations (by design, for now): no loops, no balancing between multiple
 * producers of the same item. Those are the job of the future matrix/LP solver
 * (Factory Planner's two-tier approach).
 */
public final class SequentialSolver {

    private SequentialSolver() {}

    public static PlanResult solve(Plan plan) {
        PlanResult result = new PlanResult();

        // Demand in units per second. Positive = needed, negative = surplus.
        Map<ResourceKey, Double> demand = new LinkedHashMap<>();
        for (Plan.Target target : plan.targets) {
            merge(demand, target.key, target.amountPerSecond);
        }

        for (PlanLine line : plan.lines) {
            PlannerRecipe recipe = line.recipe;

            // Runs/second this line must do: enough to satisfy the most-demanding
            // of its outputs (other outputs then overproduce -> surplus credit).
            double runsPerSecond = 0;
            for (PlannerRecipe.RStack out : recipe.allOutputs()) {
                if (out.amountPerRun <= 0) continue;
                Double needed = demand.get(out.key);
                if (needed != null && needed > 0) {
                    runsPerSecond = Math.max(runsPerSecond, needed / out.amountPerRun);
                }
            }

            if (runsPerSecond <= 0) {
                result.lines.add(new PlanResult.LineResult(line, 0, 0, 0, 0, recipe.durationTicks));
                continue;
            }

            // Machine count via GT's own overclock math.
            Overclocks.OcResult oc = Overclocks.apply(recipe, line.machine);
            double cyclesPerSecondPerMachine = 20.0 / oc.durationTicks;
            double runsPerSecondPerMachine = cyclesPerSecondPerMachine * line.machine.parallels;
            double machines = runsPerSecond / runsPerSecondPerMachine;
            // Fractional machines = duty cycle, so average power scales linearly.
            double averageEuT = machines * oc.euT;

            // Outputs reduce demand (possibly past zero -> surplus)...
            for (PlannerRecipe.RStack out : recipe.allOutputs()) {
                if (out.amountPerRun <= 0) continue;
                merge(demand, out.key, -runsPerSecond * out.amountPerRun);
            }
            // ...and inputs create new demand for lines below (amount 0 inputs are
            // non-consumed: circuits, catalysts).
            for (PlannerRecipe.RStack in : recipe.allInputs()) {
                if (in.amountPerRun <= 0) continue;
                merge(demand, in.key, runsPerSecond * in.amountPerRun);
            }

            result.lines
                .add(new PlanResult.LineResult(line, runsPerSecond, machines, oc.euT, averageEuT, oc.durationTicks));
            result.totalAverageEuT += averageEuT;
        }

        // Split the residual demand map into raw inputs vs surplus.
        for (Map.Entry<ResourceKey, Double> entry : demand.entrySet()) {
            double value = entry.getValue();
            if (value > EPSILON) {
                result.rawInputs.put(entry.getKey(), value);
            } else if (value < -EPSILON) {
                result.surplus.put(entry.getKey(), -value);
            }
        }
        return result;
    }

    private static final double EPSILON = 1e-9;

    private static void merge(Map<ResourceKey, Double> map, ResourceKey key, double delta) {
        map.merge(key, delta, Double::sum);
    }
}
