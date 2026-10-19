package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.DoloraEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Draws Dolora in whichever form she's in. While she transforms she shudders and swells out of a violet flash;
 * the wraith grows bigger the more misery it eats.
 */
public class DoloraRenderer extends MobEntityRenderer<DoloraEntity, DoloraModel> {
	private static final String[] NAMES = {"dolora_counselor", "dolora_misery", "dolora_queen"};
	private static final Identifier[] TEX = new Identifier[3], GLOW = new Identifier[3];

	static {
		for (int i = 0; i < 3; i++) {
			TEX[i] = PhantomMod.id("textures/entity/" + NAMES[i] + ".png");
			GLOW[i] = PhantomMod.id("textures/entity/" + NAMES[i] + "_glow.png");
		}
	}

	public DoloraRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new DoloraModel(ctx.getPart(DoloraModel.COUNSELOR), ctx.getPart(DoloraModel.MISERY), ctx.getPart(DoloraModel.QUEEN)), 0.5f);
		addFeature(new FeatureRenderer<>(this) {
			@Override
			public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, DoloraEntity e, float limbAngle, float limbDistance,
					float tickDelta, float age, float headYaw, float headPitch) {
				if (e.isInvisible()) return;
				getContextModel().render(matrices, consumers.getBuffer(RenderLayer.getEyes(GLOW[phase(e)])), 0xF000F0, OverlayTexture.DEFAULT_UV, -1);
			}
		});
	}

	private static int phase(DoloraEntity e) {
		return MathHelper.clamp(e.phase(), 0, 2);
	}

	@Override
	protected void scale(DoloraEntity e, MatrixStack matrices, float amount) {
		float s = e.phase() == DoloraEntity.WRAITH ? 1.1f + e.fed() * 0.6f : e.phase() == DoloraEntity.QUEEN ? 1.15f : 1f;
		int ch = e.changing();
		if (ch > 0) {
			// shuddering and swelling into the new form
			float k = 1f - ch / 60f;
			s *= 0.55f + 0.45f * k * k;
			float shake = MathHelper.sin((e.age + amount) * 2.3f) * 0.05f * (1 - k);
			matrices.translate(shake, 0, -shake);
		}
		matrices.scale(s, s, s);
	}

	@Override
	protected int getBlockLight(DoloraEntity e, BlockPos pos) {
		return e.phase() == DoloraEntity.HUMAN ? super.getBlockLight(e, pos) : 12;
	}

	@Override
	protected RenderLayer getRenderLayer(DoloraEntity e, boolean showBody, boolean translucent, boolean showOutline) {
		if (showBody && e.phase() != DoloraEntity.HUMAN) return RenderLayer.getEntityTranslucent(getTexture(e));
		return super.getRenderLayer(e, showBody, translucent, showOutline);
	}

	@Override
	public Identifier getTexture(DoloraEntity e) {
		return TEX[phase(e)];
	}
}
