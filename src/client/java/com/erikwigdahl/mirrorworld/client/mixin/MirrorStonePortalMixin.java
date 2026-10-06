package com.erikwigdahl.mirrorworld.client.mixin;

import com.erikwigdahl.mirrorworld.MirrorWorld;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class MirrorStonePortalMixin {
    // Client-only visual portal. It never creates a Nether portal or teleports the player.
    @Unique
    private static final Portal MIRROR_VISUAL_PORTAL = new Portal() {
        @Override
        public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos position) {
            return null;
        }

        @Override
        public Transition getLocalTransition() {
            return Transition.CONFUSION;
        }
    };

    @Unique
    private PortalProcessor mirrorStoneEffect;
    @Unique
    private PortalProcessor previousPortal;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void updateMirrorStoneEffect(CallbackInfo info) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        boolean charging = player.getData(MirrorWorld.MIRROR_CHARGING);
        if (charging && player.isAlive()) {
            if (mirrorStoneEffect == null) {
                previousPortal = player.portalProcess;
                mirrorStoneEffect = new PortalProcessor(MIRROR_VISUAL_PORTAL, player.blockPosition());
            }
            player.portalProcess = mirrorStoneEffect;
            mirrorStoneEffect.setAsInsidePortalThisTick(true);
        } else if (mirrorStoneEffect != null) {
            if (player.portalProcess == mirrorStoneEffect) player.portalProcess = previousPortal;
            mirrorStoneEffect = null;
            previousPortal = null;
        }
    }
}
