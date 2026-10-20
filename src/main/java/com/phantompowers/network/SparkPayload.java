package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: 3D transformation sparks. mode 0 = burst at x/y/z, mode 1 = swirl round an entity at height fraction band. */
public record SparkPayload(int mode, int entityId, float x, float y, float z, int color, int count, float band) implements CustomPayload {
	public static final CustomPayload.Id<SparkPayload> ID = new CustomPayload.Id<>(PhantomMod.id("sparks"));
	public static final PacketCodec<PacketByteBuf, SparkPayload> CODEC =
			PacketCodec.<PacketByteBuf, SparkPayload>of(SparkPayload::write, SparkPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeByte(mode);
		buf.writeVarInt(entityId);
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
		buf.writeInt(color);
		buf.writeVarInt(count);
		buf.writeFloat(band);
	}

	private static SparkPayload read(PacketByteBuf buf) {
		return new SparkPayload(buf.readByte(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), Math.min(200, buf.readVarInt()), buf.readFloat());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
