package com.eurowall.gtfactoryplanner;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Include recipes flagged hidden by GregTech (e.g. some scripted/internal maps). */
    public static boolean indexHiddenRecipes = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        indexHiddenRecipes = configuration.getBoolean(
            "indexHiddenRecipes",
            Configuration.CATEGORY_GENERAL,
            indexHiddenRecipes,
            "Whether recipes marked hidden by GregTech are included in the planner's recipe index.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
