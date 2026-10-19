package org.mtr.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.mtr.block.IBlockTooltip;

import java.util.function.Consumer;

public class ItemBlockBase extends BlockItem {

	public ItemBlockBase(Block block, Item.Properties itemSettings) {
		super(block, itemSettings);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag type) {
		super.appendHoverText(stack, context, tooltipDisplay, tooltip, type);
		if (getBlock() instanceof IBlockTooltip blockTooltip) {
			blockTooltip.appendHoverText(stack, context, tooltip, type);
		}
	}
}
