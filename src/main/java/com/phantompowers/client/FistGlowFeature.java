package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Ecto Fists: a pulsing green energy aura wrapped around the forearms and fists. */
public class FistGlowFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer WIDE = new EntityModelLayer(PhantomMod.id("ecto_fists"), "wide");
	public static final EntityModelLayer SLIM = new EntityModelLayer(PhantomMod.id("ecto_fists"), "slim");
	public static final Identifier TEXTURE = PhantomMod.id("textures/entity/ecto_fists.png");
	private static int clock;

	private final PlayerEntityModel<AbstractClientPlayerEntity> wide, slim;

	public FistGlowFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx,
			ModelPart wideRoot, ModelPart slimRoot) {
		super(ctx);
		this.wide = new PlayerEntityModel<>(wideRoot, false);
		this.slim = new PlayerEntityModel<>(slimRoot, true);
	}

	public static TexturedModelData layer(boolean slimArms) {
		return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(new Dilation(0.55f), slimArms), 64, 64);
	}

	public static void tick(MinecraftClient mc) {
		clock++;
	}

	public static boolean active(AbstractClientPlayerEntity p) {
		return !GhostFlags.CLIENT_FISTS.isEmpty() && GhostFlags.CLIENT_FISTS.contains(p.getUuid()) && !p.isInvisible();
	}

	/** Brightness pulse, as a color multiplier. */
	public static int pulse(float partial) {
		float b = 0.72f + 0.28f * MathHelper.sin((clock + partial) * 0.35f);
		int v = (int) (255 * b);
		return 0xFF000000 | v << 16 | v << 8 | v;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		if (!active(player)) return;
		PlayerEntityModel<AbstractClientPlayerEntity> m = player.getSkinTextures().model() == SkinTextures.Model.SLIM ? slim : wide;
		getContextModel().copyBipedStateTo(m);
		m.setVisible(false);
		m.rightArm.visible = true;
		m.leftArm.visible = true;
		var vc = consumers.getBuffer(RenderLayer.getEyes(TEXTURE));
		int color = pulse(tickDelta);
		m.render(matrices, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, color);
		// A tight inner glow right on the skin too
		getContextModel().rightArm.render(matrices, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, color);
		getContextModel().leftArm.render(matrices, vc, 0xF000F0, OverlayTexture.DEFAULT_UV, color);
	}
}
