package org.mtr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class BlockCeilingAuto extends BlockCeiling {

	public static final BooleanProperty LIGHT = BooleanProperty.create("light");

	public BlockCeilingAuto(BlockBehaviour.Properties blockSettings) {
		super(blockSettings);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext itemPlacementContext) {
		final boolean facing = itemPlacementContext.getHorizontalDirection().getAxis() == Direction.Axis.X;
		return super.getStateForPlacement(itemPlacementContext).setValue(FACING, facing).setValue(LIGHT, hasLight(facing, itemPlacementContext.getClickedPos()));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random).setValue(LIGHT, hasLight(IBlock.getStatePropertySafe(state, FACING), pos));
	}

	@Override
	public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
		final boolean light = hasLight(IBlock.getStatePropertySafe(state, FACING), pos);
		if (IBlock.getStatePropertySafe(state, LIGHT) != light) {
			world.setBlockAndUpdate(pos, state.setValue(LIGHT, light));
		}
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIGHT);
	}

	private static boolean hasLight(boolean facing, BlockPos pos) {
		if (facing) {
			return pos.getZ() % 3 == 0;
		} else {
			return pos.getX() % 3 == 0;
		}
	}
}
