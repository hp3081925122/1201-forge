package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.hp.cindermaw.Cindermaw;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.CindermawAttack;
import org.hp.cindermaw.entity.CindermawEntity;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.geckolib.util.RenderUtil;

public final class CindermawRenderer extends GeoEntityRenderer<CindermawEntity, CindermawRenderState> {
    private static final RenderType VENT_FIRE = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/effect/vent_fire.png"));
    private static final int FIRE_FRAMES = 16;
    private static final String[] BONES = {"hand_right", "hand_left", "furnace", "vent_0_flame", "vent_1_flame", "vent_2_flame", "head", "body"};

    public CindermawRenderer(EntityRendererProvider.Context context) {
        super(context, new CindermawModel());
        shadowRadius = 1.8F;
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        withRenderLayer(new Phase2GlowLayer(this));
    }

    @Override
    public CindermawRenderState createRenderState(CindermawEntity entity, Void related) {
        return new CindermawRenderState();
    }

    @Override
    public void extractRenderState(CindermawEntity entity, CindermawRenderState state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.entity = entity;
        state.attack = entity.getAttack();
        state.attackAge = entity.attackAge(partial);
        state.partial = partial;
        state.time = entity.tickCount + partial;
        state.phase2 = entity.isPhase2();
        state.dying = entity.isDeadOrDying();
        state.fireDrawn = 0;
    }

