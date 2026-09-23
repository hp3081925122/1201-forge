package org.hp.cindermaw;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.hp.cindermaw.entity.CindermawEntity;
import org.hp.cindermaw.entity.VolcanicFireball;
import org.hp.cindermaw.entity.GroundEruption;

// 集中管理熔渊炉心实体与刷怪蛋，公共注册类不引用客户端渲染代码。
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Cindermaw.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Cindermaw.MOD_ID);

    // 碰撞体覆盖炉心主体，宽度和高度按当前 52.6×54.5 模型单位折算。
    public static final RegistryObject<EntityType<CindermawEntity>> CINDERMAW = ENTITIES.register("cindermaw",
            () -> EntityType.Builder.of(CindermawEntity::new, MobCategory.MONSTER)
                    .sized(3.3F, 3.5F).clientTrackingRange(10).updateInterval(3)
                    .build(Cindermaw.MOD_ID + ":cindermaw"));

    // 火球使用同一实体承载飞行和短暂落地特效。
    public static final RegistryObject<EntityType<VolcanicFireball>> VOLCANIC_FIREBALL = ENTITIES.register("volcanic_fireball",
            () -> EntityType.Builder.<VolcanicFireball>of(VolcanicFireball::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(12).updateInterval(1)
                    .build(Cindermaw.MOD_ID + ":volcanic_fireball"));

    // 震地与十二路喷火共用一个短寿命实体。
    public static final RegistryObject<EntityType<GroundEruption>> GROUND_ERUPTION = ENTITIES.register("ground_eruption",
            () -> EntityType.Builder.<GroundEruption>of(GroundEruption::new, MobCategory.MISC)
                    .sized(0.1F,0.1F).clientTrackingRange(12).updateInterval(2).build(Cindermaw.MOD_ID+":ground_eruption"));

    // 刷怪蛋颜色对应玄武岩主体与炉心橙焰，便于在创造栏识别。
    public static final RegistryObject<Item> CINDERMAW_SPAWN_EGG = ITEMS.register("cindermaw_spawn_egg",
            () -> new ForgeSpawnEggItem(CINDERMAW, 0x252933, 0xE65A1B, new Item.Properties()));

    private ModEntities() {
    }
}
