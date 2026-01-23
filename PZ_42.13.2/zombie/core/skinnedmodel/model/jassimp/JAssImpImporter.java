// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model.jassimp;

import gnu.trove.map.hash.TObjectIntHashMap;
import jassimp.AiAnimation;
import jassimp.AiBone;
import jassimp.AiBuiltInWrapperProvider;
import jassimp.AiMaterial;
import jassimp.AiMatrix4f;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiNodeAnim;
import jassimp.AiScene;
import jassimp.Jassimp;
import jassimp.JassimpLibraryLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTexture;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTextureSkin;
import zombie.util.SharedStrings;
import zombie.util.list.PZArrayUtil;

public final class JAssImpImporter {
    private static final TObjectIntHashMap<String> sharedStringCounts = new TObjectIntHashMap<>();
    private static final SharedStrings sharedStrings = new SharedStrings();
    private static final HashMap<String, Integer> tempHashMap = new HashMap<>();

    public static void Init() {
        Jassimp.setLibraryLoader(new JAssImpImporter.LibraryLoader());
    }

    static AiNode FindNode(String name, AiNode node) {
        List<AiNode> children = node.getChildren();

        for (int i = 0; i < children.size(); i++) {
            AiNode child = children.get(i);
            if (child.getName().equals(name)) {
                return child;
            }

            AiNode n = FindNode(name, child);
            if (n != null) {
                return n;
            }
        }

        return null;
    }

    static Matrix4f getMatrixFromAiMatrix(AiMatrix4f srcMat) {
        return getMatrixFromAiMatrix(srcMat, new Matrix4f());
    }

    static Matrix4f getMatrixFromAiMatrix(AiMatrix4f srcMat, Matrix4f result) {
        result.m00 = srcMat.get(0, 0);
        result.m01 = srcMat.get(0, 1);
        result.m02 = srcMat.get(0, 2);
        result.m03 = srcMat.get(0, 3);
        result.m10 = srcMat.get(1, 0);
        result.m11 = srcMat.get(1, 1);
        result.m12 = srcMat.get(1, 2);
        result.m13 = srcMat.get(1, 3);
        result.m20 = srcMat.get(2, 0);
        result.m21 = srcMat.get(2, 1);
        result.m22 = srcMat.get(2, 2);
        result.m23 = srcMat.get(2, 3);
        result.m30 = srcMat.get(3, 0);
        result.m31 = srcMat.get(3, 1);
        result.m32 = srcMat.get(3, 2);
        result.m33 = srcMat.get(3, 3);
        return result;
    }

    static void CollectBoneNodes(ArrayList<AiNode> boneNodes, AiNode bonenode) {
        boneNodes.add(bonenode);

        for (int i = 0; i < bonenode.getNumChildren(); i++) {
            CollectBoneNodes(boneNodes, bonenode.getChildren().get(i));
        }
    }

    static String DumpAiMatrix(AiMatrix4f matrix) {
        String s = "";
        s = s + String.format("%1$.8f, ", matrix.get(0, 0));
        s = s + String.format("%1$.8f, ", matrix.get(0, 1));
        s = s + String.format("%1$.8f, ", matrix.get(0, 2));
        s = s + String.format("%1$.8f\n ", matrix.get(0, 3));
        s = s + String.format("%1$.8f, ", matrix.get(1, 0));
        s = s + String.format("%1$.8f, ", matrix.get(1, 1));
        s = s + String.format("%1$.8f, ", matrix.get(1, 2));
        s = s + String.format("%1$.8f\n ", matrix.get(1, 3));
        s = s + String.format("%1$.8f, ", matrix.get(2, 0));
        s = s + String.format("%1$.8f, ", matrix.get(2, 1));
        s = s + String.format("%1$.8f, ", matrix.get(2, 2));
        s = s + String.format("%1$.8f\n ", matrix.get(2, 3));
        s = s + String.format("%1$.8f, ", matrix.get(3, 0));
        s = s + String.format("%1$.8f, ", matrix.get(3, 1));
        s = s + String.format("%1$.8f, ", matrix.get(3, 2));
        return s + String.format("%1$.8f\n ", matrix.get(3, 3));
    }

    static String DumpMatrix(Matrix4f matrix) {
        String s = "";
        s = s + String.format("%1$.8f, ", matrix.m00);
        s = s + String.format("%1$.8f, ", matrix.m01);
        s = s + String.format("%1$.8f, ", matrix.m02);
        s = s + String.format("%1$.8f\n ", matrix.m03);
        s = s + String.format("%1$.8f, ", matrix.m10);
        s = s + String.format("%1$.8f, ", matrix.m11);
        s = s + String.format("%1$.8f, ", matrix.m12);
        s = s + String.format("%1$.8f\n ", matrix.m13);
        s = s + String.format("%1$.8f, ", matrix.m20);
        s = s + String.format("%1$.8f, ", matrix.m21);
        s = s + String.format("%1$.8f, ", matrix.m22);
        s = s + String.format("%1$.8f\n ", matrix.m23);
        s = s + String.format("%1$.8f, ", matrix.m30);
        s = s + String.format("%1$.8f, ", matrix.m31);
        s = s + String.format("%1$.8f, ", matrix.m32);
        return s + String.format("%1$.8f\n ", matrix.m33);
    }

