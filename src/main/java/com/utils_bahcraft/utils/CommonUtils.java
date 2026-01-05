package com.utils_bahcraft.utils;

import com.utils_bahcraft.UtilsBahCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CommonUtils {
    public static void spawnSafeLoot(Level level, ItemStack stack, double x, double y, double z) {
        // Spawn the Item Entity
        net.minecraft.world.entity.item.ItemEntity itemEntity =
                new net.minecraft.world.entity.item.ItemEntity(level, x, y + 1, z, stack);

        // --- CRITICAL FIX ---
        // Make it invulnerable so Lightning/Explosions cannot destroy it
        itemEntity.setInvulnerable(true);

        // Make it float a bit so it doesn't clip into the ground immediately
        itemEntity.setDeltaMovement(0, 0.2, 0);

        // Force the item specifically to not burn (Double safety)
        itemEntity.setUnlimitedLifetime(); // Optional: Makes it never despawn

        level.addFreshEntity(itemEntity);
    }

    public static void spawnGroundLightning(ServerLevel level, BlockPos center, int count, int radiusBlocks) {
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * (Math.PI * 2.0);
            double r = Math.sqrt(level.random.nextDouble()) * radiusBlocks;

            int dx = Mth.floor(Math.cos(angle) * r);
            int dz = Mth.floor(Math.sin(angle) * r);

            BlockPos xz = center.offset(dx, 0, dz);

            BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
            var pos = new Vec3(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D);

            HammerUtils.spawnLightningAt(level, pos, true);
        }

        HammerUtils.spawnLightningAt(level, center.getCenter(), true);
    }

}
