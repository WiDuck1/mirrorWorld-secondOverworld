package com.erikwigdahl.mirrorworld;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MirrorStoneGameTest implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void recipeAndDimensionLoad(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().getServer().getLevel(MirrorWorld.RESOURCE_WORLD) != null,
                "Mirror World must load as a dimension");
        var recipe = helper.getLevel().getRecipeManager().byKey(MirrorWorld.id("mirror_stone"));
        helper.assertTrue(recipe.isPresent(), "Mirror Stone recipe must parse on 1.21.1");
        helper.assertTrue(recipe.orElseThrow().value().getResultItem(helper.getLevel().registryAccess())
                .is(MirrorWorld.MIRROR_STONE), "Recipe must produce a Mirror Stone");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void movingCancelsCharge(GameTestHelper helper) {
        ServerPlayer player = preparePlayer(helper);
        useStone(player);
        helper.assertTrue(isCharging(player), "Using the stone must start charging");
        player.setPos(player.position().add(0.1, 0, 0));
        MirrorStoneItem.tickCharge(player);
        helper.assertFalse(isCharging(player), "Moving must cancel charging");
        helper.assertTrue(player.getCooldowns().isOnCooldown(MirrorWorld.MIRROR_STONE),
                "Cancellation must apply the item cooldown on 1.21.1");
        helper.assertTrue(player.level().dimension().equals(Level.OVERWORLD), "Cancellation must not teleport");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void switchingItemsCancelsCharge(GameTestHelper helper) {
        ServerPlayer player = preparePlayer(helper);
        useStone(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        MirrorStoneItem.tickCharge(player);
        helper.assertFalse(isCharging(player), "Switching away from the stone must cancel charging");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 240)
    public void chargedRoundTripRemembersPositions(GameTestHelper helper) {
        ServerPlayer player = preparePlayer(helper);
        Vec3 departure = player.position();
        ServerLevel destination = helper.getLevel().getServer().getLevel(MirrorWorld.RESOURCE_WORLD);
        helper.assertTrue(destination != null, "Mirror World must exist");
        Vec3 arrival = departure.add(16, 0, 16);
        prepareLanding(destination, arrival);
        CompoundTag memories = new CompoundTag();
        CompoundTag saved = new CompoundTag();
        saved.putDouble("x", arrival.x);
        saved.putDouble("y", arrival.y);
        saved.putDouble("z", arrival.z);
        saved.putFloat("yaw", 45);
        saved.putFloat("pitch", 10);
        memories.put("resource_world", saved);
        ((AttachmentTarget) player).setAttached(MirrorWorld.TRAVEL_POSITIONS, memories);

        useStone(player);
        helper.runAfterDelay(79, () -> helper.assertTrue(player.level().dimension().equals(Level.OVERWORLD),
                "Travel must wait for the four-second charge"));
        helper.runAfterDelay(82, () -> {
            helper.assertTrue(player.level().dimension().equals(MirrorWorld.RESOURCE_WORLD),
                    "A completed charge must enter Mirror World");
            helper.assertTrue(player.position().distanceToSqr(arrival) < 0.001,
                    "Travel must use the remembered destination");
            helper.assertTrue(Math.abs(player.getYRot() - 45) < 0.01 && Math.abs(player.getXRot() - 10) < 0.01,
                    "Travel must restore the remembered viewing direction");
            helper.assertFalse(isCharging(player), "Travel must clear the visual charge state");
            helper.assertTrue(player.getCooldowns().isOnCooldown(MirrorWorld.MIRROR_STONE),
                    "Travel must apply its cooldown");
        });
        helper.runAfterDelay(125, () -> useStone(player));
        helper.runAfterDelay(208, () -> {
            helper.assertTrue(player.level().dimension().equals(Level.OVERWORLD),
                    "A second completed charge must return to the Overworld");
            helper.assertTrue(player.position().distanceToSqr(departure) < 0.001,
                    "Return travel must restore the saved departure position");
            CompoundTag positions = ((AttachmentTarget) player).getAttachedOrCreate(MirrorWorld.TRAVEL_POSITIONS);
            helper.assertTrue(positions.contains("overworld") && positions.contains("resource_world"),
                    "Both dimensions must retain their separate travel memories");
            helper.assertTrue(player.getMainHandItem().is(MirrorWorld.MIRROR_STONE),
                    "Travel must not consume the stone");
            helper.succeed();
        });
    }

    private static ServerPlayer preparePlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        prepareLanding(helper.getLevel(), position);
        player.setPos(position);
        player.setNoGravity(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MirrorWorld.MIRROR_STONE));
        return player;
    }

    private static void prepareLanding(ServerLevel level, Vec3 position) {
        BlockPos feet = BlockPos.containing(position);
        level.setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
    }

    private static void useStone(ServerPlayer player) {
        MirrorWorld.MIRROR_STONE.use(player.level(), player, InteractionHand.MAIN_HAND);
    }

    private static boolean isCharging(ServerPlayer player) {
        return ((AttachmentTarget) player).getAttachedOrElse(MirrorWorld.MIRROR_CHARGING, false);
    }
}
