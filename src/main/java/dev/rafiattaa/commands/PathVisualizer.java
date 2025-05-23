package dev.rafiattaa.commands;

import com.mojang.brigadier.context.CommandContext;
import dev.rafiattaa.Pathfinder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PathVisualizer {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final Map<BlockPos, BlockState> originalBlocks = new HashMap<>();
    private static List<BlockPos> path;

    public static void showPath(ServerWorld serverWorld, World world, List<BlockPos> path, ServerPlayerEntity player) {
        PathVisualizer.path = path;
        placePath(serverWorld, world, path, player);

        // Schedule automatic cleanup
        scheduler.schedule(() -> {
            clearPlayerPath(serverWorld);
            Pathfinder.LOGGER.info("Path visualization cleared!");
        }, 120, TimeUnit.SECONDS);
    }

    private static void placePath(ServerWorld serverWorld, World world, List<BlockPos> path, ServerPlayerEntity player) {

        for (int i=0; i<path.size(); i++) {
            BlockPos pathPos = path.get(i);

            // Find best placement for path block
            BlockPos placePos = findBestPlacementPosition(serverWorld, pathPos);
            if (placePos == null) continue;

            // Store original block
            BlockState originalBlock = world.getBlockState(placePos);
            originalBlocks.put(placePos, originalBlock);

            Block blockType;
            if (i == 0){
                blockType = Blocks.LIME_CONCRETE;
            } else if (i == path.size() - 1){
                blockType = Blocks.RED_CONCRETE;
            } else {
                // Gradient of blue blocks along the path
                float progress = (float) i / path.size();
                if (progress < 0.33f) {
                    blockType = Blocks.LIGHT_BLUE_CONCRETE;
                } else if (progress < 0.66f) {
                    blockType = Blocks.BLUE_CONCRETE;
                } else {
                    blockType = Blocks.PURPLE_CONCRETE;
                }
            }

            world.setBlockState(placePos, blockType.getDefaultState());
        }

        player.sendMessage(Text.literal("Path visualization started!"), false);
        player.sendMessage(Text.literal("Autoclear in 2 Minutes!"), false);
    }

    private static BlockPos findBestPlacementPosition(ServerWorld world, BlockPos pathPos) {
        // Strategy 1: Try to place on the ground below the path
        for (int y = pathPos.getY() - 1; y >= pathPos.getY() - 3; y--) {
            BlockPos testPos = new BlockPos(pathPos.getX(), y, pathPos.getZ());

            // Check if this position has solid ground and air above
            if (world.getBlockState(testPos).isOpaque() &&
                    world.getBlockState(testPos.up()).isAir()) {
                return testPos.up(); // Place on top of solid block
            }
        }

        // Strategy 2: If no ground found, place floating at path level
        BlockPos floatingPos = new BlockPos(pathPos.getX(), pathPos.getY(), pathPos.getZ());
        if (world.getBlockState(floatingPos).isAir()) {
            return floatingPos;
        }

        // Strategy 3: Try one block above path level
        BlockPos abovePos = floatingPos.up();
        if (world.getBlockState(abovePos).isAir()) {
            return abovePos;
        }

        return null; // Couldn't find a safe placement
    }

    public static void clearPlayerPath(ServerWorld world) {
        if (path == null) return;

        // Restore original blocks
        for (BlockPos pathPos : path) {
            // Check multiple positions where we might have placed blocks
            for (int yOffset = -3; yOffset <= 2; yOffset++) {
                BlockPos checkPos = new BlockPos(pathPos.getX(), pathPos.getY() + yOffset, pathPos.getZ());

                if (originalBlocks.containsKey(checkPos)) {
                    BlockState originalState = originalBlocks.get(checkPos);
                    world.setBlockState(checkPos, originalState);
                    originalBlocks.remove(checkPos);
                }
            }
        }
        path = null;
    }


    public static int clearPath(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        clearPlayerPath(source.getWorld());
        source.sendFeedback(() -> Text.literal("Successfully cleared path!"), true);
        return 1;
    } // called via command
}
