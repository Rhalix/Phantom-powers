package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server -> client: which players are fully invisible ghosts right now. */
public record InvisiblePayload(List<UUID> players, List<UUID> ghosts, List<UUID> suited, List<UUID> flying, List<UUID> fists, List<UUID> shielded,
		List<UUID> variantIds, List<Integer> variants) implements CustomPayload {
	public static final CustomPayload.Id<InvisiblePayload> ID = new CustomPayload.Id<>(PhantomMod.id("invisible"));
	public static final PacketCodec<PacketByteBuf, InvisiblePayload> CODEC =
			PacketCodec.<PacketByteBuf, InvisiblePayload>of(InvisiblePayload::write, InvisiblePayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(players.size());
		for (UUID id : players) buf.writeUuid(id);
		buf.writeVarInt(ghosts.size());
		for (UUID id : ghosts) buf.writeUuid(id);
		buf.writeVarInt(suited.size());
		for (UUID id : suited) buf.writeUuid(id);
		buf.writeVarInt(flying.size());
		for (UUID id : flying) buf.writeUuid(id);
		buf.writeVarInt(fists.size());
		for (UUID id : fists) buf.writeUuid(id);
		buf.writeVarInt(shielded.size());
		for (UUID id : shielded) buf.writeUuid(id);
		buf.writeVarInt(variantIds.size());
		for (int i = 0; i < variantIds.size(); i++) {
			buf.writeUuid(variantIds.get(i));
			buf.writeVarInt(variants.get(i));
		}
	}

	private static InvisiblePayload read(PacketByteBuf buf) {
		int n = buf.readVarInt();
		List<UUID> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(buf.readUuid());
		int m = buf.readVarInt();
		List<UUID> ghosts = new ArrayList<>(m);
		for (int i = 0; i < m; i++) ghosts.add(buf.readUuid());
		int k = buf.readVarInt();
		List<UUID> suited = new ArrayList<>(k);
		for (int i = 0; i < k; i++) suited.add(buf.readUuid());
		int f = buf.readVarInt();
		List<UUID> flying = new ArrayList<>(f);
		for (int i = 0; i < f; i++) flying.add(buf.readUuid());
		int q = buf.readVarInt();
		List<UUID> fists = new ArrayList<>(q);
		for (int i = 0; i < q; i++) fists.add(buf.readUuid());
		int sh = buf.readVarInt();
		List<UUID> shielded = new ArrayList<>(sh);
		for (int i = 0; i < sh; i++) shielded.add(buf.readUuid());
		int vn = buf.readVarInt();
		List<UUID> variantIds = new ArrayList<>(vn);
		List<Integer> variants = new ArrayList<>(vn);
		for (int i = 0; i < vn; i++) {
			variantIds.add(buf.readUuid());
			variants.add(buf.readVarInt());
		}
		return new InvisiblePayload(list, ghosts, suited, flying, fists, shielded, variantIds, variants);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
