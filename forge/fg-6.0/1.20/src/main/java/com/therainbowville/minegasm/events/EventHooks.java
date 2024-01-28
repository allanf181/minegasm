package com.therainbowville.minegasm.events;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;

public class EventHooks {
    public static void onClientBreakBlock(LocalPlayer player, BlockState blockState, BlockPos blockPos) {
        ClientBlockBreakEvent event = new ClientBlockBreakEvent(player,blockState, blockPos);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void onClientDamageEntity(Entity entity, DamageSource source) {
        ClientDamageEvent event = new ClientDamageEvent(entity, source);
        MinecraftForge.EVENT_BUS.post(event);
    }

    public static void onClientChangeExperience(int oldExperience, int newExperience) {
        ClientChangeExperienceEvent event = new ClientChangeExperienceEvent(oldExperience, newExperience, (oldExperience - newExperience) * -1);
        if (event.getDifference() != 0) {
            MinecraftForge.EVENT_BUS.post(event);
        }
    }

    public static void onClientBreakingBlock(LocalPlayer player, BlockState blockState, BlockPos blockPos, int destroyStage) {
        ClientBreakingBlockEvent event = new ClientBreakingBlockEvent(player, blockState, blockPos, destroyStage);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
