package com.phantompowers.client;

import com.phantompowers.entity.TownspersonEntity;

/** Which model a townsperson uses: Sam, Mr. Lancer, the princess, Paulina and Tucker have their own; everyone else is player-shaped. */
public final class SamModelSelector {
	private SamModelSelector() {}

	public static TownspersonRenderer.Model select(TownspersonEntity e, TownspersonRenderer.Model wide, TownspersonRenderer.Model slim,
			TownspersonRenderer.Model sam, TownspersonRenderer.Model lancer, TownspersonRenderer.Model princess,
			TownspersonRenderer.Model paulina, TownspersonRenderer.Model tucker) {
		if (e.variant() == TownspersonEntity.LENA) return sam;
		if (e.variant() == TownspersonEntity.PEMBERTON) return lancer;
		if (e.variant() == TownspersonEntity.PAIGE) return princess;
		if (e.variant() == TownspersonEntity.TIFFANY) return paulina;
		if (e.variant() == TownspersonEntity.LEON) return tucker;
		return e.isFemale() ? slim : wide;
	}
}
