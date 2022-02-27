package danvb10.boatinertia;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class InertiaManager {

    // Support
    BoatInertia boatInertia;
    double maxSpeedMultiplier;

    // Momentum
    double speedMultiplier = 0, posVerticalSpeed = 0;

    // Reference
    Boat boat;
    Player player;

    // Comparative
    Location lastPlayerLocation;
    double lastTotalVectorMagnitude, lastPlayerLocationY;

    // Constructor
    public InertiaManager(Boat boat, BoatInertia boatInertia) {
        this.boat = boat;
        this.boatInertia = boatInertia;
        maxSpeedMultiplier = boatInertia.getMaxSpeedConfig();
        if (maxSpeedMultiplier <= 0) {
            boatInertia.getServer().getLogger().warning("[BoatInertia] Config option `max_speed_multiplier` value `" + maxSpeedMultiplier + "` is `0` or lower - falling back at default value of `1`. Change this value to a positive double to replace fallback.");
            maxSpeedMultiplier = 1;
        }
    }

    public Boat getBoat() {
        return boat;
    }

    private double getTotalVectorMagnitude() {
        if (lastPlayerLocation != null) {
            double x = lastPlayerLocation.getX() - player.getLocation().getX();
            double z = lastPlayerLocation.getZ() - player.getLocation().getZ();
            return Math.sqrt((z * z) + (x * x));
        }
        return 0D;
    }

    private boolean isAttemptingMovement() {
        return player.getVelocity().length() > 0.08;
    }

    private boolean isInAir() {
        Material targetBlock = player.getLocation().subtract(0, 0, 0).getBlock().getType();

        return targetBlock == Material.AIR;
    }

    private boolean materialSlowed(Material targetMaterial) {
        switch (targetMaterial) {
            case WATER:
            case KELP:
            case KELP_PLANT:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case ICE:
            case BLUE_ICE:
            case FROSTED_ICE:
            case PACKED_ICE:
                return false;
        }

        return true;
    }

    private boolean isLandlocked() {
        Material target = player.getLocation().subtract(0, 0, 0).getBlock().getType();

        return materialSlowed(target);
    }

    private boolean isAboveBlockOnLand() {
        if (isLandlocked()) {
            double playerY = player.getLocation().getY();
            double deltaFromGround = playerY - (int) playerY;
            return deltaFromGround > 0.56;
        }
        return false;
    }

    private double getBoatPitch() {
        return ((boat.getLocation().getPitch() + 90) * Math.PI) / 180;
    }

    private double getBoatYaw() {
        return ((boat.getLocation().getYaw() + 90) * Math.PI) / 180;
    }

    private double getVectorXComponent() {
        return Math.sin(getBoatPitch()) * Math.cos(getBoatYaw());
    }

    private double getVectorZComponent() {
        return Math.sin(getBoatPitch()) * Math.sin(getBoatYaw());
    }

    private double getVectorYComponent() {
        if (isInAir() || isAboveBlockOnLand()) {
            if (posVerticalSpeed == 0)
                posVerticalSpeed = 0.1;

            posVerticalSpeed = posVerticalSpeed * 1.098;

            if (posVerticalSpeed > 2)
                posVerticalSpeed = 2;
        } else {
            posVerticalSpeed = 0;
        }

        if (lastPlayerLocationY == player.getLocation().getY()) {
            posVerticalSpeed = 0;
        } else {
            speedMultiplier = ((speedMultiplier / (posVerticalSpeed + 1)) + (speedMultiplier * 2)) / 3;
        }

        lastPlayerLocationY = player.getLocation().getY();

        return -posVerticalSpeed;
    }

    private Vector getNewVector() {
        return new Vector(getVectorXComponent() * getCurrentSpeedMultiplier(), getVectorYComponent(), getVectorZComponent() * getCurrentSpeedMultiplier());
    }

    private double getCurrentSpeedMultiplier() {

        if (isLandlocked()) {
            if (isAttemptingMovement()) {
                double landSpeedGoal = 0.05;
                speedMultiplier = ((speedMultiplier * 19) + landSpeedGoal) / 20;
            } else {
                speedMultiplier = speedMultiplier / 1.5;
            }

        } else {

            if (!isInAir() && !isLandlocked()) {
                double waterSpeedGoal = 0.4 * maxSpeedMultiplier;

                if (isAttemptingMovement()) {
                    if(speedMultiplier < (waterSpeedGoal / 2)) {
                        speedMultiplier = ((speedMultiplier * 49) + waterSpeedGoal) / 50;
                    } else {
                        speedMultiplier = ((speedMultiplier * 99) + waterSpeedGoal) / 100;
                    }

                } else {
                    if(speedMultiplier > (waterSpeedGoal / 2)) {
                        speedMultiplier = (speedMultiplier * 99) / 100;
                    } else {
                        speedMultiplier = (speedMultiplier * 24) / 25;
                    }
                }
            }

            if (getTotalVectorMagnitude() < lastTotalVectorMagnitude || getTotalVectorMagnitude() < 0.1) {
                Material west = player.getLocation().subtract(1, 0, 0).getBlock().getType();
                Material east = player.getLocation().subtract(-1, 0, 0).getBlock().getType();
                Material north = player.getLocation().subtract(0, 0, 1).getBlock().getType();
                Material south = player.getLocation().subtract(0, 0, -1).getBlock().getType();

                double adjustor = 1, potentialAdjustor;
                if (materialSlowed(west) && getVectorXComponent() < -0.5) {
                    adjustor = getVectorXComponent() + 1;
                }
                if (materialSlowed(east) && getVectorXComponent() > 0.5) {
                    potentialAdjustor = Math.abs(getVectorXComponent() - 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }
                if (materialSlowed(north) && getVectorZComponent() < -0.5) {
                    potentialAdjustor = Math.abs(getVectorZComponent() + 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }
                if (materialSlowed(south) && getVectorZComponent() > 0.5) {
                    potentialAdjustor = Math.abs(getVectorZComponent() - 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }

                if (adjustor < 1 && isAttemptingMovement()) {
                    speedMultiplier = adjustor;
                }
            }

            if (speedMultiplier < 0.02 && isAttemptingMovement())
                speedMultiplier += 0.02;

            if (speedMultiplier < 0.1 && !isAttemptingMovement())
                speedMultiplier = speedMultiplier * 0.9;
        }

        if (speedMultiplier < 0.02 && !isAttemptingMovement())
            speedMultiplier = 0;

        return speedMultiplier;
    }

    public void update() {
        player = (Player) boat.getPassengers().get(0);
        boat.setVelocity(getNewVector());

        lastTotalVectorMagnitude = getTotalVectorMagnitude();
        lastPlayerLocation = player.getLocation();
    }
}
