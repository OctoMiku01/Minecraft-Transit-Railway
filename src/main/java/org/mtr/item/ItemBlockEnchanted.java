package org.mtr.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ItemBlockEnchanted extends ItemBlockBase {

	public ItemBlockEnchanted(Block block, Item.Properties itemSettings) {
		super(block, itemSettings);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
