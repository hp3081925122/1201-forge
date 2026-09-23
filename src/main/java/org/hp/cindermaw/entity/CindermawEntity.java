package org.hp.cindermaw.entity;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import org.hp.cindermaw.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

// 熔渊炉心的基础实体，当前先提供可召唤、可索敌和可近战的 Boss 外壳。
public final class CindermawEntity extends Monster implements GeoEntity {
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cindermaw.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.cindermaw.attack");
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation VOLLEY = RawAnimation.begin().thenPlay("animation.cindermaw.volley");
    private static final EntityDataAccessor<Integer> VOLLEY_AGE = SynchedEntityData.defineId(CindermawEntity.class, EntityDataSerializers.INT);
    private int volleyCooldown = 60;
    private UUID volleyTarget;
    private static final RawAnimation LEAP = RawAnimation.begin().thenPlay("animation.cindermaw.leap").thenLoop("animation.cindermaw.leap_air");
    private static final RawAnimation LAND = RawAnimation.begin().thenPlay("animation.cindermaw.leap_land");
    private int leapAge = -1;
    private int leapCooldown = 30;
    private int landingRecovery;
    private boolean leapAirborne;
    private Vec3 leapHorizontal = Vec3.ZERO;
    private UUID leapTarget;
    private int attackTicks;
    private UUID attackTarget;

    public CindermawEntity(EntityType<? extends CindermawEntity> type, Level level) {
        super(type, level);
        xpReward = 25;
        setMaxUpStep(1.0F);
    }

