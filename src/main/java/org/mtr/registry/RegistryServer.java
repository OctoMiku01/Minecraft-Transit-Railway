package org.mtr.registry;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.mtr.MTR;
import org.mtr.fabric.MTRFabric;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.packet.*;

import java.util.function.*;

public final class RegistryServer {

	private static final ObjectArrayList<Runnable> OBJECTS_TO_REGISTER = new ObjectArrayList<>();
	private static final Object2ObjectOpenHashMap<String, ObjectArrayList<Supplier<ItemLike>>> ITEM_GROUP_ENTRIES = new Object2ObjectOpenHashMap<>();

	public static void init() {
		OBJECTS_TO_REGISTER.forEach(Runnable::run);
	}

	public static ObjectHolder<Block> registerBlock(String registryName, Function<BlockBehaviour.Properties, Block> factory) {
		return register(BuiltInRegistries.BLOCK, Registries.BLOCK, registryName, dataRegistryKey -> factory.apply(BlockBehaviour.Properties.of()
				.setId(dataRegistryKey)
		));
	}

	public static ObjectHolder<Item> registerItem(String registryName, Function<Item.Properties, Item> factory, @Nullable String itemGroupRegistryName) {
		final ObjectHolder<Item> objectHolder = register(BuiltInRegistries.ITEM, Registries.ITEM, registryName, dataRegistryKey -> factory.apply(new Item.Properties()
				.setId(dataRegistryKey)
		));
		if (itemGroupRegistryName != null) {
			ITEM_GROUP_ENTRIES.computeIfAbsent(itemGroupRegistryName, key -> new ObjectArrayList<>()).add(objectHolder::get);
		}
		return objectHolder;
	}

	public static <T extends BlockEntity> ObjectHolder<BlockEntityType<T>> registerBlockEntityType(String registryName, BiFunction<BlockPos, BlockState, T> factory, Supplier<Block> blockSupplier) {
		return register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Registries.BLOCK_ENTITY_TYPE, registryName, dataRegistryKey -> FabricBlockEntityTypeBuilder.create(factory::apply, blockSupplier.get()).build());
	}

	public static String registerItemGroup(String registryName, Supplier<ItemStack> iconSupplier) {
		register(BuiltInRegistries.CREATIVE_MODE_TAB, Registries.CREATIVE_MODE_TAB, registryName, dataRegistryKey -> FabricCreativeModeTab.builder().icon(iconSupplier).title(Component.translatable(String.format("itemGroup.%s.%s", MTR.MOD_ID, registryName))).displayItems((displayContext, entries) -> ITEM_GROUP_ENTRIES.getOrDefault(registryName, new ObjectArrayList<>()).forEach(itemSupplier -> entries.accept(itemSupplier.get()))).build());
		return registryName;
	}

	public static ObjectHolder<SoundEvent> registerSoundEvent(String registryName, Supplier<SoundEvent> supplier) {
		return register(BuiltInRegistries.SOUND_EVENT, Registries.SOUND_EVENT, registryName, dataRegistryKey -> supplier.get());
	}

	public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> consumer) {
		CommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess, environment) -> consumer.accept(dispatcher));
	}

	public static <T> ObjectHolder<DataComponentType<T>> registerDataComponentType(String registryName, Supplier<DataComponentType<T>> supplier) {
		return register(BuiltInRegistries.DATA_COMPONENT_TYPE, Registries.DATA_COMPONENT_TYPE, registryName, dataRegistryKey -> supplier.get());
	}

	public static void setupPackets() {
		PayloadTypeRegistry.clientboundPlay().register(MTR.PACKET_IDENTIFIER_S2C, StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, CustomPacketS2C::buffer, CustomPacketS2C::new));
		PayloadTypeRegistry.serverboundPlay().register(MTR.PACKET_IDENTIFIER_C2S, StreamCodec.composite(ByteBufCodecs.BYTE_ARRAY, CustomPacketC2S::buffer, CustomPacketC2S::new));
		ServerPlayNetworking.registerGlobalReceiver(MTR.PACKET_IDENTIFIER_C2S, (customPacketC2S, context) -> PacketBufferReceiver.receive(customPacketC2S.buffer(), packetBufferReceiver -> {
			final Function<PacketBufferReceiver, ? extends PacketHandler> getInstance = MTRFabric.PACKETS.get(packetBufferReceiver.readString());
			if (getInstance != null) {
				getInstance.apply(packetBufferReceiver).runServer(context.player().level().getServer(), context.player());
			}
		}, context.player().level().getServer()::execute));
	}

	public static <T extends PacketHandler> void registerPacket(Class<T> classObject, Function<PacketBufferReceiver, T> getInstance) {
		MTRFabric.PACKETS.put(classObject.getName(), getInstance);
	}

	public static <T extends PacketHandler> void sendPacketToClient(ServerPlayer serverPlayerEntity, T data) {
		final PacketBufferSender packetBufferSender = new PacketBufferSender();
		packetBufferSender.writeString(data.getClass().getName());
		data.write(packetBufferSender);
		packetBufferSender.send(bytes -> ServerPlayNetworking.send(serverPlayerEntity, new CustomPacketS2C(bytes)), serverPlayerEntity.level().getServer()::execute);
	}

	private static <T extends U, U> ObjectHolder<T> register(Registry<U> registry, ResourceKey<Registry<U>> registryKey, String registryName, Function<ResourceKey<U>, T> factory) {
		final ResourceKey<U> dataRegistryKey = ResourceKey.create(registryKey, Identifier.fromNamespaceAndPath(MTR.MOD_ID, registryName));
		final T data = Registry.register(registry, dataRegistryKey, factory.apply(dataRegistryKey));
		final ObjectHolder<T> objectHolder = new ObjectHolder<>(() -> data);
		OBJECTS_TO_REGISTER.add(objectHolder::get);
		return objectHolder;
	}
}
