// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class FBORenderTracerEffects {
    private static FBORenderTracerEffects instance;
    private final ArrayList<FBORenderTracerEffects.Effect> effects = new ArrayList<>();
    private final ObjectPool<FBORenderTracerEffects.Effect> effectPool = new ObjectPool<>(FBORenderTracerEffects.Effect::new);
    private final ObjectPool<FBORenderTracerEffects.Drawer> drawerPool = new ObjectPool<>(FBORenderTracerEffects.Drawer::new);
    public final HashMap<IsoGameCharacter, Matrix4f> playerWeaponTransform = new HashMap<>();
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    final FBORenderTracerEffects.DoubleConfigOption1 startRadius = new FBORenderTracerEffects.DoubleConfigOption1("StartRadius", 0.001F, 0.02F, 0.0025F);
    final FBORenderTracerEffects.DoubleConfigOption1 endRadius = new FBORenderTracerEffects.DoubleConfigOption1("EndRadius", 0.001F, 0.02F, 0.01F);
    final FBORenderTracerEffects.DoubleConfigOption1 length = new FBORenderTracerEffects.DoubleConfigOption1("Length", 0.1F, 10.0, 1.0);
    final FBORenderTracerEffects.DoubleConfigOption1 speed = new FBORenderTracerEffects.DoubleConfigOption1("Speed", 0.001F, 0.5, 0.075F);
    final FBORenderTracerEffects.DoubleConfigOption1 red = new FBORenderTracerEffects.DoubleConfigOption1("Red", 0.0, 1.0, 1.0);
    final FBORenderTracerEffects.DoubleConfigOption1 green = new FBORenderTracerEffects.DoubleConfigOption1("Green", 0.0, 1.0, 1.0);
    final FBORenderTracerEffects.DoubleConfigOption1 blue = new FBORenderTracerEffects.DoubleConfigOption1("Blue", 0.0, 1.0, 1.0);
    final FBORenderTracerEffects.DoubleConfigOption1 alpha = new FBORenderTracerEffects.DoubleConfigOption1("Alpha", 0.0, 1.0, 1.0);

    public static FBORenderTracerEffects getInstance() {
        if (instance == null) {
            instance = new FBORenderTracerEffects();
        }

        return instance;
    }

    private FBORenderTracerEffects() {
        this.load();
    }

    public void releaseWeaponTransform(IsoGameCharacter chr) {
        Matrix4f mtx = this.playerWeaponTransform.remove(chr);
        if (mtx != null) {
            BaseVehicle.releaseMatrix4f(mtx);
        }
    }

    public void storeWeaponTransform(IsoGameCharacter chr, Matrix4f xfrm) {
        if (DebugOptions.instance.fboRenderChunk.bulletTracers.getValue()) {
            Matrix4f mtx = this.playerWeaponTransform.remove(chr);
            if (mtx != null) {
                BaseVehicle.releaseMatrix4f(mtx);
            }

            if (chr != null && chr.primaryHandModel != null && chr.primaryHandModel.modelScript != null) {
                ModelAttachment attachment = chr.primaryHandModel.modelScript.getAttachmentById("muzzle");
                if (attachment != null) {
                    mtx = BaseVehicle.allocMatrix4f();
                    mtx.set(xfrm);
                    mtx.transpose();
                    Matrix4f m = BaseVehicle.allocMatrix4f();
                    ModelInstanceRenderData.makeAttachmentTransform(attachment, m);
                    mtx.mul(m);
                    BaseVehicle.releaseMatrix4f(m);
                    this.playerWeaponTransform.put(chr, mtx);
                }
            }
        }
    }

    public void addEffect(IsoGameCharacter chr, float range) {
        if (chr != null && chr.getAnimationPlayer().isReady()) {
            if (this.playerWeaponTransform.containsKey(chr)) {
                FBORenderTracerEffects.Effect e = this.effectPool.alloc();
                e.x0 = chr.getX();
                e.y0 = chr.getY();
                e.z0 = chr.getZ();
                e.angle = chr.getAnimationPlayer().getRenderedAngle();
                e.range = range;
                e.r1 = (float)this.red.getValue();
                e.g1 = (float)this.green.getValue();
                e.b1 = (float)this.blue.getValue();
                e.a1 = (float)this.alpha.getValue();
                e.thickness0 = (float)this.startRadius.getValue();
                e.thickness1 = (float)this.endRadius.getValue();
                e.length = (float)this.length.getValue();
                e.speed = (float)this.speed.getValue();
                e.t = 0.0F;
                e.weaponXfrm.set(this.playerWeaponTransform.get(chr));
                this.effects.add(e);
            }
        }
    }

    public void render() {
        FBORenderTracerEffects.Drawer drawer = this.drawerPool.alloc();

        for (int i = 0; i < this.effects.size(); i++) {
            FBORenderTracerEffects.Effect effect = this.effects.get(i);
            FBORenderTracerEffects.Effect effectCopy = this.effectPool.alloc().set(effect);
            drawer.effects.add(effectCopy);
            if (!GameTime.isGamePaused()) {
                effect.t = effect.t + GameTime.getInstance().getMultiplier() * effect.speed;
            }

            if (effect.t >= 1.0F) {
                this.effects.remove(i--);
                this.effectPool.release(effect);
            }
        }

        SpriteRenderer.instance.drawGeneric(drawer);
    }

    private void registerOption(ConfigOption option) {
        this.options.add(option);
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "bulletTracerEffect-options.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, this.options);
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "bulletTracerEffect-options.ini";
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); i++) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption != null) {
                    myOption.parse(configOption.getValueAsString());
                }
            }
        }
    }

    public class DoubleConfigOption1 extends DoubleConfigOption {
        public DoubleConfigOption1(final String name, final double min, final double max, final double defaultValue) {
            Objects.requireNonNull(FBORenderTracerEffects.this);
            super(name, min, max, defaultValue);
            FBORenderTracerEffects.this.registerOption(this);
        }
    }

    private static final class Drawer extends TextureDraw.GenericDrawer {
        static final Matrix4f tempMatrix4f_1 = new Matrix4f();
        static final Vector3f tempVector3f_1 = new Vector3f();
        final ArrayList<FBORenderTracerEffects.Effect> effects = new ArrayList<>();

        @Override
        public void render() {
            GL11.glDepthFunc(515);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            VBORenderer.getInstance().setDepthTestForAllRuns(Boolean.TRUE);
            float characterX = Core.getInstance().floatParamMap.get(0);
            float characterY = Core.getInstance().floatParamMap.get(1);

            for (int i = 0; i < this.effects.size(); i++) {
                FBORenderTracerEffects.Effect effect = this.effects.get(i);
                calculateProjectViewXfrm(effect.projection, effect.view, true);
                PZGLUtil.pushAndLoadMatrix(5889, effect.projection);
                calculateModelXfrm(effect.x0, effect.y0, effect.z0, effect.angle, false, effect.model, true);
                Matrix4f m = tempMatrix4f_1.set(effect.view);
                m.mul(effect.model);
                m.mul(effect.weaponXfrm);
                m.translate(0.0F, 0.0F, effect.t * effect.range);
                PZGLUtil.pushAndLoadMatrix(5888, m);
                m.set(effect.model);
                m.mul(effect.weaponXfrm);
                m.setTranslation(0.0F, 0.0F, 0.0F);
                m.translate(0.0F, 0.0F, effect.t * effect.range);
                Vector3f v = m.transformPosition(tempVector3f_1.set(0.0F));
                float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
                IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
                    PZMath.fastfloor(characterX), PZMath.fastfloor(characterY), effect.x0 - v.x, effect.y0 - v.z, effect.z0 * 0.0F + v.y
                );
                float targetDepth = results.depthStart - (depthBufferValue + 1.0F) / 2.0F;
                VBORenderer.getInstance().setUserDepthForAllRuns(targetDepth);
                effect.render();
                VBORenderer.getInstance().flush();
                PZGLUtil.popMatrix(5888);
                PZGLUtil.popMatrix(5889);
            }

            VBORenderer.getInstance().setDepthTestForAllRuns(null);
            VBORenderer.getInstance().setUserDepthForAllRuns(null);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            FBORenderTracerEffects.getInstance().effectPool.releaseAll(this.effects);
            this.effects.clear();
            FBORenderTracerEffects.getInstance().drawerPool.release(this);
        }

        static Matrix4f calculateProjectViewXfrm(Matrix4f PROJECTION, Matrix4f VIEW, boolean bRenderThread) {
            int playerIndex = bRenderThread ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
            PlayerCamera cam = bRenderThread ? SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex) : IsoCamera.cameras[playerIndex];
            float OffscreenWidth = bRenderThread ? cam.offscreenWidth : IsoCamera.getOffscreenWidth(playerIndex);
            float OffscreenHeight = bRenderThread ? cam.offscreenHeight : IsoCamera.getOffscreenHeight(playerIndex);
            double screenWidth = OffscreenWidth / 1920.0F;
            double screenHeight = OffscreenHeight / 1920.0F;
            PROJECTION.setOrtho(
                -((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -10.0F, 10.0F
            );
            VIEW.scaling(Core.scale);
            VIEW.scale(Core.tileScale / 2.0F);
            VIEW.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
            VIEW.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
            return VIEW;
        }

        static void calculateModelXfrm(float ox, float oy, float oz, float useangle, boolean vehicle, Matrix4f MODEL, boolean bRenderThread) {
            int playerIndex = bRenderThread ? SpriteRenderer.instance.getRenderingPlayerIndex() : IsoCamera.frameState.playerIndex;
            float cx = bRenderThread ? Core.getInstance().floatParamMap.get(0) : IsoCamera.frameState.camCharacterX;
            float cy = bRenderThread ? Core.getInstance().floatParamMap.get(1) : IsoCamera.frameState.camCharacterY;
            float cz = bRenderThread ? Core.getInstance().floatParamMap.get(2) : IsoCamera.frameState.camCharacterZ;
            double x = cx;
            double y = cy;
            double z = cz;
            PlayerCamera cam = bRenderThread ? SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex) : IsoCamera.cameras[playerIndex];
            float rcx = cam.rightClickX;
            float rcy = cam.rightClickY;
            float tox = cam.getTOffX();
            float toy = cam.getTOffY();
            float defx = cam.deferedX;
            float defy = cam.deferedY;
            x -= cam.XToIso(-tox - rcx, -toy - rcy, 0.0F);
            y -= cam.YToIso(-tox - rcx, -toy - rcy, 0.0F);
            x += defx;
            y += defy;
            double difX = ox - x;
            double difY = oy - y;
            MODEL.identity();
            MODEL.translate(-((float)difX), (float)(oz - z) * 2.44949F, -((float)difY));
            if (vehicle) {
                MODEL.scale(-1.0F, 1.0F, 1.0F);
            } else {
                MODEL.scale(-1.5F, 1.5F, 1.5F);
            }

            MODEL.rotate(useangle + (float) Math.PI, 0.0F, 1.0F, 0.0F);
            if (!vehicle) {
                MODEL.translate(0.0F, -0.48F, 0.0F);
            }
        }
    }

    private static final class Effect {
        float x0;
        float y0;
        float z0;
        float angle;
        float range;
        float r0;
        float g0;
        float b0;
        float a0;
        float r1;
        float g1;
        float b1;
        float a1;
        float thickness0;
        float thickness1;
        float length;
        float speed;
        float t = -1.0F;
        final Matrix4f projection = new Matrix4f();
        final Matrix4f view = new Matrix4f();
        final Matrix4f model = new Matrix4f();
        final Matrix4f weaponXfrm = new Matrix4f();

        FBORenderTracerEffects.Effect set(FBORenderTracerEffects.Effect other) {
            this.x0 = other.x0;
            this.y0 = other.y0;
            this.z0 = other.z0;
            this.angle = other.angle;
            this.range = other.range;
            this.r1 = other.r1;
            this.g1 = other.g1;
            this.b1 = other.b1;
            this.a1 = other.a1;
            this.thickness0 = other.thickness0;
            this.thickness1 = other.thickness1;
            this.length = other.length;
            this.speed = other.speed;
            this.t = other.t;
            this.projection.set(other.projection);
            this.view.set(other.view);
            this.model.set(other.model);
            this.weaponXfrm.set(other.weaponXfrm);
            return this;
        }

        void update() {
        }

        void render() {
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.addCylinder_Fill(this.thickness0, this.thickness1, this.length, 4, 1, this.r1, this.g1, this.b1, this.a1);
        }
    }
}