    // 喷射阶段同步到客户端，驱动喷口特效。
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VOLLEY_AGE, -1);
    }
    public int getVolleyAge() { return entityData.get(VOLLEY_AGE); }

    // 发射点对应前倾十二度后的三个火山顶口，左右顺序与骨骼一致。
    public Vec3 ventOrigin(int vent) {
        double pitch = Math.toRadians(12);
        double x = vent == 0 ? 8 : vent == 2 ? -8 : 0;
        double y = (vent == 1 ? 50 : 44) - 17;
        double z = 6;
        double ry = y * Math.cos(pitch) - z * Math.sin(pitch) + 16;
        double rz = y * Math.sin(pitch) + z * Math.cos(pitch) + 1;
        double yaw = Math.toRadians(getYRot());
        return position().add((-x * Math.cos(yaw) + rz * Math.sin(yaw)) / 16, ry / 16,
                (-x * Math.sin(yaw) - rz * Math.cos(yaw)) / 16);
    }

    // 初始数值只用于让模型可以在开发世界中稳定测试，技能伤害后续单独平衡。
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    // 使用原版目标与近战任务，保持实体能直接被刷怪蛋召唤并进行基础战斗测试。
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            // 蓄力、命中与收势期间停止寻路，避免移动动画和攻击动画互相拉扯。
            @Override
            public void tick() {
                if (attackTicks > 0 || getVolleyAge() >= 0 || leapAge >= 0 || landingRecovery > 0) getNavigation().stop();
                else super.tick();
            }

            // 巨型双拳在三点五格内即可开始重击蓄力。
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 3.5D * 3.5D;
            }
        });
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // 攻击和索敌时保持头、身体与实体朝向同步，保证炉心正面始终对着目标。
    @Override
    public void tick() {
        super.tick();
        // 服务端持续同步血量和名称，治疗、受伤与命名牌修改都会立即反映到血条。
        if (!level().isClientSide) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
            bossEvent.setName(getDisplayName());
        }
        var target = getTarget();
        if (target != null && target.isAlive()) {
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            float yaw = (float) (Math.toDegrees(Math.atan2(getX() - target.getX(), target.getZ() - getZ())));
            setYRot(yaw);
            setYHeadRot(yaw);
            setYBodyRot(yaw);
        }
        // 死亡后立即关闭尚未发射的技能，已发射火球独立完成消散。
        if (!isAlive()) {
            if (!level().isClientSide) entityData.set(VOLLEY_AGE, -1);
            return;
        }
        // 跳砸只在有地面支撑且目标处于四至十六格时发动，避免贴脸连续起跳。
        if (!level().isClientSide) {
            if (leapCooldown > 0) leapCooldown--;
            if (landingRecovery > 0) { landingRecovery--; getNavigation().stop(); }
            if (leapAge < 0 && landingRecovery == 0 && getVolleyAge() < 0 && attackTicks == 0 && leapCooldown == 0
                    && onGround() && target != null && target.isAlive() && distanceToSqr(target) >= 16 && distanceToSqr(target) <= 256
                    && Math.abs(target.getY()-getY()) <= 4 && hasLineOfSight(target)) {
                leapAge = 0; leapAirborne = false; leapTarget = target.getUUID();
                triggerAnim("movement", "leap"); getNavigation().stop();
            }
            if (leapAge >= 0) {
                getNavigation().stop();
                // 蓄力十刻后只施加一次跃起速度，后续交给原版重力和方块碰撞。
                if (leapAge == 10) {
                    if (target == null || !target.isAlive() || !target.getUUID().equals(leapTarget)) {
                        leapAge = -1; leapCooldown = 60;
                        triggerAnim("movement", "land");
                    } else {
                        Vec3 offset = target.position().subtract(position());
                        leapHorizontal = new Vec3(offset.x / 23.0, 0, offset.z / 23.0);
                        setDeltaMovement(leapHorizontal.x, 0.92 + Math.max(-0.12, Math.min(0.18, offset.y / 22)), leapHorizontal.z);
                        hasImpulse = true; hurtMarked = true;
                    }
                }
                if (leapAge > 10 && !onGround()) {
                    leapAirborne = true;
                    // 保持起跳时确定的水平动量，垂直速度继续服从重力；碰墙后停止水平推进。
                    if (horizontalCollision) leapHorizontal = Vec3.ZERO;
                    setDeltaMovement(leapHorizontal.x, getDeltaMovement().y, leapHorizontal.z);
                }
                // 真正落地后才创建冲击实体；撞墙会自然在墙前落下，不穿墙传送。
                if (leapAirborne && onGround()) {
                    GroundEruption effect = ModEntities.GROUND_ERUPTION.get().create(level());
                    if (effect != null) { effect.activate(this); level().addFreshEntity(effect); }
                    setDeltaMovement(Vec3.ZERO);
                    triggerAnim("movement", "land");
                    leapAge = -1; landingRecovery = 22; leapCooldown = 220;
                } else if (leapAge > 80) {
                    // 极端高差超时退出状态，不额外施加向上速度。
                    leapAge = -1; leapCooldown = 100; triggerAnim("movement", "land");
                } else if (leapAge >= 0) leapAge++;
            }
        }
        // 目标有效且处于二十四格内时，优先开启背炉齐喷；技能期间维持朝向并暂停寻路。
        if (!level().isClientSide) {
            if (volleyCooldown > 0) volleyCooldown--;
            int age = getVolleyAge();
            if (age < 0 && attackTicks == 0 && leapAge < 0 && landingRecovery == 0 && volleyCooldown == 0 && target != null && target.isAlive()
                    && distanceToSqr(target) <= 576 && hasLineOfSight(target)) {
                volleyTarget = target.getUUID();
                entityData.set(VOLLEY_AGE, 0);
                triggerAnim("movement", "volley");
                getNavigation().stop();
            } else if (age >= 0) {
                getNavigation().stop();
                // 每六刻一次，共八颗；落点在发射时采样，此后玩家可以走位躲避。
                if (age >= 16 && age <= 58 && (age - 16) % 6 == 0 && target != null && target.isAlive()
                        && target.getUUID().equals(volleyTarget)) {
                    int shot = (age - 16) / 6;
                    Vec3 start = ventOrigin(shot % 3);
                    double angle = shot * 2.4;
                    Vec3 aim = target.position().add(Math.cos(angle) * 1.1, 0, Math.sin(angle) * 1.1);
                    var ground = level().clip(new ClipContext(aim.add(0, 3, 0), aim.add(0, -8, 0),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
                    // 无可命中的地面时跳过这一颗，避免虚空预警。
                    if (ground.getType() != HitResult.Type.MISS) {
                        VolcanicFireball ball = ModEntities.VOLCANIC_FIREBALL.get().create(level());
                        if (ball != null) {
                            ball.launch(this, start, ground.getLocation().add(0, 0.03, 0));
                            level().addFreshEntity(ball);
                        }
                    }
                }
                entityData.set(VOLLEY_AGE, age >= 75 ? -1 : age + 1);
                if (age >= 75) volleyCooldown = 160;
            }
        }
        // 第十二刻对应双拳砸下的动画帧，届时重新核对原目标、距离和视线后结算伤害。
        if (!level().isClientSide && attackTicks > 0) {
            int elapsed = 27 - attackTicks;
            if (elapsed == 12 && target != null && target.getUUID().equals(attackTarget) && target.isAlive()) {
                Vec3 offset = target.position().subtract(position());
                boolean inReach = offset.horizontalDistance() <= 4.0D && Math.abs(offset.y) < 3.0D
                        && hasLineOfSight(target) && !isAlliedTo(target)
                        && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
                if (inReach && target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                    target.knockback(0.8D, -offset.x, -offset.z);
                    target.hurtMarked = true;
                }
            }
            attackTicks--;
        }
    }

    // 原版近战任务只负责启动动作，实际伤害延迟到双拳落下的动画命中帧。
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!level().isClientSide && attackTicks == 0 && getVolleyAge() < 0 && leapAge < 0 && landingRecovery == 0 && target instanceof LivingEntity livingTarget) {
            attackTicks = 27;
            attackTarget = livingTarget.getUUID();
            triggerAnim("movement", "attack");
            getNavigation().stop();
            return true;
        }
        return false;
    }

    // 主动跳砸期间免除自身摔落伤害，普通跌落仍沿用原版规则。
    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return leapAge >= 0 ? false : super.causeFallDamage(distance, multiplier, source);
    }

    // 玩家进入或离开实体追踪范围时同步增删 Boss 血条。
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    // 实体卸载或玩家离开追踪范围后清除对应血条。
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // 地面实际移动时播放完整行走循环，静止或腾空时恢复模型原始姿态。
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 3,
                state -> onGround() && state.isMoving() ? state.setAndContinue(WALK) : PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("volley", VOLLEY)
                .triggerableAnim("leap", LEAP)
                .triggerableAnim("land", LAND));
    }

    // GeckoLib 为每只熔渊炉心提供独立缓存，避免实体之间共享姿态。
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
