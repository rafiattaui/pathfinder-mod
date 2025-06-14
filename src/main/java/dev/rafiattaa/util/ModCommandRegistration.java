package dev.rafiattaa.util;

import dev.rafiattaa.commands.PathfindCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class ModCommandRegistration {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(PathfindCommand::register);
    }
}
