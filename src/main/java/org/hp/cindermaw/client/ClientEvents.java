package org.hp.cindermaw.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.ModEntities;

// 客户端专用注册只在 Forge 客户端事件总线加载，服务器不会解析渲染类型。
@Mod.EventBusSubscriber(modid = Cindermaw.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CINDERMAW.get(), CindermawRenderer::new);
        event.registerEntityRenderer(ModEntities.VOLCANIC_FIREBALL.get(), VolcanicFireballRenderer::new);
        event.registerEntityRenderer(ModEntities.GROUND_ERUPTION.get(), GroundEruptionRenderer::new);
    }

    private ClientEvents() {
    }
}
