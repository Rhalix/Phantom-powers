package com.phantompowers;

import com.phantompowers.entity.TownspersonEntity;

/** Who's dressed up for the dance: anyone dancing, and Paulina and Sam in the girls' bathroom. */
public class DanceWardrobeState {
	public static void mark(TownspersonEntity e, boolean dancing) {
		if (dancing) e.setFormalOutfit(true);
	}

	public static TownspersonEntity initialize(TownspersonEntity e, String tag) {
		if (e != null && ("phantom_bath_paulina".equals(tag) || "phantom_bath_sam".equals(tag))) e.setFormalOutfit(true);
		return e;
	}
}
