package com.phantompowers.mixin;

import com.phantompowers.HunterSuit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The Ghost Hunter Suit soaks up damage (see HunterSuit.reduceDamage). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float phantom$suitProtection(float amount, DamageSource source, float originalAmount) {
		if ((Object) this instanceof ServerPlayerEntity p) return HunterSuit.reduceDamage(p, source, amount);
		return amount;
	}
}
