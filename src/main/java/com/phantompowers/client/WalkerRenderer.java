package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.GhostYetiEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Renderer for the walking Ghost Zone creatures: ghosts are see-through and self-lit, yetis are solid; all get a glow layer. */
public class WalkerRenderer<T extends AbstractGhostEntity> extends MobEntityRenderer<T, WalkerModel<T>> {
	private final Identifier texture, glow;
	private final float scale;
	private final boolean ghostly;

	public WalkerRenderer(EntityRendererFactory.Context ctx, WalkerModel.Kind kind, String name, float scale, float shadow) {
		super(ctx, new WalkerModel<>(ctx.getPart(layer(kind)), kind, kind != WalkerModel.Kind.YETI), shadow);
		this.texture = PhantomMod.id("textures/entity/" + name + ".png");
		this.glow = PhantomMod.id("textures/entity/" + name + "_glow.png");
		this.scale = scale;
		this.ghostly = kind != WalkerModel.Kind.YETI;
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, T e, float limbAngle, float limbDistance,
					float tickDelta, float age, float headYaw, float headPitch) {
				if (e.isInvisible()) return;
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(glow)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	private static net.minecraft.client.render.entity.model.EntityModelLayer layer(WalkerModel.Kind k) {
		return switch (k) {
			case GHOUL -> WalkerModel.GHOUL;
			case KNIGHT -> WalkerModel.KNIGHT;
			case SOUL -> WalkerModel.SOUL;
			case YETI -> WalkerModel.YETI;
		};
	}

	@Override
	public void render(T e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	protected int getBlockLight(T e, BlockPos pos) {
		return ghostly ? 12 : super.getBlockLight(e, pos);
	}

	@Override
	protected void scale(T e, MatrixStack matrices, float amount) {
		float s = scale;
		if (e instanceof GhostYetiEntity y && y.isFeral()) s *= 1.08f;
		matrices.scale(s, s, s);
	}

	@Override
	protected RenderLayer getRenderLayer(T e, boolean showBody, boolean translucent, boolean showOutline) {
		if (ghostly && showBody) return RenderLayer.getEntityTranslucent(getTexture(e));
		return super.getRenderLayer(e, showBody, translucent, showOutline);
	}

	@Override
	public Identifier getTexture(T e) {
		return texture;
	}
}
