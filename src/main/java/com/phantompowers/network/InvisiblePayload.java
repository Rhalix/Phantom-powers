package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server -> client: per-player looks and states everyone needs to draw (ghost form, suit colours, dark form...). */
public record InvisiblePayload(List<UUID> players, List<UUID> ghosts, List<UUID> suited, List<UUID> flying, List<UUID> fists, List<UUID> shielded,
		List<UUID> colorIds, List<int[]> colors, List<UUID> dark, List<UUID> acne, List<UUID> cannons, List<UUID> aiming) implements CustomPayload {
	public static final CustomPayload.Id<InvisiblePayload> ID = new CustomPayload.Id<>(PhantomMod.id("invisible"));
	public static final PacketCodec<PacketByteBuf, InvisiblePayload> CODEC =
			PacketCodec.<PacketByteBuf, InvisiblePayload>of(InvisiblePayload::write, InvisiblePayload::read);

	private static void ids(PacketByteBuf buf, List<UUID> l) {
		buf.writeVarInt(l.size());
		for (UUID id : l) buf.writeUuid(id);
	}

	private static List<UUID> ids(PacketByteBuf buf) {
		int n = buf.readVarInt();
		List<UUID> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) l.add(buf.readUuid());
		return l;
	}

	private void write(PacketByteBuf buf) {
		ids(buf, players);
		ids(buf, ghosts);
		ids(buf, suited);
		ids(buf, flying);
		ids(buf, fists);
		ids(buf, shielded);
		ids(buf, colorIds);
		for (int[] c : colors) buf.writeIntArray(c);
		ids(buf, dark);
		ids(buf, acne);
		ids(buf, cannons);
		ids(buf, aiming);
	}

	private static InvisiblePayload read(PacketByteBuf buf) {
		List<UUID> players = ids(buf), ghosts = ids(buf), suited = ids(buf), flying = ids(buf), fists = ids(buf), shielded = ids(buf), colorIds = ids(buf);
		List<int[]> colors = new ArrayList<>(colorIds.size());
		for (int i = 0; i < colorIds.size(); i++) colors.add(buf.readIntArray());
		return new InvisiblePayload(players, ghosts, suited, flying, fists, shielded, colorIds, colors, ids(buf), ids(buf), ids(buf), ids(buf));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
