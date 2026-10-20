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

	/**
	 * What the next upgrade costs: {item, count} pairs. Upgrades need ghost materials: Ghost Cores (dropped by
	 * ghosts you defeat), Spectral Shards (mined from Spectral Crystal in the Ghost Zone) and, for the top level,
	 * a Phantom Heart (dropped by ghost bosses).
	 */
	public static Object[][] cost(int nextTier) {
		return switch (nextTier) {
			case 1 -> new Object[][]{{ModItems.GHOST_CORE, 3}, {ModItems.ECTOPLASM, 6}, {Items.IRON_INGOT, 6}};
			case 2 -> new Object[][]{{ModItems.GHOST_CORE, 6}, {ModItems.SPECTRAL_SHARD, 6}, {ModItems.ECTOPLASM, 10}, {Items.DIAMOND, 2}};
			default -> new Object[][]{{ModItems.PHANTOM_HEART, 1}, {ModItems.GHOST_CORE, 10}, {ModItems.SPECTRAL_SHARD, 10}, {ModItems.ECTOPLASM, 16}};
		};
	}

	/** Suit gadgets built at the bench: 0 = wrist missiles, 1 = backpack cannons. */
	public static Object[][] gadgetCost(int gadget) {
		return gadget == 0
				? new Object[][]{{ModItems.GHOST_CORE, 5}, {ModItems.SPECTRAL_SHARD, 3}, {Items.TNT, 2}, {Items.IRON_INGOT, 4}}
				: new Object[][]{{ModItems.GHOST_CORE, 8}, {ModItems.SPECTRAL_SHARD, 6}, {Items.REDSTONE_BLOCK, 2}, {Items.IRON_BLOCK, 1}};
	}

	public static String costText(Object[][] cost) {
		StringBuilder b = new StringBuilder();
		for (Object[] c : cost) {
			if (b.length() > 0) b.append(", ");
			b.append(c[1]).append("x ").append(((Item) c[0]).getName().getString());
		}
		return b.toString();
	}

	private static boolean pay(PlayerEntity p, Object[][] cost) {
		if (p.isCreative()) return true;
		for (Object[] c : cost) if (count(p, (Item) c[0]) < (int) c[1]) {
			p.sendMessage(Text.literal("You need " + costText(cost) + ".").formatted(Formatting.RED), true);
			return false;
		}
		for (Object[] c : cost) take(p, (Item) c[0], (int) c[1]);
		return true;
	}

	public static String costText(int nextTier) {
		return costText(cost(nextTier));
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

	/** Workbench actions sent by the client: 0 = paint (value = colour, or -1 to strip it), 1 = upgrade, 2 = install a suit gadget (value). */
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
		if (action == 2 && s.isOf(ModItems.HUNTER_SUIT)) {
			String key = value == 0 ? "WristMissiles" : "BackpackCannons";
			NbtCompound n = data(s);
			if (n.getBoolean(key)) {
				p.sendMessage(Text.literal("Your suit already has that.").formatted(Formatting.YELLOW), true);
				return;
			}
			if (!pay(p, gadgetCost(value))) return;
			n.putBoolean(key, true);
			put(s, n);
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1f, 0.9f);
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.8f, 1.3f);
			p.sendMessage(Text.literal((value == 0 ? "Wrist missiles" : "Backpack cannons") + " built into your suit! Put them in a Power Slot (Phantom Menu > Slots).")
					.formatted(Formatting.AQUA), false);
			return;
		}
		if (action == 1 && upgradable(s)) {
			int t = tier(s);
			if (t >= MAX_TIER) {
				p.sendMessage(Text.literal("That's already fully upgraded!").formatted(Formatting.YELLOW), true);
				return;
			}
			if (!pay(p, cost(t + 1))) return;
			NbtCompound n = data(s);
			n.putInt(tierKey(s), t + 1);
			put(s, n);
			if (s.isOf(ModItems.HUNTER_SUIT)) HunterSuit.reapply(p, GhostState.of(p.getServer()).data(p.getUuid()));
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1f, 1f);
			p.getWorld().playSound(null, bench, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.6f, 1.4f);
			p.sendMessage(Text.literal(s.getName().getString() + " upgraded to level " + (t + 1) + "!").formatted(Formatting.AQUA), true);
		}
	}

	/** Ghosts drop Ghost Cores when you defeat them; bosses also drop a Phantom Heart. */
	public static void dropMaterials(net.minecraft.entity.LivingEntity victim, net.minecraft.entity.damage.DamageSource source) {
		if (!(victim instanceof com.phantompowers.entity.AbstractGhostEntity) && !FrostLogic.isBoss(victim)) return;
		if (victim instanceof com.phantompowers.entity.FriendlyGhostEntity || victim instanceof com.phantompowers.entity.GhostCloneEntity) return;
		if (!(source.getAttacker() instanceof PlayerEntity)) return;
		var r = victim.getRandom();
		if (FrostLogic.isBoss(victim)) {
			victim.dropStack(new ItemStack(ModItems.PHANTOM_HEART));
			victim.dropStack(new ItemStack(ModItems.GHOST_CORE, 3 + r.nextInt(3)));
		} else if (r.nextFloat() < 0.4f) {
			victim.dropStack(new ItemStack(ModItems.GHOST_CORE, 1 + (r.nextInt(4) == 0 ? 1 : 0)));
		}
	}
}
