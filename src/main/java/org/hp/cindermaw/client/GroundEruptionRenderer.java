package org.hp.cindermaw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.GroundEruption;

// 落地先抬起环形岩块，裂口预热后喷火，最终岩块回落与火焰消散。
public final class GroundEruptionRenderer extends EntityRenderer<GroundEruption> {
    public GroundEruptionRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override
    public ResourceLocation getTextureLocation(GroundEruption e) { return VolcanicMeshes.TEXTURE; }
    @Override
    public boolean shouldRender(GroundEruption e,Frustum f,double x,double y,double z) {
        return e.shouldRender(x,y,z) && f.isVisible(e.getBoundingBox().inflate(6,4,6));
    }
    @Override
    public void render(GroundEruption e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light) {
        if(!e.ready()) return;
        float age=e.age(partial),fade=Math.max(0,Math.min(1,(48-age)/12));
        // 最初八刻展开低矮冲击环，持续时间与岩块首次上抛同步。
        if(age<10) VolcanicMeshes.ring(p,b,Vec3.ZERO,0.4+age*0.45,0.23,0.15,0xFFB657,1-age/10);
        for(int i=0;i<12;i++) {
            if(!e.hasVent(i)) continue;
            Vec3 at=e.vent(i);
            float local=Math.max(0,age-(i%2)*2);
            double lift=Math.max(0,1.8*Math.sin(Math.min(1,local/28)*Math.PI));
            // 十二块有厚度的玄武岩随冲击抬起并翻转，底部露出熔红断面。
            if(local<28) {
                p.pushPose();p.translate(at.x,at.y+lift+0.1,at.z);
                p.mulPose(Axis.YP.rotationDegrees(i*30));p.mulPose(Axis.ZP.rotationDegrees((float)(lift*22)));
                p.scale(1.25F,0.55F,0.9F);
                VolcanicMeshes.box(p,b,Vec3.ZERO,0.42,i%2==0?0x3E4148:0x292C34,fade);
                VolcanicMeshes.box(p,b,new Vec3(0,-0.28,0),0.3,0xEF5718,fade);
                p.popPose();
            }
            // 火焰从岩块下方的裂口喷出，十四刻之前只有地面预热。
            VolcanicMeshes.ring(p,b,at,0.72,0.12,0,age<14?0xD6501A:0xFFB33B,fade);
            if(age>=14 && age<40) {
                float ramp=Math.min(1,(age-14)/3),decay=Math.min(1,(40-age)/7);
                double height=(2.4+0.45*Math.sin(age*1.7+i))*ramp*decay;
                p.pushPose();p.translate(at.x,at.y,at.z);p.scale(2.3F,1,2.3F);
                VolcanicMeshes.jet(p,b,Vec3.ZERO,height,0.95F*decay,age+i*3);
                p.popPose();
            }
        }
        super.render(e,yaw,partial,p,b,light);
    }
}
