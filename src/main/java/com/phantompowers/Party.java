package com.phantompowers;

import com.phantompowers.network.PartyPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Parties: invite other players to team up. Party members can't hurt each other (fists, powers, beams, arrows,
 * clones and all). Parties last until everyone leaves or the server restarts.
 */
public final class Party {
	public static final int REFRESH = 0, INVITE = 1, ACCEPT = 2, DECLINE = 3, LEAVE = 4, KICK = 5;
	public static final int MAX_SIZE = 8;
	private static final Map<UUID, UUID> LEADER_OF = new HashMap<>();
	private static final Map<UUID, Set<UUID>> MEMBERS = new HashMap<>();
	private static final Map<UUID, UUID> INVITES = new HashMap<>();
	private static final Map<UUID, Long> INVITE_TIME = new HashMap<>();

	private Party() {}

	public static Set<UUID> partyOf(UUID player) {
		UUID leader = LEADER_OF.get(player);
		return leader == null ? Set.of(player) : MEMBERS.getOrDefault(leader, Set.of(player));
	}

	public static boolean allies(UUID a, UUID b) {
		if (a == null || b == null || a.equals(b)) return false;
		UUID la = LEADER_OF.get(a);
		return la != null && la.equals(LEADER_OF.get(b));
	}

	/** Whoever is really behind this damage: the player, or the owner of their clone / projectile. */
	private static UUID culprit(DamageSource source) {
		Entity e = source.getAttacker();
		if (e instanceof ServerPlayerEntity p) return p.getUuid();
		if (e instanceof com.phantompowers.entity.PlayerCloneEntity c) return c.ownerId();
		if (e instanceof net.minecraft.entity.Ownable o && o.getOwner() instanceof ServerPlayerEntity p) return p.getUuid();
		return null;
	}

	/** Called from the damage hook: false = this hit is between party members, so cancel it. */
	public static boolean allowDamage(Entity victim, DamageSource source) {
		if (LEADER_OF.isEmpty()) return true;
		UUID target = victim instanceof ServerPlayerEntity p ? p.getUuid()
				: victim instanceof com.phantompowers.entity.PlayerCloneEntity c ? c.ownerId() : null;
		if (target == null) return true;
		return !allies(target, culprit(source));
	}

	public static void handle(ServerPlayerEntity p, int action, String targetName) {
		MinecraftServer server = p.getServer();
		ServerPlayerEntity target = targetName == null || targetName.isEmpty() ? null : server.getPlayerManager().getPlayer(targetName);
		switch (action) {
			case INVITE -> invite(p, target);
			case ACCEPT -> accept(p);
			case DECLINE -> {
				UUID from = INVITES.remove(p.getUuid());
				if (from != null) {
					ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(from);
					if (inviter != null) inviter.sendMessage(Text.literal(p.getName().getString() + " declined your party invite.").formatted(Formatting.GRAY), false);
				}
			}
			case LEAVE -> leave(server, p.getUuid(), true);
			case KICK -> {
				UUID leader = LEADER_OF.get(p.getUuid());
				if (target != null && leader != null && leader.equals(p.getUuid()) && allies(p.getUuid(), target.getUuid())) {
					leave(server, target.getUuid(), false);
					target.sendMessage(Text.literal("You were removed from the party.").formatted(Formatting.YELLOW), false);
				}
			}
			default -> {}
		}
		send(p);
	}

