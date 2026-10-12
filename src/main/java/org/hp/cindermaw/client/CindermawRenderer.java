package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.CindermawAttack;
import org.hp.cindermaw.entity.CindermawEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// Boss 渲染：模型、发光层、骨骼坐标回写，以及横扫弧光、喷焰火锥与火山喷柱。
public final class CindermawRenderer extends GeoEntityRenderer<CindermawEntity> {
    private static final String[] BONES = {"hand_right", "hand_left", "furnace", "vent_0_flame", "vent_1_flame", "vent_2_flame", "head", "body"};

    public CindermawRenderer(EntityRendererProvider.Context context) {
        super(context, new CindermawModel());
        shadowRadius = 1.8F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    // 技能特效可能超出碰撞箱，放宽视锥裁剪
    @Override
    public boolean shouldRender(CindermawEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(9.0));
    }

    @Override
    public void render(CindermawEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        // 开启骨骼矩阵追踪
        for (String name : BONES) {
            getGeoModel().getBone(name).ifPresent(bone -> bone.setTrackingMatrices(true));
        }
        super.render(entity, yaw, partial, pose, buffers, light);
        // 回写骨骼世界坐标，供客户端粒子使用
        for (int i = 0; i < BONES.length; i++) {
            GeoBone bone = getGeoModel().getBone(BONES[i]).orElse(null);
            if (bone != null) {
                var p = bone.getWorldPosition();
                entity.clientBones[i] = new Vec3(p.x, p.y, p.z);
            }
        }
        if (entity.isDeadOrDying()) {
            return;
        }
        Vec3 origin = new Vec3(Mth.lerp(partial, entity.xo, entity.getX()), Mth.lerp(partial, entity.yo, entity.getY()), Mth.lerp(partial, entity.zo, entity.getZ()));
        CindermawAttack attack = entity.getAttack();
        float age = entity.attackAge(partial);
        float time = entity.tickCount + partial;
        sweepTrail(entity, attack, age, origin, pose, buffers);
        if (attack == CindermawAttack.BREATH) {
            breathCone(entity, age, origin, partial, time, pose, buffers);
        }
        ventJets(entity, attack, age, origin, time, pose, buffers);
    }

    // 横扫弧光：记录挥击拳头的轨迹，画成渐隐的发光带
    private void sweepTrail(CindermawEntity entity, CindermawAttack attack, float age, Vec3 origin, PoseStack pose, MultiBufferSource buffers) {
        int hand = -1;
        if (attack == CindermawAttack.SWEEP && age >= 7 && age < 15) {
            hand = CindermawEntity.BONE_HAND_RIGHT;
        } else if (attack == CindermawAttack.SWEEP && age >= 20 && age < 28) {
            hand = CindermawEntity.BONE_HAND_LEFT;
        }
        if (hand >= 0 && entity.clientBones[hand] != null) {
            entity.clientTrail.add(new Vec3[]{entity.clientBones[hand], new Vec3(age, 0, 0)});
        }
        // 只保留最近五刻的采样
        entity.clientTrail.removeIf(sample -> attack != CindermawAttack.SWEEP || age - sample[1].x > 5 || age < sample[1].x);
        int count = entity.clientTrail.size();
        if (count < 2) {
            return;
        }
        VertexConsumer out = buffers.getBuffer(VfxDraw.GLOW);
        float[] r = VfxDraw.TRAIL;
        for (int i = 0; i < count - 1; i++) {
            Vec3[] a = entity.clientTrail.get(i);
            Vec3[] b = entity.clientTrail.get(i + 1);
            float fadeA = 1.0F - (float) (age - a[1].x) / 5.0F;
            float fadeB = 1.0F - (float) (age - b[1].x) / 5.0F;
            double hA = 1.1 * fadeA;
            double hB = 1.1 * fadeB;
            Vec3 pa = a[0].subtract(origin);
            Vec3 pb = b[0].subtract(origin);
            float u0 = r[0] + (r[2] - r[0]) * (1 - fadeA);
            float u1 = r[0] + (r[2] - r[0]) * (1 - fadeB);
            // 外层橙红宽带 + 内层亮黄窄带
            VfxDraw.quad2(out, pose, pa.add(0, hA, 0), pb.add(0, hB, 0), pb.add(0, -hB, 0), pa.add(0, -hA, 0), u0, r[1], u1, r[3], 0xFF6A1E, Math.min(fadeA, fadeB));
            VfxDraw.quad2(out, pose, pa.add(0, hA * 0.45, 0), pb.add(0, hB * 0.45, 0), pb.add(0, -hB * 0.45, 0), pa.add(0, -hA * 0.45, 0), u0, r[1], u1, r[3], 0xFFE08A, Math.min(fadeA, fadeB));
        }
    }

