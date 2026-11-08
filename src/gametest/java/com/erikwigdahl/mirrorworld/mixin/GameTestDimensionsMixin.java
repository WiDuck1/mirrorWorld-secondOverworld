package com.erikwigdahl.mirrorworld.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps data pack dimensions that the vanilla 1.21.1 test server otherwise discards. */
@Mixin(GameTestServer.class)
public class GameTestDimensionsMixin {
    @ModifyArg(method = "method_40377", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"), index = 0)
    private static Registry<LevelStem> includeDataPackDimensions(Registry<LevelStem> empty,
            @Local(argsOnly = true) WorldLoader.DataLoadContext context) {
        return context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
    }
}
