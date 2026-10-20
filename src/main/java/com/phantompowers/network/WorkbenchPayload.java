package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Client -> server: a Hunter Workbench action (0 = paint with value as the colour, -1 = strip paint; 1 = upgrade). */
public record WorkbenchPayload(BlockPos pos, int action, int value) implements CustomPayload {
	public static final CustomPayload.Id<WorkbenchPayload> ID = new CustomPayload.Id<>(PhantomMod.id("workbench"));
	public static final PacketCodec<PacketByteBuf, WorkbenchPayload> CODEC =
			PacketCodec.<PacketByteBuf, WorkbenchPayload>of((p, buf) -> {
				buf.writeBlockPos(p.pos);
				buf.writeVarInt(p.action);
				buf.writeInt(p.value);
			}, buf -> new WorkbenchPayload(buf.readBlockPos(), buf.readVarInt(), buf.readInt()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
