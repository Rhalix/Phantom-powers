package com.phantompowers.mixin.client;

import com.phantompowers.client.GhostTail;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No floating boots or leggings when a ghost's legs have turned into a tail, and no armor at all on an invisible ghost. */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
	@Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
	private void phantom$tailNoLegArmor(MatrixStack matrices, VertexConsumerProvider consumers, LivingEntity entity, EquipmentSlot slot,
			int light, BipedEntityModel<?> model, CallbackInfo ci) {
		if ((slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) && GhostTail.isTailed(entity)) ci.cancel();
		// Invisible ghosts don't leave their armor floating in the air (not even for themselves)
		if (com.phantompowers.client.SuitVision.isInvisibleGhost(entity)) ci.cancel();
	}
}
