package danvb10.boatinertia;

import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BoatMovementOverrider {

    // Support
    BoatInertia boatInertia;
    double maxForeMultiplier = 1, maxAftMultiplier = 0.3;
    // Packet data
    float packetTurnValue = 0, packetForeAftValue = 0;
    // Momentum
    double curHorizontalMultiplier = 0, curVerticalMultiplier = 0;
    // Reference
    Boat boat;
    Player player;
    Location lastPlayerLocation;
    double lastTotalVectorMagnitude, lastPlayerLocationY;

    // Constructor
    public BoatMovementOverrider(Boat boat, BoatInertia boatInertia) {
        this.boat = boat;
        this.boatInertia = boatInertia;
        maxForeMultiplier = boatInertia.getMaxSpeedConfig();
        if (maxForeMultiplier <= 0) {
            boatInertia.getServer().getLogger().warning("[BoatInertia] Config option `max_speed_multiplier` value `" + maxForeMultiplier + "` is `0` or lower - falling back at default value of `1`. Change this value to a positive double to replace fallback.");
            maxForeMultiplier = 1;
        }
    }

    public void tick() {
        if(player == null)
            player = (Player) boat.getPassengers().get(0);

        calculateHorizontalMultiplier();
        boat.setVelocity(calculateVector());

        lastTotalVectorMagnitude = getDeltaMagnitude();
        lastPlayerLocation = player.getLocation();
    }

    private void calculateHorizontalMultiplier() {
        double waterSpeedGoal = 0.4 * maxForeMultiplier;
        double waterSpeedGoalAft = 0.4 * maxAftMultiplier;
        if (isLandlocked()) {
            if (isAttemptingMovement()) {
                double goal = 0.08;
                curHorizontalMultiplier = ((curHorizontalMultiplier * 19) + goal) / 20;
            } else if (isAttemptingAftMovement()) {
                if(curHorizontalMultiplier > 0)
                    curHorizontalMultiplier = curHorizontalMultiplier / 1.5;

                double goal = -0.04;
                curHorizontalMultiplier = ((curHorizontalMultiplier * 19) + goal) / 20;
            } else {
                curHorizontalMultiplier = curHorizontalMultiplier / 1.5;
            }
        } else {
            if (!isInAir() && !isLandlocked()) {
                if (isAttemptingMovement()) {
                    if (curHorizontalMultiplier < 0) {
                        curHorizontalMultiplier *= 0.95;
                        curHorizontalMultiplier += 0.02;
                    } else {
                        if (curHorizontalMultiplier < (waterSpeedGoal / 2)) {
                            curHorizontalMultiplier = ((curHorizontalMultiplier * 49) + waterSpeedGoal) / 50;
                        } else {
                            curHorizontalMultiplier = ((curHorizontalMultiplier * 99) + waterSpeedGoal) / 100;
                        }
                    }
                } else if (isAttemptingAftMovement()) {
                    if (curHorizontalMultiplier > 0) {
                        curHorizontalMultiplier *= 0.95;
                        curHorizontalMultiplier -= 0.02;
                    } else {
                        if (curHorizontalMultiplier > ((-waterSpeedGoalAft) / 2)) {
                            curHorizontalMultiplier = ((curHorizontalMultiplier * 49) + (-waterSpeedGoalAft)) / 50;
                        } else {
                            curHorizontalMultiplier = ((curHorizontalMultiplier * 99) + (-waterSpeedGoalAft)) / 100;
                        }
                    }
                } else {
                    if (Math.abs(curHorizontalMultiplier) > (waterSpeedGoalAft / 2)) {
                        curHorizontalMultiplier = (curHorizontalMultiplier * 99) / 100;
                    } else {
                        curHorizontalMultiplier = (curHorizontalMultiplier * 24) / 25;
                    }
                }
            }

            if (getDeltaMagnitude() < lastTotalVectorMagnitude || getDeltaMagnitude() < 0.1) {
                Material west = player.getLocation().subtract(1, 0, 0).getBlock().getType();
                Material east = player.getLocation().subtract(-1, 0, 0).getBlock().getType();
                Material north = player.getLocation().subtract(0, 0, 1).getBlock().getType();
                Material south = player.getLocation().subtract(0, 0, -1).getBlock().getType();

                double adjustor = 1, potentialAdjustor;
                if (materialNotRideable(west) && getVectorXComponent() < -0.5) {
                    adjustor = getVectorXComponent() + 1;
                }
                if (materialNotRideable(east) && getVectorXComponent() > 0.5) {
                    potentialAdjustor = Math.abs(getVectorXComponent() - 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }
                if (materialNotRideable(north) && getVectorZComponent() < -0.5) {
                    potentialAdjustor = Math.abs(getVectorZComponent() + 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }
                if (materialNotRideable(south) && getVectorZComponent() > 0.5) {
                    potentialAdjustor = Math.abs(getVectorZComponent() - 1);
                    if (potentialAdjustor < adjustor)
                        adjustor = potentialAdjustor;
                }

                if (adjustor < 1 && (isAttemptingMovement() || isAttemptingAftMovement())) {
                    curHorizontalMultiplier = adjustor;
                }
            }

            if (curHorizontalMultiplier < 0.01 && isAttemptingMovement())
                curHorizontalMultiplier += 0.01;

            if (curHorizontalMultiplier > -0.01 && isAttemptingAftMovement())
                curHorizontalMultiplier -= 0.01;

            if (curHorizontalMultiplier < 0.1 && curHorizontalMultiplier > -0.1 && !isAttemptingMovement() && !isAttemptingAftMovement())
                curHorizontalMultiplier = curHorizontalMultiplier * 0.9;
        }

        if (packetTurnValue != 0) {
            if(curHorizontalMultiplier > 0.2) {
                curHorizontalMultiplier *= 0.95;
            }
        }

        if (curHorizontalMultiplier < 0.01 && curHorizontalMultiplier > -0.01 && !isAttemptingMovement() && !isAttemptingAftMovement())
            curHorizontalMultiplier = 0;

        if (curHorizontalMultiplier > waterSpeedGoal)
            curHorizontalMultiplier = waterSpeedGoal;

        if (curHorizontalMultiplier < -waterSpeedGoalAft)
            curHorizontalMultiplier = -waterSpeedGoalAft;
    }

    private boolean isAttemptingAftMovement() {
        return packetForeAftValue < 0;
    }

    private boolean isAttemptingMovement() {
        return packetForeAftValue > 0;
    }

    private double getDeltaMagnitude() {
        if (lastPlayerLocation != null) {
            double x = lastPlayerLocation.getX() - player.getLocation().getX();
            double z = lastPlayerLocation.getZ() - player.getLocation().getZ();
            return Math.sqrt((z * z) + (x * x));
        }
        return 0D;
    }

    private boolean isInAir() {
        Material targetBlock = player.getLocation().getBlock().getType();
        return targetBlock == Material.AIR;
    }

    private boolean materialNotRideable(Material targetMaterial) {
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

    private boolean materialIsWater(Material material) {
        switch (material) {
            case WATER:
            case KELP:
            case KELP_PLANT:
            case SEAGRASS:
            case TALL_SEAGRASS:
                return true;
        }
        return false;
    }

    private boolean isSubmerged() {
        if (materialIsWater(player.getLocation().getBlock().getType())) {
            return materialIsWater(player.getLocation().add(0, 1, 0).getBlock().getType());
        }
        return false;
    }

    private boolean isLandlocked() {
        Material target = player.getLocation().getBlock().getType();
        if (materialNotRideable(target)) {
            target = player.getLocation().add(0, 1, 0).getBlock().getType();
            return !materialIsWater(target);
        } else {
            return false;
        }
    }

    private boolean isAboveBlockOnLand() {
        if (isLandlocked()) {
            double playerY = player.getLocation().getY();
            double deltaFromGround = playerY - (int) playerY;
            return deltaFromGround > 0.56;
        }
        return false;
    }

    private Vector calculateVector() {
        return new Vector(getVectorXComponent() * curHorizontalMultiplier, getVectorYComponent(), getVectorZComponent() * curHorizontalMultiplier);
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
        boolean verticalChange = false;

        if (isInAir() || isAboveBlockOnLand()) {
            if (curVerticalMultiplier <= 0)
                curVerticalMultiplier = 0.1;

            curVerticalMultiplier = curVerticalMultiplier * 1.098;

            if (curVerticalMultiplier > 2)
                curVerticalMultiplier = 2;

            verticalChange = true;
        }

        if (isSubmerged()) {
            if (curVerticalMultiplier > -0.2)
                curVerticalMultiplier = -0.2;

            curVerticalMultiplier = curVerticalMultiplier * 1.098;

            if (curVerticalMultiplier < -2)
                curVerticalMultiplier = -2;

            verticalChange = true;
        }

        if (!verticalChange)
            curVerticalMultiplier = 0;

        if (lastPlayerLocationY == player.getLocation().getY()) {
            curVerticalMultiplier = 0;
        } else {
            curHorizontalMultiplier = ((curHorizontalMultiplier / (curVerticalMultiplier + 1)) + (curHorizontalMultiplier * 2)) / 3;
        }

        lastPlayerLocationY = player.getLocation().getY();

        // prevent flying
        if (curVerticalMultiplier < -0.5)
            curVerticalMultiplier = -0.5;

        // prevent runaway falling
        if (curVerticalMultiplier > 1)
            curVerticalMultiplier = 1;

        return -curVerticalMultiplier;
    }

    public double getMaxForeMultiplier() {
        return maxForeMultiplier;
    }

    public void setMaxForeMultiplier(double maxForeMultiplier) {
        this.maxForeMultiplier = maxForeMultiplier;
    }

    public double getCurHorizontalMultiplier() {
        return curHorizontalMultiplier;
    }

    public void setCurHorizontalMultiplier(double curHorizontalMultiplier) {
        this.curHorizontalMultiplier = curHorizontalMultiplier;
    }

    public double getCurVerticalMultiplier() {
        return curVerticalMultiplier;
    }

    public void setCurVerticalMultiplier(double curVerticalMultiplier) {
        this.curVerticalMultiplier = curVerticalMultiplier;
    }

    public Boat getBoat() {
        return boat;
    }

    public void setBoat(Boat boat) {
        this.boat = boat;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setNewPacketData(PacketEvent event) {
        Object[] packetModVals = event.getPacket().getModifier().getValues().toArray();
        packetTurnValue = (float) packetModVals[0];
        packetForeAftValue = (float) packetModVals[1];
    }
}