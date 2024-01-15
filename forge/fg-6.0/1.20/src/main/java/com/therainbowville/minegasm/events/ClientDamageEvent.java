package com.therainbowville.minegasm.events;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Event;

public class ClientDamageEvent extends Event {

    private final Entity entity;
    private final DamageSource source;

    public ClientDamageEvent(Entity entity, DamageSource source) {
        this.entity = entity;
        this.source = source;
    }

    public Entity getEntity() {
        return entity;
    }

    public DamageSource getSource() {
        return source;
    }
}
