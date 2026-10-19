package org.mtr.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.stream.LongStream;

public abstract class BlockEntityExtension extends BlockEntity {

	public BlockEntityExtension(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public final Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected final void loadAdditional(ValueInput valueInput) {
		super.loadAdditional(valueInput);
		readNbt(valueInput);
	}

	@Override
	protected final void saveAdditional(ValueOutput valueOutput) {
		super.saveAdditional(valueOutput);
		writeNbt(valueOutput);
	}

	protected void readNbt(ValueInput valueInput) {
	}

	protected void writeNbt(ValueOutput valueOutput) {
	}

	/**
	 * Long arrays have no dedicated accessor in {@link ValueInput}; {@link Codec#LONG_STREAM} reads the same long array tag that older versions wrote.
	 */
	protected static long[] readLongArray(ValueInput valueInput, String key) {
		return valueInput.read(key, Codec.LONG_STREAM).map(LongStream::toArray).orElse(new long[0]);
	}

	protected static void writeLongArray(ValueOutput valueOutput, String key, long[] values) {
		valueOutput.store(key, Codec.LONG_STREAM, LongStream.of(values));
	}
}
