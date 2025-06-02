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

                double tentativeGCost = current.gCost + movementCost; // calculate cost of neighbour

                PathNode neighborNode = allNodes.get(neighbor);  // we have never encountered this neighbour, add to explore list
                if (neighborNode == null) {
                    neighborNode = new PathNode(neighbor, tentativeGCost, heuristic(neighbor, target), current);
                    allNodes.put(neighbor, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeGCost < neighborNode.gCost) { // we've been to this neighbour before but we found a better route to reach it
                    openSet.remove(neighborNode); // remove and re-add so the queue can re-sort it
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

        // Cardinal directions (horizontal only)
        int[][] cardinal = {{1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1}};
        // Diagonal directions (horizontal only)
        int[][] diagonal = {{1,0,1}, {-1,0,1}, {1,0,-1}, {-1,0,-1}};

        // Add horizontal movements
        for (int[] dir : cardinal) {
            neighbors.add(pos.add(dir[0], dir[1], dir[2]));
        }
        for (int[] dir : diagonal) {
            neighbors.add(pos.add(dir[0], dir[1], dir[2]));
        }

        // Add simple vertical movements (straight up/down only)
        neighbors.add(pos.up());
        neighbors.add(pos.down());

        // Add step-up movements (1 block up + horizontal)
        for (int[] dir : cardinal) {
            neighbors.add(pos.add(dir[0], 1, dir[2]));
        }

        // Add step-down movements (1-3 blocks down + horizontal)
        for (int[] dir : cardinal) {
            for (int dropHeight = 1; dropHeight <= 3; dropHeight++) {
                neighbors.add(pos.add(dir[0], -dropHeight, dir[2]));
            }
        }

        return neighbors;
    }

    private double getMovementCost(BlockPos from, BlockPos to) {
        // Check if the target position is passable
        if (!canMoveTo(from, to)) { // Can't go there
            return Double.MAX_VALUE; // Infinite cost
        }

        // Base movement cost
        double cost = 1.0;

        // Diagonal movement costs more
        if (from.getX() != to.getX() && from.getZ() != to.getZ()) {
            cost = 1.414; // sqrt(2)
        }

        // Vertical movement penalty
        int diff = to.getY() - from.getY();
        if (diff > 0) {
            cost += diff * 2.0; // going up
        } else if (diff < 0) {
            cost += Math.abs(diff) * 1.5; // going down (lower cost)
        }

        // Block type penalties
        BlockState blockBelow = world.getBlockState(to.down()); // Get the block below the target position
        Block block = blockBelow.getBlock(); // what kind of block is it?

        if (block == Blocks.WATER) {
            cost += 5.0; // Avoid liquids
        } else if (block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL) {
            cost += 2.0; // Soul blocks slow movement
        } else if (block == Blocks.HONEY_BLOCK) {
            cost += 3.0; // Honey blocks are sticky
        } else if (block == Blocks.ICE || block == Blocks.PACKED_ICE) {
            cost += 1.5; // Ice is slippery
        } else if (block == Blocks.LAVA) {
            cost += Double.MAX_VALUE;
        }

        return cost;
    }

    private boolean canMoveTo(BlockPos from, BlockPos to) {
        if (!isStandablePosition(to)) {
            return false;
        }

        int heightDiff = to.getY() - from.getY();

        // Strict height constraints
        if (heightDiff > 1 || heightDiff < -3) {
            return false;
        }

        // Check movement type
        if (isDiagonalMove(from, to)) {
            // Diagonal movement only allowed on same level
            if (heightDiff != 0) {
                return false;
            }
            return canMoveDiagonally(from, to);
        } else if (heightDiff != 0) {
            // Vertical movement
            return canMoveVertically(from, to, heightDiff);
        } else {
            // Horizontal movement on same level
            return canMoveHorizontally(to);
        }
    }

    private boolean canMoveVertically(BlockPos from, BlockPos to, int heightDiff) {
        if (heightDiff > 0) {
            // Moving up - check step up
            return canStepUp(from, to);
        } else {
            // Moving down - check fall
            return canFallDown(from, to, Math.abs(heightDiff));
        }
    }

    private boolean canStepUp(BlockPos from, BlockPos to) {
        // Only allow 1 block step up
        if (to.getY() - from.getY() != 1) {
            return false;
        }

        // Check if there's clearance for the step
        BlockPos stepPos = new BlockPos(to.getX(), from.getY() + 1, to.getZ());
        return isBlockPassable(world.getBlockState(stepPos)) &&
                isBlockPassable(world.getBlockState(stepPos.up()));
    }

    private boolean canFallDown(BlockPos from, BlockPos to, int fallDistance) {
        if (fallDistance > 3) {
            return false;
        }

        // Check that all blocks in the fall path are passable
        for (int y = from.getY(); y > to.getY(); y--) {
            BlockPos checkPos = new BlockPos(to.getX(), y, to.getZ());
            if (!isBlockPassable(world.getBlockState(checkPos))) {
                return false;
            }
        }

        return true;
    }

    private boolean canMoveHorizontally(BlockPos to) {
        // Same level horizontal movement - just check passability
        return isStandablePosition(to);
    }

    private boolean isDiagonalMove(BlockPos from, BlockPos to) {
        return from.getX() != to.getX() && from.getZ() != to.getZ();
    }

    private boolean canMoveDiagonally(BlockPos from, BlockPos to) {
        // Only allow diagonal movement on same Y level
        if (from.getY() != to.getY()) {
            return false;
        }

        // Check both intermediate positions to prevent corner cutting
        BlockPos inter1 = new BlockPos(to.getX(), from.getY(), from.getZ());
        BlockPos inter2 = new BlockPos(from.getX(), from.getY(), to.getZ());

        // At least one path must be valid
        boolean path1Valid = isStandablePosition(inter1);
        boolean path2Valid = isStandablePosition(inter2);

        return path1Valid || path2Valid;
    }

    private boolean isStandablePosition(BlockPos pos) {
        BlockState floorState = world.getBlockState(pos.down());
        BlockState bodyState = world.getBlockState(pos);
        BlockState headState = world.getBlockState(pos.up());

        return isSolidFloor(floorState) &&
                isBlockPassable(bodyState) &&
                isBlockPassable(headState);
    }

    private boolean isBlockPassable(BlockState state) {
        Block block = state.getBlock();

        // Air and non-solid blocks are passable
        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
            return true;
        }

        // Some specific passable blocks
        return block == Blocks.WATER || block == Blocks.TALL_GRASS || block == Blocks.FERN ||
                block == Blocks.DEAD_BUSH || block == Blocks.SNOW;
    }

    private boolean isSolidFloor(BlockState state) {
        Block block = state.getBlock();

        // Air and liquids are not solid floors
        if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR ||
                block == Blocks.WATER || block == Blocks.LAVA) {
            return false;
        }

        // Most other blocks can be walked on
        return true;
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