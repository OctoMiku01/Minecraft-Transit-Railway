package org.mtr.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockEscalatorStep extends BlockEscalatorBase {

	public static final BooleanProperty DIRECTION = BooleanProperty.create("direction");
	public static final BooleanProperty STATUS = BooleanProperty.create("status");

	public BlockEscalatorStep(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP && !(world.getBlockState(pos.above()).getBlock() instanceof BlockEscalatorSide)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
		}
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
		if (IBlock.getStatePropertySafe(state, SIDE) == EnumSide.RIGHT) {
			IBlock.playerWillDestroyCreative(world, player, pos.relative(IBlock.getSideDirection(state)));
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		final EnumEscalatorOrientation orientation = IBlock.getStatePropertySafe(state, ORIENTATION);
		if (orientation == EnumEscalatorOrientation.FLAT || orientation == EnumEscalatorOrientation.TRANSITION_BOTTOM) {
			return Block.box(0, 0, 0, 16, 15, 16);
		} else {
			return Shapes.joinUnoptimized(Block.box(1, 0, 1, 15, 16, 15), super.getCollisionShape(state, world, pos, context), BooleanOp.AND);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean intersects) {
		super.entityInside(state, world, pos, entity, effectApplier, intersects);
		final Direction facing = IBlock.getStatePropertySafe(state, BlockStateProperties.HORIZONTAL_FACING);
		final boolean direction = IBlock.getStatePropertySafe(state, DIRECTION);
		final float speed = 0.1F;

		if (IBlock.getStatePropertySafe(state, STATUS)) {
			switch (facing) {
				case NORTH:
					entity.push(0, 0, direction ? -speed : speed);
					break;
				case EAST:
					entity.push(direction ? speed : -speed, 0, 0);
					break;
				case SOUTH:
					entity.push(0, 0, direction ? speed : -speed);
					break;
				case WEST:
					entity.push(direction ? -speed : speed, 0, 0);
					break;
				default:
					break;
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final boolean direction = IBlock.getStatePropertySafe(state, DIRECTION);
			final boolean running = IBlock.getStatePropertySafe(state, STATUS);
			final Direction blockFacing = IBlock.getStatePropertySafe(state, BlockStateProperties.HORIZONTAL_FACING);
			final boolean newDirection;
			final boolean newRunning;

			if (direction && running) {
				// FORWARD to BACKWARD
				newDirection = false;
				newRunning = true;
			} else if (!direction && running) {
				// BACKWARD to STOP
				newDirection = false;
				newRunning = false;
			} else {
				// STOP to FORWARD
				newDirection = true;
				newRunning = true;
			}

			update(world, pos, blockFacing, newDirection, newRunning);
			update(world, pos, blockFacing.getOpposite(), newDirection, newRunning);

			final BlockPos sidePos = pos.relative(IBlock.getSideDirection(state));
			if (isStep(world, sidePos)) {
				final BlockEscalatorStep block = (BlockEscalatorStep) world.getBlockState(sidePos).getBlock();
				block.update(world, sidePos, blockFacing, newDirection, newRunning);
				block.update(world, sidePos, blockFacing.getOpposite(), newDirection, newRunning);
			}
		});
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.HORIZONTAL_FACING);
		builder.add(DIRECTION);
		builder.add(ORIENTATION);
		builder.add(SIDE);
		builder.add(STATUS);
	}

	private void update(Level world, BlockPos pos, Direction offset, boolean direction, boolean running) {
		world.setBlockAndUpdate(pos, world.getBlockState(pos).setValue(DIRECTION, direction).setValue(STATUS, running));
		final BlockPos offsetPos = pos.relative(offset);

		if (isStep(world, offsetPos)) {
			update(world, offsetPos, offset, direction, running);
		}
		if (isStep(world, offsetPos.above())) {
			update(world, offsetPos.above(), offset, direction, running);
		}
		if (isStep(world, offsetPos.below())) {
			update(world, offsetPos.below(), offset, direction, running);
		}
	}

	private boolean isStep(Level world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() instanceof BlockEscalatorStep;
	}
}
