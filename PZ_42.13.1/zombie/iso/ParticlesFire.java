// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Objects;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.lwjglx.BufferUtils;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.interfaces.ITexture;
import zombie.iso.weather.ClimateManager;

public final class ParticlesFire extends Particles {
    int maxParticles = 1000000;
    int maxVortices = 4;
    int particlesDataBuffer;
    ByteBuffer particuleData;
    private final Texture texFireSmoke;
    private final Texture texFlameFire;
    public FireShader effectFire;
    public SmokeShader effectSmoke;
    public Shader effectVape;
    float windX;
    float windY;
    private static ParticlesFire instance;
    private final ParticlesArray<ParticlesFire.Particle> particles;
    private final ArrayList<ParticlesFire.Zone> zones;
    private final int intensityFire = 0;
    private final int intensitySmoke = 0;
    private final int intensitySteam = 0;
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static synchronized ParticlesFire getInstance() {
        if (instance == null) {
            instance = new ParticlesFire();
        }

        return instance;
    }

    public ParticlesFire() {
        this.particles = new ParticlesArray<>();
        this.zones = new ArrayList<>();
        this.particuleData = BufferUtils.createByteBuffer(this.maxParticles * 4 * 4);
        this.texFireSmoke = Texture.getSharedTexture("media/textures/FireSmokes.png");
        this.texFlameFire = Texture.getSharedTexture("media/textures/FireFlame.png");
        this.zones.clear();
        float cx = PZMath.fastfloor(IsoCamera.frameState.offX + IsoCamera.frameState.offscreenWidth / 2);
        float cy = PZMath.fastfloor(IsoCamera.frameState.offY + IsoCamera.frameState.offscreenHeight / 2);
        this.zones.add(new ParticlesFire.Zone(10, cx - 30.0F, cy - 10.0F, cx + 30.0F, cy + 10.0F));
        this.zones.add(new ParticlesFire.Zone(10, cx - 200.0F, cy, 50.0F));
        this.zones.add(new ParticlesFire.Zone(40, cx + 200.0F, cy, 100.0F));
        this.zones.add(new ParticlesFire.Zone(60, cx - 150.0F, cy - 300.0F, cx + 250.0F, cy - 300.0F, 10.0F));
        this.zones.add(new ParticlesFire.Zone(10, cx - 350.0F, cy - 200.0F, cx - 350.0F, cy - 300.0F, 10.0F));
    }

    private void ParticlesProcess() {
        for (int k = 0; k < this.zones.size(); k++) {
            ParticlesFire.Zone z = this.zones.get(k);
            int newParticles = (int)Math.ceil((z.intensity - z.currentParticles) * 0.1F);
            if (z.type == ParticlesFire.ZoneType.Rectangle) {
                for (int i = 0; i < newParticles; i++) {
                    ParticlesFire.Particle p = new ParticlesFire.Particle();
                    p.x = Rand.Next(z.x0, z.x1);
                    p.y = Rand.Next(z.y0, z.y1);
                    p.vx = Rand.Next(-3.0F, 3.0F);
                    p.vy = Rand.Next(1.0F, 5.0F);
                    p.tShift = 0.0F;
                    p.id = Rand.Next(-1000000.0F, 1000000.0F);
                    p.zone = z;
                    z.currentParticles++;
                    this.particles.addParticle(p);
                }
            }

            if (z.type == ParticlesFire.ZoneType.Circle) {
                for (int i = 0; i < newParticles; i++) {
                    ParticlesFire.Particle p = new ParticlesFire.Particle();
                    float a = Rand.Next(0.0F, (float) (Math.PI * 2));
                    float r = Rand.Next(0.0F, z.r);
                    p.x = (float)(z.x0 + r * Math.cos(a));
                    p.y = (float)(z.y0 + r * Math.sin(a));
                    p.vx = Rand.Next(-3.0F, 3.0F);
                    p.vy = Rand.Next(1.0F, 5.0F);
                    p.tShift = 0.0F;
                    p.id = Rand.Next(-1000000.0F, 1000000.0F);
                    p.zone = z;
                    z.currentParticles++;
                    this.particles.addParticle(p);
                }
            }

            if (z.type == ParticlesFire.ZoneType.Line) {
                for (int i = 0; i < newParticles; i++) {
                    ParticlesFire.Particle p = new ParticlesFire.Particle();
                    float a = Rand.Next(0.0F, (float) (Math.PI * 2));
                    float r = Rand.Next(0.0F, z.r);
                    float h = Rand.Next(0.0F, 1.0F);
                    p.x = (float)(z.x0 * h + z.x1 * (1.0F - h) + r * Math.cos(a));
                    p.y = (float)(z.y0 * h + z.y1 * (1.0F - h) + r * Math.sin(a));
                    p.vx = Rand.Next(-3.0F, 3.0F);
                    p.vy = Rand.Next(1.0F, 5.0F);
                    p.tShift = 0.0F;
                    p.id = Rand.Next(-1000000.0F, 1000000.0F);
                    p.zone = z;
                    z.currentParticles++;
                    this.particles.addParticle(p);
                }
            }

            if (newParticles < 0) {
                for (int i = 0; i < -newParticles; i++) {
                    z.currentParticles--;
                    this.particles.deleteParticle(Rand.Next(0, this.particles.getCount() + 1));
                }
            }
        }
    }

