// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import java.util.ArrayList;
import zombie.radio.devices.DeviceData;

public class VoiceManagerData {
    public static ArrayList<VoiceManagerData> data = new ArrayList<>();
    public long userplaychannel;
    public long userplaysound;
    public boolean userplaymute;
    public long voicetimeout;
    public final ArrayList<VoiceManagerData.RadioData> radioData = new ArrayList<>();
    public boolean isCanHearAll;
    short index;

    public VoiceManagerData(short index) {
        this.userplaymute = false;
        this.userplaychannel = 0L;
        this.userplaysound = 0L;
        this.voicetimeout = 0L;
        this.index = index;
    }

    public static VoiceManagerData get(short onlineId) {
        if (data.size() <= onlineId) {
            for (short i = (short)data.size(); i <= onlineId; i++) {
                VoiceManagerData td = new VoiceManagerData(i);
                data.add(td);
            }
        }

        VoiceManagerData d = data.get(onlineId);
        if (d == null) {
            d = new VoiceManagerData(onlineId);
            data.set(onlineId, d);
        }

        return d;
    }

    public static class RadioData {
        DeviceData deviceData;
        public int freq;
        public float distance;
        public short x;
        public short y;
        float lastReceiveDistance;

        public RadioData(float distance, float x, float y) {
            this(null, 0, distance, x, y);
        }

        public RadioData(int freq, float distance, float x, float y) {
            this(null, freq, distance, x, y);
        }

        public RadioData(DeviceData data, float x, float y) {
            this(data, data.getChannel(), data.getMicIsMuted() ? 0.0F : data.getTransmitRange(), x, y);
        }

        private RadioData(DeviceData deviceData, int freq, float distance, float x, float y) {
            this.deviceData = deviceData;
            this.freq = freq;
            this.distance = distance;
            this.x = (short)x;
            this.y = (short)y;
        }

        public boolean isTransmissionAvailable() {
            return this.freq != 0
                && this.deviceData != null
                && this.deviceData.getIsTurnedOn()
                && this.deviceData.getIsTwoWay()
                && !this.deviceData.isNoTransmit()
                && !this.deviceData.getMicIsMuted();
        }

        public boolean isReceivingAvailable(int channel) {
            return this.freq != 0
                && this.deviceData != null
                && this.deviceData.getIsTurnedOn()
                && this.deviceData.getChannel() == channel
                && this.deviceData.getDeviceVolume() != 0.0F
                && !this.deviceData.isPlayingMedia();
        }

        public DeviceData getDeviceData() {
            return this.deviceData;
        }
    }

    public static enum VoiceDataSource {
        Unknown,
        Voice,
        Radio,
        Cheat;
    }
}
