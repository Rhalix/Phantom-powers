package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: your story progress (and whether to open the path picker). */
public record StoryPayload(boolean openPicker, int path, String title, String objective, int step, int steps,
		boolean hasTarget, double tx, double ty, double tz, String targetDim) implements CustomPayload {
	public static final CustomPayload.Id<StoryPayload> ID = new CustomPayload.Id<>(PhantomMod.id("story"));
	public static final PacketCodec<PacketByteBuf, StoryPayload> CODEC =
			PacketCodec.<PacketByteBuf, StoryPayload>of(StoryPayload::write, StoryPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(openPicker);
		buf.writeVarInt(path);
		buf.writeString(title, 256);
		buf.writeString(objective, 512);
		buf.writeVarInt(step);
		buf.writeVarInt(steps);
		buf.writeBoolean(hasTarget);
		buf.writeDouble(tx);
		buf.writeDouble(ty);
		buf.writeDouble(tz);
		buf.writeString(targetDim, 128);
	}

	private static StoryPayload read(PacketByteBuf buf) {
		return new StoryPayload(buf.readBoolean(), buf.readVarInt(), buf.readString(256), buf.readString(512), buf.readVarInt(), buf.readVarInt(),
				buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readString(128));
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