    public FloatBuffer getParametersFire() {
        this.floatBuffer.clear();
        this.floatBuffer.put(this.windX);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(this.windY);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.put(0.0F);
        this.floatBuffer.flip();
        return this.floatBuffer;
    }

    public int getFireShaderID() {
        return this.effectFire.getID();
    }

    public int getSmokeShaderID() {
        return this.effectSmoke.getID();
    }

    public int getVapeShaderID() {
        return this.effectVape.getID();
    }

    public ITexture getFireFlameTexture() {
        return this.texFlameFire;
    }

    public ITexture getFireSmokeTexture() {
        return this.texFireSmoke;
    }

    @Override
    public void reloadShader() {
        RenderThread.invokeOnRenderContext(() -> {
            this.effectFire = new FireShader("fire");
            this.effectSmoke = new SmokeShader("smoke");
            this.effectVape = new Shader("vape");
        });
    }

    @Override
    void createParticleBuffers() {
        this.particlesDataBuffer = funcs.glGenBuffers();
        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        funcs.glBufferData(34962, this.maxParticles * 4 * 4, 35044);
    }

    @Override
    void destroyParticleBuffers() {
        funcs.glDeleteBuffers(this.particlesDataBuffer);
    }

    @Override
    void updateParticleParams() {
        float FireWindAngle = ClimateManager.getInstance().getWindAngleIntensity();
        float FireWindIntensity = ClimateManager.getInstance().getWindIntensity();
        this.windX = (float)Math.sin(FireWindAngle * 6.0F) * FireWindIntensity;
        this.windY = (float)Math.cos(FireWindAngle * 6.0F) * FireWindIntensity;
        this.ParticlesProcess();
        if (this.particles.getNeedToUpdate()) {
            this.particles.defragmentParticle();
            this.particuleData.clear();

            for (int i = 0; i < this.particles.size(); i++) {
                ParticlesFire.Particle p = this.particles.get(i);
                if (p != null) {
                    this.particuleData.putFloat(p.x);
                    this.particuleData.putFloat(p.y);
                    this.particuleData.putFloat(p.id);
                    this.particuleData.putFloat((float)i / this.particles.size());
                }
            }

            this.particuleData.flip();
        }

        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        funcs.glBufferData(34962, this.particuleData, 35040);
        GL20.glEnableVertexAttribArray(1);
        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        GL20.glVertexAttribPointer(1, 4, 5126, false, 0, 0L);
        GL33.glVertexAttribDivisor(1, 1);
    }

