package dev.rafiattaa.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.rafiattaa.algorithms.PathfindingAlgorithm;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;



public class PathfindCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("pathfind")
                // /pathfind <x> <y> <z>
                .then(CommandManager.argument("destination", Vec3ArgumentType.vec3())
                        .executes(PathfindCommand::executePathfind)
                )

                // /pathfind set <value>
                .then(CommandManager.literal("clear").executes(PathVisualizer::clearPath)
                )
        );
    }

    private static int executePathfind(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendFeedback(() -> Text.literal("Command must be run by a player"), false);
            return 0;
        }

        Vec3d targetVec = Vec3ArgumentType.getVec3(context, "destination");
        BlockPos target = BlockPos.ofFloored(targetVec);

        BlockPos start = player.getBlockPos();
        World world = player.getWorld();
        ServerWorld serverWorld = Objects.requireNonNull(world.getServer()).getWorld(World.OVERWORLD);

        // Check if target is too far
        if (start.getSquaredDistance(target) > 10000) { // 100 block limit
            source.sendFeedback(() -> Text.literal("Target too far! Maximum distance is 100 blocks."), false);
            return 0;
        }

        PathfindingAlgorithm pathfinder = new PathfindingAlgorithm(world);
        List<BlockPos> path = pathfinder.findPath(start, target);

        if (path.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No path found to target location!"), false);
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Path found! Distance: " + path.size() + " blocks"), false);

        // Visualize the path
        PathVisualizer.showPath(serverWorld, world, path, player);
        return 1;
    }
}
