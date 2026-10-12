package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.VoltageEntity;
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
 * Voltage: a punk-rock ghost girl. Spiky side-swept pink hair, studded leather jacket, plaid skirt,
 * torn fishnets and chunky boots, with her electric guitar slung across her.
 * In her Encore her hair blazes up into a tall electric-pink flame.
 */
public class VoltageModel extends SinglePartEntityModel<VoltageEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("voltage"), "main");
	private static final int FLAMES = 7;

	private final ModelPart root, head, hair, spikes, sideLock, backHair, flames, body, rightArm, leftArm, rightLeg, leftLeg, guitar, skirt;
	private final ModelPart[] flame = new ModelPart[FLAMES];

	public VoltageModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.hair = head.getChild("hair");
		this.spikes = hair.getChild("spikes");
		this.sideLock = hair.getChild("side_lock");
		this.backHair = hair.getChild("back_hair");
		this.flames = head.getChild("flames");
		for (int i = 0; i < FLAMES; i++) flame[i] = flames.getChild("flame" + i);
		this.body = root.getChild("body");
		this.skirt = body.getChild("skirt");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.guitar = body.getChild("guitar");
	}

	/** Flame tongue: [x, y, z, pitch, roll, size] */
	private static final float[][] FLAME_SPOTS = {
			{0f, -11f, 0.5f, -0.25f, 0f, 1.5f},
			{-2f, -11f, -1f, -0.15f, 0.25f, 1.15f},
			{2f, -11f, -1f, -0.15f, -0.25f, 1.15f},
			{-2f, -11f, 2f, -0.45f, 0.2f, 1.25f},
			{2f, -11f, 2f, -0.45f, -0.2f, 1.25f},
			{0f, -11f, 3f, -0.6f, 0f, 1.3f},
			{0f, -11f, -2.5f, -0.1f, 0f, 0.9f},
	};

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();

		ModelPartData head = r.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4f, -8f, -4f, 8, 8, 8),
				ModelTransform.pivot(0f, 0f, 0f));

		// --- Stage one hair: side-swept punk spikes
		ModelPartData hair = head.addChild("hair", ModelPartBuilder.create()
						.uv(32, 0).cuboid(-4.5f, -8.6f, -4.5f, 9, 3, 9)        // cap
						.uv(68, 0).cuboid(-4.6f, -6.0f, -4.8f, 6, 2, 1),       // swept bangs
				ModelTransform.NONE);
		hair.addChild("side_lock", ModelPartBuilder.create().uv(108, 24).cuboid(-5.1f, -7f, -4f, 1, 9, 5), ModelTransform.NONE);
		hair.addChild("back_hair", ModelPartBuilder.create().uv(90, 0).cuboid(-4f, -7.6f, 4.1f, 8, 10, 1), ModelTransform.NONE);
		ModelPartData spikes = hair.addChild("spikes", ModelPartBuilder.create(), ModelTransform.NONE);
		float[][] spikeSpots = {{-2.5f, -8.3f, -2f, -0.25f, 0.55f}, {0f, -8.4f, -1.3f, -0.4f, 0.1f}, {2.5f, -8.3f, -0.8f, -0.35f, -0.45f},
				{-1.5f, -8.2f, 2f, 0.55f, 0.3f}, {1.5f, -8.2f, 2f, 0.55f, -0.3f}, {0f, -8.3f, 1f, 0.2f, 0f}};
		for (int i = 0; i < spikeSpots.length; i++) {
			float[] s = spikeSpots[i];
			spikes.addChild("spike" + i, ModelPartBuilder.create()
							.uv(82, 0).cuboid(-1f, -3f, -1f, 2, 3, 2)
							.uv(82, 6).cuboid(-0.5f, -5.5f, -0.5f, 1, 3, 1),
					ModelTransform.of(s[0], s[1], s[2], s[3], 0f, s[4]));
		}

		// --- Encore: a tall blazing flame of hair
		ModelPartData flames = head.addChild("flames", ModelPartBuilder.create().uv(0, 88).cuboid(-4.5f, -12f, -4.5f, 9, 4, 9), ModelTransform.NONE);
		for (int i = 0; i < FLAME_SPOTS.length; i++) {
			float[] f = FLAME_SPOTS[i];
			flames.addChild("flame" + i, ModelPartBuilder.create()
							.uv(108, 0).cuboid(-2f, -4f, -2f, 4, 4, 4)
							.uv(108, 8).cuboid(-1.5f, -8f, -1.5f, 3, 4, 3)
							.uv(120, 8).cuboid(-1f, -11f, -1f, 2, 3, 2)
							.uv(108, 16).cuboid(-0.5f, -13.5f, -0.5f, 1, 3, 1),
					ModelTransform.of(f[0], f[1], f[2], f[3], 0f, f[4]));
		}

		// --- Body: crop tee, leather jacket, studded belt, plaid skirt
		ModelPartData body = r.addChild("body", ModelPartBuilder.create()
						.uv(0, 16).cuboid(-4f, 0f, -2f, 8, 12, 4)
						.uv(24, 16).cuboid(-4f, 0f, -2f, 8, 8, 4, new Dilation(0.35f)),
				ModelTransform.pivot(0f, 0f, 0f));
		body.addChild("skirt", ModelPartBuilder.create()
						.uv(48, 16).cuboid(-4.5f, 9.5f, -2.5f, 9, 3, 5)
						.uv(76, 16).cuboid(-5f, 12f, -3f, 10, 2, 6),
				ModelTransform.NONE);
		// Shoulder studs on the jacket
		body.addChild("studs", ModelPartBuilder.create()
						.uv(96, 32).cuboid(-4.4f, -0.9f, -1.5f, 1, 1, 1)
						.uv(96, 32).cuboid(-4.4f, -0.9f, 0.5f, 1, 1, 1)
						.uv(96, 32).cuboid(3.4f, -0.9f, -1.5f, 1, 1, 1)
						.uv(96, 32).cuboid(3.4f, -0.9f, 0.5f, 1, 1, 1),
				ModelTransform.NONE);

		// --- Arms (slim) with leather sleeves and spiked wristbands
		ModelPartData rightArm = r.addChild("right_arm", ModelPartBuilder.create().uv(0, 32).cuboid(-2f, -2f, -1.5f, 3, 12, 3),
				ModelTransform.pivot(-5f, 2f, 0f));
		rightArm.addChild("right_band", ModelPartBuilder.create().uv(96, 36).cuboid(-2f, 7f, -1.5f, 3, 2, 3, new Dilation(0.3f)), ModelTransform.NONE);
		ModelPartData leftArm = r.addChild("left_arm", ModelPartBuilder.create().uv(12, 32).cuboid(-1f, -2f, -1.5f, 3, 12, 3),
				ModelTransform.pivot(5f, 2f, 0f));
		leftArm.addChild("left_band", ModelPartBuilder.create().uv(96, 36).cuboid(-1f, 7f, -1.5f, 3, 2, 3, new Dilation(0.3f)), ModelTransform.NONE);

		// --- Two legs: torn fishnets and chunky platform boots
		ModelPartData rightLeg = r.addChild("right_leg", ModelPartBuilder.create().uv(24, 32).cuboid(-2f, 0f, -2f, 4, 12, 4, new Dilation(-0.25f)),
				ModelTransform.pivot(-1.9f, 12f, 0f));
		rightLeg.addChild("right_boot", ModelPartBuilder.create().uv(56, 32).cuboid(-2.5f, 6.5f, -2.8f, 5, 5, 5)
				.uv(56, 42).cuboid(-2.6f, 11.2f, -3f, 5.2f, 1, 5.6f), ModelTransform.NONE);
		ModelPartData leftLeg = r.addChild("left_leg", ModelPartBuilder.create().uv(40, 32).cuboid(-2f, 0f, -2f, 4, 12, 4, new Dilation(-0.25f)),
				ModelTransform.pivot(1.9f, 12f, 0f));
		leftLeg.addChild("left_boot", ModelPartBuilder.create().uv(76, 32).cuboid(-2.5f, 6.5f, -2.8f, 5, 5, 5)
				.uv(56, 42).cuboid(-2.6f, 11.2f, -3f, 5.2f, 1, 5.6f), ModelTransform.NONE);

		// --- Her electric guitar, slung across her front
		ModelPartData guitar = body.addChild("guitar", ModelPartBuilder.create()
						.uv(0, 64).cuboid(-4f, -3.5f, -1f, 8, 7, 2)
						.uv(20, 64).cuboid(-3f, 3.5f, -1f, 6, 2, 2)
						.uv(0, 74).cuboid(-4.5f, -6f, -1f, 3, 3, 2)          // left horn
						.uv(10, 74).cuboid(1.5f, -5f, -1f, 2.5f, 2, 2)        // right horn
						.uv(36, 64).cuboid(-0.75f, -15f, -0.75f, 1.5f, 11.5f, 1.5f)
						.uv(44, 64).cuboid(-1.5f, -18.5f, -1f, 3, 3.5f, 1.5f)
						.uv(54, 64).cuboid(-2f, -1.5f, -1.4f, 4, 1, 0.5f)
						.uv(54, 64).cuboid(-2f, 1f, -1.4f, 4, 1, 0.5f),
				ModelTransform.of(0.5f, 7f, -3.6f, 0f, 0f, 0.95f));
		guitar.addChild("strings", ModelPartBuilder.create().uv(64, 64).cuboid(-0.5f, -15f, -1.2f, 1, 18, 0.1f),
				ModelTransform.NONE);
		return TexturedModelData.of(data, 128, 128);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(VoltageEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		boolean encore = e.isEncore();
		float bob = MathHelper.sin(age * 0.09f) * 0.8f;
		head.pivotY = bob;
		body.pivotY = bob;
		rightArm.pivotY = 2f + bob;
		leftArm.pivotY = 2f + bob;
		rightLeg.pivotY = 12f + bob;
		leftLeg.pivotY = 12f + bob;

		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		// Headbanging while she shreds
		float strum = e.strum();
		head.pitch += strum * MathHelper.sin(age * 1.2f) * 0.35f;

		// Hair: spiky in stage one, a blazing flame in her Encore
		spikes.visible = !encore;
		sideLock.visible = !encore;
		backHair.visible = !encore;
		flames.visible = encore;
		hair.roll = encore ? 0f : MathHelper.sin(age * 0.25f) * 0.04f;
		if (encore) {
			for (int i = 0; i < FLAMES; i++) {
				float[] f = FLAME_SPOTS[i];
				float flick = MathHelper.sin(age * 0.55f + i * 1.7f);
				flame[i].roll = f[4] + MathHelper.sin(age * 0.3f + i) * 0.12f;
				flame[i].pitch = f[3] + MathHelper.cos(age * 0.27f + i * 0.6f) * 0.1f;
				flame[i].xScale = f[5] * (1f - flick * 0.06f);
				flame[i].zScale = f[5] * (1f - flick * 0.06f);
				flame[i].yScale = f[5] * (1f + flick * 0.16f + strum * 0.15f);
			}
		}

		// Skirt sways
		skirt.pitch = MathHelper.sin(age * 0.11f) * 0.04f + limbDistance * 0.1f;

		// Legs: stage stance, swinging as she moves, a little kick to the beat
		float walk = MathHelper.cos(limbAngle * 0.6662f) * 0.9f * limbDistance;
		float dangle = MathHelper.sin(age * 0.09f) * 0.06f;
		rightLeg.pitch = walk + dangle;
		leftLeg.pitch = -walk - dangle;
		rightLeg.roll = 0.09f;
		leftLeg.roll = -0.09f;
		rightLeg.yaw = 0.1f;
		leftLeg.yaw = -0.1f;
		if (strum > 0.3f) leftLeg.pitch -= strum * 0.25f * Math.max(0f, MathHelper.sin(age * 0.6f));

		// Fretting hand on the neck, strumming hand on the strings
		leftArm.pitch = -1.25f;
		leftArm.yaw = -0.35f;
		leftArm.roll = -0.55f;
		rightArm.pitch = -0.55f + MathHelper.sin(age * (0.8f + strum)) * (0.12f + strum * 0.35f);
		rightArm.yaw = 0.45f;
		rightArm.roll = 0.2f;
		guitar.roll = 0.95f + strum * 0.08f * MathHelper.sin(age * 1.2f);
	}
}
