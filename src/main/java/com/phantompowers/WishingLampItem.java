package com.phantompowers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

/** The Wishkeeper's lamp. Rub it (right-click) for a wish: usually a blessing... but genies are tricky. */
public class WishingLampItem extends Item {
	private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1f, 0.82f, 0.25f), 1.2f);

	public WishingLampItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) return TypedActionResult.success(stack, true);
		grantWish((ServerPlayerEntity) user, world.random.nextInt(100) < 20);
		stack.damage(1, user, LivingEntity.getSlotForHand(hand));
		user.getItemCooldownManager().set(this, 600);
		return TypedActionResult.success(stack);
	}

	/** Grants a random wish. {@code twisted} wishes go wrong in a funny way. */
	public static void grantWish(ServerPlayerEntity p, boolean twisted) {
		ServerWorld w = p.getServerWorld();
		var r = w.random;
		String msg;
		if (twisted) {
			switch (r.nextInt(3)) {
				case 0 -> {
					msg = "You wished to fly... a little TOO high!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1));
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0));
				}
				case 1 -> {
					msg = "You wished for a feast... now you're starving!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200, 1));
				}
				default -> {
					msg = "You wished for peace and quiet... and can't see a thing!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 80, 0));
				}
			}
		} else {
			switch (r.nextInt(6)) {
				case 0 -> {
					msg = "You wished to be strong!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 1));
				}
				case 1 -> {
					msg = "You wished to be fast!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200, 1));
				}
				case 2 -> {
					msg = "You wished to be healed!";
					p.setHealth(p.getMaxHealth());
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
				}
				case 3 -> {
					msg = "You wished to be protected!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1200, 1));
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 2));
				}
				case 4 -> {
					msg = "You wished to work faster!";
					p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 2400, 1));
				}
				default -> {
					msg = "You wished for gold!";
					p.giveItemStack(new ItemStack(net.minecraft.item.Items.GOLD_INGOT, 3 + r.nextInt(5)));
				}
			}
		}
		p.sendMessage(Text.literal("✨ " + msg).formatted(twisted ? Formatting.RED : Formatting.GOLD), true);
		w.spawnParticles(GOLD, p.getX(), p.getY() + 1, p.getZ(), 50, 0.5, 0.9, 0.5, 0.05);
		w.spawnParticles(ParticleTypes.WAX_OFF, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.9, 0.5, 0.5);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, twisted ? 0.6f : 1.4f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.7f, 1.5f);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.literal("Right-click to make a wish (30s recharge)").formatted(Formatting.GOLD));
		tooltip.add(Text.literal("Usually a blessing... genies can be tricky").formatted(Formatting.DARK_GRAY));
	}
}
