package com.eurowall.gtfactoryplanner.model;

import java.util.ArrayList;
import java.util.List;

import com.eurowall.gtfactoryplanner.data.ResourceKey;

/**
 * A production plan: target output rates plus an ordered list of recipe lines.
 * Line order matters for the sequential solver (demand propagates top to bottom,
 * exactly like Factory Planner's default engine).
 *
 * TODO: nested groups (Factory Planner "subfloors"), NBT (de)serialization for
 * persistence on the player, and an exchange-string export.
 */
public final class Plan {

    /** A desired output: resource + rate in units per second. */
    public static final class Target {

        public final ResourceKey key;
        public double amountPerSecond;

        public Target(ResourceKey key, double amountPerSecond) {
            this.key = key;
            this.amountPerSecond = amountPerSecond;
        }
    }

    public String name;
    public final List<Target> targets = new ArrayList<>();
    public final List<PlanLine> lines = new ArrayList<>();

    public Plan(String name) {
        this.name = name;
    }
}