    @Override
    int getParticleCount() {
        return this.particles.getCount();
    }

    public class Particle {
        float id;
        float x;
        float y;
        float tShift;
        float vx;
        float vy;
        ParticlesFire.Zone zone;

        public Particle() {
            Objects.requireNonNull(ParticlesFire.this);
            super();
        }
    }

    public class Vortice {
        float x;
        float y;
        float z;
        float size;
        float vx;
        float vy;
        float speed;
        int life;
        int lifeTime;
        ParticlesFire.Zone zone;

        public Vortice() {
            Objects.requireNonNull(ParticlesFire.this);
            super();
        }
    }

    public class Zone {
        ParticlesFire.ZoneType type;
        int intensity;
        int currentParticles;
        float x0;
        float y0;
        float x1;
        float y1;
        float r;
        float fireIntensity;
        float smokeIntensity;
        float sparksIntensity;
        float vortices;
        float vorticeSpeed;
        float area;
        float temperature;
        float centerX;
        float centerY;
        float centerRp2;
        float currentVorticesCount;

        Zone(final int intensity, final float x, final float y, final float r) {
            Objects.requireNonNull(ParticlesFire.this);
            super();
            this.type = ParticlesFire.ZoneType.Circle;
            this.intensity = intensity;
            this.currentParticles = 0;
            this.x0 = x;
            this.y0 = y;
            this.r = r;
            this.area = (float)(Math.PI * r * r);
            this.vortices = this.intensity * 0.3F;
            this.vorticeSpeed = 0.5F;
            this.temperature = 2000.0F;
            this.centerX = x;
            this.centerY = y;
            this.centerRp2 = r * r;
        }

        Zone(final int intensity, final float x0, final float y0, final float x1, final float y1) {
            Objects.requireNonNull(ParticlesFire.this);
            super();
            this.type = ParticlesFire.ZoneType.Rectangle;
            this.intensity = intensity;
            this.currentParticles = 0;
            if (x0 < x1) {
                this.x0 = x0;
                this.x1 = x1;
            } else {
                this.x1 = x0;
                this.x0 = x1;
            }

            if (y0 < y1) {
                this.y0 = y0;
                this.y1 = y1;
            } else {
                this.y1 = y0;
                this.y0 = y1;
            }

            this.area = (this.x1 - this.x0) * (this.y1 - this.y0);
            this.vortices = this.intensity * 0.3F;
            this.vorticeSpeed = 0.5F;
            this.temperature = 2000.0F;
            this.centerX = (this.x0 + this.x1) * 0.5F;
            this.centerY = (this.y0 + this.y1) * 0.5F;
            this.centerRp2 = (this.x1 - this.x0) * (this.x1 - this.x0);
        }

        Zone(final int intensity, final float x0, final float y0, final float x1, final float y1, final float r) {
            Objects.requireNonNull(ParticlesFire.this);
            super();
            this.type = ParticlesFire.ZoneType.Line;
            this.intensity = intensity;
            this.currentParticles = 0;
            if (x0 < x1) {
                this.x0 = x0;
                this.x1 = x1;
                this.y0 = y0;
                this.y1 = y1;
            } else {
                this.x1 = x0;
                this.x0 = x1;
                this.y1 = y0;
                this.y0 = y1;
            }

            this.r = r;
            this.area = (float)(this.r * Math.sqrt(Math.pow(x0 - x1, 2.0) + Math.pow(y0 - y1, 2.0)));
            this.vortices = this.intensity * 0.3F;
            this.vorticeSpeed = 0.5F;
            this.temperature = 2000.0F;
            this.centerX = (this.x0 + this.x1) * 0.5F;
            this.centerY = (this.y0 + this.y1) * 0.5F;
            this.centerRp2 = (this.x1 - this.x0 + r) * (this.x1 - this.x0 + r) * 100.0F;
        }
    }

    static enum ZoneType {
        Rectangle,
        Circle,
        Line;
    }
}
