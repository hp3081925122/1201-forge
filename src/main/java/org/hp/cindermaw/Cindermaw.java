package org.hp.cindermaw;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.hp.cindermaw.entity.CindermawEntity;

@Mod(Cindermaw.MOD_ID)
public final class Cindermaw {
    public static final String MOD_ID = "cindermaw";

    public Cindermaw(FMLJavaModLoadingContext context) {
        // 注册实体、物品与粒子类型
        var bus = context.getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModEntities.ITEMS.register(bus);
        ModParticles.PARTICLES.register(bus);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::fillCreativeTab);
    }

    // 为 Boss 注册服务端属性，确保刷怪蛋和命令召唤拥有完整属性。
    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CINDERMAW.get(), CindermawEntity.createAttributes().build());
    }

    // 仅把刷怪蛋放入生成蛋页。
    private void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModEntities.CINDERMAW_SPAWN_EGG);
        }
    }
}
