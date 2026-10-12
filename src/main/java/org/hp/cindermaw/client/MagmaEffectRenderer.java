package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.MagmaEffect;

// 地面特效网格：冲击环、熔岩裂缝、喷泉火柱、熔岩池与新星火墙。
public final class MagmaEffectRenderer extends EntityRenderer<MagmaEffect> {
    public MagmaEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MagmaEffect effect) {
        return VfxDraw.ATLAS;
    }

    // 按效果范围放宽视锥裁剪
    @Override
    public boolean shouldRender(MagmaEffect effect, Frustum frustum, double x, double y, double z) {
        if (!effect.ready()) {
            return false;
        }
        double reach = effect.data().getFloat("R") + 3;
        if (effect.kind() == MagmaEffect.FISSURE) {
            reach = effect.data().getInt("Segments") + 3;
        }
        return effect.shouldRender(x, y, z) && frustum.isVisible(effect.getBoundingBox().inflate(reach, 5, reach));
    }

    @Override
    public void render(MagmaEffect effect, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!effect.ready()) {
            return;
        }
        float age = effect.age(partial);
        float time = effect.tickCount + partial;
        var d = effect.data();
        switch (effect.kind()) {
            case MagmaEffect.SHOCKWAVE -> shockwave(d.getFloat("R"), d.getInt("Style"), age, pose, buffers);
            case MagmaEffect.FISSURE -> fissure(effect, age, time, pose, buffers);
            case MagmaEffect.GEYSERS -> geysers(effect, age, time, pose, buffers);
            case MagmaEffect.POOL -> pool(effect, d.getFloat("R"), age, time, pose, buffers);
            case MagmaEffect.NOVA -> nova(effect, age, time, pose, buffers);
            default -> {
            }
        }
    }

    // 冲击环：先快后慢扩张，发光环带加翻起的烟尘墙
    private void shockwave(float radius, int style, float age, PoseStack pose, MultiBufferSource buffers) {
        if (age > 16) {
            return;
        }
        float t = Math.min(1.0F, age / 12.0F);
        float ease = 1 - (1 - t) * (1 - t);
        float r = 0.4F + radius * ease;
        float fade = 1 - t;
        int color = 0xFF7A2A;
        if (style == 1) {
            color = 0xFFC060;
        }
        VfxDraw.ring(buffers, VfxDraw.GLOW, pose, new Vec3(0, 0.08, 0), r, Math.max(0, r - 1.1 * fade - 0.2), 0.0, VfxDraw.RING, color, fade);
        VfxDraw.ring(buffers, VfxDraw.SOFT, pose, new Vec3(0, 0.05, 0), r * 0.98, r * 0.98 - 0.4, 0.55 * fade, VfxDraw.DUST, 0x3A3236, fade * 0.8F);
        if (age < 4) {
            VfxDraw.flat(buffers, VfxDraw.GLOW, pose, new Vec3(0, 0.1, 0), radius * 0.6, VfxDraw.DISC, 0xFFD080, 1 - age / 4.0F);
        }
    }

    // 熔岩裂缝：逐段延伸的发光裂纹，喷发时每段冒出火柱
    private void fissure(MagmaEffect effect, float age, float time, PoseStack pose, MultiBufferSource buffers) {
        var d = effect.data();
        int lines = d.getInt("Lines");
        int segments = d.getInt("Segments");
        float reveal = segments * Math.min(1.0F, age / 10.0F);
        float fade = Math.min(1.0F, (effect.life() - age) / 12.0F);
        VertexConsumer glow = buffers.getBuffer(VfxDraw.GLOW);
        float[] cr = VfxDraw.CRACK;
        for (int line = 0; line < lines; line++) {
            for (int k = 0; k < segments; k++) {
                float dy = effect.groundOffset(line * segments + k);
                if (Float.isNaN(dy) || k >= reveal) {
                    break;
                }
                Vec3 a = effect.fissurePoint(line, k).add(0, dy + 0.04, 0);
                Vec3 b = effect.fissurePoint(line, k + 1).add(0, dy + 0.04, 0);
                Vec3 along = b.subtract(a);
                Vec3 side = new Vec3(-along.z, 0, along.x).normalize();
                // 裂缝宽度随喷发前的蓄热变粗
                int erupt = MagmaEffect.fissureEruptAge(k);
                float heat = Mth.clamp((age - erupt + 6) / 6.0F, 0.0F, 1.0F);
                double width = 0.22 + 0.25 * heat;
                float partial = Math.min(1.0F, reveal - k);
                Vec3 end = a.add(along.scale(partial));
                VfxDraw.quad2(glow, pose, a.add(side.scale(width)), end.add(side.scale(width)), end.subtract(side.scale(width)), a.subtract(side.scale(width)),
                        cr[0], cr[1], cr[0] + (cr[2] - cr[0]) * partial, cr[3], 0xFF8030, fade);
                // 喷发火柱：迅速窜高后回落
                float since = age - erupt;
                if (since >= 0 && since < 12) {
                    float height = (float) (2.8 * Math.sin(Math.PI * since / 12.0F));
                    Vec3 mid = a.add(along.scale(0.5));
                    VfxDraw.column(buffers, pose, mid, new Vec3(0, height, 0), 0.45, 0.15, 3, -time * 0.3F + k, 1.0F, 0xFF6A20, 0.9F);
                    VfxDraw.column(buffers, pose, mid, new Vec3(0, height * 0.6, 0), 0.25, 0.08, 2, -time * 0.35F + k, 1.0F, 0xFFE09A, 0.9F);
                }
            }
        }
    }

    // 喷泉：预警圈收拢，到时喷出巨型火柱
    private void geysers(MagmaEffect effect, float age, float time, PoseStack pose, MultiBufferSource buffers) {
        var d = effect.data();
        int delay = d.getInt("Delay");
        for (int i = 0; i < d.getInt("Count"); i++) {
            float dy = effect.groundOffset(i);
            if (Float.isNaN(dy)) {
                continue;
            }
            Vec3 at = effect.geyserPoint(i).add(0, dy + 0.05, 0);
            if (age < delay) {
                float t = age / delay;
                VfxDraw.flat(buffers, VfxDraw.GLOW, pose, at, 1.2, VfxDraw.WARN, 0xFF5020, 0.4F + 0.6F * t);
                VfxDraw.flat(buffers, VfxDraw.GLOW, pose, at.add(0, 0.01, 0), 1.2 * (1 - t) + 0.15, VfxDraw.WARN, 0xFFC070, 0.8F);
                continue;
            }
            float since = age - delay;
            if (since < 18) {
                float rise = Math.min(1.0F, since / 2.5F);
                float decay = Math.min(1.0F, (18 - since) / 7.0F);
                double height = (4.8 + Mth.sin(time * 1.9F + i) * 0.4) * rise * decay;
                VfxDraw.column(buffers, pose, at, new Vec3(0, height, 0), 0.75, 0.3, 4, -time * 0.32F + i, 1.6F, 0xFF5A1A, 0.95F);
                VfxDraw.column(buffers, pose, at, new Vec3(0, height * 0.65, 0), 0.4, 0.12, 3, -time * 0.4F + i, 1.2F, 0xFFE6A0, 0.95F);
                VfxDraw.flat(buffers, VfxDraw.GLOW, pose, at, 1.4, VfxDraw.DISC, 0xFF9A40, decay);
            }
        }
    }

    // 熔岩池：底层熔岩片，上层辉光脉动
    private void pool(MagmaEffect effect, float radius, float age, float time, PoseStack pose, MultiBufferSource buffers) {
        float grow = Math.min(1.0F, age / 4.0F);
        float fade = Math.min(1.0F, (effect.life() - age) / 18.0F);
        VfxDraw.flat(buffers, VfxDraw.SOFT, pose, new Vec3(0, 0.03, 0), radius * grow, VfxDraw.POOL, 0xFFFFFF, fade);
        float pulse = 0.45F + Mth.sin(time * 0.25F) * 0.15F;
        VfxDraw.flat(buffers, VfxDraw.GLOW, pose, new Vec3(0, 0.05, 0), radius * grow * 1.1, VfxDraw.DISC, 0xFF7A30, pulse * fade);
    }

    // 新星：火墙圆环向外推进，地面留下发光带
    private void nova(MagmaEffect effect, float age, float time, PoseStack pose, MultiBufferSource buffers) {
        float radius = effect.novaRadius(age);
        float max = effect.data().getFloat("R");
        if (radius > max + 1) {
            return;
        }
        float fade = Math.min(1.0F, (max + 1 - radius) / 3.0F);
        VfxDraw.wall(buffers, pose, new Vec3(0, 0.02, 0), radius, 1.25 * fade, -time * 0.22F, 0xFF6A22, 0.95F * fade);
        VfxDraw.wall(buffers, pose, new Vec3(0, 0.02, 0), radius - 0.12, 0.7 * fade, -time * 0.28F, 0xFFE0A0, 0.8F * fade);
        VfxDraw.ring(buffers, VfxDraw.GLOW, pose, new Vec3(0, 0.06, 0), radius + 0.2, Math.max(0, radius - 1.6), 0.0, VfxDraw.RING, 0xFF7A2A, 0.8F * fade);
    }
}
