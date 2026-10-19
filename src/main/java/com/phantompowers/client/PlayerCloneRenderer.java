package com.phantompowers.client;

import com.phantompowers.entity.PlayerCloneEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

/**
 * Your clone: your own skin, with slim arms and a swishing ponytail (made from your own hair) for a female clone,
 * or broad shoulders for a male one. In a fight it goes ghost and wears your ghost suit, glowing eyes and all.
 */
public class PlayerCloneRenderer extends LivingEntityRenderer<PlayerCloneEntity, PlayerEntityModel<PlayerCloneEntity>> {
	public static final EntityModelLayer PONYTAIL = new EntityModelLayer(com.phantompowers.PhantomMod.id("clone_ponytail"), "main");
	private final PlayerEntityModel<PlayerCloneEntity> wide, slim, suitWide, suitSlim;
	private final ModelPart ponytail;

	public PlayerCloneRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
		this.wide = this.model;
		this.slim = new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
		this.suitWide = new PlayerEntityModel<>(ctx.getPart(GhostSuitFeature.WIDE), false);
		this.suitSlim = new PlayerEntityModel<>(ctx.getPart(GhostSuitFeature.SLIM), true);
		this.ponytail = ctx.getPart(PONYTAIL);
		// ghost suit (only in a fight)
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				if (!e.isGhostForm()) return;
				PlayerEntityModel<PlayerCloneEntity> suit = e.isFemale() ? suitSlim : suitWide;
				getContextModel().copyBipedStateTo(suit);
				suit.setVisible(true);
				suit.head.visible = suit.hat.visible = suit.jacket.visible = false;
				suit.leftSleeve.visible = suit.rightSleeve.visible = suit.leftPants.visible = suit.rightPants.visible = false;
				SuitTextures.Set tex = SuitTextures.get(SuitTextures.colorsOf(e.ownerId()));
				suit.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex.suit())), light, OverlayTexture.DEFAULT_UV, -1);
				if (tex.glow() != null) suit.render(matrices, consumers.getBuffer(RenderLayer.getEyes(tex.glow())), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
				Identifier eyes = GhostSkins.eyes(ownerSkin(e));
				if (eyes != null) getContextModel().head.render(matrices, consumers.getBuffer(RenderLayer.getEyes(eyes)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
		// ponytail for a female clone, swaying as she moves
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				if (!e.isFemale()) return;
				ModelPart tail = ponytail.getChild("tail");
				tail.pitch = 0.35f + Math.min(1f, limbDistance) * 0.5f + MathHelper.sin(age * 0.12f) * 0.06f;
				tail.roll = MathHelper.sin(limbAngle * 0.6662f) * 0.15f * Math.min(1f, limbDistance);
				matrices.push();
				getContextModel().head.rotate(matrices);
				ponytail.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(e))), light, OverlayTexture.DEFAULT_UV, -1);
				matrices.pop();
			}
		});
	}

	/** A hair tie and a ponytail, textured from the top of the owner's own head so it matches their hair colour. */
	public static TexturedModelData ponytailModel() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData tail = r.addChild("tail", ModelPartBuilder.create()
						.uv(8, 0).cuboid(-1.5f, 0f, 0f, 3f, 2f, 2f, new Dilation(0.25f))
						.uv(8, 0).cuboid(-1.25f, 2f, 0.25f, 2.5f, 5f, 1.8f)
						.uv(8, 0).cuboid(-1f, 7f, 0.4f, 2f, 3f, 1.4f),
				ModelTransform.of(0f, -6.5f, 3.6f, 0.35f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public void render(PlayerCloneEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		this.model = e.isFemale() ? slim : wide;
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	protected void scale(PlayerCloneEntity e, MatrixStack matrices, float amount) {
		matrices.scale(0.9375f, 0.9375f, 0.9375f);
	}

	private static Identifier ownerSkin(PlayerCloneEntity e) {
		UUID id = e.ownerId();
		MinecraftClient mc = MinecraftClient.getInstance();
		if (id != null && mc.getNetworkHandler() != null) {
			PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(id);
			if (entry != null) return entry.getSkinTextures().texture();
		}
		return id != null ? DefaultSkinHelper.getSkinTextures(id).texture() : DefaultSkinHelper.getTexture();
	}

	@Override
	public Identifier getTexture(PlayerCloneEntity e) {
		Identifier own = ownerSkin(e);
		if (e.isGhostForm()) {
			Identifier ghost = GhostSkins.skin(own);
			if (ghost != null) return ghost;
		}
		return own;
	}

	@Override
	protected boolean hasLabel(PlayerCloneEntity e) {
		return e.hasCustomName() && MinecraftClient.getInstance().targetedEntity == e;
	}
}
