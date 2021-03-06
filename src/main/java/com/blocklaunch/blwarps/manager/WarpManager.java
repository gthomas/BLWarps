package com.blocklaunch.blwarps.manager;

import com.blocklaunch.blwarps.BLWarps;
import com.blocklaunch.blwarps.Warp;
import com.blocklaunch.blwarps.runnable.WarpPlayerRunnable;
import com.google.common.base.Optional;

import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.service.scheduler.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpManager {

    public static List<Warp> warps = new ArrayList<Warp>();
    private static Map<Player, Task> warpsInProgress = new HashMap<Player, Task>();

    private static final int TICKS_PER_SECOND = 20;

    private static final String WARP_NAME_EXISTS_MSG = "A warp with that name already exists!";
    private static final String WARP_LOCATION_EXISTS_MSG = "A warp at that location already exists!";
    private static final String WARP_NOT_EXIST = "That warp does not exist!";
    private static final String ERROR_SCHEDULING_WARP_MSG = "There was an error scheduling your warp. Please try again.";

    /**
     * Adds a warp with the passed in name and location
     * 
     * @param warpName The name of the warp
     * @param warpLocation The location of the warp
     * @return An error if the warp already exists, Optional.absent() otherwise
     */
    public static Optional<String> addWarp(Warp newWarp) {

        for (Warp warp : warps) {
            if (warp.getName().equalsIgnoreCase(newWarp.getName())) {
                // A warp with that name already exists
                return Optional.of(WARP_NAME_EXISTS_MSG);
            }
            if (warp.locationIsSame(newWarp)) {
                return Optional.of(WARP_LOCATION_EXISTS_MSG);
            }
        }

        warps.add(newWarp);

        // Save warps after putting a new one in rather than saving when server
        // shuts down to prevent loss of data if the server crashed
        saveNewWarp(newWarp);

        // No errors, return an absent optional
        return Optional.absent();

    }

    /**
     * Loads all saved warps
     */
    public static void loadWarps() {
        BLWarps.storageManager.loadWarps();
    }

    /**
     * Gets the warp with the given name
     * 
     * @param warpName The name of the warp to fetch
     * @return The corresponding warp if it exists, Optional.absent() otherwise
     */
    public static Optional<Warp> getWarp(String warpName) {
        for (Warp warp : warps) {
            if (warp.getName().equalsIgnoreCase(warpName)) {
                return Optional.of(warp);
            }
        }
        return Optional.absent();
    }

    /**
     * Deletes the warp with the provided name
     * 
     * @param warpName The name of the warp to delete
     * @return An Optional containing a potential error
     */
    public static Optional<String> deleteWarp(String warpName) {
        for (Warp warp : warps) {
            if (warp.getName().equalsIgnoreCase(warpName)) {
                warps.remove(warp);
                deleteWarp(warp);
                return Optional.absent();
            }
        }
        return Optional.of(WARP_NOT_EXIST);
    }

    /**
     * Adds the warp to the specified group (puts the group's name in the list of the warp's groups)
     * 
     * @param warp The warp to be added to the group
     * @param group The group to add the warp to
     */
    public static void addWarpToGroup(Warp warp, String group) {
        if (warp.getGroups().contains(group)) {
            return;
        }
        warp.getGroups().add(group);
        BLWarps.storageManager.updateWarp(warp);
    }

    /**
     * Removes the warp from the specified group (removes the group's name from the list of the
     * warp's groups)
     * 
     * @param warp The warp to be removed from the group
     * @param group The group to remove the warp from
     */
    public static void removeWarpFromGroup(Warp warp, String group) {
        if (!warp.getGroups().contains(group)) {
            return;
        }
        warp.getGroups().remove(group);
        BLWarps.storageManager.updateWarp(warp);
    }

    /**
     * Schedules a task to warp the specified player to the specified warp
     * 
     * @param player The player to be warped
     * @param warp The location the player is to be warped to
     * @return An error if one exists, or Optional.absent() otherwise
     */
    public static Optional<String> scheduleWarp(Player player, Warp warp) {
        long delay = BLWarps.config.getWarpDelay() * TICKS_PER_SECOND;

        // Schedule the task
        Optional<Task> optTask = BLWarps.scheduler.runTaskAfter(BLWarps.plugin, new WarpPlayerRunnable(player, warp), delay);

        if (!optTask.isPresent()) {
            // There was an error scheduling the warp
            return Optional.of(ERROR_SCHEDULING_WARP_MSG);
        }

        addWarpInProgress(player, optTask.get());

        return Optional.absent();
    }

    /**
     * Removes the specified player from the map of players who are waiting to be warped
     * 
     * @param player The player that has been teleported
     */
    public static void warpCompleted(Player player) {
        warpsInProgress.remove(player);
    }

    /**
     * Checks if the passed in player currently has a warp request
     * 
     * @param player The player to check
     * @return true if they are about to be warped, false if not.
     */
    public static boolean isWarping(Player player) {
        return warpsInProgress.containsKey(player);
    }

    /**
     * Adds the player to the map of in-progress warps
     * 
     * @param player The player to be warped
     * @param task The reference to the task to warp the player
     */
    public static void addWarpInProgress(Player player, Task task) {
        warpsInProgress.put(player, task);
    }

    /**
     * Adds a new warp and saves it
     * 
     * @param warp The warp to save
     */
    private static void saveNewWarp(Warp warp) {
        BLWarps.storageManager.saveNewWarp(warp);
    }

    /**
     * Deletes the specified warp
     * 
     * @param warp The warp to delete
     */
    private static void deleteWarp(Warp warp) {
        BLWarps.storageManager.deleteWarp(warp);
    }

}
