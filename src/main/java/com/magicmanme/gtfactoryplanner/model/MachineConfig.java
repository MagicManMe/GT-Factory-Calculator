package com.magicmanme.gtfactoryplanner.model;

/**
 * How a recipe line is run: voltage tier, parallels, OC behavior, coil heat.
 * This is the GT equivalent of Factorio's "machine + modules + beacons" choice.
 */
public final class MachineConfig {

    /** Index into {@code GTValues.V} (0 = ULV, 1 = LV, ...). */
    public int voltageTier = 1;
    /** Parallels per machine (1 for single-blocks; multiblocks vary, e.g. MEBF 256). */
    public int parallels = 1;
    /** Perfect overclocks (÷4 duration per OC), e.g. Large Chemical Reactor. */
    public boolean perfectOC = false;
    /**
     * Base heat of the installed coils in K (e.g. Cupronickel 1801); 0 = machine
     * has no coils/heat mechanic. Total heat capacity follows the EBF formula:
     * {@code coilHeat + 100 * (voltageTier - 2)}.
     */
    public int coilHeat = 0;
    /** Speed multiplier from the multiblock (e.g. 2.5 for some GT++ multis). 1 = none. */
    public double speedModifier = 1.0;
    /** EU/t discount multiplier (e.g. 0.9 = 10% less power). 1 = none. */
    public double euModifier = 1.0;

    public MachineConfig() {}

    public MachineConfig(int voltageTier) {
        this.voltageTier = voltageTier;
    }

    public MachineConfig copy() {
        MachineConfig c = new MachineConfig(voltageTier);
        c.parallels = parallels;
        c.perfectOC = perfectOC;
        c.coilHeat = coilHeat;
        c.speedModifier = speedModifier;
        c.euModifier = euModifier;
        return c;
    }
}
