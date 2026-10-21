package com.phantompowers;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** The storage inside a fridge or locker: 27 slots, like a chest. */
public class FurnitureStorageBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
	public final SimpleInventory inventory = new SimpleInventory(27) {
		@Override
		public void markDirty() {
			super.markDirty();
			FurnitureStorageBlockEntity.this.markDirty();
		}
	};

	public FurnitureStorageBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.STORAGE_FURNITURE, pos, state);
	}

	@Override
	public Text getDisplayName() {
		return getCachedState().getBlock().getName();
	}

	@Override
	public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
		return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		super.writeNbt(nbt, lookup);
		Inventories.writeNbt(nbt, inventory.getHeldStacks(), lookup);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
		super.readNbt(nbt, lookup);
		inventory.getHeldStacks().clear();
		Inventories.readNbt(nbt, inventory.getHeldStacks(), lookup);
	}
}
