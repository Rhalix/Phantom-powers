package com.phantompowers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Paint jobs and upgrades for ghost-hunter gear, done at the Hunter Workbench. Everything is stored on the item
 * itself: "Paint" (a colour, or none) and an upgrade level (weapons: WeaponTier, suit: SuitTier, glider: GliderTier).
 */
public final class HunterPaint {
	public static final int MAX_TIER = 3;

	private HunterPaint() {}

	public static boolean paintable(ItemStack s) {
		return s.isOf(ModItems.GHOST_BLASTER) || s.isOf(ModItems.ECTO_LAUNCHER) || s.isOf(ModItems.THERMOS)
				|| s.isOf(ModItems.HUNTER_SUIT) || s.isOf(ModItems.HOVER_GLIDER) || s.isOf(ModItems.ECTO_SWORD);
	}

	public static boolean upgradable(ItemStack s) {
		return s.isOf(ModItems.GHOST_BLASTER) || s.isOf(ModItems.ECTO_LAUNCHER) || s.isOf(ModItems.HUNTER_SUIT) || s.isOf(ModItems.HOVER_GLIDER) || s.isOf(ModItems.ECTO_SWORD);
	}

	private static NbtCompound data(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
	}

	private static void put(ItemStack s, NbtCompound n) {
		s.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
	}

	/** The paint colour (0xRRGGBB), or -1 for the factory finish. */
	public static int paint(ItemStack s) {
		NbtCompound n = data(s);
		return n.contains("Paint") ? n.getInt("Paint") & 0xFFFFFF : -1;
	}

	public static String tierKey(ItemStack s) {
		return s.isOf(ModItems.HUNTER_SUIT) ? "SuitTier" : s.isOf(ModItems.HOVER_GLIDER) ? "GliderTier" : "WeaponTier";
	}

	public static int tier(ItemStack s) {
		return Math.min(MAX_TIER, data(s).getInt(tierKey(s)));
	}

	/** Damage multiplier from a weapon's upgrade level. */
	public static float weaponBoost(ItemStack s) {
		return 1f + 0.25f * Math.min(MAX_TIER, data(s).getInt("WeaponTier"));
	}

	/** What the next upgrade costs: {item, count} pairs. */
	public static Object[][] cost(int nextTier) {
		return switch (nextTier) {
			case 1 -> new Object[][]{{Items.IRON_INGOT, 8}, {ModItems.ECTOPLASM, 4}};
			case 2 -> new Object[][]{{Items.DIAMOND, 4}, {ModItems.ECTOPLASM, 8}};
			default -> new Object[][]{{Items.NETHERITE_INGOT, 1}, {Items.DIAMOND, 4}, {ModItems.ECTOPLASM, 16}};
		};
	}

	public static String costText(int nextTier) {
		StringBuilder b = new StringBuilder();
		for (Object[] c : cost(nextTier)) {
			if (b.length() > 0) b.append(", ");
			b.append(c[1]).append("x ").append(((Item) c[0]).getName().getString());
		}
		return b.toString();
	}

	private static int count(PlayerEntity p, Item item) {
		int n = 0;
		for (int i = 0; i < p.getInventory().size(); i++) {
			ItemStack s = p.getInventory().getStack(i);
			if (s.isOf(item)) n += s.getCount();
		}
		return n;
	}

	private static void take(PlayerEntity p, Item item, int amount) {
		for (int i = 0; i < p.getInventory().size() && amount > 0; i++) {
			ItemStack s = p.getInventory().getStack(i);
			if (!s.isOf(item)) continue;
			int t = Math.min(amount, s.getCount());
			s.decrement(t);
			amount -= t;
		}
	}

	/** Workbench actions sent by the client: 0 = paint (value = colour, or -1 to strip it), 1 = upgrade. */
	public static void handle(ServerPlayerEntity p, BlockPos bench, int action, int value) {
		if (!p.getWorld().getBlockState(bench).isOf(ModBlocks.HUNTER_WORKBENCH) || p.squaredDistanceTo(Vec3d.ofCenter(bench)) > 64) return;
		ItemStack s = p.getMainHandStack();
		if (action == 0 && paintable(s)) {
			NbtCompound n = data(s);
			if (value < 0) n.remove("Paint");
			else n.putInt("Paint", value & 0xFFFFFF);
			put(s, n);
			p.getWorld().playSound(null, bench, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1f, 1f);
			p.getWorld().playSound(null, bench, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.BLOCKS, 0.8f, 1.2f);
			if (s.isOf(ModItems.HUNTER_SUIT)) GhostPowers.broadcastStates(p.getServer());
			return;
		}
		if (action == 1 && upgradable(s)) {
			int t = tier(s);
			if (t >= MAX_TIER) {
				p.sendMessage(Text.literal("That's already fully upgraded!").formatted(Formatting.YELLOW), true);
				return;
			}
			for (Object[] c : cost(t + 1)) {
				if (!p.isCreative() && count(p, (Item) c[0]) < (int) c[1]) {
					p.sendMessage(Text.literal("You need " + costText(t + 1) + ".").formatted(Formatting.RED), true);
					return;
				}
			}
			if (!p.isCreative()) for (Object[] c : cost(t + 1)) take(p, (Item) c[0], (int) c[1]);
			NbtCompound n = data(s);
			n.putInt(tierKey(s), t + 1);
			put(s, n);
			if (s.isOf(ModItems.HUNTER_SUIT)) HunterSuit.reapply(p, GhostState.of(p.getServer()).data(p.getUuid()));
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1f, 1f);
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.6f, 1.4f);
			p.sendMessage(Text.literal(s.getName().getString() + " upgraded to level " + (t + 1) + "!").formatted(Formatting.AQUA), true);
		}
	}
}
