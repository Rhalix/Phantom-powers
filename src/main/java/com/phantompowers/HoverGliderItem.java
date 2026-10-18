package com.phantompowers;

import com.phantompowers.entity.GliderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** Place the Hover Glider on the ground, then right-click it to hop on. */
public class HoverGliderItem extends Item {
	public HoverGliderItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World world = ctx.getWorld();
		if (world.isClient) return ActionResult.SUCCESS;
		Vec3d at = ctx.getHitPos();
		GliderEntity glider = ModEntities.GLIDER.create(world);
		if (glider == null) return ActionResult.FAIL;
		float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYaw() : 0f;
		glider.refreshPositionAndAngles(at.x, at.y + 0.3, at.z, yaw, 0f);
		var cd = ctx.getStack().get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
		if (cd != null) glider.tier = cd.copyNbt().getInt("GliderTier");
		((ServerWorld) world).spawnEntity(glider);
		if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().creativeMode) ctx.getStack().decrement(1);
		return ActionResult.CONSUME;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Place it, then right-click to ride").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Flies where you look. Right-click fires lasers.").formatted(Formatting.DARK_GRAY));
		tooltip.add(Text.literal("Sneak + right-click it to pick it up").formatted(Formatting.DARK_GRAY));
	}
}
