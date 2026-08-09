// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class MusicIntensityConfig {
    private static MusicIntensityConfig instance;
    private final ArrayList<MusicIntensityConfig.Event> events = new ArrayList<>();
    private final HashMap<String, MusicIntensityConfig.Event> eventById = new HashMap<>();

    public static MusicIntensityConfig getInstance() {
        if (instance == null) {
            instance = new MusicIntensityConfig();
        }

        return instance;
    }

    public void initEvents(KahluaTableImpl eventsTable) {
        this.events.clear();
        this.eventById.clear();
        KahluaTableIterator it = eventsTable.iterator();

        while (it.advance()) {
            String key = it.getKey().toString();
            if (!"VERSION".equalsIgnoreCase(key)) {
                KahluaTableImpl eventTable = (KahluaTableImpl)it.getValue();
                MusicIntensityConfig.Event event = new MusicIntensityConfig.Event();
                event.id = StringUtils.discardNullOrWhitespace(eventTable.rawgetStr("id"));
                event.intensity = eventTable.rawgetFloat("intensity");
                event.duration = eventTable.rawgetInt("duration");
                if (eventTable.rawget("multiple") instanceof Boolean b) {
                    event.multiple = b;
                }

                if (event.id != null) {
                    if (this.eventById.containsKey(event.id)) {
                        this.events.remove(this.eventById.get(event.id));
                    }

                    this.events.add(event);
                    this.eventById.put(event.id, event);
                }
            }
        }
    }

    public MusicIntensityEvent triggerEvent(String id, MusicIntensityEvents mie) {
        MusicIntensityConfig.Event event = this.eventById.get(id);
        return event == null ? null : mie.addEvent(event.id, event.intensity, event.duration, event.multiple);
    }

    public void checkHealthPanelVisible(IsoGameCharacter character) {
        if (character instanceof IsoPlayer player) {
            this.checkHealthPanel_SeeBite(player);
        }
    }

    private void checkHealthPanel_SeeBite(IsoPlayer player) {
        Object worldAgeHours = player.getMusicIntensityEventModData("HealthPanel_SeeBite");
        if (worldAgeHours == null) {
            BodyDamage bodyDamage = player.getBodyDamage();
            boolean bBitten = false;

            for (int i = 0; i < BodyPartType.ToIndex(BodyPartType.MAX); i++) {
                if (bodyDamage.IsBitten(i)) {
                    bBitten = true;
                    break;
                }
            }

            if (bBitten) {
                player.setMusicIntensityEventModData("HealthPanel_SeeBite", GameTime.getInstance().getWorldAgeHours());
                player.triggerMusicIntensityEvent("HealthPanel_SeeBite");
            }
        }
    }

    public void restoreToFullHealth(IsoGameCharacter character) {
        IsoPlayer player = Type.tryCastTo(character, IsoPlayer.class);
        if (player != null && player.hasModData()) {
            player.setMusicIntensityEventModData("HealthPanel_SeeBite", null);
        }
    }

    private static final class Event {
        String id;
        float intensity;
        long duration;
        boolean multiple = true;
    }
}
