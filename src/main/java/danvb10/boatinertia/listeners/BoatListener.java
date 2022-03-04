package danvb10.boatinertia.listeners;

import danvb10.boatinertia.BoatInertia;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class BoatListener implements Listener {

    BoatInertia boatInertia;

    public BoatListener(BoatInertia boatInertia) {
        this.boatInertia = boatInertia;
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player && event.getVehicle() instanceof Boat) {
            Boat boat = (Boat) event.getVehicle();
            boatInertia.trackBoat(boat);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player && event.getVehicle() instanceof Boat) {
            Boat boat = (Boat) event.getVehicle();
            boatInertia.untrackBoat(boat);
        }
    }
}