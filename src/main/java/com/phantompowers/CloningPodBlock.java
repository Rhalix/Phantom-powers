package com.phantompowers;

import com.mojang.serialization.MapCodec;
import com.phantompowers.entity.PlayerCloneEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Cloning Pod: a tall glass tube on a lab base. Use a DNA sample on it, pick whether you want a female or male
 * clone, and the tube fills with glowing ecto-fluid while your clone grows inside. It steps out as your ally.
 */
public class CloningPodBlock extends HorizontalFacingBlock {
	public static final MapCodec<CloningPodBlock> CODEC = createCodec(CloningPodBlock::new);
	public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
	private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 31, 15);
	public static final int GROW_TICKS = 140, MAX_CLONES = 3;
	/** Set by the client: opens the "female or male?" screen for a pod. */
	public static Consumer<BlockPos> openChoice = pos -> {};

	private record Job(ServerWorld world, UUID owner, String name, boolean female, boolean dark, int[] ticks) {}

	private static final Map<BlockPos, Job> JOBS = new HashMap<>();

	public CloningPodBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		// it's two blocks tall
		if (!ctx.getWorld().getBlockState(ctx.getBlockPos().up()).isReplaceable()) return null;
		return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		ItemStack held = sampleIn(player);
		if (world.isClient) {
			if (held.isOf(ModItems.DNA_SAMPLE) && !state.get(ACTIVE)) openChoice.accept(pos);
			return ActionResult.SUCCESS;
		}
		if (state.get(ACTIVE)) {
			player.sendMessage(Text.literal("A clone is already growing in this pod...").formatted(Formatting.GREEN), true);
		} else if (!held.isOf(ModItems.DNA_SAMPLE)) {
			player.sendMessage(Text.literal("Use a DNA Sample on the pod (take one with a DNA Sampler).").formatted(Formatting.YELLOW), true);
		}
		return ActionResult.CONSUME;
	}

	/** A DNA sample in either hand (main hand first). */
	private static ItemStack sampleIn(PlayerEntity p) {
		ItemStack main = p.getMainHandStack();
		return main.isOf(ModItems.DNA_SAMPLE) ? main : p.getOffHandStack();
	}

	/** Called when the player picks female or male on the screen. */
	public static void start(ServerPlayerEntity p, BlockPos pos, boolean female) {
		ServerWorld w = p.getServerWorld();
		BlockState state = w.getBlockState(pos);
		if (!(state.getBlock() instanceof CloningPodBlock) || state.get(ACTIVE) || p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64) return;
		ItemStack held = sampleIn(p);
		if (!held.isOf(ModItems.DNA_SAMPLE)) return;
		boolean dark = DnaSamplerItem.darkDna(held);
		UUID owner = DnaSamplerItem.owner(held);
		if (owner == null) owner = p.getUuid();
		String name = DnaSamplerItem.ownerName(held);
		if (name.isEmpty()) name = p.getName().getString();
		final UUID o = owner;
		int have = w.getEntitiesByClass(PlayerCloneEntity.class, p.getBoundingBox().expand(128), c -> o.equals(c.ownerId())).size();
		if (have >= MAX_CLONES) {
			p.sendMessage(Text.literal("You already have " + MAX_CLONES + " clones nearby. That's plenty!").formatted(Formatting.YELLOW), false);
			return;
		}
		held.decrementUnlessCreative(1, p);
		w.setBlockState(pos, state.with(ACTIVE, true));
		JOBS.put(pos.toImmutable(), new Job(w, owner, name, female, dark, new int[]{0}));
		w.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 1.2f, 0.6f);
		w.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);
		p.sendMessage(Text.literal("The pod fills with ecto-fluid... your " + (dark ? "evil " : "") + (female ? "female" : "male") + " ghost clone is growing!").formatted(Formatting.GREEN), false);
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<BlockPos, Job>> it = JOBS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<BlockPos, Job> en = it.next();
			BlockPos pos = en.getKey();
			Job job = en.getValue();
			ServerWorld w = job.world();
			BlockState state = w.getBlockState(pos);
			if (!(state.getBlock() instanceof CloningPodBlock)) {
				it.remove();
				continue;
			}
			int t = ++job.ticks()[0];
			Vec3d c = Vec3d.ofBottomCenter(pos);
			if (t % 3 == 0) w.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, c.x, c.y + 0.3, c.z, 3, 0.2, 0.1, 0.2, 0.05);
			if (t % 2 == 0) w.spawnParticles(GhostPowers.GREEN, c.x, c.y + 0.2 + (t % 40) / 22.0, c.z, 2, 0.2, 0.1, 0.2, 0);
			if (t % 20 == 0) w.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 1.0f, 1.0f);
			if (t % 30 == 15) w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1.9, c.z, 8, 0.3, 0.05, 0.3, 0.1);
			if (t == GROW_TICKS - 20) w.playSound(null, pos, SoundEvents.BLOCK_CONDUIT_ACTIVATE, SoundCategory.BLOCKS, 1.2f, 1.2f);
			if (t >= GROW_TICKS) {
				it.remove();
				w.setBlockState(pos, state.with(ACTIVE, false));
				Direction f = state.get(FACING);
				Vec3d out = c.add(f.getOffsetX() * 1.1, 0, f.getOffsetZ() * 1.1);
				PlayerCloneEntity clone = com.phantompowers.ModEntities.PLAYER_CLONE.create(w);
				if (clone == null) continue;
				clone.setup(job.owner(), job.name(), job.female(), job.dark());
				clone.refreshPositionAndAngles(out.x, out.y, out.z, f.asRotation(), 0);
				w.spawnEntity(clone);
				w.spawnParticles(ParticleTypes.CLOUD, c.x, c.y + 1, c.z, 30, 0.4, 0.8, 0.4, 0.05);
				w.spawnParticles(GhostPowers.GREEN, out.x, out.y + 1, out.z, 40, 0.4, 0.8, 0.4, 0.05);
				w.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1.0f, 0.6f);
				w.playSound(null, pos, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.BLOCKS, 1.0f, 1.0f);
				ServerPlayerEntity owner = w.getServer().getPlayerManager().getPlayer(job.owner());
				if (owner != null) owner.sendMessage(Text.literal("Your clone has stepped out of the pod! It will follow you and fight with you. "
						+ "(Sneak + right-click it: stay / follow)").formatted(Formatting.GREEN), false);
			}
		}
	}
}
