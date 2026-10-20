package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: the story path the player picked. */
public record StoryChoicePayload(int path) implements CustomPayload {
	public static final CustomPayload.Id<StoryChoicePayload> ID = new CustomPayload.Id<>(PhantomMod.id("story_choice"));
	public static final PacketCodec<PacketByteBuf, StoryChoicePayload> CODEC =
			PacketCodec.<PacketByteBuf, StoryChoicePayload>of((p, buf) -> buf.writeVarInt(p.path), buf -> new StoryChoicePayload(buf.readVarInt()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