    @Override
    public boolean shouldRender(CindermawEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(9.0));
    }

    @Override
    public void preRenderPass(RenderPassInfo<CindermawRenderState> pass, SubmitNodeCollector collector) {
        CindermawRenderState state = pass.renderState();
        if (state.attack == CindermawAttack.NONE && !state.dying) {
            pass.addBoneUpdater((info, snapshots) -> snapshots.ifPresent("head", head -> {
                float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F);
                float pitch = Mth.clamp(state.xRot, -25.0F, 25.0F);
                head.setRotY(head.getRotY() + yaw * Mth.DEG_TO_RAD);
                head.setRotX(head.getRotX() + pitch * Mth.DEG_TO_RAD);
                head.setRotZ(head.getRotZ() - yaw * 0.25F * Mth.DEG_TO_RAD);
            }));
        }
        for (int i = 0; i < BONES.length; i++) {
            int index = i;
            pass.model().getBone(BONES[i]).ifPresent(bone -> pass.addPerBoneRender(bone, (info, geoBone, tasks) -> captureBone(state, info, geoBone, index, tasks)));
        }
    }

    @Override
    public void postRenderPass(RenderPassInfo<CindermawRenderState> pass, SubmitNodeCollector collector) {
        super.postRenderPass(pass, collector);
        CindermawRenderState state = pass.renderState();
        CindermawEntity entity = state.entity;
        if (entity == null || state.dying) {
            return;
        }
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        PoseStack pose = pass.poseStack();
        sweepTrail(entity, state.attack, state.attackAge, origin, pose, collector);
        if (state.attack == CindermawAttack.BREATH) {
            breathCone(entity, state.attackAge, origin, state.partial, state.time, pose, collector);
        }
        ventJets(entity, state.attack, state.attackAge, origin, state.time, pose, collector);
    }

    private void captureBone(CindermawRenderState state, RenderPassInfo<CindermawRenderState> info, GeoBone bone, int index, SubmitNodeCollector tasks) {
        PoseStack pose = info.poseStack();
        if (state.entity != null) {
            Matrix4f localPose = RenderUtil.extractPoseFromRoot(new Matrix4f(pose.last().pose()), info.getPreRenderMatrixState());
            Vec3 world = RenderUtil.renderPoseToPosition(RenderUtil.addPosToMatrix(localPose, new Vec3(state.x, state.y, state.z)), 1, 1, 1);
            state.entity.clientBones[index] = world;
        }
        if (!bone.name().endsWith("_flame")) {
            return;
        }
        int vent = bone.name().charAt(5) - '0';
        if (vent < 0 || vent > 2 || (state.fireDrawn & (1 << vent)) != 0) {
            return;
        }
        state.fireDrawn |= 1 << vent;
        int drawn = vent;
        tasks.submitCustomGeometry(pose, VENT_FIRE, (captured, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(captured);
            ventFlame(state, drawn, local, consumer);
        });
    }

    private void sweepTrail(CindermawEntity entity, CindermawAttack attack, float age, Vec3 origin, PoseStack pose, SubmitNodeCollector collector) {
        int hand = -1;
        if (attack == CindermawAttack.SWEEP && age >= 9 && age < 15) {
            hand = CindermawEntity.BONE_HAND_RIGHT;
        } else if (attack == CindermawAttack.SWEEP && age >= 22 && age < 28) {
            hand = CindermawEntity.BONE_HAND_LEFT;
        }
        if (hand >= 0 && entity.clientBones[hand] != null) {
            entity.clientTrail.add(new Vec3[]{entity.clientBones[hand], new Vec3(age, 0, 0)});
        }
        entity.clientTrail.removeIf(sample -> attack != CindermawAttack.SWEEP || age - sample[1].x > 5 || age < sample[1].x);
        int count = entity.clientTrail.size();
        if (count < 2) {
            return;
        }
        VfxDraw.submit(collector, pose, VfxDraw.GLOW, (local, out) -> {
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
                VfxDraw.quad2(out, local, pa.add(0, hA, 0), pb.add(0, hB, 0), pb.add(0, -hB, 0), pa.add(0, -hA, 0), u0, r[1], u1, r[3], 0xFF6A1E, Math.min(fadeA, fadeB));
                VfxDraw.quad2(out, local, pa.add(0, hA * 0.45, 0), pb.add(0, hB * 0.45, 0), pb.add(0, -hB * 0.45, 0), pa.add(0, -hA * 0.45, 0), u0, r[1], u1, r[3], 0xFFE08A, Math.min(fadeA, fadeB));
            }
        });
    }

    private void breathCone(CindermawEntity entity, float age, Vec3 origin, float partial, float time, PoseStack pose, SubmitNodeCollector collector) {
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
        Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        Vec3 up = dir.cross(side).normalize();
        Vec3 base = furnace.subtract(origin).add(dir.scale(0.3));
        float reach = 8.5F * Math.min(1.0F, (age - 13) / 5.0F);
        VfxDraw.submit(collector, pose, VfxDraw.GLOW, (local, out) -> {
            int puffCount = 26;
            for (int i = 0; i < puffCount; i++) {
                float flow = time * 0.11F + i * 0.618F;
                float phase = flow - Mth.floor(flow);
                int cycle = Mth.floor(flow);
                float dist = phase * reach;
                float seed = Mth.sin(i * 12.9898F + cycle * 78.233F) * 43758.547F;
                seed = seed - Mth.floor(seed);
                double spreadAngle = seed * Math.PI * 2;
                double spread = dist * 0.22 * (0.4 + seed * 0.6);
                Vec3 at = base.add(dir.scale(dist)).add(side.scale(Math.cos(spreadAngle) * spread)).add(up.scale(Math.sin(spreadAngle) * spread * 0.7 + phase * phase * 0.6));
                double size = 0.3 + phase * 1.5;
                int rgb;
                if (phase < 0.35F) {
                    rgb = VfxDraw.mix(0xFFF0C0, 0xFF8A2A, phase / 0.35F);
                } else {
                    rgb = VfxDraw.mix(0xFF8A2A, 0x7A1A08, (phase - 0.35F) / 0.65F);
                }
                float fade = Math.min(1.0F, phase * 8.0F) * (1.0F - phase * phase);
                VfxDraw.billboard(out, local, at, size, seed * 6.28 + time * 0.08 * (i % 2 * 2 - 1), VfxDraw.PUFF[i % 2], rgb, 0.95F * fade * ramp);
            }
            float flare = 0.9F + Mth.sin(time * 2.1F) * 0.12F;
            VfxDraw.billboard(out, local, base, 0.85 * flare, time * 0.05, VfxDraw.DISC, 0xFFD890, ramp);
            VfxDraw.billboard(out, local, base.add(dir.scale(0.6)), 0.55, -time * 0.07, VfxDraw.PUFF[0], 0xFFF4D0, ramp);
        });
    }

    private void ventFlame(CindermawRenderState state, int vent, PoseStack bonePose, VertexConsumer out) {
        float heat = 1.0F;
        int speed = 2;
        if (state.phase2) {
            speed = 1;
        }
        if (state.dying) {
            heat = Math.max(0.0F, 1.0F - state.deathTime / 50.0F);
        }
        if (heat <= 0.05F) {
            return;
        }
        double w = 6.5 / 16.0;
        double h = 12.0 / 16.0;
        if (vent == 1) {
            w = 9.5 / 16.0;
            h = 16.0 / 16.0;
        }
        h *= heat;
        Vec3 base = new Vec3(0, -1.5 / 16.0, 0);
        int frame = ((int) (state.time / speed) + vent * 5) % FIRE_FRAMES;
        float v0 = frame / (float) FIRE_FRAMES;
        float v1 = (frame + 1) / (float) FIRE_FRAMES;
        double half = w * 0.62;
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 4;
            double dx = Math.cos(angle) * half;
            double dz = Math.sin(angle) * half;
            VfxDraw.quad(out, bonePose, base.add(-dx, h, -dz), base.add(dx, h, dz), base.add(dx, 0, dz), base.add(-dx, 0, -dz),
                    0, v0, 1, v1, 0xFFFFFF, 1.0F);
        }
    }

    private void ventJets(CindermawEntity entity, CindermawAttack attack, float age, Vec3 origin, float time, PoseStack pose, SubmitNodeCollector collector) {
        VfxDraw.submit(collector, pose, VfxDraw.FLAME, (local, out) -> {
            for (int v = 0; v < 3; v++) {
                Vec3 vent = entity.clientBones[CindermawEntity.BONE_VENT + v];
                if (vent == null) {
                    continue;
                }
                double length = 0;
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
                VfxDraw.column(out, local, base, new Vec3(0, length, 0), 0.32, 0.12, 3, -time * 0.25F, 1.0F, 0xFF7A26, 0.9F);
                VfxDraw.column(out, local, base, new Vec3(0, length * 0.6, 0), 0.18, 0.06, 3, -time * 0.3F, 1.0F, 0xFFE6A0, 0.9F);
            }
        });
    }
}
