package de.maexle.skyblockcounter;

import de.maexle.skyblockcounter.command.SkyblockCounterCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SkyblockCounterClient implements ClientModInitializer {

    private static KeyBinding toggleHudKey;

    @Override
    public void onInitializeClient() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyblockcounter.togglehud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHudKey.wasPressed()) {
                boolean currentVisible = SkyblockCounterService.isGuiVisible();
                SkyblockCounterService.setGuiVisible(!currentVisible);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            SkyblockCounterCommand.register(dispatcher);
        });

        SkyblockCounterService serviceInstance = new SkyblockCounterService();
        serviceInstance.startEventTracking();
    }
}
