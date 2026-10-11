package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.ModItems;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/**
 * The Ghost Hunter Suit before it's deployed: a compact armored pack on your back with
 * twin side canisters, a glowing core and an antenna. It vanishes when the suit wraps around you.
 */
public class BackpackFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("suit_pack"), "main");
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/suit_pack.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/suit_pack_glow.png");
	private final ModelPart pack;

	public BackpackFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart pack) {
		super(ctx);
		this.pack = pack;
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		r.addChild("pack", ModelPartBuilder.create()
				// main shell
				.uv(0, 0).cuboid(-4f, 0.5f, 2f, 8, 10, 4)
				// top flap
				.uv(24, 0).cuboid(-4.5f, -0.5f, 1.5f, 9, 2, 5)
				// side canisters
				.uv(0, 14).cuboid(-6f, 2f, 2.5f, 2, 7, 3)
				.uv(0, 14).mirrored().cuboid(4f, 2f, 2.5f, 2, 7, 3)
				// glowing core window
				.uv(10, 14).cuboid(-1.5f, 3f, 6f, 3, 4, 0.5f)
				// bottom thruster
				.uv(20, 14).cuboid(-2.5f, 10.5f, 2.5f, 5, 1.5f, 3)
				// antenna
				.uv(36, 14).cuboid(2.5f, -4.5f, 3.5f, 0.5f, 4, 0.5f),
				ModelTransform.NONE);
		return TexturedModelData.of(data, 64, 32);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (!player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.HUNTER_SUIT)) return;
		if (GhostFlags.CLIENT_SUITED.contains(player.getUuid()) || GhostFlags.CLIENT_GHOSTS.contains(player.getUuid())) return;
		if (player.isInvisible()) return;
		matrices.push();
		getContextModel().body.rotate(matrices);
		ModelPart p = pack.getChild("pack");
		p.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		p.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}
}
