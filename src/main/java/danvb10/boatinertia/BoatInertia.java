package danvb10.boatinertia;

import danvb10.boatinertia.listeners.BoatListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BoatInertia extends JavaPlugin {

    // Config
    public FileConfiguration config = getConfig();

    // Runnable reference
    int taskID;

    // Store active boats
    public List<InertiaManager> activeManagers = new ArrayList<>();

    // Start up
    @Override
    public void onEnable() {
        // Config
        configInit();

        // For server reloads, when players are online and might be in boats already
        loopRegisterPreviouslyActiveBoats();

        // For when players enter a boat
        getServer().getPluginManager().registerEvents(new BoatListener(this), this);

        // Every tick, manage active player boats
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this,
                () -> {
                    if (!activeManagers.isEmpty()) {
                        for (InertiaManager manager : activeManagers) {
                            manager.update();
                        }
                    }
                },
                0L,
                1
        );
    }

    // Destroy the runnable
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

    // Add all previously active boats to runnable's scope
    private void loopRegisterPreviouslyActiveBoats() {
        for (Player player : this.getServer().getOnlinePlayers()) {
            if (player.getVehicle() != null && player.getVehicle() instanceof Boat) {
                Boat boat = (Boat) player.getVehicle();
                registerBoat(boat);
            }
        }
    }

    // Add boat to runnable's scope
    public void registerBoat(Boat boat) {
        boolean foundSame = false;
        for (InertiaManager manager : activeManagers) {
            if (manager.getBoat().getEntityId() == boat.getEntityId()) {
                foundSame = true;
            }
        }
        if (!foundSame) {
            activeManagers.add(new InertiaManager(boat, this));
        }

    }

    // Remove boat from runnable's scope
    public void destructBoat(Boat boat) {
        for (int i = 0; i < activeManagers.size(); i++) {
            InertiaManager manager = activeManagers.get(i);

            if (manager.getBoat().getEntityId() == boat.getEntityId()) {
                activeManagers.remove(manager);
            }
        }
    }

    // Initialize config
    private void configInit() {
        config.addDefault("max_speed_multiplier", 1);
        config.options().copyDefaults(true);
        saveConfig();
        writeReadMe();
    }

    // Write custom readme file
    private void writeReadMe() {
        List<String> lines = Arrays.asList(
                "============= BOAT INERTIA 1.1-SNAPSHOT =============",
                "---------- https://github.com/danvanbueren ----------",
                "",
                "Thank you for installing the BoatInertia plugin! I",
                "made this in one night so it's not the world's best",
                "fluid and buoyancy simulator, but it approximates pr-",
                "etty well and gets the job done. This is a pretty si-",
                "mple plugin. It aims to give a server admin control",
                "over how fast boats can accelerate to. There is lite-",
                "rally only one config setting as of now; see below",
                "for related notes.",
                "",
                "================ GENERAL INFORMATION ================",
                "- `max_speed_multiplier` will be set to `1` by defau-",
                "lt, closely simulating Minecraft's default boat speed",
                "of `0.4D`.",
                "",
                "Have fun :)",
                "-dan"
        );
        Path file = Paths.get(this.getDataFolder().getAbsolutePath() + "/readme.txt");
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Get config
    public double getMaxSpeedConfig() {
        return config.getDouble("max_speed_multiplier");
    }
}
