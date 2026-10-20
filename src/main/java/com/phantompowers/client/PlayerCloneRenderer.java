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
 * evil clone: black and hot pink with red eyes. When you change back to human, so does the clone: a girl in a red
 * beanie, blue hoodie, red shorts and sneakers with long dark wavy hair, or a boy in the same outfit with messy dark hair.
 */
public class PlayerCloneRenderer extends LivingEntityRenderer<PlayerCloneEntity, PlayerEntityModel<PlayerCloneEntity>> {
	public static final EntityModelLayer HAIR_F = new EntityModelLayer(PhantomMod.id("clone_hair_female"), "main");
	public static final EntityModelLayer HAIR_M = new EntityModelLayer(PhantomMod.id("clone_hair_male"), "main");
	public static final EntityModelLayer GEAR_F = new EntityModelLayer(PhantomMod.id("clone_gear_female"), "main");
	public static final EntityModelLayer GEAR_M = new EntityModelLayer(PhantomMod.id("clone_gear_male"), "main");
	public static final EntityModelLayer HUMAN_F = new EntityModelLayer(PhantomMod.id("clone_human_female"), "main");
	public static final EntityModelLayer HUMAN_M = new EntityModelLayer(PhantomMod.id("clone_human_male"), "main");
	private static final Identifier HAIR_TEX_F = PhantomMod.id("textures/entity/clone_hair_female.png");
	private static final Identifier HAIR_TEX_M = PhantomMod.id("textures/entity/clone_hair_male.png");
	private final PlayerEntityModel<PlayerCloneEntity> wide, slim;
	private final ModelPart hairF, hairM, gearF, gearM, humanF, humanM;

