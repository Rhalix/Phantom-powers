package com.phantompowers;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final SoundEvent GHOSTLY_WAIL = register("ghostly_wail");
	public static final SoundEvent GHOST_SENSE = register("ghost_sense");
	public static final SoundEvent THERMOS_SUCK = register("thermos_suck");
	public static final SoundEvent GHOST_FRIENDLY = register("ghost_friendly");
	public static final SoundEvent GHOST_HOSTILE = register("ghost_hostile");
	public static final SoundEvent GHOST_HURT = register("ghost_hurt");
	public static final SoundEvent GHOST_DEATH = register("ghost_death");
	public static final SoundEvent JAILER_ROAR = register("jailer_roar");
	public static final SoundEvent GHOST_NONONO = register("ghost_nonono");
	public static final SoundEvent GUITAR_RIFF = register("guitar_riff");
	public static final SoundEvent GUITAR_SOLO = register("guitar_solo");
	public static final SoundEvent GUITAR_HIT = register("guitar_hit");
	public static final SoundEvent WAIL_TORNADO = register("wail_tornado");
	public static final SoundEvent NET_ZAP = register("net_zap");
	public static final SoundEvent KISS_SMOOCH = register("kiss_smooch");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		Identifier id = PhantomMod.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {}
}
