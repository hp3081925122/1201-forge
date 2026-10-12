package org.hp.cindermaw.client;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.entity.CindermawAttack;
import org.hp.cindermaw.entity.CindermawEntity;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;

public final class Phase2GlowLayer extends GeoRenderLayer<CindermawEntity, Void, CindermawRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "textures/entity/cindermaw_phase2.png");
    private static final RenderType GLOW = RenderTypes.eyes(TEXTURE);

    public Phase2GlowLayer(GeoRenderer<CindermawEntity, Void, CindermawRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void submitRenderTask(RenderPassInfo<CindermawRenderState> pass, SubmitNodeCollector collector) {
        CindermawRenderState state = pass.renderState();
        float strength = 0.0F;
        if (state.phase2) {
            strength = 1.0F;
        }
        if (state.attack == CindermawAttack.ROAR) {
            strength = Math.max(strength, Math.min(1.0F, state.attackAge / 26.0F));
        }
        if (state.dying) {
            strength *= Math.max(0.0F, 1.0F - state.deathTime / 40.0F);
        }
        if (strength <= 0.01F) {
            return;
        }
        float pulse = strength * (0.55F + 0.45F * Mth.sin(state.time * 0.16F));
        int channel = Mth.clamp(Math.round(pulse * 255.0F), 0, 255);
        int color = 0xFF000000 | (channel << 16) | (channel << 8) | channel;
        GeoRenderState renderState = (GeoRenderState) state;
        int previous = pass.renderColor();
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, color);
        getRenderer().submitRenderTasks(pass, collector.order(1), GLOW);
        renderState.addGeckolibData(DataTickets.RENDER_COLOR, previous);
    }
}