    // 喷焰火锥：外锥橙红、内芯亮黄，火焰贴图沿喷射方向滚动
    private void breathCone(CindermawEntity entity, float age, Vec3 origin, float partial, float time, PoseStack pose, MultiBufferSource buffers) {
        if (age < 13 || age > 53) {
            return;
        }
        float ramp = Math.min(1.0F, (age - 13) / 3.0F) * Math.min(1.0F, (53 - age) / 4.0F);
        Vec3 furnace = entity.clientBones[CindermawEntity.BONE_FURNACE];
        if (furnace == null) {
            furnace = entity.local(0, 1.36, 0.71);
        }
        float yawDeg = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(-7);
        Vec3 dir = new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 base = furnace.subtract(origin).add(dir.scale(0.3));
        float wobble = 1.0F + Mth.sin(time * 1.3F) * 0.06F;
        VfxDraw.column(buffers, pose, base, dir.scale(8.5 * ramp), 0.35, 2.6 * wobble, 5, -time * 0.18F, 2.0F, 0xFF5A1A, 0.85F * ramp);
        VfxDraw.column(buffers, pose, base, dir.scale(5.5 * ramp), 0.25, 1.2 * wobble, 4, -time * 0.24F, 1.5F, 0xFFD27A, 0.9F * ramp);
        VfxDraw.flat(buffers, VfxDraw.GLOW, pose, base, 1.0, VfxDraw.DISC, 0xFFB050, 0.7F * ramp);
    }

    // 火山喷柱：齐射时三口喷发，二阶段常驻小火柱
    private void ventJets(CindermawEntity entity, CindermawAttack attack, float age, Vec3 origin, float time, PoseStack pose, MultiBufferSource buffers) {
        for (int v = 0; v < 3; v++) {
            Vec3 vent = entity.clientBones[CindermawEntity.BONE_VENT + v];
            if (vent == null) {
                continue;
            }
            double length = 0;
            if (entity.isPhase2()) {
                length = 0.9 + Mth.sin(time * 0.7F + v * 2) * 0.15;
            }
            if (attack == CindermawAttack.BARRAGE) {
                for (int shot = 12; shot <= 36; shot += 12) {
                    float since = age - shot;
                    if (since >= -2 && since < 8) {
                        float pulse = Math.max(0, Math.min(1, (since + 2) / 2.0F)) * Math.max(0, 1 - Math.max(0, since) / 8.0F);
                        length = Math.max(length, 4.5 * pulse);
                    }
                }
            }
            if (attack == CindermawAttack.ROAR && age < 30) {
                length = Math.max(length, 2.0 + Mth.sin(time * 1.5F + v) * 0.5);
            }
            if (length <= 0.05) {
                continue;
            }
            Vec3 base = vent.subtract(origin);
            VfxDraw.column(buffers, pose, base, new Vec3(0, length, 0), 0.32, 0.12, 3, -time * 0.25F, 1.0F, 0xFF7A26, 0.9F);
            VfxDraw.column(buffers, pose, base, new Vec3(0, length * 0.6, 0), 0.18, 0.06, 3, -time * 0.3F, 1.0F, 0xFFE6A0, 0.9F);
        }
    }
}
