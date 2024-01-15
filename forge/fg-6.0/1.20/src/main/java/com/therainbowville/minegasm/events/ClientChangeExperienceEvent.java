package com.therainbowville.minegasm.events;

import net.minecraftforge.eventbus.api.Event;

public class ClientChangeExperienceEvent extends Event {
    private final int oldExperience;
    private final int newExperience;
    private final int difference;

    public ClientChangeExperienceEvent(int oldExperience, int newExperience, int difference) {
        this.oldExperience = oldExperience;
        this.newExperience = newExperience;
        this.difference = difference;
    }

    public int getOldExperience() {
        return oldExperience;
    }

    public int getNewExperience() {
        return newExperience;
    }

    public int getDifference() {
        return difference;
    }
}
