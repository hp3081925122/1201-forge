package org.hp.cindermaw.client;

import net.minecraft.resources.Identifier;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.entity.CindermawEntity;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

public final class CindermawModel extends GeoModel<CindermawEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "cindermaw");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/entity/cindermaw.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "cindermaw");

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CindermawEntity entity) {
        return ANIMATION;
    }
}
