package com.phantompowers;

import com.phantompowers.entity.SkimmerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** The Ecto-Skimmer, folded up. Place it on the ground (it's built for the Ghost Zone), then right-click to board. */
public class EctoSkimmerItem extends Item {
	public EctoSkimmerItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		if (world.isClient) return ActionResult.SUCCESS;
		Vec3d at = ctx.getHitPos();
		SkimmerEntity ship = ModEntities.SKIMMER.create(world);
		if (ship == null) return ActionResult.FAIL;
		float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYaw() : 0f;
		ship.refreshPositionAndAngles(at.x, at.y + 0.05, at.z, yaw, 0f);
		Box box = ship.getBoundingBox();
		if (!world.isSpaceEmpty(ship, box.contract(0.05))) {
			if (ctx.getPlayer() != null) ctx.getPlayer().sendMessage(Text.literal("Not enough room to unfold the Skimmer here.").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}
		var cd = ctx.getStack().get(DataComponentTypes.CUSTOM_DATA);
		if (cd != null) {
			NbtCompound n = cd.copyNbt();
			ship.setUpgrades(n.getInt("Upgrades"));
			if (n.contains("Hull")) ship.setHealth(n.getFloat("Hull"));
		}
		((ServerWorld) world).spawnEntity(ship);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.NEUTRAL, 1f, 0.7f);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.NEUTRAL, 1f, 0.6f);
		if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().creativeMode) ctx.getStack().decrement(1);
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Place it, then right-click to climb in").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Scans for ghost lairs in the Ghost Zone").formatted(Formatting.DARK_GRAY));
		var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
		int ups = cd == null ? 0 : cd.copyNbt().getInt("Upgrades");
		for (int i = 0; i < SkimmerEntity.UP_NAMES.length; i++) {
			if ((ups & (1 << i)) != 0) tooltip.add(Text.literal("+ " + SkimmerEntity.UP_NAMES[i]).formatted(Formatting.AQUA));
		}
		if (cd != null && cd.copyNbt().contains("Hull")) {
			tooltip.add(Text.literal("Hull: " + Math.round(cd.copyNbt().getFloat("Hull") / SkimmerEntity.MAX_HEALTH * 100) + "%").formatted(Formatting.GRAY));
		}
		tooltip.add(Text.literal("Sneak + right-click it to fold it back up").formatted(Formatting.DARK_GRAY));
	}
}
