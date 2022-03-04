package danvb10.boatinertia.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import danvb10.boatinertia.BoatInertia;
import danvb10.boatinertia.BoatMovementOverrider;

import java.util.Objects;

public class ClientSteerVehiclePacket {

    private final ProtocolManager protocolManager;

    public ClientSteerVehiclePacket(BoatInertia boatInertia) {
        protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(
                new PacketAdapter(boatInertia, PacketType.Play.Client.STEER_VEHICLE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        for (BoatMovementOverrider bmo : boatInertia.overriders) {
                            if (bmo.getBoat().getEntityId() == Objects.requireNonNull(event.getPlayer().getVehicle()).getEntityId()) {
                                // Update current controller
                                bmo.setNewPacketData(event);
                            }
                        }
                    }
                });
    }
}
