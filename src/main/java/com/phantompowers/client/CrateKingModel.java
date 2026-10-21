package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CrateKingEntity;
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
 * The Crate King: a hunched, shouting ghost who wears a shipping crate as armor and an open crate as a crown
 * (its flaps sticking up like crown points). Packing tape wraps his forearms, a shipping-label sash crosses his chest,
 * and he stomps around in taped work trousers and boots made from crates. Unboxed, he holds Pandora's Box.
 */
public class CrateKingModel extends SinglePartEntityModel<CrateKingEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("crate_king"), "main");
	private final ModelPart root;
	private final ModelPart head, body, box, lid, rightArm, leftArm, rightLeg, leftLeg;
	private final ModelPart[] flaps = new ModelPart[4];

	public CrateKingModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.box = body.getChild("box");
		this.lid = box.getChild("lid");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		ModelPart crown = head.getChild("crown");
		this.flaps[0] = crown.getChild("flap_front");
		this.flaps[1] = crown.getChild("flap_back");
		this.flaps[2] = crown.getChild("flap_right");
		this.flaps[3] = crown.getChild("flap_left");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_head = r.addChild("head", ModelPartBuilder.create()
				.uv(2, 2).cuboid(-4.5f, -8f, -4f, 9f, 8f, 8f)
				.uv(39, 2).cuboid(-0.7f, -4.3f, -4.5f, 1.4f, 1.1f, 0.8f)
				.uv(47, 2).cuboid(-4.65f, -5.6f, -0.6f, 0.5f, 2f, 1.6f)
				.uv(55, 2).cuboid(4.15f, -5.6f, -0.6f, 0.5f, 2f, 1.6f),
				ModelTransform.of(0f, 0.5f, -1.5f, 0f, 0f, 0f));
		ModelPartData p_cap = p_head.addChild("cap", ModelPartBuilder.create()
				.uv(63, 2).cuboid(-4.7f, -10f, -4.2f, 9.4f, 2.1f, 8.4f)
				.uv(102, 2).cuboid(-4.2f, -10.35f, -3.8f, 8.4f, 0.4f, 7.6f)
				.uv(137, 2).cuboid(-4.78f, -8.65f, -4.28f, 9.56f, 0.7f, 8.56f)
				.uv(177, 2).cuboid(-4.85f, -8.6f, -4.85f, 9.7f, 0.45f, 1.5f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_crown = p_head.addChild("crown", ModelPartBuilder.create(),
				ModelTransform.of(0f, -7.6f, 0f, 0f, 0f, 0f));
		ModelPartData p_flap_front = p_crown.addChild("flap_front", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_flap_back = p_crown.addChild("flap_back", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_flap_right = p_crown.addChild("flap_right", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_flap_left = p_crown.addChild("flap_left", ModelPartBuilder.create(),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(203, 2).cuboid(-5.5f, 0f, -3.5f, 11f, 11.5f, 7f)
				.uv(2, 24).cuboid(-5.6f, 6f, -3.6f, 11.2f, 6.2f, 7.2f)
				.uv(42, 24).cuboid(-4.2f, 2.9f, -3.68f, 8.4f, 5.2f, 0.28f)
				.uv(63, 24).cuboid(-4.7f, -0.05f, -3.65f, 1.15f, 4.6f, 0.25f)
				.uv(69, 24).cuboid(-4.6f, 3.2f, -3.98f, 0.95f, 1f, 0.35f)
				.uv(75, 24).cuboid(-4.7f, -0.05f, 3.45f, 1.15f, 5.9f, 0.3f)
				.uv(81, 24).cuboid(3.55f, -0.05f, -3.65f, 1.15f, 4.6f, 0.25f)
				.uv(87, 24).cuboid(3.65f, 3.2f, -3.98f, 0.95f, 1f, 0.35f)
				.uv(93, 24).cuboid(3.55f, -0.05f, 3.45f, 1.15f, 5.9f, 0.3f)
				.uv(99, 24).cuboid(-2.65f, 4.7f, -4.02f, 5.3f, 3.2f, 0.35f),
				ModelTransform.of(0f, 0f, 0f, 0.2f, 0f, 0f));
		ModelPartData p_box = p_body.addChild("box", ModelPartBuilder.create(),
				ModelTransform.of(0f, 7f, -9f, 0f, 0f, 0f));
		ModelPartData p_lid = p_box.addChild("lid", ModelPartBuilder.create(),
				ModelTransform.of(0f, -3f, 3f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(114, 24).cuboid(-2.55f, -1.1f, -2.6f, 4.6f, 6.2f, 5.2f)
				.uv(137, 24).cuboid(-2.5f, 5f, -2.5f, 4.5f, 6.4f, 5f)
				.uv(159, 24).cuboid(-2.45f, 10.3f, -2.55f, 4.4f, 1.5f, 5.05f),
				ModelTransform.of(-7f, 1.5f, -1f, 0f, 0f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(84, 41).cuboid(-2.05f, -1.1f, -2.6f, 4.6f, 6.2f, 5.2f)
				.uv(107, 41).cuboid(-2f, 5f, -2.5f, 4.5f, 6.4f, 5f)
				.uv(129, 41).cuboid(-1.95f, 10.3f, -2.55f, 4.4f, 1.5f, 5.05f),
				ModelTransform.of(7f, 1.5f, -1f, 0f, 0f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(181, 24).cuboid(-2.6f, 0f, -3.1f, 5.2f, 7.6f, 6.2f)
				.uv(207, 24).cuboid(-2.64f, 7.25f, -3.14f, 5.28f, 0.75f, 6.28f)
				.uv(2, 41).cuboid(-2.5f, 8f, -3.05f, 5f, 2.1f, 6.1f)
				.uv(28, 41).cuboid(-2.65f, 9f, -4f, 5.3f, 1.3f, 7f)
				.uv(56, 41).cuboid(-2.7f, 10f, -4.05f, 5.4f, 0.4f, 7.1f),
				ModelTransform.of(-2.8f, 12f, 2f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(151, 41).cuboid(-2.6f, 0f, -3.1f, 5.2f, 7.6f, 6.2f)
				.uv(177, 41).cuboid(-2.64f, 7.25f, -3.14f, 5.28f, 0.75f, 6.28f)
				.uv(204, 41).cuboid(-2.5f, 8f, -3.05f, 5f, 2.1f, 6.1f)
				.uv(2, 58).cuboid(-2.65f, 9f, -4f, 5.3f, 1.3f, 7f)
				.uv(30, 58).cuboid(-2.7f, 10f, -4.05f, 5.4f, 0.4f, 7.1f),
				ModelTransform.of(2.8f, 12f, 2f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 256);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(CrateKingEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		boolean evil = e.isEvil();
		float bob = MathHelper.sin(age * 0.08f) * 0.8f;
		float hurt = e.hurtTime > 0 ? MathHelper.sin((e.hurtTime - (age - (int) age)) / 10f * MathHelper.PI) : 0f;
		float swing = MathHelper.sin(e.handSwingProgress * MathHelper.PI);
		float swingUp = MathHelper.sin(MathHelper.sqrt(e.handSwingProgress) * MathHelper.PI);

		// Hunched body; rears back when hurt, lunges forward when swinging
		body.pivotY = bob;
		body.pitch = 0.2f + swing * 0.25f - hurt * 0.4f + limbDistance * 0.15f;
		head.pivotY = 0.5f + bob + swing * 0.8f;
		head.pivotZ = -1.5f - swing * 1.5f + hurt * 1.2f;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE - hurt * 0.5f;
		head.roll = hurt * 0.25f + MathHelper.sin(age * 0.6f) * 0.03f * (evil ? 1f : 0.3f);
		rightArm.pivotY = 1.5f + bob;
		leftArm.pivotY = 1.5f + bob;

		// The crate-crown flaps flutter
		for (int i = 0; i < 4; i++) {
			float flutter = MathHelper.sin(age * 0.15f + i * 1.7f) * 0.06f + hurt * 0.25f;
			if (i < 2) flaps[i].pitch = (i == 0 ? 0.25f + flutter : -0.25f - flutter);
			else flaps[i].roll = (i == 2 ? -0.25f - flutter : 0.25f + flutter);
		}

		// Stomping legs in crate boots; knees buckle when hurt
		float stride = MathHelper.cos(limbAngle * 0.6662f) * 1.0f * limbDistance;
		rightLeg.pivotY = 12f + bob;
		leftLeg.pivotY = 12f + bob;
		rightLeg.pitch = stride - swing * 0.35f - hurt * 0.3f;
		leftLeg.pitch = -stride + swing * 0.5f + hurt * 0.2f;
		rightLeg.roll = 0.04f + hurt * 0.12f;
		leftLeg.roll = -0.04f - hurt * 0.12f;

		box.visible = evil;
		if (evil) {
			// Both fists clamp Pandora's Box out in front; the lid creaks open
			rightArm.pitch = -1.15f + MathHelper.sin(age * 0.2f) * 0.05f - hurt * 0.3f;
			rightArm.yaw = -0.35f;
			rightArm.roll = 0f;
			leftArm.pitch = -1.15f + MathHelper.sin(age * 0.2f) * 0.05f - hurt * 0.3f;
			leftArm.yaw = 0.35f;
			leftArm.roll = 0f;
			lid.pitch = -e.lidOpen() * 1.3f;
			box.yaw = MathHelper.sin(age * 0.05f) * 0.1f;
		} else {
			// Heavy fists swing as he drifts; wind-up overhead and slam when attacking; flung wide when hurt
			float walk = MathHelper.cos(limbAngle * 0.6662f) * 0.7f * limbDistance;
			float idle = MathHelper.sin(age * 0.07f) * 0.06f;
			rightArm.pitch = -0.2f - walk - swingUp * 2.6f + swing * 0.9f + idle;
			rightArm.yaw = swing * 0.3f;
			rightArm.roll = 0.15f + hurt * 0.9f;
			leftArm.pitch = -0.2f + walk + swing * 0.6f - idle;
			leftArm.yaw = -swing * 0.2f;
			leftArm.roll = -0.15f - hurt * 0.9f;
		}

	}
}
