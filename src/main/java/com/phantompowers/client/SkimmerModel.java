package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.SkimmerEntity;
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
 * Ecto-Skimmer (an original design): a white armoured hover-shuttle with a hinged bubble canopy, twin engine
 * pods, a spinning sensor dish, a chin-mounted twin beam cannon and bolt-on upgrade hardware.
 */
public class SkimmerModel extends SinglePartEntityModel<SkimmerEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("ecto_skimmer"), "main");
	private final ModelPart root, canopy, dish, cannon, podL, podR, upCannons, upBoosters, upShield, upScanner, upPhase;

	public SkimmerModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		ModelPart hull = root.getChild("hull");
		canopy = hull.getChild("canopy");
		dish = hull.getChild("mast").getChild("dish");
		cannon = hull.getChild("cannon");
		podL = hull.getChild("pod_l");
		podR = hull.getChild("pod_r");
		upCannons = hull.getChild("up_cannons");
		upBoosters = hull.getChild("up_boosters");
		upShield = hull.getChild("up_shield");
		upScanner = hull.getChild("up_scanner");
		upPhase = hull.getChild("up_phase");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_hull = r.addChild("hull", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-12f, -14f, -20f, 24f, 10f, 46f)
				.uv(0, 142).cuboid(-8f, -4f, -14f, 16f, 1f, 34f)
				.uv(124, 203).cuboid(-10f, -13f, -28f, 20f, 8f, 8f)
				.uv(184, 236).cuboid(-7f, -12f, -33f, 14f, 6f, 5f)
				.uv(64, 270).cuboid(-4f, -11f, -35f, 8f, 4f, 2f)
				.uv(100, 142).cuboid(-12f, -20f, -20f, 3f, 6f, 20f)
				.uv(146, 142).cuboid(9f, -20f, -20f, 3f, 6f, 20f)
				.uv(180, 261).cuboid(-9f, -18f, -22f, 18f, 4f, 4f)
				.uv(0, 104).cuboid(-12f, -26f, 0f, 24f, 12f, 26f)
				.uv(42, 177).cuboid(-6f, -28f, 4f, 12f, 2f, 20f)
				.uv(106, 177).cuboid(-10f, -24f, 26f, 20f, 16f, 4f)
				.uv(224, 251).cuboid(-8f, -21f, 30f, 6f, 6f, 3f)
				.uv(0, 261).cuboid(2f, -21f, 30f, 6f, 6f, 3f)
				.uv(84, 270).cuboid(-3f, -13f, 30f, 6f, 4f, 2f)
				.uv(140, 0).cuboid(-12.5f, -14.75f, -20f, 2f, 2f, 46f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 56).cuboid(10.5f, -14.75f, -20f, 2f, 2f, 46f, new Dilation(0f, -0.25f, 0f))
				.uv(188, 203).cuboid(-13f, -12f, -16f, 1f, 5f, 10f)
				.uv(210, 203).cuboid(12f, -12f, -16f, 1f, 5f, 10f)
				.uv(152, 270).cuboid(-9.5f, -12f, -28.6f, 3f, 2f, 1f)
				.uv(160, 270).cuboid(6.5f, -12f, -28.6f, 3f, 2f, 1f)
				.uv(96, 56).cuboid(-12.5f, -9f, -20f, 25f, 1f, 46f)
				.uv(184, 270).cuboid(-6f, -21f, -20f, 12f, 1f, 1f),
				ModelTransform.of(0f, 24f, 0f, 0f, 0f, 0f));
		ModelPartData p_pads = p_hull.addChild("pads", ModelPartBuilder.create()
				.uv(0, 251).cuboid(-12f, -4f, -17f, 6f, 4f, 6f)
				.uv(24, 251).cuboid(6f, -4f, -17f, 6f, 4f, 6f)
				.uv(48, 251).cuboid(-12f, -4f, 15f, 6f, 4f, 6f)
				.uv(72, 251).cuboid(6f, -4f, 15f, 6f, 4f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_seat = p_hull.addChild("seat", ModelPartBuilder.create()
				.uv(96, 251).cuboid(-5f, -18f, -6f, 10f, 4f, 6f)
				.uv(112, 236).cuboid(-5f, -26f, -1f, 10f, 10f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_canopy = p_hull.addChild("canopy", ModelPartBuilder.create()
				.uv(0, 221).cuboid(-9f, 0f, -14f, 18f, 1f, 14f)
				.uv(192, 142).cuboid(-9.5f, 0f, -20f, 1f, 6f, 20f)
				.uv(0, 177).cuboid(8.5f, 0f, -20f, 1f, 6f, 20f)
				.uv(64, 221).cuboid(-1f, -0.5f, -14f, 2f, 1f, 14f)
				.uv(96, 221).cuboid(-9.5f, -0.5f, -14f, 1f, 1f, 14f)
				.uv(126, 221).cuboid(8.5f, -0.5f, -14f, 1f, 1f, 14f),
				ModelTransform.of(0f, -26f, 0f, 0f, 0f, 0f));
		ModelPartData p_windshield = p_canopy.addChild("windshield", ModelPartBuilder.create()
				.uv(156, 221).cuboid(-9f, 0f, -14f, 18f, 1f, 14f)
				.uv(220, 221).cuboid(-1f, -0.5f, -14f, 2f, 1f, 14f),
				ModelTransform.of(0f, 0f, -14f, 0.46f, 0f, 0f));
		ModelPartData p_pod_l = p_hull.addChild("pod_l", ModelPartBuilder.create()
				.uv(100, 104).cuboid(-4f, -4f, -14f, 8f, 8f, 28f)
				.uv(128, 251).cuboid(-4.5f, -4.5f, -15f, 9f, 9f, 1f)
				.uv(18, 261).cuboid(-3f, -3f, 14f, 6f, 6f, 3f)
				.uv(154, 177).cuboid(-2f, -6f, -10f, 4f, 2f, 18f),
				ModelTransform.of(-17f, -14f, 4f, 0f, 0f, 0f));
		ModelPartData p_pod_r = p_hull.addChild("pod_r", ModelPartBuilder.create()
				.uv(100, 104).cuboid(-4f, -4f, -14f, 8f, 8f, 28f)
				.uv(128, 251).cuboid(-4.5f, -4.5f, -15f, 9f, 9f, 1f)
				.uv(18, 261).cuboid(-3f, -3f, 14f, 6f, 6f, 3f)
				.uv(154, 177).cuboid(-2f, -6f, -10f, 4f, 2f, 18f),
				ModelTransform.of(17f, -14f, 4f, 0f, 0f, 0f));
		ModelPartData p_pylons = p_hull.addChild("pylons", ModelPartBuilder.create()
				.uv(206, 177).cuboid(-13f, -16f, -4f, 1f, 4f, 14f)
				.uv(0, 203).cuboid(12f, -16f, -4f, 1f, 4f, 14f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail = p_hull.addChild("tail", ModelPartBuilder.create()
				.uv(30, 203).cuboid(-1f, -11f, -5f, 2f, 11f, 7f)
				.uv(224, 261).cuboid(-1.5f, -12f, -5f, 3f, 1f, 6f),
				ModelTransform.of(0f, -28f, 24f, -0.55f, 0f, 0f));
		ModelPartData p_hood = p_hull.addChild("hood", ModelPartBuilder.create()
				.uv(56, 236).cuboid(-8f, -1f, 0f, 16f, 1f, 12f)
				.uv(222, 236).cuboid(-1f, -1.5f, 1f, 2f, 1f, 10f),
				ModelTransform.of(0f, -13f, -33f, 0.43f, 0f, 0f));
		ModelPartData p_rear_deck = p_hull.addChild("rear_deck", ModelPartBuilder.create()
				.uv(0, 270).cuboid(-11f, 0f, -1f, 22f, 1f, 6f),
				ModelTransform.of(0f, -26f, 26f, -0.5f, 0f, 0f));
		ModelPartData p_fins = p_hull.addChild("fins", ModelPartBuilder.create()
				.uv(36, 261).cuboid(-18f, -1f, -4f, 6f, 1f, 8f)
				.uv(64, 261).cuboid(12f, -1f, -4f, 6f, 1f, 8f),
				ModelTransform.of(0f, -22f, 24f, 0f, 0f, 0f));
		ModelPartData p_mast = p_hull.addChild("mast", ModelPartBuilder.create()
				.uv(56, 270).cuboid(-1f, -5f, -1f, 2f, 5f, 2f),
				ModelTransform.of(0f, -28f, 12f, 0f, 0f, 0f));
		ModelPartData p_dish = p_mast.addChild("dish", ModelPartBuilder.create()
				.uv(92, 261).cuboid(-4f, -1f, -4f, 8f, 1f, 8f)
				.uv(144, 270).cuboid(-1f, -3f, -1f, 2f, 2f, 2f),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon = p_hull.addChild("cannon", ModelPartBuilder.create()
				.uv(124, 261).cuboid(-3f, 0f, -3f, 6f, 3f, 6f)
				.uv(136, 236).cuboid(-2.5f, 1f, -12f, 2f, 2f, 10f)
				.uv(160, 236).cuboid(0.5f, 1f, -12f, 2f, 2f, 10f),
				ModelTransform.of(0f, -4f, -26f, 0f, 0f, 0f));
		ModelPartData p_up_cannons = p_hull.addChild("up_cannons", ModelPartBuilder.create()
				.uv(0, 236).cuboid(-23f, -16f, -12f, 2f, 3f, 12f)
				.uv(148, 251).cuboid(-23.5f, -15.5f, -20f, 3f, 2f, 8f)
				.uv(28, 236).cuboid(21f, -16f, -12f, 2f, 3f, 12f)
				.uv(170, 251).cuboid(20.5f, -15.5f, -20f, 3f, 2f, 8f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_boosters = p_hull.addChild("up_boosters", ModelPartBuilder.create()
				.uv(48, 203).cuboid(-19.5f, -23f, 6f, 5f, 4f, 14f)
				.uv(100, 270).cuboid(-19f, -22.5f, 20f, 4f, 3f, 2f)
				.uv(86, 203).cuboid(14.5f, -23f, 6f, 5f, 4f, 14f)
				.uv(112, 270).cuboid(15f, -22.5f, 20f, 4f, 3f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_shield = p_hull.addChild("up_shield", ModelPartBuilder.create()
				.uv(192, 251).cuboid(-4f, -30f, 16f, 8f, 2f, 8f)
				.uv(148, 261).cuboid(-3f, -33f, 17f, 6f, 3f, 6f)
				.uv(124, 270).cuboid(-1.5f, -35f, 18.5f, 3f, 2f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_scanner = p_hull.addChild("up_scanner", ModelPartBuilder.create()
				.uv(172, 261).cuboid(-11f, -34f, 20f, 1f, 8f, 1f)
				.uv(176, 261).cuboid(10f, -34f, 20f, 1f, 8f, 1f)
				.uv(168, 270).cuboid(-11.5f, -35f, 19.5f, 2f, 1f, 2f)
				.uv(176, 270).cuboid(9.5f, -35f, 19.5f, 2f, 1f, 2f)
				.uv(136, 270).cuboid(-7f, -27f, -26f, 1f, 4f, 1f)
				.uv(140, 270).cuboid(6f, -27f, -26f, 1f, 4f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_phase = p_hull.addChild("up_phase", ModelPartBuilder.create()
				.uv(210, 270).cuboid(-11f, -25f, 29f, 22f, 1f, 1f)
				.uv(0, 277).cuboid(-11f, -9f, 29f, 22f, 1f, 1f)
				.uv(180, 203).cuboid(-11f, -24f, 29f, 1f, 15f, 1f)
				.uv(184, 203).cuboid(10f, -24f, 29f, 1f, 15f, 1f)
				.uv(46, 277).cuboid(-13f, -27f, 31f, 26f, 1f, 1f)
				.uv(100, 277).cuboid(-13f, -7f, 31f, 26f, 1f, 1f)
				.uv(198, 177).cuboid(-13f, -26f, 31f, 1f, 19f, 1f)
				.uv(202, 177).cuboid(12f, -26f, 31f, 1f, 19f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 320);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(SkimmerEntity e, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
	}

	/** Pose the moving parts for this frame. */
	public void pose(SkimmerEntity e, float partial, float pilotPitch) {
		canopy.pitch = -1.05f * e.canopyOpen(partial);
		dish.yaw = e.dishSpin(partial);
		float power = e.enginePower(partial);
		float thrust = e.thrust(partial);
		// pods tilt nose-up while hovering, level out as you pick up speed
		float tilt = -0.32f * power * (1f - MathHelper.clamp(thrust, 0f, 1f)) + 0.08f * (1f - power);
		podL.pitch = tilt;
		podR.pitch = tilt;
		cannon.pitch = MathHelper.clamp(-pilotPitch, -0.6f, 0.4f) * power;
		upCannons.visible = e.has(SkimmerEntity.UP_CANNONS);
		upBoosters.visible = e.has(SkimmerEntity.UP_THRUSTERS);
		upShield.visible = e.has(SkimmerEntity.UP_SHIELD);
		upScanner.visible = e.has(SkimmerEntity.UP_SCANNER);
		upPhase.visible = e.has(SkimmerEntity.UP_PHASE);
	}
}
