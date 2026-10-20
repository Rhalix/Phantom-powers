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
 * Ecto-Skimmer (an original design): a white armoured hover-shuttle with a framed bubble canopy over a real
 * cockpit (seat, armrests, control sticks, glowing consoles), banded twin engine pods with fan intakes and cooling
 * fins, landing struts, side door, vents and piping, twin canted tail fins, three rear thruster bells, a spinning
 * sensor dish, a chin-mounted twin beam cannon and bolt-on upgrade hardware.
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
				.uv(0, 185).cuboid(-8f, -4f, -14f, 16f, 1f, 34f)
				.uv(58, 266).cuboid(-10f, -13f, -28f, 20f, 8f, 8f)
				.uv(48, 312).cuboid(-7f, -12f, -33f, 14f, 6f, 5f)
				.uv(96, 352).cuboid(-4f, -11f, -35f, 8f, 4f, 2f)
				.uv(184, 359).cuboid(-1.5f, -10f, -35.6f, 3f, 2f, 1f)
				.uv(140, 359).cuboid(-5f, -10f, -28.6f, 10f, 3f, 1f)
				.uv(192, 359).cuboid(-9.5f, -12f, -28.6f, 3f, 2f, 1f)
				.uv(200, 359).cuboid(6.5f, -12f, -28.6f, 3f, 2f, 1f)
				.uv(100, 324).cuboid(-10f, -6f, -27f, 4f, 2f, 7f)
				.uv(122, 324).cuboid(6f, -6f, -27f, 4f, 2f, 7f)
				.uv(0, 220).cuboid(-12f, -20f, -20f, 3f, 6f, 20f)
				.uv(46, 220).cuboid(9f, -20f, -20f, 3f, 6f, 20f)
				.uv(4, 343).cuboid(-9f, -22f, -22f, 18f, 4f, 4f)
				.uv(0, 147).cuboid(-12f, -26f, 0f, 24f, 12f, 26f)
				.uv(152, 220).cuboid(-6f, -28f, 4f, 12f, 2f, 20f)
				.uv(144, 324).cuboid(-5f, -30f, 16f, 1f, 2f, 7f)
				.uv(160, 324).cuboid(-3f, -30f, 16f, 1f, 2f, 7f)
				.uv(176, 324).cuboid(2f, -30f, 16f, 1f, 2f, 7f)
				.uv(192, 324).cuboid(4f, -30f, 16f, 1f, 2f, 7f)
				.uv(208, 324).cuboid(-11f, -26.75f, 2f, 6f, 1f, 8f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 334).cuboid(5f, -26.75f, 2f, 6f, 1f, 8f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 246).cuboid(-10f, -24f, 26f, 20f, 16f, 4f)
				.uv(28, 334).cuboid(-8.5f, -21.5f, 30f, 7f, 7f, 2f)
				.uv(46, 334).cuboid(1.5f, -21.5f, 30f, 7f, 7f, 2f)
				.uv(236, 352).cuboid(-7f, -20f, 31.5f, 4f, 4f, 1f)
				.uv(246, 352).cuboid(3f, -20f, 31.5f, 4f, 4f, 1f)
				.uv(116, 352).cuboid(-3f, -13f, 30f, 6f, 4f, 2f)
				.uv(208, 359).cuboid(-2f, -12f, 31.5f, 4f, 2f, 1f)
				.uv(140, 0).cuboid(-12.5f, -14.75f, -20f, 2f, 2f, 46f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 56).cuboid(10.5f, -14.75f, -20f, 2f, 2f, 46f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 104).cuboid(-12.5f, -5f, -18f, 1f, 1f, 42f)
				.uv(86, 104).cuboid(11.5f, -5f, -18f, 1f, 1f, 42f)
				.uv(174, 266).cuboid(-13f, -12f, -16f, 1f, 5f, 10f)
				.uv(196, 266).cuboid(12f, -12f, -16f, 1f, 5f, 10f)
				.uv(126, 343).cuboid(-12.6f, -12f, 12f, 1f, 6f, 1f)
				.uv(130, 343).cuboid(-12.6f, -12f, 14f, 1f, 6f, 1f)
				.uv(134, 343).cuboid(-12.6f, -12f, 16f, 1f, 6f, 1f)
				.uv(138, 343).cuboid(11.6f, -12f, 12f, 1f, 6f, 1f)
				.uv(142, 343).cuboid(11.6f, -12f, 14f, 1f, 6f, 1f)
				.uv(146, 343).cuboid(11.6f, -12f, 16f, 1f, 6f, 1f)
				.uv(100, 185).cuboid(-13f, -7f, -4f, 1f, 1f, 28f)
				.uv(158, 185).cuboid(12f, -7f, -4f, 1f, 1f, 28f)
				.uv(96, 56).cuboid(-12.5f, -9f, -20f, 25f, 1f, 46f)
				.uv(56, 246).cuboid(-12.5f, -24f, 8f, 1f, 6f, 12f)
				.uv(82, 246).cuboid(11.5f, -24f, 8f, 1f, 6f, 12f)
				.uv(0, 359).cuboid(-12f, -30f, 22f, 1f, 4f, 1f)
				.uv(4, 359).cuboid(-12.5f, -33f, 21.5f, 2f, 3f, 2f),
				ModelTransform.of(0f, 24f, 0f, 0f, 0f, 0f));
		ModelPartData p_pads = p_hull.addChild("pads", ModelPartBuilder.create()
				.uv(198, 312).cuboid(-12f, -4f, -17f, 6f, 4f, 6f)
				.uv(222, 312).cuboid(6f, -4f, -17f, 6f, 4f, 6f)
				.uv(0, 324).cuboid(-12f, -4f, 15f, 6f, 4f, 6f)
				.uv(24, 324).cuboid(6f, -4f, 15f, 6f, 4f, 6f)
				.uv(132, 352).cuboid(-14f, -6f, -15.5f, 2f, 3f, 3f)
				.uv(142, 352).cuboid(12f, -6f, -15.5f, 2f, 3f, 3f)
				.uv(152, 352).cuboid(-14f, -6f, 16.5f, 2f, 3f, 3f)
				.uv(162, 352).cuboid(12f, -6f, 16.5f, 2f, 3f, 3f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_seat = p_hull.addChild("seat", ModelPartBuilder.create()
				.uv(48, 343).cuboid(-5f, -15f, -7f, 10f, 1f, 7f)
				.uv(212, 298).cuboid(-5f, -25f, -1f, 10f, 10f, 2f)
				.uv(172, 352).cuboid(-3f, -29f, -1f, 6f, 4f, 2f)
				.uv(64, 334).cuboid(-7f, -17f, -6f, 2f, 3f, 6f)
				.uv(80, 334).cuboid(5f, -17f, -6f, 2f, 3f, 6f)
				.uv(12, 359).cuboid(-4f, -18f, -13f, 1f, 4f, 1f)
				.uv(16, 359).cuboid(3f, -18f, -13f, 1f, 4f, 1f)
				.uv(218, 359).cuboid(-4.5f, -19f, -13.5f, 2f, 1f, 2f)
				.uv(226, 359).cuboid(2.5f, -19f, -13.5f, 2f, 1f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_canopy = p_hull.addChild("canopy", ModelPartBuilder.create()
				.uv(0, 283).cuboid(-9f, 0f, -14f, 18f, 1f, 14f)
				.uv(92, 220).cuboid(-9.5f, 0f, -14f, 1f, 12f, 14f)
				.uv(122, 220).cuboid(8.5f, 0f, -14f, 1f, 12f, 14f)
				.uv(60, 298).cuboid(-9.5f, 5f, -20f, 1f, 7f, 6f)
				.uv(74, 298).cuboid(8.5f, 5f, -20f, 1f, 7f, 6f)
				.uv(150, 343).cuboid(-9f, 0f, -1f, 18f, 6f, 1f)
				.uv(64, 283).cuboid(-1f, -0.5f, -14f, 2f, 1f, 14f)
				.uv(96, 283).cuboid(-10.25f, -0.5f, -14f, 2f, 1f, 14f, new Dilation(-0.25f, 0f, 0f))
				.uv(128, 283).cuboid(8.25f, -0.5f, -14f, 2f, 1f, 14f, new Dilation(-0.25f, 0f, 0f))
				.uv(88, 298).cuboid(-10.25f, 0f, -1f, 2f, 12f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(94, 298).cuboid(8.25f, 0f, -1f, 2f, 12f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(100, 298).cuboid(-10.25f, 0f, -8f, 2f, 12f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(106, 298).cuboid(8.25f, 0f, -8f, 2f, 12f, 1f, new Dilation(-0.25f, 0f, 0f))
				.uv(20, 359).cuboid(-4f, -1f, -6f, 8f, 1f, 4f)
				.uv(234, 359).cuboid(-1f, -1.5f, -5f, 2f, 1f, 2f),
				ModelTransform.of(0f, -32f, 0f, 0f, 0f, 0f));
		ModelPartData p_windshield = p_canopy.addChild("windshield", ModelPartBuilder.create()
				.uv(160, 283).cuboid(-9f, 0f, -13f, 18f, 1f, 13f)
				.uv(222, 283).cuboid(-1f, -0.5f, -13f, 2f, 1f, 13f)
				.uv(0, 298).cuboid(-10.25f, -0.5f, -13f, 2f, 1f, 13f, new Dilation(-0.25f, 0f, 0f))
				.uv(30, 298).cuboid(8.25f, -0.5f, -13f, 2f, 1f, 13f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(0f, 0f, -14f, 0.9f, 0f, 0f));
		ModelPartData p_pod_l = p_hull.addChild("pod_l", ModelPartBuilder.create()
				.uv(100, 147).cuboid(-4f, -4f, -14f, 8f, 8f, 28f)
				.uv(48, 324).cuboid(-4.5f, -4.5f, -15f, 9f, 9f, 1f)
				.uv(86, 312).cuboid(-4.5f, -4.5f, -9f, 9f, 9f, 2f)
				.uv(108, 312).cuboid(-4.5f, -4.5f, 8f, 9f, 9f, 2f)
				.uv(96, 334).cuboid(-3.5f, -3.5f, 14f, 7f, 7f, 2f)
				.uv(188, 352).cuboid(-2.5f, -2.5f, 15.5f, 5f, 5f, 1f)
				.uv(112, 298).cuboid(-0.5f, -7f, 2f, 1f, 3f, 10f)
				.uv(134, 298).cuboid(-0.5f, 4f, 2f, 1f, 3f, 10f)
				.uv(0, 266).cuboid(-2f, -5f, -10f, 4f, 1f, 16f)
				.uv(162, 359).cuboid(-5f, -1f, -12f, 1f, 2f, 2f),
				ModelTransform.of(-17f, -14f, 4f, 0f, 0f, 0f));
		ModelPartData p_pod_r = p_hull.addChild("pod_r", ModelPartBuilder.create()
				.uv(100, 147).cuboid(-4f, -4f, -14f, 8f, 8f, 28f)
				.uv(48, 324).cuboid(-4.5f, -4.5f, -15f, 9f, 9f, 1f)
				.uv(86, 312).cuboid(-4.5f, -4.5f, -9f, 9f, 9f, 2f)
				.uv(108, 312).cuboid(-4.5f, -4.5f, 8f, 9f, 9f, 2f)
				.uv(96, 334).cuboid(-3.5f, -3.5f, 14f, 7f, 7f, 2f)
				.uv(188, 352).cuboid(-2.5f, -2.5f, 15.5f, 5f, 5f, 1f)
				.uv(112, 298).cuboid(-0.5f, -7f, 2f, 1f, 3f, 10f)
				.uv(134, 298).cuboid(-0.5f, 4f, 2f, 1f, 3f, 10f)
				.uv(0, 266).cuboid(-2f, -5f, -10f, 4f, 1f, 16f)
				.uv(162, 359).cuboid(4f, -1f, -12f, 1f, 2f, 2f),
				ModelTransform.of(17f, -14f, 4f, 0f, 0f, 0f));
		ModelPartData p_pylons = p_hull.addChild("pylons", ModelPartBuilder.create()
				.uv(108, 246).cuboid(-13f, -16f, -4f, 1f, 4f, 14f)
				.uv(138, 246).cuboid(12f, -16f, -4f, 1f, 4f, 14f)
				.uv(188, 343).cuboid(-13f, -12f, 0f, 1f, 1f, 6f)
				.uv(202, 343).cuboid(12f, -12f, 0f, 1f, 1f, 6f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_tail_l = p_hull.addChild("tail_l", ModelPartBuilder.create()
				.uv(40, 266).cuboid(-0.5f, -9f, -5f, 1f, 9f, 8f)
				.uv(216, 343).cuboid(-1f, -10f, -5f, 2f, 1f, 6f),
				ModelTransform.of(-7f, -26f, 22f, -0.45f, 0f, -0.38f));
		ModelPartData p_tail_r = p_hull.addChild("tail_r", ModelPartBuilder.create()
				.uv(40, 266).cuboid(-0.5f, -9f, -5f, 1f, 9f, 8f)
				.uv(216, 343).cuboid(-1f, -10f, -5f, 2f, 1f, 6f),
				ModelTransform.of(7f, -26f, 22f, -0.45f, 0f, 0.38f));
		ModelPartData p_fins = p_hull.addChild("fins", ModelPartBuilder.create()
				.uv(114, 334).cuboid(-18f, -1f, -4f, 6f, 1f, 8f)
				.uv(142, 334).cuboid(12f, -1f, -4f, 6f, 1f, 8f)
				.uv(82, 343).cuboid(-18.5f, -1.5f, -3f, 1f, 2f, 6f)
				.uv(96, 343).cuboid(17.5f, -1.5f, -3f, 1f, 2f, 6f),
				ModelTransform.of(0f, -22f, 24f, 0f, 0f, 0f));
		ModelPartData p_hood = p_hull.addChild("hood", ModelPartBuilder.create()
				.uv(156, 298).cuboid(-8f, -1f, 0f, 16f, 1f, 12f)
				.uv(130, 312).cuboid(-1f, -1.5f, 1f, 2f, 1f, 10f)
				.uv(44, 359).cuboid(-6f, -1.6f, 6f, 3f, 1f, 4f)
				.uv(58, 359).cuboid(3f, -1.6f, 6f, 3f, 1f, 4f),
				ModelTransform.of(0f, -13f, -33f, 0.43f, 0f, 0f));
		ModelPartData p_rear_deck = p_hull.addChild("rear_deck", ModelPartBuilder.create()
				.uv(0, 352).cuboid(-11f, 0f, -1f, 22f, 1f, 6f),
				ModelTransform.of(0f, -26f, 26f, -0.5f, 0f, 0f));
		ModelPartData p_mast = p_hull.addChild("mast", ModelPartBuilder.create()
				.uv(56, 352).cuboid(-1f, -5f, -1f, 2f, 5f, 2f),
				ModelTransform.of(0f, -28f, 10f, 0f, 0f, 0f));
		ModelPartData p_dish = p_mast.addChild("dish", ModelPartBuilder.create()
				.uv(170, 334).cuboid(-4f, -1f, -4f, 8f, 1f, 8f)
				.uv(168, 359).cuboid(-1f, -3f, -1f, 2f, 2f, 2f)
				.uv(242, 359).cuboid(-0.5f, -5f, -0.5f, 1f, 2f, 1f),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon = p_hull.addChild("cannon", ModelPartBuilder.create()
				.uv(202, 334).cuboid(-3f, 0f, -3f, 6f, 3f, 6f)
				.uv(0, 312).cuboid(-2.5f, 1f, -12f, 2f, 2f, 10f)
				.uv(24, 312).cuboid(0.5f, 1f, -12f, 2f, 2f, 10f)
				.uv(72, 359).cuboid(-3f, 0.5f, -8f, 6f, 3f, 2f),
				ModelTransform.of(0f, -4f, -26f, 0f, 0f, 0f));
		ModelPartData p_up_cannons = p_hull.addChild("up_cannons", ModelPartBuilder.create()
				.uv(114, 266).cuboid(-24f, -17f, -12f, 3f, 4f, 12f)
				.uv(154, 312).cuboid(-23.5f, -16f, -21f, 2f, 2f, 9f)
				.uv(88, 359).cuboid(-24f, -16.5f, -14f, 3f, 3f, 2f)
				.uv(144, 266).cuboid(21f, -17f, -12f, 3f, 4f, 12f)
				.uv(176, 312).cuboid(21.5f, -16f, -21f, 2f, 2f, 9f)
				.uv(98, 359).cuboid(21f, -16.5f, -14f, 3f, 3f, 2f)
				.uv(200, 352).cuboid(-22f, -16f, -6f, 1f, 2f, 4f)
				.uv(210, 352).cuboid(21f, -16f, -6f, 1f, 2f, 4f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_boosters = p_hull.addChild("up_boosters", ModelPartBuilder.create()
				.uv(168, 246).cuboid(-19.5f, -23f, 6f, 5f, 4f, 14f)
				.uv(108, 359).cuboid(-19f, -22.5f, 20f, 4f, 3f, 2f)
				.uv(246, 359).cuboid(-18.5f, -22f, 21.5f, 3f, 2f, 1f)
				.uv(64, 352).cuboid(-20f, -23.5f, 9f, 6f, 5f, 2f)
				.uv(206, 246).cuboid(14.5f, -23f, 6f, 5f, 4f, 14f)
				.uv(120, 359).cuboid(15f, -22.5f, 20f, 4f, 3f, 2f)
				.uv(0, 364).cuboid(15.5f, -22f, 21.5f, 3f, 2f, 1f)
				.uv(80, 352).cuboid(14f, -23.5f, 9f, 6f, 5f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_shield = p_hull.addChild("up_shield", ModelPartBuilder.create()
				.uv(68, 324).cuboid(-4f, -30f, 16f, 8f, 2f, 8f)
				.uv(226, 334).cuboid(-3f, -33f, 17f, 6f, 3f, 6f)
				.uv(220, 352).cuboid(-2f, -35f, 18f, 4f, 2f, 4f)
				.uv(176, 359).cuboid(-1f, -36.75f, 19f, 2f, 2f, 2f, new Dilation(0f, -0.25f, 0f)),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_scanner = p_hull.addChild("up_scanner", ModelPartBuilder.create()
				.uv(250, 334).cuboid(-11f, -34f, 20f, 1f, 8f, 1f)
				.uv(0, 343).cuboid(10f, -34f, 20f, 1f, 8f, 1f)
				.uv(8, 364).cuboid(-11.5f, -35f, 19.5f, 2f, 1f, 2f)
				.uv(16, 364).cuboid(9.5f, -35f, 19.5f, 2f, 1f, 2f)
				.uv(132, 359).cuboid(-7f, -17f, -27f, 1f, 4f, 1f)
				.uv(136, 359).cuboid(6f, -17f, -27f, 1f, 4f, 1f)
				.uv(24, 364).cuboid(-7.5f, -18f, -27.5f, 2f, 1f, 2f)
				.uv(32, 364).cuboid(5.5f, -18f, -27.5f, 2f, 1f, 2f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		ModelPartData p_up_phase = p_hull.addChild("up_phase", ModelPartBuilder.create()
				.uv(40, 364).cuboid(-9.5f, -22.5f, 32.5f, 9f, 1f, 1f)
				.uv(60, 364).cuboid(-9.5f, -14.5f, 32.5f, 9f, 1f, 1f)
				.uv(110, 343).cuboid(-9.5f, -21.5f, 32.5f, 1f, 7f, 1f)
				.uv(114, 343).cuboid(-1.5f, -21.5f, 32.5f, 1f, 7f, 1f)
				.uv(80, 364).cuboid(0.5f, -22.5f, 32.5f, 9f, 1f, 1f)
				.uv(100, 364).cuboid(0.5f, -14.5f, 32.5f, 9f, 1f, 1f)
				.uv(118, 343).cuboid(0.5f, -21.5f, 32.5f, 1f, 7f, 1f)
				.uv(122, 343).cuboid(8.5f, -21.5f, 32.5f, 1f, 7f, 1f)
				.uv(48, 246).cuboid(-12f, -25f, 27f, 1f, 18f, 1f)
				.uv(52, 246).cuboid(11f, -25f, 27f, 1f, 18f, 1f),
				ModelTransform.of(0f, 0f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 256, 512);
	}

	/** One rounded engine pod: octagonal body, fan intake, trim bands, nozzle, dorsal fin and nav lights. */
	private static ModelPartBuilder pod() {
		return ModelPartBuilder.create()
				.uv(284, 56).cuboid(-4f, -3f, -14f, 8f, 6f, 28f)
				.uv(216, 56).cuboid(-3f, -4f, -14f, 6f, 8f, 28f)
				.uv(220, 163).cuboid(-4.5f, -3.5f, -15f, 9f, 7f, 1f)
				.uv(196, 152).cuboid(-3.5f, -4.5f, -15f, 7f, 9f, 1f)
				.uv(410, 152).cuboid(-4.5f, -3.5f, -6.4f, 9f, 7f, 2f, new Dilation(-0.2f, -0.2f, -0.4f))
				.uv(18, 152).cuboid(-3.5f, -4.5f, -6.4f, 7f, 9f, 2f, new Dilation(-0.2f, -0.2f, -0.4f))
				.uv(432, 152).cuboid(-4.5f, -3.5f, 5.6f, 9f, 7f, 2f, new Dilation(-0.2f, -0.2f, -0.4f))
				.uv(36, 152).cuboid(-3.5f, -4.5f, 5.6f, 7f, 9f, 2f, new Dilation(-0.2f, -0.2f, -0.4f))
				.uv(454, 152).cuboid(-3.5f, -3.5f, 13.75f, 7f, 7f, 2f, new Dilation(0f, 0f, -0.25f))
				.uv(240, 163).cuboid(-3f, -3f, 15.25f, 6f, 6f, 2f, new Dilation(0f, 0f, -0.25f))
				.uv(248, 137).cuboid(-0.5f, -7f, 3f, 1f, 3f, 10f)
				.uv(320, 171).cuboid(-4.8f, -1f, -13f, 1f, 2f, 2f, new Dilation(-0.2f, 0f, 0f))
				.uv(326, 171).cuboid(3.8f, -1f, -13f, 1f, 2f, 2f, new Dilation(-0.2f, 0f, 0f));
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
