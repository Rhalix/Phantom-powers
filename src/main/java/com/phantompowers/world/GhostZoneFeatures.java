package com.phantompowers.world;

import com.mojang.serialization.Codec;
import com.phantompowers.ModBlocks;
import com.phantompowers.PhantomMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Decorations for the Ghost Zone biomes: ice spires, dead shadow trees, crystal clusters and haunted ruins. */
public final class GhostZoneFeatures {
	private GhostZoneFeatures() {}

	public static void init() {
		Registry.register(Registries.FEATURE, PhantomMod.id("ice_spire"), new IceSpire(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, PhantomMod.id("shadow_tree"), new ShadowTree(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, PhantomMod.id("crystal_cluster"), new CrystalCluster(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, PhantomMod.id("ghost_ruin"), new GhostRuin(DefaultFeatureConfig.CODEC));
		Registry.register(Registries.FEATURE, PhantomMod.id("ecto_bloom"), new EctoBloom(DefaultFeatureConfig.CODEC));
	}

	private static final int FLAGS = Block.NOTIFY_LISTENERS;

	/** Find solid ground at or just below the origin (the heightmap puts us on top of the highest island). */
	static BlockPos ground(StructureWorldAccess w, BlockPos origin) {
		BlockPos p = origin;
		for (int i = 0; i < 6 && w.isAir(p.down()); i++) p = p.down();
		return w.isAir(p.down()) ? null : p;
	}

	static void set(StructureWorldAccess w, BlockPos p, BlockState s) {
		if (w.isAir(p) || w.getBlockState(p).isReplaceable()) w.setBlockState(p, s, FLAGS);
	}

	/** Frostbite Tundra: a jagged cone of packed and blue ice with glowing frost veins. */
	static class IceSpire extends Feature<DefaultFeatureConfig> {
		IceSpire(Codec<DefaultFeatureConfig> c) {
			super(c);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
			StructureWorldAccess w = ctx.getWorld();
			Random r = ctx.getRandom();
			BlockPos base = ground(w, ctx.getOrigin());
			if (base == null) return false;
			int h = 6 + r.nextInt(10);
			double rad = 1.6 + r.nextDouble() * 1.6;
			double lx = (r.nextDouble() - 0.5) * 0.4, lz = (r.nextDouble() - 0.5) * 0.4;
			for (int y = -1; y < h; y++) {
				double k = 1.0 - Math.max(0, y) / (double) h;
				double rr = rad * k + 0.3;
				int cx = (int) Math.round(lx * y), cz = (int) Math.round(lz * y);
				int ri = (int) Math.ceil(rr);
				for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > rr + (r.nextDouble() - 0.5) * 0.5) continue;
					BlockState s = d < rr * 0.35 ? Blocks.BLUE_ICE.getDefaultState()
							: r.nextInt(9) == 0 ? ModBlocks.FROST_ECTO.getDefaultState() : Blocks.PACKED_ICE.getDefaultState();
					set(w, base.add(cx + dx, y, cz + dz), s);
				}
			}
			// snow drifting around the foot
			for (int i = 0; i < 10; i++) {
				BlockPos p = base.add(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
				if (w.isAir(p) && !w.isAir(p.down())) w.setBlockState(p, Blocks.SNOW.getDefaultState(), FLAGS);
			}
			return true;
		}
	}

	/** Shadow Thicket: a crooked dead tree with clawing branches, cobwebs and a hanging soul lantern. */
	static class ShadowTree extends Feature<DefaultFeatureConfig> {
		ShadowTree(Codec<DefaultFeatureConfig> c) {
			super(c);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
			StructureWorldAccess w = ctx.getWorld();
			Random r = ctx.getRandom();
			BlockPos base = ground(w, ctx.getOrigin());
			if (base == null) return false;
			BlockState log = (r.nextBoolean() ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_DARK_OAK_LOG).getDefaultState();
			BlockState wood = Blocks.DARK_OAK_WOOD.getDefaultState();
			int h = 5 + r.nextInt(5);
			BlockPos p = base;
			for (int y = 0; y < h; y++) {
				set(w, p, log);
				if (y > 1 && r.nextInt(3) == 0) {
					Direction d = Direction.Type.HORIZONTAL.random(r);
					p = p.offset(d);
					set(w, p, wood);
				}
				p = p.up();
			}
			// branches clawing outward and up
			int branches = 2 + r.nextInt(3);
			for (int b = 0; b < branches; b++) {
				Direction d = Direction.Type.HORIZONTAL.random(r);
				BlockPos q = p.down(r.nextInt(3) + 1);
				int len = 2 + r.nextInt(3);
				for (int i = 0; i < len; i++) {
					q = q.offset(d);
					if (r.nextInt(2) == 0) q = q.up();
					set(w, q, wood);
				}
				if (r.nextInt(2) == 0) set(w, q.offset(d), Blocks.COBWEB.getDefaultState());
				else if (w.isAir(q.down())) w.setBlockState(q.down(), Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true), FLAGS);
			}
			for (int i = 0; i < 6; i++) {
				BlockPos g = base.add(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
				if (w.isAir(g) && !w.isAir(g.down()) && r.nextInt(3) == 0) w.setBlockState(g, Blocks.DEAD_BUSH.getDefaultState(), FLAGS);
			}
			return true;
		}
	}

	/** Crystal Spires: tall leaning shards of glowing spectral crystal and amethyst. */
	static class CrystalCluster extends Feature<DefaultFeatureConfig> {
		CrystalCluster(Codec<DefaultFeatureConfig> c) {
			super(c);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
			StructureWorldAccess w = ctx.getWorld();
			Random r = ctx.getRandom();
			BlockPos base = ground(w, ctx.getOrigin());
			if (base == null) return false;
			int shards = 3 + r.nextInt(4);
			for (int s = 0; s < shards; s++) {
				double ax = (r.nextDouble() - 0.5) * 0.9, az = (r.nextDouble() - 0.5) * 0.9;
				int len = 4 + r.nextInt(s == 0 ? 12 : 7);
				double thick = s == 0 ? 1.6 : 0.9;
				BlockPos start = base.add(r.nextInt(3) - 1, -1, r.nextInt(3) - 1);
				for (int i = 0; i < len; i++) {
					double k = 1 - i / (double) len;
					double rr = thick * k + 0.2;
					int ri = (int) Math.ceil(rr);
					for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
						if (dx * dx + dz * dz > rr * rr + 0.3) continue;
						BlockPos q = start.add((int) Math.round(ax * i) + dx, i, (int) Math.round(az * i) + dz);
						set(w, q, (i + dx + dz) % 3 == 0 ? Blocks.AMETHYST_BLOCK.getDefaultState() : ModBlocks.SPECTRAL_CRYSTAL.getDefaultState());
					}
				}
			}
			for (int i = 0; i < 5; i++) {
				BlockPos g = base.add(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
				if (w.isAir(g) && !w.isAir(g.down())) w.setBlockState(g, Blocks.AMETHYST_CLUSTER.getDefaultState(), FLAGS);
			}
			return true;
		}
	}

	/** Wailing Wastes: broken haunted pillars, crooked gravestones and soul fire. */
	static class GhostRuin extends Feature<DefaultFeatureConfig> {
		GhostRuin(Codec<DefaultFeatureConfig> c) {
			super(c);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
			StructureWorldAccess w = ctx.getWorld();
			Random r = ctx.getRandom();
			BlockPos base = ground(w, ctx.getOrigin());
			if (base == null) return false;
			BlockState[] bricks = {Blocks.STONE_BRICKS.getDefaultState(), Blocks.CRACKED_STONE_BRICKS.getDefaultState(),
					Blocks.MOSSY_STONE_BRICKS.getDefaultState(), Blocks.CHISELED_STONE_BRICKS.getDefaultState()};
			if (r.nextBoolean()) {
				int pillars = 1 + r.nextInt(3);
				for (int i = 0; i < pillars; i++) {
					BlockPos p = base.add(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
					p = ground(w, p.up(2));
					if (p == null) continue;
					int h = 2 + r.nextInt(5);
					for (int y = 0; y < h; y++) set(w, p.up(y), bricks[r.nextInt(bricks.length)]);
					if (r.nextInt(2) == 0) set(w, p.up(h), Blocks.SOUL_LANTERN.getDefaultState());
				}
			} else {
				int graves = 2 + r.nextInt(4);
				for (int i = 0; i < graves; i++) {
					BlockPos p = ground(w, base.add(r.nextInt(9) - 4, 2, r.nextInt(9) - 4));
					if (p == null) continue;
					set(w, p, Blocks.COBBLESTONE_WALL.getDefaultState());
					set(w, p.up(), Blocks.STONE_BRICK_SLAB.getDefaultState());
					if (r.nextInt(3) == 0 && w.getBlockState(p.down()).isOf(Blocks.SOUL_SOIL)) {
						BlockPos f = p.add(1, 0, 0);
						if (w.isAir(f)) w.setBlockState(f, Blocks.SOUL_FIRE.getDefaultState(), FLAGS);
					}
				}
			}
			for (int i = 0; i < 4; i++) {
				BlockPos g = ground(w, base.add(r.nextInt(9) - 4, 2, r.nextInt(9) - 4));
				if (g != null && r.nextInt(2) == 0) set(w, g, Blocks.BONE_BLOCK.getDefaultState());
			}
			return true;
		}
	}

	/** Ecto Wilds: little glowing mounds of ecto crystal and warped growth. */
	static class EctoBloom extends Feature<DefaultFeatureConfig> {
		EctoBloom(Codec<DefaultFeatureConfig> c) {
			super(c);
		}

		@Override
		public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
			StructureWorldAccess w = ctx.getWorld();
			Random r = ctx.getRandom();
			BlockPos base = ground(w, ctx.getOrigin());
			if (base == null) return false;
			for (int i = 0; i < 8; i++) {
				BlockPos g = ground(w, base.add(r.nextInt(7) - 3, 2, r.nextInt(7) - 3));
				if (g == null) continue;
				int k = r.nextInt(4);
				if (k == 0) set(w, g, Blocks.SHROOMLIGHT.getDefaultState());
				else if (k == 1) {
					set(w, g, Blocks.WARPED_STEM.getDefaultState());
					set(w, g.up(), Blocks.WARPED_WART_BLOCK.getDefaultState());
				} else set(w, g, Blocks.SEA_LANTERN.getDefaultState());
			}
			return true;
		}
	}
}
