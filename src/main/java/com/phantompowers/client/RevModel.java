package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RevEntity;
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

/** Rev on his black sport bike: spinning wheels, engine rumble, tucked riding pose, leaning into a ram. */
@SuppressWarnings("unused")
public class RevModel extends SinglePartEntityModel<RevEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("rev"), "main");
	private final ModelPart root;
	private final ModelPart bike, frontWheel, rearWheel, body, head, rightArm, leftArm, rightLeg, leftLeg;

	public RevModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.bike = root.getChild("bike");
		this.frontWheel = bike.getChild("front_wheel");
		this.rearWheel = bike.getChild("rear_wheel");
		this.body = root.getChild("body");
		this.head = body.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_bike = r.addChild("bike", ModelPartBuilder.create()
				.uv(124, 0).cuboid(-3f, 8f, -8f, 6f, 5f, 10f)
				.uv(0, 18).cuboid(-3.5f, 10f, -13f, 7f, 6f, 6f)
				.uv(102, 30).cuboid(-3f, 9f, -15f, 6f, 4f, 2f)
				.uv(0, 0).cuboid(-4f, 13f, -10f, 8f, 4f, 14f)
				.uv(168, 0).cuboid(-2.5f, 16.5f, -7f, 5f, 2f, 11f)
				.uv(0, 30).cuboid(-2.5f, 8.5f, 2f, 5f, 2f, 6f)
				.uv(22, 30).cuboid(-2.5f, 7.2f, 7f, 5f, 2f, 5f)
				.uv(26, 18).cuboid(-3f, 12f, -4f, 6f, 4f, 7f)
				.uv(56, 38).cuboid(-4.5f, 14f, -2f, 1f, 1f, 1f)
				.uv(60, 38).cuboid(3.5f, 14f, -2f, 1f, 1f, 1f)
				.uv(24, 38).cuboid(-4f, 10.5f, 4f, 1f, 1f, 2f)
				.uv(30, 38).cuboid(3f, 10.5f, 4f, 1f, 1f, 2f)
				.uv(52, 18).cuboid(-1.5f, 15.5f, 6f, 3f, 1f, 10f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_windscreen = p_bike.addChild("windscreen", ModelPartBuilder.create()
				.uv(146, 30).cuboid(-2.5f, -4f, -0.5f, 5f, 4f, 1f, new Dilation(0f, 0f, -0.2f)),
				ModelTransform.of(0f, 8f, -12f, -0.75f, 0f, 0f));
		ModelPartData p_tail = p_bike.addChild("tail", ModelPartBuilder.create()
				.uv(78, 18).cuboid(-2.5f, -1f, 0f, 5f, 3f, 8f)
				.uv(186, 30).cuboid(-1.5f, 1.5f, 6f, 3f, 1f, 3f),
				ModelTransform.of(0f, 7.5f, 10f, -0.25f, 0f, 0f));
		ModelPartData p_muffler = p_bike.addChild("muffler", ModelPartBuilder.create()
				.uv(104, 18).cuboid(-1f, -1f, 0f, 2f, 2f, 9f, new Dilation(0.2f, 0.2f, 0f))
				.uv(198, 30).cuboid(-0.5f, -0.5f, -3f, 1f, 1f, 3f),
				ModelTransform.of(2.5f, 14f, 4f, -0.25f, 0f, 0f));
		ModelPartData p_fork = p_bike.addChild("fork", ModelPartBuilder.create()
				.uv(156, 0).cuboid(-2.5f, 0f, -0.75f, 1f, 13f, 2f, new Dilation(0.15f, 0f, -0.1f))
				.uv(162, 0).cuboid(1.5f, 0f, -0.75f, 1f, 13f, 2f, new Dilation(0.15f, 0f, -0.1f))
				.uv(206, 30).cuboid(-3f, 0f, -1f, 6f, 2f, 2f),
				ModelTransform.of(0f, 7f, -11f, -0.38f, 0f, 0f));
		ModelPartData p_handlebars = p_bike.addChild("handlebars", ModelPartBuilder.create()
				.uv(64, 38).cuboid(-5f, -0.5f, -0.5f, 3f, 1f, 1f)
				.uv(72, 38).cuboid(2f, -0.5f, -0.5f, 3f, 1f, 1f)
				.uv(222, 30).cuboid(-6f, -1f, -1f, 2f, 2f, 2f)
				.uv(230, 30).cuboid(4f, -1f, -1f, 2f, 2f, 2f)
				.uv(80, 38).cuboid(-5f, -2.5f, -2.5f, 2f, 1f, 1f)
				.uv(86, 38).cuboid(3f, -2.5f, -2.5f, 2f, 1f, 1f),
				ModelTransform.of(0f, 8f, -10f, 0f, 0f, 0f));
		ModelPartData p_front_wheel = p_bike.addChild("front_wheel", ModelPartBuilder.create()
				.uv(118, 30).cuboid(-2f, -1.5f, -1.5f, 4f, 3f, 3f)
				.uv(158, 18).cuboid(-0.6f, -2.5f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(0f, 18f, -16.5f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_t0 = p_front_wheel.addChild("front_wheel_t0", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_t1 = p_front_wheel.addChild("front_wheel_t1", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0.45f, 0f, 0f));
		ModelPartData p_front_wheel_t2 = p_front_wheel.addChild("front_wheel_t2", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0.9f, 0f, 0f));
		ModelPartData p_front_wheel_t3 = p_front_wheel.addChild("front_wheel_t3", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 1.35f, 0f, 0f));
		ModelPartData p_front_wheel_t4 = p_front_wheel.addChild("front_wheel_t4", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 1.8f, 0f, 0f));
		ModelPartData p_front_wheel_t5 = p_front_wheel.addChild("front_wheel_t5", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 2.24f, 0f, 0f));
		ModelPartData p_front_wheel_t6 = p_front_wheel.addChild("front_wheel_t6", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 2.69f, 0f, 0f));
		ModelPartData p_front_wheel_t7 = p_front_wheel.addChild("front_wheel_t7", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 3.14f, 0f, 0f));
		ModelPartData p_front_wheel_t8 = p_front_wheel.addChild("front_wheel_t8", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 3.59f, 0f, 0f));
		ModelPartData p_front_wheel_t9 = p_front_wheel.addChild("front_wheel_t9", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.04f, 0f, 0f));
		ModelPartData p_front_wheel_t10 = p_front_wheel.addChild("front_wheel_t10", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.49f, 0f, 0f));
		ModelPartData p_front_wheel_t11 = p_front_wheel.addChild("front_wheel_t11", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.94f, 0f, 0f));
		ModelPartData p_front_wheel_t12 = p_front_wheel.addChild("front_wheel_t12", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 5.39f, 0f, 0f));
		ModelPartData p_front_wheel_t13 = p_front_wheel.addChild("front_wheel_t13", ModelPartBuilder.create()
				.uv(158, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(238, 30).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 5.83f, 0f, 0f));
		ModelPartData p_front_wheel_s0 = p_front_wheel.addChild("front_wheel_s0", ModelPartBuilder.create()
				.uv(170, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_front_wheel_s1 = p_front_wheel.addChild("front_wheel_s1", ModelPartBuilder.create()
				.uv(170, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0.79f, 0f, 0f));
		ModelPartData p_front_wheel_s2 = p_front_wheel.addChild("front_wheel_s2", ModelPartBuilder.create()
				.uv(170, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 1.57f, 0f, 0f));
		ModelPartData p_front_wheel_s3 = p_front_wheel.addChild("front_wheel_s3", ModelPartBuilder.create()
				.uv(170, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 2.36f, 0f, 0f));
		ModelPartData p_rear_wheel = p_bike.addChild("rear_wheel", ModelPartBuilder.create()
				.uv(132, 30).cuboid(-2f, -1.5f, -1.5f, 4f, 3f, 3f)
				.uv(174, 18).cuboid(-0.6f, -2.5f, -2.5f, 1f, 5f, 5f),
				ModelTransform.of(0f, 18f, 13f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_t0 = p_rear_wheel.addChild("rear_wheel_t0", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_t1 = p_rear_wheel.addChild("rear_wheel_t1", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0.45f, 0f, 0f));
		ModelPartData p_rear_wheel_t2 = p_rear_wheel.addChild("rear_wheel_t2", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 0.9f, 0f, 0f));
		ModelPartData p_rear_wheel_t3 = p_rear_wheel.addChild("rear_wheel_t3", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 1.35f, 0f, 0f));
		ModelPartData p_rear_wheel_t4 = p_rear_wheel.addChild("rear_wheel_t4", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 1.8f, 0f, 0f));
		ModelPartData p_rear_wheel_t5 = p_rear_wheel.addChild("rear_wheel_t5", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 2.24f, 0f, 0f));
		ModelPartData p_rear_wheel_t6 = p_rear_wheel.addChild("rear_wheel_t6", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 2.69f, 0f, 0f));
		ModelPartData p_rear_wheel_t7 = p_rear_wheel.addChild("rear_wheel_t7", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 3.14f, 0f, 0f));
		ModelPartData p_rear_wheel_t8 = p_rear_wheel.addChild("rear_wheel_t8", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 3.59f, 0f, 0f));
		ModelPartData p_rear_wheel_t9 = p_rear_wheel.addChild("rear_wheel_t9", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.04f, 0f, 0f));
		ModelPartData p_rear_wheel_t10 = p_rear_wheel.addChild("rear_wheel_t10", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.49f, 0f, 0f));
		ModelPartData p_rear_wheel_t11 = p_rear_wheel.addChild("rear_wheel_t11", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 4.94f, 0f, 0f));
		ModelPartData p_rear_wheel_t12 = p_rear_wheel.addChild("rear_wheel_t12", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 5.39f, 0f, 0f));
		ModelPartData p_rear_wheel_t13 = p_rear_wheel.addChild("rear_wheel_t13", ModelPartBuilder.create()
				.uv(172, 30).cuboid(-2f, -6f, -1.5f, 4f, 2f, 3f)
				.uv(0, 38).cuboid(-1f, -4.4f, -1.2f, 2f, 1f, 3f, new Dilation(0f, 0f, -0.1f)),
				ModelTransform.of(0f, 0f, 0f, 5.83f, 0f, 0f));
		ModelPartData p_rear_wheel_s0 = p_rear_wheel.addChild("rear_wheel_s0", ModelPartBuilder.create()
				.uv(186, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_rear_wheel_s1 = p_rear_wheel.addChild("rear_wheel_s1", ModelPartBuilder.create()
				.uv(186, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 0.79f, 0f, 0f));
		ModelPartData p_rear_wheel_s2 = p_rear_wheel.addChild("rear_wheel_s2", ModelPartBuilder.create()
				.uv(186, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 1.57f, 0f, 0f));
		ModelPartData p_rear_wheel_s3 = p_rear_wheel.addChild("rear_wheel_s3", ModelPartBuilder.create()
				.uv(186, 18).cuboid(-0.35f, -4.5f, -0.35f, 1f, 9f, 1f, new Dilation(-0.3f, 0f, -0.3f)),
				ModelTransform.of(0f, 0f, 0f, 2.36f, 0f, 0f));
		ModelPartData p_ffender0 = p_bike.addChild("ffender0", ModelPartBuilder.create()
				.uv(10, 38).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f, new Dilation(0f, 0f, 0.15f)),
				ModelTransform.of(0f, 18f, -16.5f, -0.7f, 0f, 0f));
		ModelPartData p_ffender1 = p_bike.addChild("ffender1", ModelPartBuilder.create()
				.uv(10, 38).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f, new Dilation(0f, 0f, 0.15f)),
				ModelTransform.of(0f, 18f, -16.5f, -0.3f, 0f, 0f));
		ModelPartData p_ffender2 = p_bike.addChild("ffender2", ModelPartBuilder.create()
				.uv(10, 38).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f, new Dilation(0f, 0f, 0.15f)),
				ModelTransform.of(0f, 18f, -16.5f, 0.1f, 0f, 0f));
		ModelPartData p_ffender3 = p_bike.addChild("ffender3", ModelPartBuilder.create()
				.uv(10, 38).cuboid(-2f, -7.5f, -1.6f, 4f, 1f, 3f, new Dilation(0f, 0f, 0.15f)),
				ModelTransform.of(0f, 18f, -16.5f, 0.5f, 0f, 0f));
		ModelPartData p_body = r.addChild("body", ModelPartBuilder.create()
				.uv(44, 0).cuboid(-4f, -12f, -2f, 8f, 12f, 4f)
				.uv(68, 0).cuboid(-4f, -12f, -2f, 8f, 12f, 4f, new Dilation(0.3f))
				.uv(42, 30).cuboid(-4.5f, -13f, -2.6f, 9f, 2f, 5f),
				ModelTransform.of(0f, 9f, 3.5f, 0.75f, 0f, 0f));
		ModelPartData p_head = p_body.addChild("head", ModelPartBuilder.create()
				.uv(92, 0).cuboid(-4f, -8f, -4f, 8f, 8f, 8f)
				.uv(126, 18).cuboid(-4f, -8.6f, -4f, 8f, 3f, 8f, new Dilation(0.3f, 0f, 0.3f)),
				ModelTransform.of(0f, -12.5f, 0f, -0.85f, 0f, 0f));
		ModelPartData p_goggles = p_head.addChild("goggles", ModelPartBuilder.create()
				.uv(36, 38).cuboid(-4.4f, -1f, -0.6f, 9f, 2f, 1f, new Dilation(0f, 0f, -0.2f)),
				ModelTransform.of(0f, -5.9f, -4.2f, 0f, 0f, 0f));
		ModelPartData p_right_arm = r.addChild("right_arm", ModelPartBuilder.create()
				.uv(200, 0).cuboid(-3f, -2f, -2f, 4f, 9f, 4f, new Dilation(0.25f, 0f, 0.25f))
				.uv(70, 30).cuboid(-3f, 7f, -2f, 4f, 3f, 4f),
				ModelTransform.of(-5f, 1.2f, -3.6f, -0.75f, 0.15f, 0f));
		ModelPartData p_left_arm = r.addChild("left_arm", ModelPartBuilder.create()
				.uv(216, 0).cuboid(-1f, -2f, -2f, 4f, 9f, 4f, new Dilation(0.25f, 0f, 0.25f))
				.uv(86, 30).cuboid(-1f, 7f, -2f, 4f, 3f, 4f),
				ModelTransform.of(5f, 1.2f, -3.6f, -0.75f, -0.15f, 0f));
		ModelPartData p_right_leg = r.addChild("right_leg", ModelPartBuilder.create()
				.uv(190, 18).cuboid(-2f, 0f, -2f, 4f, 6f, 4f),
				ModelTransform.of(-2.4f, 9f, 4f, -1f, 0.25f, 0f));
		ModelPartData p_right_shin = p_right_leg.addChild("right_shin", ModelPartBuilder.create()
				.uv(206, 18).cuboid(-2f, 0f, -2f, 4f, 6f, 4f),
				ModelTransform.of(0f, 5.5f, 0f, 1.9f, 0f, 0f));
		ModelPartData p_left_leg = r.addChild("left_leg", ModelPartBuilder.create()
				.uv(222, 18).cuboid(-2f, 0f, -2f, 4f, 6f, 4f),
				ModelTransform.of(2.4f, 9f, 4f, -1f, -0.25f, 0f));
		ModelPartData p_left_shin = p_left_leg.addChild("left_shin", ModelPartBuilder.create()
				.uv(238, 18).cuboid(-2f, 0f, -2f, 4f, 6f, 4f),
				ModelTransform.of(0f, 5.5f, 0f, 1.9f, 0f, 0f));
		return TexturedModelData.of(data, 256, 128);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(RevEntity e, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
		float charge = e.charge();
		// wheels roll with movement; when revving the back wheel spins in place
		float roll = limbAngle * 1.6f + (charge > 0 ? age * 1.4f : 0);
		frontWheel.pitch = -roll;
		rearWheel.pitch = -roll - charge * age * 0.6f;
		// engine rumble
		float rumble = MathHelper.sin(age * 2.3f) * (0.12f + charge * 0.15f);
		bike.pivotY = rumble;
		// tucked low over the bars, ducking lower for a ram
		body.pitch = 0.75f + charge * 0.2f;
		body.pivotY = 9 + rumble;
		head.pitch = -0.85f - charge * 0.2f + headPitch * MathHelper.RADIANS_PER_DEGREE * 0.4f;
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.6f;
		float swing = MathHelper.sin(e.handSwingProgress * MathHelper.PI);
		rightArm.pitch = -0.75f - swing * 1.2f;
		rightArm.yaw = 0.15f - swing * 0.4f;
		rightArm.pivotY = 1.2f + rumble;
		leftArm.pitch = -0.75f;
		leftArm.yaw = -0.15f;
		leftArm.pivotY = 1.2f + rumble;
		rightLeg.pivotY = 9 + rumble;
		leftLeg.pivotY = 9 + rumble;
	}
}
