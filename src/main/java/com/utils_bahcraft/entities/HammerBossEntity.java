package com.utils_bahcraft.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HammerBossEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public HammerBossEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)       // 50 Hearts
                .add(Attributes.MOVEMENT_SPEED, 0.35)    // Speed
                .add(Attributes.ATTACK_DAMAGE, 10.0)     // 5 Hearts damage per hit
                .add(Attributes.FOLLOW_RANGE, 32.0)      // How far it sees players
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
