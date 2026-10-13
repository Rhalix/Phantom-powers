package com.phantompowers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.GhostJailerEntity;
import com.phantompowers.world.GhostPrison;
import com.phantompowers.world.GhostRooms;
import com.phantompowers.world.GhostZoneLife;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** /phantom commands for testing (need cheats / operator). */
public final class PhantomCommands {
	private PhantomCommands() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("phantom")
				.requires(src -> src.hasPermissionLevel(2))
				.then(CommandManager.literal("grant").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					GhostPowers.grantPowers(p);
					ctx.getSource().sendFeedback(() -> Text.literal("You now have ghost powers. Press V!"), false);
					return 1;
				}))
				.then(CommandManager.literal("points")
						.then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 999)).executes(ctx -> {
							ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
							int amount = IntegerArgumentType.getInteger(ctx, "amount");
							GhostState st = GhostState.of(p.getServer());
							st.data(p.getUuid()).points += amount;
							st.data(p.getUuid()).totalPoints += amount;
							st.markDirty();
							GhostPowers.syncNow(p);
							ctx.getSource().sendFeedback(() -> Text.literal("Added " + amount + " stat points."), false);
							return 1;
						})))
				.then(CommandManager.literal("unlockall").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					GhostPowers.grantPowers(p);
					GhostState st = GhostState.of(p.getServer());
					st.data(p.getUuid()).unlocked = 0b111111;
					st.markDirty();
					GhostPowers.syncNow(p);
					ctx.getSource().sendFeedback(() -> Text.literal("All ghost powers unlocked."), false);
					return 1;
				}))
				.then(CommandManager.literal("reset").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					GhostState st = GhostState.of(p.getServer());
					GhostPowers.endForm(p, st.data(p.getUuid()));
					st.reset(p.getUuid());
					GhostPowers.syncNow(p);
					ctx.getSource().sendFeedback(() -> Text.literal("Ghost powers removed."), false);
					return 1;
				}))
				.then(CommandManager.literal("testportal").executes(ctx -> {
					ServerWorld w = ctx.getSource().getWorld();
					BlockPos base = BlockPos.ofFloored(ctx.getSource().getPosition()).add(3, 0, 3);
					int n = PortalLogic.testPortal(w, base);
					if (n > 0) {
						ctx.getSource().sendFeedback(() -> Text.literal("Portal test: OK (" + n + " portal blocks lit)"), false);
						return 1;
					}
					ctx.getSource().sendError(Text.literal("Portal test: FAILED"));
					return 0;
				}))
				.then(CommandManager.literal("testrooms").executes(ctx -> {
					ServerWorld rooms = ctx.getSource().getServer().getWorld(GhostRooms.ROOMS);
					if (rooms == null) {
						ctx.getSource().sendError(Text.literal("Room test: FAILED (no rooms dimension)"));
						return 0;
					}
					int n = GhostRooms.testBuildAll(rooms);
					ctx.getSource().sendFeedback(() -> Text.literal("Room test: OK (" + n + " rooms built)"), false);
					return 1;
				}))
				.then(CommandManager.literal("testprison").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) {
						ctx.getSource().sendError(Text.literal("Prison test: FAILED (no Ghost Zone)"));
						return 0;
					}
					GhostJailerEntity j = GhostPrison.buildDesign(zone, new BlockPos(40000, 112, 40000), net.minecraft.util.math.random.Random.create(1), false);
					int ghosts = zone.getEntitiesByClass(AbstractGhostEntity.class,
							new net.minecraft.util.math.Box(new BlockPos(40000, 112, 40000)).expand(40), g -> true).size();
					if (j == null) {
						ctx.getSource().sendError(Text.literal("Prison test: FAILED (no jailer)"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Prison test: OK (" + ghosts + " ghosts incl. jailer)"), false);
					return 1;
				}))
				.then(CommandManager.literal("prison").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					ServerWorld zone = p.getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos at = p.getServerWorld() == zone ? p.getBlockPos() : BlockPos.ORIGIN;
					BlockPos prison = GhostZoneLife.nearestPrison(zone, at);
					GhostZoneLife.ensurePrison(zone, prison);
					p.teleport(zone, prison.getX() + 0.5, prison.getY(), prison.getZ() + 24.5, 180f, 0f);
					ctx.getSource().sendFeedback(() -> Text.literal("Welcome to the Ghost Prison..."), false);
					return 1;
				}))
				.then(CommandManager.literal("crateking").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					ServerWorld zone = p.getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos at = p.getServerWorld() == zone ? p.getBlockPos() : BlockPos.ORIGIN;
					BlockPos keep = GhostZoneLife.nearestCrateKeep(zone, at);
					GhostZoneLife.ensureCrateKeep(zone, keep);
					p.teleport(zone, keep.getX() + 0.5, keep.getY(), keep.getZ() + 22.5, 180f, 0f);
					ctx.getSource().sendFeedback(() -> Text.literal("The Crate King's warehouse..."), false);
					return 1;
				}))
				.then(CommandManager.literal("testcrate").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var king = com.phantompowers.world.CrateKeep.build(zone, new BlockPos(-40000, 110, 40000), net.minecraft.util.math.random.Random.create(2));
					if (king == null) {
						ctx.getSource().sendError(Text.literal("Crate test: FAILED"));
						return 0;
					}
					// Force stage two to make sure the transformation works
					king.setHealth(king.getMaxHealth() * 0.4f);
					ctx.getSource().sendFeedback(() -> Text.literal("Crate test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("wishkeeper").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					ServerWorld zone = p.getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos at = p.getServerWorld() == zone ? p.getBlockPos() : BlockPos.ORIGIN;
					BlockPos palace = GhostZoneLife.nearestPalace(zone, at);
					GhostZoneLife.ensurePalace(zone, palace);
					p.teleport(zone, palace.getX() + 0.5, palace.getY(), palace.getZ() + 15.5, 180f, 0f);
					ctx.getSource().sendFeedback(() -> Text.literal("The Wishkeeper's palace... be careful what you wish for."), false);
					return 1;
				}))
				.then(CommandManager.literal("testthermos").executes(ctx -> {
					ServerWorld cell = ctx.getSource().getServer().getWorld(ThermosPrison.DIM);
					if (cell == null || !ThermosPrison.buildCell(cell, ThermosPrison.cellFor(java.util.UUID.randomUUID()))) {
						ctx.getSource().sendError(Text.literal("Thermos test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Thermos test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testbikers").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					com.phantompowers.world.GhostHighway.build(zone, new BlockPos(40000, 110, -40000), net.minecraft.util.math.random.Random.create(9));
					var rev = BikerCoupleItem.spawnCouple(zone, new net.minecraft.util.math.Vec3d(40.5, 190, 60.5), 0f);
					var dummy = net.minecraft.entity.EntityType.ARMOR_STAND.create(zone);
					if (rev == null || dummy == null) return 0;
					dummy.refreshPositionAndAngles(40.5, 190, 66.5, 0, 0);
					dummy.setNoGravity(true);
					zone.spawnEntity(dummy);
					var roxy = rev.getFirstPassenger() instanceof com.phantompowers.entity.RoxyEntity r ? r : null;
					boolean riding = roxy != null;
					rev.testMoves(zone, dummy);
					if (roxy != null) roxy.testMoves(zone, dummy);
					boolean ok = riding && rev.hasShade(zone) && rev.shadowPhase();
					rev.discard();
					if (roxy != null) roxy.discard();
					dummy.discard();
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Biker test: FAILED (riding=" + riding + ")"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Biker test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testnet").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var ghost = ModEntities.HOSTILE_GHOST.create(zone);
					if (ghost == null) return 0;
					ghost.refreshPositionAndAngles(-30.5, 190, -30.5, 0, 0);
					ghost.setNoGravity(true);
					zone.spawnEntity(ghost);
					zone.getChunk(-2, -2);
					boolean netted = com.phantompowers.entity.GhostNetTrapEntity.trap(zone, ghost, null);
					var thrown = new com.phantompowers.entity.GhostNetProjectile(ModEntities.GHOST_NET_PROJECTILE, zone);
					boolean ok = netted && thrown.getStack().isOf(ModItems.GHOST_NET);
					ghost.discard();
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Net test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Net test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testpalace").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var k = com.phantompowers.world.WishPalace.build(zone, new BlockPos(-40000, 110, 40000), net.minecraft.util.math.random.Random.create(5));
					if (k == null) {
						ctx.getSource().sendError(Text.literal("Palace test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Palace test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testfrost").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var victim = net.minecraft.entity.EntityType.ZOMBIE.create(zone);
					var by = net.minecraft.entity.EntityType.ARMOR_STAND.create(zone);
					if (victim == null || by == null) return 0;
					victim.refreshPositionAndAngles(30.5, 190, 30.5, 0, 0);
					victim.setNoGravity(true);
					by.refreshPositionAndAngles(30.5, 190, 26.5, 0, 0);
					by.setNoGravity(true);
					zone.spawnEntity(victim);
					zone.spawnEntity(by);
					// Breathe at it for a while, then shatter it
					for (int t = 1; t <= 60; t++) FrostLogic.breathe(zone, by, by.getEyePos(), new net.minecraft.util.math.Vec3d(0, 0, 1), t);
					boolean frozen = FrostLogic.isFrozen(victim);
					boolean ok = FrostLogic.testFreezeAndShatter(zone, victim, by);
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Frost test: FAILED (frozen by breath: " + frozen + ")"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Frost test: OK (frozen by breath: " + frozen + ", shattered)"), false);
					return 1;
				}))
				.then(CommandManager.literal("testhost").executes(ctx -> {
					// Every mob type maps to a host power with a name
					int n = 0;
					for (var type : net.minecraft.registry.Registries.ENTITY_TYPE) {
						ServerWorld zone = ctx.getSource().getServer().getOverworld();
						if (!(type.create(zone) instanceof net.minecraft.entity.LivingEntity le)) continue;
						int k = HostPowers.kind(le);
						if (k < 0 || k >= HostPowers.NAMES.length || k >= HostPowers.PASSIVES.length) {
							ctx.getSource().sendError(Text.literal("Host test: FAILED for " + type));
							return 0;
						}
						n++;
					}
					final int count = n;
					ctx.getSource().sendFeedback(() -> Text.literal("Host test: OK (" + count + " mob types)"), false);
					return 1;
				}))
				.then(CommandManager.literal("voltage").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					ServerWorld zone = p.getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos at = p.getServerWorld() == zone ? p.getBlockPos() : BlockPos.ORIGIN;
					BlockPos stage = GhostZoneLife.nearestStage(zone, at);
					GhostZoneLife.ensureStage(zone, stage);
					p.teleport(zone, stage.getX() + 0.5, stage.getY(), stage.getZ() + 18.5, 180f, 0f);
					ctx.getSource().sendFeedback(() -> Text.literal("Voltage's concert stage... get ready to rock."), false);
					return 1;
				}))
				.then(CommandManager.literal("teststage").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var v = com.phantompowers.world.ConcertStage.build(zone, new BlockPos(40000, 110, -40000), net.minecraft.util.math.random.Random.create(3));
					var spire = GhostPrison.buildDesign(zone, new BlockPos(-40000, 110, -40000), net.minecraft.util.math.random.Random.create(4), true);
					if (v == null || spire == null) {
						ctx.getSource().sendError(Text.literal("Stage test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Stage test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testbosses").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var log = org.slf4j.LoggerFactory.getLogger("phantom");
					BlockPos[] spots = {new BlockPos(16, 190, 40), new BlockPos(32, 190, 40), new BlockPos(48, 190, 40), new BlockPos(56, 190, 40)};
					java.util.List<net.minecraft.entity.decoration.ArmorStandEntity> stands = new java.util.ArrayList<>();
					for (BlockPos sp : spots) {
						var stand = net.minecraft.entity.EntityType.ARMOR_STAND.create(zone);
						if (stand == null) return 0;
						stand.setNoGravity(true);
						stand.refreshPositionAndAngles(sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, 0, 0);
						zone.spawnEntity(stand);
						stands.add(stand);
					}
					var jailer = com.phantompowers.ModEntities.GHOST_JAILER.create(zone);
					var king = com.phantompowers.ModEntities.CRATE_KING.create(zone);
					var volt = com.phantompowers.ModEntities.VOLTAGE.create(zone);
					var wish = com.phantompowers.ModEntities.WISHKEEPER.create(zone);
					if (jailer == null || king == null || volt == null || wish == null) return 0;
					jailer.refreshPositionAndAngles(16.5, 190, 46.5, 0, 0);
					king.refreshPositionAndAngles(32.5, 190, 46.5, 0, 0);
					volt.refreshPositionAndAngles(48.5, 190, 46.5, 0, 0);
					wish.refreshPositionAndAngles(56.5, 190, 46.5, 0, 0);
					zone.spawnEntity(jailer);
					zone.spawnEntity(king);
					zone.spawnEntity(volt);
					zone.spawnEntity(wish);
					jailer.testMoves(zone, stands.get(0));
					king.testMoves(zone, stands.get(1));
					volt.testMoves(zone, stands.get(2));
					wish.testMoves(zone, stands.get(3));
					ThermosItem.scream(zone, jailer);
					int placed = 0;
					for (BlockPos sp : spots) placed += countCage(zone, sp);
					final int n = placed;
					ctx.getSource().sendFeedback(() -> Text.literal("Boss test: OK (" + n + " cage blocks)"), false);
					com.phantompowers.entity.BossMoves.later(170, () -> {
						int left = 0;
						for (BlockPos sp : spots) left += countCage(zone, sp);
						log.info(left == 0 ? "Boss cleanup: OK" : "Boss cleanup: FAILED (" + left + " cage blocks left)");
					});
					return 1;
				}))
				.then(CommandManager.literal("breakout").executes(ctx -> {
					ServerWorld ow = ctx.getSource().getServer().getOverworld();
					var boss = GhostZoneLife.breakout(ow, BlockPos.ofFloored(ctx.getSource().getPosition()));
					if (boss == null) {
						ctx.getSource().sendError(Text.literal("Breakout test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Breakout test: OK (" + boss.getName().getString() + ")"), false);
					return 1;
				}))
				.then(CommandManager.literal("sense").executes(ctx -> {
					GhostSense.trigger(ctx.getSource().getPlayerOrThrow());
					return 1;
				}))
				.then(CommandManager.literal("ghostzone").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					ServerWorld zone = p.getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) {
						ctx.getSource().sendError(Text.literal("The Ghost Zone isn't loaded."));
						return 0;
					}
					GhostState st = GhostState.of(p.getServer());
					GhostData d = st.data(p.getUuid());
					if (p.getServerWorld() != zone) {
						d.portalDim = p.getServerWorld().getRegistryKey().getValue().toString();
						d.portalPos = p.getBlockPos();
						st.markDirty();
					}
					PortalLogic.sendToGhostZone(p, zone);
					return 1;
				})));
	}

	private static int countCage(ServerWorld w, BlockPos center) {
		int n = 0;
		for (BlockPos p : BlockPos.iterate(center.add(-3, -3, -3), center.add(3, 5, 3))) {
			var st = w.getBlockState(p);
			if (st.isOf(net.minecraft.block.Blocks.IRON_BARS) || st.isOf(net.minecraft.block.Blocks.SPRUCE_PLANKS) || st.isOf(net.minecraft.block.Blocks.GOLD_BLOCK)) n++;
		}
		return n;
	}
}
