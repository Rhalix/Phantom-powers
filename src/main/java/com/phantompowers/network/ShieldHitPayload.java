package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: something just hit a player's Ecto Shield at this point (for the ripple). */
public record ShieldHitPayload(int entityId, float x, float y, float z) implements CustomPayload {
	public static final CustomPayload.Id<ShieldHitPayload> ID = new CustomPayload.Id<>(PhantomMod.id("shield_hit"));
	public static final PacketCodec<PacketByteBuf, ShieldHitPayload> CODEC =
			PacketCodec.<PacketByteBuf, ShieldHitPayload>of(ShieldHitPayload::write, ShieldHitPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
	}

	private static ShieldHitPayload read(PacketByteBuf buf) {
		return new ShieldHitPayload(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
