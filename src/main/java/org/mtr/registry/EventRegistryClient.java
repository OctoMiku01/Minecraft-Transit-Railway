package org.mtr.registry;

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

//? if fabric {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
//? }

//? if neoforge {
/*import org.mtr.neoforge.MainEventBusClient;
import org.mtr.neoforge.ModEventBusClient;
*///? }

public final class EventRegistryClient {

	public static void registerStartClientTick(Runnable runnable) {
//? if fabric {
		ClientTickEvents.START_CLIENT_TICK.register(minecraftServer -> runnable.run());
//? }

//? if neoforge {
		/*MainEventBusClient.startClientTickRunnable = runnable;
//
*///? }
	}

	public static void registerEndClientTick(Runnable runnable) {
//? if fabric {
		ClientTickEvents.END_CLIENT_TICK.register(minecraftServer -> runnable.run());
//? }

//? if neoforge {
		/*MainEventBusClient.endClientTickRunnable = runnable;
//
*///? }
	}

	public static void registerStartWorldTick(Consumer<ClientLevel> consumer) {
//? if fabric {
		ClientTickEvents.START_LEVEL_TICK.register(consumer::accept);
//? }

//? if neoforge {
		/*MainEventBusClient.startWorldTickRunnable = consumer;
//
*///? }
	}

	public static void registerEndWorldTick(Consumer<ClientLevel> consumer) {
//? if fabric {
		ClientTickEvents.END_LEVEL_TICK.register(consumer::accept);
//? }

//? if neoforge {
		/*MainEventBusClient.endWorldTickRunnable = consumer;
//
*///? }
	}

	public static void registerClientJoin(Runnable runnable) {
//? if fabric {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> runnable.run());
//? }

//? if neoforge {
		/*MainEventBusClient.clientJoinRunnable = runnable;
//
*///? }
	}

	public static void registerClientDisconnect(Runnable runnable) {
//? if fabric {
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> runnable.run());
//? }

//? if neoforge {
		/*MainEventBusClient.clientDisconnectRunnable = runnable;
//
*///? }
	}

	public static void registerChunkLoad(BiConsumer<ClientLevel, ChunkAccess> consumer) {
//? if fabric {
		ClientChunkEvents.CHUNK_LOAD.register(consumer::accept);
//? }

//? if neoforge {
		/*ModEventBusClient.chunkLoadConsumer = consumer;
//
*///? }
	}

	public static void registerChunkUnload(BiConsumer<ClientLevel, ChunkAccess> consumer) {
//? if fabric {
		ClientChunkEvents.CHUNK_UNLOAD.register(consumer::accept);
//? }

//? if neoforge {
		/*ModEventBusClient.chunkUnloadConsumer = consumer;
//
*///? }
	}

	public static void registerResourceReloadEvent(Runnable runnable) {
//? if fabric {
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
//? }

//? if neoforge {
		/*ModEventBusClient.resourceReloadRunnable = runnable;
//
*///? }
	}

	public static void registerWorldRenderEvent(MTRClient.WorldRenderCallback worldRenderCallback) {
//? if fabric {
		LevelRenderEvents.COLLECT_SUBMITS.register(levelRenderContext -> worldRenderCallback.accept(levelRenderContext.poseStack(), levelRenderContext.submitNodeCollector(), levelRenderContext.levelState().cameraRenderState));
//? }

//? if neoforge {
		/*MainEventBusClient.worldRenderCallback = worldRenderCallback;
//
*///? }
	}

	public static void registerHudLayerRenderEvent(Consumer<GuiGraphicsExtractor> hudLayerRenderCallback) {
//? if fabric {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(MTR.MOD_ID, "gui"), (guiGraphics, deltaTracker) -> hudLayerRenderCallback.accept(guiGraphics));
//? }

//? if neoforge {
		/*MainEventBusClient.hudLayerRenderCallback = hudLayerRenderCallback;
//
*///? }
	}
}
