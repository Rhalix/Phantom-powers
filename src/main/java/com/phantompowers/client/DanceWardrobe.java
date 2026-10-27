package com.phantompowers.client;

import com.phantompowers.DanceWardrobeState;
import com.phantompowers.PhantomMod;
import com.phantompowers.entity.TownspersonEntity;
import net.minecraft.util.Identifier;

/** Sam and Paulina in their dance outfits (model and skin). */
public class DanceWardrobe {
	public static void mark(TownspersonEntity e, boolean dancing) {
		DanceWardrobeState.mark(e, dancing);
	}

	public static TownspersonRenderer.Model select(TownspersonEntity e, TownspersonRenderer.Model normal,
			TownspersonRenderer.Model paulinaDance, TownspersonRenderer.Model samDance) {
		if (e.isFormalOutfit()) {
			if (e.variant() == TownspersonEntity.LENA) return samDance;
			if (e.variant() == TownspersonEntity.TIFFANY) return paulinaDance;
		}
		return normal;
	}

	public static Identifier texture(TownspersonEntity e, Identifier normal) {
		if (e.isFormalOutfit()) {
			if (e.variant() == TownspersonEntity.LENA) return PhantomMod.id("textures/entity/townsperson/sam_dance.png");
			if (e.variant() == TownspersonEntity.TIFFANY) return PhantomMod.id("textures/entity/townsperson/paulina_dance.png");
		}
		return normal;
	}
}
