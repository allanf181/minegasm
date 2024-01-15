package com.therainbowville.minegasm.mixins;

import com.therainbowville.minegasm.events.EventHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientLevel.class)
public class ClientLevelMixin {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(method = "destroyBlockProgress(ILnet/minecraft/core/BlockPos;I)V", at = @At(value = "HEAD"))
    private void onBreakingBlock(int p_104634_, BlockPos p_104635_, int p_104636_, CallbackInfo ci){
        EventHooks.onClientBreakingBlock(minecraft.player, ((ClientLevel)(Object)this).getBlockState(p_104635_), p_104635_, p_104636_);
    }
}
