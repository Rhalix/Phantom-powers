package com.phantompowers.network;

import com.phantompowers.PhantomMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: the player's ghost status, for the HUD. */
public record SyncPayload(boolean powers, boolean transformed, boolean intangible, boolean possessing, boolean invisible,
						  float energy, int points, int progress, int unlocked, int wailCooldown, int tornadoCooldown, int exhaust,
						  int cutscene, boolean cutsceneToGhost, int level, int cutsceneKind,
						  boolean holding, int frostCooldown, int hostAbility, int hostId, int suitVariant,
						  int loadout, boolean dark, boolean darkMode, int acne)
		implements CustomPayload {

	public static final CustomPayload.Id<SyncPayload> ID = new CustomPayload.Id<>(PhantomMod.id("sync"));
	public static final PacketCodec<PacketByteBuf, SyncPayload> CODEC =
			PacketCodec.<PacketByteBuf, SyncPayload>of(SyncPayload::write, SyncPayload::read);

	private void write(PacketByteBuf buf) {
		buf.writeBoolean(powers);
		buf.writeBoolean(transformed);
		buf.writeBoolean(intangible);
		buf.writeBoolean(possessing);
		buf.writeBoolean(invisible);
		buf.writeFloat(energy);
		buf.writeVarInt(points);
		buf.writeVarInt(progress);
		buf.writeVarInt(unlocked);
		buf.writeVarInt(wailCooldown);
		buf.writeVarInt(tornadoCooldown);
		buf.writeVarInt(exhaust);
		buf.writeVarInt(cutscene);
		buf.writeBoolean(cutsceneToGhost);
		buf.writeVarInt(level);
		buf.writeVarInt(cutsceneKind);
		buf.writeBoolean(holding);
		buf.writeVarInt(frostCooldown);
		buf.writeVarInt(hostAbility + 1);
		buf.writeVarInt(hostId + 1);
		buf.writeVarInt(suitVariant);
		buf.writeVarInt(loadout);
		buf.writeBoolean(dark);
		buf.writeBoolean(darkMode);
		buf.writeVarInt(acne);
	}

	private static SyncPayload read(PacketByteBuf buf) {
		return new SyncPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
				buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
				buf.readBoolean(), buf.readVarInt(), buf.readVarInt() - 1, buf.readVarInt() - 1, buf.readVarInt(),
				buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt());
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
