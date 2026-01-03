package com.utils_bahcraft.entities;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.geomodel.HammerBossModel;
import com.utils_bahcraft.utils.HammerUtils;
import com.utils_bahcraft.utils.goals.BossAttackGoal;
import com.utils_bahcraft.utils.goals.BossChaseGoal;
import mod.azure.azurelib.ai.pathing.AzureNavigation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class HammerBossEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public HammerBossEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(2.5f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)       // 50 Hearts
                .add(Attributes.MOVEMENT_SPEED, 0.20)    // Speed
                .add(Attributes.ATTACK_DAMAGE, 1.0)     // 5 Hearts damage per hit
                .add(Attributes.FOLLOW_RANGE, 64)      // How far it sees players
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    @Override
    protected void registerGoals() {
        // 1. Priority: Don't drown
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, true));

        // Only wander/look when idle
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override public boolean canUse() {
                return HammerBossEntity.this.getTarget() == null && super.canUse();
            }
        });

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, this.getClass(), false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);

        // 1. Create the ItemStack
        ItemStack hammerStack = new ItemStack(UtilsBahCraft.LIGHTNING_HAMMER.get());

        // 2. Set NBT Tag to enable Lightning Mode
        hammerStack.getOrCreateTag().putBoolean("LightningMode", true);

        // 3. Equip the MODIFIED stack
        this.setItemSlot(EquipmentSlot.MAINHAND, hammerStack);

        this.setDropChance(EquipmentSlot.MAINHAND, 1F);

        return result;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // 1. Perform the normal physical attack (damage calculation)
        boolean hasHit = super.doHurtTarget(target);

        // 2. If the attack landed and we are on the server...
        if (hasHit && !this.level().isClientSide) {

            // 3. Spawn Lightning at the target's position
            HammerUtils.spawnLightningAt(level(), target.position(), false);
        }
        return hasHit;
    }

    @Override
    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
        return pSize.height * 0.85F;
    }

    @Override
    public int getHeadRotSpeed() {return 10;}
    @Override
    public int getMaxHeadYRot() {return 30;}

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }


    @Override
    protected PathNavigation createNavigation(Level level) {
        PathNavigation nav = new AzureNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }
}
