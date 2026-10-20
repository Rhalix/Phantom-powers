package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.ChefGhoulashEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Chef Ghoulash in stage one, or the hulking Meat Monster (a whole different body) in stage two. */
public class ChefRenderer extends MobEntityRenderer<ChefGhoulashEntity, SinglePartEntityModel<ChefGhoulashEntity>> {
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/chef_ghoulash.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/chef_ghoulash_glow.png");
	private static final Identifier MEAT = PhantomMod.id("textures/entity/meat_monster.png");
	private static final Identifier MEAT_GLOW = PhantomMod.id("textures/entity/meat_monster_glow.png");
	private final ChefModel chef;
	private final ChefMeatModel meat;

	public ChefRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new ChefModel(ctx.getPart(ChefModel.LAYER)), 0.6f);
		this.chef = (ChefModel) this.model;
		this.meat = new ChefMeatModel(ctx.getPart(ChefMeatModel.LAYER));
		this.addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, ChefGhoulashEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(e.meat() ? MEAT_GLOW : GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	@Override
	public void render(ChefGhoulashEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		this.model = e.meat() ? meat : chef;
		this.shadowRadius = e.meat() ? 1.2f : 0.6f;
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(ChefGhoulashEntity e) {
		return e.meat() ? MEAT : TEXTURE;
	}

	@Override
	protected void scale(ChefGhoulashEntity e, MatrixStack matrices, float amount) {
		float s = e.meat() ? 1.1f : 1.0f;
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(ChefGhoulashEntity e, BlockPos pos) {
		return 15;
	}
}
