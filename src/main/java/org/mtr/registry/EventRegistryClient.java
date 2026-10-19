package org.mtr.registry;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.mtr.MTR;
import org.mtr.MTRClient;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class EventRegistryClient {

	public static void registerStartClientTick(Runnable runnable) {
		ClientTickEvents.START_CLIENT_TICK.register(minecraftServer -> runnable.run());
	}

	public static void registerEndClientTick(Runnable runnable) {
		ClientTickEvents.END_CLIENT_TICK.register(minecraftServer -> runnable.run());
	}

	public static void registerStartWorldTick(Consumer<ClientLevel> consumer) {
		ClientTickEvents.START_LEVEL_TICK.register(consumer::accept);
	}

	public static void registerEndWorldTick(Consumer<ClientLevel> consumer) {
		ClientTickEvents.END_LEVEL_TICK.register(consumer::accept);
	}

	public static void registerClientJoin(Runnable runnable) {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> runnable.run());
	}

	public static void registerClientDisconnect(Runnable runnable) {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> runnable.run());
	}

	public static void registerChunkLoad(BiConsumer<ClientLevel, ChunkAccess> consumer) {
		ClientChunkEvents.CHUNK_LOAD.register(consumer::accept);
	}

	public static void registerChunkUnload(BiConsumer<ClientLevel, ChunkAccess> consumer) {
		ClientChunkEvents.CHUNK_UNLOAD.register(consumer::accept);
	}

	public static void registerResourceReloadEvent(Runnable runnable) {
		final Identifier identifier = Identifier.fromNamespaceAndPath(Integer.toHexString(new Random().nextInt()), "resource");
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return identifier;
			}

			@Override
			public void onResourceManagerReload(ResourceManager manager) {
				runnable.run();
			}
		});
	}

	public static void registerWorldRenderEvent(MTRClient.WorldRenderCallback worldRenderCallback) {
		LevelRenderEvents.COLLECT_SUBMITS.register(levelRenderContext -> worldRenderCallback.accept(levelRenderContext.poseStack(), levelRenderContext.submitNodeCollector(), levelRenderContext.levelState().cameraRenderState));
	}

	/**
	 * Register callbacks for drawing directly to the frame buffers after the solid and after the translucent features of the level are drawn.
	 */
	public static void registerWorldDrawEvents(Runnable afterSolidFeatures, Runnable afterTranslucentFeatures) {
		LevelRenderEvents.AFTER_SOLID_FEATURES.register(levelRenderContext -> afterSolidFeatures.run());
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(levelRenderContext -> afterTranslucentFeatures.run());
	}

	public static void registerHudLayerRenderEvent(Consumer<GuiGraphicsExtractor> hudLayerRenderCallback) {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(MTR.MOD_ID, "gui"), (guiGraphics, deltaTracker) -> hudLayerRenderCallback.accept(guiGraphics));
	}
}
