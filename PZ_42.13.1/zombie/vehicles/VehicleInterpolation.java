// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.joml.Quaternionf;
import zombie.GameTime;
import zombie.core.physics.WorldSimulation;
import zombie.network.statistics.PingManager;

/**
 * Created by kroto on 1/17/2017.
 */
public class VehicleInterpolation {
    private static final ArrayDeque<VehicleInterpolationData> pool = new ArrayDeque<>();
    private static final List<VehicleInterpolationData> outdated = new ArrayList<>();
    private static final Quaternionf tempQuaternionA = new Quaternionf();
    private static final Quaternionf tempQuaternionB = new Quaternionf();
    private static final VehicleInterpolationData temp = new VehicleInterpolationData();
    final TreeSet<VehicleInterpolationData> buffer = new TreeSet<>();
    int delay;
    int history;
    int delayTarget;
    boolean buffering;
    float[] lastBuf1;
    boolean wasNull;
    long lastTime = -1L;
    long lastTimeA = -1L;
    long recountedTime;
    boolean highPing;
    boolean wasHighPing;
    private byte getPointUpdateTimeout;
    private final byte getPointUpdatePer = 10;

    VehicleInterpolation() {
        this.reset();
        this.delay = 500;
        this.history = 800;
        this.delayTarget = this.delay;
    }

    public void reset() {
        this.buffering = true;
        this.clear();
    }

    public void clear() {
        if (!this.buffer.isEmpty()) {
            pool.addAll(this.buffer);
            this.buffer.clear();
            outdated.clear();
        }
    }

    public void update(long time) {
        temp.time = time - this.delay;
        VehicleInterpolationData data_a = this.buffer.floor(temp);

        for (VehicleInterpolationData data : this.buffer) {
            if (time - data.time > this.history && data != data_a) {
                outdated.add(data);
            }
        }

        outdated.forEach(this.buffer::remove);
        pool.addAll(outdated);
        outdated.clear();
        if (this.buffer.isEmpty()) {
            this.buffering = true;
        }
    }

    void getPointUpdate() {
        if (this.getPointUpdateTimeout++ >= 10) {
            float dt = WorldSimulation.instance.periodSec * 10.0F;
            this.getPointUpdateTimeout = 0;
            int lastPing = PingManager.getPing();
            boolean isHighPing = lastPing > 290;
            if (this.highPing && !isHighPing) {
                this.wasHighPing = true;
            }

            if (isHighPing) {
                this.wasHighPing = false;
            }

            this.highPing = isHighPing;
            if (this.delay != this.delayTarget) {
                int delayStep = Math.max(1, (int)(500.0F * dt / 3.0F));
                if (this.delay < this.delayTarget) {
                    if (this.wasHighPing) {
                        this.wasHighPing = false;
                    }

                    if (this.highPing) {
                        delayStep = this.delayTarget - this.delay;
                    }

                    int d = Math.min(this.delayTarget - this.delay, delayStep);
                    this.delay += d;
                    this.history += d;
                } else {
                    if (this.wasHighPing) {
                        delayStep = (int)(delayStep / 8.0F);
                        if (delayStep < 1) {
                            delayStep = 1;
                        }
                    }

                    int d = Math.min(this.delay - this.delayTarget, delayStep);
                    this.delay -= d;
                    this.history -= d;
                }
            }

            int delayCap = 500;
            if (this.highPing) {
                delayCap = (int)(delayCap * 3.0F);
            }

            if (this.delayTarget != delayCap) {
                if (this.delayTarget < delayCap) {
                    this.delayTarget = this.delayTarget + Math.max(1, (int)(delayCap * dt / 10.0F));
                    if (this.highPing) {
                        this.delayTarget = delayCap;
                    }
                } else {
                    this.delayTarget = this.delayTarget - Math.max(1, (int)(delayCap * dt / 10.0F));
                }
            }

            if (this.wasHighPing && !this.highPing && Math.abs(this.delay - this.delayTarget) < 10 && Math.abs(delayCap - this.delayTarget) < 10) {
                this.wasHighPing = false;
                this.delayTarget = 500;
            }
        }
    }

