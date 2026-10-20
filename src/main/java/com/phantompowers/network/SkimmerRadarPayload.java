package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

/** Server -> pilot: what the Ecto-Skimmer's scanner picks up (ghost lairs, and with the Deep Scanner, ghosts nearby). */
public record SkimmerRadarPayload(boolean inZone, int ghostsNearby, List<Blip> blips) implements CustomPayload {
	public record Blip(String name, int x, int y, int z) {}

	public static final CustomPayload.Id<SkimmerRadarPayload> ID = new CustomPayload.Id<>(PhantomMod.id("skimmer_radar"));
	public static final PacketCodec<PacketByteBuf, SkimmerRadarPayload> CODEC =
			PacketCodec.<PacketByteBuf, SkimmerRadarPayload>of(SkimmerRadarPayload::write, SkimmerRadarPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(inZone);
		buf.writeVarInt(ghostsNearby + 1);
		buf.writeVarInt(blips.size());
		for (Blip b : blips) {
			buf.writeString(b.name(), 64);
			buf.writeInt(b.x());
			buf.writeInt(b.y());
			buf.writeInt(b.z());
		}
	}

	private static SkimmerRadarPayload read(PacketByteBuf buf) {
		boolean zone = buf.readBoolean();
		int ghosts = buf.readVarInt() - 1;
		int n = Math.min(16, buf.readVarInt());
		List<Blip> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(new Blip(buf.readString(64), buf.readInt(), buf.readInt(), buf.readInt()));
		return new SkimmerRadarPayload(zone, ghosts, list);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
