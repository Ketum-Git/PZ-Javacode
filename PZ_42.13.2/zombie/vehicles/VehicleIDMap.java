// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public final class VehicleIDMap {
    public static final VehicleIDMap instance = new VehicleIDMap();
    private static final int MAX_IDS = 32767;
    private static final int RESIZE_COUNT = 256;
    private int capacity = 256;
    private BaseVehicle[] idToVehicle;
    private short[] freeId;
    private short freeIdSize;
    private final boolean noise = false;
    private int warnCount;

    VehicleIDMap() {
        this.idToVehicle = new BaseVehicle[this.capacity];
        this.freeId = new short[this.capacity];

        for (int i = 0; i < this.capacity; i++) {
            this.freeId[this.freeIdSize++] = (short)i;
        }
    }

    public void put(short id, BaseVehicle vehicle) {
        if (Core.debug) {
        }

        if (GameClient.client && id >= this.capacity) {
            this.resize((id / 256 + 1) * 256);
        }

        if (id < 0 || id >= this.capacity) {
            throw new IllegalArgumentException("invalid vehicle id " + id + " max=" + this.capacity);
        } else if (this.idToVehicle[id] != null) {
            throw new IllegalArgumentException("duplicate vehicle with id " + id);
        } else if (vehicle == null) {
            throw new IllegalArgumentException("vehicle is null");
        } else {
            this.idToVehicle[id] = vehicle;
        }
    }

    public void remove(short id) {
        if (Core.debug) {
        }

        if (id < 0 || id >= this.capacity) {
            throw new IllegalArgumentException("invalid vehicle id=" + id + " max=" + this.capacity);
        } else if (this.idToVehicle[id] == null) {
            throw new IllegalArgumentException("no vehicle with id " + id);
        } else {
            this.idToVehicle[id] = null;
            if (!GameClient.client && !GameWindow.loadedAsClient) {
                this.freeId[this.freeIdSize++] = id;
            }
        }
    }

    public BaseVehicle get(short id) {
        return id >= 0 && id < this.capacity ? this.idToVehicle[id] : null;
    }

    public boolean containsKey(short id) {
        return id >= 0 && id < this.capacity && this.idToVehicle[id] != null;
    }

    public void toArrayList(ArrayList<BaseVehicle> vehicles) {
        for (int i = 0; i < this.capacity; i++) {
            if (this.idToVehicle[i] != null) {
                vehicles.add(this.idToVehicle[i]);
            }
        }
    }

    public void Reset() {
        Arrays.fill(this.idToVehicle, null);
        this.freeIdSize = (short)this.capacity;
        short i = 0;

        while (i < this.capacity) {
            this.freeId[i] = i++;
        }
    }

    public short allocateID() {
        if (GameClient.client) {
            throw new RuntimeException("client must not call this");
        } else if (this.freeIdSize > 0) {
            return this.freeId[--this.freeIdSize];
        } else if (this.capacity >= 32767) {
            if (this.warnCount < 100) {
                DebugLog.log("warning: ran out of unique vehicle ids");
                this.warnCount++;
            }

            return -1;
        } else {
            this.resize(this.capacity + 256);
            return this.allocateID();
        }
    }

    private void resize(int capacity) {
        int oldCapacity = this.capacity;
        this.capacity = Math.min(capacity, 32767);
        this.capacity = Math.min(capacity, 32767);
        this.idToVehicle = Arrays.copyOf(this.idToVehicle, this.capacity);
        this.freeId = Arrays.copyOf(this.freeId, this.capacity);

        for (int i = oldCapacity; i < this.capacity; i++) {
            this.freeId[this.freeIdSize++] = (short)i;
        }
    }
}