    static AiBone FindAiBone(String name, List<AiBone> bones) {
        int numBones = bones.size();

        for (int k = 0; k < numBones; k++) {
            AiBone aiBone = bones.get(k);
            String bonename = aiBone.getName();
            if (bonename.equals(name)) {
                return aiBone;
            }
        }

        return null;
    }

    private static void DumpMesh(VertexPositionNormalTangentTextureSkin[] vertices) {
        StringBuilder sb = new StringBuilder();

        for (VertexPositionNormalTangentTextureSkin vert : vertices) {
            sb.append(vert.position.x()).append('\t').append(vert.position.y()).append('\t').append(vert.position.z()).append('\t').append('\n');
        }

        String s = sb.toString();
        s = null;
    }

    static Vector3f GetKeyFramePosition(AiNodeAnim animNode, float time) {
        int sidx = -1;

        for (int k = 0; k < animNode.getNumPosKeys(); k++) {
            float t = (float)animNode.getPosKeyTime(k);
            if (t > time) {
                break;
            }

            sidx = k;
            if (t == time) {
                return new Vector3f(animNode.getPosKeyX(k), animNode.getPosKeyY(k), animNode.getPosKeyZ(k));
            }
        }

        if (sidx < 0) {
            return new Vector3f();
        } else if (animNode.getNumPosKeys() > sidx + 1) {
            float t1 = (float)animNode.getPosKeyTime(sidx);
            float t2 = (float)animNode.getPosKeyTime(sidx + 1);
            float r = t2 - t1;
            float s = time - t1;
            s /= r;
            float x1 = animNode.getPosKeyX(sidx);
            float x2 = animNode.getPosKeyX(sidx + 1);
            float x = x1 + s * (x2 - x1);
            float y1 = animNode.getPosKeyY(sidx);
            float y2 = animNode.getPosKeyY(sidx + 1);
            float y = y1 + s * (y2 - y1);
            float z1 = animNode.getPosKeyZ(sidx);
            float z2 = animNode.getPosKeyZ(sidx + 1);
            float z = z1 + s * (z2 - z1);
            return new Vector3f(x, y, z);
        } else {
            return new Vector3f(animNode.getPosKeyX(sidx), animNode.getPosKeyY(sidx), animNode.getPosKeyZ(sidx));
        }
    }

    static Quaternion GetKeyFrameRotation(AiNodeAnim animNode, float time) {
        boolean bFound = false;
        Quaternion foundQuat = new Quaternion();
        int sidx = -1;

        for (int k = 0; k < animNode.getNumRotKeys(); k++) {
            float t = (float)animNode.getRotKeyTime(k);
            if (t > time) {
                break;
            }

            sidx = k;
            if (t == time) {
                foundQuat.set(animNode.getRotKeyX(k), animNode.getRotKeyY(k), animNode.getRotKeyZ(k), animNode.getRotKeyW(k));
                bFound = true;
                break;
            }
        }

        if (!bFound && sidx < 0) {
            return new Quaternion();
        } else {
            if (!bFound && animNode.getNumRotKeys() > sidx + 1) {
                float t1 = (float)animNode.getRotKeyTime(sidx);
                float t2 = (float)animNode.getRotKeyTime(sidx + 1);
                float r = t2 - t1;
                float s = time - t1;
                s /= r;
                float x1 = animNode.getRotKeyX(sidx);
                float x2 = animNode.getRotKeyX(sidx + 1);
                float x = x1 + s * (x2 - x1);
                float y1 = animNode.getRotKeyY(sidx);
                float y2 = animNode.getRotKeyY(sidx + 1);
                float y = y1 + s * (y2 - y1);
                float z1 = animNode.getRotKeyZ(sidx);
                float z2 = animNode.getRotKeyZ(sidx + 1);
                float z = z1 + s * (z2 - z1);
                float w1 = animNode.getRotKeyW(sidx);
                float w2 = animNode.getRotKeyW(sidx + 1);
                float w = w1 + s * (w2 - w1);
                foundQuat.set(x, y, z, w);
                bFound = true;
            }

            if (!bFound && animNode.getNumRotKeys() > sidx) {
                foundQuat.set(animNode.getRotKeyX(sidx), animNode.getRotKeyY(sidx), animNode.getRotKeyZ(sidx), animNode.getRotKeyW(sidx));
                bFound = true;
            }

            return foundQuat;
        }
    }

