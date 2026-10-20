package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.PlayerCloneEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Your clone, grown from ghost DNA, so it always shows up in ghost form: shorter than you, with spiky 3D white
 * hair and glowing eyes. A girl clone has slim arms, a crop-top suit, silver sleeves and a swishing ponytail; a boy
 * clone has broad shoulders and a full jumpsuit with white gloves, belt and boots. Evil half-ghost DNA makes an
 * evil clone: black and hot pink with red eyes.
 */
public class PlayerCloneRenderer extends LivingEntityRenderer<PlayerCloneEntity, PlayerEntityModel<PlayerCloneEntity>> {
	public static final EntityModelLayer HAIR_F = new EntityModelLayer(PhantomMod.id("clone_hair_female"), "main");
	public static final EntityModelLayer HAIR_M = new EntityModelLayer(PhantomMod.id("clone_hair_male"), "main");
	private static final Identifier HAIR_TEX_F = PhantomMod.id("textures/entity/clone_hair_female.png");
	private static final Identifier HAIR_TEX_M = PhantomMod.id("textures/entity/clone_hair_male.png");
	private final PlayerEntityModel<PlayerCloneEntity> wide, slim;
	private final ModelPart hairF, hairM;

	public PlayerCloneRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.4f);
		this.wide = this.model;
		this.slim = new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
		this.hairF = ctx.getPart(HAIR_F);
		this.hairM = ctx.getPart(HAIR_M);
		// glowing eyes and chest emblem
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				float pulse = 0.8f + 0.2f * MathHelper.sin(age * 0.15f);
				int c = (int) (255 * pulse);
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(glow(e))), 0xF000F0, OverlayTexture.DEFAULT_UV,
						0xFF000000 | (c << 16) | (c << 8) | c);
			}
		});
		// 3D spiky hair (and the girl's ponytail, swaying as she moves)
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				ModelPart hair = e.isFemale() ? hairF : hairM;
				if (e.isFemale()) {
					ModelPart tail = hair.getChild("pony");
					tail.pitch = 0.5f + Math.min(1f, limbDistance) * 0.45f + MathHelper.sin(age * 0.12f) * 0.06f;
					tail.roll = MathHelper.sin(limbAngle * 0.6662f) * 0.18f * Math.min(1f, limbDistance);
				}
				matrices.push();
				getContextModel().head.rotate(matrices);
				hair.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(e.isFemale() ? HAIR_TEX_F : HAIR_TEX_M)), light, OverlayTexture.DEFAULT_UV, -1);
				matrices.pop();
			}
		});
	}

	public static TexturedModelData hairFemale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_hairtop = r.addChild("hairtop", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4.5f, -9.25f, -4.5f, 9f, 2f, 9f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_s0 = r.addChild("s0", ModelPartBuilder.create()
				.uv(46, 11).cuboid(-1.5f, -0.25f, -0.5f, 3f, 4f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-2.5f, -8.2f, -4.6f, -0.2f, 0f, -0.7f));
		ModelPartData p_s1 = r.addChild("s1", ModelPartBuilder.create()
				.uv(28, 18).cuboid(-1.5f, 0f, -0.5f, 3f, 3f, 1f),
				ModelTransform.of(0.5f, -8.2f, -4.6f, -0.2f, 0f, -0.45f));
		ModelPartData p_s2 = r.addChild("s2", ModelPartBuilder.create()
				.uv(36, 18).cuboid(-1f, -0.25f, -0.5f, 2f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(3f, -8.2f, -4.6f, -0.15f, 0f, 0.3f));
		ModelPartData p_s3 = r.addChild("s3", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 6f, 5f),
				ModelTransform.of(-4.6f, -8f, -2f, 0f, 0f, 0.25f));
		ModelPartData p_s4 = r.addChild("s4", ModelPartBuilder.create()
				.uv(48, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(4.6f, -8f, -2f, 0f, 0f, -0.25f));
		ModelPartData p_s5 = r.addChild("s5", ModelPartBuilder.create()
				.uv(54, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-2f, -8.5f, -2f, 0.5f, 0f, -0.4f));
		ModelPartData p_s6 = r.addChild("s6", ModelPartBuilder.create()
				.uv(0, 18).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(2f, -8.5f, -1f, 0.4f, 0f, 0.5f));
		ModelPartData p_s7 = r.addChild("s7", ModelPartBuilder.create()
				.uv(8, 18).cuboid(-1.5f, -3f, -1f, 3f, 3f, 2f),
				ModelTransform.of(0f, -8.5f, 2f, -0.6f, 0f, 0f));
		ModelPartData p_s8 = r.addChild("s8", ModelPartBuilder.create()
				.uv(38, 11).cuboid(-1.5f, 0f, -0.5f, 3f, 5f, 1f),
				ModelTransform.of(-3.5f, -7f, 4.4f, 0.25f, 0f, -0.3f));
		ModelPartData p_s9 = r.addChild("s9", ModelPartBuilder.create()
				.uv(0, 11).cuboid(-1.5f, 0f, -0.5f, 3f, 6f, 1f),
				ModelTransform.of(1f, -7f, 4.4f, 0.2f, 0f, 0.3f));
		ModelPartData p_pony = r.addChild("pony", ModelPartBuilder.create()
				.uv(42, 18).cuboid(-1.5f, -1f, 0f, 3f, 2f, 2f, new Dilation(0.2f))
				.uv(8, 11).cuboid(-2f, 1f, 0f, 4f, 4f, 3f, new Dilation(0f, 0f, -0.2f))
				.uv(22, 11).cuboid(-2.5f, 5f, 0.3f, 5f, 4f, 3f)
				.uv(18, 18).cuboid(-1.5f, 9f, 0.5f, 3f, 3f, 2f),
				ModelTransform.of(0f, -7f, 4.2f, 0.5f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static TexturedModelData hairMale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_hairtop = r.addChild("hairtop", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4.5f, -9.25f, -4.5f, 9f, 2f, 9f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_s0 = r.addChild("s0", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-1.5f, 0f, -0.5f, 3f, 2f, 1f),
				ModelTransform.of(-2.5f, -8.2f, -4.6f, -0.2f, 0f, -0.7f));
		ModelPartData p_s1 = r.addChild("s1", ModelPartBuilder.create()
				.uv(8, 16).cuboid(-1.5f, 0f, -0.5f, 3f, 2f, 1f),
				ModelTransform.of(0.5f, -8.2f, -4.6f, -0.2f, 0f, -0.45f));
		ModelPartData p_s2 = r.addChild("s2", ModelPartBuilder.create()
				.uv(16, 16).cuboid(-1f, 0f, -0.5f, 2f, 2f, 1f),
				ModelTransform.of(3f, -8.2f, -4.6f, -0.15f, 0f, 0.3f));
		ModelPartData p_s3 = r.addChild("s3", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 4f, 5f),
				ModelTransform.of(-4.6f, -8f, -2f, 0f, 0f, 0.25f));
		ModelPartData p_s4 = r.addChild("s4", ModelPartBuilder.create()
				.uv(48, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 3f, 5f),
				ModelTransform.of(4.6f, -8f, -2f, 0f, 0f, -0.25f));
		ModelPartData p_s5 = r.addChild("s5", ModelPartBuilder.create()
				.uv(0, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-2f, -8.5f, -2f, 0.5f, 0f, -0.4f));
		ModelPartData p_s6 = r.addChild("s6", ModelPartBuilder.create()
				.uv(8, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(2f, -8.5f, -1f, 0.4f, 0f, 0.5f));
		ModelPartData p_s7 = r.addChild("s7", ModelPartBuilder.create()
				.uv(16, 11).cuboid(-1.5f, -3f, -1f, 3f, 3f, 2f),
				ModelTransform.of(0f, -8.5f, 2f, -0.6f, 0f, 0f));
		ModelPartData p_m0 = r.addChild("m0", ModelPartBuilder.create()
				.uv(26, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-2.5f, -9f, -2f, 0.3f, 0f, -0.5f));
		ModelPartData p_m1 = r.addChild("m1", ModelPartBuilder.create()
				.uv(34, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(0.5f, -9f, -1f, 0.2f, 0f, 0.4f));
		ModelPartData p_m2 = r.addChild("m2", ModelPartBuilder.create()
				.uv(42, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-1f, -9f, 2f, -0.5f, 0f, -0.2f));
		ModelPartData p_m3 = r.addChild("m3", ModelPartBuilder.create()
				.uv(50, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(2.5f, -9f, 2f, -0.4f, 0f, 0.5f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public void render(PlayerCloneEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		this.model = e.isFemale() ? slim : wide;
		this.model.hat.visible = true;
		this.model.jacket.visible = this.model.leftSleeve.visible = this.model.rightSleeve.visible = true;
		this.model.leftPants.visible = this.model.rightPants.visible = true;
		super.render(e, yaw, tickDelta, matrices, consumers, light);
	}

	@Override
	protected void scale(PlayerCloneEntity e, MatrixStack matrices, float amount) {
		float s = e.isFemale() ? 0.78f : 0.8f;
		matrices.scale(s, s, s);
	}

	private static String name(PlayerCloneEntity e) {
		return "clone_" + (e.isFemale() ? "female" : "male") + (e.isDark() ? "_dark" : "");
	}

	private static Identifier glow(PlayerCloneEntity e) {
		return PhantomMod.id("textures/entity/" + name(e) + "_glow.png");
	}

	@Override
	public Identifier getTexture(PlayerCloneEntity e) {
		return PhantomMod.id("textures/entity/" + name(e) + ".png");
	}

	@Override
	protected boolean hasLabel(PlayerCloneEntity e) {
		return e.hasCustomName() && net.minecraft.client.MinecraftClient.getInstance().targetedEntity == e;
	}
}
