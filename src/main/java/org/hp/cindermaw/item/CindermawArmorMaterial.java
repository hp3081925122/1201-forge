package org.hp.cindermaw.item;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.hp.cindermaw.Cindermaw;
import org.hp.cindermaw.ModEntities;

public final class CindermawArmorMaterial {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Cindermaw.MOD_ID);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> INSTANCE = MATERIALS.register("cindermaw", () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.CHESTPLATE, 9),
            16,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(ModEntities.CINDERMAW_HEART.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Cindermaw.MOD_ID, "cindermaw"))),
            3.5F,
            0.15F));

    private CindermawArmorMaterial() {
    }
}
