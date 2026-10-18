package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: the colours picked in the ghost suit customizer. */
public record SuitColorsPayload(int[] colors) implements CustomPayload {
	public static final CustomPayload.Id<SuitColorsPayload> ID = new CustomPayload.Id<>(PhantomMod.id("suit_colors"));
	public static final PacketCodec<PacketByteBuf, SuitColorsPayload> CODEC =
			PacketCodec.<PacketByteBuf, SuitColorsPayload>of((p, buf) -> buf.writeIntArray(p.colors), buf -> new SuitColorsPayload(buf.readIntArray(16)));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
