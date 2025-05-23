package dev.rafiattaa.util;

import dev.rafiattaa.commands.PathVisualizer;
import dev.rafiattaa.commands.PathfindCommand;
import dev.rafiattaa.entities.GuideManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public class ModCommandRegistration {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(PathfindCommand::register);
    }
}
