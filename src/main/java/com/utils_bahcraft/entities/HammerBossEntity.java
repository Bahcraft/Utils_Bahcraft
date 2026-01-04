package com.utils_bahcraft.entities;

import com.utils_bahcraft.UtilsBahCraft;
import com.utils_bahcraft.utils.HammerUtils;
import com.utils_bahcraft.utils.goals.HammerAttackGoal;
import mod.azure.azurelib.ai.pathing.AzureNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import com.utils_bahcraft.utils.CommonUtils;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.item.Item;
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
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    public HammerBossEntity(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setMaxUpStep(2.5f);

        this.bossEvent.setDarkenScreen(true);
        this.bossEvent.setPlayBossMusic(true);
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

        this.goalSelector.addGoal(2, new HammerAttackGoal(this, 1.05D, true, 4.0F));

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

    /**
     * Custom boss death sequence (Forge 1.20.1).
     *
     * Timeline (dragon-inspired):
     * - Ticks 1..200: rising + explosions and "light" particles
     * - Tick 1: triggers the Ender Dragon death global level event (1028)
     * - Ticks 180..200: heavier explosion emitters
     * - Tick 200: spawns guaranteed loot + XP + final sound, then removes the entity
     */
    @Override
    protected void tickDeath() {
        ++this.deathTime;

        // Keep the boss stable during death
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);

        // Rise smoothly (dragon-like)
        this.move(MoverType.SELF, new net.minecraft.world.phys.Vec3(0.0D, 0.10D, 0.0D));

        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {

            // Dragon death global effect (sound/particles event broadcast)
            if (this.deathTime == 1 && !this.isSilent()) {
                server.globalLevelEvent(1028, this.blockPosition(), 0);
            }

            // Constant "explosion" particles around the body during the whole sequence
            {
                float xOff = (this.random.nextFloat() - 0.5F) * 8.0F;
                float yOff = (this.random.nextFloat() - 0.5F) * 4.0F;
                float zOff = (this.random.nextFloat() - 0.5F) * 8.0F;

                server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        this.getX() + xOff,
                        this.getY() + 2.0D + yOff,
                        this.getZ() + zOff,
                        1,
                        0.0D, 0.0D, 0.0D,
                        0.0D);
            }

            // "Light / shatter" look: rising END_ROD + PORTAL shimmer (server-side so everyone sees it)
            if (this.deathTime >= 20 && this.deathTime < 200 && (this.deathTime % 2 == 0)) {
                double spread = 0.6D + (this.deathTime / 60.0D);

                for (int i = 0; i < 8; i++) {
                    double dx = (this.random.nextDouble() - 0.5D) * spread;
                    double dy = (this.random.nextDouble() - 0.5D) * spread;
                    double dz = (this.random.nextDouble() - 0.5D) * spread;

                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            this.getX() + dx,
                            this.getY() + 2.0D + dy,
                            this.getZ() + dz,
                            1,
                            0.0D, 0.25D, 0.0D,
                            0.02D);

                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            this.getX() + dx,
                            this.getY() + 2.2D + dy,
                            this.getZ() + dz,
                            1,
                            0.0D, 0.15D, 0.0D,
                            0.02D);
                }
            }

            // Final second: extra emitters (big "shatter" burst)
            if (this.deathTime >= 180 && this.deathTime <= 200) {
                float xOff = (this.random.nextFloat() - 0.5F) * 8.0F;
                float yOff = (this.random.nextFloat() - 0.5F) * 4.0F;
                float zOff = (this.random.nextFloat() - 0.5F) * 8.0F;

                server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                        this.getX() + xOff,
                        this.getY() + 2.0D + yOff,
                        this.getZ() + zOff,
                        1,
                        0.0D, 0.0D, 0.0D,
                        0.0D);
            }

            // Drop loot once at the end
            if (this.deathTime >= 200) {

                if (!this.getPersistentData().getBoolean("bahcraft_loot_dropped")) {
                    this.getPersistentData().putBoolean("bahcraft_loot_dropped", true);

                    // Create the hammer
                    ItemStack hammerStack = new ItemStack(UtilsBahCraft.LIGHTNING_HAMMER.get());
                    hammerStack.getOrCreateTag().putBoolean("LightningMode", false);
                     CommonUtils.spawnSafeLoot(level(), hammerStack, this.getX(), this.getY(), this.getZ());

                    // Massive XP drop
                    this.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(
                            this.level(), this.getX(), this.getY(), this.getZ(), 2000));

                    // Final sound + final burst
                    this.level().playSound(null, this.blockPosition(),
                            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                            net.minecraft.sounds.SoundSource.HOSTILE,
                            4.0F, 1.0F);

                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                            this.getX(), this.getY() + 2.0D, this.getZ(),
                            6,
                            1.0D, 1.0D, 1.0D,
                            0.0D);
                }

                // Remove the boss
                BlockPos center = this.blockPosition();
                CommonUtils.spawnGroundLightning(server, center, 30, 15);
                HammerUtils.spawnLightningAt(this.level(), center.getCenter(), true);
                this.remove(RemovalReason.KILLED);
                this.remove(RemovalReason.KILLED);
            }
        }
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

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void aiStep() {
        if (this.deathTime > 0) {
            this.setNoGravity(true);
        }
        super.aiStep();
    }
}
