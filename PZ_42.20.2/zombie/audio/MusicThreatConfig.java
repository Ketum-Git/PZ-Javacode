// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.util.StringUtils;

@UsedFromLua
public final class MusicThreatConfig {
    private static MusicThreatConfig instance;
    private final ArrayList<MusicThreatConfig.Status> statusList = new ArrayList<>();
    private final HashMap<String, MusicThreatConfig.Status> statusById = new HashMap<>();

    public static MusicThreatConfig getInstance() {
        if (instance == null) {
            instance = new MusicThreatConfig();
        }

        return instance;
    }

    public void initStatuses(KahluaTableImpl statusesTable) {
        this.statusList.clear();
        this.statusById.clear();
        KahluaTableIterator it = statusesTable.iterator();

        while (it.advance()) {
            String key = it.getKey().toString();
            if (!"VERSION".equalsIgnoreCase(key)) {
                KahluaTableImpl statusTable = (KahluaTableImpl)it.getValue();
                MusicThreatConfig.Status status = new MusicThreatConfig.Status();
                status.id = StringUtils.discardNullOrWhitespace(statusTable.rawgetStr("id"));
                status.intensity = statusTable.rawgetFloat("intensity");
                if (status.id != null && !(status.intensity <= 0.0F)) {
                    if (this.statusById.containsKey(status.id)) {
                        this.statusList.remove(this.statusById.get(status.id));
                    }

                    this.statusList.add(status);
                    this.statusById.put(status.id, status);
                }
            }
        }
    }

    public int getStatusCount() {
        return this.statusList.size();
    }

    public String getStatusIdByIndex(int index) {
        return this.statusList.get(index).id;
    }

    public float getStatusIntensityByIndex(int index) {
        return this.statusList.get(index).intensity;
    }

    public float getStatusIntensity(String id) {
        MusicThreatConfig.Status status = this.statusById.get(id);
        return status == null ? 0.0F : status.intensity;
    }

    public void setStatusIntensityOverride(String id, float intensity) {
        MusicThreatConfig.Status status = this.statusById.get(id);
        if (status != null) {
            status.intensityOverride = intensity < 0.0F ? Float.NaN : PZMath.clamp(intensity, 0.0F, 1.0F);
        }
    }

    public float getStatusIntensityOverride(String id) {
        MusicThreatConfig.Status status = this.statusById.get(id);
        return status == null ? 0.0F : status.intensityOverride;
    }

    public boolean isStatusIntensityOverridden(String id) {
        MusicThreatConfig.Status status = this.statusById.get(id);
        return status == null ? false : !Float.isNaN(status.intensityOverride);
    }

    private static final class Status {
        String id;
        float intensity;
        float intensityOverride = Float.NaN;
    }
}
