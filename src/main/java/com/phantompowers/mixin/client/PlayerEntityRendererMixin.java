package com.phantompowers.mixin.client;

import com.phantompowers.GhostFlags;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ghost form (white suit, white hair, red eyes) and the Ghost Hunter Suit replace your skin while active. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Unique
	private static AbstractClientPlayerEntity phantom$armPlayer;

	@Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
			at = @At("HEAD"), cancellable = true)
	private void phantom$skin(AbstractClientPlayerEntity player, CallbackInfoReturnable<Identifier> cir) {
		Identifier skin = GhostFlags.clientSkinOverride(player);
		if (skin != null) {
			cir.setReturnValue(skin);
			return;
		}
		// Ghost form: same skin, but white hair and red eyes
		if (!GhostFlags.CLIENT_GHOSTS.isEmpty() && GhostFlags.CLIENT_GHOSTS.contains(player.getUuid())) {
			Identifier ghost = com.phantompowers.client.GhostSkins.skin(player.getSkinTextures().texture());
			if (ghost != null) cir.setReturnValue(ghost);
		}
	}

	@Inject(method = "renderArm", at = @At("HEAD"))
	private void phantom$captureArm(MatrixStack matrices, VertexConsumerProvider consumers, int light,
			AbstractClientPlayerEntity player, ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
		phantom$armPlayer = player;
	}

	@ModifyArg(method = "renderArm", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/RenderLayer;getEntitySolid(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
	private Identifier phantom$armSkin(Identifier original) {
		Identifier skin = phantom$armPlayer == null ? null : GhostFlags.clientSkinOverride(phantom$armPlayer);
		return skin != null ? skin : original;
	}

	@ModifyArg(method = "renderArm", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/RenderLayer;getEntityTranslucent(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
	private Identifier phantom$sleeveSkin(Identifier original) {
		Identifier skin = phantom$armPlayer == null ? null : GhostFlags.clientSkinOverride(phantom$armPlayer);
		return skin != null ? skin : original;
	}
}
