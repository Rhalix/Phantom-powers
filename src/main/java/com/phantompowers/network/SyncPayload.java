package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: the player's ghost status, for the HUD. */
public record SyncPayload(boolean powers, boolean transformed, boolean intangible, boolean possessing,
						  float energy, int points, int progress, int unlocked, int wailCooldown, int tornadoCooldown, int exhaust)
		implements CustomPayload {

	public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(PhantomMod.id("sync"));
	public static final PacketCodec<PacketByteBuf, SyncPayload> CODEC =
			PacketCodec.<PacketByteBuf, SyncPayload>of(SyncPayload::write, SyncPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(powers);
		buf.writeBoolean(transformed);
		buf.writeBoolean(intangible);
		buf.writeBoolean(possessing);
		buf.writeFloat(energy);
		buf.writeVarInt(points);
		buf.writeVarInt(progress);
		buf.writeVarInt(unlocked);
		buf.writeVarInt(wailCooldown);
		buf.writeVarInt(tornadoCooldown);
		buf.writeVarInt(exhaust);
	}

	private static SyncPayload read(PacketByteBuf buf) {
		return new SyncPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
				buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