	public PlayerCloneRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), 0.4f);
		this.wide = this.model;
		this.slim = new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true);
		this.hairF = ctx.getPart(HAIR_F);
		this.hairM = ctx.getPart(HAIR_M);
		this.gearF = ctx.getPart(GEAR_F);
		this.gearM = ctx.getPart(GEAR_M);
		this.humanF = ctx.getPart(HUMAN_F);
		this.humanM = ctx.getPart(HUMAN_M);
		// Human form: beanie (or cap), long wavy hair, hoodie with hood and pocket, sneakers
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				if (e.isGhostForm()) return;
				ModelPart gear = e.isFemale() ? humanF : humanM;
				PlayerEntityModel<PlayerCloneEntity> m = getContextModel();
				gear.getChild("head").copyTransform(m.head);
				gear.getChild("body").copyTransform(m.body);
				gear.getChild("right_arm").copyTransform(m.rightArm);
				gear.getChild("left_arm").copyTransform(m.leftArm);
				gear.getChild("right_leg").copyTransform(m.rightLeg);
				gear.getChild("left_leg").copyTransform(m.leftLeg);
				Identifier tex = PhantomMod.id("textures/entity/clone_human_gear_" + (e.isFemale() ? "female" : "male") + ".png");
				gear.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)), light, OverlayTexture.DEFAULT_UV, -1);
			}
		});
		// 3D gear: collar, raised glowing emblem, belt and buckle, shoulder pads, bracers, chunky boots
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				if (!e.isGhostForm()) return;
				ModelPart gear = e.isFemale() ? gearF : gearM;
				PlayerEntityModel<PlayerCloneEntity> m = getContextModel();
				gear.getChild("body").copyTransform(m.body);
				gear.getChild("right_arm").copyTransform(m.rightArm);
				gear.getChild("left_arm").copyTransform(m.leftArm);
				gear.getChild("right_leg").copyTransform(m.rightLeg);
				gear.getChild("left_leg").copyTransform(m.leftLeg);
				String n = "textures/entity/clone_gear_" + (e.isFemale() ? "female" : "male") + (e.isDark() ? "_dark" : "");
				gear.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(PhantomMod.id(n + ".png"))), light, OverlayTexture.DEFAULT_UV, -1);
				gear.render(matrices, consumers.getBuffer(RenderLayer.getEyes(PhantomMod.id(n + "_glow.png"))), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
		// glowing eyes and chest emblem
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, PlayerCloneEntity e,
					float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
				if (!e.isGhostForm()) return;
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
				if (!e.isGhostForm()) return;
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
				.uv(8, 19).cuboid(-1.5f, -0.25f, -0.5f, 3f, 4f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-2.5f, -8.2f, -4.6f, -0.2f, 0f, -0.7f));
		ModelPartData p_s1 = r.addChild("s1", ModelPartBuilder.create()
				.uv(34, 25).cuboid(-1.5f, 0f, -0.5f, 3f, 3f, 1f),
				ModelTransform.of(0.5f, -8.2f, -4.6f, -0.2f, 0f, -0.45f));
		ModelPartData p_s2 = r.addChild("s2", ModelPartBuilder.create()
				.uv(42, 25).cuboid(-1f, -0.25f, -0.5f, 2f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(3f, -8.2f, -4.6f, -0.15f, 0f, 0.3f));
		ModelPartData p_s3 = r.addChild("s3", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 6f, 5f),
				ModelTransform.of(-4.6f, -8f, -2f, 0f, 0f, 0.25f));
		ModelPartData p_s4 = r.addChild("s4", ModelPartBuilder.create()
				.uv(48, 0).cuboid(-0.5f, 0f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(4.6f, -8f, -2f, 0f, 0f, -0.25f));
		ModelPartData p_s5 = r.addChild("s5", ModelPartBuilder.create()
				.uv(16, 19).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-2f, -8.5f, -2f, 0.5f, 0f, -0.4f));
		ModelPartData p_s6 = r.addChild("s6", ModelPartBuilder.create()
				.uv(24, 19).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(2f, -8.5f, -1f, 0.4f, 0f, 0.5f));
		ModelPartData p_s7 = r.addChild("s7", ModelPartBuilder.create()
				.uv(32, 19).cuboid(-1.5f, -3f, -1f, 3f, 3f, 2f),
				ModelTransform.of(0f, -8.5f, 2f, -0.6f, 0f, 0f));
		ModelPartData p_s8 = r.addChild("s8", ModelPartBuilder.create()
				.uv(0, 19).cuboid(-1.5f, 0f, -0.5f, 3f, 5f, 1f),
				ModelTransform.of(-3.5f, -7f, 4.4f, 0.25f, 0f, -0.3f));
		ModelPartData p_s9 = r.addChild("s9", ModelPartBuilder.create()
				.uv(20, 11).cuboid(-1.5f, 0f, -0.5f, 3f, 6f, 1f),
				ModelTransform.of(1f, -7f, 4.4f, 0.2f, 0f, 0.3f));
		ModelPartData p_s10 = r.addChild("s10", ModelPartBuilder.create()
				.uv(0, 11).cuboid(-0.5f, 0f, -2f, 1f, 4f, 4f),
				ModelTransform.of(-4.7f, -5f, 0f, 0f, 0f, 0.45f));
		ModelPartData p_s11 = r.addChild("s11", ModelPartBuilder.create()
				.uv(10, 11).cuboid(-0.5f, 0f, -2f, 1f, 4f, 4f),
				ModelTransform.of(4.7f, -5f, 0f, 0f, 0f, -0.45f));
		ModelPartData p_s12 = r.addChild("s12", ModelPartBuilder.create()
				.uv(42, 19).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-3f, -8.5f, 1f, 0.3f, 0.3f, -0.9f));
		ModelPartData p_s13 = r.addChild("s13", ModelPartBuilder.create()
				.uv(50, 19).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(3f, -8.5f, 1f, 0.3f, -0.3f, 0.9f));
		ModelPartData p_s14 = r.addChild("s14", ModelPartBuilder.create()
				.uv(0, 25).cuboid(-1f, -2.75f, -1f, 2f, 3f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-1f, -8.5f, -3f, -0.6f, 0f, -0.3f));
		ModelPartData p_s15 = r.addChild("s15", ModelPartBuilder.create()
				.uv(8, 25).cuboid(-1f, -2.75f, -1f, 2f, 3f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(1.5f, -8.5f, -3f, -0.7f, 0f, 0.4f));
		ModelPartData p_s16 = r.addChild("s16", ModelPartBuilder.create()
				.uv(16, 25).cuboid(-1.5f, 0f, -0.5f, 3f, 4f, 1f),
				ModelTransform.of(-2f, -4f, 4.5f, 0.35f, 0f, -0.2f));
		ModelPartData p_s17 = r.addChild("s17", ModelPartBuilder.create()
				.uv(48, 25).cuboid(-1.5f, 0f, -0.5f, 3f, 3f, 1f),
				ModelTransform.of(2f, -4f, 4.5f, 0.35f, 0f, 0.2f));
		ModelPartData p_pony = r.addChild("pony", ModelPartBuilder.create()
				.uv(0, 30).cuboid(-1.5f, -1f, 0f, 3f, 2f, 2f, new Dilation(0.2f))
				.uv(28, 11).cuboid(-2f, 1f, 0f, 4f, 4f, 3f, new Dilation(0f, 0f, -0.2f))
				.uv(42, 11).cuboid(-2.5f, 5f, 0.3f, 5f, 4f, 3f)
				.uv(24, 25).cuboid(-1.5f, 9f, 0.5f, 3f, 3f, 2f),
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
				.uv(0, 21).cuboid(-1.5f, 0f, -0.5f, 3f, 2f, 1f),
				ModelTransform.of(-2.5f, -8.2f, -4.6f, -0.2f, 0f, -0.7f));
		ModelPartData p_s1 = r.addChild("s1", ModelPartBuilder.create()
				.uv(8, 21).cuboid(-1.5f, 0f, -0.5f, 3f, 2f, 1f),
				ModelTransform.of(0.5f, -8.2f, -4.6f, -0.2f, 0f, -0.45f));
		ModelPartData p_s2 = r.addChild("s2", ModelPartBuilder.create()
				.uv(16, 21).cuboid(-1f, 0f, -0.5f, 2f, 2f, 1f),
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
		ModelPartData p_s8 = r.addChild("s8", ModelPartBuilder.create()
				.uv(26, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-3f, -8.5f, 1f, 0.3f, 0.3f, -0.9f));
		ModelPartData p_s9 = r.addChild("s9", ModelPartBuilder.create()
				.uv(34, 11).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(3f, -8.5f, 1f, 0.3f, -0.3f, 0.9f));
		ModelPartData p_s10 = r.addChild("s10", ModelPartBuilder.create()
				.uv(42, 11).cuboid(-1f, -2.75f, -1f, 2f, 3f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-1f, -8.5f, -3f, -0.6f, 0f, -0.3f));
		ModelPartData p_s11 = r.addChild("s11", ModelPartBuilder.create()
				.uv(50, 11).cuboid(-1f, -2.75f, -1f, 2f, 3f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(1.5f, -8.5f, -3f, -0.7f, 0f, 0.4f));
		ModelPartData p_m0 = r.addChild("m0", ModelPartBuilder.create()
				.uv(0, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-2.5f, -9f, -2f, 0.3f, 0f, -0.5f));
		ModelPartData p_m1 = r.addChild("m1", ModelPartBuilder.create()
				.uv(8, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(0.5f, -9f, -1f, 0.2f, 0f, 0.4f));
		ModelPartData p_m2 = r.addChild("m2", ModelPartBuilder.create()
				.uv(16, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-1f, -9f, 2f, -0.5f, 0f, -0.2f));
		ModelPartData p_m3 = r.addChild("m3", ModelPartBuilder.create()
				.uv(24, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(2.5f, -9f, 2f, -0.4f, 0f, 0.5f));
		ModelPartData p_m4 = r.addChild("m4", ModelPartBuilder.create()
				.uv(32, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-3.5f, -9f, 0.5f, 0f, 0f, -0.8f));
		ModelPartData p_m5 = r.addChild("m5", ModelPartBuilder.create()
				.uv(40, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(3.5f, -9f, 0f, 0f, 0f, 0.8f));
		ModelPartData p_m6 = r.addChild("m6", ModelPartBuilder.create()
				.uv(48, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(0f, -9f, 3.8f, -0.9f, 0f, 0f));
		ModelPartData p_m7 = r.addChild("m7", ModelPartBuilder.create()
				.uv(56, 16).cuboid(-1f, -3f, -1f, 2f, 3f, 2f),
				ModelTransform.of(-1.5f, -9f, -3.6f, 0.8f, 0f, -0.2f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static TexturedModelData gearFemale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(20, 10).cuboid(-2.5f, -0.95f, -3f, 5f, 2f, 6f, new Dilation(0f, -0.35f, -0.4f))
				.uv(0, 35).cuboid(-3f, 1f, -2.8f, 6f, 5f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(24, 27).cuboid(-4.5f, 9.1f, -2.5f, 9f, 2f, 5f, new Dilation(-0.2f, -0.3f, -0.2f))
				.uv(28, 47).cuboid(-1.5f, 9.1f, -2.8f, 3f, 2f, 1f, new Dilation(-0.3f, -0.1f, -0.15f))
				.uv(14, 35).cuboid(-4.5f, 5.55f, -2.5f, 9f, 1f, 5f, new Dilation(-0.25f, -0.05f, -0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(42, 10).cuboid(-2.5f, -3f, -2.5f, 4f, 3f, 5f, new Dilation(0f, -0.4f, 0f))
				.uv(0, 0).cuboid(-2.5f, 5f, -2.5f, 4f, 5f, 5f, new Dilation(-0.05f, -0.4f, -0.05f))
				.uv(42, 35).cuboid(-2.5f, 9.3f, -2.5f, 4f, 1f, 5f, new Dilation(-0.2f, -0.1f, -0.2f))
				.uv(36, 47).cuboid(-1.5f, 6.5f, -2.95f, 2f, 2f, 1f, new Dilation(-0.4f, 0f, -0.25f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(0, 19).cuboid(-1.5f, -3f, -2.5f, 4f, 3f, 5f, new Dilation(0f, -0.4f, 0f))
				.uv(18, 0).cuboid(-1.5f, 5f, -2.5f, 4f, 5f, 5f, new Dilation(-0.05f, -0.4f, -0.05f))
				.uv(0, 41).cuboid(-1.5f, 9.3f, -2.5f, 4f, 1f, 5f, new Dilation(-0.2f, -0.1f, -0.2f))
				.uv(42, 47).cuboid(-0.5f, 6.5f, -2.95f, 2f, 2f, 1f, new Dilation(-0.4f, 0f, -0.25f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-2.5f, 8.1f, -2.5f, 5f, 4f, 5f, new Dilation(-0.1f, -0.3f, -0.1f))
				.uv(0, 47).cuboid(-2.5f, 10.05f, -3.8f, 5f, 2f, 2f, new Dilation(-0.2f, -0.25f, -0.3f))
				.uv(18, 19).cuboid(-2.5f, 11.4f, -4.05f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.45f))
				.uv(18, 41).cuboid(-2.5f, 8f, -2.5f, 5f, 1f, 5f, new Dilation(-0.05f, -0.2f, -0.05f))
				.uv(48, 47).cuboid(-1f, 3.95f, -2.75f, 2f, 2f, 1f, new Dilation(0f, -0.25f, -0.25f)),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(0, 10).cuboid(-2.5f, 8.1f, -2.5f, 5f, 4f, 5f, new Dilation(-0.1f, -0.3f, -0.1f))
				.uv(14, 47).cuboid(-2.5f, 10.05f, -3.8f, 5f, 2f, 2f, new Dilation(-0.2f, -0.25f, -0.3f))
				.uv(0, 27).cuboid(-2.5f, 11.4f, -4.05f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.45f))
				.uv(38, 41).cuboid(-2.5f, 8f, -2.5f, 5f, 1f, 5f, new Dilation(-0.05f, -0.2f, -0.05f))
				.uv(54, 47).cuboid(-1f, 3.95f, -2.75f, 2f, 2f, 1f, new Dilation(0f, -0.25f, -0.25f)),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static TexturedModelData gearMale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(20, 10).cuboid(-2.5f, -0.95f, -3f, 5f, 2f, 6f, new Dilation(0f, -0.35f, -0.4f))
				.uv(0, 36).cuboid(-3f, 1f, -2.8f, 6f, 5f, 1f, new Dilation(0f, 0f, -0.2f))
				.uv(24, 28).cuboid(-4.5f, 9.1f, -2.5f, 9f, 2f, 5f, new Dilation(-0.2f, -0.3f, -0.2f))
				.uv(28, 48).cuboid(-1.5f, 9.1f, -2.8f, 3f, 2f, 1f, new Dilation(-0.3f, -0.1f, -0.15f))
				.uv(14, 36).cuboid(-4.5f, 0.2f, -2.5f, 9f, 1f, 5f, new Dilation(-0.25f, 0f, -0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(42, 10).cuboid(-3.5f, -3f, -2.5f, 5f, 3f, 5f, new Dilation(0f, -0.4f, 0f))
				.uv(0, 0).cuboid(-3.5f, 5f, -2.5f, 5f, 5f, 5f, new Dilation(-0.05f, -0.4f, -0.05f))
				.uv(42, 36).cuboid(-3.5f, 9.3f, -2.5f, 5f, 1f, 5f, new Dilation(-0.2f, -0.1f, -0.2f))
				.uv(36, 48).cuboid(-2f, 6.5f, -2.95f, 2f, 2f, 1f, new Dilation(-0.4f, 0f, -0.25f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(0, 20).cuboid(-1.5f, -3f, -2.5f, 5f, 3f, 5f, new Dilation(0f, -0.4f, 0f))
				.uv(20, 0).cuboid(-1.5f, 5f, -2.5f, 5f, 5f, 5f, new Dilation(-0.05f, -0.4f, -0.05f))
				.uv(0, 42).cuboid(-1.5f, 9.3f, -2.5f, 5f, 1f, 5f, new Dilation(-0.2f, -0.1f, -0.2f))
				.uv(42, 48).cuboid(0f, 6.5f, -2.95f, 2f, 2f, 1f, new Dilation(-0.4f, 0f, -0.25f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(40, 0).cuboid(-2.5f, 7.1f, -2.5f, 5f, 5f, 5f, new Dilation(-0.1f, -0.3f, -0.1f))
				.uv(0, 48).cuboid(-2.5f, 10.05f, -3.8f, 5f, 2f, 2f, new Dilation(-0.2f, -0.25f, -0.3f))
				.uv(20, 20).cuboid(-2.5f, 11.4f, -4.05f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.45f))
				.uv(20, 42).cuboid(-2.5f, 7f, -2.5f, 5f, 1f, 5f, new Dilation(-0.05f, -0.2f, -0.05f))
				.uv(48, 48).cuboid(-1f, 3.95f, -2.75f, 2f, 2f, 1f, new Dilation(0f, -0.25f, -0.25f)),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(0, 10).cuboid(-2.5f, 7.1f, -2.5f, 5f, 5f, 5f, new Dilation(-0.1f, -0.3f, -0.1f))
				.uv(14, 48).cuboid(-2.5f, 10.05f, -3.8f, 5f, 2f, 2f, new Dilation(-0.2f, -0.25f, -0.3f))
				.uv(0, 28).cuboid(-2.5f, 11.4f, -4.05f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.45f))
				.uv(40, 42).cuboid(-2.5f, 7f, -2.5f, 5f, 1f, 5f, new Dilation(-0.05f, -0.2f, -0.05f))
				.uv(54, 48).cuboid(-1f, 3.95f, -2.75f, 2f, 2f, 1f, new Dilation(0f, -0.25f, -0.25f)),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static TexturedModelData humanFemale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-5f, -9.6f, -5f, 10f, 3f, 10f, new Dilation(-0.4f, -0.2f, -0.4f))
				.uv(76, 0).cuboid(-5f, -7.5f, -5f, 10f, 2f, 10f, new Dilation(-0.2f, -0.3f, -0.2f))
				.uv(0, 16).cuboid(-5f, -7f, 3.1f, 10f, 10f, 2f, new Dilation(-0.4f, 0f, -0.2f))
				.uv(0, 0).cuboid(-5.3f, -6.5f, -3.25f, 2f, 9f, 7f, new Dilation(-0.4f, 0f, -0.25f))
				.uv(18, 0).cuboid(3.3f, -6.5f, -3.25f, 2f, 9f, 7f, new Dilation(-0.4f, 0f, -0.25f))
				.uv(58, 36).cuboid(-4.5f, 2.5f, 2.5f, 9f, 3f, 2f)
				.uv(64, 16).cuboid(-5.6f, 1f, -1f, 2f, 4f, 4f, new Dilation(-0.4f, 0f, 0f))
				.uv(76, 16).cuboid(3.6f, 1f, -1f, 2f, 4f, 4f, new Dilation(-0.4f, 0f, 0f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(40, 36).cuboid(-3.5f, -0.6f, 1.7f, 7f, 4f, 2f, new Dilation(-0.3f, -0.2f, -0.2f))
				.uv(80, 36).cuboid(-2.5f, 6.4f, -2.75f, 5f, 4f, 1f, new Dilation(0f, -0.4f, -0.2f))
				.uv(92, 36).cuboid(-1.05f, 0.6f, -2.85f, 1f, 3f, 1f, new Dilation(-0.25f, -0.2f, -0.25f))
				.uv(96, 36).cuboid(0.05f, 0.4f, -2.85f, 1f, 3f, 1f, new Dilation(-0.25f, -0.4f, -0.25f))
				.uv(88, 28).cuboid(-4.5f, 10.45f, -2.5f, 9f, 2f, 5f, new Dilation(-0.25f, -0.45f, -0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(88, 16).cuboid(-2.5f, 7.5f, -2.5f, 4f, 3f, 5f, new Dilation(-0.2f, -0.4f, -0.2f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(106, 16).cuboid(-1.5f, 7.5f, -2.5f, 4f, 3f, 5f, new Dilation(-0.2f, -0.4f, -0.2f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(0, 36).cuboid(-2.5f, 7.2f, -2.5f, 5f, 2f, 5f, new Dilation(-0.3f, -0.4f, -0.3f))
				.uv(0, 28).cuboid(-2.5f, 9.15f, -2.5f, 5f, 3f, 5f, new Dilation(-0.1f, -0.45f, -0.1f))
				.uv(100, 36).cuboid(-2.5f, 10f, -3.9f, 5f, 2f, 2f, new Dilation(-0.2f, -0.3f, -0.3f))
				.uv(0, 43).cuboid(-1.5f, 9.4f, -2.6f, 3f, 1f, 1f, new Dilation(-0.1f, 0f, 0f))
				.uv(20, 28).cuboid(-2.5f, 11.3f, -4.1f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.4f))
				.uv(24, 16).cuboid(-2.5f, -0.25f, -2.5f, 5f, 4f, 5f, new Dilation(-0.25f)),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(20, 36).cuboid(-2.5f, 7.2f, -2.5f, 5f, 2f, 5f, new Dilation(-0.3f, -0.4f, -0.3f))
				.uv(44, 28).cuboid(-2.5f, 9.15f, -2.5f, 5f, 3f, 5f, new Dilation(-0.1f, -0.45f, -0.1f))
				.uv(114, 36).cuboid(-2.5f, 10f, -3.9f, 5f, 2f, 2f, new Dilation(-0.2f, -0.3f, -0.3f))
				.uv(8, 43).cuboid(-1.5f, 9.4f, -2.6f, 3f, 1f, 1f, new Dilation(-0.1f, 0f, 0f))
				.uv(64, 28).cuboid(-2.5f, 11.3f, -4.1f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.4f))
				.uv(44, 16).cuboid(-2.5f, -0.25f, -2.5f, 5f, 4f, 5f, new Dilation(-0.25f)),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	public static TexturedModelData humanMale() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5f, -9.6f, -5f, 10f, 3f, 10f, new Dilation(-0.4f, -0.2f, -0.4f))
				.uv(40, 0).cuboid(-5f, -7.5f, -5f, 10f, 2f, 10f, new Dilation(-0.2f, -0.3f, -0.2f))
				.uv(32, 37).cuboid(-4.5f, -6.2f, -4.75f, 9f, 1f, 1f)
				.uv(80, 0).cuboid(-5.3f, -6.5f, -4f, 2f, 4f, 8f, new Dilation(-0.4f, 0f, 0f))
				.uv(100, 0).cuboid(3.3f, -6.5f, -4f, 2f, 4f, 8f, new Dilation(-0.4f, 0f, 0f))
				.uv(44, 22).cuboid(-5f, -6.5f, 3.15f, 10f, 5f, 2f, new Dilation(-0.4f, 0f, -0.35f))
				.uv(38, 30).cuboid(-5f, -2f, 1.8f, 10f, 2f, 3f, new Dilation(-0.3f, -0.2f, -0.4f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tuft0 = p_head.addChild("tuft0", ModelPartBuilder.create()
				.uv(14, 37).cuboid(-1f, -0.25f, -0.5f, 2f, 2f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-3f, -5.6f, -4.9f, -0.45f, 0f, -0.6f));
		ModelPartData p_tuft1 = p_head.addChild("tuft1", ModelPartBuilder.create()
				.uv(20, 37).cuboid(-1f, -0.2f, -0.5f, 2f, 2f, 1f, new Dilation(0f, -0.2f, 0f)),
				ModelTransform.of(-0.5f, -5.6f, -4.9f, -0.45f, 0f, 0.2f));
		ModelPartData p_tuft2 = p_head.addChild("tuft2", ModelPartBuilder.create()
				.uv(26, 37).cuboid(-1f, -0.3f, -0.5f, 2f, 2f, 1f, new Dilation(0f, -0.3f, 0f)),
				ModelTransform.of(2.2f, -5.6f, -4.9f, -0.45f, 0f, 0.6f));
		ModelPartData p_tuft3 = p_head.addChild("tuft3", ModelPartBuilder.create()
				.uv(76, 30).cuboid(-0.5f, -0.25f, -0.5f, 1f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-4.9f, -3f, -1f, 0f, 0f, 0.5f));
		ModelPartData p_tuft4 = p_head.addChild("tuft4", ModelPartBuilder.create()
				.uv(80, 30).cuboid(-0.5f, -0.25f, -0.5f, 1f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(4.9f, -3f, 0f, 0f, 0f, -0.5f));
		ModelPartData p_tuft5 = p_head.addChild("tuft5", ModelPartBuilder.create()
				.uv(84, 30).cuboid(-1f, -0.25f, -0.5f, 2f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-2f, -2.5f, 4.4f, 0.5f, 0f, -0.3f));
		ModelPartData p_tuft6 = p_head.addChild("tuft6", ModelPartBuilder.create()
				.uv(90, 30).cuboid(-1f, -0.25f, -0.5f, 2f, 3f, 1f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(2f, -2.5f, 4.4f, 0.5f, 0f, 0.3f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(20, 30).cuboid(-3.5f, -0.6f, 1.7f, 7f, 4f, 2f, new Dilation(-0.3f, -0.2f, -0.2f))
				.uv(64, 30).cuboid(-2.5f, 6.4f, -2.75f, 5f, 4f, 1f, new Dilation(0f, -0.4f, -0.2f))
				.uv(96, 30).cuboid(-1.05f, 0.6f, -2.85f, 1f, 3f, 1f, new Dilation(-0.25f, -0.2f, -0.25f))
				.uv(100, 30).cuboid(0.05f, 0.4f, -2.85f, 1f, 3f, 1f, new Dilation(-0.25f, -0.4f, -0.25f))
				.uv(68, 22).cuboid(-4.5f, 10.45f, -2.5f, 9f, 2f, 5f, new Dilation(-0.25f, -0.45f, -0.25f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(40, 13).cuboid(-3.5f, 7.5f, -2.5f, 5f, 3f, 5f, new Dilation(-0.2f, -0.4f, -0.2f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(60, 13).cuboid(-1.5f, 7.5f, -2.5f, 5f, 3f, 5f, new Dilation(-0.2f, -0.4f, -0.2f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(96, 22).cuboid(-2.5f, 7.2f, -2.5f, 5f, 2f, 5f, new Dilation(-0.3f, -0.4f, -0.3f))
				.uv(80, 13).cuboid(-2.5f, 9.15f, -2.5f, 5f, 3f, 5f, new Dilation(-0.1f, -0.45f, -0.1f))
				.uv(104, 30).cuboid(-2.5f, 10f, -3.9f, 5f, 2f, 2f, new Dilation(-0.2f, -0.3f, -0.3f))
				.uv(52, 37).cuboid(-1.5f, 9.4f, -2.6f, 3f, 1f, 1f, new Dilation(-0.1f, 0f, 0f))
				.uv(100, 13).cuboid(-2.5f, 11.3f, -4.1f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.4f))
				.uv(0, 13).cuboid(-2.5f, -0.25f, -2.5f, 5f, 4f, 5f, new Dilation(-0.25f)),
				ModelTransform.of(-1.9f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(0, 30).cuboid(-2.5f, 7.2f, -2.5f, 5f, 2f, 5f, new Dilation(-0.3f, -0.4f, -0.3f))
				.uv(0, 22).cuboid(-2.5f, 9.15f, -2.5f, 5f, 3f, 5f, new Dilation(-0.1f, -0.45f, -0.1f))
				.uv(0, 37).cuboid(-2.5f, 10f, -3.9f, 5f, 2f, 2f, new Dilation(-0.2f, -0.3f, -0.3f))
				.uv(60, 37).cuboid(-1.5f, 9.4f, -2.6f, 3f, 1f, 1f, new Dilation(-0.1f, 0f, 0f))
				.uv(20, 22).cuboid(-2.5f, 11.3f, -4.1f, 5f, 1f, 7f, new Dilation(0f, -0.2f, -0.4f))
				.uv(20, 13).cuboid(-2.5f, -0.25f, -2.5f, 5f, 4f, 5f, new Dilation(-0.25f)),
				ModelTransform.of(1.9f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
		if (!e.isGhostForm()) return PhantomMod.id("textures/entity/clone_human_" + (e.isFemale() ? "female" : "male") + ".png");
		return PhantomMod.id("textures/entity/" + name(e) + ".png");
	}

	@Override
	protected boolean hasLabel(PlayerCloneEntity e) {
		return e.hasCustomName() && net.minecraft.client.MinecraftClient.getInstance().targetedEntity == e;
	}
}
