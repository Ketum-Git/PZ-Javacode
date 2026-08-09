// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.List;
import zombie.GameTime;
import zombie.WorldSoundManager;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameClient;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class VehicleAlarm {
    private static final int WORLDSOUND_RADIUS = 150;
    private static final int WORLDSOUND_VOLUME = 150;
    private VehiclePartOwner owner;
    private IVehicleAlarmListener listener;
    private double startTime;
    private float accumulator;
    private String chosenSound;
    private boolean soundOn;
    private boolean lightsOn;
    private final UpdateLimit worldSoundUpdateLimit = new UpdateLimit(1000L);

    public VehicleAlarm(VehiclePartOwner owner) {
        this.owner = owner;
    }

    public void setOwner(VehiclePartOwner owner) {
        this.owner = owner;
    }

    public void setListener(IVehicleAlarmListener listener) {
        this.listener = listener;
    }

    public void setStartTime(double worldAgeHours) {
        this.startTime = worldAgeHours;
    }

    public double getStartTime() {
        return this.startTime;
    }

    public void setChosenSound(String sound) {
        this.chosenSound = StringUtils.discardNullOrWhitespace(sound);
    }

    public String getChosenSound() {
        this.chooseSound();
        return this.chosenSound;
    }

    public boolean isActive() {
        return this.getStartTime() > 0.0;
    }

    public boolean isSoundOn() {
        return this.soundOn;
    }

    private VehicleScript getScript() {
        return this.owner.getScript();
    }

    public void trigger() {
        this.startTime = GameTime.getInstance().getWorldAgeHours();
        this.accumulator = 0.0F;
        if (this.isChosenSoundLooping()) {
            this.onAlarmStart();
        }
    }

    public void update() {
        if (!(this.startTime <= 0.0)) {
            if (this.owner.getBatteryCharge() <= 0.0F) {
                if (this.isSoundOn()) {
                    this.onAlarmStop();
                }

                this.startTime = 0.0;
            } else {
                double worldAge = GameTime.getInstance().getWorldAgeHours();
                this.startTime = GameTime.minHours(this.startTime, worldAge);
                if (worldAge >= this.startTime + 0.66 * GameTime.getInstance().getDeltaMinutesPerDay()) {
                    this.onAlarmStop();
                    this.setLightsOff();
                    this.startTime = 0.0;
                } else {
                    this.chooseSound();
                    boolean bLooping = this.isChosenSoundLooping();
                    if (bLooping && !this.soundOn) {
                        this.onAlarmStart();
                    }

                    this.accumulator = this.accumulator + GameTime.instance.getThirtyFPSMultiplier();
                    int t = (int)this.accumulator / 24;
                    if (!this.lightsOn && t % 2 == 0) {
                        if (!bLooping) {
                            this.onAlarmStart();
                        }

                        this.setLightsOn();
                    }

                    if (this.lightsOn && t % 2 == 1) {
                        if (!bLooping) {
                            this.onAlarmStop();
                        }

                        this.setLightsOff();
                    }

                    this.updateWorldSounds();
                }
            }
        }
    }

    public void chooseSound() {
        if (this.getScript() == null) {
            this.chosenSound = null;
        } else {
            VehicleScript.Sounds sounds = this.getScript().getSounds();
            if (!sounds.alarmEnable) {
                this.chosenSound = null;
            } else {
                if (this.chosenSound == null || !sounds.alarm.contains(this.chosenSound) && !sounds.alarmLoop.contains(this.chosenSound)) {
                    List<String> choices = new ArrayList<>();
                    choices.addAll(sounds.alarm);
                    choices.addAll(sounds.alarmLoop);
                    this.chosenSound = PZArrayUtil.pickRandom(choices);
                }
            }
        }
    }

    public boolean isChosenSoundLooping() {
        if (this.getScript() == null) {
            return false;
        } else {
            this.chooseSound();
            VehicleScript.Sounds sounds = this.getScript().getSounds();
            return sounds.alarmLoop.contains(this.chosenSound);
        }
    }

    public void onAlarmStart() {
        this.soundOn = true;
        if (!GameClient.client && this.getScript().getSounds().alarmEnable) {
            WorldSoundManager.instance
                .addSound(this, this.owner.getXi(), this.owner.getYi(), this.owner.getZi(), 150, 150, false, 0.0F, 1.0F, false, true, false, false, true);
        }

        this.listener.onVehicleAlarmEvent(VehicleAlarmEvent.ACTIVATED);
    }

    public void onAlarmStop() {
        this.soundOn = false;
        this.listener.onVehicleAlarmEvent(VehicleAlarmEvent.DEACTIVATED);
    }

    private void setLightsOn() {
        this.lightsOn = true;
        this.listener.onVehicleAlarmEvent(VehicleAlarmEvent.LIGHTS_ON);
    }

    private void setLightsOff() {
        this.lightsOn = false;
        this.listener.onVehicleAlarmEvent(VehicleAlarmEvent.LIGHTS_OFF);
    }

    public boolean isLightsOn() {
        return this.lightsOn;
    }

    private void updateWorldSounds() {
        if (!GameClient.client) {
            if (this.isSoundOn() && this.isChosenSoundLooping() && this.worldSoundUpdateLimit.Check()) {
                WorldSoundManager.instance.addSoundRepeating(this.owner, this.owner.getXi(), this.owner.getYi(), this.owner.getZi(), 150, 150, false, true);
            }
        }
    }
}
