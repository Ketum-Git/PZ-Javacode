// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunk;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleAuthorization;
import zombie.network.fields.vehicle.VehiclePassengers;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleFullUpdatePacket extends VehiclePacket implements INetworkPacket {
    @JSONField
    protected final VehicleInterpolationData vehiclePositionOrientation = new VehicleInterpolationData();
    @JSONField
    protected final VehicleAuthorization vehicleAuthorization = new VehicleAuthorization(this.vehicleID);
    @JSONField
    protected final VehiclePassengers vehiclePassengers = new VehiclePassengers(this.vehicleID);
    private final float[] physicsData = new float[27];
    private final BaseVehicle tempVehicle = new BaseVehicle(IsoWorld.instance.currentCell);
    private final Transform tempTransform = new Transform();

    @Override
    public void setData(Object... values) {
        super.set((BaseVehicle)values[0]);
        this.vehicleAuthorization.set((BaseVehicle)values[0]);
        this.vehiclePositionOrientation.set((BaseVehicle)values[0]);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        super.parse(bb, connection);

        try {
            if (this.isConsistent(connection) && !this.doRemove(connection)) {
                if (this.vehicleID.isConsistent(connection)) {
                    bb.get();
                    bb.get();
                    this.tempVehicle.partsClear();
                    this.tempVehicle.load(bb, 241);
                    if (this.vehicleID.getVehicle().getController() != null
                        && (this.vehicleID.getVehicle().getDriver() == null || !this.vehicleID.getVehicle().getDriver().isLocal())) {
                        this.tempTransform.setRotation(this.tempVehicle.savedRot);
                        this.tempTransform
                            .origin
                            .set(
                                this.position.getX() - WorldSimulation.instance.offsetX,
                                this.position.getZ(),
                                this.position.getY() - WorldSimulation.instance.offsetY
                            );
                        this.vehicleID.getVehicle().setWorldTransform(this.tempTransform);
                    }

                    VehicleManager.instance
                        .clientUpdateVehiclePos(this.vehicleID.getVehicle(), this.position.getX(), this.position.getY(), this.position.getZ(), this.square);
                } else {
                    boolean serialise = bb.get() != 0;
                    byte classID = bb.get();
                    if (!serialise || classID != IsoObject.getFactoryVehicle().getClassID()) {
                        DebugLog.Vehicle.error("%s parse failed", this.getClass().getSimpleName());
                    }

                    BaseVehicle vehicle = new BaseVehicle(IsoWorld.instance.currentCell);
                    vehicle.vehicleId = this.vehicleID.getID();
                    this.vehicleID.set(vehicle);
                    vehicle.square = this.square;
                    vehicle.setCurrent(this.square);
                    vehicle.load(bb, 241);
                    if (this.square != null) {
                        vehicle.chunk = vehicle.square.chunk;
                        vehicle.chunk.vehicles.add(vehicle);
                        vehicle.addToWorld();
                    }

                    IsoChunk.addFromCheckedVehicles(vehicle);
                    VehicleManager.instance.registerVehicle(vehicle);

                    for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player != null && !player.isDead() && player.getVehicle() == null) {
                            IsoWorld.instance.currentCell.putInVehicle(player);
                        }
                    }
                }

                this.vehicleAuthorization.parse(bb, connection);
                this.vehicleID
                    .getVehicle()
                    .netPlayerFromServerUpdate(this.vehicleAuthorization.getAuthorization(), this.vehicleAuthorization.getAuthorizationPlayer());
                this.vehiclePositionOrientation.parse(bb, connection);
                if (!this.vehicleID.getVehicle().isKeyboardControlled() && this.vehicleID.getVehicle().getJoypad() == -1) {
                    this.vehiclePositionOrientation.getPhysicsData(this.physicsData);
                    Bullet.setOwnVehiclePhysics(this.vehicleID.getID(), this.physicsData);
                }

                this.vehiclePassengers.parse(bb, connection);
            }
        } catch (Exception var8) {
            DebugLog.Multiplayer.printException(var8, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);

        try {
            this.vehicleID.getVehicle().save(b.bb);
            this.vehicleAuthorization.write(b);
            this.vehiclePositionOrientation.write(b);
            this.vehiclePassengers.write(b);
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
