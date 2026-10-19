package org.mtr.resource;

import net.minecraft.resources.Identifier;

@FunctionalInterface
public interface ResourceProvider {

	String get(Identifier identifier);
}
