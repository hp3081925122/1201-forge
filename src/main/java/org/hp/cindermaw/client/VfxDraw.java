package org.hp.cindermaw.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.Cindermaw;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class VfxDraw {
    public static final Identifier ATLAS = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/effect/vfx.png");
    public static final Identifier FLAME_SCROLL = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/effect/flame_scroll.png");
    public static final RenderPipeline ADDITIVE = RenderPipelines.EYES.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "pipeline/additive_glow"))
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withCull(false)
            .build();
    private static final Function<Identifier, RenderType> ADDITIVE_TYPE = Util.memoize(texture -> RenderType.create(
            "cindermaw_additive",
            RenderSetup.builder(ADDITIVE).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
    public static final RenderType GLOW = ADDITIVE_TYPE.apply(ATLAS);
    public static final RenderType SOFT = RenderTypes.entityTranslucentEmissive(ATLAS);
    public static final RenderType FLAME = ADDITIVE_TYPE.apply(FLAME_SCROLL);
    public static final RenderType MOLTEN = ADDITIVE_TYPE.apply(Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/effect/lava_molten.png"));
    public static final RenderType CRUST = ADDITIVE_TYPE.apply(Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/effect/lava_crust.png"));

    public static final float[] CRACK = region(0, 0, 64, 8);
    public static final float[] RING = region(64, 0, 128, 8);
    public static final float[] POOL = region(32, 8, 64, 40);
    public static final float[] WARN = region(64, 8, 96, 40);
    public static final float[] DISC = region(96, 8, 128, 40);
    public static final float[] TRAIL = region(0, 40, 64, 48);
    public static final float[] ROCK = region(64, 40, 80, 56);
    public static final float[] CORE = region(80, 40, 96, 56);
    public static final float[] DUST = region(0, 8, 32, 40);
    public static final float[] SCORCH = region(0, 88, 32, 120);
    public static final float[][] PUFF = {region(0, 56, 32, 88), region(32, 56, 64, 88)};

    private VfxDraw() {
    }

    public static void submit(SubmitNodeCollector collector, PoseStack pose, RenderType type, BiConsumer<PoseStack, VertexConsumer> draw) {
        collector.submitCustomGeometry(pose, type, (captured, consumer) -> {
            PoseStack local = new PoseStack();
            local.last().set(captured);
            draw.accept(local, consumer);
        });
    }

    private static float[] region(int x0, int y0, int x1, int y1) {
        return new float[]{x0 / 128F, y0 / 128F, x1 / 128F, y1 / 128F};
    }

    public static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normal, Vec3 p, float u, float v, int rgb, float alpha) {
        float k = Math.max(0, Math.min(1, alpha));
        int a = (int) (255 * k);
        out.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z)
                .setColor((int) (((rgb >> 16) & 255) * k), (int) (((rgb >> 8) & 255) * k), (int) ((rgb & 255) * k), a)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(0, 1, 0);
    }

    public static void quad(VertexConsumer out, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                            float u0, float v0, float u1, float v1, int rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        vertex(out, m, n, a, u0, v0, rgb, alpha);
        vertex(out, m, n, b, u1, v0, rgb, alpha);
        vertex(out, m, n, c, u1, v1, rgb, alpha);
        vertex(out, m, n, d, u0, v1, rgb, alpha);
    }

    public static void quad2(VertexConsumer out, PoseStack pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float u0, float v0, float u1, float v1, int rgb, float alpha) {
        quad(out, pose, a, b, c, d, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, b, a, d, c, u1, v0, u0, v1, rgb, alpha);
    }

    public static void flat(VertexConsumer out, PoseStack pose, Vec3 center, double radius, float[] r, int rgb, float alpha) {
        Vec3 a = center.add(-radius, 0, -radius);
        Vec3 b = center.add(radius, 0, -radius);
        Vec3 c = center.add(radius, 0, radius);
        Vec3 d = center.add(-radius, 0, radius);
        quad2(out, pose, a, b, c, d, r[0], r[1], r[2], r[3], rgb, alpha);
    }

    public static void ring(VertexConsumer out, PoseStack pose, Vec3 center, double outer, double inner,
                            double lift, float[] r, int rgb, float alpha) {
        int segments = Math.max(16, (int) (outer * 10));
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments;
            double a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 o0 = center.add(Math.cos(a0) * outer, 0, Math.sin(a0) * outer);
            Vec3 o1 = center.add(Math.cos(a1) * outer, 0, Math.sin(a1) * outer);
            Vec3 i1 = center.add(Math.cos(a1) * inner, lift, Math.sin(a1) * inner);
            Vec3 i0 = center.add(Math.cos(a0) * inner, lift, Math.sin(a0) * inner);
            quad2(out, pose, o0, o1, i1, i0, r[0], r[1], r[2], r[3], rgb, alpha);
        }
    }

    public static void wall(VertexConsumer out, PoseStack pose, Vec3 center, double radius, double height, float scroll, int rgb, float alpha) {
        int segments = Math.max(20, (int) (radius * 8));
        for (int i = 0; i < segments; i++) {
            double a0 = i * Math.PI * 2 / segments;
            double a1 = (i + 1) * Math.PI * 2 / segments;
            Vec3 b0 = center.add(Math.cos(a0) * radius, 0, Math.sin(a0) * radius);
            Vec3 b1 = center.add(Math.cos(a1) * radius, 0, Math.sin(a1) * radius);
            float u0 = i * 0.5F;
            quad2(out, pose, b0.add(0, height, 0), b1.add(0, height, 0), b1, b0, u0, scroll, u0 + 0.5F, scroll + 1, rgb, alpha);
        }
    }

    public static void column(VertexConsumer out, PoseStack pose, Vec3 base, Vec3 axis, double width0, double width1,
                              int planes, float scroll, float repeat, int rgb, float alpha) {
        if (alpha <= 0.01F || axis.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = axis.normalize();
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = dir.cross(new Vec3(1, 0, 0));
        }
        side = side.normalize();
        Vec3 other = dir.cross(side).normalize();
        Vec3 top = base.add(axis);
        for (int i = 0; i < planes; i++) {
            double angle = i * Math.PI / planes;
            Vec3 s = side.scale(Math.cos(angle)).add(other.scale(Math.sin(angle)));
            quad2(out, pose, top.subtract(s.scale(width1)), top.add(s.scale(width1)), base.add(s.scale(width0)), base.subtract(s.scale(width0)),
                    0, scroll, 1, scroll + repeat, rgb, alpha);
        }
    }

    public static void billboard(VertexConsumer out, PoseStack pose, Vec3 center, double size, double angle, float[] r, int rgb, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3fc leftF = camera.leftVector();
        Vector3fc upF = camera.upVector();
        Vec3 left = new Vec3(leftF.x(), leftF.y(), leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        double cos = Math.cos(angle) * size;
        double sin = Math.sin(angle) * size;
        Vec3 ax = left.scale(cos).add(up.scale(sin));
        Vec3 ay = up.scale(cos).subtract(left.scale(sin));
        quad(out, pose, center.add(ax).add(ay), center.subtract(ax).add(ay), center.subtract(ax).subtract(ay), center.add(ax).subtract(ay),
                r[0], r[1], r[2], r[3], rgb, alpha);
    }

    public static int mix(int a, int b, float t) {
        float k = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * k));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * k));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * k));
        return (r << 16) | (g << 8) | bl;
    }

    public static void box(VertexConsumer out, PoseStack pose, double r, float[] reg, int rgb, float alpha) {
        Vec3 a = new Vec3(-r, -r, -r);
        Vec3 b = new Vec3(r, -r, -r);
        Vec3 c = new Vec3(r, r, -r);
        Vec3 d = new Vec3(-r, r, -r);
        Vec3 e = new Vec3(-r, -r, r);
        Vec3 f = new Vec3(r, -r, r);
        Vec3 g = new Vec3(r, r, r);
        Vec3 h = new Vec3(-r, r, r);
        float u0 = reg[0];
        float v0 = reg[1];
        float u1 = reg[2];
        float v1 = reg[3];
        quad(out, pose, d, c, b, a, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, g, h, e, f, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, h, d, a, e, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, c, g, f, b, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, h, g, c, d, u0, v0, u1, v1, rgb, alpha);
        quad(out, pose, a, b, f, e, u0, v0, u1, v1, rgb, alpha);
    }
}
