package org.mtr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.text2speech.Narrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.mtr.MTR;
import org.mtr.config.Config;
import org.mtr.data.IGui;
import org.mtr.font.FontRenderOptions;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;

public interface IDrawing {

	float LINE_WIDTH = 2;





	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(matrixStack, vertexConsumer, x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(matrixStack, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrixStack, vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrixStack, vertexConsumer, x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Vec3 playerOffset, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(
			matrixStack, vertexConsumer,
			(float) (x1 - playerOffset.x), (float) (y1 - playerOffset.y), (float) (z1 - playerOffset.z),
			(float) (x2 - playerOffset.x), (float) (y2 - playerOffset.y), (float) (z2 - playerOffset.z),
			(float) (x3 - playerOffset.x), (float) (y3 - playerOffset.y), (float) (z3 - playerOffset.z),
			(float) (x4 - playerOffset.x), (float) (y4 - playerOffset.y), (float) (z4 - playerOffset.z),
			u1, v1, u2, v2, facing, color, light
		);
	}

	static void drawTexture(PoseStack matrixStack, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		vertexConsumer.addVertex(matrixStack.last().pose(), x1, y1, z1).setColor(color).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
		vertexConsumer.addVertex(matrixStack.last().pose(), x2, y2, z2).setColor(color).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
		vertexConsumer.addVertex(matrixStack.last().pose(), x3, y3, z3).setColor(color).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
		vertexConsumer.addVertex(matrixStack.last().pose(), x4, y4, z4).setColor(color).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
	}

	static void drawLineInWorld(PoseStack matrixStack, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
		if (new Color(color, true).getAlpha() > 0) {
			final PoseStack.Pose entry = matrixStack.last();
			final Matrix4f matrix4f = entry.pose();

			vertexConsumer.addVertex(matrix4f, x1, y1, z1).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
			vertexConsumer.addVertex(matrix4f, x2, y2, z2).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(LINE_WIDTH);
		}
	}

	static void drawSevenSegment(PoseStack matrixStack, VertexConsumer vertexConsumer, String numberString, float availableSpace, float x, float y, float height, FontRenderOptions.Alignment horizontalAlignment, int color, int light) {
		try {
			drawSevenSegment(matrixStack, vertexConsumer, Integer.parseInt(numberString), availableSpace, x, y, height, horizontalAlignment, color, light);
		} catch (Exception ignored) {
		}
	}

	static void drawSevenSegment(PoseStack matrixStack, VertexConsumer vertexConsumer, int number, float availableSpace, float x, float y, float height, FontRenderOptions.Alignment horizontalAlignment, int color, int light) {
		// Negatives and decimals are not supported right now
		final float u = 0.25F;
		final float v = 170F / 512;
		final float paddingMultiplier = 1.2F;
		final float digitWidth = height * u / v * paddingMultiplier;
		final int digits = (int) Math.floor(availableSpace / digitWidth);
		final float startX = x + horizontalAlignment.getOffset(digits * digitWidth - availableSpace);

		for (int i = 0; i < digits; i++) {
			final int digit = (number / (int) Math.pow(10, digits - i - 1)) % 10;
			final float digitX = startX + digitWidth * i;
			final float digitU = (digit % 4) * u;
			final float digitV = Math.floorDiv(digit, 4) * v;
			final float x1 = digitX + (paddingMultiplier - 1) * digitWidth / 2;
			final float x2 = x1 + digitWidth / paddingMultiplier;
			vertexConsumer.addVertex(matrixStack.last().pose(), x1, y, 0).setColor(color).setUv(digitU, digitV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			vertexConsumer.addVertex(matrixStack.last().pose(), x1, y + height, 0).setColor(color).setUv(digitU, digitV + v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			vertexConsumer.addVertex(matrixStack.last().pose(), x2, y + height, 0).setColor(color).setUv(digitU + u, digitV + v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
			vertexConsumer.addVertex(matrixStack.last().pose(), x2, y, 0).setColor(color).setUv(digitU + u, digitV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
		}
	}

	static void setPositionAndWidth(AbstractWidget widget, int x, int y, int widgetWidth) {
		widget.setX(x);
		widget.setY(y);
		widget.setWidth(Math.clamp(widgetWidth, 0, 380));
	}

	static void narrateOrAnnounce(String narrateMessage, ObjectArrayList<MutableComponent> chatMessages) {
		if (Config.getClient().getTextToSpeechAnnouncements() && !narrateMessage.isEmpty()) {
			Narrator.getNarrator().say(narrateMessage, true, 1);
		}
		if (Config.getClient().getChatAnnouncements() && !chatMessages.isEmpty()) {
			final LocalPlayer player = Minecraft.getInstance().player;
			if (player != null) {
				chatMessages.forEach(chatMessage -> {
					if (!chatMessage.getString().isEmpty()) {
						player.sendSystemMessage(chatMessage);
					}
				});
			}
		}
	}

	static MutableComponent withMTRFont(MutableComponent text) {
		return Config.getClient().getUseMTRFont() ? text.setStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath(MTR.MOD_ID, "mtr")))) : text;
	}
}
