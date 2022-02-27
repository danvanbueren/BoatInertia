package danvb10.boatinertia.tools;

import org.bukkit.entity.Player;

public class DebugMessenger {

    Player player;
    boolean debugMode = true;

    public DebugMessenger(Player player) {
        this.player = player;
    }

    public void player(String message) {
        if(debugMode) {
            player.sendMessage("[BoatInertia-Debug] " + message);
        }
    }

    public void console(String message) {
        if(debugMode) {
            System.out.println("[BoatInertia-Debug] " + message);
        }
    }
}