    private void interpolationDataCurrentAdd(BaseVehicle vehicle) {
        VehicleInterpolationData d = pool.isEmpty() ? new VehicleInterpolationData() : pool.pop();
        d.time = GameTime.getServerTimeMills() - this.delay;
        d.x = vehicle.jniTransform.origin.x + WorldSimulation.instance.offsetX;
        d.y = vehicle.jniTransform.origin.z + WorldSimulation.instance.offsetY;
        d.z = vehicle.jniTransform.origin.y;
        Quaternionf q = vehicle.jniTransform.getRotation(new Quaternionf());
        d.qx = q.x;
        d.qy = q.y;
        d.qz = q.z;
        d.qw = q.w;
        d.vx = vehicle.jniLinearVelocity.x;
        d.vy = vehicle.jniLinearVelocity.y;
        d.vz = vehicle.jniLinearVelocity.z;
        d.engineSpeed = (float)vehicle.engineSpeed;
        d.throttle = vehicle.throttle;
        d.setNumWheels((short)vehicle.wheelInfo.length);

        for (int i = 0; i < d.wheelsCount; i++) {
            d.wheelSteering[i] = vehicle.wheelInfo[i].steering;
            d.wheelRotation[i] = vehicle.wheelInfo[i].rotation;
            d.wheelSkidInfo[i] = vehicle.wheelInfo[i].skidInfo;
            if (vehicle.wheelInfo[i].suspensionLength > 0.0F) {
                d.wheelSuspensionLength[i] = vehicle.wheelInfo[i].suspensionLength;
            } else {
                d.wheelSuspensionLength[i] = 0.3F;
            }
        }

        this.buffer.add(d);
    }

    public void interpolationDataAdd(BaseVehicle vehicle, VehicleInterpolationData data, long currentTime) {
        if (this.buffer.isEmpty()) {
            this.interpolationDataCurrentAdd(vehicle);
        }

        VehicleInterpolationData d = pool.isEmpty() ? new VehicleInterpolationData() : pool.pop();
        d.copy(data);
        this.buffer.add(d);
        this.update(currentTime);
    }

    public boolean interpolationDataGet(float[] buf1, float[] buf2) {
        long time = WorldSimulation.instance.time - this.delay;
        return this.interpolationDataGet(buf1, buf2, time);
    }

    public VehicleInterpolationData getLastAddedInterpolationPoint() {
        try {
            return this.buffer.last();
        } catch (Exception var2) {
            return null;
        }
    }

    public void setDelayLength(float d) {
        this.delayTarget = (int)(d * 500.0F);
    }

    public boolean isDelayLengthIncreased() {
        return this.delayTarget > 500;
    }

