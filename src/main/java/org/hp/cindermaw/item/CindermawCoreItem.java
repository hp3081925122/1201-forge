package org.hp.cindermaw.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.hp.cindermaw.ModEntities;
import org.hp.cindermaw.entity.CindermawEntity;

import java.util.function.Consumer;

public final class CindermawCoreItem extends Item {
    public CindermawCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        CindermawEntity boss = ModEntities.CINDERMAW.get().create(server, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return InteractionResult.FAIL;
        }
        float yaw = 0;
        if (context.getPlayer() != null) {
            yaw = context.getPlayer().getYRot() + 180.0F;
        }
        boss.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        if (!server.noCollision(boss)) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable("item.cindermaw.cindermaw_core.no_space").withStyle(ChatFormatting.RED));
            }
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
            return InteractionResult.FAIL;
        }
        boss.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
        boss.beginEntrance();
        server.addFreshEntity(boss);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.cindermaw.cindermaw_core.desc").withStyle(ChatFormatting.GOLD));
    }
}
