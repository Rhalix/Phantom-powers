package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Client -> server: a power key was pressed. */
public record SkillPayload(int action) implements CustomPayload {
	public static final CustomPayload.Id<SkillPayload> ID = new CustomPayload.Id<>(PhantomMod.id("skill"));
	public static final PacketCodec<PacketByteBuf, SkillPayload> CODEC =
			PacketCodec.<PacketByteBuf, SkillPayload>of(SkillPayload::write, SkillPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(action);
	}

	private static SkillPayload read(PacketByteBuf buf) {
		return new SkillPayload(buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