	private static void invite(ServerPlayerEntity p, ServerPlayerEntity target) {
		if (target == null || target == p) {
			p.sendMessage(Text.literal("That player isn't online.").formatted(Formatting.RED), true);
			return;
		}
		if (allies(p.getUuid(), target.getUuid())) {
			p.sendMessage(Text.literal(target.getName().getString() + " is already in your party.").formatted(Formatting.YELLOW), true);
			return;
		}
		if (partyOf(p.getUuid()).size() >= MAX_SIZE) {
			p.sendMessage(Text.literal("Your party is full (" + MAX_SIZE + ").").formatted(Formatting.RED), true);
			return;
		}
		INVITES.put(target.getUuid(), p.getUuid());
		INVITE_TIME.put(target.getUuid(), p.getServer().getTicks() + 20L * 120);
		p.sendMessage(Text.literal("Party invite sent to " + target.getName().getString() + ".").formatted(Formatting.GREEN), true);
		Text accept = Text.literal("[Accept]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept")));
		Text decline = Text.literal("[Decline]").setStyle(Style.EMPTY.withColor(Formatting.RED)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party decline")));
		target.sendMessage(Text.literal(p.getName().getString() + " invited you to their party! ").formatted(Formatting.AQUA)
				.append(accept).append(Text.literal(" ")).append(decline), false);
		target.getServerWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1f, 1.4f);
		send(target);
	}

	private static void accept(ServerPlayerEntity p) {
		UUID from = INVITES.remove(p.getUuid());
		Long until = INVITE_TIME.remove(p.getUuid());
		MinecraftServer server = p.getServer();
		if (from == null || until == null || server.getTicks() > until) {
			p.sendMessage(Text.literal("You don't have a party invite (or it ran out).").formatted(Formatting.RED), true);
			return;
		}
		ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(from);
		if (inviter == null) {
			p.sendMessage(Text.literal("They've left the game.").formatted(Formatting.RED), true);
			return;
		}
		leave(server, p.getUuid(), true);
		UUID leader = LEADER_OF.computeIfAbsent(from, k -> from);
		Set<UUID> set = MEMBERS.computeIfAbsent(leader, k -> new LinkedHashSet<>(List.of(leader)));
		if (set.size() >= MAX_SIZE) {
			p.sendMessage(Text.literal("That party is full.").formatted(Formatting.RED), true);
			return;
		}
		set.add(p.getUuid());
		LEADER_OF.put(p.getUuid(), leader);
		for (UUID id : set) {
			ServerPlayerEntity m = server.getPlayerManager().getPlayer(id);
			if (m == null) continue;
			m.sendMessage(Text.literal(p.getName().getString() + " joined the party!").formatted(Formatting.GREEN), false);
			m.getServerWorld().playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.5f);
			send(m);
		}
	}

	public static void leave(MinecraftServer server, UUID id, boolean announce) {
		UUID leader = LEADER_OF.remove(id);
		if (leader == null) return;
		Set<UUID> set = MEMBERS.get(leader);
		if (set == null) return;
		set.remove(id);
		if (leader.equals(id) && !set.isEmpty()) {
			// hand the party to the next member
			UUID next = set.iterator().next();
			MEMBERS.remove(leader);
			MEMBERS.put(next, set);
			for (UUID m : set) LEADER_OF.put(m, next);
			leader = next;
		}
		if (set.size() <= 1) {
			for (UUID m : set) LEADER_OF.remove(m);
			MEMBERS.remove(leader);
		}
		ServerPlayerEntity me = server.getPlayerManager().getPlayer(id);
		String name = me != null ? me.getName().getString() : "Someone";
		for (UUID m : new ArrayList<>(set)) {
			ServerPlayerEntity sp = server.getPlayerManager().getPlayer(m);
			if (sp == null) continue;
			if (announce) sp.sendMessage(Text.literal(name + " left the party.").formatted(Formatting.YELLOW), false);
			send(sp);
		}
		if (me != null) send(me);
	}

	public static void send(ServerPlayerEntity p) {
		if (!ServerPlayNetworking.canSend(p, PartyPayload.ID)) return;
		MinecraftServer server = p.getServer();
		List<String> members = new ArrayList<>();
		for (UUID id : partyOf(p.getUuid())) {
			ServerPlayerEntity m = server.getPlayerManager().getPlayer(id);
			members.add(m != null ? m.getName().getString() : "(offline)");
		}
		UUID leader = LEADER_OF.get(p.getUuid());
		ServerPlayerEntity lp = leader == null ? null : server.getPlayerManager().getPlayer(leader);
		List<String> others = new ArrayList<>();
		for (ServerPlayerEntity o : server.getPlayerManager().getPlayerList()) {
			if (o != p && !allies(p.getUuid(), o.getUuid())) others.add(o.getName().getString());
		}
		UUID from = INVITES.get(p.getUuid());
		ServerPlayerEntity inviter = from == null ? null : server.getPlayerManager().getPlayer(from);
		ServerPlayNetworking.send(p, new PartyPayload(members, lp != null ? lp.getName().getString() : "",
				inviter != null ? inviter.getName().getString() : "", others));
	}

	public static void onLeaveGame(ServerPlayerEntity p) {
		leave(p.getServer(), p.getUuid(), true);
		INVITES.remove(p.getUuid());
	}
}
