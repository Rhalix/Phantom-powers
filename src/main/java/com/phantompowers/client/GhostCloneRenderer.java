package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.entity.GhostCloneEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.UUID;

/**
 * A ghost clone looks just like its owner in ghost form (their own skin with the ghost hair and eyes, wearing
 * their chosen suit), a little see-through. It throws its head forward and arms out when it wails.
 */
public class GhostCloneRenderer extends LivingEntityRenderer<GhostCloneEntity, PlayerEntityModel<GhostCloneEntity>> {
	private final PlayerEntityModel<GhostCloneEntity> suit;

	public GhostCloneRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.3f);
		this.suit = new PlayerEntityModel<>(ctx.getPart(GhostSuitFeature.WIDE), false);
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, GhostCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().copyBipedStateTo(suit);
				suit.setVisible(true);
				suit.head.visible = suit.hat.visible = suit.jacket.visible = false;
				suit.leftSleeve.visible = suit.rightSleeve.visible = suit.leftPants.visible = suit.rightPants.visible = false;
				SuitTextures.Set tex = SuitTextures.get(SuitTextures.colorsOf(e.owner().orElse(null)));
				suit.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(tex.suit())), light,
						OverlayTexture.DEFAULT_UV, 0xC8FFFFFF);
				Identifier glow = tex.glow();
				if (glow != null) suit.render(matrices, consumers.getBuffer(RenderLayer.getEyes(glow)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
				Identifier eyes = GhostSkins.eyes(ownerSkin(e));
				if (eyes != null) getContextModel().head.render(matrices, consumers.getBuffer(RenderLayer.getEyes(eyes)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	private static Identifier ownerSkin(GhostCloneEntity e) {
		UUID id = e.owner().orElse(null);
		MinecraftClient mc = MinecraftClient.getInstance();
		if (id != null && mc.getNetworkHandler() != null) {
			PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(id);
			if (entry != null) return entry.getSkinTextures().texture();
		}
		return id != null ? DefaultSkinHelper.getSkinTextures(id).texture() : DefaultSkinHelper.getTexture();
	}

	@Override
	public Identifier getTexture(GhostCloneEntity e) {
		Identifier own = ownerSkin(e);
		Identifier ghost = GhostSkins.skin(own);
		return ghost != null ? ghost : own;
	}

	@Override
	protected RenderLayer getRenderLayer(GhostCloneEntity e, boolean showBody, boolean translucent, boolean showOutline) {
		return RenderLayer.getEntityTranslucent(getTexture(e));
	}

	@Override
	protected void setupTransforms(GhostCloneEntity e, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
		super.setupTransforms(e, matrices, animationProgress, bodyYaw, tickDelta, scale);
		matrices.translate(0, MathHelper.sin((e.age + tickDelta) * 0.1f + e.getId()) * 0.06f, 0);
	}

	@Override
	protected boolean hasLabel(GhostCloneEntity e) {
		return false;
	}
}
