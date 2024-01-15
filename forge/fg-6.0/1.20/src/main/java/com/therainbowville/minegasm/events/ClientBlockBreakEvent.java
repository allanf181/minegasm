package com.therainbowville.minegasm.events;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

public class ClientBlockBreakEvent extends Event {

    private final LocalPlayer player;
    private final BlockState blockState;
    private final BlockPos blockPos;

    public ClientBlockBreakEvent(LocalPlayer player, BlockState blockState, BlockPos blockPos) {
        this.player = player;
        this.blockState = blockState;
        this.blockPos = blockPos;
    }

    public LocalPlayer getPlayer() {
        return player;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }
}
