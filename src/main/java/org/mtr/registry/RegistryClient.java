package org.mtr.registry;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.mtr.MTR;
import org.mtr.fabric.MTRFabric;
import org.mtr.packet.CustomPacketC2S;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.packet.PacketHandler;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public final class RegistryClient {

	public static <T extends BlockEntity, U extends T, S extends BlockEntityRenderState> void registerBlockEntityRenderer(ObjectHolder<BlockEntityType<U>> blockEntityType, BlockEntityRendererProvider<T, S> factory) {
		BlockEntityRenderers.register(blockEntityType.get(), factory);
	}

	public static void registerKeyBinding(KeyMapping keyBinding) {
		KeyMappingHelper.registerKeyMapping(keyBinding);
	}

	@SafeVarargs
	public static void registerBlockColors(BlockTintSource blockTintSource, ObjectHolder<Block>... blocks) {
		BlockColorRegistry.register(List.of(blockTintSource), Arrays.stream(blocks).map(ObjectHolder::get).toArray(Block[]::new));
	}

	public static void setupPackets() {
		ClientPlayNetworking.registerGlobalReceiver(MTR.PACKET_IDENTIFIER_S2C, (customPacketS2C, context) -> PacketBufferReceiver.receive(customPacketS2C.buffer(), packetBufferReceiver -> {
			final Function<PacketBufferReceiver, ? extends PacketHandler> getInstance = MTRFabric.PACKETS.get(packetBufferReceiver.readString());
			if (getInstance != null) {
				getInstance.apply(packetBufferReceiver).runClient();
			}
		}, Minecraft.getInstance()::execute));
	}

	public static <T extends PacketHandler> void sendPacketToServer(T data) {
		final PacketBufferSender packetBufferSender = new PacketBufferSender();
		packetBufferSender.writeString(data.getClass().getName());
		data.write(packetBufferSender);
		packetBufferSender.send(bytes -> ClientPlayNetworking.send(new CustomPacketC2S(bytes)), Minecraft.getInstance()::execute);
	}
}
