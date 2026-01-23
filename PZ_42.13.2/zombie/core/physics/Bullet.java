// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Quaternion;
import zombie.GameWindow;
import zombie.asset.AssetPath;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class Bullet {
    public static final byte TO_ADD_VEHICLE = 4;
    public static final byte TO_SCROLL_CHUNKMAP = 5;
    public static final byte TO_ACTIVATE_CHUNKMAP = 6;
    public static final byte TO_INIT_WORLD = 7;
    public static final byte TO_UPDATE_CHUNK = 8;
    public static final byte TO_DEBUG_DRAW_WORLD = 9;
    public static final byte TO_STEP_SIMULATION = 10;
    public static final byte TO_UPDATE_PLAYER_LIST = 12;
    public static final byte TO_END = -1;
    public static ByteBuffer cmdBuf;
    public static final HashMap<String, Integer> physicsShapeNameToIndex = new HashMap<>();

    public static void init() {
        String libSuffix = "";
        if ("1".equals(System.getProperty("zomboid.debuglibs.bullet"))) {
            DebugLog.General.debugln("***** Loading debug version of PZBullet");
            libSuffix = "d";
        }

        String NoOpenGL = "";
        if (GameServer.server && GameWindow.OSValidator.isUnix()) {
            NoOpenGL = "NoOpenGL";
        }

        if (System.getProperty("os.name").contains("OS X")) {
            loadLibrary("PZBullet");
        } else {
            loadLibrary("PZBullet" + NoOpenGL + "64" + libSuffix);
        }

        cmdBuf = ByteBuffer.allocateDirect(4096);
        cmdBuf.order(ByteOrder.LITTLE_ENDIAN);
        DebugLog.General.debugln("Initializing logging...");
        initPZBullet();
    }

    private static void loadLibrary(String libName) {
        DebugLog.General.debugln("Loading library: %s", libName);
        System.loadLibrary(libName);
    }

    private static native void ToBullet(ByteBuffer var0);

    public static void CatchToBullet(ByteBuffer bb) {
        try {
            ToBullet(bb);
        } catch (RuntimeException var2) {
            var2.printStackTrace();
        }
    }

    public static native void initPZBullet();

    public static native boolean isWorldInit();

    public static native void initWorld(int var0, int var1, int var2, int var3, int var4, int var5, boolean var6);

    public static native void destroyWorld();

    public static native void activateChunkMap(int var0, int var1, int var2, int var3);

    public static native void deactivateChunkMap(int var0);

    public static void initWorld(int minCellX, int minCellY, int maxCellX, int maxCellY, int offsetX, int offsetY, int wx, int wy, int chunkGridWidth) {
        initWorld(minCellX, minCellY, maxCellX, maxCellY, offsetX, offsetY, GameServer.server);
        activateChunkMap(0, wx, wy, chunkGridWidth);
    }

    public static void startLoadingPhysicsMeshes() {
        ArrayList<PhysicsShapeScript> physicsShapeScripts = ScriptManager.instance.getAllPhysicsShapes();

        for (int i = 0; i < physicsShapeScripts.size(); i++) {
            PhysicsShapeScript physicsShapeScript = physicsShapeScripts.get(i);
            PhysicsShape.PhysicsShapeAssetParams assetParams = new PhysicsShape.PhysicsShapeAssetParams();
            assetParams.postProcess = physicsShapeScript.postProcess;
            assetParams.allMeshes = physicsShapeScript.allMeshes;
            PhysicsShape var4 = (PhysicsShape)PhysicsShapeAssetManager.instance.load(new AssetPath(physicsShapeScript.meshName), assetParams);
        }
    }

    public static void initPhysicsMeshes() {
        physicsShapeNameToIndex.clear();
        WorldSimulation.instance.create();
        clearPhysicsMeshes();
        ArrayList<PhysicsShapeScript> physicsShapeScripts = ScriptManager.instance.getAllPhysicsShapes();
        int shapeCount = 0;

        for (int i = 0; i < physicsShapeScripts.size(); i++) {
            PhysicsShapeScript physicsShapeScript = physicsShapeScripts.get(i);
            PhysicsShape asset = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(physicsShapeScript.meshName);
            if (asset != null && asset.isReady()) {
                for (int j = 0; j < asset.meshes.size(); j++) {
                    PhysicsShape.OneMesh oneMesh = asset.meshes.get(j);
                    boolean bCompoundShape = asset.meshes.size() > 1;
                    definePhysicsMesh(
                        shapeCount,
                        bCompoundShape,
                        transformPhysicsMeshPoints(
                            physicsShapeScript.translate, physicsShapeScript.rotate, physicsShapeScript.scale, oneMesh.transform, oneMesh.points, false
                        )
                    );
                }

                physicsShapeNameToIndex.put(physicsShapeScript.getScriptObjectFullType(), shapeCount);
                shapeCount++;
            }
        }

        ArrayList<VehicleScript> vehicleScripts = ScriptManager.instance.getAllVehicleScripts();

        for (int ix = 0; ix < vehicleScripts.size(); ix++) {
            VehicleScript vehicleScript = vehicleScripts.get(ix);

            for (int j = 0; j < vehicleScript.getPhysicsShapeCount(); j++) {
                VehicleScript.PhysicsShape vehicleScriptPhysicsShape = vehicleScript.getPhysicsShape(j);
                if (vehicleScriptPhysicsShape.type == 3) {
                    PhysicsShapeScript physicsShapeScript = ScriptManager.instance.getPhysicsShape(vehicleScriptPhysicsShape.physicsShapeScript);
                    if (physicsShapeScript != null) {
                        PhysicsShape physicsShape = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(physicsShapeScript.meshName);
                        if (physicsShape != null && physicsShape.isReady()) {
                            for (int k = 0; k < physicsShape.meshes.size(); k++) {
                                PhysicsShape.OneMesh oneMesh = physicsShape.meshes.get(k);
                                if (oneMesh.points.length != 0) {
                                    Matrix4f xfrm = BaseVehicle.allocMatrix4f().scaling(vehicleScript.getModelScale());
                                    postMultiplyTranslateRotateScale(physicsShapeScript.translate, physicsShapeScript.rotate, physicsShapeScript.scale, xfrm);
                                    oneMesh.transform.transpose();
                                    xfrm.mul(oneMesh.transform);
                                    oneMesh.transform.transpose();
                                    float[] points = transformPhysicsMeshPoints(xfrm, oneMesh.points, false);
                                    int FIRST_USER_SHAPE = 1;
                                    defineVehiclePhysicsMesh(vehicleScript.getFullName(), 1 + j, points);
                                    BaseVehicle.releaseMatrix4f(xfrm);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static float[] transformPhysicsMeshPoints(Vector3f translate, Vector3f rotate, float scale, Matrix4f meshXfrm, float[] points, boolean bSwapYZ) {
        Matrix4f m = translationRotateScale(translate, rotate, scale, BaseVehicle.allocMatrix4f());
        if (meshXfrm != null) {
            meshXfrm.transpose();
            m.mul(meshXfrm);
            meshXfrm.transpose();
        }

        Vector3f point = new Vector3f();
        float[] result = new float[points.length];

        for (int i = 0; i < points.length; i += 3) {
            float x = points[i];
            float y = points[i + 1];
            float z = points[i + 2];
            m.transformPosition(x, y, z, point);
            result[i] = point.x;
            result[i + 1] = bSwapYZ ? point.z : point.y;
            result[i + 2] = bSwapYZ ? point.y : point.z;
        }

        BaseVehicle.releaseMatrix4f(m);
        return result;
    }

    public static float[] transformPhysicsMeshPoints(Matrix4f xfrm, float[] points, boolean bSwapYZ) {
        Vector3f point = new Vector3f();
        float[] result = new float[points.length];

        for (int i = 0; i < points.length; i += 3) {
            float x = points[i];
            float y = points[i + 1];
            float z = points[i + 2];
            xfrm.transformPosition(x, y, z, point);
            result[i] = point.x;
            result[i + 1] = bSwapYZ ? point.z : point.y;
            result[i + 2] = bSwapYZ ? point.y : point.z;
        }

        return result;
    }

    public static Matrix4f translationRotateScale(Vector3f translate, Vector3f rotate, float scale, Matrix4f result) {
        Quaternionf q = BaseVehicle.allocQuaternionf();
        q.rotationXYZ(rotate.x * (float) (Math.PI / 180.0), rotate.y * (float) (Math.PI / 180.0), rotate.z * (float) (Math.PI / 180.0));
        result.translationRotateScale(translate, q, scale);
        BaseVehicle.releaseQuaternionf(q);
        return result;
    }

    public static Matrix4f postMultiplyTranslateRotateScale(Vector3f translate, Vector3f rotate, float scale, Matrix4f result) {
        Matrix4f m2 = translationRotateScale(translate, rotate, scale, BaseVehicle.allocMatrix4f());
        result.mul(m2);
        BaseVehicle.releaseMatrix4f(m2);
        return result;
    }

    public static void updatePlayerList(ArrayList<IsoPlayer> players) {
        cmdBuf.clear();
        cmdBuf.put((byte)12);
        cmdBuf.putShort((short)players.size());

        for (IsoPlayer player : players) {
            cmdBuf.putInt(player.onlineId);
            cmdBuf.putInt(PZMath.fastfloor(player.getX()));
            cmdBuf.putInt(PZMath.fastfloor(player.getY()));
        }

        cmdBuf.put((byte)-1);
        cmdBuf.put((byte)-1);
        CatchToBullet(cmdBuf);
    }

    public static void beginUpdateChunk(IsoChunk chunk, int level) {
        cmdBuf.clear();
        cmdBuf.put((byte)8);
        cmdBuf.putShort((short)chunk.wx);
        cmdBuf.putShort((short)chunk.wy);
        cmdBuf.putShort((short)chunk.minLevel);
        cmdBuf.putShort((short)chunk.maxLevel);
        cmdBuf.putShort((short)level);
    }

    public static void updateChunk(int x, int y, int numShapes, byte[] shapes) {
        cmdBuf.put((byte)x);
        cmdBuf.put((byte)y);
        cmdBuf.put((byte)numShapes);

        for (int i = 0; i < numShapes; i++) {
            cmdBuf.put(shapes[i]);
        }
    }

    public static void endUpdateChunk() {
        if (cmdBuf.position() != 11) {
            cmdBuf.put((byte)-1);
            cmdBuf.put((byte)-1);
            CatchToBullet(cmdBuf);
        }
    }

    public static native void scrollChunkMap(int var0, int var1);

    public static void scrollChunkMapLeft(int playerIndex) {
        scrollChunkMap(playerIndex, 0);
    }

    public static void scrollChunkMapRight(int playerIndex) {
        scrollChunkMap(playerIndex, 1);
    }

    public static void scrollChunkMapUp(int playerIndex) {
        scrollChunkMap(playerIndex, 2);
    }

    public static void scrollChunkMapDown(int playerIndex) {
        scrollChunkMap(playerIndex, 3);
    }

    public static void setVehicleActive(BaseVehicle vehicle, boolean isActive) {
        vehicle.isActive = isActive;
        setVehicleActive(vehicle.getId(), isActive);
    }

    public static int setVehicleStatic(BaseVehicle vehicle, boolean isStatic) {
        vehicle.isStatic = isStatic;
        return setVehicleStatic(vehicle.getId(), isStatic);
    }

    public static boolean updatePhysicsForLevelIfNeeded(int wx, int wy, int level) {
        if (!IsoWorld.instance.metaGrid.isValidChunk(wx, wy)) {
            return false;
        } else {
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(wx, wy) : IsoWorld.instance.currentCell.getChunk(wx, wy);
            if (chunk == null) {
                return false;
            } else {
                IsoChunkLevel chunkLevel = chunk.getLevelData(level);
                if (chunkLevel == null) {
                    return false;
                } else {
                    chunkLevel.checkPhysicsLaterForActiveRagdoll();
                    if (!chunkLevel.physicsCheck) {
                        return false;
                    } else {
                        chunkLevel.physicsCheck = false;
                        chunk.updatePhysicsForLevel(level);
                        return true;
                    }
                }
            }
        }
    }

    public static native void setChunkMinMaxLevel(int var0, int var1, int var2, int var3);

    public static native void addVehicle(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, String var8);

    public static native void removeVehicle(int var0);

    public static native void controlVehicle(int var0, float var1, float var2, float var3);

    public static native void setVehicleActive(int var0, boolean var1);

    public static native void applyCentralForceToVehicle(int var0, float var1, float var2, float var3);

    public static native void applyTorqueToVehicle(int var0, float var1, float var2, float var3);

    public static native void teleportVehicle(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

    public static native void setTireInflation(int var0, int var1, float var2);

    public static native void setTireRemoved(int var0, int var1, boolean var2);

    public static native void stepSimulation(float var0, int var1, float var2);

    public static native int getVehicleCount();

    public static native int getVehiclePhysics(int var0, float[] var1);

    public static native int getOwnVehiclePhysics(int var0, float[] var1);

    public static native int setOwnVehiclePhysics(int var0, float[] var1);

    public static native int setVehicleParams(int var0, float[] var1);

    public static native int setVehicleMass(int var0, float var1);

    public static native int getObjectPhysics(float[] var0);

    public static native void createServerCell(int var0, int var1);

    public static native void removeServerCell(int var0, int var1);

    public static native int addPhysicsObject(float var0, float var1);

    public static native void defineVehicleScript(String var0, float[] var1);

    public static native void defineVehiclePhysicsMesh(String var0, int var1, float[] var2);

    public static native void setVehicleVelocityMultiplier(int var0, float var1, float var2);

    public static native int setVehicleStatic(int var0, boolean var1);

    public static native int addHingeConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7);

    public static native int addPointConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7);

    public static native int addRopeConstraint(int var0, int var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8);

    public static native void removeConstraint(int var0);

    public static native void clearPhysicsMeshes();

    public static native void definePhysicsMesh(int var0, boolean var1, float[] var2);

    public static native void initializeRagdollPose(int var0, float[] var1, float var2, float var3, float var4);

    public static native void initializeRagdollSkeleton(int var0, int[] var1);

    public static native void addRagdoll(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

    public static void addRagdoll(int id, org.lwjgl.util.vector.Vector3f in_worldPosition, Quaternion in_worldRotation) {
        addRagdoll(
            id, in_worldPosition.x, in_worldPosition.y, in_worldPosition.z, in_worldRotation.x, in_worldRotation.y, in_worldRotation.z, in_worldRotation.w
        );
    }

    public static native void removeRagdoll(int var0);

    public static native int simulateRagdoll(int var0, float[] var1);

    public static native int simulateRagdollWithRigidBodyOutput(int var0, float[] var1, float[] var2);

    public static native int updateSkeletonFromNetworkPhysics(int var0, float[] var1, float[] var2);

    public static native void getCorrectedWorldSpace(int var0, float[] var1);

    public static native void setRagdollLocalTransformRotation(int var0, float var1, float var2, float var3, float var4);

    public static native void updateRagdoll(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7);

    public static void updateRagdoll(int id, org.lwjgl.util.vector.Vector3f in_worldPosition, Quaternion in_worldRotation) {
        updateRagdoll(
            id, in_worldPosition.x, in_worldPosition.y, in_worldPosition.z, in_worldRotation.x, in_worldRotation.y, in_worldRotation.z, in_worldRotation.w
        );
    }

    public static native void updateRagdollSkeletonTransforms(int var0, int var1, float[] var2);

    public static native void updateRagdollSkeletonPreviousTransforms(int var0, int var1, float var2, float[] var3);

    public static native int getRagdollSimulationState(int var0);

    public static native void resetSkeletonPose(int var0);

    public static native void setRagdollActive(int var0, boolean var1);

    public static native void drawDebugSingleBone(int var0, boolean var1);

    public static native void drawDebugRagdollSkeleton(int var0, boolean var1, boolean var2);

    public static native void drawDebugRagdollBodyParts(int var0, boolean var1, boolean var2);

    public static native void highlightRagdollBodyPart(int var0, int var1);

    public static native void applyForce(int var0, int var1, float[] var2);

    public static native void applyImpulse(int var0, int var1, float[] var2);

    public static native void detachConstraint(int var0, int var1);

    public static native void updateBallistics(int var0, float var1, float var2, float var3);

    public static native void updateBallisticsMuzzleAimDirection(int var0, float var1, float var2, float var3);

    public static native void setBallisticsSize(int var0, float var1);

    public static native void setBallisticsColor(int var0, float var1, float var2, float var3);

    public static native int getBallisticsTargets(int var0, float var1, int var2, float[] var3);

    public static native int getBallisticsTargetsSpreadData(int var0, float var1, float var2, float var3, int var4, int var5, float[] var6);

    public static native int getBallisticsCameraTargets(int var0, float var1, int var2, boolean var3, float[] var4);

    public static native void setBallisticsRange(int var0, float var1);

    public static native void removeBallistics(int var0);

    public static native void updateBallisticsAimReticlePosition(int var0, float var1, float var2, float var3);

    public static native void updateBallisticsAimReticleRotation(int var0, float var1, float var2, float var3, float var4);

    public static native void updateBallisticsAimReticleQuaternion(int var0, float var1, float var2, float var3, float var4);

    public static native void updateBallisticsAimReticleRotate(int var0, float var1, float var2, float var3, float var4);

    public static native void updateBallisticsTargetSkeleton(int var0, int var1, float[] var2);

    public static native void updateBallisticsTarget(int var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, boolean var8);

    public static native void setBallisticsTargetAxis(int var0, float var1, float var2, float var3);

    public static native int addBallisticsTarget(int var0);

    public static native int removeBallisticsTarget(int var0);

    public static native int getTargetedBodyPart(int var0);

    public static native void setRagdollMass(float var0);

    public static native boolean checkWheelCollision(int var0, int var1, int var2);

    public static native boolean defineRagdollConstraints(float[] var0, boolean var1);

    public static native boolean defineRagdollAnchors(float[] var0, boolean var1);

    public static native boolean defineRagdollBodyPartInfo(float[] var0, boolean var1);

    public static native boolean defineRagdollBodyDynamics(float[] var0, boolean var1);

    public static native boolean setRagdollBodyDynamics(int var0, float[] var1);

    public static native boolean resetRagdollBodyDynamics(int var0);

    public static native void setBallisticsTargetAdjustingShapeScale(float var0, float var1, float var2);

    public static native void setBallisticsTargetAllPartsColor(float var0, float var1, float var2);
}
