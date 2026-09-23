package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.VolcanicFireball;

// 弹体、尾迹、落点预警和爆裂火浪统一渲染，不产生原版粒子云。
public final class VolcanicFireballRenderer extends EntityRenderer<VolcanicFireball> {
    public VolcanicFireballRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override
    public ResourceLocation getTextureLocation(VolcanicFireball e) { return VolcanicMeshes.TEXTURE; }

    // 同时检查弹体和落点，避免火球在头顶离开视野时预警也被裁剪。
    @Override
    public boolean shouldRender(VolcanicFireball e, Frustum f, double x, double y, double z) {
        return super.shouldRender(e,f,x,y,z) || e.shouldRender(x,y,z) && f.isVisible(new AABB(e.landing(), e.landing()).inflate(3));
    }

    @Override
    public void render(VolcanicFireball e, float yaw, float partial, PoseStack p, MultiBufferSource buffers, int light) {
        // 等待弹道同步完成，避免首帧在世界原点绘制预警。
        if (!e.ready()) return;
        Vec3 interpolated = new Vec3(e.xo + (e.getX()-e.xo)*partial, e.yo+(e.getY()-e.yo)*partial, e.zo+(e.getZ()-e.zo)*partial);
        Vec3 landing = e.landing().subtract(interpolated);
        float age = e.age(partial);
        if (!e.impacted()) {
            // 飞行前期不显示落点，最后十二刻逐渐点亮裂纹圆环。
            if (age >= 20) {
                float progress = Math.min(1,(age-20)/12);
                VolcanicMeshes.ring(p,buffers,landing.add(0,0.025,0),2.0,0.08+progress*0.12,0,0xFF792B,0.5F+progress*0.5F);
                VolcanicMeshes.ring(p,buffers,landing.add(0,0.035,0),2.0*(1-progress),0.055,0,0xFFD064,0.85F);
            }
            // 尾迹沿已走过的曲线逐段收细，不会穿过弯曲弹道的内侧。
            Vec3 at = e.sample(age).subtract(interpolated);
            for (int i=1;i<=7;i++) {
                Vec3 tail=e.sample(Math.max(0,age-i*0.7F)).subtract(interpolated);
                VolcanicMeshes.box(p,buffers,tail,0.24*(1-i/9.0),i<3?0xFFCA55:0xE54B16,(8-i)/9F);
            }
            p.pushPose(); p.translate(at.x,at.y,at.z);
            p.mulPose(Axis.YP.rotationDegrees(age*13)); p.mulPose(Axis.XP.rotationDegrees(age*9));
            VolcanicMeshes.box(p,buffers,Vec3.ZERO,0.26,0xFF9A28,1);
            // 六块不相连的岩壳露出内部熔核，轮廓保留像素风的棱角。
            for (int i=0;i<6;i++) {
                double sign=i%2==0?1:-1;
                Vec3 shell=i<2?new Vec3(sign*0.25,0,0):i<4?new Vec3(0,sign*0.25,0):new Vec3(0,0,sign*0.25);
                VolcanicMeshes.box(p,buffers,shell,0.15,i%2==0?0x44241E:0x291C1B,1);
            }
            p.popPose();
        } else {
            float t=Math.min(1,e.impactAge(partial)/VolcanicFireball.FADE_TICKS);
            float fade=1-t;
            // 同一命中实体绘制向外展开的低矮火浪和逐渐熄灭的焦痕。
            VolcanicMeshes.ring(p,buffers,landing,0.25+2.2*t,0.22,0.35*fade,0xFF6E20,fade);
            VolcanicMeshes.ring(p,buffers,landing.add(0,0.012,0),1.6,1.55,0,0x341A17,fade*0.7F);
            if(t<0.3F) VolcanicMeshes.jet(p,buffers,landing,1.4*(1-t/0.3),1-t/0.3F,age);
            // 六个有限碎块采用解析抛物线，生命周期与主体一致。
            for(int i=0;i<6;i++) {
                double a=i*Math.PI/3;
                Vec3 chip=landing.add(Math.cos(a)*t*2,Math.max(0,2.4*t-3*t*t),Math.sin(a)*t*2);
                VolcanicMeshes.box(p,buffers,chip,0.09*fade,0xD95220,fade);
            }
        }
        super.render(e,yaw,partial,p,buffers,light);
    }
}
