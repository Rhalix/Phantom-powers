package com.phantompowers.client;

import com.phantompowers.entity.TownspersonEntity;

/** Which model a townsperson uses: Lena, Mr. Pemberton and Paige have their own; everyone else is player-shaped. */
public final class SamModelSelector {
	private SamModelSelector() {}

	public static TownspersonRenderer.Model select(TownspersonEntity e, TownspersonRenderer.Model wide, TownspersonRenderer.Model slim,
			TownspersonRenderer.Model sam, TownspersonRenderer.Model lancer, TownspersonRenderer.Model princess) {
		if (e.variant() == 0) return sam;
		if (e.variant() == 1) return lancer;
		if (e.variant() == 2) return princess;
		return e.isFemale() ? slim : wide;
	}
}
