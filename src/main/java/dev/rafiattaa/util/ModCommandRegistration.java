package dev.rafiattaa.util;

import dev.rafiattaa.commands.PathfindCommand;
import dev.rafiattaa.entities.GuideManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ModCommandRegistration {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(PathfindCommand::register);
        ServerTickEvents.END_SERVER_TICK.register(GuideManager::spawnParticles); // At the end of every server tick,
    }
}
