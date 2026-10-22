package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.TownspersonEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Amity Park locals: player-shaped people with their own skins (slim arms for the girls), who bop along when dancing. */
public class TownspersonRenderer extends MobEntityRenderer<TownspersonEntity, TownspersonRenderer.Model> {
	private final Model wide, slim, sam, lancer, princess;

	public TownspersonRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new Model(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
		this.wide = this.model;
		this.slim = new Model(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
		this.sam = new Model(SamModelData.getTexturedModelData().createModel(), true);
		this.lancer = new Model(LancerModelData.getTexturedModelData().createModel(), false);
		this.princess = new Model(PrincessModelData.getTexturedModelData().createModel(), true);
	}

	@Override
	public void render(TownspersonEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		this.model = SamModelSelector.select(e, wide, slim, sam, lancer, princess);
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	public Identifier getTexture(TownspersonEntity e) {
		return PhantomMod.id("textures/entity/townsperson/" + TownspersonEntity.SKINS[e.variant()] + ".png");
	}

	@Override
	protected void scale(TownspersonEntity e, MatrixStack matrices, float amount) {
		float s = 0.9375f;
		matrices.scale(s, s, s);
	}

	public static class Model extends PlayerEntityModel<TownspersonEntity> {
		public Model(ModelPart root, boolean thinArms) {
			super(root, thinArms);
		}

		@Override
		public void setAngles(TownspersonEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			super.setAngles(e, limbAngle, limbDistance, age, headYaw, headPitch);
			if (!e.dancing()) return;
			float beat = age * 0.35f + e.getId();
			rightArm.pitch = -2.5f + MathHelper.sin(beat) * 0.45f;
			leftArm.pitch = -2.5f + MathHelper.cos(beat) * 0.45f;
			rightArm.roll = 0.3f;
			leftArm.roll = -0.3f;
			head.pitch = MathHelper.sin(beat * 2f) * 0.15f;
			head.roll = MathHelper.sin(beat) * 0.12f;
			rightLeg.pitch = MathHelper.sin(beat) * 0.25f;
			leftLeg.pitch = -MathHelper.sin(beat) * 0.25f;
			body.roll = MathHelper.sin(beat) * 0.05f;
			hat.copyTransform(head);
			rightSleeve.copyTransform(rightArm);
			leftSleeve.copyTransform(leftArm);
			rightPants.copyTransform(rightLeg);
			leftPants.copyTransform(leftLeg);
			jacket.copyTransform(body);
		}
	}
}
