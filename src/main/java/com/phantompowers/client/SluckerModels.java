package com.phantompowers.client;

import com.phantompowers.PhantomMod;
import com.phantompowers.entity.CollectorEntity;
import net.minecraft.util.Identifier;

/** When the Collector is dazed he shows his little green form; otherwise his armour (Upgraded or not). */
public final class SluckerModels {
	private SluckerModels() {}

	public static CollectorModel model(CollectorEntity e, CollectorModel armored, CollectorModel green) {
		return e.dazed() ? green : armored;
	}

	public static Identifier texture(CollectorEntity e, Identifier normal, Identifier upgraded) {
		if (e.dazed()) return PhantomMod.id("textures/entity/slucker_green.png");
		return e.upgraded() ? upgraded : normal;
	}
}
