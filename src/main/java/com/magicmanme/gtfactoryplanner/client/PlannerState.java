package com.magicmanme.gtfactoryplanner.client;

import com.magicmanme.gtfactoryplanner.model.Plan;
import com.magicmanme.gtfactoryplanner.solver.PlanResult;
import com.magicmanme.gtfactoryplanner.solver.SequentialSolver;

import gregtech.api.enums.GTValues;

/**
 * Client-side session state: the plan being edited and its latest solve result.
 * Lives for the game session so closing/reopening the planner keeps your work.
 *
 * TODO: persist to NBT / file so plans survive restarts; multiple named plans.
 */
public final class PlannerState {

    public static final Plan plan = new Plan("Plan 1");
    public static PlanResult result = new PlanResult();

    private PlannerState() {}

    /** Re-run the solver against the current plan. Cheap; call after any mutation. */
    public static void resolve() {
        result = SequentialSolver.solve(plan);
    }

    /** Lowest voltage tier (>= LV) whose voltage can run a recipe of the given EU/t. */
    public static int minTierFor(int euT) {
        for (int tier = 1; tier < GTValues.V.length; tier++) {
            if (GTValues.V[tier] >= euT) return tier;
        }
        return GTValues.V.length - 1;
    }
}
