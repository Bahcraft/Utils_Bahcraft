package com.utils_bahcraft.items;

import com.utils_bahcraft.interfaces.LightningHammerBase;
import com.utils_bahcraft.utils.HammerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SimpleLightningHammerItem extends LightningHammerBase {
    private static final String TAG_MODE = "SIMPLE_HAMMER_MODE";

    public SimpleLightningHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull String getModeTag() {
        return TAG_MODE;
    }

    @Override
    protected @NotNull String getHammerName() {
        return "Martelão da Destruição";
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (level.isClientSide || !HammerUtils.isModeActive(context.getItemInHand(), getModeTag())) {
            return InteractionResult.SUCCESS;
        }

        boolean dropItems = !(player != null && player.isCreative());

        HammerUtils.strikeBlockWithLightning(level, pos, dropItems);

        return InteractionResult.SUCCESS;
    }

}