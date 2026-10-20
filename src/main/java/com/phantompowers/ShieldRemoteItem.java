package com.phantompowers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Shield Remote: right-click Ecto-Shield Emitters to link them, then right-click anywhere to raise or drop the
 * dome. Sneak + right-click in the air to forget every link.
 */
public class ShieldRemoteItem extends Item {
	public static final int RANGE = 160;

	public ShieldRemoteItem(Settings settings) {
		super(settings);
	}

	private static NbtCompound data(ItemStack stack) {
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		return c == null ? new NbtCompound() : c.copyNbt();
	}

	private static void save(ItemStack stack, NbtCompound n) {
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
	}

	public static List<BlockPos> links(ItemStack stack) {
		List<BlockPos> out = new ArrayList<>();
		for (long l : data(stack).getLongArray("Links")) out.add(BlockPos.fromLong(l));
		return out;
	}

	/** Each remote gets its own network id the first time it links something. */
	public static int net(ItemStack stack) {
		NbtCompound n = data(stack);
		if (!n.contains("Net")) {
			n.putInt("Net", (int) (Math.random() * Integer.MAX_VALUE) | 1);
			save(stack, n);
		}
		return n.getInt("Net");
	}

	public static boolean link(ItemStack stack, World world, BlockPos pos) {
		NbtCompound n = data(stack);
		String dim = world.getRegistryKey().getValue().toString();
		List<BlockPos> list = links(stack);
		if (!dim.equals(n.getString("Dim"))) list.clear();
		boolean added;
		if (list.contains(pos)) {
			list.remove(pos);
			added = false;
		} else {
			if (list.size() >= ShieldDomes.MAX_EMITTERS) return false;
			list.add(pos.toImmutable());
			added = true;
		}
		n.putString("Dim", dim);
		n.putLongArray("Links", list.stream().mapToLong(BlockPos::asLong).toArray());
		save(stack, n);
		net(stack);
		return added;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		World w = ctx.getWorld();
		if (!w.getBlockState(ctx.getBlockPos()).isOf(ModBlocks.SHIELD_EMITTER)) return ActionResult.PASS;
		PlayerEntity p = ctx.getPlayer();
		if (!w.isClient && p != null) {
			ItemStack stack = ctx.getStack();
			int before = links(stack).size();
			boolean added = link(stack, w, ctx.getBlockPos());
			int now = links(stack).size();
			if (added) {
				p.sendMessage(Text.literal("Emitter linked (" + now + "/" + ShieldDomes.MAX_EMITTERS + "). Right-click the air to raise the dome.").formatted(Formatting.GREEN), true);
				w.playSound(null, ctx.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
			} else if (now < before) {
				p.sendMessage(Text.literal("Emitter unlinked (" + now + " left).").formatted(Formatting.YELLOW), true);
				w.playSound(null, ctx.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.PLAYERS, 0.8f, 0.8f);
			} else {
				p.sendMessage(Text.literal("This remote is already linked to " + ShieldDomes.MAX_EMITTERS + " emitters.").formatted(Formatting.RED), true);
			}
			// Re-shape a dome that's already up
			int id = net(stack);
			if (ShieldDomes.isUp(id)) ShieldDomes.raise((ServerWorld) w, id, links(stack));
		}
		return ActionResult.success(w.isClient);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack);
		if (user.isSneaking()) {
			NbtCompound n = data(stack);
			n.remove("Links");
			save(stack, n);
			ShieldDomes.lower(world.getServer(), net(stack), true);
			user.sendMessage(Text.literal("All emitter links cleared.").formatted(Formatting.YELLOW), true);
			return TypedActionResult.success(stack);
		}
		toggle((ServerWorld) world, user, stack);
		user.getItemCooldownManager().set(this, 10);
		return TypedActionResult.success(stack);
	}

	/** Press the big green button. */
	public static void toggle(ServerWorld world, PlayerEntity user, ItemStack stack) {
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 0.8f, 1.5f);
		int id = net(stack);
		if (ShieldDomes.isUp(id)) {
			ShieldDomes.lower(world.getServer(), id, true);
			user.sendMessage(Text.literal("Ecto-Shield down.").formatted(Formatting.YELLOW), true);
			return;
		}
		List<BlockPos> links = links(stack);
		if (links.isEmpty()) {
			user.sendMessage(Text.literal("No emitters linked! Right-click Ecto-Shield Emitters with this remote first.").formatted(Formatting.RED), true);
			return;
		}
		if (!world.getRegistryKey().getValue().toString().equals(data(stack).getString("Dim"))) {
			user.sendMessage(Text.literal("Your emitters are in another dimension.").formatted(Formatting.RED), true);
			return;
		}
		BlockPos first = links.get(0);
		if (first.getSquaredDistance(user.getPos()) > (double) RANGE * RANGE) {
			user.sendMessage(Text.literal("Too far away from your emitters (" + RANGE + " block range).").formatted(Formatting.RED), true);
			return;
		}
		ShieldDomes.Dome d = ShieldDomes.raise(world, id, links);
		if (d == null) {
			user.sendMessage(Text.literal("None of the linked emitters are there any more.").formatted(Formatting.RED), true);
		} else {
			user.sendMessage(Text.literal("Ecto-Shield up! (" + d.emitters.size() + " emitters, " + Math.round(d.radius) + " block radius)").formatted(Formatting.GREEN), true);
		}
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return false;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		int n = links(stack).size();
		tooltip.add(Text.literal("Linked emitters: " + n).formatted(n > 0 ? Formatting.GREEN : Formatting.GRAY));
		tooltip.add(Text.literal("Right-click an emitter: link / unlink").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Right-click the air: raise / drop the dome").formatted(Formatting.GRAY));
		tooltip.add(Text.literal("Sneak + right-click the air: clear links").formatted(Formatting.DARK_GRAY));
	}
}
