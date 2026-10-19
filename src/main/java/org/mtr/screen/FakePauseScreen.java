package org.mtr.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.mtr.data.IGui;
import org.mtr.generated.lang.TranslationProvider;

public class FakePauseScreen extends ScreenBase implements IGui {

	private long textCooldown;
	private final String dismissPauseScreenText = TranslationProvider.GUI_MTR_DISMISS_PAUSE_SCREEN.getString();
	private final int textWidth = font.width(dismissPauseScreenText);

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		if (System.currentTimeMillis() < textCooldown) {
			context.pose().pushMatrix();
			final float newWidth = textWidth + TEXT_PADDING * 2F;
			final float newHeight = TEXT_HEIGHT + TEXT_PADDING * 2F;
			context.pose().translate(width / 2F - newWidth / 2F, height / 2F - newHeight / 2F);
			context.pose().scale(newWidth / width, newHeight / height);
			extractBackground(context, mouseX, mouseY, delta);
			context.pose().popMatrix();
			context.centeredText(Minecraft.getInstance().font, dismissPauseScreenText, width / 2, height / 2 - TEXT_HEIGHT / 2, ARGB_WHITE);
		}
		super.extractRenderState(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		Minecraft.getInstance().gui.setScreen(null);
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		textCooldown = System.currentTimeMillis() + 1000;
	}
}
