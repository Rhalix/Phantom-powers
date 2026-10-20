package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Client -> server: a click in the Phantom Menu. kind 0 = party action (value = action, text = player name),
 * 1 = story (value: 0 open, 1 restart, 2 fast travel to Amity Park), 2 = power slot (value = slot * 64 + skill, text "ghost"/"hunter").
 */
public record MenuActionPayload(int kind, int value, String text) implements CustomPayload {
	public static final CustomPayload.Id<MenuActionPayload> ID = new CustomPayload.Id<>(PhantomMod.id("menu_action"));
	public static final PacketCodec<PacketByteBuf, MenuActionPayload> CODEC = PacketCodec.<PacketByteBuf, MenuActionPayload>of(
			(p, buf) -> {
				buf.writeVarInt(p.kind);
				buf.writeVarInt(p.value);
				buf.writeString(p.text, 64);
			},
			buf -> new MenuActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readString(64)));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
