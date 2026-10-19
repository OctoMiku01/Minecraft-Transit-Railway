package org.mtr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.mtr.MTRClient;
import org.mtr.client.DynamicTextureCache;
import org.mtr.client.MinecraftClientData;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.core.data.InterchangeColorsForStationName;
import org.mtr.data.ArrivalsCacheClient;
import org.mtr.data.IGui;
import org.mtr.generated.lang.TranslationProvider;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import org.mtr.model.NewOptimizedModel;
import org.mtr.registry.KeyBindings;
import org.mtr.resource.RenderStage;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Top-level coordinator for every per-frame rendering operation the mod performs.
 *
 * <p>Three responsibilities live here:</p>
 * <ol>
 *   <li><b>Tick management</b> — {@link #render(PoseStack, SubmitNodeCollector, CameraRenderState)}
 *       is the single entry point the loader (Fabric / NeoForge) wires into the world-render
 *       pipeline. It advances {@link #timerMillis}, ticks {@link DynamicTextureCache},
 *       riding state, and the arrivals cache, then dispatches into the per-domain
 *       renderers ({@link RenderVehicles}, {@link RenderLifts}, {@link RenderRails}).</li>
 *   <li><b>Batched draw scheduling</b> — code anywhere in the mod can call
 *       {@link #scheduleRender(Identifier, boolean, QueuedRenderLayer, ScheduledRender)} or
 *       {@link #renderModel(Object2ObjectOpenHashMap, StoredMatrixTransformations, int)} to
 *       enqueue draw work for the current frame. The drains at the end of
 *       {@link #render(PoseStack, SubmitNodeCollector, CameraRenderState)} sort and dispatch by
 *       {@link QueuedRenderLayer} and {@link RenderStage}.</li>
 *   <li><b>Shared frame utilities</b> — {@link #getFlashingLight()},
 *       {@link #getFlashingColor(Color, int)}, and {@link #getInterchangeRouteNames(Consumer)}
 *       hand out values derived from the current frame timer / world state, so renderers
 *       don't reinvent them.</li>
 * </ol>
 *
 * <p>The class also exposes the shared {@link #WORKER_THREAD} used for off-main-thread work
 * (occlusion culling, dynamic-texture generation, model parsing). See
 * {@link WorkerThread} for the queueing contract.</p>
 *
 * <p><b>Threading:</b> every method on this class except {@code WORKER_THREAD}'s public
 * surface must be called on the render thread. The internal {@code RENDERS} /
 * {@code CURRENT_RENDERS} / {@code MODEL_RENDERS*} maps are not thread-safe.</p>
 *
 * @see WorkerThread
 * @see QueuedRenderLayer
 * @see NewOptimizedModel
 */
public class MainRenderer {

	/**
	 * Get a continuously ticking timer for rendering, suitable for animations.
	 * Returns a value in milliseconds representing the time elapsed, incremented when
	 * {@link MainRenderer#render(PoseStack, SubmitNodeCollector, CameraRenderState)} is invoked.
	 */
	@Getter
	private static long timerMillis;
	private static long lastRenderedMillis;

	/**
	 * Shared off-main-thread worker for occlusion culling and dynamic texture generation.
	 * Submit work via {@link WorkerThread#scheduleVehicles(Consumer)} and friends, or by
	 * posting directly to {@link WorkerThread#worker} for one-shot tasks like model parsing.
	 */
	public static final WorkerThread WORKER_THREAD = new WorkerThread();

	private static final int FLASHING_INTERVAL = 1000;
	private static final int TOTAL_RENDER_STAGES = 2;
	/**
	 * Cached enum-values arrays. The render loop walks these many times per frame so we hold a single
	 * shared snapshot. See {@code docs/PERFORMANCE.md} §3.3.
	 */
	private static final QueuedRenderLayer[] QUEUED_RENDER_LAYERS = QueuedRenderLayer.values();
	/**
	 * Reusable identifier for {@link #scheduleRender(QueuedRenderLayer, ScheduledRender)} — see {@code docs/PERFORMANCE.md} §4.3.
	 */
	private static final Identifier EMPTY_IDENTIFIER = Identifier.parse("");
	private static final ObjectArrayList<ObjectArrayList<Object2ObjectArrayMap<Identifier, ObjectArrayList<ScheduledRender>>>> RENDERS = new ObjectArrayList<>(TOTAL_RENDER_STAGES);
	private static final ObjectArrayList<ObjectArrayList<Object2ObjectArrayMap<Identifier, ObjectArrayList<ScheduledRender>>>> CURRENT_RENDERS = new ObjectArrayList<>(TOTAL_RENDER_STAGES);
	private static final ObjectArrayList<ScheduledTextRender> TEXT_RENDERS = new ObjectArrayList<>(TOTAL_RENDER_STAGES);
	private static final ObjectArrayList<ScheduledTextRender> CURRENT_TEXT_RENDERS = new ObjectArrayList<>(TOTAL_RENDER_STAGES);
	private static final ObjectArrayList<ScheduledSubmit> SUBMITS = new ObjectArrayList<>();
	private static final ObjectArrayList<ScheduledSubmit> CURRENT_SUBMITS = new ObjectArrayList<>();
	private static final Object2ObjectOpenHashMap<NewOptimizedModel, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<ObjectIntImmutablePair<StoredMatrixTransformations>>>> MODEL_RENDERS = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<NewOptimizedModel, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<ObjectIntImmutablePair<StoredMatrixTransformations>>>> MODEL_RENDERS_TRANSLUCENT = new Object2ObjectOpenHashMap<>();

	static {
		for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
			final int renderStageCount = QUEUED_RENDER_LAYERS.length;
			final ObjectArrayList<Object2ObjectArrayMap<Identifier, ObjectArrayList<ScheduledRender>>> rendersList = new ObjectArrayList<>(renderStageCount);
			final ObjectArrayList<Object2ObjectArrayMap<Identifier, ObjectArrayList<ScheduledRender>>> currentRendersList = new ObjectArrayList<>(renderStageCount);

			for (int j = 0; j < renderStageCount; j++) {
				rendersList.add(j, new Object2ObjectArrayMap<>());
				currentRendersList.add(j, new Object2ObjectArrayMap<>());
			}

			RENDERS.add(i, rendersList);
			CURRENT_RENDERS.add(i, currentRendersList);
		}
	}

	/**
	 * Per-frame entry point invoked by the loader's world-render hook.
	 *
	 * <p>Advances {@link #timerMillis} by the elapsed wall-clock time, ticks the simulation
	 * mirror (vehicles, lifts, riding state, arrivals cache, dynamic texture cache), then
	 * dispatches into {@link RenderVehicles}, {@link RenderLifts} and {@link RenderRails}.
	 * Finally drains the {@code MODEL_RENDERS*} maps and the scheduled-render queues into
	 * actual draw calls grouped by render layer.</p>
	 *
	 * @param matrixStack         the current world matrix stack (caller-owned, do not retain)
	 * @param submitNodeCollector the collector receiving every draw of this frame
	 * @param camera              the camera of the world frame
	 */
	public static void render(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		final Vec3 offset = camera.pos;
		final Minecraft minecraftClient = Minecraft.getInstance();
		final ClientLevel clientWorld = minecraftClient.level;
		final LocalPlayer clientPlayerEntity = minecraftClient.player;

		if (clientWorld == null || clientPlayerEntity == null) {
			return;
		}

		final long millisElapsed = getMillisElapsed();
		timerMillis += millisElapsed;

		MinecraftClientData.getInstance().blockedRailIds.clear();
		MinecraftClientData.getInstance().vehicles.forEach(vehicle -> vehicle.simulate(millisElapsed));
		MinecraftClientData.getInstance().lifts.forEach(lift -> {
			lift.tick(millisElapsed);
			if (VehicleRidingMovement.isRiding(lift.getId()) && VehicleRidingMovement.showShiftProgressBar()) {
				clientPlayerEntity.sendOverlayMessage(TranslationProvider.GUI_MTR_PRESS_TO_SELECT_FLOOR.getText(KeyBindings.LIFT_MENU.getTranslatedKeyMessage().getString()));
			}
		});
		lastRenderedMillis = MTRClient.getGameMillis();
		WORKER_THREAD.start();
		DynamicTextureCache.instance.tick();
		// Tick the riding cool down (dismount player if they are no longer riding a vehicle) and store the player offset cache
		VehicleRidingMovement.tick();
		ArrivalsCacheClient.INSTANCE.tick();

		final Vec3 cameraShakeOffset = clientPlayerEntity.position().subtract(offset);
		RenderVehicles.render(millisElapsed, cameraShakeOffset);
		RenderLifts.render(millisElapsed, cameraShakeOffset);
		RenderRails.render(matrixStack, submitNodeCollector, offset);
		RenderPassengers.render();

		renderModel(matrixStack, submitNodeCollector, MODEL_RENDERS, offset);
		renderModel(matrixStack, submitNodeCollector, MODEL_RENDERS_TRANSLUCENT, offset);
		MODEL_RENDERS.clear();
		MODEL_RENDERS_TRANSLUCENT.clear();

		for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
			for (int j = 0; j < QUEUED_RENDER_LAYERS.length; j++) {
				CURRENT_RENDERS.get(i).get(j).clear();
				CURRENT_RENDERS.get(i).get(j).putAll(RENDERS.get(i).get(j));
				RENDERS.get(i).get(j).clear();
			}
		}

		CURRENT_TEXT_RENDERS.clear();
		CURRENT_TEXT_RENDERS.addAll(TEXT_RENDERS);
		TEXT_RENDERS.clear();

		CURRENT_SUBMITS.clear();
		CURRENT_SUBMITS.addAll(SUBMITS);
		SUBMITS.clear();

		for (int i = 0; i < TOTAL_RENDER_STAGES; i++) {
			// Priority draws go into a later submit order so they land on top of normal draws
			final OrderedSubmitNodeCollector orderedSubmitNodeCollector = submitNodeCollector.order(i);
			for (int j = 0; j < QUEUED_RENDER_LAYERS.length; j++) {
				final QueuedRenderLayer queuedRenderLayer = QUEUED_RENDER_LAYERS[j];
				CURRENT_RENDERS.get(i).get(j).forEach((key, value) -> {
					final RenderType renderLayer = switch (queuedRenderLayer) {
						case LIGHT -> MoreRenderLayers.getLight(key, false);
						case LIGHT_TRANSLUCENT -> MoreRenderLayers.getLight(key, true);
						case LIGHT_2 -> MoreRenderLayers.getLight2(key);
						case INTERIOR -> MoreRenderLayers.getInterior(key);
						case INTERIOR_TRANSLUCENT -> MoreRenderLayers.getInteriorTranslucent(key);
						case EXTERIOR -> MoreRenderLayers.getExterior(key);
						case EXTERIOR_TRANSLUCENT -> MoreRenderLayers.getExteriorTranslucent(key);
						case LINES -> RenderTypes.lines();
					};
					final ObjectArrayList<ScheduledRender> scheduledRenders = new ObjectArrayList<>(value);
					orderedSubmitNodeCollector.submitCustomGeometry(matrixStack, renderLayer, (pose, vertexConsumer) -> {
						final PoseStack newMatrixStack = createMatrixStack(pose);
						scheduledRenders.forEach(scheduledRender -> scheduledRender.accept(newMatrixStack, vertexConsumer, offset));
					});
				});
			}
		}

		final WorldDrawTarget worldDrawTarget = new WorldDrawTarget(submitNodeCollector);
		CURRENT_TEXT_RENDERS.forEach(scheduledTextRender -> scheduledTextRender.accept(matrixStack, worldDrawTarget, offset));
		worldDrawTarget.flush();
		CURRENT_SUBMITS.forEach(scheduledSubmit -> scheduledSubmit.accept(matrixStack, submitNodeCollector, camera, offset));
	}

	/**
	 * Enqueue a set of pre-built {@link NewOptimizedModel} instances to be drawn this frame
	 * at the given transformation and light level. Models are grouped by render stage —
	 * translucent stages drain after opaque stages so transparency sorts correctly.
	 *
	 * @param models                      the pre-built optimised models keyed by render stage
	 * @param storedMatrixTransformations the transform applied at draw time
	 * @param light                       packed lightmap value (block + sky in the low bits)
	 */
	public static void renderModel(Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models, StoredMatrixTransformations storedMatrixTransformations, int light) {
		models.forEach((renderStage, newOptimizedModels) -> newOptimizedModels.forEach(newOptimizedModel -> (renderStage.isTranslucent ? MODEL_RENDERS_TRANSLUCENT : MODEL_RENDERS)
			.computeIfAbsent(newOptimizedModel, key -> new Object2ObjectOpenHashMap<>())
			.computeIfAbsent(renderStage, key -> new ObjectArrayList<>())
			.add(new ObjectIntImmutablePair<>(storedMatrixTransformations, light))
		));
	}

	/**
	 * Schedule a vertex-consumer-style draw to run later this frame against the given
	 * {@link QueuedRenderLayer}. Use {@code priority = true} when the draw should land in
	 * front of normal-priority work (e.g. UI overlays drawn over world geometry).
	 *
	 * @param identifier        the texture identifier driving the render layer, or {@code null} to skip
	 * @param priority          {@code true} for the priority bucket, {@code false} for normal
	 * @param queuedRenderLayer the render-layer family to dispatch into
	 * @param scheduledRender   the draw callback invoked during the drain pass
	 */
	public static void scheduleRender(@Nullable Identifier identifier, boolean priority, QueuedRenderLayer queuedRenderLayer, ScheduledRender scheduledRender) {
		if (identifier != null) {
			RENDERS.get(priority ? 1 : 0).get(queuedRenderLayer.ordinal()).computeIfAbsent(identifier, key -> new ObjectArrayList<>()).add(scheduledRender);
		}
	}

	/**
	 * Convenience overload of
	 * {@link #scheduleRender(Identifier, boolean, QueuedRenderLayer, ScheduledRender)}
	 * for draws that aren't keyed to a specific texture (e.g. line / debug renders).
	 */
	public static void scheduleRender(QueuedRenderLayer queuedRenderLayer, ScheduledRender scheduledRender) {
		scheduleRender(EMPTY_IDENTIFIER, false, queuedRenderLayer, scheduledRender);
	}

	/**
	 * Schedule a text render to run later this frame.
	 *
	 * @param scheduledTextRender the draw callback invoked during the drain pass
	 */
	public static void scheduleTextRender(ScheduledTextRender scheduledTextRender) {
		TEXT_RENDERS.add(scheduledTextRender);
	}

	/**
	 * Schedule work that submits directly to the frame's collector later this frame, such as rendering entities.
	 *
	 * @param scheduledSubmit the callback invoked during the drain pass
	 */
	public static void scheduleSubmit(ScheduledSubmit scheduledSubmit) {
		SUBMITS.add(scheduledSubmit);
	}

	/**
	 * Drop every queued draw associated with the given identifier from both the pending
	 * and current frame buckets. Called when a dynamic texture is evicted so stale draws
	 * don't reference a disposed {@link DynamicTexture}.
	 */
	public static void cancelRender(Identifier identifier) {
		RENDERS.forEach(renderForPriority -> renderForPriority.forEach(renderForPriorityAndQueuedRenderLayer -> renderForPriorityAndQueuedRenderLayer.remove(identifier)));
		CURRENT_RENDERS.forEach(renderForPriority -> renderForPriority.forEach(renderForPriorityAndQueuedRenderLayer -> renderForPriorityAndQueuedRenderLayer.remove(identifier)));
	}

	/**
	 * Flatten interchange data for a station into the comma-joined display string used by
	 * "Interchange:" signs and PIDS rows.
	 *
	 * @param getInterchanges visitor producing {@code (connectingStationName, colors)} pairs
	 * @return the formatted interchange list, ready to render
	 */
	public static String getInterchangeRouteNames(Consumer<BiConsumer<String, InterchangeColorsForStationName>> getInterchanges) {
		final ObjectArrayList<String> interchangeRouteNames = new ObjectArrayList<>();
		getInterchanges.accept((connectingStationName, interchangeColorsForStationName) -> interchangeColorsForStationName.forEach((color, interchangeRouteNamesForColor) -> interchangeRouteNamesForColor.forEach(interchangeRouteNames::add)));
		return IGui.mergeStationsWithCommas(interchangeRouteNames);
	}

	/**
	 * @return a packed lightmap value that pulses sinusoidally between 0 and 0xF on a
	 * {@link #FLASHING_INTERVAL}-millisecond period. Suitable for signal-light and
	 * door-warning glows.
	 */
	public static int getFlashingLight() {
		final int light = (int) Math.round(((Math.sin(Math.PI * 2 * (getTimerMillis() % FLASHING_INTERVAL) / FLASHING_INTERVAL) + 1) / 2) * 0xF);
		return LightCoordsUtil.pack(light, light);
	}

	public static Color getFlashingColor(Color color, int multiplier) {
		final double flashingProgress = ((Math.sin(Math.PI * 2 * (getTimerMillis() % FLASHING_INTERVAL) / FLASHING_INTERVAL) + 1) / 2);
		return new Color(
			(int) (color.getRed() * Math.min(1, flashingProgress * multiplier)),
			(int) (color.getGreen() * Math.min(1, flashingProgress * multiplier)),
			(int) (color.getBlue() * Math.min(1, flashingProgress * multiplier))
		);
	}

	public static int getFlashingColor(int color, int multiplier) {
		return getFlashingColor(new Color(color), multiplier).getRGB();
	}

	private static void renderModel(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, Object2ObjectOpenHashMap<NewOptimizedModel, Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<ObjectIntImmutablePair<StoredMatrixTransformations>>>> modelRenders, Vec3 offset) {
		modelRenders.forEach((newOptimizedModel, modelsForRenderStage) -> modelsForRenderStage.forEach((renderStage, renderDetails) -> {
			final Identifier texture = newOptimizedModel.texture;
			final RenderType renderLayer = switch (renderStage) {
				case LIGHT -> MoreRenderLayers.getLight(texture, false);
				case ALWAYS_ON_LIGHT -> MoreRenderLayers.getLight(texture, true);
				case INTERIOR -> MoreRenderLayers.getInterior(texture);
				case INTERIOR_TRANSLUCENT -> MoreRenderLayers.getInteriorTranslucent(texture);
				case EXTERIOR -> MoreRenderLayers.getExterior(texture);
			};
			final ObjectArrayList<ObjectIntImmutablePair<StoredMatrixTransformations>> renderDetailsCopy = new ObjectArrayList<>(renderDetails);
			submitNodeCollector.submitCustomGeometry(matrixStack, renderLayer, (pose, vertexConsumer) -> renderDetailsCopy.forEach(renderDetailsEntry -> {
				final PoseStack newMatrixStack = createMatrixStack(pose);
				renderDetailsEntry.left().transform(newMatrixStack, offset);
				final int light = renderStage.isFullBrightness ? LightCoordsUtil.FULL_BRIGHT : LightCoordsUtil.pack(renderDetailsEntry.rightInt(), renderDetailsEntry.rightInt());
				newOptimizedModel.render(vertexConsumer, newMatrixStack.last(), light);
			}));
		}));
	}

	/**
	 * Submitted geometry is drawn after the submit call returns, so every deferred draw needs its own matrix stack starting from the submitted pose.
	 */
	private static PoseStack createMatrixStack(PoseStack.Pose pose) {
		final PoseStack matrixStack = new PoseStack();
		matrixStack.last().set(pose);
		return matrixStack;
	}

	private static long getMillisElapsed() {
		final long millisElapsed = MTRClient.getGameMillis() - lastRenderedMillis;
		final long gameMillisElapsed = (long) (MTRClient.getGameTimeDeltaTicks() * 50);
		return Math.abs(gameMillisElapsed - millisElapsed) < 50 ? gameMillisElapsed : millisElapsed;
	}

	@FunctionalInterface
	public interface ScheduledRender {
		/**
		 * Run the deferred draw against the given buffer and offset.
		 *
		 * @param matrixStack    the frame's matrix stack, already positioned for world space
		 * @param vertexConsumer the buffer for the resolved {@code RenderLayer}
		 * @param offset         camera-relative world offset
		 */
		void accept(PoseStack matrixStack, VertexConsumer vertexConsumer, Vec3 offset);
	}

	@FunctionalInterface
	public interface ScheduledSubmit {
		/**
		 * Submit to the frame's collector.
		 *
		 * @param matrixStack         the frame's matrix stack, already positioned for world space
		 * @param submitNodeCollector the collector of the frame
		 * @param camera              the camera of the frame
		 * @param offset              camera-relative world offset
		 */
		void accept(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, Vec3 offset);
	}

	@FunctionalInterface
	public interface ScheduledTextRender {
		/**
		 * Run the deferred text draw against the given buffer and offset.
		 *
		 * @param matrixStack the frame's matrix stack, already positioned for world space
		 * @param drawTarget  the target receiving the text
		 * @param offset      camera-relative world offset
		 */
		void accept(PoseStack matrixStack, DrawTarget drawTarget, Vec3 offset);
	}
}
