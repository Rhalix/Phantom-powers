package com.phantompowers;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

/** The DNA Sampler: right-click to take a sample of your own ghost DNA (a reusable tool). The filled sample goes into a Cloning Pod. */
public class DnaSamplerItem extends Item {
	private final boolean filled;

	public DnaSamplerItem(boolean filled, Settings settings) {
		super(settings);
		this.filled = filled;
	}

	public static UUID owner(ItemStack s) {
		NbtCompound n = s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
		return n.containsUuid("DnaOwner") ? n.getUuid("DnaOwner") : null;
	}

	public static String ownerName(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getString("DnaName");
	}

	public static boolean darkDna(ItemStack s) {
		return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getBoolean("DnaDark");
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (filled) return TypedActionResult.pass(stack);
		if (!world.isClient && user instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
			GhostData d = GhostState.of(sp.getServer()).data(sp.getUuid());
			boolean dark = d.dark && d.darkMode;
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1f, 1.4f);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, dark ? 0.6f : 1.4f);
			ItemStack sample = new ItemStack(ModItems.DNA_SAMPLE);
			NbtCompound n = new NbtCompound();
			n.putUuid("DnaOwner", user.getUuid());
			n.putString("DnaName", user.getName().getString());
			n.putBoolean("DnaDark", dark);
			sample.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
			// The sampler is a tool: it wears down a little each time instead of being used up
			stack.damage(1, sp, net.minecraft.entity.LivingEntity.getSlotForHand(hand));
			if (!user.giveItemStack(sample)) user.dropItem(sample, false);
			((net.minecraft.server.world.ServerWorld) world).spawnParticles(dark ? new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(1f, 0.31f, 0.78f), 1.2f) : GhostPowers.GREEN,
					user.getX(), user.getY() + 1, user.getZ(), 20, 0.3, 0.5, 0.3, 0);
			user.sendMessage(Text.literal(dark ? "You took a sample of your evil ghost DNA. Put it in a Cloning Pod!"
					: "You took a sample of your ghost DNA. Put it in a Cloning Pod!").formatted(dark ? Formatting.LIGHT_PURPLE : Formatting.GREEN), true);
		}
		return TypedActionResult.success(stack, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		if (!filled) {
			tooltip.add(Text.literal("Right-click to sample your ghost DNA").formatted(Formatting.GRAY));
			tooltip.add(Text.literal("Reusable. Clones always come out in ghost form").formatted(Formatting.DARK_GRAY));
			return;
		}
		String name = ownerName(stack);
		tooltip.add(Text.literal("Ghost DNA of: " + (name.isEmpty() ? "unknown" : name)).formatted(darkDna(stack) ? Formatting.LIGHT_PURPLE : Formatting.GREEN));
		if (darkDna(stack)) tooltip.add(Text.literal("Evil half-ghost DNA").formatted(Formatting.DARK_PURPLE));
		tooltip.add(Text.literal("Use it on a Cloning Pod").formatted(Formatting.GRAY));
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return filled;
	}
}
