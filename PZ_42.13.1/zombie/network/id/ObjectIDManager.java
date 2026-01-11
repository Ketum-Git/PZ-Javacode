// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.id;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;

public class ObjectIDManager {
    private static final ObjectIDManager instance = new ObjectIDManager();
    private static final int saveLastIDNumber = 100;
    private static int objectIdManagerCheckLimiter;

    public static ObjectIDManager getInstance() {
        return instance;
    }

    private ObjectIDManager() {
    }

    public void clear() {
        for (ObjectIDType type : ObjectIDType.values()) {
            type.lastId = 0L;
            type.countNewId = 0L;
        }
    }

    public void load(DataInputStream input, int WorldVersion) throws IOException {
        byte size = input.readByte();

        for (byte i = 0; i < size; i++) {
            byte index = input.readByte();
            long lastID = input.readLong();
            ObjectIDType.valueOf(index).lastId = lastID + 100L;
            ObjectIDType.valueOf(index).countNewId = 0L;
            DebugLog.General.println(ObjectIDType.valueOf(index));
        }
    }

    private void save(DataOutputStream output) throws IOException {
        output.write(ObjectIDType.permanentObjectIDTypes);

        for (ObjectIDType type : ObjectIDType.values()) {
            if (type.isPermanent) {
                output.writeByte(type.index);
                output.writeLong(type.lastId);
            }

            type.countNewId = 0L;
            DebugLog.General.println(type);
        }
    }

    private boolean isNeedToSave() {
        for (ObjectIDType type : ObjectIDType.values()) {
            if (type.countNewId >= 100L) {
                return true;
            }
        }

        return false;
    }

    public void checkForSaveDataFile(boolean force) {
        if (!GameClient.client) {
            objectIdManagerCheckLimiter++;
            if (force || objectIdManagerCheckLimiter > 300) {
                objectIdManagerCheckLimiter = 0;
                if (force || this.isNeedToSave()) {
                    DebugLog.General.println("The id_manager_data.bin file is saved");
                    File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("id_manager_data.bin");

                    try (
                        FileOutputStream fos = new FileOutputStream(outFile);
                        DataOutputStream output = new DataOutputStream(fos);
                    ) {
                        output.writeInt(IsoWorld.getWorldVersion());
                        this.save(output);
                    } catch (IOException var11) {
                        DebugLog.General.printException(var11, "Save failed", LogSeverity.Error);
                    }
                }
            }
        }
    }

    public static IIdentifiable get(ObjectID id) {
        return id.getType().idToObjectMap.get(id.getObjectID());
    }

    public void remove(ObjectID id) {
        IIdentifiable obj = id.getType().idToObjectMap.get(id.getObjectID());
        boolean isRemoved = false;
        if (id.getType().idToObjectMap.contains(id.getObjectID())) {
            isRemoved = id.getType().idToObjectMap.remove(id.getObjectID(), obj);
        }
    }

    public void addObject(IIdentifiable object) {
        if (object == null) {
            DebugLog.General.warn("%s ObjectID: is null");
        } else {
            long id = object.getObjectID().getObjectID();
            ObjectIDType type = object.getObjectID().getType();
            if (id == -1L) {
                if (GameClient.client) {
                    return;
                }

                id = (short)type.allocateID();

                while (type.idToObjectMap.get(id) != null) {
                    id = (short)type.allocateID();
                }
            }

            type.idToObjectMap.add(id, object);
            object.getObjectID().set(id, type);
        }
    }

    public static ObjectID createObjectID(ObjectIDType type) {
        try {
            Constructor<?> ctr = type.type.getDeclaredConstructor(ObjectIDType.class);
            return (ObjectID)ctr.newInstance(type);
        } catch (Exception var2) {
            DebugLog.General.printException(var2, "ObjectID creation failed", LogSeverity.Error);
            throw new RuntimeException();
        }
    }
}
