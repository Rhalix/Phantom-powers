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

	public static final String[] PLACES = {"amity_park", "ghost_lab", "ghost_prison", "crate_keep", "concert_stage", "wish_palace", "ghost_highway", "gloom_manor"};

	/** Teleport to one of the mod's structures (building it first if it isn't there yet). */
	static int teleport(ServerCommandSource src, String place) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		var server = src.getServer();
		if (place.equals("amity_park") || place.equals("ghost_lab")) {
			if (!com.phantompowers.world.AmityPark.request(server)) {
				src.sendFeedback(() -> Text.literal("Amity Park is being built (" + com.phantompowers.world.AmityPark.progress(server) + "%). Try again in a moment!"), false);
				return 0;
			}
			BlockPos c = com.phantompowers.world.AmityPark.state(server).center;
			net.minecraft.util.math.Vec3d at = place.equals("amity_park") ? com.phantompowers.world.AmityPark.entrance(c)
					: net.minecraft.util.math.Vec3d.ofBottomCenter(com.phantompowers.world.AmityPark.lab(c)).add(0, 0, 2);
			p.teleport(server.getOverworld(), at.x, at.y, at.z, place.equals("amity_park") ? -90f : 180f, 0f);
			src.sendFeedback(() -> Text.literal("Teleported to " + (place.equals("amity_park") ? "Amity Park" : "the Ghost Lab")), false);
			return 1;
		}
		ServerWorld zone = server.getWorld(PortalLogic.GHOST_ZONE);
		if (zone == null) return 0;
		BlockPos near = p.getWorld() == zone ? p.getBlockPos() : BlockPos.ORIGIN;
		BlockPos at;
		switch (place) {
			case "ghost_prison" -> { at = GhostZoneLife.nearestPrison(zone, near); GhostZoneLife.ensurePrison(zone, at); }
			case "crate_keep" -> { at = GhostZoneLife.nearestCrateKeep(zone, near); GhostZoneLife.ensureCrateKeep(zone, at); }
			case "concert_stage" -> { at = GhostZoneLife.nearestStage(zone, near); GhostZoneLife.ensureStage(zone, at); }
			case "wish_palace" -> { at = GhostZoneLife.nearestPalace(zone, near); GhostZoneLife.ensurePalace(zone, at); }
			case "ghost_highway" -> { at = GhostZoneLife.nearestRoad(zone, near); GhostZoneLife.ensureRoad(zone, at); }
			case "gloom_manor" -> { at = GhostZoneLife.nearestManor(zone, near); GhostZoneLife.ensureManor(zone, at); }
			default -> {
				src.sendError(Text.literal("Unknown place. Try: " + String.join(", ", PLACES)));
				return 0;
			}
		}
		// land on top of whatever is there
		BlockPos.Mutable m = new BlockPos.Mutable(at.getX(), Math.min(zone.getTopY() - 2, at.getY() + 40), at.getZ());
		while (m.getY() > at.getY() - 20 && zone.getBlockState(m).isAir()) m.move(0, -1, 0);
		double y = zone.getBlockState(m).isAir() ? at.getY() + 2 : m.getY() + 1;
		p.teleport(zone, at.getX() + 0.5, y, at.getZ() + 0.5, p.getYaw(), 0f);
		p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING, 100, 0, false, false));
		String nice = place.replace('_', ' ');
		src.sendFeedback(() -> Text.literal("Teleported to the " + nice + " (" + at.getX() + ", " + at.getZ() + ")"), false);
		return 1;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("party")
				.then(CommandManager.literal("invite").then(CommandManager.argument("player", net.minecraft.command.argument.EntityArgumentType.player())
						.executes(ctx -> {
							Party.handle(ctx.getSource().getPlayerOrThrow(), Party.INVITE,
									net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player").getName().getString());
							return 1;
						})))
				.then(CommandManager.literal("accept").executes(ctx -> { Party.handle(ctx.getSource().getPlayerOrThrow(), Party.ACCEPT, ""); return 1; }))
				.then(CommandManager.literal("decline").executes(ctx -> { Party.handle(ctx.getSource().getPlayerOrThrow(), Party.DECLINE, ""); return 1; }))
				.then(CommandManager.literal("leave").executes(ctx -> { Party.handle(ctx.getSource().getPlayerOrThrow(), Party.LEAVE, ""); return 1; })));
		dispatcher.register(CommandManager.literal("story")
				.executes(ctx -> {
					Story.open(ctx.getSource().getPlayerOrThrow());
					return 1;
				})
				.then(CommandManager.literal("restart").executes(ctx -> {
					Story.restart(ctx.getSource().getPlayerOrThrow());
					return 1;
				}))
				.then(CommandManager.literal("travel").executes(ctx -> {
					Story.fastTravel(ctx.getSource().getPlayerOrThrow());
					return 1;
				})));
		dispatcher.register(CommandManager.literal("phantom")
				.requires(src -> src.hasPermissionLevel(2))
				.then(CommandManager.literal("tp")
						.then(CommandManager.argument("place", com.mojang.brigadier.arguments.StringArgumentType.word())
								.suggests((ctx, b) -> net.minecraft.command.CommandSource.suggestMatching(PLACES, b))
								.executes(ctx -> teleport(ctx.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "place")))))
				.then(CommandManager.literal("acne")
						.then(CommandManager.literal("cure").executes(ctx -> {
							DarkPowers.endAcne(ctx.getSource().getPlayerOrThrow(), false);
							ctx.getSource().sendFeedback(() -> Text.literal("Ecto acne cured."), false);
							return 1;
						}))
						.then(CommandManager.literal("finish").executes(ctx -> {
							DarkPowers.endAcne(ctx.getSource().getPlayerOrThrow(), true);
							ctx.getSource().sendFeedback(() -> Text.literal("Ecto acne finished: you're the dark half-ghost now."), false);
							return 1;
						})))
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
					// the couple's special stage: put them both below half and make sure they team up
					if (roxy != null) {
						rev.setHealth(rev.getMaxHealth() * 0.3f);
						roxy.setHealth(roxy.getMaxHealth() * 0.3f);
						rev.startRideOrDie(zone, roxy);
					}
					var shadeBoss = ModEntities.SHADE.create(zone);
					boolean shadeOk = false;
					if (shadeBoss != null) {
						shadeBoss.refreshPositionAndAngles(36.5, 190, 60.5, 0, 0);
						zone.spawnEntity(shadeBoss);
						shadeBoss.testMoves(zone, dummy);
						shadeOk = shadeBoss.isBoss() && com.phantompowers.FrostLogic.isBoss(shadeBoss);
						shadeBoss.discard();
					}
					boolean ok = shadeOk && riding && rev.hasShade(zone) && rev.shadowPhase() && rev.rideOrDie() && roxy != null && roxy.getVehicle() == rev;
					rev.discard();
					if (roxy != null) roxy.discard();
					dummy.discard();
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Biker test: FAILED (riding=" + riding + ", shadeBoss=" + shadeOk + ")"));
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
				.then(CommandManager.literal("testzone").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var biomes = zone.getRegistryManager().get(net.minecraft.registry.RegistryKeys.BIOME);
					boolean ok = true;
					for (String b : new String[]{"ghost_zone", "frostbite_tundra", "shadow_thicket", "crystal_spires", "wailing_wastes"})
						ok &= biomes.containsId(PhantomMod.id(b));
					var ghoul = ModEntities.GHOUL.create(zone);
					var knight = ModEntities.SPECTER_KNIGHT.create(zone);
					var soul = ModEntities.LOST_SOUL.create(zone);
					var yeti = ModEntities.GHOST_YETI.create(zone);
					ok &= ghoul != null && ghoul.isWalker() && knight != null && soul != null && yeti != null && !yeti.isTamed();
					// sample the biome map over a wide area: more than one biome should show up
					java.util.Set<String> seen = new java.util.HashSet<>();
					for (int x = -3000; x <= 3000; x += 250) for (int z = -3000; z <= 3000; z += 250) {
						zone.getChunkManager().getChunkGenerator().getBiomeSource();
						var key = zone.getChunkManager().getNoiseConfig() == null ? null
								: zone.getChunkManager().getChunkGenerator().getBiomeSource().getBiome(x >> 2, 100 >> 2, z >> 2,
										zone.getChunkManager().getNoiseConfig().getMultiNoiseSampler()).getKey();
						if (key != null && key.isPresent()) seen.add(key.get().getValue().getPath());
					}
					ok &= seen.size() >= 3;
					final String found = String.join(",", seen);
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Zone test: FAILED (" + found + ")"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Zone test: OK (" + found + ")"), false);
					return 1;
				}))
				.then(CommandManager.literal("testdolora").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos c = new BlockPos(60000, 120, -60000);
					zone.getChunk(c.getX() >> 4, c.getZ() >> 4);
					var dolora = com.phantompowers.world.GloomManor.build(zone, c, net.minecraft.util.math.random.Random.create(3));
					var dummy = ModEntities.FRIENDLY_GHOST.create(zone);
					boolean ok = dolora != null && dummy != null;
					if (ok) {
						dummy.refreshPositionAndAngles(c.getX() + 2.5, c.getY() + 1, c.getZ() + 2.5, 0, 0);
						zone.spawnEntity(dummy);
						dolora.testMoves(zone, dummy);
						ok = dolora.phase() == com.phantompowers.entity.DoloraEntity.QUEEN && FrostLogic.isBoss(dolora);
						dolora.discard();
						dummy.discard();
					}
					// cloning: a clone can be created and set up
					var clone = ModEntities.PLAYER_CLONE.create(zone);
					ok &= clone != null;
					if (clone != null) {
						clone.setup(java.util.UUID.randomUUID(), "Tester", true);
						ok &= clone.isFemale() && clone.getCustomName() != null;
					}
					ok &= ModBlocks.CLONING_POD.getDefaultState().contains(CloningPodBlock.ACTIVE);
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Dolora test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Dolora test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testdark").executes(ctx -> {
					ServerWorld zone = ctx.getSource().getServer().getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					var c = ModEntities.ECTO_CONSTRUCT.create(zone);
					var m = new com.phantompowers.entity.MissileEntity(ModEntities.MISSILE, zone);
					int[] col = SuitColors.sanitize(new int[]{0x123456, 0xFFFFFFFF, 1, 2, 3, 7});
					boolean ok = c != null && col[1] == 0xFFFFFF && col[SuitColors.FLAGS] == 1
							&& GhostPowers.darkOnly(GhostPowers.STINGER) && !GhostPowers.darkOnly(GhostPowers.BLAST)
							&& GhostPowers.NAMES.length == GhostPowers.SKILL_COUNT && GhostPowers.DESCRIPTIONS.length == GhostPowers.SKILL_COUNT
							&& GhostPowers.COSTS.length == GhostPowers.SKILL_COUNT
							&& HunterGear.tier(new net.minecraft.item.ItemStack(ModItems.HUNTER_SUIT)) == 0 && m.getStack() != null
							&& ModItems.PROTOTYPE_PORTAL instanceof net.minecraft.item.BlockItem
							&& ModBlocks.PROTOTYPE_PORTAL.getDefaultState().contains(PrototypePortalBlock.FACING)
							&& DarkPowers.portalKnock(DarkPowers.PORTAL_TICKS) > 2.0 && DarkPowers.portalKnock(10) < 0
							&& DarkPowers.spinPos(net.minecraft.util.math.Vec3d.ZERO, 20).length() > 1
							&& Cutscenes.originForward(Cutscenes.ORIGIN_TICKS) > 2 && Cutscenes.darkLift(40) > 0
							&& HunterPaint.paint(new net.minecraft.item.ItemStack(ModItems.GHOST_BLASTER)) == -1
							&& HunterPaint.weaponBoost(new net.minecraft.item.ItemStack(ModItems.GHOST_BLASTER)) == 1f
							&& !HunterPaint.costText(3).isEmpty() && ModBlocks.HUNTER_WORKBENCH.getDefaultState().contains(HunterWorkbenchBlock.FACING)
							&& ModItems.ACNE_CREAM != null && Cutscenes.heroLift(30) > 0 && Cutscenes.SUIT_DONE < Cutscenes.SUIT_TICKS
							&& Cutscenes.heroOffLift(0) > 0 && Cutscenes.darkOffLift(30) > 0 && Cutscenes.SUIT_OFF_DONE < Cutscenes.SUIT_OFF_TICKS;
					if (!ok) {
						ctx.getSource().sendError(Text.literal("Dark test: FAILED"));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Dark test: OK"), false);
					return 1;
				}))
				.then(CommandManager.literal("testgear").executes(ctx -> {
					var server = ctx.getSource().getServer();
					ServerWorld zone = server.getWorld(PortalLogic.GHOST_ZONE);
					if (zone == null) return 0;
					BlockPos o = new BlockPos(7000, 150, 7000);
					zone.getChunk(o);
					zone.getChunk(o.add(12, 0, 12));
					java.util.List<BlockPos> ems = java.util.List.of(o, o.east(10), o.south(10));
					for (BlockPos e : ems) zone.setBlockState(e, ModBlocks.SHIELD_EMITTER.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
					ShieldDomes.Dome d = ShieldDomes.raise(zone, 424242, ems);
					boolean domeOk = d != null && d.radius > 6 && zone.getBlockState(o).get(ShieldEmitterBlock.ACTIVE) && ShieldDomes.isUp(424242);
					boolean pushed = false;
					if (d != null) {
						var ghost = ModEntities.HOSTILE_GHOST.create(zone);
						if (ghost != null) {
							ghost.refreshPositionAndAngles(d.center.x, d.center.y + 2, d.center.z, 0, 0);
							zone.spawnEntity(ghost);
							ShieldDomes.tick(server);
							pushed = ghost.getBoundingBox().getCenter().distanceTo(d.center) > d.radius;
							ghost.discard();
						}
					}
					ShieldDomes.lower(server, 424242, false);
					boolean off = !zone.getBlockState(o).get(ShieldEmitterBlock.ACTIVE) && !ShieldDomes.isUp(424242);
					for (BlockPos e : ems) zone.setBlockState(e, net.minecraft.block.Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);
					var sk = ModEntities.SKIMMER.create(zone);
					boolean skOk = false;
					if (sk != null) {
						sk.setUpgrades(com.phantompowers.entity.SkimmerEntity.UP_CANNONS | com.phantompowers.entity.SkimmerEntity.UP_SCANNER);
						var item = sk.asItem();
						skOk = item.isOf(ModItems.ECTO_SKIMMER) && sk.has(com.phantompowers.entity.SkimmerEntity.UP_SCANNER)
								&& !sk.has(com.phantompowers.entity.SkimmerEntity.UP_PHASE) && sk.canopyOpen(0) == 0f && sk.seatProgress(0) == 1f;
					}
					int lairs = com.phantompowers.world.GhostZoneLife.lairSignals(zone, o).size();
					int lab = 0;
					for (int i = 1; i <= PortalLogic.LAB_W; i++) for (int j = 1; j <= PortalLogic.LAB_H; j++) if (PortalLogic.labOpening(i, j)) lab++;
					BlockPos town = com.phantompowers.world.AmityPark.ensure(server);
					ServerWorld ow = server.getOverworld();
					BlockPos labFloor = com.phantompowers.world.AmityPark.lab(town);
					boolean amity = ow.getBlockState(labFloor.down()).isOf(net.minecraft.block.Blocks.SMOOTH_STONE)
							|| ow.getBlockState(labFloor.down()).isOf(net.minecraft.block.Blocks.LIME_CONCRETE);
					boolean frame = false;
					for (BlockPos q : BlockPos.iterate(labFloor.add(-12, 0, -12), labFloor.add(12, 7, 12))) {
						if (ow.getBlockState(q).isOf(ModBlocks.PORTAL_FRAME)) { frame = true; break; }
					}
					boolean story = Story.missionTitle(Story.HUNTER, 1).contains("Ghost Must Die") && Story.missionTitle(Story.HERO, 1).contains("Molecules")
							&& Story.missionTitle(Story.EVIL, 2).contains("Lunch Rush");
					// Chef Ghoulash (mission 2's ghost): every move, the Meat Monster form, and the story daze
					boolean chef = false;
					{
						var ch = ModEntities.CHEF_GHOULASH.create(ow);
						var stand = net.minecraft.entity.EntityType.ARMOR_STAND.create(ow);
						if (ch != null && stand != null) {
							net.minecraft.util.math.Vec3d cafe = com.phantompowers.world.AmityPark.cafeteria(town);
							ch.refreshPositionAndAngles(cafe.x, cafe.y + 1, cafe.z, 0, 0);
							stand.refreshPositionAndAngles(cafe.x + 3, cafe.y, cafe.z, 0, 0);
							ch.addCommandTag(com.phantompowers.entity.ChefGhoulashEntity.STORY_TAG);
							ow.spawnEntity(ch);
							ow.spawnEntity(stand);
							ch.testMoves(ow, stand);
							boolean big = ch.meat() && ch.getWidth() > 1.2f;
							ch.damage(ow.getDamageSources().magic(), 9999f);
							boolean dazedAlive = ch.isAlive() && ch.dazed() && ThermosItem.canTrap(ch);
							chef = big && dazedAlive;
							for (var e : ow.getOtherEntities(null, ch.getBoundingBox().expand(30), e -> e instanceof com.phantompowers.entity.FoodProjectileEntity
									|| e.getCommandTags().contains("phantom_kitchen"))) e.discard();
							ch.discard();
							stand.discard();
						}
					}
					// the Rage Wyvern (mission 3): every move, Berserk, the story daze and pulling the amulet off
					boolean wyvernOk = false;
					{
						var wy = ModEntities.RAGE_WYVERN.create(ow);
						var stand = net.minecraft.entity.EntityType.ARMOR_STAND.create(ow);
						if (wy != null && stand != null) {
							net.minecraft.util.math.Vec3d f = com.phantompowers.world.AmityPark.parkFountain(town);
							wy.refreshPositionAndAngles(f.x, f.y + 4, f.z, 0, 0);
							stand.refreshPositionAndAngles(f.x + 3, f.y, f.z, 0, 0);
							wy.addCommandTag(com.phantompowers.entity.RageWyvernEntity.STORY_TAG);
							ow.spawnEntity(wy);
							ow.spawnEntity(stand);
							wy.testMoves(ow, stand);
							wy.damage(ow.getDamageSources().magic(), 9999f);
							wyvernOk = wy.berserk() && wy.isAlive() && wy.dazed() && !ThermosItem.canTrap(wy)
									&& Story.missionTitle(Story.HERO, 3).contains("Dance Disaster");
							for (var e : ow.getOtherEntities(null, wy.getBoundingBox().expand(30), e -> e instanceof com.phantompowers.entity.WyvernFireballEntity)) e.discard();
							wy.discard();
							stand.discard();
						}
					}
					story = story && chef && wyvernOk;
					boolean sword = ModItems.ECTO_SWORD != null && !EctoSwordItem.lit(new net.minecraft.item.ItemStack(ModItems.ECTO_SWORD))
							&& HunterPaint.cost(3)[0][0] == ModItems.PHANTOM_HEART && HunterPaint.gadgetCost(1).length == 4
							&& !Party.allies(java.util.UUID.randomUUID(), java.util.UUID.randomUUID()) && Slots.GADGETS.length == 4;
					GhostData td = new GhostData();
					td.unlocked = 0b111;
					td.loadout = 0b11;
					Slots.ensure(td);
					sword = sword && td.slots[0] == 0 && td.slots[1] == 1 && td.slots[2] == -1 && td.equipped(1);
					String detail = "dome=" + domeOk + " pushed=" + pushed + " off=" + off + " skimmer=" + skOk + " lairs=" + lairs + " lab=" + lab
							+ " amity=" + amity + " frame=" + frame + " story=" + story + " sword=" + sword;
					if (!(domeOk && pushed && off && skOk && lairs == 6 && lab == 16 && amity && frame && story && sword)) {
						ctx.getSource().sendError(Text.literal("Gear test: FAILED " + detail));
						return 0;
					}
					ctx.getSource().sendFeedback(() -> Text.literal("Gear test: OK (" + detail + ")"), false);
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
