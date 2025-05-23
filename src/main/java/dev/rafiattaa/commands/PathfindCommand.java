package dev.rafiattaa.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import static dev.rafiattaa.entities.GuideManager.spawnGuide;
import java.util.Objects;

public class PathfindCommand {
    private int pathfindAlgorithm = 1; // 1 = Djikstra, 2 = A-Star


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("pathfind"). // /pathfind
                then(CommandManager.argument("destination", Vec3ArgumentType.vec3()). // <x> <y> <z>
                        executes(PathfindCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PosArgument destination = context.getArgument("destination", PosArgument.class);
        Vec3d destinationPos = destination.toAbsolutePos(context.getSource());
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        ServerWorld world = player.getServerWorld();

        BlockPos playerPos = Objects.requireNonNull(player.getBlockPos());
        context.getSource().sendFeedback(()-> Text.literal("You are at " + playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ() + "."), true);
        context.getSource().sendFeedback(()-> Text.literal("Pathfinding to " + destinationPos.getX() + ", " + destinationPos.getY() + ", " + destinationPos.getZ() + "."), true);

        spawnGuide((int) destinationPos.getX(), (int) destinationPos.getY(), (int) destinationPos.getZ(), world);

        return 1;
    }

    public int getPathfindAlgorithm() {
        return pathfindAlgorithm;
    }

    public void setPathfindAlgorithm(int pathfindAlgorithm) {
        this.pathfindAlgorithm = pathfindAlgorithm;
    }
}
