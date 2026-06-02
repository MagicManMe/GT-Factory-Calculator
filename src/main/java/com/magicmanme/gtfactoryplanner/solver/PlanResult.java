package com.magicmanme.gtfactoryplanner.solver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.magicmanme.gtfactoryplanner.data.ResourceKey;
import com.magicmanme.gtfactoryplanner.model.PlanLine;

/**
 * Solved plan: per-line machine counts and rates, plus the plan-level balance
 * (raw ingredients still needed, surplus byproducts, total average power).
 */
public final class PlanResult {

    /** Solved values for one plan line. */
    public static final class LineResult {

        public final PlanLine line;
        /** Recipe runs per second demanded of this line (0 = line is idle/unused). */
        public final double runsPerSecond;
        /** Fractional machine count needed (round up to build; fraction = duty cycle). */
        public final double machines;
        /** EU/t of a single machine while running (after OC). */
        public final long euTPerMachine;
        /** Average EU/t of the whole line (machines x per-machine, duty-cycle adjusted). */
        public final double averageEuT;
        /** Cycle duration after overclocking, in ticks. */
        public final double durationTicks;

        public LineResult(PlanLine line, double runsPerSecond, double machines, long euTPerMachine, double averageEuT,
            double durationTicks) {
            this.line = line;
            this.runsPerSecond = runsPerSecond;
            this.machines = machines;
            this.euTPerMachine = euTPerMachine;
            this.averageEuT = averageEuT;
            this.durationTicks = durationTicks;
        }
    }

    public final List<LineResult> lines = new ArrayList<>();
    /** Resources still demanded after all lines ran = raw inputs (units/second). */
    public final Map<ResourceKey, Double> rawInputs = new LinkedHashMap<>();
    /** Overproduced resources = byproducts/surplus (units/second, positive values). */
    public final Map<ResourceKey, Double> surplus = new LinkedHashMap<>();
    /** Total average power draw of the plan in EU/t. */
    public double totalAverageEuT;
}
