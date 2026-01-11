// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.iso.Vector2;

public class NetworkCharacter {
    float minMovement;
    float maxMovement;
    long deltaTime;
    public final NetworkCharacter.Transform transform = new NetworkCharacter.Transform();
    final Vector2 movement = new Vector2();
    final NetworkCharacter.Point d1 = new NetworkCharacter.Point();
    final NetworkCharacter.Point d2 = new NetworkCharacter.Point();

    public NetworkCharacter() {
        this.minMovement = 0.075F;
        this.maxMovement = 0.5F;
        this.deltaTime = 10L;
    }

    NetworkCharacter(float minMovement, float maxMovement, long deltaTime) {
        this.minMovement = minMovement;
        this.maxMovement = maxMovement;
        this.deltaTime = deltaTime;
    }

    public void updateTransform(float px, float py, float rx, float ry) {
        this.transform.position.x = px;
        this.transform.position.y = py;
        this.transform.rotation.x = rx;
        this.transform.rotation.y = ry;
    }

    public void updateInterpolationPoint(int t, float px, float py, float rx, float ry) {
        if (this.d2.t == 0) {
            this.updateTransform(px, py, rx, ry);
        }

        this.d2.t = t;
        this.d2.px = px;
        this.d2.py = py;
        this.d2.rx = rx;
        this.d2.ry = ry;
    }

    public void updatePointInternal(float px, float py, float rx, float ry) {
        this.d1.px = px;
        this.d1.py = py;
        this.d1.rx = rx;
        this.d1.ry = ry;
    }

    public void updateExtrapolationPoint(int t, float px, float py, float rx, float ry) {
        if (t > this.d1.t) {
            this.d2.t = this.d1.t;
            this.d2.px = this.d1.px;
            this.d2.py = this.d1.py;
            this.d2.rx = this.d1.rx;
            this.d2.ry = this.d1.ry;
            this.d1.t = t;
            this.d1.px = px;
            this.d1.py = py;
            this.d1.rx = rx;
            this.d1.ry = ry;
        }
    }

    void extrapolate(int t) {
        float f = (float)(t - this.d1.t) / (this.d1.t - this.d2.t);
        float vX = this.d1.px - this.d2.px;
        float vY = this.d1.py - this.d2.py;
        this.movement.x = vX * f;
        this.movement.y = vY * f;
        if (vX > this.minMovement || vY > this.minMovement || -vX > this.minMovement || -vY > this.minMovement) {
            this.transform.moving = true;
            this.transform.rotation.x = this.movement.x;
            this.transform.rotation.y = this.movement.y;
            this.transform.rotation.normalize();
        }

        this.transform.position.x = this.d1.px + this.movement.x;
        this.transform.position.y = this.d1.py + this.movement.y;
        this.transform.operation = NetworkCharacter.Operation.EXTRAPOLATION;
    }

    void extrapolateInternal(int t, float px, float py) {
        float f = (float)(t - this.d1.t) / (t - this.d1.t);
        float vX = px - this.d1.px;
        float vY = py - this.d1.py;
        this.movement.x = vX * f;
        this.movement.y = vY * f;
        if (this.movement.getLength() > this.maxMovement) {
            this.movement.setLength(this.maxMovement);
        }

        if (vX > this.minMovement || vY > this.minMovement || -vX > this.minMovement || -vY > this.minMovement) {
            this.transform.moving = true;
            this.transform.rotation.x = this.movement.x;
            this.transform.rotation.y = this.movement.y;
            this.transform.rotation.normalize();
        }

        this.transform.position.x = px + this.movement.x;
        this.transform.position.y = py + this.movement.y;
        this.transform.operation = NetworkCharacter.Operation.EXTRAPOLATION;
    }

    void interpolate(int t) {
        float f = (float)(t - this.d1.t) / (this.d2.t - this.d1.t);
        float vX = this.d2.px - this.d1.px;
        float vY = this.d2.py - this.d1.py;
        this.movement.x = vX * f;
        this.movement.y = vY * f;
        if (this.movement.getLength() > this.maxMovement) {
            this.movement.setLength(this.maxMovement);
        }

        if (vX > this.minMovement || vY > this.minMovement || -vX > this.minMovement || -vY > this.minMovement) {
            this.transform.moving = true;
            this.transform.rotation.x = this.movement.x;
            this.transform.rotation.y = this.movement.y;
            this.transform.rotation.normalize();
        }

        this.transform.position.x = this.d1.px + this.movement.x;
        this.transform.position.y = this.d1.py + this.movement.y;
        this.transform.operation = NetworkCharacter.Operation.INTERPOLATION;
    }

    public NetworkCharacter.Transform predict(int dt, int t, float px, float py, float rx, float ry) {
        this.transform.moving = false;
        this.transform.operation = NetworkCharacter.Operation.NONE;
        this.transform.time = t + dt;
        this.updateExtrapolationPoint(t, px, py, rx, ry);
        if (this.d1.t != 0 && this.d2.t != 0) {
            this.extrapolate(dt + t);
        } else {
            this.updateTransform(px, py, rx, ry);
        }

        return this.transform;
    }

    public NetworkCharacter.Transform reconstruct(int t, float px, float py, float rx, float ry) {
        this.transform.moving = false;
        this.transform.operation = NetworkCharacter.Operation.NONE;
        if (this.d2.t != 0) {
            if (t + this.deltaTime <= this.d2.t) {
                this.updatePointInternal(px, py, rx, ry);
                if (this.d1.t != 0 && this.d1.t != t) {
                    this.interpolate(t);
                }

                this.d1.t = t;
            } else if (t > this.d2.t && t < this.d2.t + 2000) {
                this.extrapolateInternal(t, px, py);
                this.updatePointInternal(px, py, rx, ry);
                this.d1.t = t;
            }
        }

        return this.transform;
    }

    public void checkReset(int t) {
        if (t > 2000 + this.d2.t) {
            this.reset();
        }
    }

    public void checkResetPlayer(int t) {
        if (t > 2000 + this.d1.t) {
            this.reset();
        }
    }

    public void reset() {
        this.d1.t = 0;
        this.d1.px = 0.0F;
        this.d1.py = 0.0F;
        this.d1.rx = 0.0F;
        this.d1.ry = 0.0F;
        this.d2.t = 0;
        this.d2.px = 0.0F;
        this.d2.py = 0.0F;
        this.d2.rx = 0.0F;
        this.d2.ry = 0.0F;
    }

    public static enum Operation {
        INTERPOLATION,
        EXTRAPOLATION,
        NONE;
    }

    static class Point {
        public float px = 0.0F;
        public float py = 0.0F;
        public float rx = 0.0F;
        public float ry = 0.0F;
        public int t = 0;
    }

    public static class Transform {
        public Vector2 position = new Vector2();
        public Vector2 rotation = new Vector2();
        public NetworkCharacter.Operation operation = NetworkCharacter.Operation.NONE;
        public boolean moving = false;
        public int time = 0;
    }
}
