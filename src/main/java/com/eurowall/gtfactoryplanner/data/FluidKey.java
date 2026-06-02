package com.eurowall.gtfactoryplanner.data;

import net.minecraftforge.fluids.FluidStack;

/**
 * Fluid identity, keyed by registry name. Amounts are in millibuckets (L).
 */
public final class FluidKey implements ResourceKey {

    public final String fluidName;
    private final String localizedName;

    private FluidKey(String fluidName, String localizedName) {
        this.fluidName = fluidName;
        this.localizedName = localizedName;
    }

    public static FluidKey of(FluidStack stack) {
        return new FluidKey(
            stack.getFluid()
                .getName(),
            stack.getLocalizedName());
    }

    @Override
    public String displayName() {
        return localizedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FluidKey)) return false;
        return fluidName.equals(((FluidKey) o).fluidName);
    }

    @Override
    public int hashCode() {
        return fluidName.hashCode();
    }

    @Override
    public String toString() {
        return "fluid:" + fluidName;
    }
}
