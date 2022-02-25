package danvb10.boatinertia;

import danvb10.boatinertia.listeners.BoatListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.Charset;
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
    public List<InertiaManager> activeManagers = new ArrayList<InertiaManager>();

    // Start up
    @Override
    public void onEnable() {
        // Config
        configInit();

        // For server reloads, when players are online and might be in boats already
        loopRegisterPreviouslyActiveBoats();

        // For when players enter a boat
        BoatListener listener = new BoatListener(this);
        getServer().getPluginManager().registerEvents(new BoatListener(this), this);

        // Every tick, manage active player boats
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                this,
                new Runnable() {
                    @Override
                    public void run() {
                        if(!activeManagers.isEmpty()) {
                            for (InertiaManager manager: activeManagers) {
                                manager.update();
                            }
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
            if(player.getVehicle() != null && player.getVehicle() instanceof Boat) {
                Boat boat = (Boat) player.getVehicle();
                registerBoat(boat);
            }
        }
    }

    // Add boat to runnable's scope
    public void registerBoat(Boat boat) {
        boolean foundSame = false;
        for (int i = 0; i < activeManagers.size(); i++) {
            InertiaManager manager = activeManagers.get(i);

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

    private void configInit() {
        config.addDefault("max_speed_multiplier", 1);
        config.options().copyDefaults(true);
        saveConfig();
        writeReadMe();
    }

    private void writeReadMe() {
        List<String> lines = Arrays.asList(
                "============= BOAT INERTIA 1.0-SNAPSHOT =============",
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
                "- `max_speed_multiplier` can only accept an integer,",
                "so don't input anything other than an integer.",
                "- `max_speed_multiplier` isn't perfectly scaled with",
                "movement speed. For every whole number you add to the",
                "value, movement speed will increase roughly . Sorry",
                "for the bug.",
                "",
                "=========== MODERATELY HANDY SPEED CHART ============",
                "- `max_speed_multiplier`:`1` => `0.4D` (Normal speed)",
                "- `max_speed_multiplier`:`2` => `0.76D` (1.9x faster)",
                "- `max_speed_multiplier`:`3` => `1.12D` (2.8x faster)",
                "- `max_speed_multiplier`:`4` => `1.47D` (3.7x faster)",
                "- `max_speed_multiplier`:`5` => `1.82D` (4.6x faster)",
                "So on, so forth... and dont forget:",
                "- `max_speed_multiplier`:`20` => `7.6D` ( LIGHTSPEED)",
                "Any faster than that and the game is gonna get BUSTED",
                "",
                "Have fun :)",
                "-dan"
                );
        Path file = Paths.get(this.getDataFolder().getAbsolutePath() + "/readme.txt");
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getMaxSpeedConfig() {
        return config.getInt("max_speed_multiplier");
    }
}
