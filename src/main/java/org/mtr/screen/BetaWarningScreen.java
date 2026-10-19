package org.mtr.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.mtr.client.IDrawing;
import org.mtr.config.Config;
import org.mtr.core.tool.Utilities;
import org.mtr.data.IGui;

public class BetaWarningScreen extends ScreenBase implements IGui, Utilities {

	private final Button buttonPatreon;
	private final Button buttonYouTube;

	private static long openTime;
	private static long lastMillis;
	private static final int BUTTON_WIDTH = SQUARE_SIZE * 8;
	private static final int FORCE_OPEN_DURATION = 20000;

	public BetaWarningScreen() {
		super();
		buttonPatreon = Button.builder(Component.literal("Support this mod on Patreon"), button -> Util.getPlatform().openUri("https://www.patreon.com/minecraft_transit_railway")).build();
		buttonYouTube = Button.builder(Component.literal("Subscribe on YouTube"), button -> Util.getPlatform().openUri("https://www.youtube.com/@JonathanHo33")).build();
	}

	@Override
	protected void init() {
		super.init();
		IDrawing.setPositionAndWidth(buttonPatreon, width / 2 - SQUARE_SIZE / 2 - BUTTON_WIDTH, height - SQUARE_SIZE * 2, BUTTON_WIDTH);
		IDrawing.setPositionAndWidth(buttonYouTube, width / 2 + SQUARE_SIZE / 2, height - SQUARE_SIZE * 2, BUTTON_WIDTH);
		addRenderableWidget(buttonPatreon);
		addRenderableWidget(buttonYouTube);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
		super.extractRenderState(context, mouseX, mouseY, delta);

		context.pose().pushMatrix();
		context.pose().translate(width / 2F, SQUARE_SIZE);
		context.pose().scale(2, 2);
		context.centeredText(font, "Minecraft Transit Railway 4.1.0", 0, 0, ARGB_WHITE);
		context.pose().popMatrix();

		int i = SQUARE_SIZE * 2;
		context.centeredText(font, "Please back up your worlds before continuing!", width / 2, i += TEXT_HEIGHT, System.currentTimeMillis() % 1000 < 500 ? 0xFFFF00 : 0xFF9900);
		i = wrapAndRender(context, "- This is a very early release for Minecraft 1.21.x versions. Please report any bugs you find.", i + SQUARE_SIZE);
		i = wrapAndRender(context, "- Lift / Elevator rendering has NOT been implemented yet.", i);
		i = wrapAndRender(context, "- Some model previews in GUI screens have NOT been implemented yet.", i);
		context.centeredText(font, "Thank you and enjoy :)", width / 2, i, ARGB_WHITE);

		context.pose().pushMatrix();
		context.pose().translate(width / 2F, i + TEXT_HEIGHT + TEXT_PADDING / 2F);
		context.pose().scale(0.5F, 0.5F);
		context.centeredText(font, openTime < FORCE_OPEN_DURATION ? String.format("Please read the above carefully to continue! (%s)", (FORCE_OPEN_DURATION - openTime) / MILLIS_PER_SECOND) : "Press ESC to continue", 0, 0, ARGB_WHITE);
		context.pose().popMatrix();

		drawSprite(context, Identifier.parse("mtr/patreon"), width / 2 - BUTTON_WIDTH / 2 - SQUARE_SIZE, height - SQUARE_SIZE * 3 - TEXT_PADDING, SQUARE_SIZE, SQUARE_SIZE);
		final int youTubeIconPadding = Math.round(SQUARE_SIZE * (90F / 64 - 1) / 2);
		drawSprite(context, Identifier.parse("mtr/youtube"), width / 2 + BUTTON_WIDTH / 2 - youTubeIconPadding, height - SQUARE_SIZE * 3 - TEXT_PADDING, SQUARE_SIZE * 90 / 64, SQUARE_SIZE);
	}

	@Override
	public void tick() {
		super.tick();
		if (openTime < FORCE_OPEN_DURATION) {
			final long currentMillis = System.currentTimeMillis();
			if (lastMillis > 0) {
				openTime += currentMillis - lastMillis;
			}
			lastMillis = currentMillis;
		}
	}

	@Override
	public void onClose() {
		if (openTime >= FORCE_OPEN_DURATION) {
			Config.getClient().hideBetaWarningScreen();
			Config.save();
			super.onClose();
		}
	}

	private int wrapAndRender(GuiGraphicsExtractor context, String text, int y) {
		final int textWidth = Math.min(512, width - SQUARE_SIZE * 2);
		final int[] newY = {y};
		font.split(Component.literal(text), textWidth).forEach(orderedText -> {
			context.text(font, orderedText, (width - textWidth) / 2 + (newY[0] == y ? 0 : 10), newY[0], ARGB_LIGHT_GRAY, true);
			newY[0] += TEXT_HEIGHT + TEXT_PADDING / 2;
		});
		return newY[0] + TEXT_PADDING * 3 / 2;
	}

	public static void handle() {
		if (openTime < FORCE_OPEN_DURATION && Config.getClient().showBetaWarningScreen()) {
			final Screen screen = Minecraft.getInstance().gui.screen();
			if (screen != null && screen.getTitle().toString().contains("narrator.screen.title")) {
				Minecraft.getInstance().gui.setScreen(new BetaWarningScreen());
			}
		} else {
			openTime = FORCE_OPEN_DURATION;
		}
	}

	private static void drawSprite(GuiGraphicsExtractor context, Identifier resourceLocation, int x, int y, int width, int height) {
		context.blitSprite(RenderPipelines.GUI_TEXTURED, resourceLocation, x, y, width, height);
	}
}
