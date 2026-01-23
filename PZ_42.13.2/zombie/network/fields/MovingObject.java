// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.ServerMap;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

public class MovingObject implements INetworkPacketField {
    private final byte objectTypeNone = 0;
    private final byte objectTypeIsoObject = 1;
    private final byte objectTypePlayer = 2;
    private final byte objectTypeZombie = 3;
    private final byte objectTypeAnimal = 4;
    private final byte objectTypeVehicle = 5;
    private final byte objectTypeDeadBody = 6;
    private final byte objectTypeMovingObject = 7;
    @JSONField
    private byte objectType;
    @JSONField
    private short objectId;
    @JSONField
    private int squareX;
    @JSONField
    private int squareY;
    @JSONField
    private byte squareZ;
    private boolean isProcessed;
    private IsoObject object;

    public void set(IsoObject value) {
        this.object = value;
        this.isProcessed = true;
        if (this.object == null) {
            this.objectType = 0;
            this.objectId = 0;
        } else if (this.object instanceof IsoAnimal isoAnimal) {
            this.objectType = 4;
            this.objectId = isoAnimal.getOnlineID();
        } else if (this.object instanceof IsoPlayer isoPlayer) {
            this.objectType = 2;
            this.objectId = isoPlayer.getOnlineID();
        } else if (this.object instanceof IsoZombie isoZombie) {
            this.objectType = 3;
            this.objectId = isoZombie.getOnlineID();
        } else if (this.object instanceof BaseVehicle baseVehicle) {
            this.objectType = 5;
            this.objectId = baseVehicle.vehicleId;
        } else if (this.object instanceof IsoDeadBody movingObject) {
            this.objectType = 6;
            this.objectId = (short)this.object.getStaticMovingObjectIndex();
            IsoGridSquare square = movingObject.getSquare();
            if (square != null) {
                this.squareX = square.getX();
                this.squareY = square.getY();
                this.squareZ = (byte)square.getZ();
            }
        } else {
            if (this.object instanceof IsoMovingObject isoMovingObject) {
                this.objectType = 7;
                IsoGridSquare square = isoMovingObject.getCurrentSquare();
                if (square == null) {
                    return;
                }

                this.objectId = (short)square.getMovingObjects().indexOf(this.object);
                this.squareX = square.getX();
                this.squareY = square.getY();
                this.squareZ = (byte)square.getZ();
            }

            this.objectType = 1;
            IsoGridSquare square = this.object.getSquare();
            if (square != null) {
                this.objectId = (short)square.getObjects().indexOf(this.object);
                this.squareX = square.getX();
                this.squareY = square.getY();
                this.squareZ = (byte)square.getZ();
            }
        }
    }

    public IsoObject getObject() {
        if (!this.isProcessed) {
            if (this.objectType == 0) {
                this.object = null;
            }

            if (this.objectType == 4) {
                this.object = AnimalInstanceManager.getInstance().get(this.objectId);
            }

            if (this.objectType == 2) {
                if (GameServer.server) {
                    this.object = GameServer.IDToPlayerMap.get(this.objectId);
                } else if (GameClient.client) {
                    this.object = GameClient.IDToPlayerMap.get(this.objectId);
                }
            }

            if (this.objectType == 3) {
                if (GameServer.server) {
                    this.object = ServerMap.instance.zombieMap.get(this.objectId);
                } else if (GameClient.client) {
                    this.object = GameClient.IDToZombieMap.get(this.objectId);
                }
            }

            if (this.objectType == 5) {
                this.object = VehicleManager.instance.getVehicleByID(this.objectId);
            }

            if (this.objectType == 6) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                if (square != null && this.objectId < square.getStaticMovingObjects().size()) {
                    this.object = square.getStaticMovingObjects().get(this.objectId);
                } else {
                    this.object = null;
                }
            }

            if (this.objectType == 7) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                if (square != null && this.objectId < square.getMovingObjects().size() && this.objectId >= 0) {
                    this.object = square.getMovingObjects().get(this.objectId);
                } else {
                    this.object = null;
                }
            }

            if (this.objectType == 1) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                if (square != null && this.objectId < square.getObjects().size() && this.objectId >= 0) {
                    this.object = square.getObjects().get(this.objectId);
                } else {
                    this.object = null;
                }
            }

            this.isProcessed = true;
        }

        return this.object;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.objectType = b.get();
        this.objectId = b.getShort();
        if (this.objectType == 7 || this.objectType == 6 || this.objectType == 1) {
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.get();
        }

        this.isProcessed = false;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.objectType);
        b.putShort(this.objectId);
        if (this.objectType == 7 || this.objectType == 6 || this.objectType == 1) {
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.putByte(this.squareZ);
        }
    }

    @Override
    public int getPacketSizeBytes() {
        return this.objectType != 7 && this.objectType != 6 && this.objectType != 1 ? 3 : 12;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.getObject() != null;
    }
}
