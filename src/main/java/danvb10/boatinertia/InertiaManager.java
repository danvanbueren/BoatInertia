package danvb10.boatinertia;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class InertiaManager {

    double speedMultiplier, gravity = 0, sinkRate = 0;
    Boat boat;
    Player player;
    BoatInertia boatInertia;
    Location lastPlayerLocation;
    int multiplierReductionBuffer = 0;
    boolean falling = false;

    double maxSpeedMultiplier = 1;

    public InertiaManager(Boat boat, BoatInertia boatInertia) {
        speedMultiplier = 0;
        this.boat = boat;
        this.boatInertia = boatInertia;
        maxSpeedMultiplier = boatInertia.getMaxSpeedConfig() * 0.395;


    }

    public Boat getBoat() {
        return boat;
    }

    private double getLocationDelta() {
        if (lastPlayerLocation != null) {

            double x = lastPlayerLocation.getX() - player.getLocation().getX();
            double z = lastPlayerLocation.getZ() - player.getLocation().getZ();
            double pythag = Math.sqrt((z * z) + (x * x));
            return pythag;
        } else {
            return 0;
        }
    }

    public void update() {

        player = (Player) boat.getPassengers().get(0);
        Material targetBlock = player.getLocation().subtract(0, 0, 0).getBlock().getType();

        // kill speed if not over water
        if (!(targetBlock == Material.WATER || targetBlock == Material.KELP || targetBlock == Material.SEAGRASS || targetBlock == Material.TALL_SEAGRASS || targetBlock == Material.AIR)) {
            speedMultiplier = 0.01;
        }

        // if over air, turn on gravity lol
        if (targetBlock == Material.AIR) {
            gravity += 0.025;
            sinkRate += gravity;
            speedMultiplier /= 1.2;
            falling = true;
        } else {
            gravity = 0;
            sinkRate = 0;
            falling = false;

            double currentY = player.getLocation().getY();
            int truncY = (int) currentY;
            double deltaY = currentY - truncY;

            if (deltaY > 0.08) {
                sinkRate = 0.1;
            } else {
                sinkRate = 0;
            }
        }

        // if player trying to move forward
        if (player.getVelocity().length() > 0.08 || falling) {
            //the player is trying to move

            if (multiplierReductionBuffer > 10) {
                speedMultiplier = 0.01;
                multiplierReductionBuffer = 0;
                if (falling) {
                    gravity = 0;
                    sinkRate = 0;
                }
            }

            if (getLocationDelta() < 0.04) {
                multiplierReductionBuffer++;
            } else {
                if (multiplierReductionBuffer > 0) {
                    multiplierReductionBuffer--;
                }
            }

            if (getLocationDelta() < 0.4) {
                if (speedMultiplier > getLocationDelta()) {
                    speedMultiplier = (getLocationDelta() + speedMultiplier) / 2;
                }
            }

            if (speedMultiplier < maxSpeedMultiplier) {
                speedMultiplier += 0.01;
            }

        } else {
            if (speedMultiplier > 0) {
                if (speedMultiplier > 1) {
                    speedMultiplier = speedMultiplier / 1.2;
                } else if (speedMultiplier > 0.5) {
                    speedMultiplier -= 0.07;
                } else if (speedMultiplier > 0.2) {
                    speedMultiplier -= 0.02;
                } else {
                    speedMultiplier -= 0.007;
                }

                if (speedMultiplier < 0) {
                    speedMultiplier = 0;
                }
            }
        }

        // set vector based on boat direction
        double pitch = ((boat.getLocation().getPitch() + 90) * Math.PI) / 180;
        double yaw = ((boat.getLocation().getYaw() + 90) * Math.PI) / 180;
        double x = Math.sin(pitch) * Math.cos(yaw);
        double z = Math.sin(pitch) * Math.sin(yaw);
        Vector vector = new Vector(x, -sinkRate, z);

        boat.setVelocity(vector.multiply(speedMultiplier));

        lastPlayerLocation = player.getLocation();

    }
}
