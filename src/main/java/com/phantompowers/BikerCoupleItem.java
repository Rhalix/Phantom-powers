package com.phantompowers;

import com.phantompowers.entity.RevEntity;
import com.phantompowers.entity.RoxyEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Spawn egg for the Biker Couple fight: Rev on his bike with Roxy riding on the back. */
public class BikerCoupleItem extends Item {
	public BikerCoupleItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext ctx) {
		if (!(ctx.getWorld() instanceof ServerWorld w)) return ActionResult.SUCCESS;
		BlockPos at = ctx.getBlockPos().offset(ctx.getSide());
		spawnCouple(w, Vec3d.ofBottomCenter(at), ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYaw() + 180);
		if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getStack().decrement(1);
		return ActionResult.CONSUME;
	}

	/** Rev with Roxy on the back seat. */
	public static RevEntity spawnCouple(ServerWorld w, Vec3d at, float yaw) {
		RevEntity rev = ModEntities.REV.create(w);
		RoxyEntity roxy = ModEntities.ROXY.create(w);
		if (rev == null || roxy == null) return null;
		rev.refreshPositionAndAngles(at.x, at.y, at.z, yaw, 0);
		roxy.refreshPositionAndAngles(at.x, at.y + 1, at.z, yaw, 0);
		rev.setPersistent();
		roxy.setPersistent();
		w.spawnEntity(rev);
		w.spawnEntity(roxy);
		roxy.startRiding(rev, true);
		return rev;
	}
}
