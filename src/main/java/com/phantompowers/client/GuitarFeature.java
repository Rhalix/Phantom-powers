package com.phantompowers.client;

import com.phantompowers.ThunderGuitarItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/** The Thunder Guitar, held across the body like a real guitar (neck to the left, body at the right hip). */
public class GuitarFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public GuitarFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	/** The guitar this entity is holding (either hand), or null. */
	public static ItemStack guitarOf(LivingEntity e) {
		ItemStack main = e.getMainHandStack();
		if (main.getItem() instanceof ThunderGuitarItem) return main;
		ItemStack off = e.getOffHandStack();
		if (off.getItem() instanceof ThunderGuitarItem) return off;
		return null;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		ItemStack stack = guitarOf(player);
		if (stack == null || player.isInvisible()) return;
		matrices.push();
		getContextModel().body.rotate(matrices);
		// In front of the belly, a little to the right; the neck angles up to the left hand
		matrices.translate(-2.5f / 16f, 9f / 16f, -3.6f / 16f);
		float jiggle = player.handSwinging ? (float) Math.sin(age * 1.8f) * 2.5f : 0f;
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(250f + jiggle));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
		matrices.scale(1.15f, 1.15f, 1.15f);
		// Center the guitar's body (not the whole item) on that spot
		matrices.translate(1f / 16f, 4.5f / 16f, 0f);
		MinecraftClient.getInstance().getItemRenderer().renderItem(player, stack, ModelTransformationMode.NONE, false,
				matrices, consumers, player.getWorld(), light, OverlayTexture.DEFAULT_UV, player.getId());
		matrices.pop();
	}
}
