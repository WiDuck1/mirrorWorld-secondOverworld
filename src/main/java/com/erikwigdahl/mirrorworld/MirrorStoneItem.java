package com.erikwigdahl.mirrorworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.particles.ParticleTypes;

public class MirrorStoneItem extends Item {
    private static final int CHARGE_TICKS = 80;
    // Transient state belongs to the player; it is never saved across disconnects or death.
    private static final AttachmentType<Charge> CHARGE = AttachmentRegistry.create(MirrorWorld.id("mirror_charge"));

    private record Charge(Vec3 start, ResourceKey<Level> dimension, InteractionHand hand,
                          ItemStack stack, long finishesAt) { }

    public MirrorStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        boolean returning = level.dimension().equals(MirrorWorld.RESOURCE_WORLD);
        if (!returning && !level.dimension().equals(Level.OVERWORLD)) {
            return fail(player, "wrong_dimension");
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (player.isPassenger() || player.isSleeping()) return fail(player, "busy");

        AttachmentTarget attachments = (AttachmentTarget) serverPlayer;
        if (attachments.getAttached(CHARGE) != null) return InteractionResult.SUCCESS;
        attachments.setAttached(CHARGE, new Charge(player.position(), level.dimension(), hand,
                player.getItemInHand(hand), level.getGameTime() + CHARGE_TICKS));
        attachments.setAttached(MirrorWorld.MIRROR_CHARGING, true);
        return InteractionResult.SUCCESS;
    }

    public static void tickCharge(ServerPlayer player) {
        AttachmentTarget attachments = (AttachmentTarget) player;
        Charge charge = attachments.getAttached(CHARGE);
        if (charge == null) return;
        if (!player.isAlive() || player.isPassenger() || player.isSleeping()
                || !player.level().dimension().equals(charge.dimension())
                || player.getItemInHand(charge.hand()) != charge.stack()
                || !charge.stack().is(MirrorWorld.MIRROR_STONE)
                || player.position().distanceToSqr(charge.start()) > 0.0001) {
            clearCharge(player);
            player.sendOverlayMessage(Component.translatable("message.mirrorworld.mirror_stone.cancelled"));
            // Prevent a held use button from immediately starting the charge again.
            player.getCooldowns().addCooldown(charge.stack(), 10);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(),
                4, 0.4, 0.6, 0.4, 0.1);
        if (level.getGameTime() >= charge.finishesAt()) {
            clearCharge(player);
            teleport(player, charge.stack());
        }
    }

    private static void clearCharge(ServerPlayer player) {
        AttachmentTarget attachments = (AttachmentTarget) player;
        attachments.removeAttached(CHARGE);
        attachments.setAttached(MirrorWorld.MIRROR_CHARGING, false);
    }

    private static InteractionResult teleport(ServerPlayer serverPlayer, ItemStack stack) {
        Player player = serverPlayer;
        Level level = player.level();
        boolean returning = level.dimension().equals(MirrorWorld.RESOURCE_WORLD);

        ServerLevel destination = ((ServerLevel) level).getServer().getLevel(
                returning ? Level.OVERWORLD : MirrorWorld.RESOURCE_WORLD);
        if (destination == null) return fail(player, "missing_dimension");

        AttachmentTarget attachments = (AttachmentTarget) serverPlayer;
        CompoundTag memories = attachments.getAttachedOrCreate(MirrorWorld.TRAVEL_POSITIONS).copy();
        // Preserve the Overworld return position from older compass versions.
        if (!memories.contains("overworld")) {
            CompoundTag legacy = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getCompoundOrEmpty("resource_compass_returns")
                    .getCompoundOrEmpty(player.getUUID().toString());
            if (legacy.contains("x")) memories.put("overworld", legacy.copy());
        }
        CompoundTag saved = memories.getCompoundOrEmpty(returning ? "overworld" : "resource_world");
        Vec3 target = null;
        if (saved.contains("x") && saved.contains("y") && saved.contains("z")) {
            Vec3 remembered = new Vec3(saved.getDoubleOr("x", 0), saved.getDoubleOr("y", 0), saved.getDoubleOr("z", 0));
            if (safe(destination, serverPlayer, remembered)) target = remembered;
        }
        BlockPos origin = saved.contains("x")
                ? BlockPos.containing(saved.getDoubleOr("x", 0), 0, saved.getDoubleOr("z", 0))
                : (returning ? destination.getRespawnData().pos() : player.blockPosition());
        if (target == null) target = findSurface(destination, serverPlayer, origin);
        if (target == null) return fail(player, "no_safe_position");

        Vec3 departure = player.position();
        CompoundTag location = new CompoundTag();
        location.putDouble("x", departure.x);
        location.putDouble("y", departure.y);
        location.putDouble("z", departure.z);
        location.putFloat("yaw", player.getYRot());
        location.putFloat("pitch", player.getXRot());
        float yaw = saved.getFloatOr("yaw", player.getYRot());
        float pitch = saved.getFloatOr("pitch", player.getXRot());
        ServerPlayer teleported = serverPlayer.teleport(new TeleportTransition(destination, target, Vec3.ZERO,
                yaw, pitch, TeleportTransition.PLAY_PORTAL_SOUND));
        if (teleported == null) return InteractionResult.FAIL;
        memories.put(returning ? "resource_world" : "overworld", location);
        ((AttachmentTarget) teleported).setAttached(MirrorWorld.TRAVEL_POSITIONS, memories);
        teleported.resetFallDistance();
        teleported.getCooldowns().addCooldown(stack, 40);
        teleported.sendOverlayMessage(Component.translatable(
                "message.mirrorworld.mirror_stone." + (returning ? "overworld" : "mirror_world")));
        return InteractionResult.SUCCESS;
    }

    private static Vec3 findSurface(ServerLevel level, ServerPlayer player, BlockPos origin) {
        // Bound the search to avoid generating an unbounded number of chunks on use.
        for (int radius = 0; radius <= 32; radius += 4) {
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, level.getSeaLevel(), z))) continue;
                    // getHeight does not load chunks and otherwise returns the minimum world Y.
                    level.getChunk(x >> 4, z >> 4);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    Vec3 position = new Vec3(x + 0.5, y, z + 0.5);
                    if (safe(level, player, position)) return position;
                }
            }
        }
        return null;
    }

    private static boolean safe(ServerLevel level, ServerPlayer player, Vec3 position) {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) return false;
        BlockPos feet = BlockPos.containing(position);
        if (!level.getWorldBorder().isWithinBounds(feet) || level.isOutsideBuildHeight(feet)
                || level.isOutsideBuildHeight(feet.above())) return false;
        level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
        var floor = level.getBlockState(feet.below());
        var body = player.getBoundingBox().move(position.subtract(player.position()));
        // Accept vegetation and partial-height floors (such as slabs) when the body fits.
        return !level.noCollision(player, body.move(0, -0.05, 0))
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CACTUS)
                && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()
                && !level.getBlockState(feet).is(Blocks.FIRE) && !level.getBlockState(feet).is(Blocks.SOUL_FIRE)
                && !level.getBlockState(feet).is(Blocks.POWDER_SNOW)
                && level.noCollision(player, body);
    }

    private static InteractionResult fail(Player player, String reason) {
        player.sendOverlayMessage(Component.translatable("message.mirrorworld.mirror_stone." + reason));
        return InteractionResult.FAIL;
    }
}
