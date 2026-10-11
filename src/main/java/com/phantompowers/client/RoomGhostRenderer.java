package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.RoomGhostEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Each room's ghost gets its own look. */
public class RoomGhostRenderer extends MobEntityRenderer<RoomGhostEntity, GhostModel<RoomGhostEntity>> {
	private static final Identifier[] TEXTURES = new Identifier[RoomGhostEntity.NAMES.length];

	static {
		for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = PhantomMod.id("textures/entity/room_ghost_" + i + ".png");
	}

	public RoomGhostRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new GhostModel<>(ctx.getPart(GhostModel.LAYER)), 0.0f);
	}

	@Override
	public Identifier getTexture(RoomGhostEntity e) {
		return TEXTURES[e.getVariant()];
	}

	@Override
	protected int getBlockLight(RoomGhostEntity e, BlockPos pos) {
		return 15;
	}
}
