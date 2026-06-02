package com.magicmanme.gtfactoryplanner.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.magicmanme.gtfactoryplanner.client.gui.GuiPlanner;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/**
 * Registers the "open planner" keybind (default P, rebindable in Controls) and
 * opens the planner GUI when pressed in-world.
 */
public final class PlannerKeybinds {

    public static final KeyBinding OPEN_PLANNER = new KeyBinding(
        "key.gtfactoryplanner.open",
        Keyboard.KEY_P,
        "key.categories.gtfactoryplanner");

    private PlannerKeybinds() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_PLANNER);
        FMLCommonHandler.instance()
            .bus()
            .register(new PlannerKeybinds.Handler());
    }

    public static class Handler {

        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            if (!OPEN_PLANNER.isPressed()) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen == null && mc.theWorld != null) {
                mc.displayGuiScreen(new GuiPlanner());
            }
        }
    }
}
