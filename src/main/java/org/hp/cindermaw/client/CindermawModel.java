package org.hp.cindermaw.client;

import net.minecraft.resources.ResourceLocation;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.entity.CindermawEntity;
import software.bernie.geckolib.model.GeoModel;

// GeckoLib 资源路径与 Blockbench 导出的几何、像素贴图保持一一对应。
public final class CindermawModel extends GeoModel<CindermawEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(Cindermaw.MOD_ID, "geo/cindermaw.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/entity/cindermaw.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(Cindermaw.MOD_ID, "animations/cindermaw.animation.json");

    @Override
    public ResourceLocation getModelResource(CindermawEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(CindermawEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(CindermawEntity entity) {
        return ANIMATION;
    }
}
