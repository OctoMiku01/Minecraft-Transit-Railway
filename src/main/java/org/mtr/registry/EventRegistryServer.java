package org.mtr.registry;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class EventRegistryServer {

	public static void registerServerStarting(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STARTING.register(consumer::accept);
	}

	public static void registerServerStarted(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STARTED.register(consumer::accept);
	}

	public static void registerServerStopping(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STOPPING.register(consumer::accept);
	}

	public static void registerServerStopped(Consumer<MinecraftServer> consumer) {
		ServerLifecycleEvents.SERVER_STOPPED.register(consumer::accept);
	}

	public static void registerStartServerTick(Runnable runnable) {
		ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> runnable.run());
	}

	public static void registerEndServerTick(Runnable runnable) {
		ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> runnable.run());
	}

	public static void registerStartWorldTick(Consumer<ServerLevel> consumer) {
		ServerTickEvents.START_LEVEL_TICK.register(consumer::accept);
	}

	public static void registerEndWorldTick(Consumer<ServerLevel> consumer) {
		ServerTickEvents.END_LEVEL_TICK.register(consumer::accept);
	}

	public static void registerPlayerJoin(BiConsumer<MinecraftServer, ServerPlayer> consumer) {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> consumer.accept(server, handler.player));
	}

	public static void registerPlayerDisconnect(BiConsumer<MinecraftServer, ServerPlayer> consumer) {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> consumer.accept(server, handler.player));
	}

	public static void registerChunkLoad(BiConsumer<ServerLevel, ChunkAccess> consumer) {
		ServerChunkEvents.CHUNK_LOAD.register((serverLevel, levelChunk, generated) -> consumer.accept(serverLevel, levelChunk));
	}

	public static void registerChunkUnload(BiConsumer<ServerLevel, ChunkAccess> consumer) {
		ServerChunkEvents.CHUNK_UNLOAD.register(consumer::accept);
	}
}
