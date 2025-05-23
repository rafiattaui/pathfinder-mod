package dev.rafiattaa.algorithms;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

public class PathfindingAlgorithm {
    private final World world;
    private final Set<BlockPos> visited = new HashSet<>();
    private final PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
    private final Map<BlockPos, PathNode> allNodes = new HashMap<>();

    public PathfindingAlgorithm(World world) {
        this.world = world;
    }

    public List<BlockPos> findPath(BlockPos start, BlockPos target) {
        // Clear previous search data
        visited.clear();      // Positions we've fully explored
        openSet.clear();      // Positions to explore (priority queue)
        allNodes.clear();     // All nodes we've created

        PathNode startNode = new PathNode(start, 0, heuristic(start, target), null);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;
        final int MAX_ITERATIONS = 5000;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            PathNode current = openSet.poll(); // Get the most promising node

            if (current.pos.equals(target)) {
                return reconstructPath(current); // We found the target!
            }

            visited.add(current.pos); // Mark this position as fully explored

            for (BlockPos neighbor : getNeighbors(current.pos)) {
                if (visited.contains(neighbor)) continue; // Skip already processed positions

                double movementCost = getMovementCost(current.pos, neighbor);
                if (movementCost == Double.MAX_VALUE) continue; // Impassable

                double tentativeGCost = current.gCost + movementCost;

                PathNode neighborNode = allNodes.get(neighbor);
                if (neighborNode == null) {
                    neighborNode = new PathNode(neighbor, tentativeGCost, heuristic(neighbor, target), current);
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeGCost < neighborNode.gCost) {
                    openSet.remove(neighborNode);
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.fCost = tentativeGCost + heuristic(neighbor, target);
                    neighborNode.parent = current;
                    openSet.add(neighborNode);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        // 8 horizontal directions + up/down
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
                {0, 1, 0}, {0, -1, 0}
        };

        for (int[] dir : directions) {
            neighbors.add(pos.add(dir[0], dir[1], dir[2]));
        }

        return neighbors;
    }

    private double getMovementCost(BlockPos from, BlockPos to) {
        // Check if the target position is passable
        if (!isPassable(to)) { // Can't go there
            return Double.MAX_VALUE; // Infinite cost
        }

        // Base movement cost
        double cost = 1.0;

        // Diagonal movement costs more
        if (from.getX() != to.getX() && from.getZ() != to.getZ()) {
            cost = 1.414; // sqrt(2)
        }

        // Vertical movement penalty
        if (from.getY() != to.getY()) {
            cost += Math.abs(to.getY() - from.getY()) * 2.0;
        }

        // Block type penalties
        BlockState blockBelow = world.getBlockState(to.down()); // Get the block below the target position
        Block block = blockBelow.getBlock(); // what kind of block is it?

        if (block == Blocks.WATER || block == Blocks.LAVA) {
            cost += 5.0; // Avoid liquids
        } else if (block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL) {
            cost += 2.0; // Soul blocks slow movement
        } else if (block == Blocks.HONEY_BLOCK) {
            cost += 3.0; // Honey blocks are sticky
        } else if (block == Blocks.ICE || block == Blocks.PACKED_ICE) {
            cost += 1.5; // Ice is slippery
        }

        return cost;
    }

    private boolean isPassable(BlockPos pos) {
        // Check if player can fit (2 blocks high)
        BlockState state1 = world.getBlockState(pos);
        BlockState state2 = world.getBlockState(pos.up());

        // Both blocks must be passable
        return isBlockPassable(state1) && isBlockPassable(state2) &&
                hasFloorSupport(pos.down());
    }

    private boolean isBlockPassable(BlockState state) {
        Block block = state.getBlock();

        // Air and non-solid blocks are passable
        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return true;
        }

        // Some specific passable blocks
        return block == Blocks.WATER || block == Blocks.TALL_GRASS ||
                block == Blocks.GRASS_BLOCK || block == Blocks.FERN ||
                block == Blocks.DEAD_BUSH || block == Blocks.SNOW;
    }

    private boolean hasFloorSupport(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Can't walk on air or liquids (unless it's a valid liquid floor)
        return block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR;// Most blocks can be walked on
    }

    private double heuristic(BlockPos a, BlockPos b) {
        // 3D Manhattan distance with slight preference for direct paths
        return Math.abs(a.getX() - b.getX()) +
                Math.abs(a.getY() - b.getY()) +
                Math.abs(a.getZ() - b.getZ());
    }

    private List<BlockPos> reconstructPath(PathNode node) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = node;

        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    private static class PathNode {
        final BlockPos pos;
        double gCost;
        double fCost;
        PathNode parent;

        PathNode(BlockPos pos, double gCost, double hCost, PathNode parent) {
            this.pos = pos;
            this.gCost = gCost;
            this.fCost = gCost + hCost;
            this.parent = parent;
        }
    }
}
