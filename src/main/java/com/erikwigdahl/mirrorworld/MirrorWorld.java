package com.erikwigdahl.mirrorworld;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.function.Supplier;

@Mod(MirrorWorld.MOD_ID)
public class MirrorWorld {
    public static final String MOD_ID = "mirrorworld";
    public static final ResourceKey<Level> RESOURCE_WORLD = ResourceKey.create(Registries.DIMENSION, id("resource_world"));
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);
    public static final Supplier<AttachmentType<CompoundTag>> TRAVEL_POSITIONS = ATTACHMENTS.register("travel_positions",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC.fieldOf("positions")).copyOnDeath().build());
    public static final Supplier<AttachmentType<Boolean>> MIRROR_CHARGING = ATTACHMENTS.register("mirror_charging",
            () -> AttachmentType.builder(() -> false).sync((holder, player) -> holder == player, ByteBufCodecs.BOOL).build());
    public static final DeferredItem<MirrorStoneItem> MIRROR_STONE = ITEMS.registerItem("mirror_stone", MirrorStoneItem::new,
            properties -> properties.stacksTo(1));

    public MirrorWorld(IEventBus modBus) {
        MirrorStoneItem.registerAttachments();
        ITEMS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(MirrorWorld::creativeTab);
        NeoForge.EVENT_BUS.addListener(MirrorWorld::tick);
    }
    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) event.accept(MIRROR_STONE.get());
    }
    private static void tick(ServerTickEvent.Post event) {
        event.getServer().getPlayerList().getPlayers().forEach(MirrorStoneItem::tickCharge);
    }
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
}
