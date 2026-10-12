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
				.uv(42, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f),
				ModelTransform.of(0f, 0.5f, -1.5f, 0f, 0f, 0f));
		ModelPartData p_crown = p_head.addChild("crown", ModelPartBuilder.create()
				.uv(40, 19).cuboid(-5f, -3f, -5f, 10f, 3f, 10f, new Dilation(0.1f, 0f, 0.1f)),
				ModelTransform.of(0f, -7.6f, 0f, 0f, 0f, 0f));
		ModelPartData p_flap_front = p_crown.addChild("flap_front", ModelPartBuilder.create()
				.uv(100, 44).cuboid(-5f, -4f, -0.5f, 10f, 4f, 1f, new Dilation(0.1f, 0f, -0.3f)),
				ModelTransform.of(0f, -3f, -5f, 0.25f, 0f, 0f));
		ModelPartData p_flap_back = p_crown.addChild("flap_back", ModelPartBuilder.create()
				.uv(0, 53).cuboid(-5f, -4f, -0.5f, 10f, 4f, 1f, new Dilation(0.1f, 0f, -0.3f)),
				ModelTransform.of(0f, -3f, 5f, -0.25f, 0f, 0f));
		ModelPartData p_flap_right = p_crown.addChild("flap_right", ModelPartBuilder.create()
				.uv(74, 0).cuboid(-0.5f, -4f, -5f, 1f, 4f, 10f, new Dilation(-0.3f, 0f, 0.1f)),
				ModelTransform.of(-5f, -3f, 0f, 0f, 0f, -0.25f));
		ModelPartData p_flap_left = p_crown.addChild("flap_left", ModelPartBuilder.create()
				.uv(96, 0).cuboid(-0.5f, -4f, -5f, 1f, 4f, 10f, new Dilation(-0.3f, 0f, 0.1f)),
				ModelTransform.of(5f, -3f, 0f, 0f, 0f, 0.25f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-5.5f, 0f, -3.5f, 11f, 12f, 7f),
				ModelTransform.of(0f, 0f, 0f, 0.2f, 0f, 0f));
		ModelPartData p_sash = p_body.addChild("sash", ModelPartBuilder.create()
				.uv(36, 0).cuboid(-1f, -8.5f, -0.5f, 2f, 17f, 1f, new Dilation(-0.1f, 0f, -0.3f)),
				ModelTransform.of(0f, 6f, -3.7f, 0f, 0f, -0.65f));
		ModelPartData p_box = p_body.addChild("box", ModelPartBuilder.create()
				.uv(80, 19).cuboid(-3f, -3f, -3f, 6f, 6f, 6f),
				ModelTransform.of(0f, 7f, -9f, 0f, 0f, 0f));
		ModelPartData p_lid = p_box.addChild("lid", ModelPartBuilder.create()
				.uv(68, 33).cuboid(-3.5f, -1.6f, -7f, 7f, 2f, 7f, new Dilation(0f, -0.2f, 0f)),
				ModelTransform.of(0f, -3f, 3f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(104, 19).cuboid(-3f, -2f, -2f, 4f, 7f, 4f)
				.uv(96, 33).cuboid(-3f, 5f, -2f, 4f, 5f, 4f, new Dilation(0.25f, 0f, 0.25f))
				.uv(0, 44).cuboid(-3.5f, 9.5f, -2.5f, 5f, 4f, 5f),
				ModelTransform.of(-7f, 1.5f, -1f, 0f, 0f, 0f));
		ModelPartData p_right_pad = p_right_arm.addChild("right_pad", ModelPartBuilder.create()
				.uv(20, 44).cuboid(-4f, -3f, -3f, 5f, 3f, 6f, new Dilation(0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, -0.15f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(0, 33).cuboid(-1f, -2f, -2f, 4f, 7f, 4f)
				.uv(42, 44).cuboid(-1f, 5f, -2f, 4f, 5f, 4f, new Dilation(0.25f, 0f, 0.25f))
				.uv(58, 44).cuboid(-1.5f, 9.5f, -2.5f, 5f, 4f, 5f),
				ModelTransform.of(7f, 1.5f, -1f, 0f, 0f, 0f));
		ModelPartData p_left_pad = p_left_arm.addChild("left_pad", ModelPartBuilder.create()
				.uv(78, 44).cuboid(-1f, -3f, -3f, 5f, 3f, 6f, new Dilation(0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0.15f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(0, 19).cuboid(-2.5f, 0f, -2.5f, 5f, 9f, 5f)
				.uv(16, 33).cuboid(-3f, 8.25f, -3.5f, 6f, 4f, 7f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(-2.8f, 12f, 2f, 0f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(20, 19).cuboid(-2.5f, 0f, -2.5f, 5f, 9f, 5f)
				.uv(42, 33).cuboid(-3f, 8.25f, -3.5f, 6f, 4f, 7f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(2.8f, 12f, 2f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 128, 128);
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
