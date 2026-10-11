package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: draw an Ecto Blast beam between two points. */
public record BeamPayload(float x1, float y1, float z1, float x2, float y2, float z2, int color) implements CustomPayload {
	public static final CustomPayload.Id<BeamPayload> ID = new CustomPayload.Id<>(PhantomMod.id("beam"));
	public static final PacketCodec<PacketByteBuf, BeamPayload> CODEC =
			PacketCodec.<PacketByteBuf, BeamPayload>of(BeamPayload::write, BeamPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeFloat(x1);
		buf.writeFloat(y1);
		buf.writeFloat(z1);
		buf.writeFloat(x2);
		buf.writeFloat(y2);
		buf.writeFloat(z2);
		buf.writeInt(color);
	}

	private static BeamPayload read(PacketByteBuf buf) {
		return new BeamPayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
