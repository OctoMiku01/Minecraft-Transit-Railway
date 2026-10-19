package org.mtr.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.mtr.generated.lang.TranslationProvider;

import java.util.List;
import java.util.function.Consumer;

public class BlockStationColorSlab extends SlabBlock implements IBlockTooltip {

	public BlockStationColorSlab(BlockBehaviour.Properties settings) {
		super(settings);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag options) {
		tooltip.accept(TranslationProvider.TOOLTIP_MTR_STATION_COLOR.getMutableText().withStyle(ChatFormatting.GRAY));
	}
}
