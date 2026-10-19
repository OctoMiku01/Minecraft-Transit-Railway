package org.mtr.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.mtr.data.IGui;
import org.mtr.generated.lang.TranslationProvider;

public class ReloadCustomResourcesScreen extends ScreenBase implements IGui {

	int i = 0;

	private final Runnable task;
	private final String resourceReloadingInProgressText = TranslationProvider.GUI_MTR_RESOURCE_RELOADING_IN_PROGRESS.getString();

	public ReloadCustomResourcesScreen(Runnable task) {
		this.task = task;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		extractBackground(context, mouseX, mouseY, delta);
		context.centeredText(Minecraft.getInstance().font, resourceReloadingInProgressText, width / 2, height / 2 - TEXT_HEIGHT / 2, ARGB_WHITE);
		super.extractRenderState(context, mouseX, mouseY, delta);

		if (i > 10) {
			i = -1;
			task.run();
		} else if (i < 0) {
			Minecraft.getInstance().gui.setScreen(null);
		} else {
			i++;
		}
	}
}
