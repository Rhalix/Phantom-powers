package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: this player's ghost sense just went off. */
public record GhostSensePayload(int entityId) implements CustomPayload {
	public static final CustomPayload.Id<GhostSensePayload> ID = new CustomPayload.Id<>(PhantomMod.id("ghost_sense"));
	public static final PacketCodec<PacketByteBuf, GhostSensePayload> CODEC =
			PacketCodec.<PacketByteBuf, GhostSensePayload>of(GhostSensePayload::write, GhostSensePayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(entityId);
	}

	private static GhostSensePayload read(PacketByteBuf buf) {
		return new GhostSensePayload(buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
