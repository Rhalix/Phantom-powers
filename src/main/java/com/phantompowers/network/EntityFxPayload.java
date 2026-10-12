package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: which entities are possessed (glowing ghost eyes) and which are frozen solid (ice). */
public record EntityFxPayload(int[] hosts, int[] frozen) implements CustomPayload {
	public static final CustomPayload.Id<EntityFxPayload> ID = new CustomPayload.Id<>(PhantomMod.id("entity_fx"));
	public static final PacketCodec<PacketByteBuf, EntityFxPayload> CODEC =
			PacketCodec.<PacketByteBuf, EntityFxPayload>of(EntityFxPayload::write, EntityFxPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeIntArray(hosts);
		buf.writeIntArray(frozen);
	}

	private static EntityFxPayload read(PacketByteBuf buf) {
		return new EntityFxPayload(buf.readIntArray(), buf.readIntArray());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
