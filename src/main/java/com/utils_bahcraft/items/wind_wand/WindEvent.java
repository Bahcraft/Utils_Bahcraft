package com.utils_bahcraft.items.wind_wand;

import com.utils_bahcraft.network.ModNet;
import com.utils_bahcraft.network.packets.WindImpulseS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.management.ManagementFactory;

@Mod.EventBusSubscriber(modid = "utils_bahcraft", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WindEvent {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag nbt = entity.getPersistentData();
        if (!nbt.contains("WindWandTimer")) return;

        var position = entity.position();
        int timer = nbt.getInt("WindWandTimer");

        // PHASE 1: CHARGING (0 to 14 ticks)
        if (timer < 15) {
            entity.setNoGravity(true);
            entity.setOnGround(false);
            entity.setDeltaMovement(0, 0.2, 0);

            nbt.putInt("WindWandTimer", timer + 1);
            return;
        }

        // PHASE 2: LAUNCH
        if (timer < 300) {
            entity.setNoGravity(false);
            entity.setOnGround(false);

            Vec3 addVel = new Vec3(0, 10, 0);

            // Server applies motion
            entity.setDeltaMovement(addVel);
            entity.hasImpulse = true;
            entity.hurtMarked = true;

            // If it's a player, also apply the same impulse on the physical client
            if (entity instanceof net.minecraft.server.level.ServerPlayer sp) {
                ModNet.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        new WindImpulseS2CPacket(addVel.x, addVel.y, addVel.z)
                );
            }

            boolean isDebugMode = ManagementFactory.getRuntimeMXBean().getInputArguments()
                    .toString().contains("jdwp");

            if (isDebugMode) {
                Vec3 vel = entity.getDeltaMovement();
                double speed = vel.length();

                if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                            Component.literal(
                                    "§e[DEBUG] §fEntity: " + entity.getName().getString() +
                                            " | §bXYZ: " + String.format("%.2f, %.2f, %.2f", vel.x, vel.y, vel.z) +
                                            " | §cTotal Speed: " + String.format("%.4f", speed)
                            ),
                            false
                    );
                }
            }

            // Sound/Particles only on first launch tick
            if (timer == 15) {
                entity.level().playSound(null, position.x, position.y, position.z,
                        SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.NEUTRAL, 100f, 0.5f);

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
            }

            nbt.putInt("WindWandTimer", timer + 1);
            return;
        }

        // PHASE 3: CLEANUP
        nbt.remove("WindWandTimer");
    }
}
