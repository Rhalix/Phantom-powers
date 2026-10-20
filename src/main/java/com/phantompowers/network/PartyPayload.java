package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: your party (members, leader, any invite waiting for you, and who else is online to invite). */
public record PartyPayload(List<String> members, String leader, String inviteFrom, List<String> others) implements CustomPayload {
	public static final CustomPayload.Id<PartyPayload> ID = new CustomPayload.Id<>(PhantomMod.id("party"));
	public static final PacketCodec<PacketByteBuf, PartyPayload> CODEC = PacketCodec.<PacketByteBuf, PartyPayload>of(PartyPayload::write, PartyPayload::read);

	private static void list(PacketByteBuf buf, List<String> l) {
		buf.writeVarInt(l.size());
		for (String s : l) buf.writeString(s, 64);
	}

	private static List<String> list(PacketByteBuf buf) {
		int n = Math.min(64, buf.readVarInt());
		List<String> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) l.add(buf.readString(64));
		return l;
	}

	private void write(PacketByteBuf buf) {
		list(buf, members);
		buf.writeString(leader, 64);
		buf.writeString(inviteFrom, 64);
		list(buf, others);
	}

	private static PartyPayload read(PacketByteBuf buf) {
		return new PartyPayload(list(buf), buf.readString(64), buf.readString(64), list(buf));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
