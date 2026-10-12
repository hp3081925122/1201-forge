package org.hp.cindermaw.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.ModEntities;
import org.hp.cindermaw.ModParticles;

// 客户端注册只在 Forge 客户端事件总线加载，服务器不会解析渲染类。
@Mod.EventBusSubscriber(modid = Cindermaw.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CINDERMAW.get(), CindermawRenderer::new);
        event.registerEntityRenderer(ModEntities.LAVA_BOMB.get(), LavaBombRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGMA_EFFECT.get(), MagmaEffectRenderer::new);
    }

    // 五种像素粒子分别绑定行为样式
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.EMBER.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.EMBER));
        event.registerSpriteSet(ModParticles.CINDER_SMOKE.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.SMOKE));
        event.registerSpriteSet(ModParticles.BLAZE_FLAME.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLAME));
        event.registerSpriteSet(ModParticles.MAGMA_CHUNK.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.CHUNK));
        event.registerSpriteSet(ModParticles.HEAT_FLASH.get(), sprites -> new CinderParticle.Provider(sprites, CinderParticle.Style.FLASH));
    }

    private ClientEvents() {
    }
}
