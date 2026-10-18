package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Ghost form: a solid white suit (with gloves and boots) worn over your body like armor.
 * Your own head, face, skin color and hairstyle stay exactly as they are.
 */
public class GhostSuitFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer WIDE = new EntityModelLayer(PhantomMod.id("ghost_suit"), "wide");
	public static final EntityModelLayer SLIM = new EntityModelLayer(PhantomMod.id("ghost_suit"), "slim");
	private final PlayerEntityModel<AbstractClientPlayerEntity> wide;
	private final PlayerEntityModel<AbstractClientPlayerEntity> slim;

	public GhostSuitFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx,
			ModelPart wideRoot, ModelPart slimRoot) {
		super(ctx);
		this.wide = new PlayerEntityModel<>(wideRoot, false);
		this.slim = new PlayerEntityModel<>(slimRoot, true);
	}

	/** A copy of the player model, puffed out a little so the suit sits on top of the skin and its outer layer. */
	public static TexturedModelData layer(boolean slimArms) {
		return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(new Dilation(0.3f), slimArms), 64, 64);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (player.isInvisible()) return;
		LookTransition.T t = LookTransition.get(player);
		boolean preview = GhostSuitScreen.previewing && player == net.minecraft.client.MinecraftClient.getInstance().player;
		if (t == null || preview) {
			if (!preview && (GhostFlags.CLIENT_GHOSTS.isEmpty() || !GhostFlags.CLIENT_GHOSTS.contains(player.getUuid()))) return;
			draw(matrices, consumers, light, player, vc -> vc);
			return;
		}
		// Mid-transformation: the suit only covers what the rings have already swept over (or not yet, going human)
		double[] band = LookTransition.band(player, t, tickDelta);
		double cy = LookTransition.camY();
		double lo = band[0] - cy, hi = band[1] - cy;
		if (t.after == LookTransition.GHOST) {
			draw(matrices, consumers, light, player, vc -> new ClipConsumer(vc, lo, hi));
		}
		if (t.before == LookTransition.GHOST) {
			draw(matrices, consumers, light, player, vc -> new ClipConsumer(vc, -1e9, lo));
			draw(matrices, consumers, light, player, vc -> new ClipConsumer(vc, hi, 1e9));
		}
	}

	private void draw(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			java.util.function.UnaryOperator<VertexConsumer> wrap) {
		PlayerEntityModel<AbstractClientPlayerEntity> m = player.getSkinTextures().model() == SkinTextures.Model.SLIM ? slim : wide;
		getContextModel().copyBipedStateTo(m);
		m.setVisible(true);
		m.head.visible = false;
		m.hat.visible = false;
		m.jacket.visible = false;
		m.leftSleeve.visible = false;
		m.rightSleeve.visible = false;
		m.leftPants.visible = false;
		m.rightPants.visible = false;
		if (GhostTail.isTailed(player)) {
			m.rightLeg.visible = false;
			m.leftLeg.visible = false;
		}
		SuitTextures.Set tex = SuitTextures.of(player);
		m.render(matrices, wrap.apply(consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex.suit()))), light, OverlayTexture.DEFAULT_UV, -1);
		if (tex.glow() != null) {
			m.render(matrices, wrap.apply(consumers.getBuffer(RenderLayer.getEyes(tex.glow()))), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
		}
		// Glowing red eyes
		Identifier eyes = GhostSkins.eyes(player.getSkinTextures().texture());
		if (eyes != null) {
			getContextModel().head.render(matrices, wrap.apply(consumers.getBuffer(RenderLayer.getEyes(eyes))), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
		}
	}
}
