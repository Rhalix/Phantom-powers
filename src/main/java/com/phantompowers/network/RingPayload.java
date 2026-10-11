package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: draw transformation rings around an entity for a number of ticks. */
public record RingPayload(int entityId, int duration) implements CustomPayload {
	public static final CustomPayload.Id<RingPayload> ID = new CustomPayload.Id<>(PhantomMod.id("rings"));
	public static final PacketCodec<PacketByteBuf, RingPayload> CODEC =
			PacketCodec.<PacketByteBuf, RingPayload>of(RingPayload::write, RingPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeVarInt(duration);
	}

	private static RingPayload read(PacketByteBuf buf) {
		return new RingPayload(buf.readVarInt(), buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
