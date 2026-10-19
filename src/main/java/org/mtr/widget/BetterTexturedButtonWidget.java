package org.mtr.widget;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import org.mtr.data.IGui;

public final class BetterTexturedButtonWidget extends ImageButton {

	private final boolean playSound;

	public BetterTexturedButtonWidget(Identifier normalTexture, Identifier highlightedTexture, OnPress onPress, boolean playSound) {
		super(0, 0, 0, IGui.SQUARE_SIZE, new WidgetSprites(normalTexture, highlightedTexture), onPress);
		this.playSound = playSound;
	}

	public BetterTexturedButtonWidget(Identifier normalTexture, Identifier highlightedTexture, Identifier disabledTexture, OnPress onPress, boolean playSound) {
		super(0, 0, 0, IGui.SQUARE_SIZE, new WidgetSprites(normalTexture, disabledTexture, highlightedTexture), onPress);
		this.playSound = playSound;
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
		if (playSound) {
			super.playDownSound(soundManager);
		}
	}
}
