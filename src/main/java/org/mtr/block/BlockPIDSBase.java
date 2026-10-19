package org.mtr.block;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.mtr.generated.lang.TranslationProvider;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.mtr.packet.PacketOpenBlockEntityScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public abstract class BlockPIDSBase extends Block implements EntityBlock, IBlockTooltip {

	public final int maxArrivals;
	public final BiPredicate<Level, BlockPos> canStoreData;
	public final BiFunction<Level, BlockPos, BlockPos> getBlockPosWithData;

	private static final Object2IntOpenHashMap<DyeColor> DYE_TO_PIDS_COLOR = new Object2IntOpenHashMap<>();

	static {
		DYE_TO_PIDS_COLOR.put(DyeColor.WHITE, 0xF9FFFE);
		DYE_TO_PIDS_COLOR.put(DyeColor.ORANGE, 0xFF9900);
		DYE_TO_PIDS_COLOR.put(DyeColor.MAGENTA, 0xEB4CDF);
		DYE_TO_PIDS_COLOR.put(DyeColor.LIGHT_BLUE, 0x4DB3F9);
		DYE_TO_PIDS_COLOR.put(DyeColor.YELLOW, 0xFED83D);
		DYE_TO_PIDS_COLOR.put(DyeColor.LIME, 0x98DB24);
		DYE_TO_PIDS_COLOR.put(DyeColor.PINK, 0xF87CC4);
		DYE_TO_PIDS_COLOR.put(DyeColor.GRAY, 0x6D7778);
		DYE_TO_PIDS_COLOR.put(DyeColor.LIGHT_GRAY, 0xA9A9A2);
		DYE_TO_PIDS_COLOR.put(DyeColor.CYAN, 0x09F6F6);
		DYE_TO_PIDS_COLOR.put(DyeColor.PURPLE, 0xBE4EFB);
		DYE_TO_PIDS_COLOR.put(DyeColor.BLUE, 0x6F79EC);
		DYE_TO_PIDS_COLOR.put(DyeColor.BROWN, 0xB3652D);
		DYE_TO_PIDS_COLOR.put(DyeColor.GREEN, 0x1FAD1F);
		DYE_TO_PIDS_COLOR.put(DyeColor.RED, 0xFF5555);
		DYE_TO_PIDS_COLOR.put(DyeColor.BLACK, 0x000000);
	}

	public BlockPIDSBase(BlockBehaviour.Properties settings, int maxArrivals, BiPredicate<Level, BlockPos> canStoreData, BiFunction<Level, BlockPos, BlockPos> getBlockPosWithData) {
		super(settings.lightLevel(blockState -> 5).noOcclusion());
		this.maxArrivals = maxArrivals;
		this.canStoreData = canStoreData;
		this.getBlockPosWithData = getBlockPosWithData;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
		final BlockPos newBlockPos = getBlockPosWithData.apply(world, pos);
		final BlockEntity entity = world.getBlockEntity(newBlockPos);

		if (entity instanceof BlockEntityBase blockEntity) {
			final InteractionResult brushResult = IBlock.checkHoldingBrush(world, player, () -> PacketOpenBlockEntityScreen.sendDirectlyToServer((ServerLevel) world, (ServerPlayer) player, newBlockPos));

			for (final InteractionHand interactionHand : InteractionHand.values()) {
				final ItemStack itemStack = player.getItemInHand(interactionHand);
				final DyeColor dyeColor = itemStack.get(DataComponents.DYE);
				if (dyeColor != null) {
					blockEntity.customColor = convertPIDSColor(dyeColor);
					blockEntity.setChanged();
					player.playSound(SoundEvents.DYE_USE);
					if (!player.isCreative()) {
						itemStack.shrink(1);
					}
					return InteractionResult.SUCCESS;
				} else if (itemStack.getItem() == Items.MILK_BUCKET) {
					blockEntity.customColor = null;
					blockEntity.setChanged();
					player.playSound(SoundEvents.BUCKET_FILL_TADPOLE);
					if (!player.isCreative()) {
						player.setItemInHand(interactionHand, new ItemStack(Items.BUCKET));
					}
					return InteractionResult.SUCCESS;
				}
			}

			return brushResult;
		}

		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag options) {
		tooltip.accept(TranslationProvider.TOOLTIP_MTR_ARRIVALS.getMutableText(maxArrivals).withStyle(ChatFormatting.GRAY));
	}

	public static abstract class BlockEntityBase extends BlockEntityExtension {

		public final int maxArrivals;
		public final BiPredicate<Level, BlockPos> canStoreData;
		public final BiFunction<Level, BlockPos, BlockPos> getBlockPosWithData;

		private final @Nullable String[] messages;
		private final boolean[] hideArrivalArray;
		@Getter
		private final LongAVLTreeSet platformIds = new LongAVLTreeSet();
		@Getter
		private int displayPage;
		@Getter
		private @Nullable Integer customColor;

		private static final String KEY_MESSAGE = "message";
		private static final String KEY_HIDE_ARRIVAL = "hide_arrival";
		private static final String KEY_PLATFORM_IDS = "platform_ids";
		private static final String KEY_DISPLAY_PAGE = "display_page";
		private static final String KEY_CUSTOM_COLOR = "custom_color";

		public BlockEntityBase(int maxArrivals, BiPredicate<Level, BlockPos> canStoreData, BiFunction<Level, BlockPos, BlockPos> getBlockPosWithData, BlockEntityType<?> type, BlockPos pos, BlockState state) {
			super(type, pos, state);
			this.maxArrivals = maxArrivals;
			this.canStoreData = canStoreData;
			this.getBlockPosWithData = getBlockPosWithData;
			messages = new String[maxArrivals];
			for (int i = 0; i < maxArrivals; i++) {
				messages[i] = "";
			}
			hideArrivalArray = new boolean[maxArrivals];
			customColor = null;
		}

		@Override
		protected void readNbt(ValueInput valueInput) {
			for (int i = 0; i < maxArrivals; i++) {
				messages[i] = valueInput.getStringOr(KEY_MESSAGE + i, "");
				hideArrivalArray[i] = valueInput.getBooleanOr(KEY_HIDE_ARRIVAL + i, false);
			}

			platformIds.clear();
			final long[] platformIdsArray = readLongArray(valueInput, KEY_PLATFORM_IDS);
			for (final long platformId : platformIdsArray) {
				platformIds.add(platformId);
			}

			displayPage = valueInput.getIntOr(KEY_DISPLAY_PAGE, 0);
			final int tempCustomColor = valueInput.getIntOr(KEY_CUSTOM_COLOR, 0);
			customColor = tempCustomColor == 0 ? null : tempCustomColor;
		}

		@Override
		protected void writeNbt(ValueOutput valueOutput) {
			for (int i = 0; i < maxArrivals; i++) {
				valueOutput.putString(KEY_MESSAGE + i, messages[i] == null ? "" : messages[i]);
				valueOutput.putBoolean(KEY_HIDE_ARRIVAL + i, hideArrivalArray[i]);
			}
			writeLongArray(valueOutput, KEY_PLATFORM_IDS, platformIds.toLongArray());
			valueOutput.putInt(KEY_DISPLAY_PAGE, displayPage);
			valueOutput.putInt(KEY_CUSTOM_COLOR, customColor == null ? 0 : customColor);
		}

		public void setData(String[] messages, boolean[] hideArrivalArray, LongAVLTreeSet platformIds, int displayPage) {
			System.arraycopy(messages, 0, this.messages, 0, Math.min(messages.length, this.messages.length));
			System.arraycopy(hideArrivalArray, 0, this.hideArrivalArray, 0, Math.min(hideArrivalArray.length, this.hideArrivalArray.length));
			final LongAVLTreeSet platformIdsCopy = new LongAVLTreeSet(platformIds);
			this.platformIds.clear();
			this.platformIds.addAll(platformIdsCopy);
			this.displayPage = displayPage;
			setChanged();
		}

		public String getMessage(int index) {
			if (index >= 0 && index < maxArrivals) {
				if (messages[index] == null) {
					messages[index] = "";
				}
				return messages[index];
			} else {
				return "";
			}
		}

		public boolean getHideArrival(int index) {
			if (index >= 0 && index < maxArrivals) {
				return hideArrivalArray[index];
			} else {
				return false;
			}
		}

		public abstract boolean showArrivalNumber();

		public abstract boolean alternateLines();

		public abstract int textColorArrived();

		public abstract int textColor();
	}

	public static int convertPIDSColor(DyeColor color) {
		return DYE_TO_PIDS_COLOR.getInt(color);
	}
}
