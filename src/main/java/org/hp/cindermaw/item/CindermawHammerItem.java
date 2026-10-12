package org.hp.cindermaw.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.hp.cindermaw.entity.MagmaEffect;

import java.util.function.Consumer;

public final class CindermawHammerItem extends Item {
    private static final int CHARGE = 16;
    private static final int COOLDOWN = 80;

    public CindermawHammerItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 7, -3.2F));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.setRemainingFireTicks(100);
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (!(user instanceof Player player) || getUseDuration(stack, player) - remaining < CHARGE) {
            return false;
        }
        player.swing(player.getUsedItemHand(), true);
        if (level.isClientSide()) {
            return true;
        }
        float yaw = player.getYRot();
        Vec3 front = player.position().add(-Math.sin(Math.toRadians(yaw)) * 1.2, 0, Math.cos(Math.toRadians(yaw)) * 1.2);
        var hit = level.clip(new ClipContext(front.add(0, 1.5, 0), front.add(0, -3.0, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3 at = hit.getLocation();
        MagmaEffect.shockwave(player, at, 2.6F, 0);
        MagmaEffect.fissure(player, at, yaw, 3, 7.0F, 9.0F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.5F);
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.hurtAndBreak(3, player, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.cindermaw.cindermaw_hammer.desc").withStyle(ChatFormatting.GOLD));
    }
}
