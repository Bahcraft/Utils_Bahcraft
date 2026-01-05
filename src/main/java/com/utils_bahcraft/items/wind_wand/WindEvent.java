package com.utils_bahcraft.items.wind_wand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "utils_bahcraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WindEvent {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag nbt = entity.getPersistentData();
        if (nbt.contains("WindWandTimer")) {
            var position = entity.position();
            int timer = nbt.getInt("WindWandTimer");

            // --- PHASE 1: SLOW RISE (0 to 40 ticks / 2 seconds) ---
            if (timer < 15) {
                entity.setNoGravity(true);
                entity.setOnGround(false);
                entity.setDeltaMovement(0, 0.2, 0);

                nbt.putInt("WindWandTimer", timer + 1);
            }

            // --- PHASE 2: THE LAUNCH  ---
            else {
                entity.setNoGravity(false);
                entity.setOnGround(false);

                entity.setDeltaMovement(0.0, 15, 0.0);
                entity.hasImpulse = true;
                entity.hurtMarked = true;

                entity.level().playSound(null, position.x, position.y, position.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 100f, 0.5f);

                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            1, 0.0, 0.0, 0.0, 0.0);

                    for (int i = 0; i < 10; i++) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                                entity.getX() + (entity.getRandom().nextDouble() - 0.5),
                                entity.getY() + 0.5,
                                entity.getZ() + (entity.getRandom().nextDouble() - 0.5),
                                1, 0.0, 0.1, 0.0, 0.1);
                    }
                }

                nbt.remove("WindWandTimer");
            }
        }
    }
}
