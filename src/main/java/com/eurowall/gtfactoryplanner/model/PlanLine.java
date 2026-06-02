package com.eurowall.gtfactoryplanner.model;

import com.eurowall.gtfactoryplanner.data.PlannerRecipe;

/**
 * One row of a plan: a user-chosen recipe and the machine configuration to run it
 * with. Recipes are always chosen explicitly by the user (never auto-picked) —
 * GT items routinely have many recipes, and auto-picking is what breaks generic
 * calculators on GregTech.
 */
public final class PlanLine {

    public PlannerRecipe recipe;
    public MachineConfig machine;

    public PlanLine(PlannerRecipe recipe, MachineConfig machine) {
        this.recipe = recipe;
        this.machine = machine;
    }
}
