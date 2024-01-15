package com.therainbowville.minegasm.mixins;

import com.therainbowville.minegasm.events.EventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Shadow
    private ClientLevel level;

    @Final
    @Shadow
    private Minecraft minecraft;


    @Inject(method = "handleDamageEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onDamage(ClientboundDamageEventPacket p_270800_, CallbackInfo ci, Entity entity) {
        EventHooks.onClientDamageEntity(entity, p_270800_.getSource(level));
    }

    @Inject(method = "handleSetExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setExperienceValues(FII)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onChangeExperience(ClientboundSetExperiencePacket p_270801_, CallbackInfo ci){
        assert minecraft.player != null;
        EventHooks.onClientChangeExperience(minecraft.player.totalExperience, p_270801_.getTotalExperience());
    }

}
