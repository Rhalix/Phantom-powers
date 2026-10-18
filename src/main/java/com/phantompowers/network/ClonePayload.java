package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Client -> server: start growing a clone in the cloning pod at this position (female or male). */
public record ClonePayload(BlockPos pos, boolean female) implements CustomPayload {
	public static final CustomPayload.Id<ClonePayload> ID = new CustomPayload.Id<>(PhantomMod.id("clone"));
	public static final PacketCodec<PacketByteBuf, ClonePayload> CODEC =
			PacketCodec.<PacketByteBuf, ClonePayload>of((p, buf) -> {
				buf.writeBlockPos(p.pos);
				buf.writeBoolean(p.female);
			}, buf -> new ClonePayload(buf.readBlockPos(), buf.readBoolean()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
