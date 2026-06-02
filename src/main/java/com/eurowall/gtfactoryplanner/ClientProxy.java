package com.eurowall.gtfactoryplanner;

import com.eurowall.gtfactoryplanner.client.PlannerKeybinds;

import cpw.mods.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        PlannerKeybinds.register();
    }
}
