package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: where a Wail Tornado is right now (drawn as a swirling funnel on the client). */
public record TornadoPayload(int id, double x, double y, double z, int age, boolean done) implements CustomPayload {
	public static final CustomPayload.Id<TornadoPayload> ID = new CustomPayload.Id<>(PhantomMod.id("tornado"));
	public static final PacketCodec<PacketByteBuf, TornadoPayload> CODEC =
			PacketCodec.<PacketByteBuf, TornadoPayload>of(TornadoPayload::write, TornadoPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(id);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeVarInt(age);
		buf.writeBoolean(done);
	}

	private static TornadoPayload read(PacketByteBuf buf) {
		return new TornadoPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readBoolean());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
