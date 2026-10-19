package org.mtr.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.function.Supplier;

public class MoreRenderLayers {

	private static final Object2ObjectOpenHashMap<Identifier, RenderType> LIGHT_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> LIGHT_TRANSLUCENT_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> LIGHT_2_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> INTERIOR_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> INTERIOR_TRANSLUCENT_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> EXTERIOR_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Object2ObjectOpenHashMap<Identifier, RenderType> EXTERIOR_TRANSLUCENT_CACHE = new Object2ObjectOpenHashMap<>();

	public static void removeFromCache(Identifier identifier) {
		LIGHT_CACHE.remove(identifier);
		LIGHT_TRANSLUCENT_CACHE.remove(identifier);
		LIGHT_2_CACHE.remove(identifier);
		INTERIOR_CACHE.remove(identifier);
		INTERIOR_TRANSLUCENT_CACHE.remove(identifier);
		EXTERIOR_CACHE.remove(identifier);
		EXTERIOR_TRANSLUCENT_CACHE.remove(identifier);
	}

	public static RenderType getLight(Identifier texture, boolean isTranslucent) {
		return checkCache(texture, () -> RenderTypes.beaconBeam(texture, isTranslucent), isTranslucent ? LIGHT_TRANSLUCENT_CACHE : LIGHT_CACHE);
	}

	public static RenderType getLight2(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.text(texture), LIGHT_2_CACHE);
	}

	public static RenderType getInterior(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityCutoutCull(texture), INTERIOR_CACHE);
	}

	public static RenderType getInteriorTranslucent(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityTranslucentCullItemTarget(texture), INTERIOR_TRANSLUCENT_CACHE);
	}

	public static RenderType getExterior(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityCutoutCull(texture), EXTERIOR_CACHE);
	}

	public static RenderType getExteriorTranslucent(Identifier texture) {
		return checkCache(texture, () -> RenderTypes.entityTranslucentCullItemTarget(texture), EXTERIOR_TRANSLUCENT_CACHE);
	}

	private static RenderType checkCache(Identifier identifier, Supplier<RenderType> supplier, Object2ObjectOpenHashMap<Identifier, RenderType> cache) {
		if (cache.containsKey(identifier)) {
			return cache.get(identifier);
		} else {
			final RenderType renderLayer = supplier.get();
			cache.put(identifier, renderLayer);
			return renderLayer;
		}
	}
}