    static Vector3f GetKeyFrameScale(AiNodeAnim animNode, float time) {
        int sidx = -1;

        for (int k = 0; k < animNode.getNumScaleKeys(); k++) {
            float t = (float)animNode.getScaleKeyTime(k);
            if (t > time) {
                break;
            }

            sidx = k;
            if (t == time) {
                return new Vector3f(animNode.getScaleKeyX(k), animNode.getScaleKeyY(k), animNode.getScaleKeyZ(k));
            }
        }

        if (sidx < 0) {
            return new Vector3f(1.0F, 1.0F, 1.0F);
        } else if (animNode.getNumScaleKeys() > sidx + 1) {
            float t1 = (float)animNode.getScaleKeyTime(sidx);
            float t2 = (float)animNode.getScaleKeyTime(sidx + 1);
            float r = t2 - t1;
            float s = time - t1;
            s /= r;
            float x1 = animNode.getScaleKeyX(sidx);
            float x2 = animNode.getScaleKeyX(sidx + 1);
            float x = x1 + s * (x2 - x1);
            float y1 = animNode.getScaleKeyY(sidx);
            float y2 = animNode.getScaleKeyY(sidx + 1);
            float y = y1 + s * (y2 - y1);
            float z1 = animNode.getScaleKeyZ(sidx);
            float z2 = animNode.getScaleKeyZ(sidx + 1);
            float z = z1 + s * (z2 - z1);
            return new Vector3f(x, y, z);
        } else {
            return new Vector3f(animNode.getScaleKeyX(sidx), animNode.getScaleKeyY(sidx), animNode.getScaleKeyZ(sidx));
        }
    }

    static void replaceHashMapKeys(HashMap<String, Integer> hashMap, String countKey) {
        tempHashMap.clear();
        tempHashMap.putAll(hashMap);
        hashMap.clear();

        for (Entry<String, Integer> e : tempHashMap.entrySet()) {
            String key = getSharedString(e.getKey(), countKey);
            hashMap.put(key, e.getValue());
        }

        tempHashMap.clear();
    }

    public static String getSharedString(String str, String countKey) {
        String shared = sharedStrings.get(str);
        if (Core.debug && str != shared) {
            sharedStringCounts.adjustOrPutValue(countKey, 1, 0);
        }

        return shared;
    }

    private static void takeOutTheTrash(VertexPositionNormalTangentTexture[] vs) {
        PZArrayUtil.forEach(vs, JAssImpImporter::takeOutTheTrash);
        Arrays.fill(vs, null);
    }

    private static void takeOutTheTrash(VertexPositionNormalTangentTextureSkin[] vs) {
        PZArrayUtil.forEach(vs, JAssImpImporter::takeOutTheTrash);
        Arrays.fill(vs, null);
    }

    private static void takeOutTheTrash(VertexPositionNormalTangentTexture v) {
        v.normal = null;
        v.position = null;
        v.textureCoordinates = null;
        v.tangent = null;
    }

    private static void takeOutTheTrash(VertexPositionNormalTangentTextureSkin v) {
        v.normal = null;
        v.position = null;
        v.textureCoordinates = null;
        v.tangent = null;
        v.blendWeights = null;
        v.blendIndices = null;
    }

    public static void takeOutTheTrash(AiScene scene) {
        for (AiAnimation anim : scene.getAnimations()) {
            anim.getChannels().clear();
        }

        scene.getAnimations().clear();
        scene.getCameras().clear();
        scene.getLights().clear();

        for (AiMaterial material : scene.getMaterials()) {
            material.getProperties().clear();
        }

        scene.getMaterials().clear();

        for (AiMesh mesh : scene.getMeshes()) {
            for (AiBone bone : mesh.getBones()) {
                bone.getBoneWeights().clear();
            }

            mesh.getBones().clear();
        }

        scene.getMeshes().clear();
        AiNode node = scene.getSceneRoot(new AiBuiltInWrapperProvider());
        takeOutTheTrash(node);
    }

    private static void takeOutTheTrash(AiNode node) {
        for (AiNode child : node.getChildren()) {
            takeOutTheTrash(child);
        }

        node.getChildren().clear();
    }

    private static class LibraryLoader extends JassimpLibraryLoader {
        @Override
        public void loadLibrary() {
            if (System.getProperty("os.name").contains("OS X")) {
                System.loadLibrary("jassimp");
            } else if (System.getProperty("os.name").startsWith("Win")) {
                System.loadLibrary("jassimp64");
            } else {
                System.loadLibrary("jassimp64");
            }
        }
    }

    public static enum LoadMode {
        Normal,
        StaticMesh,
        AnimationOnly;
    }
}
