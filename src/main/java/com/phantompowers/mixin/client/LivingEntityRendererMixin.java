package com.phantompowers.mixin.client;

import com.phantompowers.GhostFlags;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ghost form look: your own skin glows ghost-green and turns see-through. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Unique
	private static final int GHOST_TINT = 0xB4A8FFCC;
	@Unique
	private static final int FULL_BRIGHT = 0xF000F0;
	@Unique
	private static LivingEntity phantom$current;

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"))
	private void phantom$capture(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider consumers, int light, CallbackInfo ci) {
		phantom$current = entity;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
	private void phantom$ghostLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline,
			CallbackInfoReturnable<RenderLayer> cir) {
		if (showBody && GhostFlags.clientGhostLook(entity)) {
			cir.setReturnValue(RenderLayer.getItemEntityTranslucentCull(((LivingEntityRenderer) (Object) this).getTexture(entity)));
		}
	}

	@ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
			index = 4)
	private int phantom$tint(int color) {
		return phantom$current != null && GhostFlags.clientGhostLook(phantom$current) ? GHOST_TINT : color;
	}

	@ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
			index = 2)
	private int phantom$glow(int light) {
		return phantom$current != null && GhostFlags.clientGhostLook(phantom$current) ? FULL_BRIGHT : light;
	}
}
