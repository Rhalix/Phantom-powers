package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: a ghost just slammed into an Ecto-Shield dome here (for the ripple). */
public record DomeHitPayload(int domeId, float x, float y, float z) implements CustomPayload {
	public static final CustomPayload.Id<DomeHitPayload> ID = new CustomPayload.Id<>(PhantomMod.id("dome_hit"));
	public static final PacketCodec<PacketByteBuf, DomeHitPayload> CODEC =
			PacketCodec.<PacketByteBuf, DomeHitPayload>of(DomeHitPayload::write, DomeHitPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(domeId);
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
	}

	private static DomeHitPayload read(PacketByteBuf buf) {
		return new DomeHitPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
