package com.phantompowers.client;

import com.phantompowers.GhostFlags;
import com.phantompowers.PhantomMod;
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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Backpack cannons: twin shoulder-mounted blasters that unfold from the suit pack and aim where you look. */
public class CannonFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(PhantomMod.id("backpack_cannons"), "main");
	private static final Identifier TEXTURE = PhantomMod.id("textures/entity/backpack_cannons.png");
	private static final Identifier GLOW = PhantomMod.id("textures/entity/backpack_cannons_glow.png");
	/** Unfold progress per player (0 stowed .. 1 deployed). */
	private static final Map<UUID, Float> DEPLOY = new ConcurrentHashMap<>();
	private final ModelPart root, mountL, mountR, jointL, jointR, cannonL, cannonR;

	public CannonFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx, ModelPart root) {
		super(ctx);
		this.root = root;
		mountL = root.getChild("mount_left");
		mountR = root.getChild("mount_right");
		jointL = mountL.getChild("joint_left");
		jointR = mountR.getChild("joint_right");
		cannonL = jointL.getChild("cannon_left");
		cannonR = jointR.getChild("cannon_right");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData r = data.getRoot();
		ModelPartData p_mount_left = r.addChild("mount_left", ModelPartBuilder.create()
				.uv(24, 26).cuboid(-1.5f, -1f, -1.5f, 3f, 2f, 3f)
				.uv(16, 19).cuboid(-1f, -5f, -1f, 2f, 4f, 2f),
				ModelTransform.of(4f, 1f, 4f, 0f, 0f, 0f));
		ModelPartData p_joint_left = p_mount_left.addChild("joint_left", ModelPartBuilder.create()
				.uv(24, 19).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f)
				.uv(32, 32).cuboid(0.25f, -1f, -1f, 2f, 2f, 2f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon_left = p_joint_left.addChild("cannon_left", ModelPartBuilder.create()
				.uv(0, 0).cuboid(0f, -2f, -2f, 4f, 4f, 7f)
				.uv(44, 0).cuboid(0.5f, -1.5f, -7f, 3f, 3f, 5f)
				.uv(36, 26).cuboid(0f, -2f, -5f, 4f, 4f, 1f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(46, 26).cuboid(0f, -2f, -3f, 4f, 4f, 1f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(16, 11).cuboid(-0.5f, -2.5f, -9f, 5f, 5f, 2f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(30, 11).cuboid(0.5f, -3.75f, -1f, 3f, 2f, 5f, new Dilation(0f, -0.25f, 0f))
				.uv(40, 32).cuboid(1.5f, -4.5f, 0f, 1f, 1f, 3f)
				.uv(48, 32).cuboid(1f, -5.75f, -1f, 2f, 2f, 2f, new Dilation(0f, -0.25f, 0f))
				.uv(36, 19).cuboid(3.75f, -1f, 0f, 2f, 2f, 4f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(1.5f, -1f, 0f, 0f, 0f, 0f));
		ModelPartData p_mount_right = r.addChild("mount_right", ModelPartBuilder.create()
				.uv(0, 32).cuboid(-1.5f, -1f, -1.5f, 3f, 2f, 3f)
				.uv(48, 19).cuboid(-1f, -5f, -1f, 2f, 4f, 2f),
				ModelTransform.of(-4f, 1f, 4f, 0f, 0f, 0f));
		ModelPartData p_joint_right = p_mount_right.addChild("joint_right", ModelPartBuilder.create()
				.uv(0, 26).cuboid(-1.5f, -1.5f, -1.5f, 3f, 3f, 3f)
				.uv(56, 32).cuboid(-2.25f, -1f, -1f, 2f, 2f, 2f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(0f, -5f, 0f, 0f, 0f, 0f));
		ModelPartData p_cannon_right = p_joint_right.addChild("cannon_right", ModelPartBuilder.create()
				.uv(22, 0).cuboid(-4f, -2f, -2f, 4f, 4f, 7f)
				.uv(0, 11).cuboid(-3.5f, -1.5f, -7f, 3f, 3f, 5f)
				.uv(12, 32).cuboid(-4f, -2f, -5f, 4f, 4f, 1f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(22, 32).cuboid(-4f, -2f, -3f, 4f, 4f, 1f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(46, 11).cuboid(-4.5f, -2.5f, -9f, 5f, 5f, 2f, new Dilation(-0.25f, -0.25f, 0f))
				.uv(0, 19).cuboid(-3.5f, -3.75f, -1f, 3f, 2f, 5f, new Dilation(0f, -0.25f, 0f))
				.uv(0, 37).cuboid(-2.5f, -4.5f, 0f, 1f, 1f, 3f)
				.uv(8, 37).cuboid(-3f, -5.75f, -1f, 2f, 2f, 2f, new Dilation(0f, -0.25f, 0f))
				.uv(12, 26).cuboid(-5.75f, -1f, 0f, 2f, 2f, 4f, new Dilation(-0.25f, 0f, 0f)),
				ModelTransform.of(-1.5f, -1f, 0f, 0f, 0f, 0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, AbstractClientPlayerEntity p,
			float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
		boolean on = !GhostFlags.CLIENT_CANNONS.isEmpty() && GhostFlags.CLIENT_CANNONS.contains(p.getUuid());
		float d = DEPLOY.getOrDefault(p.getUuid(), 0f);
		d = MathHelper.clamp(d + (on ? 0.02f : -0.03f) * Math.max(1f, 60f / Math.max(1f, net.minecraft.client.MinecraftClient.getInstance().getCurrentFps())), 0f, 1f);
		if (d <= 0f) {
			DEPLOY.remove(p.getUuid());
			return;
		}
		DEPLOY.put(p.getUuid(), d);
		if (p.isInvisible()) return;
		float e = d * d * (3 - 2 * d);
		// mounts swing up out of the pack, then the barrels track your aim
		mountL.pitch = mountR.pitch = (1 - e) * 1.6f;
		mountL.pivotY = mountR.pivotY = 1f + (1 - e) * 4f;
		float yaw = MathHelper.clamp(headYaw, -60f, 60f) * MathHelper.RADIANS_PER_DEGREE * e;
		float pitch = MathHelper.clamp(headPitch, -50f, 60f) * MathHelper.RADIANS_PER_DEGREE * e;
		float recoil = MathHelper.sin(age * 0.9f) * 0.02f * e;
		// the elbow joints lift the guns clear of your shoulders, the barrels follow your aim
		jointL.pitch = jointR.pitch = -(1 - e) * 1.2f;
		jointL.roll = -0.12f * e;
		jointR.roll = 0.12f * e;
		cannonL.yaw = cannonR.yaw = yaw;
		cannonL.pitch = cannonR.pitch = pitch + recoil;
		matrices.push();
		getContextModel().body.rotate(matrices);
		root.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
		root.render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV);
		matrices.pop();
	}
}
