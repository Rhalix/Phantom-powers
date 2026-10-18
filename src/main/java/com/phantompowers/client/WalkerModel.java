package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.GhostYetiEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Shared model for the two-legged Ghost Zone creatures (Ghoul, Specter Knight, Lost Soul, Ghost Yeti):
 * a walking stride, swinging arms, head tracking, an attack swipe, and little per-creature touches
 * (the ghoul's hunch and twitching claws, the knight's sword and cape, the yeti's lumbering gait and sitting pose).
 */
public class WalkerModel<T extends AbstractGhostEntity> extends SinglePartEntityModel<T> {
	public enum Kind { GHOUL, KNIGHT, SOUL, YETI }

	public static final EntityModelLayer GHOUL = new EntityModelLayer(PhantomMod.id("ghoul"), "main");
	public static final EntityModelLayer KNIGHT = new EntityModelLayer(PhantomMod.id("specter_knight"), "main");
	public static final EntityModelLayer SOUL = new EntityModelLayer(PhantomMod.id("lost_soul"), "main");
	public static final EntityModelLayer YETI = new EntityModelLayer(PhantomMod.id("ghost_yeti"), "main");

	private final Kind kind;
	private final ModelPart root, body, head, rightArm, leftArm, rightLeg, leftLeg;
	private final ModelPart extra1, extra2;
	private final float bodyPitch, legY, armY;

	public WalkerModel(ModelPart root, Kind kind, boolean translucent) {
		super(translucent ? RenderLayer::getEntityTranslucent : RenderLayer::getEntityCutoutNoCull);
		this.kind = kind;
		this.root = root;
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.extra1 = kind == Kind.GHOUL ? body.getChild("cloak") : kind == Kind.KNIGHT ? body.getChild("cape") : kind == Kind.SOUL ? leftArm.getChild("lantern") : null;
		this.extra2 = kind == Kind.KNIGHT ? rightArm.getChild("sword") : null;
		this.bodyPitch = body.pitch;
		this.legY = rightLeg.pivotY;
		this.armY = rightArm.pivotY;
	}

	public static TexturedModelData ghoul() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 18).cuboid(-3.5f, 0f, -2f, 7f, 11f, 4f)
				.uv(0, 33).cuboid(-4f, 0f, -2.5f, 8f, 7f, 5f, new Dilation(0.1f)),
				ModelTransform.of(0f, -1f, 0f, 0.3f, 0f, 0f));
		ModelPartData p_cloak = p_body.addChild("cloak", ModelPartBuilder.create()
				.uv(26, 33).cuboid(-4f, 0f, -3f, 8f, 6f, 6f, new Dilation(0f, 0f, -0.4f)),
				ModelTransform.of(0f, 6f, 0f, 0.15f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(22, 18).cuboid(-3.5f, -8f, -4f, 7f, 8f, 7f)
				.uv(24, 54).cuboid(-2.5f, -1f, -4.5f, 5f, 2f, 1f),
				ModelTransform.of(0f, 0f, -0.5f, -0.3f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-2f, -1f, -1.5f, 3f, 15f, 3f)
				.uv(32, 45).cuboid(-2.5f, -1.5f, -2f, 4f, 4f, 4f, new Dilation(0.05f)),
				ModelTransform.of(-5f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_claws = p_right_arm.addChild("right_claws", ModelPartBuilder.create()
				.uv(8, 54).cuboid(-2.1f, 0f, -1.6f, 1f, 3f, 1f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(0, 54).cuboid(-0.9f, -0.25f, -1.6f, 1f, 4f, 1f, new Dilation(-0.1f, -0.25f, -0.1f))
				.uv(12, 54).cuboid(0.3f, 0f, -1.6f, 1f, 3f, 1f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(0f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(12, 0).cuboid(-1f, -1f, -1.5f, 3f, 15f, 3f)
				.uv(48, 45).cuboid(-1.5f, -1.5f, -2f, 4f, 4f, 4f, new Dilation(0.05f)),
				ModelTransform.of(5f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_claws = p_left_arm.addChild("left_claws", ModelPartBuilder.create()
				.uv(16, 54).cuboid(-1.3f, 0f, -1.6f, 1f, 3f, 1f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(4, 54).cuboid(-0.1f, -0.25f, -1.6f, 1f, 4f, 1f, new Dilation(-0.1f, -0.25f, -0.1f))
				.uv(20, 54).cuboid(1.1f, 0f, -1.6f, 1f, 3f, 1f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(0f, 14f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(24, 0).cuboid(-1.5f, 0f, -1.5f, 3f, 14f, 3f)
				.uv(0, 45).cuboid(-2f, 0f, -2f, 4f, 5f, 4f, new Dilation(0.05f)),
				ModelTransform.of(-2f, 10f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-1.5f, 0f, -1.5f, 3f, 14f, 3f)
				.uv(16, 45).cuboid(-2f, 0f, -2f, 4f, 5f, 4f, new Dilation(0.05f)),
				ModelTransform.of(2f, 10f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	public static TexturedModelData specterKnight() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(26, 0).cuboid(-4f, 0f, -2f, 8f, 12f, 4f)
				.uv(32, 17).cuboid(-4.5f, 0f, -3f, 9f, 8f, 6f, new Dilation(0f, 0f, -0.4f))
				.uv(48, 33).cuboid(-4.5f, 9f, -3f, 9f, 3f, 6f, new Dilation(0.05f, 0.05f, -0.35f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_cape = p_body.addChild("cape", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4.5f, 0f, 0f, 9f, 16f, 1f),
				ModelTransform.of(0f, 0.5f, 2.6f, 0.1f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(50, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(102, 17).cuboid(-0.5f, -11f, -3f, 1f, 3f, 7f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(82, 0).cuboid(-3f, -2f, -2f, 4f, 12f, 4f)
				.uv(0, 33).cuboid(-4.25f, -3f, -3f, 6f, 4f, 6f, new Dilation(-0.25f, 0f, -0.4f))
				.uv(78, 33).cuboid(-3.5f, 6f, -2.5f, 5f, 4f, 5f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_sword = p_right_arm.addChild("sword", ModelPartBuilder.create()
				.uv(118, 33).cuboid(-0.5f, -1f, -0.5f, 1f, 4f, 1f)
				.uv(0, 43).cuboid(-2.5f, 2.6f, -1f, 5f, 1f, 2f, new Dilation(0f, 0f, -0.25f))
				.uv(20, 0).cuboid(-1f, 3.6f, -0.5f, 2f, 16f, 1f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(-1f, 10f, -1f, 1.57f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(98, 0).cuboid(-1f, -2f, -2f, 4f, 12f, 4f)
				.uv(24, 33).cuboid(-1.75f, -3f, -3f, 6f, 4f, 6f, new Dilation(-0.25f, 0f, -0.4f))
				.uv(98, 33).cuboid(-1.5f, 6f, -2.5f, 5f, 4f, 5f, new Dilation(-0.1f, 0f, -0.1f)),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(0, 17).cuboid(-2f, 0f, -2f, 4f, 12f, 4f)
				.uv(62, 17).cuboid(-2.5f, 4f, -2.6f, 5f, 8f, 5f, new Dilation(-0.2f, 0f, -0.1f)),
				ModelTransform.of(-2f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(16, 17).cuboid(-2f, 0f, -2f, 4f, 12f, 4f)
				.uv(82, 17).cuboid(-2.5f, 4f, -2.6f, 5f, 8f, 5f, new Dilation(-0.2f, 0f, -0.1f)),
				ModelTransform.of(2f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	public static TexturedModelData lostSoul() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-4f, 0f, -2f, 8f, 12f, 4f)
				.uv(68, 16).cuboid(-4.5f, 8f, -2.5f, 9f, 5f, 5f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(24, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(28, 16).cuboid(-5f, -8.5f, -5f, 10f, 1f, 10f)
				.uv(0, 16).cuboid(-3.5f, -13.25f, -3.5f, 7f, 5f, 7f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(56, 0).cuboid(-3f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(-5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(72, 0).cuboid(-1f, -2f, -2f, 4f, 12f, 4f),
				ModelTransform.of(5f, 2f, 0f, 0f, 0f, 0f));
		ModelPartData p_lantern = p_left_arm.addChild("lantern", ModelPartBuilder.create()
				.uv(108, 16).cuboid(-0.5f, 0f, -0.5f, 1f, 2f, 1f)
				.uv(96, 16).cuboid(-1.5f, 2f, -1.5f, 3f, 4f, 3f),
				ModelTransform.of(1f, 11f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(88, 0).cuboid(-2f, 0f, -2f, 4f, 12f, 4f),
				ModelTransform.of(-2f, 12f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(104, 0).cuboid(-2f, 0f, -2f, 4f, 12f, 4f),
				ModelTransform.of(2f, 12f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 64);
	}

	public static TexturedModelData ghostYeti() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-6.5f, 0f, -4f, 13f, 14f, 8f)
				.uv(32, 22).cuboid(-7f, 10f, -4.5f, 14f, 4f, 9f)
				.uv(32, 38).cuboid(-5f, 1f, -4.7f, 10f, 8f, 1f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, -2f, 0f, 0.12f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(0, 22).cuboid(-4f, -8f, -4.5f, 8f, 8f, 8f)
				.uv(68, 49).cuboid(-3f, -6f, -4.9f, 6f, 6f, 1f)
				.uv(0, 38).cuboid(-4.5f, -5f, -4f, 9f, 4f, 7f, new Dilation(0.2f)),
				ModelTransform.of(0f, 0f, -1f, -0.1f, 0f, 0f));
		ModelPartData p_horn_r = p_head.addChild("horn_r", ModelPartBuilder.create()
				.uv(114, 49).cuboid(-1f, -3f, -1f, 2f, 3f, 2f)
				.uv(8, 58).cuboid(-2.2f, -4.5f, -1f, 2f, 2f, 2f, new Dilation(-0.4f, 0f, -0.4f)),
				ModelTransform.of(-3.5f, -7f, -1f, 0f, 0f, 0.5f));
		ModelPartData p_horn_l = p_head.addChild("horn_l", ModelPartBuilder.create()
				.uv(0, 58).cuboid(-1f, -3f, -1f, 2f, 3f, 2f)
				.uv(16, 58).cuboid(0.2f, -4.5f, -1f, 2f, 2f, 2f, new Dilation(-0.4f, 0f, -0.4f)),
				ModelTransform.of(3.5f, -7f, -1f, 0f, 0f, -0.5f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(42, 0).cuboid(-4f, -2f, -2.5f, 5f, 17f, 5f)
				.uv(78, 22).cuboid(-4.5f, -2.5f, -3f, 6f, 6f, 6f)
				.uv(54, 38).cuboid(-4.5f, 11f, -3f, 6f, 3f, 6f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(82, 49).cuboid(-3.5f, 15f, -2f, 4f, 3f, 4f),
				ModelTransform.of(-8.5f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(62, 0).cuboid(-1f, -2f, -2.5f, 5f, 17f, 5f)
				.uv(102, 22).cuboid(-1.5f, -2.5f, -3f, 6f, 6f, 6f)
				.uv(78, 38).cuboid(-1.5f, 11f, -3f, 6f, 3f, 6f, new Dilation(-0.1f, 0f, -0.1f))
				.uv(98, 49).cuboid(-0.5f, 15f, -2f, 4f, 3f, 4f),
				ModelTransform.of(8.5f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(82, 0).cuboid(-2.5f, 0f, -2.5f, 5f, 12f, 5f)
				.uv(102, 38).cuboid(-3f, 7f, -3f, 6f, 3f, 6f)
				.uv(24, 49).cuboid(-2.5f, 10.25f, -4f, 5f, 2f, 6f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-3.5f, 12f, 0.5f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(102, 0).cuboid(-2.5f, 0f, -2.5f, 5f, 12f, 5f)
				.uv(0, 49).cuboid(-3f, 7f, -3f, 6f, 3f, 6f)
				.uv(46, 49).cuboid(-2.5f, 10.25f, -4f, 5f, 2f, 6f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(3.5f, 12f, 0.5f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(T e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float stride = kind == Kind.YETI ? 0.5f : 0.6662f;
		float swing = MathHelper.cos(limbAngle * stride) * (kind == Kind.YETI ? 1.0f : 1.3f) * Math.min(1f, limbDistance);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE + (kind == Kind.GHOUL ? -0.3f : kind == Kind.YETI ? -0.1f : 0f);
		body.pitch = bodyPitch;
		body.roll = 0;
		rightLeg.pivotY = leftLeg.pivotY = legY;
		rightArm.pivotY = leftArm.pivotY = armY;
		rightLeg.pitch = swing;
		leftLeg.pitch = -swing;
		rightLeg.yaw = leftLeg.yaw = 0;
		float idle = MathHelper.sin(age * 0.07f) * 0.05f;
		rightArm.pitch = -swing * 0.8f + idle;
		leftArm.pitch = swing * 0.8f - idle;
		rightArm.roll = 0.05f + idle;
		leftArm.roll = -0.05f - idle;
		rightArm.yaw = leftArm.yaw = 0;
		float attack = MathHelper.sin(handSwingProgress * MathHelper.PI);
		switch (kind) {
			case GHOUL -> {
				// arms dangle forward, claws twitch, lunging swipe
				rightArm.pitch = -0.45f - swing * 0.4f + MathHelper.sin(age * 0.3f) * 0.04f - attack * 1.4f;
				leftArm.pitch = -0.45f + swing * 0.4f + MathHelper.cos(age * 0.33f) * 0.04f - attack * 0.6f;
				body.pitch = bodyPitch + attack * 0.3f;
				extra1.pitch = 0.15f + Math.abs(swing) * 0.25f + MathHelper.sin(age * 0.12f) * 0.05f;
			}
			case KNIGHT -> {
				// sword raised at the ready, a heavy overhead chop when attacking
				rightArm.pitch = -0.5f - swing * 0.3f - attack * 1.9f;
				rightArm.yaw = -0.1f;
				leftArm.pitch = swing * 0.6f;
				extra2.pitch = 1.57f + attack * 0.5f;
				extra1.pitch = 0.1f + Math.abs(swing) * 0.35f + MathHelper.sin(age * 0.1f) * 0.06f;
			}
			case SOUL -> {
				// a gentle drifting walk, lantern held out and swaying
				leftArm.pitch = -0.55f + MathHelper.sin(age * 0.08f) * 0.06f;
				extra1.pitch = 0.55f - MathHelper.sin(age * 0.08f) * 0.06f + MathHelper.sin(age * 0.1f) * 0.1f;
				float bob = MathHelper.sin(age * 0.09f) * 0.6f;
				body.pivotY = bob;
				rightArm.pivotY = armY + bob;
				leftArm.pivotY = armY + bob;
			}
			case YETI -> {
				// lumbering side-to-side gait; huge two-armed smash when attacking
				body.roll = MathHelper.sin(limbAngle * stride) * 0.08f * Math.min(1f, limbDistance);
				rightArm.pitch = -swing * 0.6f + idle - attack * 2.2f;
				leftArm.pitch = swing * 0.6f - idle - attack * 2.2f;
				rightArm.roll = 0.15f + idle;
				leftArm.roll = -0.15f - idle;
				if (e instanceof GhostYetiEntity y && y.isSitting()) {
					// plopped down on its bottom, legs out front, arms resting on its knees
					rightLeg.pitch = leftLeg.pitch = -1.45f;
					rightLeg.yaw = 0.25f;
					leftLeg.yaw = -0.25f;
					rightLeg.pivotY = leftLeg.pivotY = legY + 9f;
					body.pivotY = 9f;
					rightArm.pivotY = leftArm.pivotY = armY + 9f;
					rightArm.pitch = leftArm.pitch = -0.7f;
				} else body.pivotY = -2f;
			}
		}
	}
}
