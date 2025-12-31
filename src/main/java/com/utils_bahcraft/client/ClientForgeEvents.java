package com.utils_bahcraft.client;

import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.utils_bahcraft.UtilsBahCraft;

@Mod.EventBusSubscriber(modid = UtilsBahCraft.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (event.getPlayer().isHolding(UtilsBahCraft.LIGHTNING_HAMMER.get())) {
            event.setNewFovModifier(1.0f);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 1. Validations: Run only on Client, only for the actual User (not other players)
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!event.player.isLocalPlayer()) return;

        var player = event.player;

        // 2. Condition: Holding Hammer + In Air + Pressing Shift (Crouch)
        boolean isHoldingHammer = player.isHolding(UtilsBahCraft.LIGHTNING_HAMMER.get());
        boolean isInAir = !player.onGround() && !player.getAbilities().flying;
        boolean isCrouching = Minecraft.getInstance().options.keyShift.isDown(); // Checks actual key input

        // BOMB Logic
        if (isHoldingHammer && isInAir && isCrouching) {

            Vec3 motion = player.getDeltaMovement();

            if (motion.y > HammerUtils.BOMB_SPEED) {
                player.setDeltaMovement(motion.x, HammerUtils.BOMB_SPEED, motion.z);
            }
        }
    }
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        // Check if the player is using the Hammer
        if (event.getEntity().isUsingItem() && event.getEntity().getUseItem().getItem() == UtilsBahCraft.LIGHTNING_HAMMER.get()) {

            event.getInput().leftImpulse *= 5.0f;
            event.getInput().forwardImpulse *= 5.0f;
        }
    }

}