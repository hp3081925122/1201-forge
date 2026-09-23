package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

// 几何材质绘制碎块与热浪，透明序列材质绘制翻卷火焰。
public final class VolcanicMeshes {
    public static final ResourceLocation TEXTURE = new ResourceLocation("cindermaw", "textures/effect/mesh.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final RenderType FLAME = RenderType.entityTranslucentEmissive(
            new ResourceLocation("cindermaw", "textures/effect/flame_atlas.png"));
    private VolcanicMeshes() {}

    // 通用四边形写入顶点颜色，使用深度测试保持被墙壁遮挡。
    public static void quad(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float opacity) {
        VertexConsumer out = buffers.getBuffer(TYPE);
        Vec3[] points = {a,b,c,d};
        for (int i = 0; i < 4; i++) {
            Vec3 v = points[i];
            out.vertex(pose.last().pose(), (float)v.x, (float)v.y, (float)v.z)
                    .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (int)(255 * Math.max(0, Math.min(1, opacity))))
                    .uv(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 1 : 0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(pose.last().normal(), 0, 1, 0).endVertex();
        }
    }

    // 六个面构成真实体积，避免火球转到侧面时消失。
    public static void box(PoseStack p, MultiBufferSource b, Vec3 center, double r, int color, float alpha) {
        Vec3 a = center.add(-r,-r,-r), c = center.add(r,-r,-r), d = center.add(r,r,-r), e = center.add(-r,r,-r);
        Vec3 f = center.add(-r,-r,r), g = center.add(r,-r,r), h = center.add(r,r,r), i = center.add(-r,r,r);
        quad(p,b,a,e,d,c,color,alpha); quad(p,b,f,g,h,i,color,alpha);
        quad(p,b,a,f,i,e,color,alpha); quad(p,b,c,d,h,g,color,alpha);
        quad(p,b,e,i,h,d,color,alpha); quad(p,b,a,c,g,f,color,alpha);
    }

    // 环形四边形逐段变化亮度和外缘半径，形成裂纹式预警与低矮火浪。
    public static void ring(PoseStack p, MultiBufferSource b, Vec3 center, double radius, double width, double height, int color, float alpha) {
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16, z = (i + 1) * Math.PI / 16;
            double r = radius * (i % 4 == 0 ? 1.035 : 1);
            Vec3 v0 = center.add(Math.cos(a)*r,0,Math.sin(a)*r);
            Vec3 v1 = center.add(Math.cos(z)*r,0,Math.sin(z)*r);
            Vec3 v2 = center.add(Math.cos(z)*(r-width),height,Math.sin(z)*(r-width));
            Vec3 v3 = center.add(Math.cos(a)*(r-width),height,Math.sin(a)*(r-width));
            quad(p,b,v0,v1,v2,v3,color,alpha);
        }
    }

    // 三张交叉火焰面共用八帧图集，每两刻换帧，保留遮挡与透明轮廓。
    public static void jet(PoseStack p, MultiBufferSource b, Vec3 start, double length, float alpha, float time) {
        if (length <= 0 || alpha <= 0) return;
        VertexConsumer out = b.getBuffer(FLAME);
        double radius = Math.min(0.42, length * 0.36);
        int opacity = (int)(255 * Math.min(1, alpha));
        for (int plane=0; plane<3; plane++) {
            int frame = Math.floorMod((int)Math.floor(time / 2) + plane * 2, 8);
            float u0 = (frame % 4) / 4F, u1 = u0 + 0.25F;
            float v0 = (frame / 4) / 2F, v1 = v0 + 0.5F;
            double angle = plane * Math.PI / 3;
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            // 根部下移抵消图集留白，让火焰贴住喷口；顶端保留自然破碎形状。
            Vec3 root = start.add(0, -length * 0.055, 0);
            Vec3[] points = {root.add(-x,0,-z), root.add(x,0,z),
                    root.add(x,length,z), root.add(-x,length,-z)};
            for (int i=0; i<4; i++) {
                Vec3 vertex = points[i];
                out.vertex(p.last().pose(), (float)vertex.x, (float)vertex.y, (float)vertex.z)
                        .color(255,255,255,opacity).uv(i==1 || i==2 ? u1 : u0, i>=2 ? v0 : v1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
                        .normal(p.last().normal(), (float)-Math.sin(angle),0,(float)Math.cos(angle)).endVertex();
            }
        }
    }
}
