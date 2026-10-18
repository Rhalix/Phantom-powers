package com.phantompowers.mixin.client;

import com.phantompowers.client.ClientCutscene;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** During cutscenes, hide the hotbar, hearts, XP bar and crosshair so the movie bars and captions are clear. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderMainHud", at = @At("HEAD"), cancellable = true)
	private void phantom$hideMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (ClientCutscene.active()) ci.cancel();
	}

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void phantom$hideCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (ClientCutscene.active()) ci.cancel();
	}

	@Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
	private void phantom$hideItemName(DrawContext context, CallbackInfo ci) {
		if (ClientCutscene.active()) ci.cancel();
	}
}
