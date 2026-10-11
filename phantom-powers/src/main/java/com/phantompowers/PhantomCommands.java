package com.phantompowers;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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
							st.markDirty();
							GhostPowers.syncNow(p);
							ctx.getSource().sendFeedback(() -> Text.literal("Added " + amount + " stat points."), false);
							return 1;
						})))
				.then(CommandManager.literal("unlockall").executes(ctx -> {
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					GhostPowers.grantPowers(p);
					GhostState st = GhostState.of(p.getServer());
					st.data(p.getUuid()).unlocked = 0b1111;
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
}
