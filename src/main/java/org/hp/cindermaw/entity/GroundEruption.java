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

// 一次落地只生成一个特效实体，十二个地面喷口共享同步状态和寿命。
public final class GroundEruption extends Projectile {
    private static final EntityDataAccessor<CompoundTag> DATA = SynchedEntityData.defineId(GroundEruption.class, EntityDataSerializers.COMPOUND_TAG);
    public GroundEruption(EntityType<? extends GroundEruption> type, Level level) { super(type,level); setNoGravity(true); }
    @Override
    protected void defineSynchedData() { entityData.define(DATA,new CompoundTag()); }
    public boolean ready() { return !entityData.get(DATA).isEmpty(); }
    public float age(float partial) { return level().getGameTime()-entityData.get(DATA).getLong("Start")+partial; }
    public boolean hasVent(int i) { return entityData.get(DATA).getBoolean("V"+i); }
    public Vec3 vent(int i) {
        CompoundTag d=entityData.get(DATA);
        return new Vec3(d.getDouble("X"+i),d.getDouble("Y"+i),d.getDouble("Z"+i));
    }

    // 落点沿环形分布逐个贴合地面，悬崖外或没有支撑的位置不生成喷火。
    public void activate(CindermawEntity owner) {
        setOwner(owner); setPos(owner.getX(),owner.getY()+0.04,owner.getZ());
        CompoundTag d=new CompoundTag(); d.putLong("Start",level().getGameTime());
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6, r=i%2==0?3.2:4.5;
            Vec3 offset=new Vec3(Math.cos(a)*r,0,Math.sin(a)*r);
            Vec3 point=position().add(offset);
            var hit=level().clip(new ClipContext(point.add(0,2,0),point.add(0,-4,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
            if(hit.getType()==HitResult.Type.MISS) continue;
            Vec3 local=hit.getLocation().subtract(position()).add(0,0.04,0);
            d.putBoolean("V"+i,true); d.putDouble("X"+i,local.x);d.putDouble("Y"+i,local.y);d.putDouble("Z"+i,local.z);
        }
        entityData.set(DATA,d);
    }

    // 冲击与喷火分别只结算一轮，保存过的状态不会因重新加载重复触发。
    @Override
    public void tick() {
        super.tick(); setDeltaMovement(Vec3.ZERO);
        if(level().isClientSide || !ready()) return;
        if(age(0)>48) { discard(); return; }
        CompoundTag d=entityData.get(DATA);
        boolean shock=!d.getBoolean("ShockDone");
        boolean fire=age(0)>=14 && !d.getBoolean("FireDone");
        if(!shock && !fire) return;
        CompoundTag updated=d.copy();
        if(shock) updated.putBoolean("ShockDone",true);
        if(fire) updated.putBoolean("FireDone",true);
        entityData.set(DATA,updated);
        if(!(getOwner() instanceof CindermawEntity owner) || !owner.isAlive()) return;
        for(LivingEntity victim:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(6,4,6))) {
            if(!victim.isAlive() || victim instanceof CindermawEntity || owner.isAlliedTo(victim) || victim.isSpectator()
                    || victim instanceof Player player && player.isCreative()) continue;
            Vec3 offset=victim.position().subtract(position());
            boolean hit=shock && offset.horizontalDistance()<=3.8 && Math.abs(offset.y)<2.5;
            if(fire) {
                for(int i=0;i<12;i++) {
                    if(!hasVent(i)) continue;
                    Vec3 local=offset.subtract(vent(i));
                    if(local.horizontalDistance()<0.9 && local.y> -0.5 && local.y<3) hit=true;
                }
            }
            if(!hit || level().clip(new ClipContext(position().add(0,0.4,0),victim.getBoundingBox().getCenter(),
                    ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()!=HitResult.Type.MISS) continue;
            victim.hurt(damageSources().mobProjectile(this,owner),(float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*(shock?1.5F:0.6F));
            if(shock) { victim.knockback(0.9,-offset.x,-offset.z); victim.hurtMarked=true; }
            if(fire) victim.setSecondsOnFire(3);
        }
    }
    // 持久化起始时刻与结算标志，重载后只播放剩余部分。
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.put("Eruption",entityData.get(DATA).copy()); }
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);entityData.set(DATA,tag.getCompound("Eruption")); }
}
