package com.phantompowers;

import com.phantompowers.entity.TownspersonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * /npcs: lines up one of every person model in the mod (the story characters, their dance outfits and the everyday
 * townsfolk) in front of you, each with a name tag, so you can see them all side by side. /npcs clear puts them away.
 */
public final class NpcShowcase {
	public static final String TAG = "phantom_showcase";

	private NpcShowcase() {}

	private record Look(int variant, boolean formal, String name) {}

	private static List<Look> looks() {
		List<Look> l = new ArrayList<>();
		l.add(new Look(TownspersonEntity.LENA, false, "Sam"));
		l.add(new Look(TownspersonEntity.LENA, true, "Sam (dance outfit)"));
		l.add(new Look(TownspersonEntity.TIFFANY, false, "Paulina"));
		l.add(new Look(TownspersonEntity.TIFFANY, true, "Paulina (dance outfit)"));
		l.add(new Look(TownspersonEntity.LEON, false, "Tucker"));
		l.add(new Look(TownspersonEntity.PEMBERTON, false, "Mr. Lancer"));
		l.add(new Look(TownspersonEntity.PAIGE, false, "Princess Dorathea"));
		l.add(new Look(TownspersonEntity.OWEN, false, "Dad"));
		l.add(new Look(TownspersonEntity.MARCUS, false, "Marcus"));
		l.add(new Look(TownspersonEntity.JASMINE, false, "Jasmine"));
		for (int i = 0; i < TownspersonEntity.CITIZENS; i++)
			l.add(new Look(TownspersonEntity.CITIZEN + i, false, "Townsperson " + (i + 1)));
		return l;
	}

	private static String ownTag(ServerPlayerEntity p) {
		return TAG + "_" + p.getUuidAsString();
	}

	/** Put away this player's line-up. Returns how many were removed. */
	public static int clear(ServerPlayerEntity p) {
		int n = 0;
		for (ServerWorld w : p.getServer().getWorlds())
			for (Entity e : w.getEntitiesByType(TypeFilter.instanceOf(TownspersonEntity.class), v -> v.getCommandTags().contains(ownTag(p)))) {
				e.discard();
				n++;
			}
		return n;
	}

	public static int show(ServerPlayerEntity p) {
		clear(p);
		ServerWorld w = p.getServerWorld();
		List<Look> looks = looks();
		// two rows across in front of you, facing you
		Vec3d fwd = Vec3d.fromPolar(0, p.getYaw()).normalize();
		Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
		int perRow = (looks.size() + 1) / 2;
		float faceYaw = p.getYaw() + 180f;
		int made = 0;
		for (int i = 0; i < looks.size(); i++) {
			Look look = looks.get(i);
			int row = i / perRow, col = i % perRow;
			Vec3d at = p.getPos().add(fwd.multiply(5 + row * 3)).add(side.multiply((col - (perRow - 1) / 2.0) * 1.8));
			BlockPos spot = standingSpot(w, BlockPos.ofFloored(at));
			TownspersonEntity v = ModEntities.TOWNSPERSON.create(w);
			if (v == null) continue;
			v.setVariant(look.variant());
			v.setFormalOutfit(look.formal());
			v.refreshPositionAndAngles(at.x, spot.getY(), at.z, faceYaw, 0f);
			v.setHeadYaw(faceYaw);
			v.setBodyYaw(faceYaw);
			v.setCustomName(Text.literal(look.name()).formatted(look.formal() ? Formatting.LIGHT_PURPLE : Formatting.WHITE));
			v.setCustomNameVisible(true);
			v.setAiDisabled(true);
			v.setInvulnerable(true);
			v.setPersistent();
			v.addCommandTag(TAG);
			v.addCommandTag(ownTag(p));
			if (w.spawnEntity(v)) made++;
		}
		final int count = made;
		p.sendMessage(Text.literal("Showing all " + count + " people models. ").formatted(Formatting.GREEN)
				.append(Text.literal("/npcs clear").formatted(Formatting.YELLOW)).append(Text.literal(" puts them away.").formatted(Formatting.GREEN)), false);
		return count;
	}

	/** The nearest place to stand (two blocks of room over solid ground), looking a few blocks up and down. */
	private static BlockPos standingSpot(ServerWorld w, BlockPos base) {
		for (int r = 0; r <= 6; r++)
			for (int dy : new int[]{r, -r}) {
				BlockPos q = base.up(dy);
				if (w.getBlockState(q).getCollisionShape(w, q).isEmpty() && w.getBlockState(q.up()).getCollisionShape(w, q.up()).isEmpty()
						&& w.getBlockState(q).getFluidState().isEmpty() && !w.getBlockState(q.down()).getCollisionShape(w, q.down()).isEmpty())
					return q;
			}
		return base;
	}
}
