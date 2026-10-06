package com.erikwigdahl.mirrorworld;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MirrorWorld implements ModInitializer {
	public static final String MOD_ID = "mirrorworld";
	public static final ResourceKey<Level> RESOURCE_WORLD = ResourceKey.create(Registries.DIMENSION, id("resource_world"));
	public static final AttachmentType<CompoundTag> TRAVEL_POSITIONS = AttachmentRegistry.create(
			id("travel_positions"), builder -> builder.initializer(CompoundTag::new)
					.persistent(CompoundTag.CODEC).copyOnDeath());
	public static final AttachmentType<Boolean> MIRROR_CHARGING = AttachmentRegistry.create(
			id("mirror_charging"), builder -> builder.initializer(() -> false)
					.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly()));
	public static final Item MIRROR_STONE = Registry.register(BuiltInRegistries.ITEM, id("mirror_stone"),
			new MirrorStoneItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("mirror_stone"))).stacksTo(1)));

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> entries.accept(MIRROR_STONE));
		ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers()
				.forEach(MirrorStoneItem::tickCharge));
		LOGGER.info("Resource world and compass initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
