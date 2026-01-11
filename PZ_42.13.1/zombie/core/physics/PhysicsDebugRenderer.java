// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ShaderHelper;
import zombie.core.math.PZMath;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

public final class PhysicsDebugRenderer extends TextureDraw.GenericDrawer {
    private static final ObjectPool<PhysicsDebugRenderer> POOL = new ObjectPool<>(PhysicsDebugRenderer::new);
    private float camOffX;
    private float camOffY;
    private float deferredX;
    private float deferredY;
    private int drawOffsetX;
    private int drawOffsetY;
    private int playerIndex;
    private float playerX;
    private float playerY;
    private float playerZ;
    private float offscreenWidth;
    private float offscreenHeight;
    private final TFloatArrayList elements = new TFloatArrayList();
    private static final ArrayList<RagdollController> renderedRagdollControllers = new ArrayList<>();
    private static final ArrayList<BallisticsController> renderedBallisticsControllers = new ArrayList<>();
    private static final ArrayList<BallisticsTarget> renderedBallisticsTargets = new ArrayList<>();
    private static final ArrayList<BaseVehicle> renderedVehicles = new ArrayList<>();

    public static PhysicsDebugRenderer alloc() {
        return POOL.alloc();
    }

    public void release() {
        POOL.release(this);
    }

    public void init(IsoPlayer player) {
        this.playerIndex = player.getPlayerNum();
        this.camOffX = IsoCamera.getRightClickOffX() + IsoCamera.playerOffsetX;
        this.camOffY = IsoCamera.getRightClickOffY() + IsoCamera.playerOffsetY;
        this.camOffX = this.camOffX
            + this.XToScreenExact(player.getX() - PZMath.fastfloor(player.getX()), player.getY() - PZMath.fastfloor(player.getY()), 0.0F, 0);
        this.camOffY = this.camOffY
            + this.YToScreenExact(player.getX() - PZMath.fastfloor(player.getX()), player.getY() - PZMath.fastfloor(player.getY()), 0.0F, 0);
        this.deferredX = IsoCamera.cameras[this.playerIndex].deferedX;
        this.deferredY = IsoCamera.cameras[this.playerIndex].deferedY;
        this.drawOffsetX = PZMath.fastfloor(player.getX());
        this.drawOffsetY = PZMath.fastfloor(player.getY());
        this.playerX = player.getX();
        this.playerY = player.getY();
        this.playerZ = IsoCamera.frameState.camCharacterZ;
        this.offscreenWidth = Core.getInstance().getOffscreenWidth(this.playerIndex);
        this.offscreenHeight = Core.getInstance().getOffscreenHeight(this.playerIndex);
        this.elements.resetQuick();
        int ddwX = PZMath.fastfloor(WorldSimulation.instance.offsetX) - this.drawOffsetX;
        int ddwY = PZMath.fastfloor(WorldSimulation.instance.offsetY) - this.drawOffsetY;
        int minLevel = -32;
        int maxLevel = 31;
        if (DebugOptions.instance.physicsRenderPlayerLevelOnly.getValue()) {
            minLevel = maxLevel = PZMath.fastfloor(this.playerZ);
        }

        if (DebugOptions.instance.physicsRenderBallisticsControllers.getValue()) {
            for (BallisticsController ballisticsController : renderedBallisticsControllers) {
                this.renderBallistics(ballisticsController.getID(), ddwX, ddwY);
            }
        }

        renderedBallisticsControllers.clear();
        if (DebugOptions.instance.physicsRenderBallisticsTargets.getValue()) {
            Bullet.setBallisticsTargetAllPartsColor(Color.white.r, Color.white.g, Color.white.b);

            for (BallisticsTarget ballisticsTarget : renderedBallisticsTargets) {
                if (ballisticsTarget.isValidIsoGameCharacter()) {
                    this.renderBallisticsTarget(ballisticsTarget.getID(), ddwX, ddwY);
                }
            }
        }

        renderedBallisticsTargets.clear();
        if (DebugOptions.instance.character.debug.ragdoll.render.enable.getValue()) {
            for (RagdollController ragdollController : renderedRagdollControllers) {
                this.renderRagdoll(ragdollController.getID(), ddwX, ddwY);
            }
        }

        if (DebugOptions.instance.physicsRenderBallisticsTargets.getValue()) {
            for (BaseVehicle baseVehicle : renderedVehicles) {
                this.renderVehicle(baseVehicle.getId(), ddwX, ddwY);
            }
        }

        if (DebugOptions.instance.physicsRender.getValue()) {
            this.n_debugDrawWorld(ddwX, ddwY, minLevel, maxLevel);
        }
    }

    public static void addRagdollRender(RagdollController ragdollController) {
        if (!renderedRagdollControllers.contains(ragdollController)) {
            renderedRagdollControllers.add(ragdollController);
        }
    }

    public static void addBallisticsRender(BallisticsController ballisticsController) {
        if (!renderedBallisticsControllers.contains(ballisticsController)) {
            renderedBallisticsControllers.add(ballisticsController);
        }
    }

    public static void addBallisticsRender(BallisticsTarget ballisticsTarget) {
        if (!renderedBallisticsTargets.contains(ballisticsTarget)) {
            renderedBallisticsTargets.add(ballisticsTarget);
        }
    }

    public static void removeRagdollRender(RagdollController ragdollController) {
        renderedRagdollControllers.remove(ragdollController);
    }

    public static void removeBallisticsRender(BallisticsTarget ballisticsTarget) {
        renderedBallisticsTargets.remove(ballisticsTarget);
    }

    public static void addVehicleRender(BaseVehicle baseVehicle) {
        if (!renderedVehicles.contains(baseVehicle)) {
            renderedVehicles.add(baseVehicle);
        }
    }

