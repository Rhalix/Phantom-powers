package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Server -> client: an Ecto-Shield dome is up (or just went down). Sent again every second while it's up. */
public record DomePayload(int id, float x, float y, float z, float radius, boolean up, List<BlockPos> emitters) implements CustomPayload {
	public static final CustomPayload.Id<DomePayload> ID = new CustomPayload.Id<>(PhantomMod.id("dome"));
	public static final PacketCodec<PacketByteBuf, DomePayload> CODEC =
			PacketCodec.<PacketByteBuf, DomePayload>of(DomePayload::write, DomePayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(id);
		buf.writeFloat(x);
		buf.writeFloat(y);
		buf.writeFloat(z);
		buf.writeFloat(radius);
		buf.writeBoolean(up);
		buf.writeVarInt(emitters.size());
		for (BlockPos p : emitters) buf.writeLong(p.asLong());
	}

	private static DomePayload read(PacketByteBuf buf) {
		int id = buf.readVarInt();
		float x = buf.readFloat(), y = buf.readFloat(), z = buf.readFloat(), r = buf.readFloat();
		boolean up = buf.readBoolean();
		int n = Math.min(64, buf.readVarInt());
		List<BlockPos> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(BlockPos.fromLong(buf.readLong()));
		return new DomePayload(id, x, y, z, r, up, list);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
