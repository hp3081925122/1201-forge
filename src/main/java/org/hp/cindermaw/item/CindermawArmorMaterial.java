package org.hp.cindermaw.item;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.hp.cindermaw.Cindermaw;

public final class CindermawArmorMaterial {
    public static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "repairs_cindermaw_armor"));
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Cindermaw.MOD_ID, "cindermaw"));
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(
            37,
            Map.of(ArmorType.CHESTPLATE, 9),
            16,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.5F,
            0.15F,
            REPAIR,
            ASSET);

    private CindermawArmorMaterial() {
    }
}
