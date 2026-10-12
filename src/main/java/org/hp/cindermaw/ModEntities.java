package org.hp.cindermaw;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.hp.cindermaw.entity.CindermawEntity;
import org.hp.cindermaw.entity.LavaBomb;
import org.hp.cindermaw.entity.MagmaEffect;
import org.hp.cindermaw.item.CindermawArmorMaterial;
import org.hp.cindermaw.item.CindermawCoreItem;
import org.hp.cindermaw.item.CindermawHammerItem;

// 集中注册 Boss、熔岩弹与地面特效实体，公共注册类不引用客户端代码。
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Cindermaw.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Cindermaw.MOD_ID);

    // 碰撞体覆盖撑地后的双臂与躯干，免疫火焰与岩浆。
    public static final DeferredHolder<EntityType<?>, EntityType<CindermawEntity>> CINDERMAW = ENTITIES.register("cindermaw",
            () -> EntityType.Builder.of(CindermawEntity::new, MobCategory.MONSTER)
                    .sized(3.2F, 3.3F).fireImmune().clientTrackingRange(10).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "cindermaw"))));

    // 火山齐射的熔岩弹，弹道由服务端一次性确定。
    public static final DeferredHolder<EntityType<?>, EntityType<LavaBomb>> LAVA_BOMB = ENTITIES.register("lava_bomb",
            () -> EntityType.Builder.<LavaBomb>of(LavaBomb::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).fireImmune().clientTrackingRange(12).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "lava_bomb"))));

    // 冲击环、熔岩裂缝、喷泉环、熔岩池与新星火墙共用一个短寿命实体。
    public static final DeferredHolder<EntityType<?>, EntityType<MagmaEffect>> MAGMA_EFFECT = ENTITIES.register("magma_effect",
            () -> EntityType.Builder.<MagmaEffect>of(MagmaEffect::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).fireImmune().clientTrackingRange(12).updateInterval(5)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "magma_effect"))));

    // 熔渊炉核：召唤 Boss 的消耗品，防火、史诗稀有度
    public static final DeferredHolder<Item, Item> CINDERMAW_CORE = ITEMS.registerItem("cindermaw_core",
            properties -> new CindermawCoreItem(properties.stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    // 熔渊之心：Boss 必掉的核心材料
    public static final DeferredHolder<Item, Item> CINDERMAW_HEART = ITEMS.registerItem("cindermaw_heart",
            properties -> new Item(properties.rarity(Rarity.EPIC).fireResistant()));

    // 熔渊锤：重型近战武器，蓄力砸地
    public static final DeferredHolder<Item, Item> CINDERMAW_HAMMER = ITEMS.registerItem("cindermaw_hammer",
            properties -> new CindermawHammerItem(properties.rarity(Rarity.EPIC).fireResistant()));

    // 炉心胸甲：下界合金胸甲锻造升级
    public static final DeferredHolder<Item, Item> CINDERMAW_CHESTPLATE = ITEMS.registerItem("cindermaw_chestplate",
            properties -> new Item(properties.humanoidArmor(CindermawArmorMaterial.MATERIAL, ArmorType.CHESTPLATE).rarity(Rarity.EPIC).fireResistant()));

    private ModEntities() {
    }
}
