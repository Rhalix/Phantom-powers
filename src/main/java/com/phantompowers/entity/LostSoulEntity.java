package com.phantompowers.entity;

import com.phantompowers.ModItems;
import com.phantompowers.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** A harmless wandering ghost in old-fashioned clothes, carrying a little ghost lantern. Talk to it! */
public class LostSoulEntity extends WalkerGhostEntity {
	private static final String[] LINES = {
			"Have you seen my house? It was right here... a hundred years ago.",
			"The yetis in the frozen lands are gentle, mostly. Bring them fish.",
			"Mind the Ghouls in the shadow woods. They don't like the living.",
			"The knights in the crystal spires still think the war is on.",
			"Don't stare into the floating doors too long, friend.",
			"I keep my lantern lit so I don't forget my way home.",
			"Somewhere out there, a ghost feeds on sadness. Stay cheerful!",
			"Ectoplasm? Here, I won't be needing it."};
	private boolean gave;

	public LostSoulEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 2;
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 16.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22);
	}

	@Override
	protected void initGoals() {
		addIdleGoals();
		this.goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (getWorld().isClient) return ActionResult.SUCCESS;
		int i = random.nextInt(LINES.length);
		player.sendMessage(Text.literal("Lost Soul: \"" + LINES[i] + "\"").formatted(Formatting.AQUA), false);
		if (i == LINES.length - 1 && !gave) {
			gave = true;
			player.giveItemStack(new ItemStack(ModItems.ECTOPLASM, 2 + random.nextInt(3)));
		}
		getNavigation().stop();
		getLookControl().lookAt(player);
		return ActionResult.CONSUME;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("Gave", gave);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		gave = nbt.getBoolean("Gave");
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.GHOST_FRIENDLY;
	}
}
