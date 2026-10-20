package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: what's in your four Power Slots (ghost powers) and four gadget slots (hunter suit). */
public record SlotsPayload(int[] slots, int[] gadgets) implements CustomPayload {
	public static final CustomPayload.Id<SlotsPayload> ID = new CustomPayload.Id<>(PhantomMod.id("slots"));
	public static final PacketCodec<PacketByteBuf, SlotsPayload> CODEC = PacketCodec.<PacketByteBuf, SlotsPayload>of(
			(p, buf) -> {
				buf.writeIntArray(p.slots);
				buf.writeIntArray(p.gadgets);
			},
			buf -> new SlotsPayload(buf.readIntArray(8), buf.readIntArray(8)));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
