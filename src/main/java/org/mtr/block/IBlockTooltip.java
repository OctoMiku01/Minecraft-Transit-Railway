package org.mtr.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * Blocks can no longer add tooltips directly, so {@link org.mtr.item.ItemBlockBase} forwards tooltip requests to blocks implementing this interface.
 */
public interface IBlockTooltip {

	void appendHoverText(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag options);
}
