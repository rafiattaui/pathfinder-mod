package dev.rafiattaa.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import dev.rafiattaa.Pathfinder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuideManager {

    private static final Set<ArmorStandEntity> guideEntities = Collections.synchronizedSet(new HashSet<>());

    public static void spawnGuide(int x, int y, int z, World world) {
        ArmorStandEntity guide = new ArmorStandEntity(world, x,y,z);
        guide.setInvisible(false);
        guide.setNoGravity(true);
        guide.setInvulnerable(true);
        guide.setSilent(true);
        guide.setPos(x + 0.5, y, z + 0.5);
        guide.addCommandTag("guide");

        world.spawnEntity(guide);
        guideEntities.add(guide);
    }

    public static void spawnParticles(MinecraftServer server){
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return;

        guideEntities.removeIf(Entity::isRemoved);

        Pathfinder.LOGGER.info("Spawned guide particles!");

        guideEntities.forEach(guide -> {
            world.spawnParticles(ParticleTypes.END_ROD, guide.getX(), guide.getY() + 0.5, guide.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
        });
    }
}
