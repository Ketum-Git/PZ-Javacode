// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.zones;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import zombie.debug.DebugLog;

public class ZoneHandler<U extends Zone> {
    private final HashMap<UUID, U> zones = new HashMap<>();

    public void Dispose() {
        this.zones.clear();
    }

    public void addZone(U zone) {
        DebugLog.Zone.debugln(zone);
        this.zones.put(zone.id, zone);
    }

    public U getZone(UUID id) {
        return this.zones.get(id);
    }

    public Collection<U> getZones() {
        return this.zones.values();
    }
}
