package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server -> client: draw a shockwave.
 * kind 0 = a cone of sound-wave rings flying forward along (dx,dy,dz);
 * kind 1 = a ring rolling outward along the ground from (x,y,z).
 */
public record ShockwavePayload(float x, float y, float z, float dx, float dy, float dz, float range, int color, int kind)
		implements CustomPayload {
	public static final CustomPayload.Id<ShockwavePayload> ID = new CustomPayload.Id<>(PhantomMod.id("shockwave"));
	public static final PacketCodec<PacketByteBuf, ShockwavePayload> CODEC =
			PacketCodec.<PacketByteBuf, ShockwavePayload>of(ShockwavePayload::write, ShockwavePayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
		buf.writeFloat(dx);
		buf.writeFloat(dy);
		buf.writeFloat(dz);
		buf.writeFloat(range);
		buf.writeInt(color);
		buf.writeVarInt(kind);
	}

	private static ShockwavePayload read(PacketByteBuf buf) {
		return new ShockwavePayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
