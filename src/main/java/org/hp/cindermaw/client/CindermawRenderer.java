package org.hp.cindermaw.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.hp.cindermaw.entity.CindermawEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

// 使用基础贴图和同 UV 发光遮罩绘制炉心、裂缝、眼睛与排焰口。
public final class CindermawRenderer extends GeoEntityRenderer<CindermawEntity> {
    public CindermawRenderer(EntityRendererProvider.Context context) {
        super(context, new CindermawModel());
        shadowRadius = 1.1F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    // 骨骼矩阵驱动喷流起点，让前倾和各喷口后坐都反映到特效上。
    @Override
    public void render(CindermawEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        for (int i=0;i<3;i++) getGeoModel().getBone("vent_"+i+"_flame").ifPresent(b->b.setTrackingMatrices(true));
        super.render(entity,yaw,partial,pose,buffers,light);
        int age=entity.getVolleyAge();
        if(age<0 || !entity.isAlive()) return;
        for(int i=0;i<3;i++) {
            var bone=getGeoModel().getBone("vent_"+i+"_flame").orElse(null);
            if(bone==null) continue;
            Vector4f origin=bone.getLocalSpaceMatrix().transform(new Vector4f(0,0,0,1));
            Vec3 start=new Vec3(origin.x(),origin.y(),origin.z());
            float pulse=0;
            for(int shot=i;shot<8;shot+=3) {
                float since=age+partial-(16+shot*6);
                if(since>=0 && since<4) pulse=Math.max(pulse,1-since/4);
            }
            float charge=age<16?(age+partial)/16F:Math.max(0,(76-age-partial)/12F);
            VolcanicMeshes.jet(pose,buffers,start,0.15+Math.min(1,charge)*0.25+pulse*1.35,0.9F,age+partial+i*5);
            if(pulse>0) VolcanicMeshes.ring(pose,buffers,start,0.2+(1-pulse)*0.6,0.055,0,0xFFC774,pulse);
        }
    }
}

