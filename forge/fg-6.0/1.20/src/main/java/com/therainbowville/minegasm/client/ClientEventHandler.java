package com.therainbowville.minegasm.client;

import com.therainbowville.minegasm.common.Minegasm;
import com.therainbowville.minegasm.config.ClientConfig;
import com.therainbowville.minegasm.config.MinegasmConfig;
import com.therainbowville.minegasm.events.ClientBlockBreakEvent;
import com.therainbowville.minegasm.events.ClientBreakingBlockEvent;
import com.therainbowville.minegasm.events.ClientChangeExperienceEvent;
import com.therainbowville.minegasm.events.ClientDamageEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;

import java.util.*;


@Mod.EventBusSubscriber(modid = Minegasm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandler {

    private static final long window = Minecraft.getInstance().getWindow().getWindow();

    public static boolean isFocus() {
        return GLFW.glfwGetWindowAttrib(window, 131073) != 0;
    }

    public static boolean lastFocus = isFocus();
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();
    private static final int TICKS_PER_SECOND = 20;
    private static int tickCounter = -1;
    private static int clientTickCounter = -1;
    private static final double[] state = new double[12000];
    private static boolean paused = false;
    private static UUID playerId = null;


    private static void clearState() {
        tickCounter = -1;
        clientTickCounter = -1;
        Arrays.fill(state, 0);
        paused = false;
    }

    private static int getStateCounter() {
        return tickCounter / 2;
    }

    private static int getNextStateCounter() {
        return (getStateCounter() + 1) % state.length;
    }

    private static void setState(int start, int duration, int intensity, boolean decay) {
        if (duration <= 0) {
            return;
        }

        duration = duration * 10;

        if (decay) {
            int safeDuration = Math.max(0, duration - 20);
            for (int i = 0; i < safeDuration; i++) {
                setState(start + i, intensity);
            }
            for (int i = 0; i < 20; i++) {
                setState(start + safeDuration + i, intensity * (20 - i) / 20);
            }
        } else {
            for (int i = 0; i < duration; i++) {
                setState(start + i, intensity);
            }
        }
    }

    private static void setState(int counter, int intensity) {
        boolean accumulate = false; //XXX reserved for future use
        setState(counter, intensity, accumulate);
    }

    private static void setState(int counter, int intensity, boolean accumulate) {

        int safeCounter = counter % state.length;
        if (intensity > 0){
            LOGGER.trace("SetState intensity: " + intensity + " at: [" + safeCounter + "](" + counter + ")");
        }
        if (accumulate) {
            state[safeCounter] = Math.min(1.0, state[safeCounter] + (intensity / 100.0));
        } else {
            state[safeCounter] = Math.min(1.0, Math.max(state[safeCounter], (intensity / 100.0)));
        }
    }

    private static int getIntensity(String type) {
        Map<String, Integer> normal = new HashMap<>();
        normal.put("attack", 60);
        normal.put("hurt", 0);
        normal.put("mine", 80);
        normal.put("xpChange", 100);
        normal.put("harvest", 0);
        normal.put("vitality", 0);

        Map<String, Integer> masochist = new HashMap<>();
        masochist.put("attack", 0);
        masochist.put("hurt", 100);
        masochist.put("mine", 0);
        masochist.put("xpChange", 0);
        masochist.put("harvest", 0);
        masochist.put("vitality", 10);

        Map<String, Integer> hedonist = new HashMap<>();
        hedonist.put("attack", 60);
        hedonist.put("hurt", 10);
        hedonist.put("mine", 80);
        hedonist.put("xpChange", 100);
        hedonist.put("harvest", 20);
        hedonist.put("vitality", 10);

        Map<String, Integer> custom = new HashMap<>();
        custom.put("attack", MinegasmConfig.attackIntensity);
        custom.put("hurt", MinegasmConfig.hurtIntensity);
        custom.put("mine", MinegasmConfig.mineIntensity);
        custom.put("xpChange", MinegasmConfig.xpChangeIntensity);
        custom.put("harvest", MinegasmConfig.harvestIntensity);
        custom.put("vitality", MinegasmConfig.vitalityIntensity);

        if (MinegasmConfig.mode.equals(ClientConfig.GameplayMode.MASOCHIST)) {
            return masochist.get(type);
        } else if (MinegasmConfig.mode.equals(ClientConfig.GameplayMode.HEDONIST)) {
            return hedonist.get(type);
        } else if (MinegasmConfig.mode.equals(ClientConfig.GameplayMode.CUSTOM)) {
            return custom.get(type);
        } else {
            return normal.get(type);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        try {
            if (event.phase == TickEvent.Phase.END) {
                Player player = event.player;
                UUID uuid = player.getGameProfile().getId();

                float playerHealth = player.getHealth();
                float playerFoodLevel = player.getFoodData().getFoodLevel();

                tickCounter = (tickCounter + 1) % (20 * (60 * TICKS_PER_SECOND)); // 20 min day cycle

                if (tickCounter % 2 == 0){
                    if (uuid.equals(playerId)) {
                        int stateCounter = getStateCounter();

                        if (MinegasmConfig.mode.equals(ClientConfig.GameplayMode.MASOCHIST)) {
                            if (playerHealth > 0 && playerHealth <= 1) {
                                setState(stateCounter, getIntensity("vitality"));
                            }
                        } else if (playerHealth >= 20 && playerFoodLevel >= 20) {
                            setState(stateCounter, getIntensity("vitality"));
                        }

                        if (lastFocus != isFocus() && ToyController.getDeviceName().contains("XInput")) {
                            lastFocus = isFocus();
                            LOGGER.trace("Focus changed to: " + isFocus());
                            state[stateCounter] = 0;
                        }

                        double newVibrationLevel = state[stateCounter];
                        state[stateCounter] = 0;

                        if (ToyController.currentVibrationLevel != newVibrationLevel) {
                            LOGGER.trace("Tick " + stateCounter + ": " + newVibrationLevel);
                            ToyController.setVibrationLevel(newVibrationLevel);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (tickCounter >= 0) {
                if (tickCounter != clientTickCounter) {
                    clientTickCounter = tickCounter;
                    paused = false;
                } else {
                    if (!paused) {
                        paused = true;
                        LOGGER.trace("Pausing");
                        ToyController.setVibrationLevel(0);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        try {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                UUID uuid = player.getGameProfile().getId();

                if (uuid.equals(playerId)) {
                    setState(getNextStateCounter(), 3, getIntensity("attack"), true);
                }
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    private static void processDamage(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            UUID uuid = player.getGameProfile().getId();

            if (uuid.equals(playerId)) {
                setState(getNextStateCounter(), 3, getIntensity("hurt"), true);
            }
        }
    }

    private static void processXpChange(int difference) {
        long duration = Math.round(Math.ceil(Math.log(difference + 0.5)));

        LOGGER.trace("XP CHANGE: " + difference + " duration: " + duration);

        setState(getNextStateCounter(), Math.toIntExact(duration), getIntensity("xpChange"), true);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        try {
            processDamage(event.getEntity());
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        try {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                UUID uuid = player.getGameProfile().getId();

                if (uuid.equals(playerId)) {
                    ToyController.setVibrationLevel(0);
                }
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onClientBreakingBlock(ClientBreakingBlockEvent event){
        LOGGER.trace("ClientBlockBreak: " + event.getBlockState().getBlock().toString() + " at " + event.getBlockPos().toString() + " destroy stage: " + event.getDestroyStage());

        UUID uuid = event.getPlayer().getGameProfile().getId();

        if (uuid.equals(playerId)) {
            BlockState blockState = event.getBlockState();
            Block block = blockState.getBlock();
            boolean canHarvest = ForgeHooks.isCorrectToolForDrops(blockState, event.getPlayer());

            // ToolType. AXE, HOE, PICKAXE, SHOVEL
            @SuppressWarnings("ConstantConditions") float blockHardness = block.defaultBlockState().getDestroySpeed(null, null);
            LOGGER.trace("Harvest: tool: " +
                    "?" +
                    " can harvest? " + canHarvest + " hardness: " + blockHardness);

            int minIntensity = Math.toIntExact(Math.round((getIntensity("harvest") * (Math.min(blockHardness, 20) / 20.0))));
            int maxIntensity = Math.toIntExact(Math.round((getIntensity("mine") * (Math.min(blockHardness, 20) / 20.0))));

            int intensity = minIntensity + (maxIntensity - minIntensity) * (event.getDestroyStage()+1) / 10;

            if (canHarvest) {
                setState(getNextStateCounter(),  intensity);
            }
        }
    }

    private static void processBreakEvent(BlockState blockState, Player player) {
        Block block = blockState.getBlock();
        @SuppressWarnings("ConstantConditions") float blockHardness = block.defaultBlockState().getDestroySpeed(null, null);

        LOGGER.trace("Breaking: " + block.toString());

        ItemStack mainhandItem = player.getMainHandItem();
        boolean usingAppropriateTool = mainhandItem.isCorrectToolForDrops(blockState);
        LOGGER.trace("using pickaxe: " + mainhandItem.toString() + ", using appropriate tool: " + usingAppropriateTool);

        if (usingAppropriateTool) {
            int duration = Math.max(1, Math.min(5, Math.toIntExact(Math.round(Math.ceil(Math.log(blockHardness + 0.5))))));
            int intensity = Math.toIntExact(Math.round((getIntensity("mine") * (Math.min(blockHardness, 20) / 20.0))));
            setState(getNextStateCounter(), duration, intensity, true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        try {
            Player player = event.getPlayer();
            UUID uuid = player.getGameProfile().getId();

            if (uuid.equals(playerId)) {
                processBreakEvent(event.getState(), player);
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onClientBlockBreak(ClientBlockBreakEvent event){
        LOGGER.trace("ClientBlockBreak: " + event.getBlockState().getBlock().toString() + " at " + event.getBlockPos().toString());
        try {
            Player player = event.getPlayer();
            UUID uuid = player.getGameProfile().getId();
            if (uuid.equals(playerId)) {
                processBreakEvent(event.getBlockState(), player);
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onClientDamageEvent(ClientDamageEvent event){
        try {
            processDamage(event.getEntity());
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        try {
            Player player = event.getEntity();
            UUID uuid = player.getGameProfile().getId();

            if (uuid.equals(playerId)) {
                processXpChange(event.getAmount());
            }
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onClientXpChange(ClientChangeExperienceEvent event){
        LOGGER.trace("Client XP Change from " + event.getOldExperience() + " to " + event.getNewExperience() + " (" + event.getDifference() + ")");
        if (event.getOldExperience() == 0){
            LOGGER.trace("Ignoring initial XP change");
            return;
        }
        try {
            processXpChange(event.getDifference());
        } catch (Throwable e) {
            LOGGER.throwing(e);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            LOGGER.info("Client Entered world: " + entity.toString());

            try {
                Player player = (Player) entity;
                UUID uuid = player.getGameProfile().getId();

                if (uuid.equals(Minecraft.getInstance().player.getGameProfile().getId())) {
                    LOGGER.info("Player in: " + player.getGameProfile().getName() + " " + player.getGameProfile().getId().toString());
                    clearState();
                    ToyController.setVibrationLevel(0);
                    playerId = uuid;
                }
            } catch (Throwable e) {
                LOGGER.throwing(e);
            }
        }

    }

    @SubscribeEvent
    public static void onWorldEntry(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (entity instanceof Player) {
            LOGGER.info("Player respawn world: " + entity.toString());

            try {
                Player player = (Player) entity;
                UUID uuid = player.getGameProfile().getId();

                if (uuid.equals(Minecraft.getInstance().player.getGameProfile().getId())) {
                    LOGGER.info("Player in: " + player.getGameProfile().getName() + " " + player.getGameProfile().getId().toString());
                    if (ToyController.connectDevice()) {
                        setState(getStateCounter(), 1, 5, false);
                        player.displayClientMessage(Component.literal(String.format("Connected to " + ChatFormatting.GREEN + "%s" + ChatFormatting.RESET + " [%d]", ToyController.getDeviceName(), ToyController.getDeviceId())), true);
                    } else {
                        player.displayClientMessage(Component.literal(String.format(ChatFormatting.YELLOW + "Minegasm " + ChatFormatting.RESET + "failed to start\n%s", ToyController.getLastErrorMessage())), false);
                    }
                    playerId = uuid;
                }
            } catch (Throwable e) {
                LOGGER.throwing(e);
            }
        }
    }
}

