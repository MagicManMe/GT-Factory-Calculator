package com.eurowall.gtfactoryplanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * GregTech Factory Planner — an in-game production planner for GT:NH, inspired by
 * Factorio's Helmod / Factory Planner.
 *
 * The mod is read-only with respect to game state: it indexes GregTech's recipe maps
 * client-side and computes machine counts / overclocks / EU-t for user-built plans.
 */
@Mod(
    modid = GTFactoryPlanner.MODID,
    version = Tags.VERSION,
    name = "GregTech Factory Planner",
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:gregtech")
public class GTFactoryPlanner {

    public static final String MODID = "gtfactoryplanner";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "com.eurowall.gtfactoryplanner.ClientProxy",
        serverSide = "com.eurowall.gtfactoryplanner.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