    public boolean interpolationDataGet(float[] buf1, float[] buf2, long time) {
        this.getPointUpdate();
        temp.time = time;
        VehicleInterpolationData data_b = this.buffer.higher(temp);
        VehicleInterpolationData data_a = this.buffer.floor(temp);
        if (this.buffering) {
            if (this.buffer.size() < 2 || data_b == null || data_a == null) {
                return false;
            }

            this.buffering = false;
        } else if (this.buffer.isEmpty()) {
            this.reset();
            return false;
        }

        int n = 0;
        if (data_b == null) {
            if (data_a == null) {
                this.reset();
                return false;
            } else {
                this.wasNull = true;
                this.lastTimeA = -1L;
                this.lastTime = data_a.time;
                this.recountedTime = this.lastTime;
                buf2[0] = data_a.engineSpeed;
                buf2[1] = data_a.throttle;
                buf1[n++] = data_a.x;
                buf1[n++] = data_a.y;
                buf1[n++] = data_a.z;
                buf1[n++] = data_a.qx;
                buf1[n++] = data_a.qy;
                buf1[n++] = data_a.qz;
                buf1[n++] = data_a.qw;
                buf1[n++] = data_a.vx;
                buf1[n++] = data_a.vy;
                buf1[n++] = data_a.vz;
                buf1[n++] = data_a.wheelsCount;

                for (int i = 0; i < data_a.wheelsCount; i++) {
                    buf1[n++] = data_a.wheelSteering[i];
                    buf1[n++] = data_a.wheelRotation[i];
                    buf1[n++] = data_a.wheelSkidInfo[i];
                    buf1[n++] = data_a.wheelSuspensionLength[i];
                }

                this.lastBuf1 = new float[buf1.length];

                for (int i = 0; i < buf1.length; i++) {
                    this.lastBuf1[i] = buf1[i];
                }

                this.reset();
                return true;
            }
        } else if (data_a != null && (Math.abs(data_b.time - data_a.time) >= 10L || this.wasNull) && (!this.wasNull || data_b.time - this.lastTime >= 10L)) {
            if (this.lastTimeA == -1L) {
                this.lastTimeA = data_a.time;
            }

            if (this.lastTimeA != data_a.time && this.wasNull) {
                this.lastTimeA = data_a.time;
                this.wasNull = false;
            }

            float tempTime_m;
            if (this.wasNull) {
                if (time - this.recountedTime > 20L) {
                    long timeChunk = (data_b.time - this.lastTime) / 7L;
                    this.recountedTime += timeChunk > 20L ? timeChunk : 20L;
                } else {
                    this.recountedTime = time;
                }

                tempTime_m = (float)(this.recountedTime - this.lastTime) / (float)(data_b.time - this.lastTime);
            } else {
                tempTime_m = (float)(time - data_a.time) / (float)(data_b.time - data_a.time);
            }

            float time_m = tempTime_m;
            buf2[0] = (data_b.engineSpeed - data_a.engineSpeed) * tempTime_m + data_a.engineSpeed;
            buf2[1] = (data_b.throttle - data_a.throttle) * tempTime_m + data_a.throttle;
            if (this.wasNull) {
                buf1[n] = (data_b.x - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                buf1[++n] = (data_b.y - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                buf1[++n] = (data_b.z - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                n++;
                tempQuaternionA.set(data_a.qx, data_a.qy, data_a.qz, data_a.qw);
                tempQuaternionB.set(data_b.qx, data_b.qy, data_b.qz, data_b.qw);
                tempQuaternionA.nlerp(tempQuaternionB, tempTime_m);
                buf1[n++] = tempQuaternionA.x;
                buf1[n++] = tempQuaternionA.y;
                buf1[n++] = tempQuaternionA.z;
                buf1[n++] = tempQuaternionA.w;
                buf1[n] = (data_b.vx - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                buf1[++n] = (data_b.vy - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                buf1[++n] = (data_b.vz - this.lastBuf1[n]) * tempTime_m + this.lastBuf1[n];
                int var60 = ++n;
                n++;
                buf1[var60] = data_b.wheelsCount;

                for (int i = 0; i < data_b.wheelsCount; i++) {
                    buf1[n++] = (data_b.wheelSteering[i] - data_a.wheelSteering[i]) * time_m + data_a.wheelSteering[i];
                    buf1[n++] = (data_b.wheelRotation[i] - data_a.wheelRotation[i]) * time_m + data_a.wheelRotation[i];
                    buf1[n++] = (data_b.wheelSkidInfo[i] - data_a.wheelSkidInfo[i]) * time_m + data_a.wheelSkidInfo[i];
                    buf1[n++] = (data_b.wheelSuspensionLength[i] - data_a.wheelSuspensionLength[i]) * time_m + data_a.wheelSuspensionLength[i];
                }
            } else {
                buf1[n++] = (data_b.x - data_a.x) * tempTime_m + data_a.x;
                buf1[n++] = (data_b.y - data_a.y) * tempTime_m + data_a.y;
                buf1[n++] = (data_b.z - data_a.z) * tempTime_m + data_a.z;
                tempQuaternionA.set(data_a.qx, data_a.qy, data_a.qz, data_a.qw);
                tempQuaternionB.set(data_b.qx, data_b.qy, data_b.qz, data_b.qw);
                tempQuaternionA.nlerp(tempQuaternionB, tempTime_m);
                buf1[n++] = tempQuaternionA.x;
                buf1[n++] = tempQuaternionA.y;
                buf1[n++] = tempQuaternionA.z;
                buf1[n++] = tempQuaternionA.w;
                buf1[n++] = (data_b.vx - data_a.vx) * tempTime_m + data_a.vx;
                buf1[n++] = (data_b.vy - data_a.vy) * tempTime_m + data_a.vy;
                buf1[n++] = (data_b.vz - data_a.vz) * tempTime_m + data_a.vz;
                buf1[n++] = data_b.wheelsCount;

                for (int i = 0; i < data_b.wheelsCount; i++) {
                    buf1[n++] = (data_b.wheelSteering[i] - data_a.wheelSteering[i]) * time_m + data_a.wheelSteering[i];
                    buf1[n++] = (data_b.wheelRotation[i] - data_a.wheelRotation[i]) * time_m + data_a.wheelRotation[i];
                    buf1[n++] = (data_b.wheelSkidInfo[i] - data_a.wheelSkidInfo[i]) * time_m + data_a.wheelSkidInfo[i];
                    buf1[n++] = (data_b.wheelSuspensionLength[i] - data_a.wheelSuspensionLength[i]) * time_m + data_a.wheelSuspensionLength[i];
                }
            }

            this.wasNull = false;
            return true;
        } else {
            return false;
        }
    }
}
