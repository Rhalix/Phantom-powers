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
	// ------------------------------------------------------------------ /ghosts

	private static String ghostTag(ServerPlayerEntity p) {
		return TAG + "_ghosts_" + p.getUuidAsString();
	}

	private static List<net.minecraft.entity.EntityType<? extends com.phantompowers.entity.AbstractGhostEntity>> ghostTypes() {
		return List.of(ModEntities.HOSTILE_GHOST, ModEntities.FRIENDLY_GHOST, ModEntities.GHOUL, ModEntities.SPECTER_KNIGHT,
				ModEntities.LOST_SOUL, ModEntities.GHOST_YETI, ModEntities.SHADE, ModEntities.GHOST_JAILER, ModEntities.CRATE_KING,
				ModEntities.VOLTAGE, ModEntities.WISHKEEPER, ModEntities.REV, ModEntities.ROXY, ModEntities.CHEF_GHOULASH,
				ModEntities.COLLECTOR, ModEntities.DOLORA, ModEntities.RAGE_WYVERN);
	}

	/** Put away this player's ghost line-up. */
	public static int clearGhosts(ServerPlayerEntity p) {
		int n = 0;
		String tag = ghostTag(p);
		for (ServerWorld w : p.getServer().getWorlds())
			for (Entity e : w.getEntitiesByType(TypeFilter.instanceOf(com.phantompowers.entity.AbstractGhostEntity.class), v -> v.getCommandTags().contains(tag))) {
				e.discard();
				n++;
			}
		return n;
	}

	/** One of every ghost (and every room ghost) in rows in front of you, frozen in place with name tags. */
	public static int showGhosts(ServerPlayerEntity p) {
		clearGhosts(p);
		ServerWorld w = p.getServerWorld();
		List<com.phantompowers.entity.AbstractGhostEntity> ghosts = new ArrayList<>();
		for (var type : ghostTypes()) {
			var g = type.create(w);
			if (g != null) ghosts.add(g);
		}
		for (int i = 0; i < com.phantompowers.entity.RoomGhostEntity.NAMES.length; i++) {
			var g = ModEntities.ROOM_GHOST.create(w);
			if (g == null) continue;
			g.setVariant(i);
			g.setCustomName(Text.literal(com.phantompowers.entity.RoomGhostEntity.NAMES[i]).formatted(Formatting.AQUA));
			ghosts.add(g);
		}
		Vec3d fwd = Vec3d.fromPolar(0, p.getYaw()).normalize();
		Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
		float faceYaw = p.getYaw() + 180f;
		double rowWidth = 26, across = 0, depth = 0, rowStart = 5;
		List<Object[]> row = new ArrayList<>();
		List<Object[]> placed = new ArrayList<>();
		// lay them out in rows, giving the big ones (the dragon, Lunch Lady's meat monster) more room
		for (var g : ghosts) {
			double wdt = Math.max(1.2, g.getWidth()) + 1.2;
			if (across + wdt > rowWidth && !row.isEmpty()) {
				placed.addAll(centre(row, across, rowStart + depth / 2));
				rowStart += depth + 1.5;
				row.clear();
				across = 0;
				depth = 0;
			}
			row.add(new Object[]{g, across + wdt / 2});
			across += wdt;
			depth = Math.max(depth, wdt);
		}
		if (!row.isEmpty()) placed.addAll(centre(row, across, rowStart + depth / 2));
		int made = 0;
		for (Object[] o : placed) {
			var g = (com.phantompowers.entity.AbstractGhostEntity) o[0];
			double sideOff = (double) o[1], fwdOff = (double) o[2];
			Vec3d at = p.getPos().add(fwd.multiply(fwdOff)).add(side.multiply(sideOff));
			BlockPos spot = standingSpot(w, BlockPos.ofFloored(at));
			g.refreshPositionAndAngles(at.x, spot.getY(), at.z, faceYaw, 0f);
			g.setHeadYaw(faceYaw);
			g.setBodyYaw(faceYaw);
			if (!g.hasCustomName()) g.setCustomName(g.getType().getName().copy().formatted(Formatting.GREEN));
			g.setCustomNameVisible(true);
			g.setAiDisabled(true);
			g.setInvulnerable(true);
			g.setPersistent();
			g.setSilent(true);
			g.addCommandTag(TAG);
			g.addCommandTag(ghostTag(p));
			if (w.spawnEntity(g)) made++;
		}
		final int count = made;
		p.sendMessage(Text.literal("Showing all " + count + " ghost models. ").formatted(Formatting.GREEN)
				.append(Text.literal("/ghosts clear").formatted(Formatting.YELLOW)).append(Text.literal(" puts them away.").formatted(Formatting.GREEN)), false);
		return count;
	}

	/** Centre a row across your view: returns {ghost, sideways offset, forward offset}. */
	private static List<Object[]> centre(List<Object[]> row, double width, double forward) {
		List<Object[]> out = new ArrayList<>();
		for (Object[] o : row) out.add(new Object[]{o[0], (double) o[1] - width / 2, forward});
		return out;
	}
}
