package org.mtr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockEscalatorSide extends BlockEscalatorBase {

	public BlockEscalatorSide(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.DOWN && !(world.getBlockState(pos.below()).getBlock() instanceof BlockEscalatorStep)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		}
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		// Prevents culling optimization mods from culling our see-through escalator side
		return Shapes.empty();
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.joinUnoptimized(getShape(state, world, pos, context), super.getCollisionShape(state, world, pos, context), BooleanOp.AND);
	}

	@Override
	protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		BlockPos offsetPos = pos.below();
		if (IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT) {
			offsetPos = offsetPos.relative(IBlock.getSideDirection(state));
		}
		IBlock.playerWillDestroyCreative(world, player, offsetPos);
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		final EnumEscalatorOrientation orientation = getOrientation(world, pos, state);
		final boolean isBottom = orientation == EnumEscalatorOrientation.LANDING_BOTTOM;
		final boolean isTop = orientation == EnumEscalatorOrientation.LANDING_TOP;
		final boolean isRight = IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT;
		return IBlock.getVoxelShapeByDirection(isRight ? 12 : 0, 0, isTop ? 8 : 0, isRight ? 16 : 4, 16, isBottom ? 8 : 16, IBlock.getStatePropertySafe(state, BlockStateProperties.HORIZONTAL_FACING));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
		builder.add(ORIENTATION);
		builder.add(SIDE);
	}
}
