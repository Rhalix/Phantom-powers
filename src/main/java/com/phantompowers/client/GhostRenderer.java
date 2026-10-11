package com.phantompowers.client;

import com.phantompowers.entity.AbstractGhostEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Draws ghosts see-through and glowing. */
public class GhostRenderer<T extends AbstractGhostEntity> extends MobEntityRenderer<T, GhostModel<T>> {
	private final Identifier texture;
	private final float scale;

	public GhostRenderer(EntityRendererFactory.Context ctx, Identifier texture, float scale) {
		super(ctx, new GhostModel<>(ctx.getPart(GhostModel.LAYER)), 0.0f);
		this.texture = texture;
		this.scale = scale;
	}

	@Override
	public Identifier getTexture(T entity) {
		return texture;
	}

	@Override
	protected void scale(T entity, MatrixStack matrices, float amount) {
		matrices.scale(scale, scale, scale);
	}

	@Override
	protected int getBlockLight(T entity, BlockPos pos) {
		return 15;
	}
}