    public static void removeVehicleRender(BaseVehicle baseVehicle) {
        renderedVehicles.remove(baseVehicle);
    }

    @Override
    public void render() {
        DebugLog.Physics.debugOnceln("");
        GL11.glPushAttrib(1048575);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        boolean bDefaultShaderActive = DefaultShader.isActive;
        int shaderID = GL11.glGetInteger(35725);
        GL20.glUseProgram(0);
        Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
        PROJECTION.setOrtho(0.0F, this.offscreenWidth, this.offscreenHeight, 0.0F, 10000.0F, -10000.0F);
        Core.getInstance().projectionMatrixStack.push(PROJECTION);
        Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
        MODELVIEW.identity();
        int ddwX = -this.drawOffsetX;
        int ddwY = -this.drawOffsetY;
        float adjustX = this.deferredX;
        float adjustY = this.deferredY;
        MODELVIEW.translate(this.offscreenWidth / 2.0F, this.offscreenHeight / 2.0F, 0.0F);
        float dx = this.XToScreenExact(adjustX, adjustY, this.playerZ, 0);
        float dy = this.YToScreenExact(adjustX, adjustY, this.playerZ, 0);
        dx += this.camOffX;
        dy += this.camOffY;
        MODELVIEW.translate(-dx, -dy, 0.0F);
        ddwX = (int)(ddwX + WorldSimulation.instance.offsetX);
        ddwY = (int)(ddwY + WorldSimulation.instance.offsetY);
        int side = 32 * Core.tileScale;
        float f = (float)Math.sqrt(side * side + side * side);
        MODELVIEW.scale(f, f, f);
        MODELVIEW.rotate((float) (Math.PI * 7.0 / 6.0), 1.0F, 0.0F, 0.0F);
        MODELVIEW.rotate((float) (-Math.PI / 4), 0.0F, 1.0F, 0.0F);
        Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.startRun(vbor.formatPositionColor);
        vbor.setMode(1);
        vbor.setLineWidth(1.0F);
        vbor.setDepthTest(false);
        float alpha = 0.5F;
        int i = 0;

        while (i < this.elements.size()) {
            float x0 = this.elements.getQuick(i++);
            float y0 = this.elements.getQuick(i++);
            float z0 = this.elements.getQuick(i++);
            float x1 = this.elements.getQuick(i++);
            float y1 = this.elements.getQuick(i++);
            float z1 = this.elements.getQuick(i++);
            float r0 = this.elements.getQuick(i++);
            float g0 = this.elements.getQuick(i++);
            float b0 = this.elements.getQuick(i++);
            float r1 = this.elements.getQuick(i++);
            float g1 = this.elements.getQuick(i++);
            float b1 = this.elements.getQuick(i++);
            vbor.addLine(x0, y0, z0, x1, y1, z1, r0, g0, b0, 0.5F, r1, g1, b1, 0.5F);
        }

        vbor.endRun();
        vbor.flush();
        GL11.glLineWidth(1.0F);
        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
        GL11.glEnable(3042);
        GL11.glEnable(3553);
        GL11.glPopAttrib();
        GL20.glUseProgram(shaderID);
        DefaultShader.isActive = bDefaultShaderActive;
        ShaderHelper.forgetCurrentlyBound();
        Texture.lastTextureID = -1;
    }

    @Override
    public void postRender() {
        this.release();
    }

    public float YToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        return IsoUtils.YToScreen(objectX, objectY, objectZ, screenZ);
    }

    public float XToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        return IsoUtils.XToScreen(objectX, objectY, objectZ, screenZ);
    }

    public void drawLine(
        float fromX, float fromY, float fromZ, float toX, float toY, float toZ, float fromR, float fromG, float fromB, float toR, float toG, float toB
    ) {
        DebugLog.Physics.debugOnceln("Called from JNI");
        if (!(fromX < -1000.0F) && !(fromX > 1000.0F) && !(fromY < -1000.0F) && !(fromY > 1000.0F)) {
            this.elements.add(fromX);
            this.elements.add(fromY);
            this.elements.add(fromZ);
            this.elements.add(toX);
            this.elements.add(toY);
            this.elements.add(toZ);
            this.elements.add(fromR);
            this.elements.add(fromG);
            this.elements.add(fromB);
            this.elements.add(toR);
            this.elements.add(toG);
            this.elements.add(toB);
        }
    }

    public void drawSphere(float pX, float pY, float pZ, float radius, float r, float g, float b) {
        DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
    }

    public void drawTriangle(float aX, float aY, float aZ, float bX, float bY, float bZ, float cX, float cY, float cZ, float r, float g, float b, float alpha) {
        DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
    }

    public void drawContactPoint(
        float pointOnBX,
        float pointOnBY,
        float pointOnBZ,
        float normalOnBX,
        float normalOnBY,
        float normalOnBZ,
        float distance,
        int lifeTime,
        float r,
        float g,
        float b
    ) {
        DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
    }

    public void drawCapsule(
        float radius, float halfHeight, int upAxis, float pX, float pY, float pZ, float rX, float rY, float rZ, float qZ, float r, float g, float b, float a
    ) {
        DebugLog.Physics.debugln("[Not Implemented] - Called from JNI");
    }

    public native void n_debugDrawWorld(int arg0, int arg1, int arg2, int arg3);

    public native void renderRagdoll(int arg0, int arg1, int arg2);

    public native void renderBallistics(int arg0, int arg1, int arg2);

    public native void renderBallisticsTarget(int arg0, int arg1, int arg2);

    public native void renderVehicle(int arg0, int arg1, int arg2);
}
