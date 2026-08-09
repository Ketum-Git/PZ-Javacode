// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.joml.Quaternionf;
import zombie.GameTime;
import zombie.core.physics.WorldSimulation;
import zombie.network.GameClient;

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
        VehicleInterpolationData dataA = this.buffer.floor(temp);

        for (VehicleInterpolationData data : this.buffer) {
            if (time - data.time > this.history && data != dataA) {
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

    void getPointUpdate(VehicleInterpolation towedByInterpolation) {
        if (towedByInterpolation != null) {
            this.delay = towedByInterpolation.delay;
            this.history = towedByInterpolation.history;
            this.delayTarget = towedByInterpolation.delayTarget;
            this.highPing = towedByInterpolation.highPing;
            this.wasHighPing = towedByInterpolation.wasHighPing;
        } else if (this.getPointUpdateTimeout++ >= 10) {
            float dt = WorldSimulation.instance.periodSec * 10.0F;
            this.getPointUpdateTimeout = 0;
            int lastPing = GameClient.connection.getLastPing();
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
        d.engineSpeed = (float)vehicle.getEngineSpeed();
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
        return this.interpolationDataGet(buf1, buf2, null);
    }

    public boolean interpolationDataGet(float[] buf1, float[] buf2, VehicleInterpolation towedByInterpolation) {
        if (towedByInterpolation != null) {
            this.delay = towedByInterpolation.delay;
        }

        long time = WorldSimulation.instance.time - this.delay;
        return this.interpolationDataGet(buf1, buf2, time, towedByInterpolation);
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

    public boolean interpolationDataGet(float[] buf1, float[] buf2, long time, VehicleInterpolation towedByInterpolation) {
        this.getPointUpdate(towedByInterpolation);
        temp.time = time;
        VehicleInterpolationData dataB = this.buffer.higher(temp);
        VehicleInterpolationData dataA = this.buffer.floor(temp);
        if (this.buffering) {
            if (this.buffer.size() < 2 || dataB == null || dataA == null) {
                return false;
            }

            this.buffering = false;
        } else if (this.buffer.isEmpty()) {
            this.reset();
            return false;
        }

        int n = 0;
        if (dataB == null) {
            if (dataA == null) {
                this.reset();
                return false;
            } else {
                this.wasNull = true;
                this.lastTimeA = -1L;
                this.lastTime = dataA.time;
                this.recountedTime = this.lastTime;
                buf2[0] = dataA.engineSpeed;
                buf2[1] = dataA.throttle;
                buf1[n++] = dataA.x;
                buf1[n++] = dataA.y;
                buf1[n++] = dataA.z;
                buf1[n++] = dataA.qx;
                buf1[n++] = dataA.qy;
                buf1[n++] = dataA.qz;
                buf1[n++] = dataA.qw;
                buf1[n++] = dataA.vx;
                buf1[n++] = dataA.vy;
                buf1[n++] = dataA.vz;
                buf1[n++] = dataA.wheelsCount;

                for (int i = 0; i < dataA.wheelsCount; i++) {
                    buf1[n++] = dataA.wheelSteering[i];
                    buf1[n++] = dataA.wheelRotation[i];
                    buf1[n++] = dataA.wheelSkidInfo[i];
                    buf1[n++] = dataA.wheelSuspensionLength[i];
                }

                this.lastBuf1 = new float[buf1.length];

                for (int i = 0; i < buf1.length; i++) {
                    this.lastBuf1[i] = buf1[i];
                }

                this.reset();
                return true;
            }
        } else if (dataA != null && (Math.abs(dataB.time - dataA.time) >= 10L || this.wasNull) && (!this.wasNull || dataB.time - this.lastTime >= 10L)) {
            if (this.lastTimeA == -1L) {
                this.lastTimeA = dataA.time;
            }

            if (this.lastTimeA != dataA.time && this.wasNull) {
                this.lastTimeA = dataA.time;
                this.wasNull = false;
            }

            float tempTimeM;
            if (this.wasNull) {
                if (time - this.recountedTime > 20L) {
                    long timeChunk = (dataB.time - this.lastTime) / 7L;
                    this.recountedTime += timeChunk > 20L ? timeChunk : 20L;
                } else {
                    this.recountedTime = time;
                }

                tempTimeM = (float)(this.recountedTime - this.lastTime) / (float)(dataB.time - this.lastTime);
            } else {
                tempTimeM = (float)(time - dataA.time) / (float)(dataB.time - dataA.time);
            }

            float timeM = tempTimeM;
            buf2[0] = (dataB.engineSpeed - dataA.engineSpeed) * tempTimeM + dataA.engineSpeed;
            buf2[1] = (dataB.throttle - dataA.throttle) * tempTimeM + dataA.throttle;
            if (this.wasNull) {
                buf1[n] = (dataB.x - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                buf1[++n] = (dataB.y - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                buf1[++n] = (dataB.z - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                n++;
                tempQuaternionA.set(dataA.qx, dataA.qy, dataA.qz, dataA.qw);
                tempQuaternionB.set(dataB.qx, dataB.qy, dataB.qz, dataB.qw);
                tempQuaternionA.nlerp(tempQuaternionB, tempTimeM);
                buf1[n++] = tempQuaternionA.x;
                buf1[n++] = tempQuaternionA.y;
                buf1[n++] = tempQuaternionA.z;
                buf1[n++] = tempQuaternionA.w;
                buf1[n] = (dataB.vx - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                buf1[++n] = (dataB.vy - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                buf1[++n] = (dataB.vz - this.lastBuf1[n]) * tempTimeM + this.lastBuf1[n];
                int var61 = ++n;
                n++;
                buf1[var61] = dataB.wheelsCount;

                for (int i = 0; i < dataB.wheelsCount; i++) {
                    buf1[n++] = (dataB.wheelSteering[i] - dataA.wheelSteering[i]) * timeM + dataA.wheelSteering[i];
                    buf1[n++] = (dataB.wheelRotation[i] - dataA.wheelRotation[i]) * timeM + dataA.wheelRotation[i];
                    buf1[n++] = (dataB.wheelSkidInfo[i] - dataA.wheelSkidInfo[i]) * timeM + dataA.wheelSkidInfo[i];
                    buf1[n++] = (dataB.wheelSuspensionLength[i] - dataA.wheelSuspensionLength[i]) * timeM + dataA.wheelSuspensionLength[i];
                }
            } else {
                buf1[n++] = (dataB.x - dataA.x) * tempTimeM + dataA.x;
                buf1[n++] = (dataB.y - dataA.y) * tempTimeM + dataA.y;
                buf1[n++] = (dataB.z - dataA.z) * tempTimeM + dataA.z;
                tempQuaternionA.set(dataA.qx, dataA.qy, dataA.qz, dataA.qw);
                tempQuaternionB.set(dataB.qx, dataB.qy, dataB.qz, dataB.qw);
                tempQuaternionA.nlerp(tempQuaternionB, tempTimeM);
                buf1[n++] = tempQuaternionA.x;
                buf1[n++] = tempQuaternionA.y;
                buf1[n++] = tempQuaternionA.z;
                buf1[n++] = tempQuaternionA.w;
                buf1[n++] = (dataB.vx - dataA.vx) * tempTimeM + dataA.vx;
                buf1[n++] = (dataB.vy - dataA.vy) * tempTimeM + dataA.vy;
                buf1[n++] = (dataB.vz - dataA.vz) * tempTimeM + dataA.vz;
                buf1[n++] = dataB.wheelsCount;

                for (int i = 0; i < dataB.wheelsCount; i++) {
                    buf1[n++] = (dataB.wheelSteering[i] - dataA.wheelSteering[i]) * timeM + dataA.wheelSteering[i];
                    buf1[n++] = (dataB.wheelRotation[i] - dataA.wheelRotation[i]) * timeM + dataA.wheelRotation[i];
                    buf1[n++] = (dataB.wheelSkidInfo[i] - dataA.wheelSkidInfo[i]) * timeM + dataA.wheelSkidInfo[i];
                    buf1[n++] = (dataB.wheelSuspensionLength[i] - dataA.wheelSuspensionLength[i]) * timeM + dataA.wheelSuspensionLength[i];
                }
            }

            this.wasNull = false;
            return true;
        } else {
            return false;
        }
    }
}
