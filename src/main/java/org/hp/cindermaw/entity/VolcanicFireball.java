package org.hp.cindermaw.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// 单一实体承载固定弹道、落点预警和命中后的火浪，避免为每个视觉片段创建实体。
public final class VolcanicFireball extends Projectile {
    private static final EntityDataAccessor<CompoundTag> FLIGHT = SynchedEntityData.defineId(VolcanicFireball.class, EntityDataSerializers.COMPOUND_TAG);
    public static final int FLIGHT_TICKS = 32;
    public static final int FADE_TICKS = 24;

    public VolcanicFireball(EntityType<? extends VolcanicFireball> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    // 弹道参数只在发射和碰撞时同步，客户端依据世界时间重建同一条曲线。
    @Override
    protected void defineSynchedData() { entityData.define(FLIGHT, new CompoundTag()); }

    public void launch(CindermawEntity owner, Vec3 start, Vec3 end) {
        setOwner(owner);
        CompoundTag data = new CompoundTag();
        data.putDouble("SX", start.x); data.putDouble("SY", start.y); data.putDouble("SZ", start.z);
        data.putDouble("EX", end.x); data.putDouble("EY", end.y); data.putDouble("EZ", end.z);
        data.putLong("Start", level().getGameTime());
        entityData.set(FLIGHT, data);
        setPos(start.x, start.y, start.z);
    }

    // 公开只读采样供火球和尾迹共用，发射后绝不重新读取目标位置。
    public boolean ready() { return !entityData.get(FLIGHT).isEmpty(); }
    public float age(float partial) { return level().getGameTime() - entityData.get(FLIGHT).getLong("Start") + partial; }
    public boolean impacted() { return entityData.get(FLIGHT).getBoolean("Hit"); }
    public float impactAge(float partial) { return level().getGameTime() - entityData.get(FLIGHT).getLong("ImpactTime") + partial; }
    public Vec3 landing() {
        CompoundTag d = entityData.get(FLIGHT);
        return new Vec3(d.getDouble("EX"), d.getDouble("EY"), d.getDouble("EZ"));
    }
    public Vec3 sample(float age) {
        CompoundTag d = entityData.get(FLIGHT);
        Vec3 start = new Vec3(d.getDouble("SX"), d.getDouble("SY"), d.getDouble("SZ"));
        double t = Math.max(0, Math.min(1, age / FLIGHT_TICKS));
        return start.add(landing().subtract(start).scale(t)).add(0, 18 * t * (1 - t), 0);
    }

    // 按实际曲线逐段检测方块；屋顶和墙壁会提前引爆，寿命不会因区块重载而刷新。
    @Override
    public void tick() {
        super.tick();
        if (entityData.get(FLIGHT).isEmpty()) return;
        if (impacted()) {
            if (!level().isClientSide && impactAge(0) >= FADE_TICKS) discard();
            return;
        }
        Vec3 next = sample(age(0));
        if (!level().isClientSide) {
            var hit = level().clip(new ClipContext(position(), next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
            if (hit.getType() != HitResult.Type.MISS || age(0) >= FLIGHT_TICKS) {
                Vec3 contact = hit.getType() == HitResult.Type.MISS ? landing() : hit.getLocation();
                setPos(contact.x, contact.y + 0.03, contact.z);
                CompoundTag data = entityData.get(FLIGHT).copy();
                data.putBoolean("Hit", true); data.putLong("ImpactTime", level().getGameTime());
                data.putDouble("EX", contact.x); data.putDouble("EY", contact.y + 0.03); data.putDouble("EZ", contact.z);
                entityData.set(FLIGHT, data);
                // 伤害只结算一次，排除自身、同类、队友和无敌模式，不破坏地形。
                if (getOwner() instanceof CindermawEntity owner && owner.isAlive()) {
                    for (LivingEntity victim : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.0))) {
                        if (!victim.isAlive() || victim instanceof CindermawEntity || owner.isAlliedTo(victim)
                                || victim.isSpectator() || victim instanceof Player player && player.isCreative()) continue;
                        Vec3 point = victim.getBoundingBox().getCenter();
                        if (point.distanceToSqr(position()) > 4.0 || level().clip(new ClipContext(position().add(0, 0.15, 0), point,
                                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
                        victim.hurt(damageSources().mobProjectile(this, owner), (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F);
                    }
                }
                return;
            }
        }
        setDeltaMovement(next.subtract(position()));
        setPos(next.x, next.y, next.z);
    }

    // 保存绝对时间和命中标志，防止重载后重复爆炸。
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.put("Flight", entityData.get(FLIGHT).copy()); }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(FLIGHT, tag.getCompound("Flight")); }
}
