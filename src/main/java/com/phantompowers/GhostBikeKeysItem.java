package com.phantompowers;

import com.phantompowers.entity.GhostBikeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Rev's Bike Keys (dropped by Rev): use them on the ground and his black ghost bike appears for you to ride. */
public class GhostBikeKeysItem extends Item {
	public GhostBikeKeysItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		if (!(ctx.getWorld() instanceof ServerWorld w)) return ActionResult.SUCCESS;
		Vec3d at = ctx.getHitPos();
		GhostBikeEntity bike = ModEntities.GHOST_BIKE.create(w);
		if (bike == null) return ActionResult.FAIL;
		float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYaw() : 0f;
		bike.refreshPositionAndAngles(at.x, at.y + 0.1, at.z, yaw, 0f);
		w.spawnEntity(bike);
		w.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.5, at.z, 30, 0.8, 0.4, 0.8, 0.03);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.0f, 1.8f);
		if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().creativeMode) ctx.getStack().decrement(1);
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Use on the ground to summon Johnny 13's ghost bike").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Steers where you look. Sprint = nitro, jump = hop").formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.literal("Sneak + right-click the bike to pocket it").formatted(Formatting.DARK_GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}
}
