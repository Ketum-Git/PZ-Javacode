// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjglx.BufferUtils;
import org.lwjglx.util.glu.Cylinder;
import org.lwjglx.util.glu.PartialDisk;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.BoxedStaticValues;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationMultiTrack;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.Mask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteModel;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.PhysicsShapeScript;
import zombie.scripting.objects.VehicleScript;
import zombie.seating.SeatingManager;
import zombie.tileDepth.CylinderUtils;
import zombie.tileDepth.TileDepthTexture;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryFile;
import zombie.tileDepth.TileGeometryManager;
import zombie.tileDepth.TileGeometryUtils;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.Rasterize;

@UsedFromLua
public final class UI3DScene extends UIElement {
    public static final float Z_SCALE = 0.8164967F;
    private final ArrayList<UI3DScene.SceneObject> objects = new ArrayList<>();
    private UI3DScene.View view = UI3DScene.View.Right;
    private UI3DScene.TransformMode transformMode = UI3DScene.TransformMode.Local;
    private int viewX;
    private int viewY;
    private final Vector3f viewRotation = new Vector3f();
    private int zoom = 3;
    private int zoomMax = 10;
    private int gridDivisions = 1;
    private UI3DScene.GridPlane gridPlane = UI3DScene.GridPlane.YZ;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private static final long VIEW_CHANGE_TIME = 350L;
    private long viewChangeTime;
    private final Quaternionf modelViewChange = new Quaternionf();
    private boolean drawAttachments;
    private boolean drawGrid = true;
    private boolean drawGridAxes;
    private boolean drawGeometry = true;
    private boolean drawGridPlane;
    private final UI3DScene.CharacterSceneModelCamera characterSceneModelCamera = new UI3DScene.CharacterSceneModelCamera();
    private final UI3DScene.VehicleSceneModelCamera vehicleSceneModelCamera = new UI3DScene.VehicleSceneModelCamera();
    private static final ObjectPool<UI3DScene.SetModelCamera> s_SetModelCameraPool = new ObjectPool<>(UI3DScene.SetModelCamera::new);
    private final UI3DScene.StateData[] stateData = new UI3DScene.StateData[3];
    private UI3DScene.Gizmo gizmo;
    private final UI3DScene.RotateGizmo rotateGizmo = new UI3DScene.RotateGizmo();
    private final UI3DScene.ScaleGizmo scaleGizmo = new UI3DScene.ScaleGizmo();
    private final UI3DScene.TranslateGizmo translateGizmo = new UI3DScene.TranslateGizmo();
    private final Vector3f gizmoPos = new Vector3f();
    private final Vector3f gizmoRotate = new Vector3f();
    private UI3DScene.SceneObject gizmoParent;
    private UI3DScene.SceneObject gizmoOrigin;
    private UI3DScene.SceneObject gizmoChild;
    private final UI3DScene.OriginAttachment originAttachment = new UI3DScene.OriginAttachment(this);
    private final UI3DScene.OriginBone originBone = new UI3DScene.OriginBone(this);
    private final UI3DScene.OriginGeometry originGeometry = new UI3DScene.OriginGeometry(this);
    private final UI3DScene.OriginGizmo originGizmo = new UI3DScene.OriginGizmo(this);
    private final UI3DScene.OriginVehiclePart originVehiclePart = new UI3DScene.OriginVehiclePart(this);
    private float gizmoScale = 1.0F;
    private boolean gizmoAxisVisibleX = true;
    private boolean gizmoAxisVisibleY = true;
    private boolean gizmoAxisVisibleZ = true;
    private String selectedAttachment;
    private final ArrayList<UI3DScene.PositionRotation> axes = new ArrayList<>();
    private final UI3DScene.OriginBone highlightBone = new UI3DScene.OriginBone(this);
    private final UI3DScene.OriginVehiclePart highlightPartBone = new UI3DScene.OriginVehiclePart(this);
    private final UI3DScene.PolygonEditor polygonEditor = new UI3DScene.PolygonEditor(this);
    private static Clipper clipper;
    private static final ObjectPool<UI3DScene.PositionRotation> s_posRotPool = new ObjectPool<>(UI3DScene.PositionRotation::new);
    private final ArrayList<UI3DScene.AABB> aabb = new ArrayList<>();
    private static final ObjectPool<UI3DScene.AABB> s_aabbPool = new ObjectPool<>(UI3DScene.AABB::new);
    private final ArrayList<UI3DScene.Box3D> box3d = new ArrayList<>();
    private static final ObjectPool<UI3DScene.Box3D> s_box3DPool = new ObjectPool<>(UI3DScene.Box3D::new);
    private final ArrayList<UI3DScene.PhysicsMesh> physicsMesh = new ArrayList<>();
    private static final ObjectPool<UI3DScene.PhysicsMesh> s_physicsMeshPool = new ObjectPool<>(UI3DScene.PhysicsMesh::new);
    final Vector3f tempVector3f = new Vector3f();
    final int[] viewport = new int[]{0, 0, 0, 0};
    private static final float GRID_DARK = 0.1F;
    private static final float GRID_LIGHT = 0.2F;
    private float gridAlpha = 1.0F;
    private static final int HALF_GRID = 5;
    private static VBORenderer vboRenderer;
    private static final ThreadLocal<ObjectPool<UI3DScene.Ray>> TL_Ray_pool = ThreadLocal.withInitial(UI3DScene.RayObjectPool::new);
    private static final ThreadLocal<ObjectPool<UI3DScene.Plane>> TL_Plane_pool = ThreadLocal.withInitial(UI3DScene.PlaneObjectPool::new);
    static final float SMALL_NUM = 1.0E-8F;

    public UI3DScene(KahluaTable table) {
        super(table);

        for (int i = 0; i < this.stateData.length; i++) {
            this.stateData[i] = new UI3DScene.StateData();
            this.stateData[i].gridPlaneDrawer = new UI3DScene.GridPlaneDrawer(this);
            this.stateData[i].overlaysDrawer = new UI3DScene.OverlaysDrawer();
        }
    }

    UI3DScene.SceneObject getSceneObjectById(String id, boolean required) {
        for (int i = 0; i < this.objects.size(); i++) {
            UI3DScene.SceneObject sceneObject = this.objects.get(i);
            if (sceneObject.id.equalsIgnoreCase(id)) {
                return sceneObject;
            }
        }

        if (required) {
            throw new NullPointerException("scene object \"" + id + "\" not found");
        } else {
            return null;
        }
    }

    <C> C getSceneObjectById(String id, Class<C> clazz, boolean required) {
        for (int i = 0; i < this.objects.size(); i++) {
            UI3DScene.SceneObject sceneObject = this.objects.get(i);
            if (sceneObject.id.equalsIgnoreCase(id)) {
                if (clazz.isInstance(sceneObject)) {
                    return clazz.cast(sceneObject);
                }

                if (required) {
                    throw new ClassCastException(
                        "scene object \"" + id + "\" is " + sceneObject.getClass().getSimpleName() + " expected " + clazz.getSimpleName()
                    );
                }
            }
        }

        if (required) {
            throw new NullPointerException("scene object \"" + id + "\" not found");
        } else {
            return null;
        }
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            vboRenderer = VBORenderer.getInstance();
            super.render();
            IndieGL.glDepthMask(true);
            SpriteRenderer.instance.glClearDepth(1.0F);
            IndieGL.glClear(256);
            UI3DScene.StateData stateData = this.stateDataMain();
            this.setModelViewProjection(stateData);
            if (this.drawGridPlane) {
                SpriteRenderer.instance.drawGeneric(stateData.gridPlaneDrawer);
            }

            PZArrayUtil.forEach(stateData.objectData, UI3DScene.SceneObjectRenderData::release);
            stateData.objectData.clear();

            for (int i = 0; i < this.objects.size(); i++) {
                UI3DScene.SceneObject sceneObject = this.objects.get(i);
                if (sceneObject.visible) {
                    if (sceneObject.autoRotate) {
                        sceneObject.autoRotateAngle = (float)(sceneObject.autoRotateAngle + UIManager.getMillisSinceLastRender() / 30.0);
                        if (sceneObject.autoRotateAngle > 360.0F) {
                            sceneObject.autoRotateAngle = 0.0F;
                        }
                    }

                    UI3DScene.SceneObjectRenderData renderData = sceneObject.renderMain();
                    if (renderData != null) {
                        stateData.objectData.add(renderData);
                    }
                }
            }

            float mouseX = Mouse.getXA() - this.getAbsoluteX().intValue();
            float mouseY = Mouse.getYA() - this.getAbsoluteY().intValue();
            this.setGizmoTransforms(stateData);
            if (this.gizmo != null) {
                stateData.gizmoAxis = this.gizmo.hitTest(mouseX, mouseY);
            }

            stateData.overlaysDrawer.init();
            SpriteRenderer.instance.drawGeneric(stateData.overlaysDrawer);
            if (this.drawGrid) {
                Vector3f scenePos = this.uiToScene(mouseX, mouseY, 0.0F, this.tempVector3f);
                scenePos.x = Math.round(scenePos.x * this.gridMult()) / this.gridMult();
                scenePos.y = Math.round(scenePos.y * this.gridMult()) / this.gridMult();
                scenePos.z = Math.round(scenePos.z * this.gridMult()) / this.gridMult();
                int xWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("X: %.3f", scenePos.x));
                int yWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Y: %.3f", scenePos.y));
                int zWidth = TextManager.instance.MeasureStringX(UIFont.Small, String.format("Z: %.3f", scenePos.z));
                this.DrawText(
                    UIFont.Small, String.format("X: %.3f", scenePos.x), this.width - 20.0F - zWidth - 20.0F - yWidth - 20.0F - xWidth, 10.0, 1.0, 0.0, 0.0, 1.0
                );
                this.DrawText(UIFont.Small, String.format("Y: %.3f", scenePos.y), this.width - 20.0F - zWidth - 20.0F - yWidth, 10.0, 0.0, 1.0, 0.0, 1.0);
                this.DrawText(UIFont.Small, String.format("Z: %.3f", scenePos.z), this.width - 20.0F - zWidth, 10.0, 0.0, 0.5, 1.0, 1.0);
            }

            if (this.gizmo == this.rotateGizmo && this.rotateGizmo.trackAxis != UI3DScene.Axis.None) {
                Vector3f xln = this.rotateGizmo.startXfrm.getTranslation(allocVector3f());
                float x = this.sceneToUIX(xln.x, xln.y, xln.z);
                float y = this.sceneToUIY(xln.x, xln.y, xln.z);
                LineDrawer.drawLine(x, y, mouseX, mouseY, 0.5F, 0.5F, 0.5F, 1.0F, 1);
                releaseVector3f(xln);
            }

            if (this.highlightBone.boneName != null) {
                Matrix4f boneMatrix = this.highlightBone.getGlobalTransform(allocMatrix4f());
                if (this.highlightBone.character != null) {
                    this.highlightBone.character.getGlobalTransform(allocMatrix4f()).mul(boneMatrix, boneMatrix);
                } else if (this.highlightBone.sceneModel != null) {
                    this.highlightBone.sceneModel.getGlobalTransform(allocMatrix4f()).mul(boneMatrix, boneMatrix);
                }

                Vector3f scenePos = boneMatrix.getTranslation(allocVector3f());
                float x = this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
                float y = this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
                LineDrawer.drawCircle(x, y, 10.0F, 16, 1.0F, 1.0F, 1.0F);
                releaseVector3f(scenePos);
                releaseMatrix4f(boneMatrix);
            }

            if (this.highlightPartBone.vehicle != null) {
                Matrix4f boneMatrix = this.highlightPartBone.getGlobalBoneTransform(allocMatrix4f());
                Vector3f scenePos = boneMatrix.getTranslation(allocVector3f());
                float x = this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
                float y = this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
                LineDrawer.drawCircle(x, y, 10.0F, 16, 1.0F, 1.0F, 1.0F);
                releaseVector3f(scenePos);
                releaseMatrix4f(boneMatrix);
            }

            for (int ix = 0; ix < this.objects.size(); ix++) {
                UI3DScene.ScenePolygon scenePolygon = Type.tryCastTo(this.objects.get(ix), UI3DScene.ScenePolygon.class);
                if (scenePolygon != null && scenePolygon.editing) {
                    scenePolygon.renderPoints();
                }
            }
        }
    }

    private void setModelViewProjection(UI3DScene.StateData stateData) {
        this.calcMatrices(this.projection, this.modelView);
        stateData.projection.set(this.projection);
        long ms = System.currentTimeMillis();
        if (this.viewChangeTime + 350L > ms) {
            float f = (float)(this.viewChangeTime + 350L - ms) / 350.0F;
            Quaternionf q = allocQuaternionf().setFromUnnormalized(this.modelView);
            stateData.modelView.set(this.modelViewChange.slerp(q, 1.0F - f));
            releaseQuaternionf(q);
        } else {
            stateData.modelView.set(this.modelView);
        }

        stateData.zoom = this.zoom;
    }

    private void setGizmoTransforms(UI3DScene.StateData stateData) {
        stateData.gizmo = this.gizmo;
        if (this.gizmo != null) {
            stateData.gizmoTranslate.set(this.gizmoPos);
            stateData.gizmoRotate.set(this.gizmoRotate);
            stateData.gizmoTransform.translation(this.gizmoPos);
            stateData.gizmoTransform
                .rotateXYZ(
                    this.gizmoRotate.x * (float) (Math.PI / 180.0),
                    this.gizmoRotate.y * (float) (Math.PI / 180.0),
                    this.gizmoRotate.z * (float) (Math.PI / 180.0)
                );
        }

        stateData.gizmoChildTransform.identity();
        stateData.gizmoChildAttachmentTransform.identity();
        stateData.selectedAttachmentIsChildAttachment = this.gizmoChild != null
            && this.gizmoChild.attachment != null
            && this.gizmoChild.attachment.equals(this.selectedAttachment);
        if (this.gizmoChild != null) {
            this.gizmoChild.getLocalTransform(stateData.gizmoChildTransform);
            this.gizmoChild.getAttachmentTransform(this.gizmoChild.attachment, stateData.gizmoChildAttachmentTransform);
            stateData.gizmoChildAttachmentTransformInv.set(stateData.gizmoChildAttachmentTransform).invert();
        }

        stateData.gizmoOriginTransform.identity();
        stateData.hasGizmoOrigin = this.gizmoOrigin != null;
        stateData.gizmoOriginIsGeometry = this.gizmoOrigin == this.originGeometry;
        if (this.gizmoOrigin != null && this.gizmoOrigin != this.gizmoParent) {
            this.gizmoOrigin.getGlobalTransform(stateData.gizmoOriginTransform);
        }

        stateData.gizmoParentTransform.identity();
        if (this.gizmoParent != null) {
            this.gizmoParent.getGlobalTransform(stateData.gizmoParentTransform);
        }
    }

    private float gridMult() {
        return 100 * this.gridDivisions;
    }

    private float zoomMult() {
        return (float)Math.exp(this.zoom * 0.2F) * 160.0F / Math.max(1.82F, 1.0F);
    }

    private static Matrix4f allocMatrix4f() {
        return BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    public static UI3DScene.Ray allocRay() {
        return TL_Ray_pool.get().alloc();
    }

    public static void releaseRay(UI3DScene.Ray ray) {
        TL_Ray_pool.get().release(ray);
    }

    public static UI3DScene.Plane allocPlane() {
        return TL_Plane_pool.get().alloc();
    }

    public static void releasePlane(UI3DScene.Plane Plane) {
        TL_Plane_pool.get().release(Plane);
    }

    private static Vector2 allocVector2() {
        return Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector2f allocVector2f() {
        return BaseVehicle.allocVector2f();
    }

    private static void releaseVector2f(Vector2f vector2f) {
        BaseVehicle.releaseVector2f(vector2f);
    }

    private static Vector3f allocVector3f() {
        return BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "clearAABBs":
                s_aabbPool.release(this.aabb);
                this.aabb.clear();
                return null;
            case "clearAxes":
                s_posRotPool.release(this.axes);
                this.axes.clear();
                return null;
            case "clearBox3Ds":
                s_box3DPool.release(this.box3d);
                this.box3d.clear();
                return null;
            case "clearGizmoRotate":
                this.gizmoRotate.set(0.0F);
                return null;
            case "clearHighlightBone":
                this.highlightBone.boneName = null;
                this.highlightPartBone.vehicle = null;
                return null;
            case "clearPhysicsMeshes":
                s_physicsMeshPool.releaseAll(this.physicsMesh);
                this.physicsMesh.clear();
                return null;
            case "getDrawGeometry":
                return this.drawGeometry ? Boolean.TRUE : Boolean.FALSE;
            case "getGeometryNames":
                ArrayList<String> names = new ArrayList<>();

                for (UI3DScene.SceneObject sceneObjectx : this.objects) {
                    if (sceneObjectx instanceof UI3DScene.SceneGeometry geometry) {
                        names.add(geometry.id);
                    }
                }

                return names;
            case "getGizmoPos":
                return this.gizmoPos;
            case "getGridMult":
                return BoxedStaticValues.toDouble(this.gridMult());
            case "getObjectNames":
                ArrayList<String> names = new ArrayList<>();

                for (UI3DScene.SceneObject sceneObject : this.objects) {
                    names.add(sceneObject.id);
                }

                return names;
            case "getView":
                return this.view.name();
            case "getViewRotation":
                return this.viewRotation;
            case "getModelCount":
                int count = 0;

                for (int i = 0; i < this.objects.size(); i++) {
                    if (this.objects.get(i) instanceof UI3DScene.SceneModel) {
                        count++;
                    }
                }

                return BoxedStaticValues.toDouble(count);
            case "rotateAllGeometry":
                Matrix4f m = allocMatrix4f().rotationXYZ(0.0F, (float) (Math.PI * 3.0 / 2.0), 0.0F);
                Matrix4f m2 = allocMatrix4f();
                Quaternionf q = allocQuaternionf();

                for (UI3DScene.SceneObject sceneObject : this.objects) {
                    if (sceneObject instanceof UI3DScene.SceneGeometry sceneGeometry) {
                        sceneGeometry.getLocalTransform(m2);
                        m.mul(m2, m2);
                        m2.getTranslation(sceneGeometry.translate);
                        q.setFromUnnormalized(m2);
                        q.getEulerAnglesXYZ(sceneGeometry.rotate);
                        sceneGeometry.rotate.mul(180.0F / (float)Math.PI);
                    }
                }

                releaseMatrix4f(m);
                releaseMatrix4f(m2);
                releaseQuaternionf(q);
                return null;
            case "stopGizmoTracking":
                if (this.gizmo != null) {
                    this.gizmo.stopTracking();
                }

                return null;
            default:
                throw new IllegalArgumentException("unhandled \"" + func + "\"");
        }
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "addCylinderAABB": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                this.aabb.add(object.getAABB(s_aabbPool.alloc()));
                return null;
            }
            case "createCharacter": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                }

                UI3DScene.SceneCharacter character = new UI3DScene.ScenePlayer(this, (String)arg0);
                character.initAnimatedModel();
                this.objects.add(character);
                return character;
            }
            case "createBox": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                }

                UI3DScene.SceneBox box = new UI3DScene.SceneBox(this, (String)arg0);
                this.objects.add(box);
                return box;
            }
            case "createCylinder": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                }

                UI3DScene.SceneCylinder cylinder = new UI3DScene.SceneCylinder(this, (String)arg0);
                cylinder.height = 1.0F;
                cylinder.radius = 0.5F;
                this.objects.add(cylinder);
                return cylinder;
            }
            case "createPolygon": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                } else {
                    UI3DScene.ScenePolygon scenePolygon = new UI3DScene.ScenePolygon(this, (String)arg0);

                    for (int ix = 0; ix < this.objects.size(); ix++) {
                        if (!(this.objects.get(ix) instanceof UI3DScene.SceneGeometry)) {
                            this.objects.add(ix, scenePolygon);
                            return scenePolygon;
                        }
                    }

                    this.objects.add(scenePolygon);
                    return scenePolygon;
                }
            }
            case "createVehicle": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                }

                UI3DScene.SceneVehicle vehicle = new UI3DScene.SceneVehicle(this, (String)arg0);
                this.objects.add(vehicle);
                return null;
            }
            case "getBoxMaxExtents": {
                UI3DScene.SceneBox sceneObject = this.getSceneObjectById((String)arg0, UI3DScene.SceneBox.class, true);
                return sceneObject.max;
            }
            case "getBoxMinExtents": {
                UI3DScene.SceneBox sceneObject = this.getSceneObjectById((String)arg0, UI3DScene.SceneBox.class, true);
                return sceneObject.min;
            }
            case "getCharacterAnimate": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                return character.animatedModel.isAnimate();
            }
            case "getCharacterAnimationDuration": {
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                AnimationPlayer animPlayerx = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayerx == null) {
                    return null;
                } else {
                    AnimationMultiTrack multiTrack = animPlayerx.getMultiTrack();
                    if (multiTrack != null && !multiTrack.getTracks().isEmpty()) {
                        return KahluaUtil.toDouble((double)multiTrack.getTracks().get(0).getDuration());
                    }

                    return null;
                }
            }
            case "getCharacterAnimationTime": {
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                AnimationPlayer animPlayer = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                } else {
                    AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                    if (multiTrack != null && !multiTrack.getTracks().isEmpty()) {
                        return KahluaUtil.toDouble((double)multiTrack.getTracks().get(0).getCurrentTimeValue());
                    }

                    return null;
                }
            }
            case "getCharacterShowBones": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                return character.showBones;
            }
            case "getCylinderHeight": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                return (double)object.height;
            }
            case "getCylinderRadius": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                return (double)object.radius;
            }
            case "getGeometryType":
                UI3DScene.SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, UI3DScene.SceneGeometry.class, false);
                return sceneGeometry == null ? null : sceneGeometry.getTypeName();
            case "getPolygonExtents": {
                UI3DScene.ScenePolygon geometry = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                return geometry.extents;
            }
            case "getPolygonPlane": {
                UI3DScene.ScenePolygon geometry = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                return geometry.plane.name();
            }
            case "getModelIgnoreVehicleScale": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                return sceneModel.ignoreVehicleScale ? Boolean.TRUE : Boolean.FALSE;
            }
            case "getModelScript":
                int count = 0;

                for (int i = 0; i < this.objects.size(); i++) {
                    if (this.objects.get(i) instanceof UI3DScene.SceneModel sceneModelx && count++ == ((Double)arg0).intValue()) {
                        return sceneModelx.modelScript;
                    }
                }

                return null;
            case "getModelSpriteModel": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                return sceneModel.spriteModel;
            }
            case "getObjectAutoRotate": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.autoRotate ? Boolean.TRUE : Boolean.FALSE;
            }
            case "getObjectExists":
                return this.getSceneObjectById((String)arg0, false) != null;
            case "getObjectParent": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parent == null ? null : sceneObject.parent.id;
            }
            case "getObjectParentAttachment": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentAttachment;
            }
            case "getObjectParentVehicle": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.vehicle.id;
            }
            case "getObjectParentVehiclePart": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.partId;
            }
            case "getObjectParentVehiclePartModel": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.partModelId;
            }
            case "getObjectParentVehiclePartModelAttachment": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.parentVehiclePart == null ? null : sceneObject.parentVehiclePart.attachmentName;
            }
            case "getObjectRotation": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.rotate;
            }
            case "getObjectScale": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.scale;
            }
            case "getObjectTranslation": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.translate;
            }
            case "getVehicleScript": {
                UI3DScene.SceneVehicle vehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                return vehicle.script;
            }
            case "isCharacterFemale": {
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                return sceneCharacter.animatedModel.isFemale();
            }
            case "isObjectVisible": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                return sceneObject.visible ? Boolean.TRUE : Boolean.FALSE;
            }
            case "moveCylinderToGround": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                UI3DScene.AABB aabb = object.getAABB(s_aabbPool.alloc());
                Boolean result = Boolean.FALSE;
                if (object.translate.y != aabb.h / 2.0F) {
                    object.translate.y = aabb.h / 2.0F;
                    result = Boolean.TRUE;
                }

                s_aabbPool.release(aabb);
                return result;
            }
            case "moveCylinderToOrigin": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                UI3DScene.AABB aabb = object.getAABB(s_aabbPool.alloc());
                object.translate.set(0.0F, aabb.h / 2.0F, 0.0F);
                s_aabbPool.release(aabb);
                return null;
            }
            case "recalculateBoxCenter": {
                UI3DScene.SceneBox box = this.getSceneObjectById((String)arg0, UI3DScene.SceneBox.class, true);
                Vector3f min = allocVector3f().set(box.min);
                Vector3f max = allocVector3f().set(box.max);
                Vector3f center = allocVector3f().set(max).add(min).mul(0.5F).setComponent(1, min.y);
                Matrix4f m = box.getLocalTransform(allocMatrix4f());
                min.sub(center);
                max.sub(center);
                m.transformPosition(center);
                box.translate.set(center);
                box.getLocalTransform(m);
                m.invert();
                box.min.set(min);
                box.max.set(max);
                releaseMatrix4f(m);
                releaseVector3f(center);
                releaseVector3f(max);
                releaseVector3f(min);
                return null;
            }
            case "removeModel": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                this.objects.remove(sceneModel);

                for (UI3DScene.SceneObject sceneObject : this.objects) {
                    if (sceneObject.parent == sceneModel) {
                        sceneObject.attachment = null;
                        sceneObject.parent = null;
                        sceneObject.parentAttachment = null;
                    }
                }

                return null;
            }
            case "removeObject": {
                UI3DScene.SceneObject object = this.getSceneObjectById((String)arg0, true);
                this.objects.remove(object);
                return null;
            }
            case "setDrawAttachments":
                this.drawAttachments = (Boolean)arg0;
                return null;
            case "setDrawGrid":
                this.drawGrid = (Boolean)arg0;
                return null;
            case "setDrawGridAxes":
                this.drawGridAxes = (Boolean)arg0;
                return null;
            case "setDrawGeometry":
                this.drawGeometry = (Boolean)arg0;
                return null;
            case "setDrawGridPlane":
                this.drawGridPlane = (Boolean)arg0;
                return null;
            case "setGizmoOrigin":
                String origin = (String)arg0;
                byte var69 = -1;
                switch (origin.hashCode()) {
                    case 3387192:
                        if (origin.equals("none")) {
                            var69 = 0;
                        }
                    default:
                        switch (var69) {
                            case 0:
                                this.gizmoParent = null;
                                this.gizmoOrigin = null;
                                this.gizmoChild = null;
                            default:
                                return null;
                        }
                }
            case "setGizmoPos":
                Vector3f newPos = (Vector3f)arg0;
                if (!this.gizmoPos.equals(newPos)) {
                    this.gizmoPos.set(newPos);
                }

                return null;
            case "setGizmoRotate":
                Vector3f newRot = (Vector3f)arg0;
                if (!this.gizmoRotate.equals(newRot)) {
                    this.gizmoRotate.set(newRot);
                }

                return null;
            case "setGizmoScale":
                this.gizmoScale = Math.max(((Double)arg0).floatValue(), 0.01F);
                return null;
            case "setGizmoVisible":
                String rst = (String)arg0;
                this.rotateGizmo.visible = "rotate".equalsIgnoreCase(rst);
                this.scaleGizmo.visible = "scale".equalsIgnoreCase(rst);
                this.translateGizmo.visible = "translate".equalsIgnoreCase(rst);
                switch (rst) {
                    case "rotate":
                        this.gizmo = this.rotateGizmo;
                        break;
                    case "scale":
                        this.gizmo = this.scaleGizmo;
                        break;
                    case "translate":
                        this.gizmo = this.translateGizmo;
                        break;
                    default:
                        this.gizmo = null;
                }

                return null;
            case "setGridMult":
                this.gridDivisions = PZMath.clamp(((Double)arg0).intValue(), 1, 100);
                return null;
            case "setGridPlane":
                this.gridPlane = UI3DScene.GridPlane.valueOf((String)arg0);
                return null;
            case "setMaxZoom":
                this.zoomMax = PZMath.clamp(((Double)arg0).intValue(), 1, 20);
                return null;
            case "setRotateGizmoSnap":
                this.rotateGizmo.snap = (Boolean)arg0;
                return null;
            case "setScaleGizmoSnap":
                this.scaleGizmo.snap = (Boolean)arg0;
                return null;
            case "setSelectedAttachment":
                this.selectedAttachment = (String)arg0;
                return null;
            case "setTransformMode":
                this.transformMode = UI3DScene.TransformMode.valueOf((String)arg0);
                return null;
            case "setZoom":
                this.zoom = PZMath.clamp(((Double)arg0).intValue(), 1, this.zoomMax);
                this.calcMatrices(this.projection, this.modelView);
                return null;
            case "setView":
                UI3DScene.View old = this.view;
                this.view = UI3DScene.View.valueOf((String)arg0);
                if (old != this.view) {
                    long ms = System.currentTimeMillis();
                    if (this.viewChangeTime + 350L < ms) {
                        this.modelViewChange.setFromUnnormalized(this.modelView);
                    }

                    this.viewChangeTime = ms;
                }

                this.calcMatrices(this.projection, this.modelView);
                return null;
            case "zoom":
                int delta = -((Double)arg0).intValue();
                float mouseX = Mouse.getXA() - this.getAbsoluteX().intValue();
                float mouseY = Mouse.getYA() - this.getAbsoluteY().intValue();
                float ox = this.uiToSceneX(mouseX, mouseY);
                float oy = this.uiToSceneY(mouseX, mouseY);
                this.zoom = PZMath.clamp(this.zoom + delta, 1, this.zoomMax);
                this.calcMatrices(this.projection, this.modelView);
                float nx = this.uiToSceneX(mouseX, mouseY);
                float ny = this.uiToSceneY(mouseX, mouseY);
                this.viewX = (int)(this.viewX - (nx - ox) * this.zoomMult());
                this.viewY = (int)(this.viewY + (ny - oy) * this.zoomMult());
                this.calcMatrices(this.projection, this.modelView);
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
        }
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        switch (func) {
            case "addAttachment": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                if (sceneModel.modelScript.getAttachmentById((String)arg1) != null) {
                    throw new IllegalArgumentException("model script \"" + arg0 + "\" already has attachment named \"" + arg1 + "\"");
                }

                ModelAttachment attach = new ModelAttachment((String)arg1);
                sceneModel.modelScript.addAttachment(attach);
                return attach;
            }
            case "addBoneAxis": {
                UI3DScene.PositionRotation axis = s_posRotPool.alloc();
                Matrix4f mat = allocMatrix4f().identity();
                mat.getTranslation(axis.pos);
                releaseMatrix4f(mat);
                Quaternionf q = mat.getUnnormalizedRotation(allocQuaternionf());
                q.getEulerAnglesXYZ(axis.rot);
                releaseQuaternionf(q);
                this.axes.add(axis);
                return null;
            }
            case "addPolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygon.points.add(new Vector2f((Vector2f)arg1));
                scenePolygon.triangulate();
                return null;
            }
            case "applyDeltaRotation":
                Vector3f eulerXYZ = (Vector3f)arg0;
                Vector3f deltaXYZ = (Vector3f)arg1;
                Quaternionf q0 = allocQuaternionf()
                    .rotationXYZ(eulerXYZ.x * (float) (Math.PI / 180.0), eulerXYZ.y * (float) (Math.PI / 180.0), eulerXYZ.z * (float) (Math.PI / 180.0));
                Quaternionf q1 = allocQuaternionf()
                    .rotationXYZ(deltaXYZ.x * (float) (Math.PI / 180.0), deltaXYZ.y * (float) (Math.PI / 180.0), deltaXYZ.z * (float) (Math.PI / 180.0));
                q0.mul(q1);
                q0.getEulerAnglesXYZ(eulerXYZ);
                releaseQuaternionf(q0);
                releaseQuaternionf(q1);
                eulerXYZ.mul(180.0F / (float)Math.PI);
                if (this.rotateGizmo.snap) {
                    eulerXYZ.x = (float)Math.floor(eulerXYZ.x + 0.5F);
                    eulerXYZ.y = (float)Math.floor(eulerXYZ.y + 0.5F);
                    eulerXYZ.z = (float)Math.floor(eulerXYZ.z + 0.5F);
                }

                return eulerXYZ;
            case "cloneObject":
                UI3DScene.SceneObject sceneObject1 = this.getSceneObjectById((String)arg0, true);
                UI3DScene.SceneObject sceneObject2 = this.getSceneObjectById((String)arg1, false);
                if (sceneObject2 != null) {
                    throw new IllegalStateException("scene object \"" + arg1 + "\" exists");
                }

                sceneObject2 = sceneObject1.clone((String)arg1);
                this.objects.add(sceneObject2);
                return sceneObject2;
            case "configDepthTexture":
                UI3DScene.SceneDepthTexture sceneDepthTexture = this.getSceneObjectById((String)arg0, UI3DScene.SceneDepthTexture.class, true);
                sceneDepthTexture.texture = (Texture)arg1;
                return sceneDepthTexture;
            case "copyGeometryFromSpriteGrid":
                String modID = (String)arg0;
                String tileName = (String)arg1;
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(tileName);
                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                int spriteGridIndex = spriteGrid.getSpriteIndex(sprite);
                int i = 0;

                for (; i < spriteGrid.getSpriteCount(); i++) {
                    if (i != spriteGridIndex) {
                        IsoSprite sprite2 = spriteGrid.getSpriteFromIndex(i);
                        if (sprite2 != null) {
                            String tilesetName = sprite2.tilesetName;
                            int col = sprite2.tileSheetIndex % 8;
                            int row = sprite2.tileSheetIndex / 8;
                            ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                            if (geometries != null && !geometries.isEmpty()) {
                                tilesetName = sprite.tilesetName;
                                col = sprite.tileSheetIndex % 8;
                                row = sprite.tileSheetIndex / 8;
                                TileGeometryManager.getInstance().copyGeometry(modID, tilesetName, col, row, geometries);
                                int gx1 = spriteGrid.getSpriteGridPosX(sprite);
                                int gy1 = spriteGrid.getSpriteGridPosY(sprite);
                                int gx2 = spriteGrid.getSpriteGridPosX(sprite2);
                                int gy2 = spriteGrid.getSpriteGridPosY(sprite2);
                                geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);

                                for (int j = 0; j < geometries.size(); j++) {
                                    TileGeometryFile.Geometry geometry = geometries.get(j);
                                    geometry.offset(gx2 - gx1, gy2 - gy1);
                                }
                                break;
                            }
                        }
                    }
                }

                return null;
            case "createModel": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                } else {
                    ModelScript modelScriptx = ScriptManager.instance.getModelScript((String)arg1);
                    if (modelScriptx == null) {
                        throw new NullPointerException("model script \"" + arg1 + "\" not found");
                    } else {
                        Model model = ModelManager.instance.getLoadedModel((String)arg1);
                        if (model == null) {
                            throw new NullPointerException("model \"" + arg1 + "\" not found");
                        }

                        UI3DScene.SceneModel sceneModelx = new UI3DScene.SceneModel(this, (String)arg0, modelScriptx, model);
                        this.objects.add(sceneModelx);
                        return null;
                    }
                }
            }
            case "dragGizmo":
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)arg1).floatValue();
                if (this.gizmo == null) {
                    throw new NullPointerException("gizmo is null");
                }

                this.gizmo.updateTracking(uiX, uiY);
                return null;
            case "dragView": {
                int x = ((Double)arg0).intValue();
                int y = ((Double)arg1).intValue();
                this.viewX -= x;
                this.viewY -= y;
                this.calcMatrices(this.projection, this.modelView);
                return null;
            }
            case "getCharacterAnimationKeyframeTimes":
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                AnimationPlayer animPlayerx = sceneCharacter.animatedModel.getAnimationPlayer();
                if (animPlayerx == null) {
                    return null;
                } else {
                    AnimationMultiTrack multiTrack = animPlayerx.getMultiTrack();
                    if (multiTrack != null && !multiTrack.getTracks().isEmpty()) {
                        AnimationTrack track = multiTrack.getTracks().get(0);
                        AnimationClip clip = track.getClip();
                        if (clip == null) {
                            return null;
                        }

                        if (arg1 == null) {
                            arg1 = new ArrayList();
                        }

                        ArrayList<Double> times = (ArrayList<Double>)arg1;
                        times.clear();
                        Keyframe[] keyframes = clip.getKeyframes();

                        for (int i = 0; i < keyframes.length; i++) {
                            Keyframe keyframe = keyframes[i];
                            Double time = KahluaUtil.toDouble((double)keyframe.time);
                            if (!times.contains(time)) {
                                times.add(time);
                            }
                        }

                        return times;
                    }

                    return null;
                }
            case "moveCylinderToOrigin": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                String axis = (String)arg1;
                UI3DScene.AABB aabb = object.getAABB(s_aabbPool.alloc());
                if (axis == null || "None".equalsIgnoreCase(axis)) {
                    object.translate.set(0.0F, aabb.h / 2.0F, 0.0F);
                } else if ("X".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(0, 0.0F);
                } else if ("Y".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(1, aabb.h / 2.0F);
                } else if ("Z".equalsIgnoreCase(axis)) {
                    object.translate.setComponent(2, 0.0F);
                }

                s_aabbPool.release(aabb);
                return null;
            }
            case "removeAttachment": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                ModelAttachment attach = sceneModel.modelScript.getAttachmentById((String)arg1);
                if (attach == null) {
                    throw new IllegalArgumentException("model script \"" + arg0 + "\" attachment \"" + arg1 + "\" not found");
                }

                sceneModel.modelScript.removeAttachment(attach);
                return null;
            }
            case "removePolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                int pointIndex = ((Double)arg1).intValue();
                if (scenePolygon.points.size() <= 3) {
                    return null;
                }

                scenePolygon.points.remove(pointIndex);
                scenePolygon.triangulate();
                return null;
            }
            case "setCharacterAlpha": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.animatedModel.setAlpha(((Double)arg1).floatValue());
                return null;
            }
            case "setCharacterAnimate": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.animatedModel.setAnimate((Boolean)arg1);
                return null;
            }
            case "setCharacterAnimationClip": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                AnimationSet animSet = AnimationSet.GetAnimationSet(character.animatedModel.GetAnimSetName(), false);
                if (animSet == null) {
                    return null;
                } else {
                    AnimState state = animSet.GetState(character.animatedModel.getState());
                    if (state != null && !state.nodes.isEmpty()) {
                        AnimNode node = state.nodes.get(0);
                        node.animName = (String)arg1;
                        character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                        character.animatedModel.getAdvancedAnimator().setState(state.name);
                        return null;
                    }

                    return null;
                }
            }
            case "setCharacterAnimationSpeed": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                AnimationMultiTrack multiTrack = character.animatedModel.getAnimationPlayer().getMultiTrack();
                if (multiTrack.getTracks().isEmpty()) {
                    return null;
                }

                multiTrack.getTracks().get(0).setSpeedDelta(PZMath.clamp(((Double)arg1).floatValue(), 0.0F, 10.0F));
                return null;
            }
            case "setCharacterAnimationTime": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.animatedModel.setTrackTime(((Double)arg1).floatValue());
                AnimationPlayer animPlayer = character.animatedModel.getAnimationPlayer();
                if (animPlayer == null) {
                    return null;
                } else {
                    AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                    if (multiTrack != null && !multiTrack.getTracks().isEmpty()) {
                        multiTrack.getTracks().get(0).setCurrentTimeValue(((Double)arg1).floatValue());
                        return null;
                    }

                    return null;
                }
            }
            case "setCharacterAnimSet": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                String animSet = (String)arg1;
                if (!animSet.equals(character.animatedModel.GetAnimSetName())) {
                    character.animatedModel.setAnimSetName(animSet);
                    character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                    ActionGroup actionGroup = ActionGroup.getActionGroup(character.animatedModel.GetAnimSetName());
                    ActionContext actionContext = character.animatedModel.getActionContext();
                    if (actionGroup != actionContext.getGroup()) {
                        actionContext.setGroup(actionGroup);
                    }

                    character.animatedModel
                        .getAdvancedAnimator()
                        .setState(actionContext.getCurrentStateName(), PZArrayUtil.listConvert(actionContext.getChildStates(), state -> state.getName()));
                }

                return null;
            }
            case "setCharacterClearDepthBuffer": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.clearDepthBuffer = (Boolean)arg1;
                return null;
            }
            case "setCharacterFemale": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                boolean bFemale = (Boolean)arg1;
                if (bFemale != character.animatedModel.isFemale()) {
                    character.animatedModel.setOutfitName("Naked", bFemale, false);
                }

                return null;
            }
            case "setCharacterShowBones": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.showBones = (Boolean)arg1;
                return null;
            }
            case "setCharacterShowBip01": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.showBip01 = (Boolean)arg1;
                return null;
            }
            case "setCharacterUseDeferredMovement": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.useDeferredMovement = (Boolean)arg1;
                return null;
            }
            case "setCylinderHeight": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                object.height = PZMath.clamp(((Double)arg1).floatValue(), 0.01F, 2.44949F);
                return null;
            }
            case "setCylinderRadius": {
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                object.radius = PZMath.clamp(((Double)arg1).floatValue(), 0.01F, 10.0F);
                return null;
            }
            case "setGeometryExtents": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygon.extents.set((Vector3f)arg1);
                return null;
            }
            case "setGeometrySelected":
                UI3DScene.SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, UI3DScene.SceneGeometry.class, true);
                sceneGeometry.selected = (Boolean)arg1;
                return null;
            case "setPolygonEditing": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygon.editing = (Boolean)arg1;
                return null;
            }
            case "setPolygonHighlightPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygon.highlightPointIndex = ((Double)arg1).intValue();
                return null;
            }
            case "setPolygonPlane":
                UI3DScene.ScenePolygon scenePolygonx = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygonx.plane = UI3DScene.GridPlane.valueOf((String)arg1);
                switch (scenePolygonx.plane) {
                    case XY:
                        scenePolygonx.rotate.set(0.0F, 0.0F, 0.0F);
                        break;
                    case XZ:
                        scenePolygonx.rotate.set(270.0F, 0.0F, 0.0F);
                        break;
                    case YZ:
                        scenePolygonx.rotate.set(0.0F, 90.0F, 0.0F);
                }

                if (scenePolygonx.points.isEmpty()) {
                    scenePolygonx.points.add(new Vector2f(-0.5F, -0.5F));
                    scenePolygonx.points.add(new Vector2f(0.5F, -0.5F));
                    scenePolygonx.points.add(new Vector2f(0.5F, 0.5F));
                    scenePolygonx.points.add(new Vector2f(-0.5F, 0.5F));
                }

                scenePolygonx.triangulate();
                return null;
            case "setGizmoAxisVisible":
                UI3DScene.Axis axisx = UI3DScene.Axis.valueOf((String)arg0);
                Boolean visible = (Boolean)arg1;
                switch (axisx) {
                    case X:
                        this.gizmoAxisVisibleX = visible;
                        break;
                    case Y:
                        this.gizmoAxisVisibleY = visible;
                        break;
                    case Z:
                        this.gizmoAxisVisibleZ = visible;
                }

                return null;
            case "setGizmoOrigin":
                String origin = (String)arg0;
                switch (origin) {
                    case "centerOfMass":
                        this.gizmoParent = this.getSceneObjectById((String)arg1, UI3DScene.SceneVehicle.class, true);
                        this.gizmoOrigin = this.gizmoParent;
                        this.gizmoChild = null;
                        break;
                    case "chassis":
                        UI3DScene.SceneVehicle vehiclex = this.getSceneObjectById((String)arg1, UI3DScene.SceneVehicle.class, true);
                        this.gizmoParent = vehiclex;
                        this.originGizmo.translate.set(vehiclex.script.getCenterOfMassOffset());
                        this.originGizmo.rotate.zero();
                        this.gizmoOrigin = this.originGizmo;
                        this.gizmoChild = null;
                        break;
                    case "character":
                        UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg1, UI3DScene.SceneCharacter.class, true);
                        this.gizmoParent = sceneCharacter;
                        this.gizmoOrigin = this.gizmoParent;
                        this.gizmoChild = null;
                        break;
                    case "model":
                        UI3DScene.SceneModel sceneModelx = this.getSceneObjectById((String)arg1, UI3DScene.SceneModel.class, true);
                        this.gizmoParent = sceneModelx;
                        this.gizmoOrigin = this.gizmoParent;
                        this.gizmoChild = null;
                        break;
                    case "object":
                        UI3DScene.SceneObject sceneObjectx = this.getSceneObjectById((String)arg1, true);
                        this.gizmoParent = sceneObjectx;
                        this.gizmoOrigin = this.gizmoParent;
                        this.gizmoChild = null;
                        break;
                    case "vehicleModel":
                        UI3DScene.SceneVehicle vehiclexx = this.getSceneObjectById((String)arg1, UI3DScene.SceneVehicle.class, true);
                        this.gizmoParent = vehiclexx;
                        this.originGizmo.translate.set(vehiclexx.script.getModel().getOffset());
                        this.originGizmo.rotate.zero();
                        this.gizmoOrigin = this.originGizmo;
                        this.gizmoChild = null;
                }

                return null;
            case "setCharacterState": {
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                character.animatedModel.setState((String)arg1);
                return null;
            }
            case "setHighlightBone": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                if (sceneObject instanceof UI3DScene.SceneCharacter sceneCharacter) {
                    String boneName = (String)arg1;
                    this.highlightBone.character = sceneCharacter;
                    this.highlightBone.sceneModel = null;
                    this.highlightBone.boneName = boneName;
                    this.highlightPartBone.vehicle = null;
                }

                if (sceneObject instanceof UI3DScene.SceneModel sceneModel) {
                    String boneName = (String)arg1;
                    this.highlightBone.character = null;
                    this.highlightBone.sceneModel = sceneModel;
                    this.highlightBone.boneName = boneName;
                    this.highlightPartBone.vehicle = null;
                }

                return null;
            }
            case "setModelIgnoreVehicleScale": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                sceneModel.ignoreVehicleScale = (Boolean)arg1;
                return null;
            }
            case "setModelScript": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                ModelScript modelScript = ScriptManager.instance.getModelScript((String)arg1);
                if (modelScript == null) {
                    throw new NullPointerException("model script \"" + arg1 + "\" not found");
                } else {
                    Model model = ModelManager.instance.getLoadedModel((String)arg1);
                    if (model == null) {
                        throw new NullPointerException("model \"" + arg1 + "\" not found");
                    }

                    sceneModel.modelScript = modelScript;
                    sceneModel.model = model;
                    return null;
                }
            }
            case "setModelSpriteModel": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                SpriteModel spriteModel = (SpriteModel)arg1;
                sceneModel.setSpriteModel(spriteModel);
                return null;
            }
            case "setModelSpriteModelEditor": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                sceneModel.spriteModelEditor = (Boolean)arg1;
                return null;
            }
            case "setModelUseWorldAttachment": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                sceneModel.useWorldAttachment = (Boolean)arg1;
                return null;
            }
            case "setModelWeaponRotationHack": {
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                sceneModel.weaponRotationHack = (Boolean)arg1;
                return null;
            }
            case "setObjectAutoRotate": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.autoRotate = (Boolean)arg1;
                if (!sceneObject.autoRotate) {
                    sceneObject.autoRotateAngle = 0.0F;
                }

                return null;
            }
            case "setObjectVisible": {
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.visible = (Boolean)arg1;
                return null;
            }
            case "setVehicleScript":
                UI3DScene.SceneVehicle vehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                vehicle.setScriptName((String)arg1);
                return null;
            case "subtractSpriteGridPixels":
                String modID = (String)arg0;
                String tileName = (String)arg1;
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(tileName);
                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                int spriteGridIndex = spriteGrid.getSpriteIndex(sprite);
                int gridPosX = spriteGrid.getSpriteGridPosX(sprite);
                int gridPosY = spriteGrid.getSpriteGridPosY(sprite);
                TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getTexture(modID, sprite.tilesetName, sprite.tileSheetIndex);

                for (int ix = 0; ix < spriteGrid.getSpriteCount(); ix++) {
                    if (ix != spriteGridIndex) {
                        IsoSprite sprite2 = spriteGrid.getSpriteFromIndex(ix);
                        if (sprite2 != null) {
                            Texture texture = sprite2.getTextureForCurrentFrame(IsoDirections.N);
                            if (texture != null && texture.getMask() != null) {
                                int gridPosX2 = spriteGrid.getSpriteGridPosX(sprite2);
                                int gridPosY2 = spriteGrid.getSpriteGridPosY(sprite2);
                                int dx = gridPosX - gridPosX2;
                                int dy = gridPosY - gridPosY2;

                                for (int yx = 0; yx < 256; yx++) {
                                    int y1 = yx + dx * 32 + dy * 32;

                                    for (int xx = 0; xx < 128; xx++) {
                                        int x1 = xx + dx * 64 - dy * 64;
                                        if (texture.isMaskSet(x1, y1)) {
                                            depthTexture.setPixel(xx, yx, -1.0F);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                depthTexture.updateGPUTexture();
                return null;
            case "testGizmoAxis": {
                int x = ((Double)arg0).intValue();
                int y = ((Double)arg1).intValue();
                if (this.gizmo == null) {
                    return "None";
                }

                UI3DScene.StateData stateData = this.stateDataMain();
                this.setModelViewProjection(stateData);
                this.setGizmoTransforms(stateData);
                return this.gizmo.hitTest(x, y).toString();
            }
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\"", func, arg0, arg1));
        }
    }

    public Object fromLua3(String func, Object arg0, Object arg1, Object arg2) {
        switch (func) {
            case "addAxis": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x, y, z));
                return null;
            }
            case "changeCylinderHeight":
                UI3DScene.SceneCylinder object = this.getSceneObjectById((String)arg0, UI3DScene.SceneCylinder.class, true);
                String what = (String)arg1;
                float newHeight = PZMath.clamp(((Double)arg2).floatValue(), 0.01F, 2.44949F);
                Matrix4f xfrm = object.getLocalTransform(allocMatrix4f());
                Vector3f zAxis = xfrm.transformDirection(allocVector3f().set(0.0F, 0.0F, 1.0F));
                if ("zMax".equalsIgnoreCase(what)) {
                    object.translate.add(zAxis.mul(newHeight - object.height).div(2.0F));
                } else if ("zMin".equalsIgnoreCase(what)) {
                    object.translate.add(zAxis.mul(-(newHeight - object.height)).div(2.0F));
                }

                object.height = newHeight;
                releaseMatrix4f(xfrm);
                releaseVector3f(zAxis);
                return null;
            case "copyGeometryFrom":
                String modID = (String)arg0;
                String tileNameDst = (String)arg1;
                String tileNameSrc = (String)arg2;
                IsoSprite spriteSrc = IsoSpriteManager.instance.getSprite(tileNameSrc);
                IsoSprite spriteDst = IsoSpriteManager.instance.getSprite(tileNameDst);
                String tilesetName = spriteSrc.tilesetName;
                int col = spriteSrc.tileSheetIndex % 8;
                int row = spriteSrc.tileSheetIndex / 8;
                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries != null && !geometries.isEmpty()) {
                    tilesetName = spriteDst.tilesetName;
                    col = spriteDst.tileSheetIndex % 8;
                    row = spriteDst.tileSheetIndex / 8;
                    TileGeometryManager.getInstance().copyGeometry(modID, tilesetName, col, row, geometries);
                    return null;
                }

                return null;
            case "createAnimal":
                UI3DScene.SceneObject sceneObjectx = this.getSceneObjectById((String)arg0, false);
                if (sceneObjectx != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                }

                UI3DScene.SceneCharacter character = new UI3DScene.SceneAnimal(this, (String)arg0, (AnimalDefinitions)arg1, (AnimalBreed)arg2);
                character.initAnimatedModel();
                this.objects.add(character);
                return character;
            case "getGeometryDepthAt":
                UI3DScene.SceneGeometry sceneGeometry = this.getSceneObjectById((String)arg0, UI3DScene.SceneGeometry.class, true);
                float tileX = ((Double)arg1).floatValue();
                float tileY = ((Double)arg2).floatValue();
                return (double)sceneGeometry.getNormalizedDepthAt(tileX, tileY);
            case "getPolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                int pointIndex = ((Double)arg1).intValue();
                ((Vector2f)arg2).set(scenePolygon.points.get(pointIndex));
                return arg2;
            }
            case "pickCharacterBone": {
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                float uiX = ((Double)arg1).floatValue();
                float uiY = ((Double)arg2).floatValue();
                return sceneCharacter.pickBone(uiX, uiY);
            }
            case "pickModelBone": {
                UI3DScene.SceneModel sceneModelxx = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                if (sceneModelxx.model.isStatic) {
                    return "";
                }

                float uiX = ((Double)arg1).floatValue();
                float uiY = ((Double)arg2).floatValue();
                return sceneModelxx.pickBone(uiX, uiY);
            }
            case "placeAttachmentAtOrigin":
                UI3DScene.SceneModel sceneModelx = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                ModelAttachment attachx = sceneModelx.modelScript.getAttachmentById((String)arg1);
                Boolean bWeaponRotationHackx = (Boolean)arg2;
                if (attachx == null) {
                    throw new IllegalArgumentException("model script \"" + arg0 + "\" attachment \"" + arg1 + "\" not found");
                }

                Matrix4f transform = allocMatrix4f();
                transform.identity();
                if (bWeaponRotationHackx) {
                    transform.rotateXYZ(0.0F, (float) Math.PI, (float) (Math.PI / 2));
                }

                Matrix4f attachmentXfrm = ModelInstanceRenderData.makeAttachmentTransform(attachx, allocMatrix4f());
                attachmentXfrm.invert();
                transform.mul(attachmentXfrm);
                transform.getTranslation(sceneModelx.translate);
                Quaternionf rotation = transform.getUnnormalizedRotation(allocQuaternionf());
                rotation.getEulerAnglesXYZ(sceneModelx.rotate);
                sceneModelx.rotate.mul(180.0F / (float)Math.PI);
                releaseQuaternionf(rotation);
                releaseMatrix4f(attachmentXfrm);
                releaseMatrix4f(transform);
                return null;
            case "polygonToUI": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                this.polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
                this.polygonEditor.planeToUI((Vector2f)arg1, (Vector2f)arg2);
                return arg2;
            }
            case "rasterizePolygon": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                scenePolygon.rasterize(
                    (xx, yx) -> LuaManager.caller
                        .protectedCallVoid(LuaManager.thread, arg1, arg2, BoxedStaticValues.toDouble(xx), BoxedStaticValues.toDouble(yx))
                );
                return null;
            }
            case "setAnimalDefinition":
                UI3DScene.SceneAnimal sceneAnimal = this.getSceneObjectById((String)arg0, UI3DScene.SceneAnimal.class, true);
                sceneAnimal.setAnimalDefinition((AnimalDefinitions)arg1, (AnimalBreed)arg2);
                return null;
            case "setAttachmentToOrigin":
                UI3DScene.SceneModel sceneModel = this.getSceneObjectById((String)arg0, UI3DScene.SceneModel.class, true);
                ModelAttachment attach = sceneModel.modelScript.getAttachmentById((String)arg1);
                Boolean bWeaponRotationHack = (Boolean)arg2;
                if (attach == null) {
                    throw new IllegalArgumentException("model script \"" + arg0 + "\" attachment \"" + arg1 + "\" not found");
                }

                Matrix4f transform = sceneModel.getGlobalTransform(allocMatrix4f());
                if (bWeaponRotationHack) {
                    Matrix4f transform2 = allocMatrix4f().rotationXYZ(0.0F, (float) Math.PI, (float) (Math.PI / 2));
                    transform2.invert();
                    transform2.mul(transform, transform);
                    releaseMatrix4f(transform2);
                }

                transform.invert();
                transform.getTranslation(attach.getOffset());
                Quaternionf rotation = transform.getUnnormalizedRotation(allocQuaternionf());
                rotation.getEulerAnglesXYZ(attach.getRotate());
                attach.getRotate().mul(180.0F / (float)Math.PI);
                releaseQuaternionf(rotation);
                releaseMatrix4f(transform);
                return null;
            case "setPolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                int pointIndex = ((Double)arg1).intValue();
                scenePolygon.points.get(pointIndex).set((Vector2f)arg2);
                scenePolygon.triangulate();
                return null;
            }
            case "setGizmoOrigin":
                String origin = (String)arg0;
                switch (origin) {
                    case "bone":
                        UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg1, true);
                        if (sceneObject instanceof UI3DScene.SceneCharacter sceneCharacterx) {
                            this.gizmoParent = sceneCharacterx;
                            this.originBone.character = sceneCharacterx;
                            this.originBone.sceneModel = null;
                            this.originBone.boneName = (String)arg2;
                            this.gizmoOrigin = this.originBone;
                            this.gizmoChild = null;
                        }

                        if (sceneObject instanceof UI3DScene.SceneModel sceneModel) {
                            this.gizmoParent = sceneModel;
                            this.originBone.character = null;
                            this.originBone.sceneModel = sceneModel;
                            this.originBone.boneName = (String)arg2;
                            this.gizmoOrigin = this.originBone;
                            this.gizmoChild = null;
                        }
                        break;
                    case "geometry":
                        UI3DScene.SceneGeometry sceneGeometryx = this.getSceneObjectById((String)arg1, UI3DScene.SceneGeometry.class, true);
                        this.gizmoParent = sceneGeometryx;
                        this.originGeometry.sceneGeometry = sceneGeometryx;
                        this.originGeometry.originHint = (String)arg2;
                        this.gizmoOrigin = this.originGeometry;
                        this.gizmoChild = null;
                }

                return null;
            case "setGizmoXYZ": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                this.gizmoPos.set(x, y, z);
                return null;
            }
            case "setShowVehiclePartBones":
                UI3DScene.SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                String partId = (String)arg1;
                String partModelId = (String)arg2;
                sceneVehicle.showBonesPartId = partId;
                sceneVehicle.showBonesModelId = partModelId;
                return null;
            case "startGizmoTracking": {
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)arg1).floatValue();
                UI3DScene.Axis axis = UI3DScene.Axis.valueOf((String)arg2);
                if (this.gizmo != null) {
                    this.gizmo.startTracking(uiX, uiY, axis);
                }

                return null;
            }
            case "setViewRotation": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                x %= 360.0F;
                y %= 360.0F;
                z %= 360.0F;
                this.viewRotation.set(x, y, z);
                return null;
            }
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2));
        }
    }

    public Object fromLua4(String func, Object arg0, Object arg1, Object arg2, Object arg3) {
        switch (func) {
            case "loadFromGeometryFile":
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                int i = this.objects.size() - 1;

                for (; i >= 0; i--) {
                    UI3DScene.SceneObject object = this.objects.get(i);
                    if (Type.tryCastTo(object, UI3DScene.SceneGeometry.class) != null) {
                        this.objects.remove(i);
                    }
                }

                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries == null) {
                    return null;
                } else {
                    int n = 1;

                    for (TileGeometryFile.Geometry tileGeometry : geometries) {
                        TileGeometryFile.Box tileBox = tileGeometry.asBox();
                        if (tileBox != null) {
                            UI3DScene.SceneBox sceneBox = new UI3DScene.SceneBox(this, "box" + n);
                            sceneBox.translate.set(tileBox.translate);
                            sceneBox.rotate.set(tileBox.rotate);
                            sceneBox.min.set(tileBox.min);
                            sceneBox.max.set(tileBox.max);
                            this.objects.add(n - 1, sceneBox);
                            n++;
                        } else {
                            TileGeometryFile.Cylinder tileCylinder = tileGeometry.asCylinder();
                            if (tileCylinder != null) {
                                UI3DScene.SceneCylinder sceneCylinder = new UI3DScene.SceneCylinder(this, "cylinder" + n);
                                sceneCylinder.translate.set(tileCylinder.translate);
                                sceneCylinder.rotate.set(tileCylinder.rotate);
                                sceneCylinder.radius = Math.max(tileCylinder.radius1, tileCylinder.radius2);
                                sceneCylinder.height = tileCylinder.height;
                                this.objects.add(n - 1, sceneCylinder);
                                n++;
                            } else {
                                TileGeometryFile.Polygon tilePolygon = tileGeometry.asPolygon();
                                if (tilePolygon != null) {
                                    UI3DScene.ScenePolygon scenePolygon = new UI3DScene.ScenePolygon(this, "polygon" + n);
                                    scenePolygon.translate.set(tilePolygon.translate);
                                    scenePolygon.rotate.set(tilePolygon.rotate);
                                    scenePolygon.plane = UI3DScene.GridPlane.valueOf(tilePolygon.plane.name());

                                    for (int ix = 0; ix < tilePolygon.points.size(); ix += 2) {
                                        scenePolygon.points.add(new Vector2f(tilePolygon.points.get(ix), tilePolygon.points.get(ix + 1)));
                                    }

                                    scenePolygon.triangulate();
                                    this.objects.add(n - 1, scenePolygon);
                                    n++;
                                }
                            }
                        }
                    }

                    return null;
                }
            case "pickPolygonEdge": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                int index = scenePolygon.pickEdge(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return BoxedStaticValues.toDouble(index);
            }
            case "pickPolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                int index = scenePolygon.pickPoint(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return BoxedStaticValues.toDouble(index);
            }
            case "setGizmoOrigin":
                String origin = (String)arg0;
                byte var37 = -1;
                switch (origin.hashCode()) {
                    case -1963501277:
                        if (origin.equals("attachment")) {
                            var37 = 0;
                        }
                    default:
                        switch (var37) {
                            case 0:
                                UI3DScene.SceneObject sceneObjectx = this.getSceneObjectById((String)arg1, true);
                                this.gizmoParent = this.getSceneObjectById((String)arg2, true);
                                this.originAttachment.object = this.gizmoParent;
                                this.originAttachment.attachmentName = (String)arg3;
                                this.gizmoOrigin = this.originAttachment;
                                this.gizmoChild = sceneObjectx;
                            default:
                                return null;
                        }
                }
            case "setHighlightPartBone":
                UI3DScene.SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                this.highlightPartBone.vehicle = sceneVehicle;
                this.highlightPartBone.partId = (String)arg1;
                this.highlightPartBone.partModelId = (String)arg2;
                this.highlightPartBone.attachmentName = (String)arg3;
                this.highlightBone.character = null;
                this.highlightBone.sceneModel = null;
                this.highlightBone.boneName = null;
                return null;
            case "setObjectParent":
                UI3DScene.SceneObject sceneObject1 = this.getSceneObjectById((String)arg0, true);
                sceneObject1.translate.zero();
                sceneObject1.rotate.zero();
                sceneObject1.attachment = (String)arg1;
                sceneObject1.parent = this.getSceneObjectById((String)arg2, false);
                sceneObject1.parentAttachment = (String)arg3;
                if (sceneObject1.parent != null && sceneObject1.parent.parent == sceneObject1) {
                    sceneObject1.parent.parent = null;
                }

                sceneObject1.parentVehiclePart = null;
                return null;
            case "setObjectPosition":
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, true);
                sceneObject.translate.set(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), ((Double)arg3).floatValue());
                return null;
            case "setPassengerPosition":
                UI3DScene.SceneCharacter character = this.getSceneObjectById((String)arg0, UI3DScene.SceneCharacter.class, true);
                UI3DScene.SceneVehicle vehicle = this.getSceneObjectById((String)arg1, UI3DScene.SceneVehicle.class, true);
                VehicleScript.Passenger pngr = vehicle.script.getPassengerById((String)arg2);
                if (pngr == null) {
                    return null;
                }

                VehicleScript.Position pos = pngr.getPositionById((String)arg3);
                if (pos != null) {
                    this.tempVector3f.set(vehicle.script.getModel().getOffset());
                    this.tempVector3f.add(pos.getOffset());
                    character.translate.set(this.tempVector3f);
                    character.rotate.set(pos.rotate);
                    character.parent = vehicle;
                    if (character.animatedModel != null) {
                        String animSet = "inside".equalsIgnoreCase(pos.getId()) ? "player-vehicle" : "player-editor";
                        if (!animSet.equals(character.animatedModel.GetAnimSetName())) {
                            character.animatedModel.setAnimSetName(animSet);
                            character.animatedModel.getAdvancedAnimator().OnAnimDataChanged(false);
                            ActionGroup actionGroup = ActionGroup.getActionGroup(character.animatedModel.GetAnimSetName());
                            ActionContext actionContext = character.animatedModel.getActionContext();
                            if (actionGroup != actionContext.getGroup()) {
                                actionContext.setGroup(actionGroup);
                            }

                            character.animatedModel
                                .getAdvancedAnimator()
                                .setState(
                                    actionContext.getCurrentStateName(), PZArrayUtil.listConvert(actionContext.getChildStates(), state -> state.getName())
                                );
                        }
                    }
                }

                return null;
            case "uiToPolygonPoint": {
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                this.polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
                this.polygonEditor.uiToPlane2D(((Double)arg1).floatValue(), ((Double)arg2).floatValue(), (Vector2f)arg3);
                return arg3;
            }
            case "uiToGrid":
                float uiX = ((Double)arg0).floatValue();
                float uiY = ((Double)arg1).floatValue();
                return this.uiToGrid(uiX, uiY, UI3DScene.GridPlane.valueOf((String)arg2), (Vector3f)arg3) ? Boolean.TRUE : Boolean.FALSE;
            case "updateGeometryFile":
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                ArrayList<TileGeometryFile.Geometry> geometries = new ArrayList<>();

                for (UI3DScene.SceneObject object : this.objects) {
                    if (object instanceof UI3DScene.SceneGeometry sceneGeometry) {
                        TileGeometryFile.Geometry tileGeometryx = sceneGeometry.toGeometryFileObject();
                        if (tileGeometryx != null) {
                            geometries.add(tileGeometryx);
                        }
                    }
                }

                TileGeometryManager.getInstance().setGeometry(modID, tilesetName, col, row, geometries);
                geometries.clear();
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3));
        }
    }

    public Object fromLua5(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        switch (func) {
            case "addPolygonPointOnEdge":
                UI3DScene.ScenePolygon scenePolygon = this.getSceneObjectById((String)arg0, UI3DScene.ScenePolygon.class, true);
                float mouseX = ((Double)arg1).floatValue();
                float mouseY = ((Double)arg2).floatValue();
                float pointX = ((Double)arg3).floatValue();
                float pointY = ((Double)arg4).floatValue();
                return BoxedStaticValues.toDouble(scenePolygon.addPointOnEdge(mouseX, mouseY, pointX, pointY));
            case "pickVehiclePartBone":
                UI3DScene.SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                String partId = (String)arg1;
                String partModelId = (String)arg2;
                float uiX = ((Double)arg3).floatValue();
                float uiY = ((Double)arg4).floatValue();
                VehicleScript.Part part = sceneVehicle.script.getPartById(partId);
                VehicleScript.Model model = part.getModelById(partModelId);
                return sceneVehicle.pickBone(part, model, uiX, uiY);
            case "setObjectParentToVehiclePart":
                UI3DScene.SceneObject sceneObject1 = this.getSceneObjectById((String)arg1, true);
                sceneObject1.translate.zero();
                sceneObject1.rotate.zero();
                sceneObject1.parent = null;
                sceneObject1.attachment = null;
                sceneObject1.parentAttachment = null;
                sceneObject1.parentVehiclePart = new UI3DScene.ParentVehiclePart();
                sceneObject1.parentVehiclePart.vehicle = this.getSceneObjectById((String)arg0, UI3DScene.SceneVehicle.class, true);
                sceneObject1.parentVehiclePart.partId = (String)arg2;
                sceneObject1.parentVehiclePart.partModelId = (String)arg3;
                sceneObject1.parentVehiclePart.attachmentName = (String)arg4;
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4));
        }
    }

    public Object fromLua6(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        switch (func) {
            case "addAABB": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float w = ((Double)arg3).floatValue();
                float h = ((Double)arg4).floatValue();
                float L = ((Double)arg5).floatValue();
                this.aabb.add(s_aabbPool.alloc().set(x, y, z, w, h, L, 1.0F, 1.0F, 1.0F, 1.0F, false));
                return null;
            }
            case "addAxis": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float rx = ((Double)arg3).floatValue();
                float ry = ((Double)arg4).floatValue();
                float rz = ((Double)arg5).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x, y, z, rx, ry, rz));
                return null;
            }
            case "addAxisRelativeToOrigin": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float rx = ((Double)arg3).floatValue();
                float ry = ((Double)arg4).floatValue();
                float rz = ((Double)arg5).floatValue();
                this.axes.add(s_posRotPool.alloc().set(x, y, z, rx, ry, rz));
                this.axes.get(this.axes.size() - 1).relativeToOrigin = true;
                return null;
            }
            case "addBox3D":
                Vector3f offset = (Vector3f)arg0;
                Vector3f extents = (Vector3f)arg1;
                Vector3f rotate = (Vector3f)arg2;
                float r = ((Double)arg3).floatValue();
                float g = ((Double)arg4).floatValue();
                float b = ((Double)arg5).floatValue();
                this.box3d
                    .add(
                        s_box3DPool.alloc()
                            .set(
                                offset.x,
                                offset.y,
                                offset.z,
                                -extents.x / 2.0F,
                                -extents.y / 2.0F,
                                -extents.z / 2.0F,
                                extents.x / 2.0F,
                                extents.y / 2.0F,
                                extents.z / 2.0F,
                                rotate.x,
                                rotate.y,
                                rotate.z,
                                r,
                                g,
                                b,
                                1.0F,
                                false
                            )
                    );
                return null;
            case "getAdjacentSeatingPosition":
                String modIDx = (String)arg0;
                UI3DScene.SceneCharacter sceneCharacter = this.getSceneObjectById((String)arg1, UI3DScene.SceneCharacter.class, true);
                String spriteName = (String)arg2;
                String sitDirection = (String)arg3;
                String side = (String)arg4;
                IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
                Vector2f localPos = (Vector2f)arg5;
                boolean valid = SeatingManager.getInstance()
                    .getAdjacentPosition(
                        modIDx,
                        sprite,
                        sitDirection,
                        side,
                        sceneCharacter.animatedModel.getAnimationPlayer().getModel(),
                        "player",
                        "sitonfurniture",
                        "SitOnFurniture" + side,
                        localPos
                    );
                if (valid) {
                    Vector3f xln = SeatingManager.getInstance().getTranslation(modIDx, sprite, sitDirection, new Vector3f());
                    localPos.sub(xln.x(), xln.y());
                    localPos.add(sceneCharacter.translate.x, sceneCharacter.translate.z);
                }

                return valid ? Boolean.TRUE : Boolean.FALSE;
            case "shiftGeometryByPixels":
                String modID = (String)arg0;
                String tilesetName = (String)arg1;
                int col = ((Double)arg2).intValue();
                int row = ((Double)arg3).intValue();
                int shiftX = ((Double)arg4).intValue();
                int shiftY = ((Double)arg5).intValue();
                ArrayList<TileGeometryFile.Geometry> geometries = TileGeometryManager.getInstance().getGeometry(modID, tilesetName, col, row);
                if (geometries == null) {
                    return null;
                } else {
                    int TILE_WIDTH = 128;
                    float onePixel = 0.015625F;

                    for (TileGeometryFile.Geometry tileGeometry : geometries) {
                        TileGeometryFile.Box tileBox = tileGeometry.asBox();
                        if (tileBox != null) {
                            tileBox.translate.add(shiftX * 0.0078125F, 0.0F, -shiftX * 0.0078125F);
                        } else {
                            TileGeometryFile.Cylinder tileCylinder = tileGeometry.asCylinder();
                            if (tileCylinder != null) {
                                tileCylinder.translate.add(shiftX * 0.0078125F, 0.0F, -shiftX * 0.0078125F);
                            } else {
                                TileGeometryFile.Polygon tilePolygon = tileGeometry.asPolygon();
                                if (tilePolygon != null) {
                                    tilePolygon.translate.add(shiftX * 0.0078125F, 0.0F, -shiftX * 0.0078125F);
                                }
                            }
                        }
                    }

                    return null;
                }
            case "renderSpriteGridTextureMask": {
                UI3DScene.SpriteGridTextureMaskDrawer drawer = new UI3DScene.SpriteGridTextureMaskDrawer();
                drawer.scene = this;
                drawer.sx = ((Double)arg0).floatValue();
                drawer.sy = ((Double)arg1).floatValue();
                drawer.sx2 = ((Double)arg2).floatValue();
                drawer.sy2 = ((Double)arg3).floatValue();
                drawer.pixelSize = ((Double)arg4).floatValue();
                drawer.sprite = (IsoSprite)arg5;
                drawer.r = 1.0F;
                drawer.g = 0.0F;
                drawer.b = 0.0F;
                drawer.a = 1.0F;
                SpriteRenderer.instance.drawGeneric(drawer);
                return null;
            }
            case "renderTextureMask": {
                UI3DScene.TextureMaskDrawer drawer = new UI3DScene.TextureMaskDrawer();
                drawer.scene = this;
                drawer.sx = ((Double)arg0).floatValue();
                drawer.sy = ((Double)arg1).floatValue();
                drawer.sx2 = ((Double)arg2).floatValue();
                drawer.sy2 = ((Double)arg3).floatValue();
                drawer.pixelSize = ((Double)arg4).floatValue();
                drawer.texture = (Texture)arg5;
                drawer.r = 1.0F;
                drawer.g = 0.0F;
                drawer.b = 0.0F;
                drawer.a = 1.0F;
                SpriteRenderer.instance.drawGeneric(drawer);
                return null;
            }
            case "setGizmoOrigin":
                String origin = (String)arg0;
                byte col = -1;
                switch (origin.hashCode()) {
                    case 211153215:
                        if (origin.equals("vehiclePart")) {
                            col = 0;
                        }
                    default:
                        switch (col) {
                            case 0:
                                UI3DScene.SceneVehicle sceneVehicle = this.getSceneObjectById((String)arg1, UI3DScene.SceneVehicle.class, true);
                                this.gizmoParent = sceneVehicle;
                                this.originVehiclePart.vehicle = sceneVehicle;
                                this.originVehiclePart.partId = (String)arg2;
                                this.originVehiclePart.partModelId = (String)arg3;
                                this.originVehiclePart.attachmentName = (String)arg4;
                                this.originVehiclePart.boneOnly = (Boolean)arg5;
                                this.gizmoOrigin = this.originVehiclePart;
                                this.gizmoChild = null;
                            default:
                                return null;
                        }
                }
            default:
                throw new IllegalArgumentException(
                    String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4, arg5)
                );
        }
    }

    public Object fromLua7(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        switch (func) {
            case "addBox3D": {
                Vector3f translate = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                Vector3f min = (Vector3f)arg2;
                Vector3f max = (Vector3f)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                this.box3d
                    .add(
                        s_box3DPool.alloc()
                            .set(
                                translate.x,
                                translate.y,
                                translate.z,
                                min.x,
                                min.y,
                                min.z,
                                max.x,
                                max.y,
                                max.z,
                                rotate.x,
                                rotate.y,
                                rotate.z,
                                r,
                                g,
                                b,
                                1.0F,
                                false
                            )
                    );
                return null;
            }
            case "addPhysicsMesh": {
                Vector3f offset = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                float scale = ((Double)arg2).floatValue();
                String physicsShapeScriptName = (String)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                this.physicsMesh.add(s_physicsMeshPool.alloc().set(offset, rotate, scale, physicsShapeScriptName, r, g, b));
                return null;
            }
            case "createDepthTexture":
                UI3DScene.SceneObject sceneObject = this.getSceneObjectById((String)arg0, false);
                if (sceneObject != null) {
                    throw new IllegalStateException("scene object \"" + arg0 + "\" exists");
                } else {
                    UI3DScene.SceneDepthTexture sceneDepthTexture = new UI3DScene.SceneDepthTexture(this, (String)arg0);
                    sceneDepthTexture.texture = (Texture)arg1;

                    for (int i = 0; i < this.objects.size(); i++) {
                        if (!(this.objects.get(i) instanceof UI3DScene.SceneDepthTexture)) {
                            this.objects.add(i, sceneDepthTexture);
                            return sceneDepthTexture;
                        }
                    }

                    this.objects.add(sceneDepthTexture);
                    return sceneDepthTexture;
                }
            default:
                throw new IllegalArgumentException(
                    String.format("unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"", func, arg0, arg1, arg2, arg3, arg4, arg5, arg6)
                );
        }
    }

    public Object fromLua9(String func, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8) {
        switch (func) {
            case "addAABB": {
                float x = ((Double)arg0).floatValue();
                float y = ((Double)arg1).floatValue();
                float z = ((Double)arg2).floatValue();
                float w = ((Double)arg3).floatValue();
                float h = ((Double)arg4).floatValue();
                float L = ((Double)arg5).floatValue();
                float r = ((Double)arg6).floatValue();
                float g = ((Double)arg7).floatValue();
                float b = ((Double)arg8).floatValue();
                this.aabb.add(s_aabbPool.alloc().set(x, y, z, w, h, L, r, g, b, 1.0F, false));
                return null;
            }
            case "addBox3D": {
                Vector3f translate = (Vector3f)arg0;
                Vector3f rotate = (Vector3f)arg1;
                Vector3f min = (Vector3f)arg2;
                Vector3f max = (Vector3f)arg3;
                float r = ((Double)arg4).floatValue();
                float g = ((Double)arg5).floatValue();
                float b = ((Double)arg6).floatValue();
                float a = ((Double)arg7).floatValue();
                boolean bQuads = (Boolean)arg8;
                this.box3d
                    .add(
                        s_box3DPool.alloc()
                            .set(
                                translate.x,
                                translate.y,
                                translate.z,
                                min.x,
                                min.y,
                                min.z,
                                max.x,
                                max.y,
                                max.z,
                                rotate.x,
                                rotate.y,
                                rotate.z,
                                r,
                                g,
                                b,
                                a,
                                bQuads
                            )
                    );
                return null;
            }
            default:
                throw new IllegalArgumentException(
                    String.format(
                        "unhandled \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\" \"%s\"",
                        func,
                        arg0,
                        arg1,
                        arg2,
                        arg3,
                        arg4,
                        arg5,
                        arg6,
                        arg7,
                        arg8
                    )
                );
        }
    }

    private int screenWidth() {
        return (int)this.width;
    }

    private int screenHeight() {
        return (int)this.height;
    }

    public float uiToSceneX(float uiX, float uiY) {
        float sceneX = uiX - this.screenWidth() / 2.0F;
        sceneX += this.viewX;
        return sceneX / this.zoomMult();
    }

    public float uiToSceneY(float uiX, float uiY) {
        float sceneY = uiY - this.screenHeight() / 2.0F;
        sceneY *= -1.0F;
        sceneY -= this.viewY;
        return sceneY / this.zoomMult();
    }

    public Vector3f uiToScene(float uiX, float uiY, float uiZ, Vector3f out) {
        this.uiToScene(null, uiX, uiY, uiZ, out);
        switch (this.view) {
            case Left:
            case Right:
                out.x = 0.0F;
                break;
            case Top:
            case Bottom:
                out.y = 0.0F;
                break;
            case Front:
            case Back:
                out.z = 0.0F;
        }

        if (this.view == UI3DScene.View.UserDefined) {
            Vector3f orientation = allocVector3f();
            switch (this.gridPlane) {
                case XY:
                    orientation.set(0.0F, 0.0F, 1.0F);
                    break;
                case XZ:
                    orientation.set(0.0F, 1.0F, 0.0F);
                    break;
                case YZ:
                    orientation.set(1.0F, 0.0F, 0.0F);
            }

            Vector3f center = allocVector3f().set(0.0F);
            UI3DScene.Plane plane = allocPlane().set(orientation, center);
            releaseVector3f(orientation);
            releaseVector3f(center);
            UI3DScene.Ray cameraRay = this.getCameraRay(uiX, this.screenHeight() - uiY, allocRay());
            if (intersect_ray_plane(plane, cameraRay, out) != 1) {
                out.set(0.0F);
            }

            releasePlane(plane);
            releaseRay(cameraRay);
        }

        return out;
    }

    public Vector3f uiToScene(Matrix4f modelTransform, float uiX, float uiY, float uiZ, Vector3f out) {
        uiY = this.screenHeight() - uiY;
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        if (modelTransform != null) {
            matrix4f.mul(modelTransform);
        }

        matrix4f.invert();
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.unprojectInv(uiX, uiY, uiZ, this.viewport, out);
        releaseMatrix4f(matrix4f);
        return out;
    }

    public float sceneToUIX(float sceneX, float sceneY, float sceneZ) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, this.tempVector3f);
        releaseMatrix4f(matrix4f);
        return this.tempVector3f.x();
    }

    public float sceneToUIY(float sceneX, float sceneY, float sceneZ) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, this.tempVector3f);
        releaseMatrix4f(matrix4f);
        return this.screenHeight() - this.tempVector3f.y();
    }

    public float sceneToUIX(Vector3f scenePos) {
        return this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
    }

    public float sceneToUIY(Vector3f scenePos) {
        return this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
    }

    public boolean uiToGrid(float uiX, float uiY, UI3DScene.GridPlane gridPlane, Vector3f outScenePos) {
        UI3DScene.Plane plane = allocPlane();
        plane.point.set(0.0F);
        switch (gridPlane) {
            case XY:
                plane.normal.set(0.0F, 0.0F, 1.0F);
                break;
            case XZ:
                plane.normal.set(0.0F, 1.0F, 0.0F);
                break;
            case YZ:
                plane.normal.set(1.0F, 0.0F, 0.0F);
        }

        UI3DScene.Ray cameraRay = this.getCameraRay(uiX, this.screenHeight() - uiY, allocRay());
        boolean hitPlane = intersect_ray_plane(plane, cameraRay, outScenePos) == 1;
        if (!hitPlane) {
            outScenePos.set(0.0F);
        }

        releasePlane(plane);
        releaseRay(cameraRay);
        return hitPlane;
    }

    private void renderGridXY(int div) {
        for (int x = -5; x < 5; x++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(x + (float)i / div, -5.0F, 0.0F, x + (float)i / div, 5.0F, 0.0F, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int y = -5; y < 5; y++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(-5.0F, y + (float)i / div, 0.0F, 5.0F, y + (float)i / div, 0.0F, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int x = -5; x <= 5; x++) {
            vboRenderer.addLine(x, -5.0F, 0.0F, x, 5.0F, 0.0F, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        for (int y = -5; y <= 5; y++) {
            vboRenderer.addLine(-5.0F, y, 0.0F, 5.0F, y, 0.0F, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        if (this.drawGridAxes) {
            int z = 0;
            vboRenderer.addLine(-5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, this.gridAlpha);
            z = 0;
            vboRenderer.addLine(0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, 0.0F, this.gridAlpha);
        }
    }

    private void renderGridXZ(int div) {
        for (int x = -5; x < 5; x++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(x + (float)i / div, 0.0F, -5.0F, x + (float)i / div, 0.0F, 5.0F, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int z = -5; z < 5; z++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(-5.0F, 0.0F, z + (float)i / div, 5.0F, 0.0F, z + (float)i / div, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int x = -5; x <= 5; x++) {
            vboRenderer.addLine(x, 0.0F, -5.0F, x, 0.0F, 5.0F, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        for (int z = -5; z <= 5; z++) {
            vboRenderer.addLine(-5.0F, 0.0F, z, 5.0F, 0.0F, z, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        if (this.drawGridAxes) {
            int z = 0;
            vboRenderer.addLine(-5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, this.gridAlpha);
            z = 0;
            vboRenderer.addLine(0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.gridAlpha);
        }
    }

    private void renderGridYZ(int div) {
        for (int y = -5; y < 5; y++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(0.0F, y + (float)i / div, -5.0F, 0.0F, y + (float)i / div, 5.0F, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int z = -5; z < 5; z++) {
            for (int i = 1; i < div; i++) {
                vboRenderer.addLine(0.0F, -5.0F, z + (float)i / div, 0.0F, 5.0F, z + (float)i / div, 0.2F, 0.2F, 0.2F, this.gridAlpha);
            }
        }

        for (int y = -5; y <= 5; y++) {
            vboRenderer.addLine(0.0F, y, -5.0F, 0.0F, y, 5.0F, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        for (int z = -5; z <= 5; z++) {
            vboRenderer.addLine(0.0F, -5.0F, z, 0.0F, 5.0F, z, 0.1F, 0.1F, 0.1F, this.gridAlpha);
        }

        if (this.drawGridAxes) {
            int z = 0;
            vboRenderer.addLine(0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, 0.0F, this.gridAlpha);
            z = 0;
            vboRenderer.addLine(0.0F, 0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 1.0F, this.gridAlpha);
        }
    }

    private void renderGrid() {
        vboRenderer.startRun(vboRenderer.formatPositionColor);
        vboRenderer.setMode(1);
        vboRenderer.setLineWidth(1.0F);
        this.gridAlpha = 1.0F;
        long ms = System.currentTimeMillis();
        if (this.viewChangeTime + 350L > ms) {
            float f = (float)(this.viewChangeTime + 350L - ms) / 350.0F;
            this.gridAlpha = 1.0F - f;
            this.gridAlpha = this.gridAlpha * this.gridAlpha;
        }

        switch (this.view) {
            case Left:
            case Right:
                this.renderGridYZ(10);
                vboRenderer.endRun();
                return;
            case Top:
            case Bottom:
                this.renderGridXZ(10);
                vboRenderer.endRun();
                return;
            case Front:
            case Back:
                this.renderGridXY(10);
                vboRenderer.endRun();
                return;
            default:
                switch (this.gridPlane) {
                    case XY:
                        this.renderGridXY(10);
                        vboRenderer.endRun();
                        break;
                    case XZ:
                        this.renderGridXZ(10);
                        vboRenderer.endRun();
                        break;
                    case YZ:
                        this.renderGridYZ(10);
                        vboRenderer.endRun();
                }
        }
    }

    void renderAxis(UI3DScene.PositionRotation axis) {
        this.renderAxis(axis.pos, axis.rot, axis.relativeToOrigin);
    }

    void renderAxis(Vector3f pos, Vector3f rot, boolean bRelativeToOrigin) {
        UI3DScene.StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        Matrix4f matrix4f = allocMatrix4f().identity();
        if (!bRelativeToOrigin) {
            matrix4f.mul(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
        }

        matrix4f.translate(pos);
        matrix4f.rotateXYZ(rot.x * (float) (Math.PI / 180.0), rot.y * (float) (Math.PI / 180.0), rot.z * (float) (Math.PI / 180.0));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        releaseMatrix4f(matrix4f);
        float LENGTH = 0.1F;
        Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 3.0F);
        PZGLUtil.popMatrix(5888);
    }

    private void renderAABB(
        float x, float y, float z, float xMin, float yMin, float zMin, float xMax, float yMax, float zMax, float r, float g, float b, float a, boolean bQuads
    ) {
        vboRenderer.addAABB(x, y, z, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, a, bQuads);
    }

    private void renderAABB(float x, float y, float z, Vector3f min, Vector3f max, float r, float g, float b) {
        vboRenderer.addAABB(x, y, z, min, max, r, g, b);
    }

    private void renderBox3D(
        float x,
        float y,
        float z,
        float xMin,
        float yMin,
        float zMin,
        float xMax,
        float yMax,
        float zMax,
        float rx,
        float ry,
        float rz,
        float r,
        float g,
        float b,
        float a,
        boolean bQuads
    ) {
        UI3DScene.StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.identity();
        matrix4f.translate(x, y, z);
        matrix4f.rotateXYZ(rx * (float) (Math.PI / 180.0), ry * (float) (Math.PI / 180.0), rz * (float) (Math.PI / 180.0));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        releaseMatrix4f(matrix4f);
        this.renderAABB(x * 0.0F, y * 0.0F, z * 0.0F, xMin, yMin, zMin, xMax, yMax, zMax, r, g, b, a, bQuads);
        vboRenderer.flush();
        PZGLUtil.popMatrix(5888);
    }

    private void renderPhysicsMesh(float x, float y, float z, float rx, float ry, float rz, float r, float g, float b, float[] points) {
        UI3DScene.StateData stateData = this.stateDataRender();
        vboRenderer.flush();
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.identity();
        matrix4f.translate(x, y, z);
        matrix4f.rotateXYZ(rx * (float) (Math.PI / 180.0), ry * (float) (Math.PI / 180.0), rz * (float) (Math.PI / 180.0));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        releaseMatrix4f(matrix4f);
        vboRenderer.startRun(vboRenderer.formatPositionColor);
        vboRenderer.setMode(1);

        for (int i = 0; i < points.length / 3 - 1; i++) {
            int i1 = i * 3;
            int i2 = (i + 1) * 3;
            vboRenderer.addLine(points[i1], points[i1 + 1], points[i1 + 2], points[i2], points[i2 + 1], points[i2 + 2], r, g, b, 1.0F);
        }

        vboRenderer.endRun();
        vboRenderer.flush();
        PZGLUtil.popMatrix(5888);
    }

    private void calcMatrices(Matrix4f projection, Matrix4f modelView) {
        float w = this.screenWidth();
        float scale = 1366.0F / w;
        float h = this.screenHeight() * scale;
        w = 1366.0F;
        w /= this.zoomMult();
        h /= this.zoomMult();
        projection.setOrtho(-w / 2.0F, w / 2.0F, -h / 2.0F, h / 2.0F, -10.0F, 10.0F);
        float m_view_x = this.viewX / this.zoomMult() * scale;
        float m_view_y = this.viewY / this.zoomMult() * scale;
        projection.translate(-m_view_x, m_view_y, 0.0F);
        modelView.identity();
        float rotateX = 0.0F;
        float rotateY = 0.0F;
        float rotateZ = 0.0F;
        switch (this.view) {
            case Left:
                rotateY = 270.0F;
                break;
            case Right:
                rotateY = 90.0F;
                break;
            case Top:
                rotateY = 90.0F;
                rotateZ = 90.0F;
                break;
            case Bottom:
                rotateY = 90.0F;
                rotateZ = 270.0F;
            case Front:
            default:
                break;
            case Back:
                rotateY = 180.0F;
                break;
            case UserDefined:
                rotateX = this.viewRotation.x;
                rotateY = this.viewRotation.y;
                rotateZ = this.viewRotation.z;
        }

        modelView.rotateXYZ(rotateX * (float) (Math.PI / 180.0), rotateY * (float) (Math.PI / 180.0), rotateZ * (float) (Math.PI / 180.0));
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, UI3DScene.Ray camera_ray) {
        return this.getCameraRay(uiX, uiY, this.projection, this.modelView, camera_ray);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, UI3DScene.Ray camera_ray) {
        return this.getCameraRay(uiX, uiY, projection, modelView, this.screenWidth(), this.screenHeight(), camera_ray);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, int viewWidth, int viewHeight, UI3DScene.Ray camera_ray) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        matrix4f.invert();
        this.viewport[2] = viewWidth;
        this.viewport[3] = viewHeight;
        Vector3f ray_start = matrix4f.unprojectInv(uiX, uiY, 0.0F, this.viewport, allocVector3f());
        Vector3f ray_end = matrix4f.unprojectInv(uiX, uiY, 1.0F, this.viewport, allocVector3f());
        camera_ray.origin.set(ray_start);
        camera_ray.direction.set(ray_end.sub(ray_start).normalize());
        releaseVector3f(ray_end);
        releaseVector3f(ray_start);
        releaseMatrix4f(matrix4f);
        return camera_ray;
    }

    public static float closest_distance_between_lines(UI3DScene.Ray l1, UI3DScene.Ray l2) {
        Vector3f u = allocVector3f().set(l1.direction);
        Vector3f v = allocVector3f().set(l2.direction);
        Vector3f w = allocVector3f().set(l1.origin).sub(l2.origin);
        float a = u.dot(u);
        float b = u.dot(v);
        float c = v.dot(v);
        float d = u.dot(w);
        float e = v.dot(w);
        float D = a * c - b * b;
        float sc;
        float tc;
        if (D < 1.0E-8F) {
            sc = 0.0F;
            tc = b > c ? d / b : e / c;
        } else {
            sc = (b * e - c * d) / D;
            tc = (a * e - b * d) / D;
        }

        Vector3f dP = w.add(u.mul(sc)).sub(v.mul(tc));
        l1.t = sc;
        l2.t = tc;
        releaseVector3f(u);
        releaseVector3f(v);
        releaseVector3f(w);
        return dP.length();
    }

    static Vector3f project(Vector3f A, Vector3f B, Vector3f out) {
        return out.set(B).mul(A.dot(B) / B.dot(B));
    }

    static Vector3f reject(Vector3f A, Vector3f B, Vector3f out) {
        Vector3f P = project(A, B, allocVector3f());
        out.set(A).sub(P);
        releaseVector3f(P);
        return out;
    }

    public static int intersect_ray_plane(UI3DScene.Plane Pn, UI3DScene.Ray S, Vector3f out) {
        Vector3f u = allocVector3f().set(S.direction).mul(100.0F);
        Vector3f w = allocVector3f().set(S.origin).sub(Pn.point);

        byte sI;
        try {
            float D = Pn.normal.dot(u);
            float N = -Pn.normal.dot(w);
            if (!(Math.abs(D) < 1.0E-8F)) {
                float sIx = N / D;
                if (!(sIx < 0.0F) && !(sIx > 1.0F)) {
                    out.set(S.origin).add(u.mul(sIx));
                    return 1;
                }

                return 0;
            }

            if (N != 0.0F) {
                return 0;
            }

            sI = 2;
        } finally {
            releaseVector3f(u);
            releaseVector3f(w);
        }

        return sI;
    }

    public static float distance_between_point_ray(Vector3f P, UI3DScene.Ray L) {
        Vector3f v = allocVector3f().set(L.direction).mul(100.0F);
        Vector3f w = allocVector3f().set(P).sub(L.origin);
        float c1 = w.dot(v);
        float c2 = v.dot(v);
        float b = c1 / c2;
        Vector3f Pb = v.mul(b).add(L.origin);
        float dist = Pb.sub(P).length();
        releaseVector3f(w);
        releaseVector3f(v);
        return dist;
    }

    public static float closest_distance_line_circle(UI3DScene.Ray ray, UI3DScene.Circle c, Vector3f point) {
        UI3DScene.Plane plane = allocPlane().set(c.orientation, c.center);
        Vector3f on_plane = allocVector3f();
        float dist;
        if (intersect_ray_plane(plane, ray, on_plane) == 1) {
            point.set(on_plane).sub(c.center).normalize().mul(c.radius).add(c.center);
            dist = on_plane.sub(point).length();
        } else {
            Vector3f A = allocVector3f().set(ray.origin).sub(c.center);
            Vector3f rt = reject(A, c.orientation, allocVector3f());
            point.set(rt.normalize().mul(c.radius).add(c.center));
            dist = distance_between_point_ray(point, ray);
            releaseVector3f(rt);
            releaseVector3f(A);
        }

        releaseVector3f(on_plane);
        releasePlane(plane);
        return dist;
    }

    private UI3DScene.StateData stateDataMain() {
        return this.stateData[SpriteRenderer.instance.getMainStateIndex()];
    }

    private UI3DScene.StateData stateDataRender() {
        return this.stateData[SpriteRenderer.instance.getRenderStateIndex()];
    }

    private static final class AABB {
        float x;
        float y;
        float z;
        float w;
        float h;
        float l;
        float r;
        float g;
        float b;
        float a;
        boolean quads;

        UI3DScene.AABB set(UI3DScene.AABB rhs) {
            return this.set(rhs.x, rhs.y, rhs.z, rhs.w, rhs.h, rhs.l, rhs.r, rhs.g, rhs.b, rhs.a, rhs.quads);
        }

        UI3DScene.AABB set(float x, float y, float z, float w, float h, float L, float r, float g, float b, float a, boolean bQuads) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.l = L;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.quads = bQuads;
            return this;
        }
    }

    static enum Axis {
        None,
        X,
        Y,
        Z,
        XY,
        XZ,
        YZ;
    }

    private static final class Box3D {
        float x;
        float y;
        float z;
        float xMin;
        float yMin;
        float zMin;
        float xMax;
        float yMax;
        float zMax;
        float rx;
        float ry;
        float rz;
        float r;
        float g;
        float b;
        float a;
        boolean quads;

        UI3DScene.Box3D set(UI3DScene.Box3D rhs) {
            return this.set(
                rhs.x, rhs.y, rhs.z, rhs.xMin, rhs.yMin, rhs.zMin, rhs.xMax, rhs.yMax, rhs.zMax, rhs.rx, rhs.ry, rhs.rz, rhs.r, rhs.g, rhs.b, rhs.a, rhs.quads
            );
        }

        UI3DScene.Box3D set(
            float x,
            float y,
            float z,
            float xMin,
            float yMin,
            float zMin,
            float xMax,
            float yMax,
            float zMax,
            float rx,
            float ry,
            float rz,
            float r,
            float g,
            float b,
            float a,
            boolean bQuads
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xMin = xMin;
            this.yMin = yMin;
            this.zMin = zMin;
            this.xMax = xMax;
            this.yMax = yMax;
            this.zMax = zMax;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.quads = bQuads;
            return this;
        }
    }

    private static final class CharacterDrawer extends TextureDraw.GenericDrawer {
        UI3DScene.SceneCharacter character;
        UI3DScene.CharacterRenderData renderData;
        boolean rendered;

        public void init(UI3DScene.SceneCharacter character, UI3DScene.CharacterRenderData renderData) {
            this.character = character;
            this.renderData = renderData;
            this.rendered = false;
            this.character.animatedModel.renderMain();
        }

        @Override
        public void render() {
            if (this.character.clearDepthBuffer) {
                GL11.glClear(256);
            }

            boolean showBones = DebugOptions.instance.model.render.bones.getValue();
            DebugOptions.instance.model.render.bones.setValue(this.character.showBones);
            this.character.scene.characterSceneModelCamera.renderData = this.renderData;
            this.character.animatedModel.setShowBip01(this.character.showBip01);
            this.character.animatedModel.DoRender(this.character.scene.characterSceneModelCamera);
            DebugOptions.instance.model.render.bones.setValue(showBones);
            this.rendered = true;
            GL11.glDepthMask(true);
        }

        @Override
        public void postRender() {
            this.character.animatedModel.postRender(this.rendered);
        }
    }

    private static class CharacterRenderData extends UI3DScene.SceneObjectRenderData {
        final UI3DScene.CharacterDrawer drawer = new UI3DScene.CharacterDrawer();
        private static final ObjectPool<UI3DScene.CharacterRenderData> s_pool = new ObjectPool<>(UI3DScene.CharacterRenderData::new);

        UI3DScene.SceneObjectRenderData initCharacter(UI3DScene.SceneCharacter sceneObject) {
            this.drawer.init(sceneObject, this);
            this.init(sceneObject);
            return this;
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private final class CharacterSceneModelCamera extends UI3DScene.SceneModelCamera {
        private CharacterSceneModelCamera() {
            Objects.requireNonNull(UI3DScene.this);
            super();
        }

        @Override
        public void Begin() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
            GL11.glViewport(
                UI3DScene.this.getAbsoluteX().intValue(),
                Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(),
                UI3DScene.this.getWidth().intValue(),
                UI3DScene.this.getHeight().intValue()
            );
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    public static final class Circle {
        public final Vector3f center = new Vector3f();
        public final Vector3f orientation = new Vector3f();
        public float radius = 1.0F;
    }

    private abstract class Gizmo {
        static final float LENGTH = 0.5F;
        static final float THICKNESS = 0.05F;
        boolean visible;

        private Gizmo() {
            Objects.requireNonNull(UI3DScene.this);
            super();
        }

        abstract UI3DScene.Axis hitTest(float arg0, float arg1);

        abstract void startTracking(float arg0, float arg1, UI3DScene.Axis arg2);

        abstract void updateTracking(float arg0, float arg1);

        abstract void stopTracking();

        abstract void render();

        Vector3f getPointOnAxis(float uiX, float uiY, UI3DScene.Axis axis1, Matrix4f gizmoXfrm, Vector3f out) {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
            if (axis1 != UI3DScene.Axis.XY && axis1 != UI3DScene.Axis.XZ && axis1 != UI3DScene.Axis.YZ) {
                uiY = UI3DScene.this.screenHeight() - uiY;
                UI3DScene.Ray camera_ray = UI3DScene.this.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                UI3DScene.Ray axis = UI3DScene.allocRay();
                gizmoXfrm.transformPosition(axis.origin.set(0.0F, 0.0F, 0.0F));
                switch (axis1) {
                    case X:
                        axis.direction.set(1.0F, 0.0F, 0.0F);
                        break;
                    case Y:
                        axis.direction.set(0.0F, 1.0F, 0.0F);
                        break;
                    case Z:
                        axis.direction.set(0.0F, 0.0F, 1.0F);
                }

                gizmoXfrm.transformDirection(axis.direction).normalize();
                UI3DScene.closest_distance_between_lines(axis, camera_ray);
                UI3DScene.releaseRay(camera_ray);
                out.set(axis.direction).mul(axis.t).add(axis.origin);
                UI3DScene.releaseRay(axis);
                return out;
            } else {
                Vector3f planePoint = gizmoXfrm.transformPosition(UI3DScene.allocVector3f().set(0.0F, 0.0F, 0.0F));
                Vector3f planeRotate = UI3DScene.allocVector3f();
                UI3DScene.GridPlane gridPlane = UI3DScene.GridPlane.XY;
                switch (axis1) {
                    case XY:
                        planeRotate.set(0.0F, 0.0F, 0.0F);
                        gridPlane = UI3DScene.GridPlane.XY;
                        break;
                    case XZ:
                        planeRotate.set(90.0F, 0.0F, 0.0F);
                        gridPlane = UI3DScene.GridPlane.XZ;
                        break;
                    case YZ:
                        planeRotate.set(0.0F, 90.0F, 0.0F);
                        gridPlane = UI3DScene.GridPlane.YZ;
                }

                UI3DScene.this.polygonEditor.setPlane(planePoint, planeRotate, gridPlane);
                UI3DScene.this.polygonEditor.uiToPlane3D(uiX, uiY, out.set(0.0F));
                UI3DScene.releaseVector3f(planePoint);
                UI3DScene.releaseVector3f(planeRotate);
                return out;
            }
        }

        boolean hitTestRect(float uiX, float uiY, float x0, float y0, float z0, float x1, float y1, float z1) {
            float xx0 = UI3DScene.this.sceneToUIX(x0, y0, z0);
            float yy0 = UI3DScene.this.sceneToUIY(x0, y0, z0);
            float xx1 = UI3DScene.this.sceneToUIX(x1, y1, z1);
            float yy1 = UI3DScene.this.sceneToUIY(x1, y1, z1);
            float dx = 0.025F * UI3DScene.this.zoomMult();
            float dy = 0.025F * UI3DScene.this.zoomMult();
            float xmin = Math.min(xx0 - dx, xx1 - dx);
            float xmax = Math.max(xx0 + dx, xx1 + dx);
            float ymin = Math.min(yy0 - dy, yy1 - dy);
            float ymax = Math.max(yy0 + dy, yy1 + dy);
            return uiX >= xmin && uiY >= ymin && uiX < xmax && uiY < ymax;
        }

        boolean getPointOnDualAxis(float uiX, float uiY, UI3DScene.Axis axis, Matrix4f gizmoXfrm, Vector3f pointOnPlane3D, Vector2f pointOnPlane2D) {
            UI3DScene.Plane plane = UI3DScene.allocPlane();
            gizmoXfrm.transformPosition(plane.point.set(0.0F, 0.0F, 0.0F));
            switch (axis) {
                case XY:
                    plane.normal.set(0.0F, 0.0F, 1.0F);
                    break;
                case XZ:
                    plane.normal.set(0.0F, 1.0F, 0.0F);
                    break;
                case YZ:
                    plane.normal.set(1.0F, 0.0F, 0.0F);
            }

            gizmoXfrm.transformDirection(plane.normal);
            UI3DScene.Ray cameraRay = UI3DScene.this.getCameraRay(uiX, UI3DScene.this.screenHeight() - uiY, UI3DScene.allocRay());
            boolean hit = UI3DScene.intersect_ray_plane(plane, cameraRay, pointOnPlane3D) == 1;
            UI3DScene.releaseRay(cameraRay);
            UI3DScene.releasePlane(plane);
            if (hit) {
                Matrix4f m = UI3DScene.allocMatrix4f().set(gizmoXfrm);
                m.invert();
                Vector3f localToPlaneOrigin = m.transformPosition(pointOnPlane3D, UI3DScene.allocVector3f());
                UI3DScene.releaseMatrix4f(m);
                switch (axis) {
                    case XY:
                        pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.y);
                        break;
                    case XZ:
                        pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.z);
                        break;
                    case YZ:
                        pointOnPlane2D.set(localToPlaneOrigin.y, localToPlaneOrigin.z);
                }

                UI3DScene.releaseVector3f(localToPlaneOrigin);
                return true;
            } else {
                return false;
            }
        }

        void renderLineToOrigin() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
            if (stateData.hasGizmoOrigin) {
                UI3DScene.this.renderAxis(stateData.gizmoTranslate, stateData.gizmoRotate, false);
                Vector3f gizmoPos = stateData.gizmoTranslate;
                UI3DScene.vboRenderer.flush();
                Matrix4f matrix4f = UI3DScene.allocMatrix4f();
                matrix4f.set(stateData.modelView);
                matrix4f.mul(stateData.gizmoParentTransform);
                matrix4f.mul(stateData.gizmoOriginTransform);
                matrix4f.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                UI3DScene.vboRenderer.cmdPushAndLoadMatrix(5888, matrix4f);
                UI3DScene.releaseMatrix4f(matrix4f);
                UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
                UI3DScene.vboRenderer.setMode(1);
                UI3DScene.vboRenderer.setLineWidth(2.0F);
                UI3DScene.vboRenderer.addLine(gizmoPos.x, gizmoPos.y, gizmoPos.z, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                UI3DScene.vboRenderer.endRun();
                UI3DScene.vboRenderer.cmdPopMatrix(5888);
                UI3DScene.vboRenderer.flush();
            }
        }
    }

    public static enum GridPlane {
        XY,
        XZ,
        YZ;
    }

    private final class GridPlaneDrawer extends TextureDraw.GenericDrawer {
        final UI3DScene scene;

        GridPlaneDrawer(final UI3DScene scene) {
            Objects.requireNonNull(UI3DScene.this);
            super();
            this.scene = scene;
        }

        @Override
        public void render() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            GL11.glPushAttrib(2048);
            GL11.glViewport(
                UI3DScene.this.getAbsoluteX().intValue(),
                Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(),
                UI3DScene.this.getWidth().intValue(),
                UI3DScene.this.getHeight().intValue()
            );
            float HALF_GRID = 5.0F;
            UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            UI3DScene.vboRenderer.setMode(4);
            UI3DScene.vboRenderer.setDepthTest(true);
            if (this.scene.gridPlane == UI3DScene.GridPlane.XZ) {
                UI3DScene.vboRenderer.addTriangle(-5.0F, 0.0F, -5.0F, 5.0F, 0.0F, -5.0F, -5.0F, 0.0F, 5.0F, 0.5F, 0.5F, 0.5F, 1.0F);
                UI3DScene.vboRenderer.addTriangle(5.0F, 0.0F, 5.0F, -5.0F, 0.0F, 5.0F, 5.0F, 0.0F, -5.0F, 0.5F, 0.5F, 0.5F, 1.0F);
            }

            UI3DScene.vboRenderer.endRun();
            UI3DScene.vboRenderer.flush();
            GL11.glPopAttrib();
            ShaderHelper.glUseProgramObjectARB(0);
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    private static final class ModelDrawer extends TextureDraw.GenericDrawer {
        UI3DScene.SceneModel model;
        UI3DScene.ModelRenderData renderData;
        boolean rendered;
        FloatBuffer matrixPalette;
        TFloatArrayList boneCoords;
        Texture texture;

        public void init(UI3DScene.SceneModel model, UI3DScene.ModelRenderData renderData) {
            this.model = model;
            this.renderData = renderData;
            this.rendered = false;
            this.matrixPalette = null;
            this.boneCoords = null;
            this.texture = model.texture;
            if (!model.modelScript.isStatic && model.spriteModel != null && model.spriteModel.getAnimationName() != null) {
                this.matrixPalette = IsoObjectAnimations.getInstance()
                    .getMatrixPaletteForFrame(model.model, model.spriteModel.getAnimationName(), model.spriteModel.getAnimationTime());
                if (this.matrixPalette != null) {
                    this.matrixPalette.position(0);
                }

                this.boneCoords = IsoObjectAnimations.getInstance()
                    .getBonesForFrame(model.model, model.spriteModel.getAnimationName(), model.spriteModel.getAnimationTime());
            }

            if (!model.modelScript.isStatic && model.spriteModel == null) {
                SpriteModel spriteModel = ScriptManager.instance.getSpriteModel(model.modelScript.name);
                if (spriteModel != null && spriteModel.getAnimationName() != null) {
                    this.matrixPalette = IsoObjectAnimations.getInstance()
                        .getMatrixPaletteForFrame(model.model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
                    if (this.matrixPalette != null) {
                        this.matrixPalette.position(0);
                    }

                    this.boneCoords = IsoObjectAnimations.getInstance()
                        .getBonesForFrame(model.model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
                }
            }
        }

        @Override
        public void render() {
            UI3DScene.StateData stateData = this.model.scene.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            Model model = this.model.model;
            Shader Effect = model.effect;
            if (Effect != null && model.mesh != null && model.mesh.isReady()) {
                GL11.glPushAttrib(1048575);
                GL11.glPushClientAttrib(-1);
                UI3DScene scene = this.renderData.object.scene;
                GL11.glViewport(
                    scene.getAbsoluteX().intValue(),
                    Core.getInstance().getScreenHeight() - scene.getAbsoluteY().intValue() - scene.getHeight().intValue(),
                    scene.getWidth().intValue(),
                    scene.getHeight().intValue()
                );
                GL11.glDepthFunc(513);
                GL11.glDepthMask(true);
                GL11.glDepthRange(0.0, 1.0);
                GL11.glEnable(2929);
                ModelScript modelScript = this.model.modelScript;
                if (modelScript == null || modelScript.cullFace == -1) {
                    GL11.glDisable(2884);
                } else if (modelScript.cullFace == 0) {
                    GL11.glDisable(2884);
                } else {
                    GL11.glEnable(2884);
                    GL11.glCullFace(modelScript.cullFace);
                }

                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                Effect.Start();
                Texture tex = model.tex;
                if (this.texture != null) {
                    tex = this.texture;
                }

                if (tex != null) {
                    Effect.setTexture(tex, "Texture", 0);
                    if (Effect.getShaderProgram().getName().equalsIgnoreCase("door")) {
                        int widthHW = tex.getWidthHW();
                        int heightHW = tex.getHeightHW();
                        float x1 = tex.xStart * widthHW - tex.offsetX;
                        float y1 = tex.yStart * heightHW - tex.offsetY;
                        float x2 = x1 + tex.getWidthOrig();
                        float y2 = y1 + tex.getHeightOrig();
                        Vector2 tempVector2 = BaseVehicle.allocVector2();
                        Effect.getShaderProgram().setValue("UVOffset", tempVector2.set(x1 / widthHW, y1 / heightHW));
                        Effect.getShaderProgram().setValue("UVScale", tempVector2.set((x2 - x1) / widthHW, (y2 - y1) / heightHW));
                        BaseVehicle.releaseVector2(tempVector2);
                        GL11.glEnable(2884);
                        GL11.glCullFace(1028);
                    }
                }

                Effect.setDepthBias(0.0F);
                Effect.setAmbient(1.0F);
                Effect.setLightingAmount(1.0F);
                Effect.setHueShift(0.0F);
                Effect.setTint(1.0F, 1.0F, 1.0F);
                Effect.setAlpha(1.0F);

                for (int i = 0; i < 5; i++) {
                    Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                }

                if (model.isStatic) {
                    Effect.setTransformMatrix(this.renderData.transform, false);
                } else if ("door".equalsIgnoreCase(Effect.getShaderProgram().getName())) {
                    if (this.matrixPalette != null) {
                        Effect.setMatrixPalette(this.matrixPalette);
                    }

                    PZGLUtil.pushAndMultMatrix(5888, this.renderData.transform);
                    if (this.model.modelScript.meshName.contains("door1")) {
                    }
                }

                Effect.setTargetDepth(0.5F);
                model.mesh.Draw(Effect);
                Effect.End();
                if (DebugOptions.instance.model.render.bones.getValue()) {
                    this.renderSkeleton();
                }

                if (!model.isStatic && "door".equalsIgnoreCase(Effect.getShaderProgram().getName())) {
                    PZGLUtil.popMatrix(5888);
                }

                if (Core.debug) {
                }

                if (DebugOptions.instance.model.render.axis.getValue()) {
                }

                if (scene.drawAttachments) {
                    Matrix4f modelXfrm = UI3DScene.allocMatrix4f();
                    modelXfrm.set(this.renderData.transform);
                    modelXfrm.mul(model.mesh.transform);
                    modelXfrm.scale(this.model.modelScript.scale);
                    Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();

                    for (int i = 0; i < this.model.modelScript.getAttachmentCount(); i++) {
                        ModelAttachment attachment = this.model.modelScript.getAttachment(i);
                        ModelInstanceRenderData.makeAttachmentTransform(attachment, attachmentXfrm);
                        modelXfrm.mul(attachmentXfrm, attachmentXfrm);
                        PZGLUtil.pushAndMultMatrix(5888, attachmentXfrm);
                        Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 1.0F);
                        PZGLUtil.popMatrix(5888);
                    }

                    UI3DScene.releaseMatrix4f(modelXfrm);
                    UI3DScene.releaseMatrix4f(attachmentXfrm);
                }

                GL11.glPopAttrib();
                GL11.glPopClientAttrib();
                Texture.lastTextureID = -1;
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                SpriteRenderer.ringBuffer.restoreVbos = true;
                GL20.glUseProgram(0);
                ShaderHelper.forgetCurrentlyBound();
            }

            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            this.rendered = true;
        }

        @Override
        public void postRender() {
        }

        private void renderSkeleton() {
            if (this.boneCoords != null) {
                if (!this.boneCoords.isEmpty()) {
                    VBORenderer vbor = VBORenderer.getInstance();
                    vbor.flush();
                    vbor.startRun(vbor.formatPositionColor);
                    vbor.setDepthTest(false);
                    vbor.setLineWidth(1.0F);
                    vbor.setMode(1);

                    for (int i = 0; i < this.boneCoords.size(); i += 6) {
                        Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                        float x1 = this.boneCoords.get(i) / this.model.modelScript.scale;
                        float y1 = this.boneCoords.get(i + 1) / this.model.modelScript.scale;
                        float z1 = this.boneCoords.get(i + 2) / this.model.modelScript.scale;
                        float x2 = this.boneCoords.get(i + 3) / this.model.modelScript.scale;
                        float y2 = this.boneCoords.get(i + 4) / this.model.modelScript.scale;
                        float z2 = this.boneCoords.get(i + 5) / this.model.modelScript.scale;
                        vbor.addLine(x1, y1, z1, x2, y2, z2, c.r, c.g, c.b, 1.0F);
                    }

                    vbor.endRun();
                    vbor.flush();
                    GL11.glColor3f(1.0F, 1.0F, 1.0F);
                    GL11.glEnable(2929);
                }
            }
        }
    }

    private static class ModelRenderData extends UI3DScene.SceneObjectRenderData {
        final UI3DScene.ModelDrawer drawer = new UI3DScene.ModelDrawer();
        private static final ObjectPool<UI3DScene.ModelRenderData> s_pool = new ObjectPool<>(UI3DScene.ModelRenderData::new);

        UI3DScene.SceneObjectRenderData initModel(UI3DScene.SceneModel sceneObject) {
            this.init(sceneObject);
            if (sceneObject.useWorldAttachment) {
                if (sceneObject.weaponRotationHack) {
                    this.transform.rotateXYZ(0.0F, (float) Math.PI, (float) (Math.PI / 2));
                }

                if (sceneObject.modelScript != null) {
                    ModelAttachment attachment = sceneObject.modelScript.getAttachmentById("world");
                    if (attachment != null) {
                        Matrix4f m = ModelInstanceRenderData.makeAttachmentTransform(attachment, UI3DScene.allocMatrix4f());
                        m.invert();
                        this.transform.mul(m);
                        UI3DScene.releaseMatrix4f(m);
                    }
                }
            }

            if (sceneObject.ignoreVehicleScale && sceneObject.parentVehiclePart != null && sceneObject.parentVehiclePart.vehicle.script != null) {
                this.transform.scale(1.5F / sceneObject.parentVehiclePart.vehicle.script.getModelScale());
            }

            ModelInstanceRenderData.postMultiplyMeshTransform(this.transform, sceneObject.model.mesh);
            if (sceneObject.modelScript != null && sceneObject.modelScript.scale != 1.0F) {
                this.transform.scale(sceneObject.modelScript.scale);
            }

            this.drawer.init(sceneObject, this);
            return this;
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private static final class OriginAttachment extends UI3DScene.SceneObject {
        UI3DScene.SceneObject object;
        String attachmentName;

        OriginAttachment(UI3DScene scene) {
            super(scene, "OriginAttachment");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        Matrix4f getGlobalTransform(Matrix4f transform) {
            return this.object.getAttachmentTransform(this.attachmentName, transform);
        }
    }

    private static final class OriginBone extends UI3DScene.SceneObject {
        UI3DScene.SceneCharacter character;
        UI3DScene.SceneModel sceneModel;
        String boneName;

        OriginBone(UI3DScene scene) {
            super(scene, "OriginBone");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        Matrix4f getGlobalTransform(Matrix4f transform) {
            return this.sceneModel != null ? this.sceneModel.getBoneMatrix(this.boneName, transform) : this.character.getBoneMatrix(this.boneName, transform);
        }
    }

    private static final class OriginGeometry extends UI3DScene.SceneObject {
        UI3DScene.SceneGeometry sceneGeometry;
        String originHint;

        OriginGeometry(UI3DScene scene) {
            super(scene, "OriginGeometry");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        Matrix4f getGlobalTransform(Matrix4f transform) {
            return this.sceneGeometry.getOriginTransform(this.originHint, transform);
        }
    }

    private static final class OriginGizmo extends UI3DScene.SceneObject {
        OriginGizmo(UI3DScene scene) {
            super(scene, "OriginGizmo");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            return null;
        }
    }

    private static final class OriginVehiclePart extends UI3DScene.SceneObject {
        UI3DScene.SceneVehicle vehicle;
        String partId;
        String partModelId;
        String attachmentName;
        boolean boneOnly = true;

        OriginVehiclePart(UI3DScene scene) {
            super(scene, "OriginVehiclePart");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            return null;
        }

        @Override
        Matrix4f getGlobalTransform(Matrix4f transform) {
            this.vehicle.getTransformForPart(this.partId, this.partModelId, this.attachmentName, this.boneOnly, transform);
            UI3DScene.VehicleRenderData vrd = UI3DScene.VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this.vehicle);
            UI3DScene.VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
            if (parentVMRD != null) {
                parentVMRD.xfrm.mul(transform, transform);
            }

            vrd.release();
            return transform;
        }

        Matrix4f getGlobalBoneTransform(Matrix4f transform) {
            transform.identity();
            UI3DScene.SceneVehicleModelInfo modelInfo = this.vehicle.getModelInfoForPart(this.partId);
            if (modelInfo == null) {
                return transform;
            } else {
                AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
                if (animationPlayer == null) {
                    return transform;
                } else {
                    SkinningBone bone = animationPlayer.getSkinningData().getBone(this.attachmentName);
                    if (bone == null) {
                        return transform;
                    } else {
                        transform = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), transform);
                        transform.transpose();
                        UI3DScene.VehicleRenderData vrd = UI3DScene.VehicleRenderData.s_pool.alloc();
                        vrd.initVehicle(this.vehicle);
                        UI3DScene.VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
                        if (parentVMRD != null) {
                            parentVMRD.xfrm.mul(transform, transform);
                        }

                        vrd.release();
                        return transform;
                    }
                }
            }
        }
    }

    private final class OverlaysDrawer extends TextureDraw.GenericDrawer {
        private OverlaysDrawer() {
            Objects.requireNonNull(UI3DScene.this);
            super();
        }

        void init() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
            UI3DScene.s_aabbPool.release(stateData.aabb);
            stateData.aabb.clear();

            for (int i = 0; i < UI3DScene.this.aabb.size(); i++) {
                UI3DScene.AABB aabb = UI3DScene.this.aabb.get(i);
                stateData.aabb.add(UI3DScene.s_aabbPool.alloc().set(aabb));
            }

            UI3DScene.s_box3DPool.release(stateData.box3d);
            stateData.box3d.clear();

            for (int i = 0; i < UI3DScene.this.box3d.size(); i++) {
                UI3DScene.Box3D box3D = UI3DScene.this.box3d.get(i);
                stateData.box3d.add(UI3DScene.s_box3DPool.alloc().set(box3D));
            }

            UI3DScene.s_physicsMeshPool.releaseAll(stateData.physicsMesh);
            stateData.physicsMesh.clear();

            for (int i = 0; i < UI3DScene.this.physicsMesh.size(); i++) {
                UI3DScene.PhysicsMesh physicsMesh = UI3DScene.this.physicsMesh.get(i);
                if (physicsMesh.physicsShapeScript != null) {
                    PhysicsShapeScript physicsShapeScript = ScriptManager.instance.getPhysicsShape(physicsMesh.physicsShapeScript);
                    if (physicsShapeScript != null) {
                        PhysicsShape asset = (PhysicsShape)PhysicsShapeAssetManager.instance.getAssetTable().get(physicsShapeScript.meshName);
                        if (asset != null && asset.isReady()) {
                            for (int j = 0; j < asset.meshes.size(); j++) {
                                PhysicsShape.OneMesh oneMesh = asset.meshes.get(j);
                                UI3DScene.PhysicsMesh physicsMesh1 = UI3DScene.s_physicsMeshPool.alloc().set(physicsMesh);
                                Matrix4f xfrm = Bullet.translationRotateScale(
                                    physicsShapeScript.translate,
                                    physicsShapeScript.rotate,
                                    physicsShapeScript.scale * physicsMesh.scale,
                                    UI3DScene.allocMatrix4f()
                                );
                                oneMesh.transform.transpose();
                                xfrm.mul(oneMesh.transform);
                                oneMesh.transform.transpose();
                                physicsMesh1.points = Bullet.transformPhysicsMeshPoints(xfrm, oneMesh.points, false);
                                UI3DScene.releaseMatrix4f(xfrm);
                                stateData.physicsMesh.add(physicsMesh1);
                            }
                        }
                    }
                }
            }

            UI3DScene.s_posRotPool.release(stateData.axes);
            stateData.axes.clear();

            for (int ix = 0; ix < UI3DScene.this.axes.size(); ix++) {
                UI3DScene.PositionRotation axis = UI3DScene.this.axes.get(ix);
                stateData.axes.add(UI3DScene.s_posRotPool.alloc().set(axis));
            }
        }

        @Override
        public void render() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
            DefaultShader.isActive = false;
            ShaderHelper.forgetCurrentlyBound();
            GL20.glUseProgram(0);
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            GL11.glPushAttrib(2048);
            GL11.glViewport(
                UI3DScene.this.getAbsoluteX().intValue(),
                Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(),
                UI3DScene.this.getWidth().intValue(),
                UI3DScene.this.getHeight().intValue()
            );
            UI3DScene.vboRenderer.setOffset(0.0F, 0.0F, 0.0F);
            if (UI3DScene.this.drawGrid) {
                UI3DScene.this.renderGrid();
            }

            for (int i = 0; i < stateData.aabb.size(); i++) {
                UI3DScene.AABB aabb = stateData.aabb.get(i);
                UI3DScene.this.renderAABB(
                    aabb.x,
                    aabb.y,
                    aabb.z,
                    -aabb.w / 2.0F,
                    -aabb.h / 2.0F,
                    -aabb.l / 2.0F,
                    aabb.w / 2.0F,
                    aabb.h / 2.0F,
                    aabb.l / 2.0F,
                    aabb.r,
                    aabb.g,
                    aabb.b,
                    1.0F,
                    false
                );
            }

            for (int i = 0; i < stateData.box3d.size(); i++) {
                UI3DScene.Box3D b = stateData.box3d.get(i);
                UI3DScene.this.renderBox3D(b.x, b.y, b.z, b.xMin, b.yMin, b.zMin, b.xMax, b.yMax, b.zMax, b.rx, b.ry, b.rz, b.r, b.g, b.b, b.a, b.quads);
            }

            for (int i = 0; i < stateData.physicsMesh.size(); i++) {
                UI3DScene.PhysicsMesh m = stateData.physicsMesh.get(i);
                UI3DScene.this.renderPhysicsMesh(m.x, m.y, m.z, m.rx, m.ry, m.rz, m.r, m.g, m.b, m.points);
            }

            for (int i = 0; i < stateData.axes.size(); i++) {
                UI3DScene.this.renderAxis(stateData.axes.get(i));
            }

            UI3DScene.vboRenderer.flush();
            if (stateData.gizmo != null) {
                GL11.glDisable(3553);
                stateData.gizmo.render();
                GL11.glEnable(3553);
            }

            UI3DScene.vboRenderer.flush();
            GL11.glPopAttrib();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }
    }

    private static final class ParentVehiclePart {
        UI3DScene.SceneVehicle vehicle;
        String partId;
        String partModelId;
        String attachmentName;

        Matrix4f getGlobalTransform(Matrix4f transform) {
            this.vehicle.getTransformForPart(this.partId, this.partModelId, this.attachmentName, false, transform);
            UI3DScene.VehicleRenderData vrd = UI3DScene.VehicleRenderData.s_pool.alloc();
            vrd.initVehicle(this.vehicle);
            UI3DScene.VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(this.partId);
            if (parentVMRD != null) {
                parentVMRD.xfrm.mul(transform, transform);
            }

            vrd.release();
            return transform;
        }

        VehicleScript.Part getScriptPart() {
            return this.vehicle != null && this.vehicle.script != null && this.partId != null ? this.vehicle.script.getPartById(this.partId) : null;
        }

        VehicleScript.Model getScriptModel() {
            VehicleScript.Part scriptPart = this.getScriptPart();
            return scriptPart != null && this.partModelId != null ? scriptPart.getModelById(this.partModelId) : null;
        }
    }

    public static final class PhysicsMesh {
        float x;
        float y;
        float z;
        float rx;
        float ry;
        float rz;
        float r;
        float g;
        float b;
        String physicsShapeScript;
        float scale;
        float[] points;

        UI3DScene.PhysicsMesh set(Vector3f translate, Vector3f rotate, float scale, String physicsShapeScript, float r, float g, float b) {
            this.x = translate.x;
            this.y = translate.y;
            this.z = translate.z;
            this.rx = rotate.x;
            this.ry = rotate.y;
            this.rz = rotate.z;
            this.scale = scale;
            this.physicsShapeScript = physicsShapeScript;
            this.r = r;
            this.g = g;
            this.b = b;
            this.points = null;
            return this;
        }

        UI3DScene.PhysicsMesh set(UI3DScene.PhysicsMesh rhs) {
            this.x = rhs.x;
            this.y = rhs.y;
            this.z = rhs.z;
            this.rx = rhs.rx;
            this.ry = rhs.ry;
            this.rz = rhs.rz;
            this.scale = rhs.scale;
            this.physicsShapeScript = rhs.physicsShapeScript;
            this.r = rhs.r;
            this.g = rhs.g;
            this.b = rhs.b;
            this.points = rhs.points;
            return this;
        }
    }

    public static final class Plane {
        public final Vector3f point = new Vector3f();
        public final Vector3f normal = new Vector3f();

        public Plane() {
        }

        public Plane(Vector3f normal, Vector3f point) {
            this.point.set(point);
            this.normal.set(normal);
        }

        public UI3DScene.Plane set(Vector3f normal, Vector3f point) {
            this.point.set(point);
            this.normal.set(normal);
            return this;
        }
    }

    public static final class PlaneObjectPool extends ObjectPool<UI3DScene.Plane> {
        int allocated;

        public PlaneObjectPool() {
            super(UI3DScene.Plane::new);
        }

        protected UI3DScene.Plane makeObject() {
            this.allocated++;
            return (UI3DScene.Plane)super.makeObject();
        }
    }

    public static final class PolygonEditor {
        final UI3DScene scene;
        final UI3DScene.Plane plane = new UI3DScene.Plane();
        final Vector3f rotate = new Vector3f();
        UI3DScene.GridPlane gridPlane = UI3DScene.GridPlane.XY;

        PolygonEditor(UI3DScene scene) {
            this.scene = scene;
        }

        void setPlane(Vector3f translate, Vector3f rotate, UI3DScene.GridPlane gridPlane) {
            this.plane.point.set(translate);
            this.plane.normal.set(0.0F, 0.0F, 1.0F);
            Matrix4f m = UI3DScene.allocMatrix4f()
                .rotationXYZ(rotate.x * (float) (Math.PI / 180.0), rotate.y * (float) (Math.PI / 180.0), rotate.z * (float) (Math.PI / 180.0));
            m.transformDirection(this.plane.normal);
            UI3DScene.releaseMatrix4f(m);
            this.rotate.set(rotate);
            this.gridPlane = gridPlane;
        }

        boolean uiToPlane3D(float uiX, float uiY, Vector3f result) {
            boolean hit = false;
            UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, this.scene.screenHeight() - uiY, UI3DScene.allocRay());
            if (UI3DScene.intersect_ray_plane(this.plane, cameraRay, result) == 1) {
                hit = true;
            }

            UI3DScene.releaseRay(cameraRay);
            return hit;
        }

        boolean uiToPlane2D(float uiX, float uiY, Vector2f result) {
            Vector3f pointOnPlane = UI3DScene.allocVector3f();
            boolean hit = this.uiToPlane3D(uiX, uiY, pointOnPlane);
            if (hit) {
                Matrix4f m = UI3DScene.allocMatrix4f();
                m.translation(this.plane.point);
                m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
                m.invert();
                m.transformPosition(pointOnPlane);
                result.set(pointOnPlane.x, pointOnPlane.y);
                UI3DScene.releaseMatrix4f(m);
            }

            UI3DScene.releaseVector3f(pointOnPlane);
            return hit;
        }

        Vector3f planeTo3D(Vector2f pointOnPlane, Vector3f result) {
            Matrix4f m = UI3DScene.allocMatrix4f();
            m.translation(this.plane.point);
            m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            m.transformPosition(pointOnPlane.x, pointOnPlane.y, 0.0F, result);
            UI3DScene.releaseMatrix4f(m);
            return result;
        }

        Vector2f planeToUI(Vector2f pointOnPlane, Vector2f result) {
            Vector3f scenePos = this.planeTo3D(pointOnPlane, UI3DScene.allocVector3f());
            result.set(this.scene.sceneToUIX(scenePos), this.scene.sceneToUIY(scenePos));
            UI3DScene.releaseVector3f(scenePos);
            return result;
        }
    }

    private static final class PositionRotation {
        final Vector3f pos = new Vector3f();
        final Vector3f rot = new Vector3f();
        boolean relativeToOrigin;

        UI3DScene.PositionRotation set(UI3DScene.PositionRotation rhs) {
            this.pos.set(rhs.pos);
            this.rot.set(rhs.rot);
            this.relativeToOrigin = rhs.relativeToOrigin;
            return this;
        }

        UI3DScene.PositionRotation set(float x, float y, float z) {
            this.pos.set(x, y, z);
            this.rot.set(0.0F, 0.0F, 0.0F);
            this.relativeToOrigin = false;
            return this;
        }

        UI3DScene.PositionRotation set(float x, float y, float z, float rx, float ry, float rz) {
            this.pos.set(x, y, z);
            this.rot.set(rx, ry, rz);
            this.relativeToOrigin = false;
            return this;
        }
    }

    public static final class Ray {
        public final Vector3f origin = new Vector3f();
        public final Vector3f direction = new Vector3f();
        public float t;

        public UI3DScene.Ray set(UI3DScene.Ray rhs) {
            this.origin.set(rhs.origin);
            this.direction.set(rhs.direction);
            this.t = rhs.t;
            return this;
        }
    }

    public static final class RayObjectPool extends ObjectPool<UI3DScene.Ray> {
        int allocated;

        public RayObjectPool() {
            super(UI3DScene.Ray::new);
        }

        protected UI3DScene.Ray makeObject() {
            this.allocated++;
            return (UI3DScene.Ray)super.makeObject();
        }
    }

    private final class RotateGizmo extends UI3DScene.Gizmo {
        UI3DScene.Axis trackAxis;
        boolean snap;
        final UI3DScene.Circle trackCircle;
        final Matrix4f startXfrm;
        final Matrix4f startInvXfrm;
        final Vector3f startPointOnCircle;
        final Vector3f currentPointOnCircle;
        final ArrayList<Vector3f> circlePointsMain;
        final ArrayList<Vector3f> circlePointsRender;

        private RotateGizmo() {
            Objects.requireNonNull(UI3DScene.this);
            super();
            this.trackAxis = UI3DScene.Axis.None;
            this.snap = true;
            this.trackCircle = new UI3DScene.Circle();
            this.startXfrm = new Matrix4f();
            this.startInvXfrm = new Matrix4f();
            this.startPointOnCircle = new Vector3f();
            this.currentPointOnCircle = new Vector3f();
            this.circlePointsMain = new ArrayList<>();
            this.circlePointsRender = new ArrayList<>();
        }

        @Override
        UI3DScene.Axis hitTest(float uiX, float uiY) {
            if (!this.visible) {
                return UI3DScene.Axis.None;
            } else {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
                UI3DScene.this.setModelViewProjection(stateData);
                UI3DScene.this.setGizmoTransforms(stateData);
                uiY = UI3DScene.this.screenHeight() - uiY;
                UI3DScene.Ray camera_ray = UI3DScene.this.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
                gizmoXfrm.set(stateData.gizmoParentTransform);
                gizmoXfrm.mul(stateData.gizmoOriginTransform);
                gizmoXfrm.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                gizmoXfrm.mul(stateData.gizmoTransform);
                Vector3f scale = gizmoXfrm.getScale(UI3DScene.allocVector3f());
                gizmoXfrm.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
                UI3DScene.releaseVector3f(scale);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    gizmoXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float radius = 0.5F * SCALE;
                Vector3f center = gizmoXfrm.transformProject(UI3DScene.allocVector3f().set(0.0F, 0.0F, 0.0F));
                Vector3f xAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(1.0F, 0.0F, 0.0F)).normalize();
                Vector3f yAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(0.0F, 1.0F, 0.0F)).normalize();
                Vector3f zAxis = gizmoXfrm.transformDirection(UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F)).normalize();
                Vector2 point = UI3DScene.allocVector2();
                this.getCircleSegments(center, radius, yAxis, zAxis, this.circlePointsMain);
                float distX = this.hitTestCircle(camera_ray, this.circlePointsMain, point);
                BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
                this.circlePointsMain.clear();
                this.getCircleSegments(center, radius, xAxis, zAxis, this.circlePointsMain);
                float distY = this.hitTestCircle(camera_ray, this.circlePointsMain, point);
                BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
                this.circlePointsMain.clear();
                this.getCircleSegments(center, radius, xAxis, yAxis, this.circlePointsMain);
                float distZ = this.hitTestCircle(camera_ray, this.circlePointsMain, point);
                BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsMain);
                this.circlePointsMain.clear();
                UI3DScene.releaseVector2(point);
                UI3DScene.releaseVector3f(xAxis);
                UI3DScene.releaseVector3f(yAxis);
                UI3DScene.releaseVector3f(zAxis);
                UI3DScene.releaseVector3f(center);
                UI3DScene.releaseRay(camera_ray);
                UI3DScene.releaseMatrix4f(gizmoXfrm);
                float DIST = 8.0F;
                if (distX < distY && distX < distZ) {
                    return distX <= 8.0F ? UI3DScene.Axis.X : UI3DScene.Axis.None;
                } else if (distY < distX && distY < distZ) {
                    return distY <= 8.0F ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
                } else if (distZ < distX && distZ < distY) {
                    return distZ <= 8.0F ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
                } else {
                    return UI3DScene.Axis.None;
                }
            }
        }

        @Override
        void startTracking(float uiX, float uiY, UI3DScene.Axis axis) {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
            UI3DScene.this.setModelViewProjection(stateData);
            UI3DScene.this.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }

            if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            this.startInvXfrm.set(stateData.gizmoParentTransform);
            this.startInvXfrm.mul(stateData.gizmoOriginTransform);
            this.startInvXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startInvXfrm.mul(stateData.gizmoTransform);
            }

            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.trackCircle, this.startXfrm, this.startPointOnCircle);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.trackCircle, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPointOnCircle.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
            } else {
                this.currentPointOnCircle.set(pos);
                UI3DScene.releaseVector3f(pos);
                float angle = this.calculateRotation(this.startPointOnCircle, this.currentPointOnCircle, this.trackCircle);
                if (GameKeyboard.isKeyDown(29)) {
                    if (angle > 0.0F) {
                        angle = (int)(angle / 5.0F) * 5;
                    } else {
                        angle = Math.round(-angle / 5.0F) * -5;
                    }
                }

                switch (this.trackAxis) {
                    case X:
                        this.trackCircle.orientation.set(1.0F, 0.0F, 0.0F);
                        break;
                    case Y:
                        this.trackCircle.orientation.set(0.0F, 1.0F, 0.0F);
                        break;
                    case Z:
                        this.trackCircle.orientation.set(0.0F, 0.0F, 1.0F);
                }

                Vector3f orientation = UI3DScene.allocVector3f().set(this.trackCircle.orientation);
                UI3DScene.Ray cameraRay = UI3DScene.this.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                Vector3f orientation2 = this.startXfrm.transformDirection(UI3DScene.allocVector3f().set(orientation)).normalize();
                float dot = cameraRay.direction.dot(orientation2);
                UI3DScene.releaseVector3f(orientation2);
                UI3DScene.releaseRay(cameraRay);
                if (UI3DScene.this.gizmoParent instanceof UI3DScene.SceneCharacter) {
                    if (dot > 0.0F) {
                        angle *= -1.0F;
                    }
                } else if (UI3DScene.this.gizmoOrigin instanceof UI3DScene.OriginVehiclePart) {
                    if (dot > 0.0F) {
                        angle *= -1.0F;
                    }
                } else if (dot < 0.0F) {
                    angle *= -1.0F;
                }

                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    this.startInvXfrm.transformDirection(orientation);
                }

                Quaternionf rotation_quat = UI3DScene.allocQuaternionf().fromAxisAngleDeg(orientation, angle);
                UI3DScene.releaseVector3f(orientation);
                orientation2 = rotation_quat.getEulerAnglesXYZ(new Vector3f());
                UI3DScene.releaseQuaternionf(rotation_quat);
                orientation2.mul(180.0F / (float)Math.PI);
                if (this.snap) {
                }

                LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), UI3DScene.this.table, orientation2);
            }
        }

        @Override
        void stopTracking() {
            this.trackAxis = UI3DScene.Axis.None;
        }

        @Override
        void render() {
            if (this.visible) {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
                Matrix4f matrix4f = UI3DScene.allocMatrix4f();
                matrix4f.set(stateData.gizmoParentTransform);
                matrix4f.mul(stateData.gizmoOriginTransform);
                matrix4f.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                matrix4f.mul(stateData.gizmoTransform);
                Vector3f scale = matrix4f.getScale(UI3DScene.allocVector3f());
                matrix4f.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
                UI3DScene.releaseVector3f(scale);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    matrix4f.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                float mouseX = Mouse.getXA() - UI3DScene.this.getAbsoluteX().intValue();
                float mouseY = Mouse.getYA() - UI3DScene.this.getAbsoluteY().intValue();
                UI3DScene.Ray cameraRay = UI3DScene.this.getCameraRay(
                    mouseX, UI3DScene.this.screenHeight() - mouseY, stateData.projection, stateData.modelView, UI3DScene.allocRay()
                );
                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float radius = 0.5F * SCALE;
                GL11.glClear(256);
                GL11.glEnable(2929);
                GL11.glDepthFunc(513);
                Matrix4f axisMatrix4f = UI3DScene.allocMatrix4f();
                UI3DScene.Axis axis = this.trackAxis == UI3DScene.Axis.None ? stateData.gizmoAxis : this.trackAxis;
                if (this.trackAxis == UI3DScene.Axis.None || this.trackAxis == UI3DScene.Axis.X) {
                    float r = axis == UI3DScene.Axis.X ? 1.0F : 0.5F;
                    float g = 0.0F;
                    float b = 0.0F;
                    axisMatrix4f.set(matrix4f);
                    axisMatrix4f.rotateY((float) (Math.PI / 2));
                    this.renderAxis(axisMatrix4f, 0.01F * SCALE, radius / 2.0F, r, 0.0F, 0.0F, cameraRay);
                }

                if (this.trackAxis == UI3DScene.Axis.None || this.trackAxis == UI3DScene.Axis.Y) {
                    float r = 0.0F;
                    float g = axis == UI3DScene.Axis.Y ? 1.0F : 0.5F;
                    float b = 0.0F;
                    axisMatrix4f.set(matrix4f);
                    axisMatrix4f.rotateX((float) (Math.PI / 2));
                    this.renderAxis(axisMatrix4f, 0.01F * SCALE, radius / 2.0F, 0.0F, g, 0.0F, cameraRay);
                }

                if (this.trackAxis == UI3DScene.Axis.None || this.trackAxis == UI3DScene.Axis.Z) {
                    float r = 0.0F;
                    float g = 0.0F;
                    float b = axis == UI3DScene.Axis.Z ? 1.0F : 0.5F;
                    axisMatrix4f.set(matrix4f);
                    this.renderAxis(axisMatrix4f, 0.01F * SCALE, radius / 2.0F, 0.0F, 0.0F, b, cameraRay);
                }

                UI3DScene.releaseMatrix4f(axisMatrix4f);
                UI3DScene.releaseRay(cameraRay);
                UI3DScene.releaseMatrix4f(matrix4f);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                GL11.glDepthFunc(519);
                this.renderLineToOrigin();
                GLStateRenderThread.restore();
            }
        }

        void getCircleSegments(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, ArrayList<Vector3f> out) {
            Vector3f p1 = UI3DScene.allocVector3f();
            Vector3f p2 = UI3DScene.allocVector3f();
            int segments = 32;
            double t = 0.0;
            double cos = Math.cos(t);
            double sin = Math.sin(t);
            orthoNormal1.mul((float)cos, p1);
            orthoNormal2.mul((float)sin, p2);
            p1.add(p2).mul(radius);
            out.add(UI3DScene.allocVector3f().set(center).add(p1));

            for (int i = 1; i <= 32; i++) {
                t = i * 360.0 / 32.0 * (float) (Math.PI / 180.0);
                cos = Math.cos(t);
                sin = Math.sin(t);
                orthoNormal1.mul((float)cos, p1);
                orthoNormal2.mul((float)sin, p2);
                p1.add(p2).mul(radius);
                out.add(UI3DScene.allocVector3f().set(center).add(p1));
            }

            UI3DScene.releaseVector3f(p1);
            UI3DScene.releaseVector3f(p2);
        }

        private float hitTestCircle(UI3DScene.Ray cameraRay, ArrayList<Vector3f> circlePoints, Vector2 closestPoint) {
            UI3DScene.Ray ray = UI3DScene.allocRay();
            Vector3f v = UI3DScene.allocVector3f();
            float camX = UI3DScene.this.sceneToUIX(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
            float camY = UI3DScene.this.sceneToUIY(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z);
            float closestDist = Float.MAX_VALUE;
            Vector3f p1 = circlePoints.get(0);

            for (int i = 1; i < circlePoints.size(); i++) {
                Vector3f p2 = circlePoints.get(i);
                float x1 = UI3DScene.this.sceneToUIX(p1.x, p1.y, p1.z);
                float y1 = UI3DScene.this.sceneToUIY(p1.x, p1.y, p1.z);
                float x2 = UI3DScene.this.sceneToUIX(p2.x, p2.y, p2.z);
                float y2 = UI3DScene.this.sceneToUIY(p2.x, p2.y, p2.z);
                double lenSq = Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0);
                if (lenSq < 0.001) {
                    p1 = p2;
                } else {
                    double u = ((camX - x1) * (x2 - x1) + (camY - y1) * (y2 - y1)) / lenSq;
                    double xu = x1 + u * (x2 - x1);
                    double yu = y1 + u * (y2 - y1);
                    if (u <= 0.0) {
                        xu = x1;
                        yu = y1;
                    } else if (u >= 1.0) {
                        xu = x2;
                        yu = y2;
                    }

                    float dist = IsoUtils.DistanceTo2D(camX, camY, (float)xu, (float)yu);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closestPoint.set((float)xu, (float)yu);
                    }

                    p1 = p2;
                }
            }

            UI3DScene.releaseVector3f(v);
            UI3DScene.releaseRay(ray);
            return closestDist;
        }

        void renderAxis(Matrix4f axisMatrix4f, float r, float c, float r1, float g1, float b1, UI3DScene.Ray cameraRay) {
            UI3DScene.Ray cameraRay2 = UI3DScene.allocRay().set(cameraRay);
            axisMatrix4f.invert();
            axisMatrix4f.transformPosition(cameraRay2.origin);
            axisMatrix4f.transformDirection(cameraRay2.direction);
            axisMatrix4f.invert();
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.cmdPushAndMultMatrix(5888, axisMatrix4f);
            vbor.addTorus(r, c, 8, 32, r1, g1, b1, cameraRay2);
            vbor.cmdPopMatrix(5888);
            vbor.flush();
            UI3DScene.releaseRay(cameraRay2);
        }

        void renderAxis(Vector3f center, float radius, Vector3f orthoNormal1, Vector3f orthoNormal2, float r, float g, float b, UI3DScene.Ray cameraRay) {
            UI3DScene.vboRenderer.flush();
            UI3DScene.vboRenderer.setLineWidth(6.0F);
            this.getCircleSegments(center, radius, orthoNormal1, orthoNormal2, this.circlePointsRender);
            Vector3f spoke = UI3DScene.allocVector3f();
            Vector3f p0 = this.circlePointsRender.get(0);

            for (int i = 1; i < this.circlePointsRender.size(); i++) {
                Vector3f p1 = this.circlePointsRender.get(i);
                spoke.set(p1.x - center.x, p1.y - center.y, p1.z - center.z).normalize();
                float dot = spoke.dot(cameraRay.direction);
                if (dot < 0.1F) {
                    UI3DScene.vboRenderer.addLine(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, r, g, b, 1.0F);
                } else {
                    UI3DScene.vboRenderer.addLine(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, r / 2.0F, g / 2.0F, b / 2.0F, 0.25F);
                }

                p0 = p1;
            }

            BaseVehicle.TL_vector3f_pool.get().release(this.circlePointsRender);
            this.circlePointsRender.clear();
            UI3DScene.releaseVector3f(spoke);
            UI3DScene.vboRenderer.flush();
        }

        Vector3f getPointOnAxis(float uiX, float uiY, UI3DScene.Axis axis, UI3DScene.Circle circle, Matrix4f gizmoXfrm, Vector3f out) {
            float SCALE = 1.0F;
            circle.radius = 0.5F;
            gizmoXfrm.getTranslation(circle.center);
            float cx = UI3DScene.this.sceneToUIX(circle.center.x, circle.center.y, circle.center.z);
            float cy = UI3DScene.this.sceneToUIY(circle.center.x, circle.center.y, circle.center.z);
            circle.center.set(cx, cy, 0.0F);
            circle.orientation.set(0.0F, 0.0F, 1.0F);
            UI3DScene.Ray camera_ray = UI3DScene.allocRay();
            camera_ray.origin.set(uiX, uiY, 0.0F);
            camera_ray.direction.set(0.0F, 0.0F, -1.0F);
            UI3DScene.closest_distance_line_circle(camera_ray, circle, out);
            UI3DScene.releaseRay(camera_ray);
            return out;
        }

        float calculateRotation(Vector3f Pp, Vector3f Pc, UI3DScene.Circle circle) {
            if (Pp.equals(Pc)) {
                return 0.0F;
            } else {
                Vector3f Vp = UI3DScene.allocVector3f().set(Pp).sub(circle.center).normalize();
                Vector3f Vc = UI3DScene.allocVector3f().set(Pc).sub(circle.center).normalize();
                float angle = (float)Math.acos(Vc.dot(Vp));
                Vector3f cross = Vp.cross(Vc, UI3DScene.allocVector3f());
                int sign = (int)Math.signum(cross.dot(circle.orientation));
                UI3DScene.releaseVector3f(Vp);
                UI3DScene.releaseVector3f(Vc);
                UI3DScene.releaseVector3f(cross);
                return sign * angle * (180.0F / (float)Math.PI);
            }
        }
    }

    private final class ScaleGizmo extends UI3DScene.Gizmo {
        final Matrix4f startXfrm;
        final Matrix4f startInvXfrm;
        final Vector3f startPos;
        final Vector3f currentPos;
        UI3DScene.Axis trackAxis;
        boolean snap;
        boolean hideX;
        boolean hideY;
        boolean hideZ;
        final Cylinder cylinder;

        private ScaleGizmo() {
            Objects.requireNonNull(UI3DScene.this);
            super();
            this.startXfrm = new Matrix4f();
            this.startInvXfrm = new Matrix4f();
            this.startPos = new Vector3f();
            this.currentPos = new Vector3f();
            this.trackAxis = UI3DScene.Axis.None;
            this.snap = true;
            this.cylinder = new Cylinder();
        }

        @Override
        UI3DScene.Axis hitTest(float uiX, float uiY) {
            if (!this.visible) {
                return UI3DScene.Axis.None;
            } else {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
                Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
                gizmoXfrm.set(stateData.gizmoParentTransform);
                gizmoXfrm.mul(stateData.gizmoOriginTransform);
                gizmoXfrm.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                gizmoXfrm.mul(stateData.gizmoTransform);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    gizmoXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                uiY = UI3DScene.this.screenHeight() - uiY;
                UI3DScene.Ray camera_ray = UI3DScene.this.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                UI3DScene.Ray axis = UI3DScene.allocRay();
                gizmoXfrm.transformProject(axis.origin.set(0.0F, 0.0F, 0.0F));
                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float LENGTH = 0.5F * SCALE;
                float THICKNESS = 0.05F * SCALE;
                float OFFSET = 0.1F * SCALE;
                gizmoXfrm.transformDirection(axis.direction.set(1.0F, 0.0F, 0.0F)).normalize();
                float distX = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float x_t = axis.t;
                float cam_x_t = camera_ray.t;
                if (x_t < OFFSET || x_t >= OFFSET + LENGTH) {
                    x_t = Float.MAX_VALUE;
                    distX = Float.MAX_VALUE;
                }

                float xdot = axis.direction.dot(camera_ray.direction);
                this.hideX = Math.abs(xdot) > 0.9F;
                gizmoXfrm.transformDirection(axis.direction.set(0.0F, 1.0F, 0.0F)).normalize();
                float distY = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float y_t = axis.t;
                float cam_y_t = camera_ray.t;
                if (y_t < OFFSET || y_t >= OFFSET + LENGTH) {
                    y_t = Float.MAX_VALUE;
                    distY = Float.MAX_VALUE;
                }

                float ydot = axis.direction.dot(camera_ray.direction);
                this.hideY = Math.abs(ydot) > 0.9F;
                gizmoXfrm.transformDirection(axis.direction.set(0.0F, 0.0F, 1.0F)).normalize();
                float distZ = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float z_t = axis.t;
                float cam_z_t = camera_ray.t;
                if (z_t < OFFSET || z_t >= OFFSET + LENGTH) {
                    z_t = Float.MAX_VALUE;
                    distZ = Float.MAX_VALUE;
                }

                float zdot = axis.direction.dot(camera_ray.direction);
                this.hideZ = Math.abs(zdot) > 0.9F;
                UI3DScene.releaseRay(axis);
                UI3DScene.releaseRay(camera_ray);
                UI3DScene.releaseMatrix4f(gizmoXfrm);
                if (x_t >= OFFSET && x_t < OFFSET + LENGTH && distX < distY && distX < distZ) {
                    return distX <= THICKNESS / 2.0F ? UI3DScene.Axis.X : UI3DScene.Axis.None;
                } else if (y_t >= OFFSET && y_t < OFFSET + LENGTH && distY < distX && distY < distZ) {
                    return distY <= THICKNESS / 2.0F ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
                } else if (z_t >= OFFSET && z_t < OFFSET + LENGTH && distZ < distX && distZ < distY) {
                    return distZ <= THICKNESS / 2.0F ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
                } else {
                    return UI3DScene.Axis.None;
                }
            }
        }

        @Override
        void startTracking(float uiX, float uiY, UI3DScene.Axis axis) {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
            UI3DScene.this.setModelViewProjection(stateData);
            UI3DScene.this.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }

            if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            this.startInvXfrm.set(this.startXfrm);
            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPos.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
            } else {
                this.currentPos.set(pos);
                UI3DScene.releaseVector3f(pos);
                UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
                UI3DScene.this.setModelViewProjection(stateData);
                UI3DScene.this.setGizmoTransforms(stateData);
                Vector3f delta = new Vector3f(this.currentPos).sub(this.startPos);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                    Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                    Matrix4f m = UI3DScene.allocMatrix4f();
                    m.set(stateData.gizmoParentTransform);
                    m.mul(stateData.gizmoOriginTransform);
                    if (!stateData.selectedAttachmentIsChildAttachment) {
                        m.mul(stateData.gizmoChildTransform);
                    }

                    m.invert();
                    m.transformPosition(vs);
                    m.transformPosition(vc);
                    UI3DScene.releaseMatrix4f(m);
                    delta.set(vc).sub(vs);
                    UI3DScene.releaseVector3f(vs);
                    UI3DScene.releaseVector3f(vc);
                } else {
                    Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                    Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                    delta.set(vc).sub(vs);
                    UI3DScene.releaseVector3f(vs);
                    UI3DScene.releaseVector3f(vc);
                }

                if (this.snap) {
                    delta.x = PZMath.fastfloor(delta.x * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
                    delta.y = PZMath.fastfloor(delta.y * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
                    delta.z = PZMath.fastfloor(delta.z * UI3DScene.this.gridMult()) / UI3DScene.this.gridMult();
                }

                LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), UI3DScene.this.table, delta);
            }
        }

        @Override
        void stopTracking() {
            this.trackAxis = UI3DScene.Axis.None;
        }

        @Override
        void render() {
            if (this.visible) {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float LENGTH = 0.5F * SCALE;
                float THICKNESS = 0.05F * SCALE;
                float OFFSET = 0.1F * SCALE;
                Matrix4f matrix4f = UI3DScene.allocMatrix4f();
                matrix4f.set(stateData.gizmoParentTransform);
                matrix4f.mul(stateData.gizmoOriginTransform);
                matrix4f.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                matrix4f.mul(stateData.gizmoTransform);
                Vector3f scale = matrix4f.getScale(UI3DScene.allocVector3f());
                matrix4f.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
                UI3DScene.releaseVector3f(scale);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    matrix4f.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                stateData.modelView.mul(matrix4f, matrix4f);
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.cmdPushAndLoadMatrix(5888, matrix4f);
                if (!this.hideX) {
                    float r = stateData.gizmoAxis != UI3DScene.Axis.X && this.trackAxis != UI3DScene.Axis.X ? 0.5F : 1.0F;
                    float g = 0.0F;
                    float b = 0.0F;
                    float a = 1.0F;
                    matrix4f.rotation((float) (Math.PI / 2), 0.0F, 1.0F, 0.0F);
                    matrix4f.translate(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, r, 0.0F, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS, THICKNESS, 0.1F * SCALE, 8, 1, r, 0.0F, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                if (!this.hideY) {
                    float r = 0.0F;
                    float g = stateData.gizmoAxis != UI3DScene.Axis.Y && this.trackAxis != UI3DScene.Axis.Y ? 0.5F : 1.0F;
                    float b = 0.0F;
                    float a = 1.0F;
                    matrix4f.rotation((float) (-Math.PI / 2), 1.0F, 0.0F, 0.0F);
                    matrix4f.translate(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, g, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS, THICKNESS, 0.1F * SCALE, 8, 1, 0.0F, g, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                if (!this.hideZ) {
                    float r = 0.0F;
                    float g = 0.0F;
                    float b = stateData.gizmoAxis != UI3DScene.Axis.Z && this.trackAxis != UI3DScene.Axis.Z ? 0.5F : 1.0F;
                    float a = 1.0F;
                    matrix4f.translation(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, 0.0F, b, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS, THICKNESS, 0.1F * SCALE, 8, 1, 0.0F, 0.0F, b, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                UI3DScene.releaseMatrix4f(matrix4f);
                vbor.cmdPopMatrix(5888);
                this.renderLineToOrigin();
                GLStateRenderThread.restore();
            }
        }
    }

    private static final class SceneAnimal extends UI3DScene.SceneCharacter implements IAnimalVisual {
        AnimalVisual visual;
        final ItemVisuals itemVisuals = new ItemVisuals();
        AnimalDefinitions definition;
        AnimalBreed breed;

        SceneAnimal(UI3DScene scene, String id, AnimalDefinitions definition, AnimalBreed breed) {
            super(scene, id);
            this.definition = definition;
            this.breed = breed;
        }

        void setAnimalDefinition(AnimalDefinitions definition, AnimalBreed breed) {
            this.definition = definition;
            this.breed = breed;
            if (this.isFemale()) {
                this.visual.setSkinTextureName(PZArrayUtil.pickRandom(this.breed.texture));
            } else {
                this.visual.setSkinTextureName(this.breed.textureMale);
            }

            if (!this.animatedModel.GetAnimSetName().endsWith("-editor")) {
                this.animatedModel.setAnimSetName(this.definition.animset);
            }

            this.animatedModel.setModelData(this.visual, this.itemVisuals);
        }

        @Override
        void initAnimatedModel() {
            this.visual = new AnimalVisual(this);
            if (this.isFemale()) {
                this.visual.setSkinTextureName(PZArrayUtil.pickRandom(this.breed.texture));
            } else {
                this.visual.setSkinTextureName(this.breed.textureMale);
            }

            this.animatedModel.setAnimSetName(this.definition.animset);
            this.animatedModel.setState("idle");
            this.animatedModel.setModelData(this.visual, this.itemVisuals);
            this.animatedModel.setAlpha(0.5F);
            this.animatedModel.setAnimate(false);
        }

        @Override
        public AnimalVisual getAnimalVisual() {
            return this.visual;
        }

        @Override
        public String getAnimalType() {
            return this.definition.getAnimalType();
        }

        @Override
        public float getAnimalSize() {
            return 1.0F;
        }

        @Override
        public HumanVisual getHumanVisual() {
            return null;
        }

        @Override
        public void getItemVisuals(ItemVisuals itemVisuals) {
            itemVisuals.clear();
            itemVisuals.addAll(this.itemVisuals);
        }

        @Override
        public boolean isFemale() {
            return this.definition.female;
        }

        @Override
        public boolean isZombie() {
            return false;
        }

        @Override
        public boolean isSkeleton() {
            return false;
        }
    }

    private static final class SceneBox extends UI3DScene.SceneGeometry {
        final Vector3f min = new Vector3f(-0.5F, 0.0F, -0.5F);
        final Vector3f max = new Vector3f(0.5F, 2.44949F, 0.5F);

        SceneBox(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "box";
        }

        @Override
        void initClone(UI3DScene.SceneObject clone) {
            UI3DScene.SceneBox box = (UI3DScene.SceneBox)clone;
            super.initClone(clone);
            box.min.set(this.min);
            box.max.set(this.max);
        }

        @Override
        UI3DScene.SceneObject clone(String id) {
            UI3DScene.SceneBox clone = new UI3DScene.SceneBox(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            } else {
                UI3DScene.Box3D box3D = UI3DScene.s_box3DPool.alloc();
                box3D.x = this.translate.x;
                box3D.y = this.translate.y;
                box3D.z = this.translate.z;
                box3D.rx = this.rotate.x;
                box3D.ry = this.rotate.y;
                box3D.rz = this.rotate.z;
                box3D.xMin = this.min.x;
                box3D.yMin = this.min.y;
                box3D.zMin = this.min.z;
                box3D.xMax = this.max.x;
                box3D.yMax = this.max.y;
                box3D.zMax = this.max.z;
                box3D.r = box3D.g = box3D.b = box3D.a = 1.0F;
                box3D.quads = false;
                if (this.selected) {
                    box3D.b = 0.0F;
                }

                this.scene.box3d.add(box3D);
                return null;
            }
        }

        @Override
        public boolean isBox() {
            return true;
        }

        @Override
        Matrix4f getOriginTransform(String hint, Matrix4f xfrm) {
            xfrm.identity();
            switch (hint) {
                case "xMin":
                    xfrm.translation(this.min.x(), 0.0F, 0.0F);
                    break;
                case "xMax":
                    xfrm.translation(this.max.x(), 0.0F, 0.0F);
                    break;
                case "yMin":
                    xfrm.translation(0.0F, this.min.y(), 0.0F);
                    break;
                case "yMax":
                    xfrm.translation(0.0F, this.max.y(), 0.0F);
                    break;
                case "zMin":
                    xfrm.translation(0.0F, 0.0F, this.min.z());
                    break;
                case "zMax":
                    xfrm.translation(0.0F, 0.0F, this.max.z());
            }

            return xfrm;
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnBoxAt(tileX, tileY, this.translate, this.rotate, this.min, this.max);
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Box tileBox = new TileGeometryFile.Box();
            tileBox.translate.set(this.translate);
            tileBox.rotate.set(this.rotate);
            tileBox.min.set(this.min);
            tileBox.max.set(this.max);
            return tileBox;
        }
    }

    private abstract static class SceneCharacter extends UI3DScene.SceneObject {
        final AnimatedModel animatedModel;
        boolean showBones;
        boolean showBip01;
        boolean clearDepthBuffer = true;
        boolean useDeferredMovement;

        SceneCharacter(UI3DScene scene, String id) {
            super(scene, id);
            this.animatedModel = new AnimatedModel();
        }

        abstract void initAnimatedModel();

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            this.animatedModel.update();
            UI3DScene.CharacterRenderData renderData = UI3DScene.CharacterRenderData.s_pool.alloc();
            renderData.initCharacter(this);
            SpriteRenderer.instance.drawGeneric(renderData.drawer);
            return renderData;
        }

        @Override
        Matrix4f getLocalTransform(Matrix4f transform) {
            transform.identity();
            transform.translate(this.translate.x, this.translate.y, this.translate.z);
            float rotateY = this.rotate.y;
            transform.rotateXYZ(-this.rotate.x * (float) (Math.PI / 180.0), -rotateY * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            transform.scale(1.5F * this.scale.x, 1.5F * this.scale.y, 1.5F * this.scale.z);
            Matrix4f m = UI3DScene.allocMatrix4f();
            m.identity();
            m.rotateY((float) Math.PI);
            m.scale(-1.0F, 1.0F, 1.0F);
            transform.mul(m);
            UI3DScene.releaseMatrix4f(m);
            if (this.autoRotate) {
                transform.rotateY(this.autoRotateAngle * (float) (Math.PI / 180.0));
            }

            if (this.animatedModel.getAnimationPlayer().getMultiTrack().getTracks().isEmpty()) {
                return transform;
            } else {
                if (this.useDeferredMovement) {
                    AnimationMultiTrack multiTrack = this.animatedModel.getAnimationPlayer().getMultiTrack();
                    float deferredRotation = multiTrack.getTracks().get(0).getCurrentDeferredRotation();
                    org.lwjgl.util.vector.Vector3f translate = new org.lwjgl.util.vector.Vector3f();
                    multiTrack.getTracks().get(0).getCurrentDeferredPosition(translate);
                    transform.translate(translate.x, translate.y, translate.z);
                }

                return transform;
            }
        }

        @Override
        Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
            transform.identity();
            boolean bFemale = this.animatedModel.isFemale();
            ModelScript modelScript = ScriptManager.instance.getModelScript(bFemale ? "FemaleBody" : "MaleBody");
            if (modelScript == null) {
                return transform;
            } else {
                ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
                if (attachment == null) {
                    return transform;
                } else {
                    ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
                    if (attachment.getBone() != null) {
                        Matrix4f boneXfrm = this.getBoneMatrix(attachment.getBone(), UI3DScene.allocMatrix4f());
                        boneXfrm.mul(transform, transform);
                        UI3DScene.releaseMatrix4f(boneXfrm);
                    }

                    return transform;
                }
            }
        }

        int hitTestBone(int boneIndex, UI3DScene.Ray boneRay, UI3DScene.Ray cameraRay, Matrix4f characterMatrix, Vector2f out) {
            AnimationPlayer animPlayer = this.animatedModel.getAnimationPlayer();
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            } else {
                org.lwjgl.util.vector.Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
                boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                characterMatrix.transformPosition(boneRay.origin);
                boneMatrix = animPlayer.getModelTransformAt(boneIndex);
                Vector3f nextBone = UI3DScene.allocVector3f();
                nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                characterMatrix.transformPosition(nextBone);
                boneRay.direction.set(nextBone).sub(boneRay.origin);
                float boneLength = boneRay.direction.length();
                boneRay.direction.normalize();
                UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
                float camX = this.scene
                    .sceneToUIX(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float camY = this.scene
                    .sceneToUIY(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float boneX = this.scene
                    .sceneToUIX(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                float boneY = this.scene
                    .sceneToUIY(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                int hitBone = -1;
                float PICK_DIST = 10.0F;
                float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
                if (dist < 10.0F) {
                    if ((!(boneRay.t >= 0.0F) || !(boneRay.t <= 0.01F)) && (!(boneRay.t >= 0.09F) || !(boneRay.t <= 1.0F))) {
                        out.set(dist, 0.0F);
                    } else {
                        out.set(dist / 10.0F, 0.0F);
                    }

                    if (boneRay.t >= 0.0F && boneRay.t < boneLength * 0.5F) {
                        hitBone = parentIndex;
                    } else if (boneRay.t >= boneLength * 0.5F && boneRay.t < boneLength) {
                        hitBone = boneIndex;
                    }
                }

                UI3DScene.releaseVector3f(nextBone);
                return hitBone;
            }
        }

        String pickBone(float uiX, float uiY) {
            if (this.animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
                return "";
            } else {
                uiY = this.scene.screenHeight() - uiY;
                UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                Matrix4f characterXfrm = UI3DScene.allocMatrix4f();
                this.getLocalTransform(characterXfrm);
                UI3DScene.Ray boneRay = UI3DScene.allocRay();
                int hitBoneIndex = -1;
                Vector2f dist = UI3DScene.allocVector2f();
                float closestDist = Float.MAX_VALUE;

                for (int i = 0; i < this.animatedModel.getAnimationPlayer().getModelTransformsCount(); i++) {
                    int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, characterXfrm, dist);
                    if (testBoneIndex != -1 && dist.x < closestDist) {
                        closestDist = dist.x;
                        hitBoneIndex = testBoneIndex;
                    }
                }

                UI3DScene.releaseVector2f(dist);
                UI3DScene.releaseRay(boneRay);
                UI3DScene.releaseRay(cameraRay);
                UI3DScene.releaseMatrix4f(characterXfrm);
                return hitBoneIndex == -1 ? "" : this.animatedModel.getAnimationPlayer().getSkinningData().getBoneAt(hitBoneIndex).name;
            }
        }

        Matrix4f getBoneMatrix(String boneName, Matrix4f mat) {
            mat.identity();
            if (this.animatedModel.getAnimationPlayer().getModelTransformsCount() == 0) {
                return mat;
            } else {
                SkinningBone bone = this.animatedModel.getAnimationPlayer().getSkinningData().getBone(boneName);
                if (bone == null) {
                    return mat;
                } else {
                    mat = PZMath.convertMatrix(this.animatedModel.getAnimationPlayer().getModelTransformAt(bone.index), mat);
                    mat.transpose();
                    return mat;
                }
            }
        }

        UI3DScene.PositionRotation getBoneAxis(String boneName, UI3DScene.PositionRotation axis) {
            Matrix4f mat = UI3DScene.allocMatrix4f().identity();
            mat.getTranslation(axis.pos);
            UI3DScene.releaseMatrix4f(mat);
            Quaternionf q = mat.getUnnormalizedRotation(UI3DScene.allocQuaternionf());
            q.getEulerAnglesXYZ(axis.rot);
            UI3DScene.releaseQuaternionf(q);
            return axis;
        }
    }

    private static final class SceneCylinder extends UI3DScene.SceneGeometry {
        float radius;
        float height;

        SceneCylinder(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "cylinder";
        }

        @Override
        void initClone(UI3DScene.SceneObject clone) {
            UI3DScene.SceneCylinder cylinder = (UI3DScene.SceneCylinder)clone;
            super.initClone(clone);
            cylinder.radius = this.radius;
            cylinder.height = this.height;
        }

        @Override
        UI3DScene.SceneObject clone(String id) {
            UI3DScene.SceneCylinder clone = new UI3DScene.SceneCylinder(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            } else {
                UI3DScene.SceneCylinderDrawer drawer = UI3DScene.SceneCylinderDrawer.s_pool.alloc();
                drawer.sceneObject = this;
                drawer.radiusBase = this.radius;
                drawer.radiusTop = this.radius;
                drawer.length = this.height;
                SpriteRenderer.instance.drawGeneric(drawer);
                return null;
            }
        }

        @Override
        public boolean isCylinder() {
            return true;
        }

        @Override
        Matrix4f getOriginTransform(String hint, Matrix4f xfrm) {
            xfrm.identity();
            switch (hint) {
                case "xMin":
                    xfrm.translation(-this.radius, 0.0F, 0.0F);
                    break;
                case "xMax":
                    xfrm.translation(this.radius, 0.0F, 0.0F);
                    break;
                case "yMin":
                    xfrm.translation(0.0F, -this.radius, 0.0F);
                    break;
                case "yMax":
                    xfrm.translation(0.0F, this.radius, 0.0F);
                    break;
                case "zMin":
                    xfrm.translation(0.0F, 0.0F, -this.height / 2.0F);
                    break;
                case "zMax":
                    xfrm.translation(0.0F, 0.0F, this.height / 2.0F);
            }

            return xfrm;
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            return TileGeometryUtils.getNormalizedDepthOnCylinderAt(tileX, tileY, this.translate, this.rotate, this.radius, this.height);
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Cylinder cylinder = new TileGeometryFile.Cylinder();
            cylinder.translate.set(this.translate);
            cylinder.rotate.set(this.rotate);
            cylinder.radius1 = this.radius;
            cylinder.radius2 = this.radius;
            cylinder.height = this.height;
            return cylinder;
        }

        boolean intersect(UI3DScene.Ray rayIn, CylinderUtils.IntersectionRecord outRecord) {
            return CylinderUtils.intersect(this.radius, this.height, rayIn, outRecord);
        }

        UI3DScene.AABB getAABB(UI3DScene.AABB aabb) {
            Matrix4f m = UI3DScene.allocMatrix4f()
                .rotationXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            Vector3f zAxis = m.transformDirection(UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F)).normalize();
            UI3DScene.releaseMatrix4f(m);
            Vector3f pa = UI3DScene.allocVector3f().set(zAxis).mul(-this.height / 2.0F).add(this.translate);
            Vector3f pb = UI3DScene.allocVector3f().set(zAxis).mul(this.height / 2.0F).add(this.translate);
            UI3DScene.releaseVector3f(zAxis);
            Vector3f a = UI3DScene.allocVector3f().set(pb).sub(pa);
            Vector3f axa = UI3DScene.allocVector3f().set(a).mul(a);
            axa.div(a.dot(a));
            UI3DScene.releaseVector3f(a);
            Vector3f e = UI3DScene.allocVector3f()
                .set(this.radius * Math.sqrt(1.0 - axa.x), this.radius * Math.sqrt(1.0 - axa.y), this.radius * Math.sqrt(1.0 - axa.z));
            UI3DScene.releaseVector3f(axa);
            Vector3f paMe = UI3DScene.allocVector3f().set(pa).sub(e);
            Vector3f pbMe = UI3DScene.allocVector3f().set(pb).sub(e);
            Vector3f paPe = UI3DScene.allocVector3f().set(pa).add(e);
            Vector3f pbPe = UI3DScene.allocVector3f().set(pb).add(e);
            UI3DScene.releaseVector3f(pa);
            UI3DScene.releaseVector3f(pb);
            UI3DScene.releaseVector3f(e);
            Vector3f min = UI3DScene.allocVector3f().set(paMe).min(pbMe);
            Vector3f max = UI3DScene.allocVector3f().set(paPe).max(pbPe);
            UI3DScene.releaseVector3f(paMe);
            UI3DScene.releaseVector3f(pbMe);
            UI3DScene.releaseVector3f(paPe);
            UI3DScene.releaseVector3f(pbPe);
            aabb.set(this.translate.x, this.translate.y, this.translate.z, max.x - min.x, max.y - min.y, max.z - min.z, 1.0F, 1.0F, 1.0F, 1.0F, false);
            UI3DScene.releaseVector3f(min);
            UI3DScene.releaseVector3f(max);
            return aabb;
        }
    }

    private static final class SceneCylinderDrawer extends TextureDraw.GenericDrawer {
        UI3DScene.SceneCylinder sceneObject;
        float radiusBase;
        float radiusTop;
        float length;
        int slices = 32;
        int stacks = 2;
        private static final ObjectPool<UI3DScene.SceneCylinderDrawer> s_pool = new ObjectPool<>(UI3DScene.SceneCylinderDrawer::new);

        @Override
        public void render() {
            UI3DScene scene = this.sceneObject.scene;
            UI3DScene.StateData stateData = scene.stateDataRender();
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            PZGLUtil.pushAndLoadMatrix(5888, stateData.modelView);
            boolean wireframe = false;
            GL11.glPolygonMode(1032, 6914);
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
            GL11.glDisable(2929);

            for (int i = 7; i >= 0; i--) {
                GL13.glActiveTexture(33984 + i);
                GL11.glDisable(3553);
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            boolean bLighting = false;
            VBORenderer vbor = VBORenderer.getInstance();
            Matrix4f cylinderMatrix = UI3DScene.allocMatrix4f();
            this.sceneObject.getGlobalTransform(cylinderMatrix);
            PZGLUtil.pushAndMultMatrix(5888, cylinderMatrix);
            GL11.glEnable(2929);
            GL11.glDepthFunc(513);
            Core.getInstance().modelViewMatrixStack.peek().translate(0.0F, 0.0F, -this.length / 2.0F);
            float r = 1.0F;
            float g = 1.0F;
            float b = this.sceneObject.selected ? 0.0F : 1.0F;
            vbor.addCylinder_Line(this.radiusBase, this.radiusTop, this.length, this.slices, this.stacks, 1.0F, 1.0F, b, 0.75F);
            vbor.flush();
            Core.getInstance().modelViewMatrixStack.peek().translate(0.0F, 0.0F, this.length / 2.0F);
            GL11.glDisable(2929);
            GL11.glDepthFunc(519);
            GL11.glPolygonMode(1032, 6914);
            float mouseX = Mouse.getXA() - scene.getAbsoluteX().intValue();
            float mouseY = Mouse.getYA() - scene.getAbsoluteY().intValue();
            UI3DScene.Ray cameraRay = scene.getCameraRay(mouseX, scene.screenHeight() - mouseY, stateData.projection, stateData.modelView, UI3DScene.allocRay());
            cylinderMatrix.invert();
            cylinderMatrix.transformPosition(cameraRay.origin);
            cylinderMatrix.transformDirection(cameraRay.direction);
            UI3DScene.releaseMatrix4f(cylinderMatrix);
            CylinderUtils.IntersectionRecord intersectionRecord = new CylinderUtils.IntersectionRecord();
            if (this.sceneObject.intersect(cameraRay, intersectionRecord)) {
                vbor.startRun(vbor.formatPositionColor);
                vbor.setMode(1);
                vbor.addLine(
                    intersectionRecord.location.x,
                    intersectionRecord.location.y,
                    intersectionRecord.location.z,
                    intersectionRecord.location.x + intersectionRecord.normal.x,
                    intersectionRecord.location.y + intersectionRecord.normal.y,
                    intersectionRecord.location.z + intersectionRecord.normal.z,
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
                );
                UI3DScene.vboRenderer.endRun();
                vbor.flush();
                Model.debugDrawAxis(intersectionRecord.location.x, intersectionRecord.location.y, intersectionRecord.location.z, 0.1F, 1.0F);
            }

            UI3DScene.releaseRay(cameraRay);
            PZGLUtil.popMatrix(5888);
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
            s_pool.release(this);
        }
    }

    private static final class SceneDepthTexture extends UI3DScene.SceneObject {
        Texture texture;

        SceneDepthTexture(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            IndieGL.enableDepthTest();
            IndieGL.glDepthMask(true);
            IndieGL.glDepthFunc(519);
            if (!this.scene.drawGeometry) {
                IndieGL.glColorMask(false, false, false, false);
            }

            this.renderTexture(this.translate.x, this.translate.y, this.translate.z);
            if (!this.scene.drawGeometry) {
                IndieGL.glColorMask(true, true, true, true);
            }

            IndieGL.disableDepthTest();
            IndieGL.glDepthMask(false);
            return null;
        }

        static float calculatePixelSize(UI3DScene scene) {
            float sx = scene.sceneToUIX(0.0F, 0.0F, 0.0F);
            float sy = scene.sceneToUIY(0.0F, 0.0F, 0.0F);
            float sx2 = scene.sceneToUIX(1.0F, 0.0F, 0.0F);
            float sy2 = scene.sceneToUIY(1.0F, 0.0F, 0.0F);
            return (float)(Math.sqrt((sx2 - sx) * (sx2 - sx) + (sy2 - sy) * (sy2 - sy)) / Math.sqrt(5120.0));
        }

        static Vector2f calculateTextureTopLeft(UI3DScene scene, float sceneX, float sceneY, float sceneZ, Vector2f topLeft) {
            float sx = scene.sceneToUIX(sceneX, sceneY, sceneZ);
            float sy = scene.sceneToUIY(sceneX, sceneY, sceneZ);
            float pixelSize = calculatePixelSize(scene);
            float tileX = sx - 64.0F * pixelSize;
            float tileY = sy - 224.0F * pixelSize;
            int TEXTURE_OFFSET_X = 1;
            tileX += 1.0F * pixelSize;
            return topLeft.set(tileX, tileY);
        }

        void renderTexture(float x, float y, float z) {
            Matrix4f mvp = UI3DScene.allocMatrix4f();
            mvp.set(this.scene.projection);
            mvp.mul(this.scene.modelView);
            Vector3f v = UI3DScene.allocVector3f();
            float frontDepthZ = mvp.transformPosition(v.set(x + 1.5F, y + 0.0F, z + 1.5F)).z;
            float farDepthZ = mvp.transformPosition(v.set(x - 0.5F, y + 0.0F, z - 0.5F)).z;
            frontDepthZ = (frontDepthZ + 1.0F) / 2.0F;
            farDepthZ = (farDepthZ + 1.0F) / 2.0F;
            UI3DScene.releaseMatrix4f(mvp);
            UI3DScene.releaseVector3f(v);
            this.texture.getTextureId().setMagFilter(9728);
            IndieGL.StartShader(SceneShaderStore.tileDepthShader.getID());
            IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepth", frontDepthZ);
            boolean drawPixels = false;
            IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "drawPixels", 0);
            IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepthBlendZ", frontDepthZ);
            IndieGL.shaderSetValue(SceneShaderStore.tileDepthShader, "zDepthBlendToZ", farDepthZ);
            float pixelSize = calculatePixelSize(this.scene);
            Vector2f topLeft = calculateTextureTopLeft(this.scene, x, y, z, UI3DScene.allocVector2f());
            SpriteRenderer.instance
                .render(
                    this.texture,
                    topLeft.x + this.texture.getOffsetX() * pixelSize,
                    topLeft.y + this.texture.getOffsetY() * pixelSize,
                    this.texture.getWidth() * pixelSize,
                    this.texture.getHeight() * pixelSize,
                    1.0F,
                    1.0F,
                    1.0F,
                    0.5F,
                    null
                );
            UI3DScene.releaseVector2f(topLeft);
            int numSprites = SpriteRenderer.instance.states.getPopulatingActiveState().numSprites;
            TextureDraw textureDraw = SpriteRenderer.instance.states.getPopulatingActiveState().sprite[numSprites - 1];
            Texture tex = this.texture;
            textureDraw.tex1 = tex;
            textureDraw.tex1U0 = tex.getXStart();
            textureDraw.tex1U1 = tex.getXEnd();
            textureDraw.tex1U2 = tex.getXEnd();
            textureDraw.tex1U3 = tex.getXStart();
            textureDraw.tex1V0 = tex.getYStart();
            textureDraw.tex1V1 = tex.getYStart();
            textureDraw.tex1V2 = tex.getYEnd();
            textureDraw.tex1V3 = tex.getYEnd();
            IndieGL.EndShader();
        }
    }

    private abstract static class SceneGeometry extends UI3DScene.SceneObject {
        boolean selected;

        SceneGeometry(UI3DScene scene, String id) {
            super(scene, id);
        }

        public abstract String getTypeName();

        public boolean isBox() {
            return false;
        }

        public UI3DScene.SceneBox asBox() {
            return Type.tryCastTo(this, UI3DScene.SceneBox.class);
        }

        public boolean isCylinder() {
            return false;
        }

        public UI3DScene.SceneCylinder asCylinder() {
            return Type.tryCastTo(this, UI3DScene.SceneCylinder.class);
        }

        public boolean isPolygon() {
            return false;
        }

        public UI3DScene.ScenePolygon asPolygon() {
            return Type.tryCastTo(this, UI3DScene.ScenePolygon.class);
        }

        abstract Matrix4f getOriginTransform(String arg0, Matrix4f arg1);

        abstract float getNormalizedDepthAt(float arg0, float arg1);

        abstract TileGeometryFile.Geometry toGeometryFileObject();
    }

    private static final class SceneModel extends UI3DScene.SceneObject {
        SpriteModel spriteModel;
        ModelScript modelScript;
        Model model;
        Texture texture;
        boolean useWorldAttachment;
        boolean weaponRotationHack;
        boolean ignoreVehicleScale;
        boolean spriteModelEditor;
        static AnimationPlayer animationPlayer;

        SceneModel(UI3DScene scene, String id, ModelScript modelScript, Model model) {
            super(scene, id);
            Objects.requireNonNull(modelScript);
            Objects.requireNonNull(model);
            this.modelScript = modelScript;
            this.model = model;
            this.setSpriteModel(null);
        }

        void setSpriteModel(SpriteModel spriteModel) {
            this.spriteModel = spriteModel;
            this.texture = null;
            if (spriteModel != null && spriteModel.getTextureName() != null) {
                if (spriteModel.getTextureName().contains("media/")) {
                    this.texture = Texture.getSharedTexture(spriteModel.getTextureName());
                } else {
                    this.texture = Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
                }
            }
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            if (!this.model.isReady()) {
                return null;
            } else {
                UI3DScene.ModelRenderData renderData = UI3DScene.ModelRenderData.s_pool.alloc();
                renderData.initModel(this);
                SpriteRenderer.instance.drawGeneric(renderData.drawer);
                return renderData;
            }
        }

        @Override
        Matrix4f getLocalTransform(Matrix4f transform) {
            super.getLocalTransform(transform);
            return transform;
        }

        @Override
        Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
            transform.identity();
            ModelAttachment attachment = this.modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            } else {
                ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
                return transform;
            }
        }

        static AnimationPlayer initAnimationPlayer(Model model) {
            if (animationPlayer != null) {
                while (animationPlayer.getMultiTrack().getTrackCount() > 0) {
                    animationPlayer.getMultiTrack().removeTrackAt(0);
                }
            }

            if (animationPlayer != null && animationPlayer.getModel() != model) {
                animationPlayer.release();
                animationPlayer = null;
            }

            if (animationPlayer == null) {
                animationPlayer = AnimationPlayer.alloc(model);
            }

            if (!animationPlayer.isReady()) {
                return null;
            } else {
                AnimationTrack track = animationPlayer.play("Open", false);
                if (track == null) {
                    return null;
                } else {
                    float duration = track.getDuration();
                    track.setCurrentTimeValue(0.0F);
                    track.setBlendWeight(1.0F);
                    track.setSpeedDelta(1.0F);
                    track.isPlaying = false;
                    track.reverse = false;
                    animationPlayer.Update(100.0F);
                    return animationPlayer;
                }
            }
        }

        int hitTestBone(int boneIndex, UI3DScene.Ray boneRay, UI3DScene.Ray cameraRay, Matrix4f characterMatrix, Vector2f out) {
            AnimationPlayer animPlayer = animationPlayer;
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            } else {
                org.lwjgl.util.vector.Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
                boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                characterMatrix.transformPosition(boneRay.origin);
                boneMatrix = animPlayer.getModelTransformAt(boneIndex);
                Vector3f nextBone = UI3DScene.allocVector3f();
                nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                characterMatrix.transformPosition(nextBone);
                boneRay.direction.set(nextBone).sub(boneRay.origin);
                float boneLength = boneRay.direction.length();
                if (boneLength < 0.001F) {
                    if (PZArrayUtil.contains(skinningData.skeletonHierarchy, i -> i == boneIndex)) {
                        return -1;
                    }

                    boneRay.direction.set(0.0F, 1.0F, 0.0F);
                    boneLength = 1.0F;
                }

                boneRay.direction.normalize();
                UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
                float camX = this.scene
                    .sceneToUIX(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float camY = this.scene
                    .sceneToUIY(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float boneX = this.scene
                    .sceneToUIX(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                float boneY = this.scene
                    .sceneToUIY(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                int hitBone = -1;
                float PICK_DIST = 10.0F;
                float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
                if (dist < 10.0F) {
                    if ((!(boneRay.t >= 0.0F) || !(boneRay.t <= 0.01F)) && (!(boneRay.t >= 0.09F) || !(boneRay.t <= 1.0F))) {
                        out.set(dist, 0.0F);
                    } else {
                        out.set(dist / 10.0F, 0.0F);
                    }

                    if (boneRay.t >= 0.0F && boneRay.t < boneLength * 0.5F) {
                        hitBone = parentIndex;
                    } else if (boneRay.t >= boneLength * 0.5F && boneRay.t < boneLength) {
                        hitBone = boneIndex;
                    }
                }

                UI3DScene.releaseVector3f(nextBone);
                return hitBone;
            }
        }

        String pickBone(float uiX, float uiY) {
            AnimationPlayer animationPlayer = initAnimationPlayer(this.model);
            if (animationPlayer == null) {
                return "";
            } else if (animationPlayer.getModelTransformsCount() == 0) {
                return "";
            } else {
                uiY = this.scene.screenHeight() - uiY;
                UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                Matrix4f characterXfrm = UI3DScene.allocMatrix4f();
                this.getLocalTransform(characterXfrm);
                UI3DScene.Ray boneRay = UI3DScene.allocRay();
                int hitBoneIndex = -1;
                Vector2f dist = UI3DScene.allocVector2f();
                float closestDist = Float.MAX_VALUE;

                for (int i = 0; i < animationPlayer.getModelTransformsCount(); i++) {
                    int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, characterXfrm, dist);
                    if (testBoneIndex != -1 && dist.x < closestDist) {
                        closestDist = dist.x;
                        hitBoneIndex = testBoneIndex;
                    }
                }

                UI3DScene.releaseVector2f(dist);
                UI3DScene.releaseRay(boneRay);
                UI3DScene.releaseRay(cameraRay);
                UI3DScene.releaseMatrix4f(characterXfrm);
                return hitBoneIndex == -1 ? "" : animationPlayer.getSkinningData().getBoneAt(hitBoneIndex).name;
            }
        }

        Matrix4f getBoneMatrix(String boneName, Matrix4f mat) {
            mat.identity();
            AnimationPlayer animationPlayer = initAnimationPlayer(this.model);
            if (animationPlayer == null) {
                return mat;
            } else if (animationPlayer.getModelTransformsCount() == 0) {
                return mat;
            } else {
                SkinningBone bone = animationPlayer.getSkinningData().getBone(boneName);
                if (bone == null) {
                    return mat;
                } else {
                    mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
                    mat.transpose();
                    return mat;
                }
            }
        }
    }

    private abstract class SceneModelCamera extends ModelCamera {
        UI3DScene.SceneObjectRenderData renderData;

        private SceneModelCamera() {
            Objects.requireNonNull(UI3DScene.this);
            super();
        }
    }

    private abstract static class SceneObject {
        final UI3DScene scene;
        final String id;
        boolean visible = true;
        final Vector3f translate = new Vector3f();
        final Vector3f rotate = new Vector3f();
        final Vector3f scale = new Vector3f(1.0F);
        UI3DScene.SceneObject parent;
        String attachment;
        String parentAttachment;
        boolean autoRotate;
        float autoRotateAngle;
        UI3DScene.ParentVehiclePart parentVehiclePart;

        SceneObject(UI3DScene scene, String id) {
            this.scene = scene;
            this.id = id;
        }

        void initClone(UI3DScene.SceneObject clone) {
            clone.visible = this.visible;
            clone.translate.set(this.translate);
            clone.rotate.set(this.rotate);
            clone.scale.set(this.scale);
            clone.parent = this.parent;
            clone.attachment = this.attachment;
            clone.parentAttachment = this.parentAttachment;
            clone.parentVehiclePart = this.parentVehiclePart;
            clone.autoRotate = this.autoRotate;
            clone.autoRotateAngle = this.autoRotateAngle;
        }

        UI3DScene.SceneObject clone(String id) {
            throw new RuntimeException("not implemented");
        }

        abstract UI3DScene.SceneObjectRenderData renderMain();

        Matrix4f getLocalTransform(Matrix4f transform) {
            transform.identity();
            UI3DScene.SceneModel sceneModel = Type.tryCastTo(this, UI3DScene.SceneModel.class);
            boolean bInvertX = sceneModel != null && sceneModel.spriteModelEditor;
            if (sceneModel != null && bInvertX) {
                transform.translate(this.translate.x, this.translate.y, this.translate.z);
            } else if (sceneModel != null && sceneModel.useWorldAttachment) {
                transform.translate(-this.translate.x, this.translate.y, this.translate.z);
            } else {
                transform.translate(this.translate);
            }

            float rotateY = this.rotate.y;
            transform.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), rotateY * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            transform.scale(this.scale.x, this.scale.y, this.scale.z);
            if (sceneModel != null && bInvertX) {
                transform.scale(-1.5F, 1.5F, 1.5F);
            } else if (sceneModel != null && sceneModel.useWorldAttachment) {
                transform.scale(-1.5F, 1.5F, 1.5F);
            }

            if (this.attachment != null) {
                Matrix4f attachmentXfrm = this.getAttachmentTransform(this.attachment, UI3DScene.allocMatrix4f());
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }

                transform.mul(attachmentXfrm);
                UI3DScene.releaseMatrix4f(attachmentXfrm);
            }

            if (this.autoRotate) {
                transform.rotateY(this.autoRotateAngle * (float) (Math.PI / 180.0));
            }

            return transform;
        }

        Matrix4f getGlobalTransform(Matrix4f transform) {
            this.getLocalTransform(transform);
            if (this.parent != null) {
                if (this.parentAttachment != null) {
                    Matrix4f attachmentXfrm = this.parent.getAttachmentTransform(this.parentAttachment, UI3DScene.allocMatrix4f());
                    attachmentXfrm.mul(transform, transform);
                    UI3DScene.releaseMatrix4f(attachmentXfrm);
                }

                Matrix4f parentXfrm = this.parent.getGlobalTransform(UI3DScene.allocMatrix4f());
                parentXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(parentXfrm);
            }

            if (this.parentVehiclePart != null) {
                Matrix4f parentXfrm = this.parentVehiclePart.getGlobalTransform(UI3DScene.allocMatrix4f());
                Matrix4f attachmentXfrm = this.getAttachmentTransform(this.parentVehiclePart.attachmentName, UI3DScene.allocMatrix4f());
                transform.mul(attachmentXfrm);
                UI3DScene.releaseMatrix4f(attachmentXfrm);
                parentXfrm.mul(transform, transform);
                UI3DScene.releaseMatrix4f(parentXfrm);
            }

            return transform;
        }

        Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
            transform.identity();
            return transform;
        }
    }

    private static class SceneObjectRenderData {
        UI3DScene.SceneObject object;
        final Matrix4f transform = new Matrix4f();
        private static final ObjectPool<UI3DScene.SceneObjectRenderData> s_pool = new ObjectPool<>(UI3DScene.SceneObjectRenderData::new);

        UI3DScene.SceneObjectRenderData init(UI3DScene.SceneObject sceneObject) {
            this.object = sceneObject;
            sceneObject.getGlobalTransform(this.transform);
            return this;
        }

        void release() {
            s_pool.release(this);
        }
    }

    private static final class ScenePlayer extends UI3DScene.SceneCharacter {
        ScenePlayer(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        void initAnimatedModel() {
            this.animatedModel.setAnimSetName("player-vehicle");
            this.animatedModel.setState("idle");
            this.animatedModel.setOutfitName("Naked", false, false);
            this.animatedModel.setVisual(new HumanVisual(this.animatedModel));
            this.animatedModel.getHumanVisual().setHairModel("Bald");
            this.animatedModel.getHumanVisual().setBeardModel("");
            this.animatedModel.getHumanVisual().setSkinTextureIndex(0);
            this.animatedModel.setAlpha(0.5F);
            this.animatedModel.setAnimate(false);
        }
    }

    private static final class ScenePolygon extends UI3DScene.SceneGeometry {
        UI3DScene.GridPlane plane = UI3DScene.GridPlane.XZ;
        final Vector3f extents = new Vector3f(1.0F);
        final ArrayList<Vector2f> points = new ArrayList<>();
        boolean editing = true;
        int highlightPointIndex = -1;
        final TFloatArrayList triangles = new TFloatArrayList();
        static final Rasterize s_rasterize = new Rasterize();

        ScenePolygon(UI3DScene scene, String id) {
            super(scene, id);
        }

        @Override
        public String getTypeName() {
            return "polygon";
        }

        @Override
        void initClone(UI3DScene.SceneObject clone) {
            UI3DScene.ScenePolygon polygon = (UI3DScene.ScenePolygon)clone;
            super.initClone(clone);
            polygon.plane = this.plane;
            polygon.extents.set(this.extents);

            for (Vector2f point : this.points) {
                polygon.points.add(new Vector2f(point));
            }
        }

        @Override
        UI3DScene.SceneObject clone(String id) {
            UI3DScene.ScenePolygon clone = new UI3DScene.ScenePolygon(this.scene, id);
            this.initClone(clone);
            return clone;
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            if (!this.scene.drawGeometry) {
                return null;
            } else {
                UI3DScene.ScenePolygonRenderData renderData = UI3DScene.ScenePolygonRenderData.s_pool.alloc();
                renderData.initPolygon(this);
                SpriteRenderer.instance.drawGeneric(renderData.drawer);
                return renderData;
            }
        }

        @Override
        Matrix4f getLocalTransform(Matrix4f transform) {
            super.getLocalTransform(transform);
            return transform;
        }

        @Override
        Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
            transform.identity();
            return transform;
        }

        @Override
        public boolean isPolygon() {
            return true;
        }

        @Override
        Matrix4f getOriginTransform(String hint, Matrix4f xfrm) {
            return this.getGlobalTransform(xfrm);
        }

        @Override
        float getNormalizedDepthAt(float tileX, float tileY) {
            Vector3f normal = UI3DScene.allocVector3f().set(0.0F, 0.0F, 1.0F);
            Matrix4f m = UI3DScene.allocMatrix4f()
                .rotationXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            m.transformDirection(normal);
            UI3DScene.releaseMatrix4f(m);
            float depth = TileGeometryUtils.getNormalizedDepthOnPlaneAt(tileX, tileY, this.translate, normal);
            UI3DScene.releaseVector3f(normal);
            return depth;
        }

        @Override
        TileGeometryFile.Geometry toGeometryFileObject() {
            TileGeometryFile.Polygon tilePolygon = new TileGeometryFile.Polygon();
            tilePolygon.plane = TileGeometryFile.Plane.valueOf(this.plane.name());
            tilePolygon.translate.set(this.translate);
            tilePolygon.rotate.set(this.rotate);

            for (Vector2f point : this.points) {
                tilePolygon.points.add(point.x);
                tilePolygon.points.add(point.y);
            }

            return tilePolygon;
        }

        int addPointOnEdge(float uiX, float uiY, float pointX, float pointY) {
            if (this.pickPoint(uiX, uiY, 5.0F) != -1) {
                return -1;
            } else {
                int edgeIndex = this.pickEdge(uiX, uiY, 10.0F);
                if (edgeIndex == -1) {
                    return -1;
                } else {
                    Vector2f newPoint = new Vector2f();
                    if (this.scene.polygonEditor.uiToPlane2D(pointX, pointY, newPoint)) {
                        this.points.add(edgeIndex + 1, newPoint);
                        this.triangulate();
                        return edgeIndex;
                    } else {
                        return -1;
                    }
                }
            }
        }

        int pickEdge(float uiX, float uiY, float maxDist) {
            float closestDist = Float.MAX_VALUE;
            int segment = -1;
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f p = UI3DScene.allocVector2f().set(uiX, uiY);
            Vector2f p1 = UI3DScene.allocVector2f();
            Vector2f p2 = UI3DScene.allocVector2f();

            for (int i = 0; i < this.points.size(); i++) {
                this.scene.polygonEditor.planeToUI(this.points.get(i), p1);
                this.scene.polygonEditor.planeToUI(this.points.get((i + 1) % this.points.size()), p2);
                float dist = this.distanceOfPointToLineSegment(p1, p2, p);
                if (dist < closestDist && dist < maxDist) {
                    closestDist = dist;
                    segment = i;
                }
            }

            UI3DScene.releaseVector2f(p);
            UI3DScene.releaseVector2f(p1);
            UI3DScene.releaseVector2f(p2);
            return segment;
        }

        float distanceOfPointToLineSegment(Vector2f p1, Vector2f p2, Vector2f p) {
            Vector2f n = UI3DScene.allocVector2f().set(p2).sub(p1);
            Vector2f pa = UI3DScene.allocVector2f().set(p1).sub(p);
            float c = n.dot(pa);
            if (c > 0.0F) {
                float result = pa.dot(pa);
                UI3DScene.releaseVector2f(n);
                UI3DScene.releaseVector2f(pa);
                return result;
            } else {
                Vector2f bp = UI3DScene.allocVector2f().set(p).sub(p2);
                if (n.dot(bp) > 0.0F) {
                    float result = bp.dot(bp);
                    UI3DScene.releaseVector2f(bp);
                    UI3DScene.releaseVector2f(n);
                    UI3DScene.releaseVector2f(pa);
                    return result;
                } else {
                    UI3DScene.releaseVector2f(bp);
                    Vector2f e = UI3DScene.allocVector2f().set(n).mul(c / n.dot(n));
                    pa.sub(e, e);
                    float result = e.dot(e);
                    UI3DScene.releaseVector2f(e);
                    UI3DScene.releaseVector2f(n);
                    UI3DScene.releaseVector2f(pa);
                    return result;
                }
            }
        }

        boolean isClockwise() {
            float sum = 0.0F;

            for (int i = 0; i < this.points.size(); i++) {
                float p1x = this.points.get(i).x;
                float p1y = this.points.get(i).y;
                float p2x = this.points.get((i + 1) % this.points.size()).x;
                float p2y = this.points.get((i + 1) % this.points.size()).y;
                sum += (p2x - p1x) * (p2y + p1y);
            }

            return sum > 0.0;
        }

        void triangulate() {
            this.triangles.clear();
            if (this.points.size() >= 3) {
                if (UI3DScene.clipper == null) {
                    UI3DScene.clipper = new Clipper();
                }

                UI3DScene.clipper.clear();
                ByteBuffer bb = ByteBuffer.allocateDirect(8 * this.points.size() * 3);
                if (this.isClockwise()) {
                    for (int i = this.points.size() - 1; i >= 0; i--) {
                        Vector2f point = this.points.get(i);
                        bb.putFloat(point.x);
                        bb.putFloat(point.y);
                    }
                } else {
                    for (int i = 0; i < this.points.size(); i++) {
                        Vector2f point = this.points.get(i);
                        bb.putFloat(point.x);
                        bb.putFloat(point.y);
                    }
                }

                UI3DScene.clipper.addPath(this.points.size(), bb, false);
                int numPolygons = UI3DScene.clipper.generatePolygons();
                if (numPolygons >= 1) {
                    bb.clear();
                    int numPoints = UI3DScene.clipper.triangulate(0, bb);
                    this.triangles.clear();

                    for (int i = 0; i < numPoints; i++) {
                        this.triangles.add(bb.getFloat());
                        this.triangles.add(bb.getFloat());
                    }
                }
            }
        }

        int pickPoint(float uiX, float uiY, float maxDist) {
            float closestDist = Float.MAX_VALUE;
            int closestIndex = -1;
            Vector2f uiPos = UI3DScene.allocVector2f();
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);

            for (int i = 0; i < this.points.size(); i++) {
                Vector2f point = this.points.get(i);
                this.scene.polygonEditor.planeToUI(point, uiPos);
                float dist = IsoUtils.DistanceTo2D(uiX, uiY, uiPos.x, uiPos.y);
                if (dist < maxDist && dist < closestDist) {
                    closestDist = dist;
                    closestIndex = i;
                }
            }

            UI3DScene.releaseVector2f(uiPos);
            return closestIndex;
        }

        void renderPoints() {
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f uiPos = UI3DScene.allocVector2f();

            for (int i = 0; i < this.points.size(); i++) {
                Vector2f point = this.points.get(i);
                this.scene.polygonEditor.planeToUI(point, uiPos);
                if (i == this.highlightPointIndex) {
                    this.scene.DrawTextureScaledCol(null, uiPos.x - 5.0, uiPos.y - 5.0, 10.0, 10.0, 0.0, 1.0, 0.0, 1.0);
                } else {
                    this.scene.DrawTextureScaledCol(null, uiPos.x - 5.0, uiPos.y - 5.0, 10.0, 10.0, 1.0, 1.0, 1.0, 1.0);
                }
            }

            UI3DScene.releaseVector2f(uiPos);
        }

        Vector2f uiToTile(Vector2f tileXY, float pixelSize, Vector2f uiPos, Vector2f tilePos) {
            float x = (uiPos.x - tileXY.x) / pixelSize;
            float y = (uiPos.y - tileXY.y) / pixelSize;
            return tilePos.set(x, y);
        }

        void rasterize(Rasterize.ICallback consumer) {
            Vector2f tileXY = UI3DScene.SceneDepthTexture.calculateTextureTopLeft(this.scene, 0.0F, 0.0F, 0.0F, UI3DScene.allocVector2f());
            float pixelSize = UI3DScene.SceneDepthTexture.calculatePixelSize(this.scene);
            this.scene.polygonEditor.setPlane(this.translate, this.rotate, this.plane);
            Vector2f point = UI3DScene.allocVector2f();
            Vector2f uiPos1 = UI3DScene.allocVector2f();
            Vector2f uiPos2 = UI3DScene.allocVector2f();
            Vector2f uiPos3 = UI3DScene.allocVector2f();

            for (int i = 0; i < this.triangles.size(); i += 6) {
                float x0 = this.triangles.get(i);
                float y0 = this.triangles.get(i + 1);
                float x1 = this.triangles.get(i + 2);
                float y1 = this.triangles.get(i + 3);
                float x2 = this.triangles.get(i + 4);
                float y2 = this.triangles.get(i + 5);
                this.scene.polygonEditor.planeToUI(point.set(x0, y0), uiPos1);
                this.scene.polygonEditor.planeToUI(point.set(x1, y1), uiPos2);
                this.scene.polygonEditor.planeToUI(point.set(x2, y2), uiPos3);
                this.uiToTile(tileXY, pixelSize, uiPos1, uiPos1);
                this.uiToTile(tileXY, pixelSize, uiPos2, uiPos2);
                this.uiToTile(tileXY, pixelSize, uiPos3, uiPos3);
                s_rasterize.scanTriangle(uiPos1.x, uiPos1.y, uiPos2.x, uiPos2.y, uiPos3.x, uiPos3.y, -1000, 1000, consumer);
            }

            UI3DScene.releaseVector2f(point);
            UI3DScene.releaseVector2f(uiPos1);
            UI3DScene.releaseVector2f(uiPos2);
            UI3DScene.releaseVector2f(uiPos3);
            UI3DScene.releaseVector2f(tileXY);
        }
    }

    private static final class ScenePolygonDrawer extends TextureDraw.GenericDrawer {
        UI3DScene.ScenePolygonRenderData renderData;

        public void init(UI3DScene.ScenePolygonRenderData renderData) {
            this.renderData = renderData;
        }

        @Override
        public void render() {
            UI3DScene scene = this.renderData.polygon.scene;
            UI3DScene.StateData stateData = scene.stateDataRender();
            GL11.glViewport(
                scene.getAbsoluteX().intValue(),
                Core.getInstance().getScreenHeight() - scene.getAbsoluteY().intValue() - scene.getHeight().intValue(),
                scene.getWidth().intValue(),
                scene.getHeight().intValue()
            );
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glDepthMask(false);
            GL11.glDepthFunc(513);
            UI3DScene.ScenePolygon scenePolygon = this.renderData.polygon;
            Vector3f extents = scenePolygon.extents;
            GL11.glPolygonMode(1032, 6914);
            UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            UI3DScene.vboRenderer.setLineWidth(2.0F);
            UI3DScene.vboRenderer.setMode(4);
            UI3DScene.vboRenderer.setDepthTest(true);
            GL20.glUseProgram(0);
            ShaderHelper.forgetCurrentlyBound();
            boolean drawGeometry = scene.drawGeometry;
            GL11.glColorMask(drawGeometry, drawGeometry, drawGeometry, drawGeometry);
            float alpha = 0.25F;
            ArrayList<Vector3f> tris = this.renderData.triangles;
            if (!tris.isEmpty()) {
                float r = 0.0F;
                float g = 1.0F;
                float b = 0.0F;

                for (int i = 0; i < tris.size(); i += 3) {
                    Vector3f p0 = tris.get(i);
                    Vector3f p1 = tris.get(i + 1);
                    Vector3f p2 = tris.get(i + 2);
                    UI3DScene.vboRenderer.addTriangle(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, r, g, b, 0.25F);
                }
            }

            UI3DScene.vboRenderer.endRun();
            UI3DScene.vboRenderer.startRun(UI3DScene.vboRenderer.formatPositionColor);
            UI3DScene.vboRenderer.setMode(1);
            UI3DScene.vboRenderer.setDepthTest(false);
            GL11.glDepthFunc(519);
            GL11.glColorMask(true, true, true, true);
            GL11.glPolygonMode(1032, 6914);

            for (int i = 0; i < this.renderData.points.size(); i++) {
                Vector3f scenePos1 = this.renderData.points.get(i);
                Vector3f scenePos2 = this.renderData.points.get((i + 1) % this.renderData.points.size());
                UI3DScene.vboRenderer
                    .addLine(scenePos1.x, scenePos1.y, scenePos1.z, scenePos2.x, scenePos2.y, scenePos2.z, 1.0F, 1.0F, 1.0F, scenePolygon.editing ? 1.0F : 0.5F);
            }

            UI3DScene.vboRenderer.endRun();
            UI3DScene.vboRenderer.flush();
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
            ShaderHelper.glUseProgramObjectARB(0);
            GLStateRenderThread.restore();
        }

        @Override
        public void postRender() {
        }
    }

    private static class ScenePolygonRenderData extends UI3DScene.SceneObjectRenderData {
        final UI3DScene.ScenePolygonDrawer drawer = new UI3DScene.ScenePolygonDrawer();
        UI3DScene.ScenePolygon polygon;
        final ArrayList<Vector3f> points = new ArrayList<>();
        final ArrayList<Vector3f> triangles = new ArrayList<>();
        private static final ObjectPool<UI3DScene.ScenePolygonRenderData> s_pool = new ObjectPool<>(UI3DScene.ScenePolygonRenderData::new);

        UI3DScene.SceneObjectRenderData initPolygon(UI3DScene.ScenePolygon scenePolygon) {
            this.init(scenePolygon);
            UI3DScene.PolygonEditor polygonEditor = scenePolygon.scene.polygonEditor;
            polygonEditor.setPlane(scenePolygon.translate, scenePolygon.rotate, scenePolygon.plane);
            this.points.clear();

            for (int i = 0; i < scenePolygon.points.size(); i++) {
                Vector2f pointOnPlane = scenePolygon.points.get(i);
                this.points.add(UI3DScene.allocVector3f().set(pointOnPlane.x, pointOnPlane.y, 0.0F));
            }

            this.triangles.clear();

            for (int i = 0; i < scenePolygon.triangles.size(); i += 2) {
                float x = scenePolygon.triangles.get(i);
                float y = scenePolygon.triangles.get(i + 1);
                this.triangles.add(UI3DScene.allocVector3f().set(x, y, 0.0F));
            }

            this.drawer.init(this);
            this.polygon = scenePolygon;
            return this;
        }

        @Override
        void release() {
            BaseVehicle.TL_vector3f_pool.get().releaseAll(this.points);
            this.points.clear();
            BaseVehicle.TL_vector3f_pool.get().releaseAll(this.triangles);
            this.triangles.clear();
            s_pool.release(this);
        }
    }

    private static final class SceneVehicle extends UI3DScene.SceneObject {
        String scriptName = "Base.ModernCar";
        VehicleScript script;
        final ArrayList<UI3DScene.SceneVehicleModelInfo> modelInfo = new ArrayList<>();
        String showBonesPartId;
        String showBonesModelId;
        boolean init;

        SceneVehicle(UI3DScene scene, String id) {
            super(scene, id);
            this.setScriptName("Base.ModernCar");
        }

        @Override
        UI3DScene.SceneObjectRenderData renderMain() {
            if (this.script == null) {
                return null;
            } else {
                if (!this.init) {
                    this.init = true;
                    String modelName = this.script.getModel().file;
                    Model model = ModelManager.instance.getLoadedModel(modelName);
                    if (model == null) {
                        return null;
                    }

                    UI3DScene.SceneVehicleModelInfo modelInfo = UI3DScene.SceneVehicleModelInfo.s_pool.alloc();
                    modelInfo.sceneVehicle = this;
                    modelInfo.part = null;
                    modelInfo.scriptModel = this.script.getModel();
                    modelInfo.modelScript = ScriptManager.instance.getModelScript(modelInfo.scriptModel.file);
                    modelInfo.wheelIndex = -1;
                    modelInfo.model = model;
                    modelInfo.tex = model.tex;
                    if (this.script.getSkinCount() > 0) {
                        modelInfo.tex = Texture.getSharedTexture("media/textures/" + this.script.getSkin(0).texture + ".png");
                    }

                    modelInfo.releaseAnimationPlayer();
                    modelInfo.animPlayer = null;
                    modelInfo.track = null;
                    this.modelInfo.add(modelInfo);

                    for (int i = 0; i < this.script.getPartCount(); i++) {
                        VehicleScript.Part scriptPart = this.script.getPart(i);
                        if (scriptPart.wheel == null) {
                            for (int j = 0; j < scriptPart.getModelCount(); j++) {
                                VehicleScript.Model scriptModel = scriptPart.getModel(j);
                                modelName = scriptModel.file;
                                if (modelName != null) {
                                    model = ModelManager.instance.getLoadedModel(modelName);
                                    if (model != null) {
                                        modelInfo = UI3DScene.SceneVehicleModelInfo.s_pool.alloc();
                                        modelInfo.sceneVehicle = this;
                                        modelInfo.part = scriptPart;
                                        modelInfo.scriptModel = scriptModel;
                                        modelInfo.modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
                                        modelInfo.wheelIndex = -1;
                                        modelInfo.model = model;
                                        modelInfo.tex = model.tex;
                                        modelInfo.releaseAnimationPlayer();
                                        modelInfo.animPlayer = null;
                                        modelInfo.track = null;
                                        this.modelInfo.add(modelInfo);
                                    }
                                }
                            }
                        }
                    }
                }

                if (this.modelInfo.isEmpty()) {
                    return null;
                } else {
                    UI3DScene.VehicleRenderData renderData = UI3DScene.VehicleRenderData.s_pool.alloc();
                    renderData.initVehicle(this);
                    UI3DScene.SetModelCamera setModelCamera = UI3DScene.s_SetModelCameraPool.alloc();
                    SpriteRenderer.instance.drawGeneric(setModelCamera.init(this.scene.vehicleSceneModelCamera, renderData));
                    SpriteRenderer.instance.drawGeneric(renderData.drawer);
                    return renderData;
                }
            }
        }

        @Override
        Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
            transform.identity();
            ModelAttachment attachment = this.script.getAttachmentById(attachmentName);
            if (attachment == null) {
                return transform;
            } else {
                ModelInstanceRenderData.makeAttachmentTransform(attachment, transform);
                if (attachment.getBone() != null) {
                    Matrix4f boneXfrm = this.getBoneMatrix(attachment.getBone(), UI3DScene.allocMatrix4f());
                    boneXfrm.mul(transform, transform);
                    UI3DScene.releaseMatrix4f(boneXfrm);
                }

                return transform;
            }
        }

        Matrix4f getBoneMatrix(String boneName, Matrix4f mat) {
            mat.identity();
            if (this.modelInfo.isEmpty()) {
                return mat;
            } else {
                UI3DScene.SceneVehicleModelInfo modelInfo = this.modelInfo.get(0);
                if (modelInfo == null) {
                    return mat;
                } else {
                    AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
                    if (animationPlayer == null) {
                        return mat;
                    } else if (animationPlayer.getModelTransformsCount() == 0) {
                        return mat;
                    } else {
                        SkinningBone bone = animationPlayer.getSkinningData().getBone(boneName);
                        if (bone == null) {
                            return mat;
                        } else {
                            mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
                            mat.transpose();
                            return mat;
                        }
                    }
                }
            }
        }

        Matrix4f getTransformForPart(String partId, String partModelId, String attachmentName, boolean bBoneOnly, Matrix4f transform) {
            transform.identity();
            VehicleScript.Part part = this.script.getPartById(partId);
            if (part == null) {
                return transform;
            } else {
                VehicleScript.Model partModel = part.getModelById(partModelId);
                if (partModel == null) {
                    return transform;
                } else if (partModel.getFile() == null) {
                    return transform;
                } else {
                    ModelScript modelScript = ScriptManager.instance.getModelScript(partModel.getFile());
                    if (modelScript == null) {
                        return transform;
                    } else {
                        ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
                        if (attachment == null) {
                            return transform;
                        } else {
                            transform.scale(1.0F / modelScript.scale);
                            if (attachment.getBone() != null) {
                                Matrix4f boneXfrm = this.getBoneMatrix(part, attachment, UI3DScene.allocMatrix4f());
                                boneXfrm.mul(transform, transform);
                                UI3DScene.releaseMatrix4f(boneXfrm);
                            }

                            if (bBoneOnly) {
                                return transform;
                            } else {
                                Matrix4f attachmentXfrm = ModelInstanceRenderData.makeAttachmentTransform(attachment, UI3DScene.allocMatrix4f());
                                transform.mul(attachmentXfrm);
                                UI3DScene.releaseMatrix4f(attachmentXfrm);
                                return transform;
                            }
                        }
                    }
                }
            }
        }

        Matrix4f getBoneMatrix(VehicleScript.Part part, ModelAttachment attachment, Matrix4f mat) {
            mat.identity();
            UI3DScene.SceneVehicleModelInfo modelInfo = this.getModelInfoForPart(part.getId());
            if (modelInfo == null) {
                return mat;
            } else {
                AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
                if (animationPlayer == null) {
                    return mat;
                } else if (animationPlayer.getModelTransformsCount() == 0) {
                    return mat;
                } else {
                    SkinningBone bone = animationPlayer.getSkinningData().getBone(attachment.getBone());
                    if (bone == null) {
                        return mat;
                    } else {
                        mat = PZMath.convertMatrix(animationPlayer.getModelTransformAt(bone.index), mat);
                        mat.transpose();
                        return mat;
                    }
                }
            }
        }

        int hitTestBone(int boneIndex, UI3DScene.Ray boneRay, UI3DScene.Ray cameraRay, AnimationPlayer animPlayer, Matrix4f modelMatrix, Vector2f out) {
            SkinningData skinningData = animPlayer.getSkinningData();
            int parentIndex = skinningData.skeletonHierarchy.get(boneIndex);
            if (parentIndex == -1) {
                return -1;
            } else {
                org.lwjgl.util.vector.Matrix4f boneMatrix = animPlayer.getModelTransformAt(parentIndex);
                boneRay.origin.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                modelMatrix.transformPosition(boneRay.origin);
                boneMatrix = animPlayer.getModelTransformAt(boneIndex);
                Vector3f nextBone = UI3DScene.allocVector3f();
                nextBone.set(boneMatrix.m03, boneMatrix.m13, boneMatrix.m23);
                modelMatrix.transformPosition(nextBone);
                boneRay.direction.set(nextBone).sub(boneRay.origin);
                float boneLength = boneRay.direction.length();
                boneRay.direction.normalize();
                UI3DScene.closest_distance_between_lines(cameraRay, boneRay);
                float camX = this.scene
                    .sceneToUIX(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float camY = this.scene
                    .sceneToUIY(
                        cameraRay.origin.x + cameraRay.direction.x * cameraRay.t,
                        cameraRay.origin.y + cameraRay.direction.y * cameraRay.t,
                        cameraRay.origin.z + cameraRay.direction.z * cameraRay.t
                    );
                float boneX = this.scene
                    .sceneToUIX(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                float boneY = this.scene
                    .sceneToUIY(
                        boneRay.origin.x + boneRay.direction.x * boneRay.t,
                        boneRay.origin.y + boneRay.direction.y * boneRay.t,
                        boneRay.origin.z + boneRay.direction.z * boneRay.t
                    );
                int hitBone = -1;
                float PICK_DIST = 10.0F;
                float dist = (float)Math.sqrt(Math.pow(boneX - camX, 2.0) + Math.pow(boneY - camY, 2.0));
                if (dist < 10.0F) {
                    if ((!(boneRay.t >= 0.0F) || !(boneRay.t <= 0.01F)) && (!(boneRay.t >= 0.09F) || !(boneRay.t <= 1.0F))) {
                        out.set(dist, 0.0F);
                    } else {
                        out.set(dist / 10.0F, 0.0F);
                    }

                    if (boneRay.t >= 0.0F && boneRay.t < boneLength * 0.5F) {
                        hitBone = parentIndex;
                    } else if (boneRay.t >= boneLength * 0.5F && boneRay.t < boneLength) {
                        hitBone = boneIndex;
                    }
                }

                UI3DScene.releaseVector3f(nextBone);
                return hitBone;
            }
        }

        String pickBone(VehicleScript.Part part, VehicleScript.Model model, float uiX, float uiY) {
            UI3DScene.SceneVehicleModelInfo modelInfo = this.getModelInfoForPart(part.getId(), model.getId());
            if (modelInfo == null) {
                return "";
            } else {
                AnimationPlayer animationPlayer = modelInfo.getAnimationPlayer();
                if (animationPlayer != null && animationPlayer.getModelTransformsCount() != 0) {
                    uiY = this.scene.screenHeight() - uiY;
                    UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                    Matrix4f modelXfrm = UI3DScene.allocMatrix4f();
                    this.getLocalTransform(modelXfrm);
                    UI3DScene.VehicleRenderData vrd = UI3DScene.VehicleRenderData.s_pool.alloc();
                    vrd.initVehicle(this);
                    UI3DScene.VehicleModelRenderData parentVMRD = vrd.partToRenderData.get(part.getId());
                    if (parentVMRD != null) {
                        modelXfrm.set(parentVMRD.xfrm);
                    }

                    vrd.release();
                    UI3DScene.Ray boneRay = UI3DScene.allocRay();
                    int hitBoneIndex = -1;
                    Vector2f dist = UI3DScene.allocVector2f();
                    float closestDist = Float.MAX_VALUE;

                    for (int i = 0; i < animationPlayer.getModelTransformsCount(); i++) {
                        int testBoneIndex = this.hitTestBone(i, boneRay, cameraRay, animationPlayer, modelXfrm, dist);
                        if (testBoneIndex != -1 && dist.x < closestDist) {
                            closestDist = dist.x;
                            hitBoneIndex = testBoneIndex;
                        }
                    }

                    UI3DScene.releaseVector2f(dist);
                    UI3DScene.releaseRay(boneRay);
                    UI3DScene.releaseRay(cameraRay);
                    UI3DScene.releaseMatrix4f(modelXfrm);
                    return hitBoneIndex == -1 ? "" : animationPlayer.getSkinningData().getBoneAt(hitBoneIndex).name;
                } else {
                    return "";
                }
            }
        }

        void setScriptName(String scriptName) {
            this.scriptName = scriptName;
            this.script = ScriptManager.instance.getVehicle(scriptName);
            UI3DScene.SceneVehicleModelInfo.s_pool.releaseAll(this.modelInfo);
            this.modelInfo.clear();
            this.init = false;
        }

        UI3DScene.SceneVehicleModelInfo getModelInfoForPart(String partId) {
            for (int i = 0; i < this.modelInfo.size(); i++) {
                UI3DScene.SceneVehicleModelInfo modelInfo = this.modelInfo.get(i);
                if (modelInfo.part != null && modelInfo.part.getId().equalsIgnoreCase(partId)) {
                    return modelInfo;
                }
            }

            return null;
        }

        UI3DScene.SceneVehicleModelInfo getModelInfoForPart(String partId, String partModelId) {
            for (int i = 0; i < this.modelInfo.size(); i++) {
                UI3DScene.SceneVehicleModelInfo modelInfo = this.modelInfo.get(i);
                if (modelInfo.part != null && modelInfo.part.getId().equalsIgnoreCase(partId) && modelInfo.scriptModel.getId().equalsIgnoreCase(partModelId)) {
                    return modelInfo;
                }
            }

            return null;
        }
    }

    private static final class SceneVehicleModelInfo {
        UI3DScene.SceneVehicle sceneVehicle;
        VehicleScript.Part part;
        VehicleScript.Model scriptModel;
        ModelScript modelScript;
        int wheelIndex;
        Model model;
        Texture tex;
        AnimationPlayer animPlayer;
        AnimationTrack track;
        private static final ObjectPool<UI3DScene.SceneVehicleModelInfo> s_pool = new ObjectPool<>(UI3DScene.SceneVehicleModelInfo::new);

        public AnimationPlayer getAnimationPlayer() {
            if (this.part != null && this.part.parent != null) {
                UI3DScene.SceneVehicleModelInfo modelInfoParent = this.sceneVehicle.getModelInfoForPart(this.part.parent);
                if (modelInfoParent != null) {
                    return modelInfoParent.getAnimationPlayer();
                }
            }

            String modelName = this.scriptModel.file;
            Model model = ModelManager.instance.getLoadedModel(modelName);
            if (model != null && !model.isStatic) {
                if (this.animPlayer != null && this.animPlayer.getModel() != model) {
                    this.animPlayer = Pool.tryRelease(this.animPlayer);
                }

                if (this.animPlayer == null) {
                    this.animPlayer = AnimationPlayer.alloc(model);
                }

                return this.animPlayer;
            } else {
                return null;
            }
        }

        public void releaseAnimationPlayer() {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }

        public void playPartAnim(String animId) {
            VehicleScript.Anim anim = this.part.getAnimById(animId);
            if (anim != null && !StringUtils.isNullOrWhitespace(anim.anim)) {
                AnimationPlayer animPlayer = this.getAnimationPlayer();
                if (animPlayer != null && animPlayer.isReady()) {
                    if (animPlayer.getMultiTrack().getIndexOfTrack(this.track) != -1) {
                        animPlayer.getMultiTrack().removeTrack(this.track);
                    }

                    this.track = null;
                    SkinningData skinningData = animPlayer.getSkinningData();
                    if (skinningData == null || skinningData.animationClips.containsKey(anim.anim)) {
                        AnimationTrack track = animPlayer.play(anim.anim, anim.loop);
                        this.track = track;
                        if (track != null) {
                            track.setBlendWeight(1.0F);
                            track.setSpeedDelta(anim.rate);
                            track.isPlaying = anim.animate;
                            track.reverse = anim.reverse;
                            if (!this.modelScript.boneWeights.isEmpty()) {
                                track.setBoneWeights(this.modelScript.boneWeights);
                                track.initBoneWeights(skinningData);
                            }

                            if (this.part.window != null) {
                                float openDelta = 0.0F;
                                track.setCurrentTimeValue(track.getDuration() * 0.0F);
                            }
                        }
                    }
                }
            }
        }

        protected void updateAnimationPlayer() {
            AnimationPlayer animPlayer = this.getAnimationPlayer();
            if (animPlayer != null && animPlayer.isReady()) {
                AnimationMultiTrack multiTrack = animPlayer.getMultiTrack();
                float del = 0.016666668F;
                del *= 0.8F;
                del *= GameTime.instance.getUnmoddedMultiplier();
                animPlayer.Update(del);

                for (int i = 0; i < multiTrack.getTrackCount(); i++) {
                    AnimationTrack track = multiTrack.getTracks().get(i);
                    if (track.isPlaying && track.isFinished()) {
                        multiTrack.removeTrackAt(i);
                        i--;
                    }
                }

                if (this.part != null) {
                    if (this.track != null && multiTrack.getIndexOfTrack(this.track) == -1) {
                        this.track = null;
                    }

                    if (this.track != null) {
                        if (this.part.window != null) {
                            AnimationTrack track = this.track;
                            float openDelta = 0.0F;
                            track.setCurrentTimeValue(track.getDuration() * 0.0F);
                        }
                    } else {
                        if (this.part.door != null) {
                            boolean bOpen = false;
                            this.playPartAnim("Closed");
                        }

                        if (this.part.window != null) {
                            this.playPartAnim("ClosedToOpen");
                        }
                    }
                }
            }
        }

        void release() {
            s_pool.release(this);
        }
    }

    private static final class SetModelCamera extends TextureDraw.GenericDrawer {
        UI3DScene.SceneModelCamera camera;
        UI3DScene.SceneObjectRenderData renderData;

        UI3DScene.SetModelCamera init(UI3DScene.SceneModelCamera camera, UI3DScene.SceneObjectRenderData renderData) {
            this.camera = camera;
            this.renderData = renderData;
            return this;
        }

        @Override
        public void render() {
            this.camera.renderData = this.renderData;
            ModelCamera.instance = this.camera;
        }

        @Override
        public void postRender() {
            UI3DScene.s_SetModelCameraPool.release(this);
        }
    }

    private static final class SpriteGridTextureMaskDrawer extends TextureDraw.GenericDrawer {
        UI3DScene scene;
        IsoSprite sprite;
        float sx;
        float sy;
        float sx2;
        float sy2;
        float pixelSize;
        float r;
        float g;
        float b;
        float a;

        @Override
        public void render() {
            IsoSpriteGrid spriteGrid = this.sprite.getSpriteGrid();
            int spriteGridIndex = spriteGrid.getSpriteIndex(this.sprite);
            int gridPosX = spriteGrid.getSpriteGridPosX(this.sprite);
            int gridPosY = spriteGrid.getSpriteGridPosY(this.sprite);
            int gridPosZ = spriteGrid.getSpriteGridPosZ(this.sprite);

            for (int i = 0; i < spriteGrid.getSpriteCount(); i++) {
                if (i != spriteGridIndex) {
                    IsoSprite sprite2 = spriteGrid.getSpriteFromIndex(i);
                    if (sprite2 != null) {
                        Texture texture = sprite2.getTextureForCurrentFrame(IsoDirections.N);
                        if (texture != null && texture.getMask() != null) {
                            int gridPosX2 = spriteGrid.getSpriteGridPosX(sprite2);
                            int gridPosY2 = spriteGrid.getSpriteGridPosY(sprite2);
                            int gridPosZ2 = spriteGrid.getSpriteGridPosZ(sprite2);
                            int dx = gridPosX2 - gridPosX;
                            int dy = gridPosY2 - gridPosY;
                            int dz = gridPosZ2 - gridPosZ;
                            this.render(
                                texture,
                                this.scene.sceneToUIX(dx + 0.5F, dz * 3 * 0.8164967F, dy + 0.5F) - 64.0F * this.pixelSize,
                                this.scene.sceneToUIY(dx + 0.5F, dz * 3 * 0.8164967F, dy + 0.5F) - 256.0F * this.pixelSize
                            );
                        }
                    }
                }
            }
        }

        void render(Texture texture, float sx, float sy) {
            Mask mask = texture.getMask();
            if (mask != null) {
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.startRun(VBORenderer.getInstance().formatPositionColor);
                float z = 0.0F;

                for (int y = 0; y < 256; y++) {
                    for (int x = 0; x < 128; x++) {
                        if (texture.isMaskSet(x, y)) {
                            float x1 = sx + x * this.pixelSize;
                            float y1 = sy + y * this.pixelSize;
                            float x2 = sx + (x + 1) * this.pixelSize;
                            float y2 = sy + (y + 1) * this.pixelSize;
                            vbor.addLine(x1, y1, 0.0F, x2, y1, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x2, y1, 0.0F, x2, y2, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x2, y2, 0.0F, x1, y2, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x1, y2, 0.0F, x1, y1, 0.0F, this.r, this.g, this.b, this.a);
                        }
                    }
                }

                vbor.endRun();
                vbor.flush();
            }
        }
    }

    private static final class StateData {
        final Matrix4f projection = new Matrix4f();
        final Matrix4f modelView = new Matrix4f();
        int zoom;
        UI3DScene.GridPlaneDrawer gridPlaneDrawer;
        UI3DScene.OverlaysDrawer overlaysDrawer;
        final ArrayList<UI3DScene.SceneObjectRenderData> objectData = new ArrayList<>();
        UI3DScene.Gizmo gizmo;
        final Vector3f gizmoTranslate = new Vector3f();
        final Vector3f gizmoRotate = new Vector3f();
        final Matrix4f gizmoParentTransform = new Matrix4f();
        final Matrix4f gizmoOriginTransform = new Matrix4f();
        final Matrix4f gizmoChildTransform = new Matrix4f();
        final Matrix4f gizmoChildAttachmentTransform = new Matrix4f();
        final Matrix4f gizmoChildAttachmentTransformInv = new Matrix4f();
        final Matrix4f gizmoTransform = new Matrix4f();
        boolean hasGizmoOrigin;
        boolean gizmoOriginIsGeometry;
        boolean selectedAttachmentIsChildAttachment;
        UI3DScene.Axis gizmoAxis = UI3DScene.Axis.None;
        final UI3DScene.TranslateGizmoRenderData translateGizmoRenderData = new UI3DScene.TranslateGizmoRenderData();
        final ArrayList<UI3DScene.PositionRotation> axes = new ArrayList<>();
        final ArrayList<UI3DScene.AABB> aabb = new ArrayList<>();
        final ArrayList<UI3DScene.Box3D> box3d = new ArrayList<>();
        final ArrayList<UI3DScene.PhysicsMesh> physicsMesh = new ArrayList<>();

        private float zoomMult() {
            return (float)Math.exp(this.zoom * 0.2F) * 160.0F / Math.max(1.82F, 1.0F);
        }
    }

    private static final class TextureMaskDrawer extends TextureDraw.GenericDrawer {
        UI3DScene scene;
        Texture texture;
        float sx;
        float sy;
        float sx2;
        float sy2;
        float pixelSize;
        float r;
        float g;
        float b;
        float a;

        @Override
        public void render() {
            Mask mask = this.texture.getMask();
            if (mask != null) {
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.startRun(VBORenderer.getInstance().formatPositionColor);
                float z = 0.0F;

                for (int y = 0; y < 256; y++) {
                    for (int x = 0; x < 128; x++) {
                        if (this.texture.isMaskSet(x, y)) {
                            float x1 = this.sx + x * this.pixelSize;
                            float y1 = this.sy + y * this.pixelSize;
                            float x2 = this.sx + (x + 1) * this.pixelSize;
                            float y2 = this.sy + (y + 1) * this.pixelSize;
                            vbor.addLine(x1, y1, 0.0F, x2, y1, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x2, y1, 0.0F, x2, y2, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x2, y2, 0.0F, x1, y2, 0.0F, this.r, this.g, this.b, this.a);
                            vbor.addLine(x1, y2, 0.0F, x1, y1, 0.0F, this.r, this.g, this.b, this.a);
                        }
                    }
                }

                vbor.endRun();
                vbor.flush();
            }
        }
    }

    private static enum TransformMode {
        Global,
        Local;
    }

    private final class TranslateGizmo extends UI3DScene.Gizmo {
        final Matrix4f startXfrm;
        final Matrix4f startInvXfrm;
        final Vector3f startPos;
        final Vector3f currentPos;
        UI3DScene.Axis trackAxis;
        boolean doubleAxis;
        final PartialDisk disk;

        private TranslateGizmo() {
            Objects.requireNonNull(UI3DScene.this);
            super();
            this.startXfrm = new Matrix4f();
            this.startInvXfrm = new Matrix4f();
            this.startPos = new Vector3f();
            this.currentPos = new Vector3f();
            this.trackAxis = UI3DScene.Axis.None;
            this.disk = new PartialDisk();
        }

        @Override
        UI3DScene.Axis hitTest(float uiX, float uiY) {
            if (!this.visible) {
                return UI3DScene.Axis.None;
            } else {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
                UI3DScene.this.setModelViewProjection(stateData);
                UI3DScene.this.setGizmoTransforms(stateData);
                Matrix4f gizmoXfrm = UI3DScene.allocMatrix4f();
                gizmoXfrm.set(stateData.gizmoParentTransform);
                gizmoXfrm.mul(stateData.gizmoOriginTransform);
                gizmoXfrm.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                gizmoXfrm.mul(stateData.gizmoTransform);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    gizmoXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                uiY = UI3DScene.this.screenHeight() - uiY;
                UI3DScene.Ray camera_ray = UI3DScene.this.getCameraRay(uiX, uiY, UI3DScene.allocRay());
                UI3DScene.Ray axis = UI3DScene.allocRay();
                gizmoXfrm.transformPosition(axis.origin.set(0.0F, 0.0F, 0.0F));
                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float LENGTH = 0.5F * SCALE;
                float THICKNESS = 0.05F * SCALE;
                float OFFSET = 0.1F * SCALE;
                gizmoXfrm.transformDirection(axis.direction.set(1.0F, 0.0F, 0.0F)).normalize();
                float distX = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float x_t = axis.t;
                float cam_x_t = camera_ray.t;
                if (!UI3DScene.this.gizmoAxisVisibleX || x_t < OFFSET || x_t >= OFFSET + LENGTH) {
                    x_t = Float.MAX_VALUE;
                    distX = Float.MAX_VALUE;
                }

                float xdot = axis.direction.dot(camera_ray.direction);
                stateData.translateGizmoRenderData.hideX = !UI3DScene.this.gizmoAxisVisibleX || Math.abs(xdot) > 0.9F;
                gizmoXfrm.transformDirection(axis.direction.set(0.0F, 1.0F, 0.0F)).normalize();
                float distY = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float y_t = axis.t;
                float cam_y_t = camera_ray.t;
                if (!UI3DScene.this.gizmoAxisVisibleY || y_t < OFFSET || y_t >= OFFSET + LENGTH) {
                    y_t = Float.MAX_VALUE;
                    distY = Float.MAX_VALUE;
                }

                float ydot = axis.direction.dot(camera_ray.direction);
                stateData.translateGizmoRenderData.hideY = !UI3DScene.this.gizmoAxisVisibleY || Math.abs(ydot) > 0.9F;
                gizmoXfrm.transformDirection(axis.direction.set(0.0F, 0.0F, 1.0F)).normalize();
                float distZ = UI3DScene.closest_distance_between_lines(axis, camera_ray);
                float z_t = axis.t;
                float cam_z_t = camera_ray.t;
                if (!UI3DScene.this.gizmoAxisVisibleZ || z_t < OFFSET || z_t >= OFFSET + LENGTH) {
                    z_t = Float.MAX_VALUE;
                    distZ = Float.MAX_VALUE;
                }

                float zdot = axis.direction.dot(camera_ray.direction);
                stateData.translateGizmoRenderData.hideZ = !UI3DScene.this.gizmoAxisVisibleZ || Math.abs(zdot) > 0.9F;
                UI3DScene.Axis doubleAxis = UI3DScene.Axis.None;
                if (this.doubleAxis) {
                    float offset = THICKNESS * 1.5F;
                    float inner = offset + OFFSET;
                    float outer = inner + LENGTH / 2.0F;
                    Vector3f pointOnPlane3D = UI3DScene.allocVector3f();
                    Vector2f pointOnPlane2D = UI3DScene.allocVector2f();
                    if (UI3DScene.this.gizmoOrigin instanceof UI3DScene.SceneCharacter) {
                        gizmoXfrm.scale(0.6666667F);
                    }

                    if (this.getPointOnDualAxis(uiX, -(uiY - UI3DScene.this.screenHeight()), UI3DScene.Axis.XY, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                        && pointOnPlane2D.x >= 0.0F
                        && pointOnPlane2D.y >= 0.0F
                        && pointOnPlane2D.length() >= inner
                        && pointOnPlane2D.length() < outer) {
                        doubleAxis = UI3DScene.Axis.XY;
                    }

                    if (this.getPointOnDualAxis(uiX, -(uiY - UI3DScene.this.screenHeight()), UI3DScene.Axis.XZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                        && pointOnPlane2D.x >= 0.0F
                        && pointOnPlane2D.y >= 0.0F
                        && pointOnPlane2D.length() >= inner
                        && pointOnPlane2D.length() < outer) {
                        doubleAxis = UI3DScene.Axis.XZ;
                    }

                    if (this.getPointOnDualAxis(uiX, -(uiY - UI3DScene.this.screenHeight()), UI3DScene.Axis.YZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                        && pointOnPlane2D.x >= 0.0F
                        && pointOnPlane2D.y >= 0.0F
                        && pointOnPlane2D.length() >= inner
                        && pointOnPlane2D.length() < outer) {
                        doubleAxis = UI3DScene.Axis.YZ;
                    }

                    UI3DScene.releaseVector3f(pointOnPlane3D);
                    UI3DScene.releaseVector2f(pointOnPlane2D);
                }

                UI3DScene.releaseRay(axis);
                UI3DScene.releaseRay(camera_ray);
                UI3DScene.releaseMatrix4f(gizmoXfrm);
                if (doubleAxis != UI3DScene.Axis.None) {
                    return doubleAxis;
                } else if (x_t >= OFFSET && x_t < OFFSET + LENGTH && distX < distY && distX < distZ) {
                    return distX <= THICKNESS / 2.0F ? UI3DScene.Axis.X : UI3DScene.Axis.None;
                } else if (y_t >= OFFSET && y_t < OFFSET + LENGTH && distY < distX && distY < distZ) {
                    return distY <= THICKNESS / 2.0F ? UI3DScene.Axis.Y : UI3DScene.Axis.None;
                } else if (z_t >= OFFSET && z_t < OFFSET + LENGTH && distZ < distX && distZ < distY) {
                    return distZ <= THICKNESS / 2.0F ? UI3DScene.Axis.Z : UI3DScene.Axis.None;
                } else {
                    return UI3DScene.Axis.None;
                }
            }
        }

        @Override
        void startTracking(float uiX, float uiY, UI3DScene.Axis axis) {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
            UI3DScene.this.setModelViewProjection(stateData);
            UI3DScene.this.setGizmoTransforms(stateData);
            this.startXfrm.set(stateData.gizmoParentTransform);
            this.startXfrm.mul(stateData.gizmoOriginTransform);
            this.startXfrm.mul(stateData.gizmoChildTransform);
            if (!stateData.selectedAttachmentIsChildAttachment) {
                this.startXfrm.mul(stateData.gizmoTransform);
            }

            if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                this.startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            this.startInvXfrm.set(this.startXfrm);
            this.startInvXfrm.invert();
            this.trackAxis = axis;
            this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        }

        @Override
        void updateTracking(float uiX, float uiY) {
            Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, UI3DScene.allocVector3f());
            if (this.currentPos.equals(pos)) {
                UI3DScene.releaseVector3f(pos);
            } else {
                this.currentPos.set(pos);
                UI3DScene.releaseVector3f(pos);
                UI3DScene.StateData stateData = UI3DScene.this.stateDataMain();
                UI3DScene.this.setModelViewProjection(stateData);
                UI3DScene.this.setGizmoTransforms(stateData);
                Vector3f delta = new Vector3f(this.currentPos).sub(this.startPos);
                if (UI3DScene.this.selectedAttachment == null && UI3DScene.this.gizmoChild == null && !stateData.gizmoOriginIsGeometry) {
                    delta.set(this.currentPos).sub(this.startPos);
                } else if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                    Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                    Matrix4f m = UI3DScene.allocMatrix4f();
                    m.set(stateData.gizmoParentTransform);
                    m.mul(stateData.gizmoOriginTransform);
                    if (!stateData.selectedAttachmentIsChildAttachment) {
                        m.mul(stateData.gizmoChildTransform);
                    }

                    m.invert();
                    m.transformPosition(vs);
                    m.transformPosition(vc);
                    UI3DScene.releaseMatrix4f(m);
                    delta.set(vc).sub(vs);
                    UI3DScene.releaseVector3f(vs);
                    UI3DScene.releaseVector3f(vc);
                } else {
                    Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, UI3DScene.allocVector3f());
                    Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, UI3DScene.allocVector3f());
                    Matrix4f m = UI3DScene.allocMatrix4f();
                    m.set(stateData.gizmoTransform);
                    m.transformPosition(vs);
                    m.transformPosition(vc);
                    UI3DScene.releaseMatrix4f(m);
                    delta.set(vc).sub(vs);
                    UI3DScene.releaseVector3f(vs);
                    UI3DScene.releaseVector3f(vc);
                }

                LuaManager.caller.pcall(UIManager.getDefaultThread(), UI3DScene.this.getTable().rawget("onGizmoChanged"), UI3DScene.this.table, delta);
            }
        }

        @Override
        void stopTracking() {
            this.trackAxis = UI3DScene.Axis.None;
        }

        @Override
        void render() {
            if (this.visible) {
                UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
                Matrix4f matrix4f = UI3DScene.allocMatrix4f();
                matrix4f.set(stateData.gizmoParentTransform);
                matrix4f.mul(stateData.gizmoOriginTransform);
                matrix4f.mul(stateData.gizmoChildTransform);
                if (stateData.selectedAttachmentIsChildAttachment) {
                    matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
                }

                matrix4f.mul(stateData.gizmoTransform);
                Vector3f scale = matrix4f.getScale(UI3DScene.allocVector3f());
                matrix4f.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
                UI3DScene.releaseVector3f(scale);
                if (UI3DScene.this.transformMode == UI3DScene.TransformMode.Global) {
                    matrix4f.setRotationXYZ(0.0F, 0.0F, 0.0F);
                }

                stateData.modelView.mul(matrix4f, matrix4f);
                float SCALE = UI3DScene.this.gizmoScale / stateData.zoomMult() * 1000.0F;
                float THICKNESS = 0.05F * SCALE;
                float LENGTH = 0.5F * SCALE;
                float OFFSET = 0.1F * SCALE;
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.cmdPushAndLoadMatrix(5888, matrix4f);
                if (!stateData.translateGizmoRenderData.hideX) {
                    boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.X || this.trackAxis == UI3DScene.Axis.X;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.XY || this.trackAxis == UI3DScene.Axis.XY;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.XZ || this.trackAxis == UI3DScene.Axis.XZ;
                    float r = highlight ? 1.0F : 0.5F;
                    float g = 0.0F;
                    float b = 0.0F;
                    float a = 1.0F;
                    matrix4f.rotation((float) (Math.PI / 2), 0.0F, 1.0F, 0.0F);
                    matrix4f.translate(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, r, 0.0F, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, r, 0.0F, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                if (!stateData.translateGizmoRenderData.hideY) {
                    boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.Y || this.trackAxis == UI3DScene.Axis.Y;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.XY || this.trackAxis == UI3DScene.Axis.XY;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.YZ || this.trackAxis == UI3DScene.Axis.YZ;
                    float r = 0.0F;
                    float g = highlight ? 1.0F : 0.5F;
                    float b = 0.0F;
                    float a = 1.0F;
                    matrix4f.rotation((float) (-Math.PI / 2), 1.0F, 0.0F, 0.0F);
                    matrix4f.translate(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, g, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, 0.0F, g, 0.0F, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                if (!stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.Z || this.trackAxis == UI3DScene.Axis.Z;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.XZ || this.trackAxis == UI3DScene.Axis.XZ;
                    highlight |= stateData.gizmoAxis == UI3DScene.Axis.YZ || this.trackAxis == UI3DScene.Axis.YZ;
                    float r = 0.0F;
                    float g = 0.0F;
                    float b = highlight ? 1.0F : 0.5F;
                    float a = 1.0F;
                    matrix4f.translation(0.0F, 0.0F, OFFSET);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, 0.0F, b, 1.0F);
                    vbor.cmdPopMatrix(5888);
                    matrix4f.translate(0.0F, 0.0F, LENGTH);
                    vbor.cmdPushAndMultMatrix(5888, matrix4f);
                    vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, 0.0F, 0.0F, b, 1.0F);
                    vbor.cmdPopMatrix(5888);
                }

                if (this.doubleAxis) {
                    float offset = THICKNESS * 1.5F;
                    if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideY) {
                        boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.XY || this.trackAxis == UI3DScene.Axis.XY;
                        GL11.glColor4f(1.0F, 1.0F, 0.0F, highlight ? 1.0F : 0.5F);
                        GL11.glTranslatef(offset, offset, 0.0F);
                        this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 0.0F, 90.0F);
                        GL11.glTranslatef(-offset, -offset, 0.0F);
                    }

                    if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideZ) {
                        boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.XZ || this.trackAxis == UI3DScene.Axis.XZ;
                        GL11.glColor4f(1.0F, 0.0F, 1.0F, highlight ? 1.0F : 0.5F);
                        GL11.glTranslatef(offset, 0.0F, offset);
                        GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
                        this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 90.0F, 90.0F);
                        GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                        GL11.glTranslatef(-offset, 0.0F, -offset);
                    }

                    if (!stateData.translateGizmoRenderData.hideY && !stateData.translateGizmoRenderData.hideZ) {
                        boolean highlight = stateData.gizmoAxis == UI3DScene.Axis.YZ || this.trackAxis == UI3DScene.Axis.YZ;
                        GL11.glColor4f(0.0F, 1.0F, 1.0F, highlight ? 1.0F : 0.5F);
                        GL11.glTranslatef(0.0F, offset, offset);
                        GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
                        this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 0.0F, 90.0F);
                        GL11.glRotated(90.0, 0.0, 1.0, 0.0);
                        GL11.glTranslatef(0.0F, -offset, -offset);
                    }
                }

                UI3DScene.releaseMatrix4f(matrix4f);
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
                this.renderLineToOrigin();
                GLStateRenderThread.restore();
            }
        }
    }

    private static final class TranslateGizmoRenderData {
        boolean hideX;
        boolean hideY;
        boolean hideZ;
    }

    private static final class VehicleDrawer extends TextureDraw.GenericDrawer {
        UI3DScene.SceneVehicle vehicle;
        UI3DScene.VehicleRenderData renderData;
        boolean rendered;
        final float[] fzeroes = new float[16];
        final Vector3f paintColor = new Vector3f(0.0F, 0.5F, 0.5F);
        static final Matrix4f IDENTITY = new Matrix4f();

        public void init(UI3DScene.SceneVehicle sceneVehicle, UI3DScene.VehicleRenderData renderData) {
            this.vehicle = sceneVehicle;
            this.renderData = renderData;
            this.rendered = false;
        }

        @Override
        public void render() {
            for (int i = 0; i < this.renderData.models.size(); i++) {
                GL11.glPushAttrib(1048575);
                GL11.glPushClientAttrib(-1);
                this.render(i);
                GL11.glPopAttrib();
                GL11.glPopClientAttrib();
                Texture.lastTextureID = -1;
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                SpriteRenderer.ringBuffer.restoreVbos = true;
            }

            GL11.glPushAttrib(1048575);
            GL11.glPushClientAttrib(-1);

            for (int i = 0; i < this.renderData.models.size(); i++) {
                UI3DScene.VehicleModelRenderData modelRenderData = this.renderData.models.get(i);
                this.renderData.transform.set(modelRenderData.xfrm);
                ModelCamera.instance.Begin();
                this.renderSkeleton(modelRenderData);
                ModelCamera.instance.End();
            }

            GL11.glPopAttrib();
            GL11.glPopClientAttrib();
        }

        private void render(int modelIndex) {
            UI3DScene.VehicleModelRenderData modelRenderData = this.renderData.models.get(modelIndex);
            this.renderData.transform.set(modelRenderData.xfrm);
            ModelCamera.instance.Begin();
            Model model = modelRenderData.model;
            boolean bStatic = model.isStatic;
            if (Core.debug && DebugOptions.instance.model.render.wireframe.getValue()) {
                GL11.glPolygonMode(1032, 6913);
                GL11.glEnable(2848);
                GL11.glLineWidth(0.75F);
                Shader Effect = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", bStatic, false);
                if (Effect != null) {
                    Effect.Start();
                    if (model.isStatic) {
                        Effect.setTransformMatrix(IDENTITY.identity(), false);
                    } else {
                        Effect.setMatrixPalette(modelRenderData.matrixPalette, true);
                    }

                    model.mesh.Draw(Effect);
                    Effect.End();
                }

                GL11.glDisable(2848);
                ModelCamera.instance.End();
            } else {
                Shader Effect = model.effect;
                if (Effect != null && Effect.isVehicleShader()) {
                    GL11.glDepthFunc(513);
                    GL11.glDepthMask(true);
                    GL11.glDepthRange(0.0, 1.0);
                    GL11.glEnable(2929);
                    GL11.glColor3f(1.0F, 1.0F, 1.0F);
                    Effect.Start();
                    Texture tex = modelRenderData.tex;
                    if (tex == null && modelIndex > 0) {
                        tex = this.renderData.models.get(0).tex;
                    }

                    if (tex != null) {
                        Effect.setTexture(tex, "Texture0", 0);
                        GL11.glTexEnvi(8960, 8704, 7681);
                        if (this.vehicle.script.getSkinCount() > 0 && this.vehicle.script.getSkin(0).textureMask != null) {
                            Texture textureMask = Texture.getSharedTexture("media/textures/" + this.vehicle.script.getSkin(0).textureMask + ".png");
                            Effect.setTexture(textureMask, "TextureMask", 2);
                            GL11.glTexEnvi(8960, 8704, 7681);
                        }
                    }

                    Effect.setDepthBias(0.0F);
                    Effect.setAmbient(1.0F);
                    Effect.setLightingAmount(1.0F);
                    Effect.setHueShift(0.0F);
                    Effect.setTint(1.0F, 1.0F, 1.0F);
                    Effect.setAlpha(1.0F);

                    for (int i = 0; i < 5; i++) {
                        Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                    }

                    Effect.setTextureUninstall1(this.fzeroes);
                    Effect.setTextureUninstall2(this.fzeroes);
                    Effect.setTextureLightsEnables2(this.fzeroes);
                    Effect.setTextureDamage1Enables1(this.fzeroes);
                    Effect.setTextureDamage1Enables2(this.fzeroes);
                    Effect.setTextureDamage2Enables1(this.fzeroes);
                    Effect.setTextureDamage2Enables2(this.fzeroes);
                    Effect.setMatrixBlood1(this.fzeroes, this.fzeroes);
                    Effect.setMatrixBlood2(this.fzeroes, this.fzeroes);
                    Effect.setTextureRustA(0.0F);
                    Effect.setTexturePainColor(this.paintColor, 1.0F);
                    if (model.isStatic) {
                        Effect.setTransformMatrix(IDENTITY.identity(), false);
                    } else {
                        Effect.setMatrixPalette(modelRenderData.matrixPalette, true);
                    }

                    Effect.setTargetDepth(0.5F);
                    model.mesh.Draw(Effect);
                    Effect.End();
                } else if (Effect != null && model.mesh != null && model.mesh.isReady()) {
                    GL11.glDepthFunc(513);
                    GL11.glDepthMask(true);
                    GL11.glDepthRange(0.0, 1.0);
                    GL11.glEnable(2929);
                    GL11.glColor3f(1.0F, 1.0F, 1.0F);
                    Effect.Start();
                    if (model.tex != null) {
                        Effect.setTexture(model.tex, "Texture", 0);
                    }

                    Effect.setDepthBias(0.0F);
                    Effect.setAmbient(1.0F);
                    Effect.setLightingAmount(1.0F);
                    Effect.setHueShift(0.0F);
                    Effect.setTint(1.0F, 1.0F, 1.0F);
                    Effect.setAlpha(1.0F);

                    for (int i = 0; i < 5; i++) {
                        Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                    }

                    Effect.setTransformMatrix(IDENTITY.identity(), false);
                    Effect.setTargetDepth(0.5F);
                    model.mesh.Draw(Effect);
                    Effect.End();
                }

                ModelCamera.instance.End();
                this.rendered = true;
            }
        }

        private void renderSkeleton(UI3DScene.VehicleModelRenderData renderData) {
            TFloatArrayList boneCoords = renderData.boneCoords;
            if (!boneCoords.isEmpty()) {
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.startRun(vbor.formatPositionColor);
                vbor.setDepthTest(false);
                vbor.setLineWidth(1.0F);
                vbor.setMode(1);

                for (int i = 0; i < boneCoords.size(); i += 6) {
                    Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                    vbor.addElement();
                    vbor.setColor(c.r, c.g, c.b, 1.0F);
                    float x = boneCoords.get(i);
                    float y = boneCoords.get(i + 1);
                    float z = boneCoords.get(i + 2);
                    vbor.setVertex(x, y, z);
                    vbor.addElement();
                    vbor.setColor(c.r, c.g, c.b, 1.0F);
                    x = boneCoords.get(i + 3);
                    y = boneCoords.get(i + 4);
                    z = boneCoords.get(i + 5);
                    vbor.setVertex(x, y, z);
                }

                vbor.endRun();
                vbor.flush();
                GL11.glColor3f(1.0F, 1.0F, 1.0F);
                GL11.glEnable(2929);
            }
        }

        @Override
        public void postRender() {
        }
    }

    private static class VehicleModelRenderData {
        public Model model;
        public Texture tex;
        public final Matrix4f xfrm = new Matrix4f();
        public FloatBuffer matrixPalette;
        private final TFloatArrayList boneCoords = new TFloatArrayList();
        private final ArrayList<org.lwjgl.util.vector.Matrix4f> boneMatrices = new ArrayList<>();
        private static final ObjectPool<UI3DScene.VehicleModelRenderData> s_pool = new ObjectPool<>(UI3DScene.VehicleModelRenderData::new);

        void initSkeleton(UI3DScene.SceneVehicleModelInfo modelInfo) {
            this.boneCoords.clear();
            this.initSkeleton(modelInfo.getAnimationPlayer());
        }

        private void initSkeleton(AnimationPlayer animPlayer) {
            if (animPlayer != null && animPlayer.hasSkinningData() && !animPlayer.isBoneTransformsNeedFirstFrame()) {
                Integer translationBoneIndex = animPlayer.getSkinningData().boneIndices.get("Translation_Data");

                for (int i = 0; i < animPlayer.getModelTransformsCount(); i++) {
                    if (translationBoneIndex == null || i != translationBoneIndex) {
                        int parentIdx = animPlayer.getSkinningData().skeletonHierarchy.get(i);
                        if (parentIdx >= 0) {
                            this.initSkeleton(animPlayer, i);
                            this.initSkeleton(animPlayer, parentIdx);
                        }
                    }
                }
            }
        }

        private void initSkeleton(AnimationPlayer animPlayer, int boneIndex) {
            org.lwjgl.util.vector.Matrix4f boneTransform = animPlayer.getModelTransformAt(boneIndex);
            float x = boneTransform.m03;
            float y = boneTransform.m13;
            float z = boneTransform.m23;
            this.boneCoords.add(x);
            this.boneCoords.add(y);
            this.boneCoords.add(z);
        }

        void release() {
            s_pool.release(this);
        }
    }

    private static class VehicleRenderData extends UI3DScene.SceneObjectRenderData {
        final ArrayList<UI3DScene.VehicleModelRenderData> models = new ArrayList<>();
        final HashMap<String, UI3DScene.VehicleModelRenderData> partToRenderData = new HashMap<>();
        final UI3DScene.VehicleDrawer drawer = new UI3DScene.VehicleDrawer();
        private static final ObjectPool<UI3DScene.VehicleRenderData> s_pool = new ObjectPool<>(UI3DScene.VehicleRenderData::new);

        UI3DScene.SceneObjectRenderData initVehicle(UI3DScene.SceneVehicle sceneVehicle) {
            this.init(sceneVehicle);
            UI3DScene.VehicleModelRenderData.s_pool.release(this.models);
            this.models.clear();
            VehicleScript script = sceneVehicle.script;
            if (script.getModel() == null) {
                return null;
            } else {
                this.initVehicleModel(sceneVehicle);
                float scale = script.getModelScale();
                Vector3f modelOffset = script.getModel().getOffset();
                int rightToLeftHand = 1;
                Matrix4f vehicleTransform = UI3DScene.allocMatrix4f();
                vehicleTransform.translationRotateScale(modelOffset.x * 1.0F, modelOffset.y, modelOffset.z, 0.0F, 0.0F, 0.0F, 1.0F, scale);
                this.transform.mul(vehicleTransform, vehicleTransform);

                for (int i = 0; i < script.getPartCount(); i++) {
                    VehicleScript.Part scriptPart = script.getPart(i);
                    if (scriptPart.wheel == null) {
                        this.initPartModels(sceneVehicle, scriptPart, vehicleTransform);
                    } else {
                        this.initWheelModel(sceneVehicle, scriptPart, vehicleTransform);
                    }
                }

                UI3DScene.releaseMatrix4f(vehicleTransform);
                this.drawer.init(sceneVehicle, this);
                return this;
            }
        }

        private void initVehicleModel(UI3DScene.SceneVehicle sceneVehicle) {
            UI3DScene.SceneVehicleModelInfo modelInfo = sceneVehicle.modelInfo.get(0);
            UI3DScene.VehicleModelRenderData modelRenderData = UI3DScene.VehicleModelRenderData.s_pool.alloc();
            modelRenderData.model = modelInfo.model;
            modelRenderData.tex = modelInfo.tex;
            modelRenderData.boneCoords.clear();
            this.models.add(modelRenderData);
            VehicleScript script = sceneVehicle.script;
            float scale = script.getModelScale();
            float scale2 = 1.0F;
            ModelScript modelScript = modelInfo.modelScript;
            if (modelScript != null && modelScript.scale != 1.0F) {
                scale2 = modelScript.scale;
            }

            float scaleInvertX = 1.0F;
            if (modelScript != null) {
                scaleInvertX = modelScript.invertX ? -1.0F : 1.0F;
            }

            scaleInvertX *= -1.0F;
            Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
            int rightToLeftHand = 1;
            Matrix4f renderTransform = modelRenderData.xfrm;
            Vector3f modelRotate = script.getModel().getRotate();
            modelRotQ.rotationXYZ(
                modelRotate.x * (float) (Math.PI / 180.0), modelRotate.y * (float) (Math.PI / 180.0), modelRotate.z * (float) (Math.PI / 180.0)
            );
            Vector3f modelOffset = script.getModel().getOffset();
            renderTransform.translationRotateScale(
                modelOffset.x * 1.0F,
                modelOffset.y,
                modelOffset.z,
                modelRotQ.x,
                modelRotQ.y,
                modelRotQ.z,
                modelRotQ.w,
                scale * scale2 * scaleInvertX,
                scale * scale2,
                scale * scale2
            );
            ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, modelRenderData.model.mesh);
            this.transform.mul(renderTransform, renderTransform);
            UI3DScene.releaseQuaternionf(modelRotQ);
        }

        private void initWheelModel(UI3DScene.SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, Matrix4f vehicleTransform) {
            VehicleScript script = sceneVehicle.script;
            float scale = script.getModelScale();
            VehicleScript.Wheel scriptWheel = script.getWheelById(scriptPart.wheel);
            if (scriptWheel != null && !scriptPart.models.isEmpty()) {
                VehicleScript.Model scriptModel = scriptPart.models.get(0);
                Vector3f modelOffset = scriptModel.getOffset();
                Vector3f modelRotate = scriptModel.getRotate();
                Model model = ModelManager.instance.getLoadedModel(scriptModel.file);
                if (model != null) {
                    UI3DScene.VehicleModelRenderData modelRenderData = UI3DScene.VehicleModelRenderData.s_pool.alloc();
                    modelRenderData.model = model;
                    modelRenderData.tex = model.tex;
                    modelRenderData.boneCoords.clear();
                    this.models.add(modelRenderData);
                    float scale1 = scriptModel.scale;
                    float scale2 = 1.0F;
                    float scaleInvertX = 1.0F;
                    ModelScript modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
                    if (modelScript != null) {
                        scale2 = modelScript.scale;
                        scaleInvertX = modelScript.invertX ? -1.0F : 1.0F;
                    }

                    Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
                    modelRotQ.rotationXYZ(
                        modelRotate.x * (float) (Math.PI / 180.0), modelRotate.y * (float) (Math.PI / 180.0), modelRotate.z * (float) (Math.PI / 180.0)
                    );
                    int rightToLeftHand = 1;
                    Matrix4f renderTransform = modelRenderData.xfrm;
                    renderTransform.translation(scriptWheel.offset.x / scale * 1.0F, scriptWheel.offset.y / scale, scriptWheel.offset.z / scale);
                    Matrix4f matrix4f = UI3DScene.allocMatrix4f();
                    matrix4f.translationRotateScale(
                        modelOffset.x * 1.0F,
                        modelOffset.y,
                        modelOffset.z,
                        modelRotQ.x,
                        modelRotQ.y,
                        modelRotQ.z,
                        modelRotQ.w,
                        scale1 * scale2 * scaleInvertX,
                        scale1 * scale2,
                        scale1 * scale2
                    );
                    renderTransform.mul(matrix4f);
                    UI3DScene.releaseMatrix4f(matrix4f);
                    vehicleTransform.mul(renderTransform, renderTransform);
                    ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, model.mesh);
                    UI3DScene.releaseQuaternionf(modelRotQ);
                }
            }
        }

        private void initPartModels(UI3DScene.SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, Matrix4f vehicleTransform) {
            for (int i = 0; i < scriptPart.getModelCount(); i++) {
                VehicleScript.Model scriptModel = scriptPart.getModel(i);
                if (scriptPart.parent != null && scriptModel.attachmentNameParent != null) {
                    UI3DScene.VehicleModelRenderData renderData = this.partToRenderData.get(scriptPart.parent);
                    if (renderData != null) {
                        this.initChildPartModel(sceneVehicle, renderData, scriptPart, scriptModel);
                    }
                } else {
                    this.initPartModel(sceneVehicle, scriptPart, scriptModel, vehicleTransform);
                }
            }
        }

        private void initPartModel(
            UI3DScene.SceneVehicle sceneVehicle, VehicleScript.Part scriptPart, VehicleScript.Model scriptModel, Matrix4f vehicleTransform
        ) {
            UI3DScene.SceneVehicleModelInfo modelInfo = sceneVehicle.getModelInfoForPart(scriptPart.getId());
            if (modelInfo != null) {
                Vector3f modelOffset = scriptModel.getOffset();
                Vector3f modelRotate = scriptModel.getRotate();
                Model model = modelInfo.model;
                if (model != null) {
                    UI3DScene.VehicleModelRenderData modelRenderData = UI3DScene.VehicleModelRenderData.s_pool.alloc();
                    modelRenderData.model = model;
                    modelRenderData.tex = model.tex == null ? sceneVehicle.modelInfo.get(0).tex : model.tex;
                    modelRenderData.boneCoords.clear();
                    AnimationPlayer animationPlayer = model.isStatic ? null : modelInfo.getAnimationPlayer();
                    if (animationPlayer != null) {
                        modelInfo.updateAnimationPlayer();
                        SkinningData skinningData = (SkinningData)model.tag;
                        if (Core.debug && skinningData == null) {
                            DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
                        }

                        org.lwjgl.util.vector.Matrix4f[] skinTransforms = animationPlayer.getSkinTransforms(skinningData);
                        int matrixFloats = 16;
                        if (modelRenderData.matrixPalette == null || modelRenderData.matrixPalette.capacity() < skinTransforms.length * 16) {
                            modelRenderData.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
                        }

                        modelRenderData.matrixPalette.clear();

                        for (int i = 0; i < skinTransforms.length; i++) {
                            skinTransforms[i].store(modelRenderData.matrixPalette);
                        }

                        modelRenderData.matrixPalette.flip();
                        if (scriptPart.getId().equalsIgnoreCase(sceneVehicle.showBonesPartId)
                            && scriptModel.getId().equalsIgnoreCase(sceneVehicle.showBonesModelId)) {
                            modelRenderData.initSkeleton(modelInfo);
                        }

                        this.partToRenderData.put(scriptPart.getId(), modelRenderData);
                    }

                    this.models.add(modelRenderData);
                    float scale1 = scriptModel.scale;
                    float scale2 = 1.0F;
                    float scaleInvertX = 1.0F;
                    ModelScript modelScript = ScriptManager.instance.getModelScript(scriptModel.file);
                    if (modelScript != null) {
                        scale2 = modelScript.scale;
                        scaleInvertX = modelScript.invertX ? -1.0F : 1.0F;
                    }

                    scaleInvertX *= -1.0F;
                    Quaternionf modelRotQ = UI3DScene.allocQuaternionf();
                    modelRotQ.rotationXYZ(
                        modelRotate.x * (float) (Math.PI / 180.0), modelRotate.y * (float) (Math.PI / 180.0), modelRotate.z * (float) (Math.PI / 180.0)
                    );
                    int rightToLeftHand = 1;
                    Matrix4f renderTransform = modelRenderData.xfrm;
                    renderTransform.translationRotateScale(
                        modelOffset.x * 1.0F,
                        modelOffset.y,
                        modelOffset.z,
                        modelRotQ.x,
                        modelRotQ.y,
                        modelRotQ.z,
                        modelRotQ.w,
                        scale1 * scale2 * scaleInvertX,
                        scale1 * scale2,
                        scale1 * scale2
                    );
                    vehicleTransform.mul(renderTransform, renderTransform);
                    ModelInstanceRenderData.postMultiplyMeshTransform(renderTransform, model.mesh);
                    UI3DScene.releaseQuaternionf(modelRotQ);
                }
            }
        }

        void initChildPartModel(
            UI3DScene.SceneVehicle sceneVehicle,
            UI3DScene.VehicleModelRenderData parentRenderData,
            VehicleScript.Part scriptPart,
            VehicleScript.Model scriptModel
        ) {
            UI3DScene.SceneVehicleModelInfo modelInfo = sceneVehicle.getModelInfoForPart(scriptPart.getId());
            if (modelInfo != null) {
                Model model = modelInfo.model;
                if (model != null) {
                    UI3DScene.VehicleModelRenderData modelRenderData = UI3DScene.VehicleModelRenderData.s_pool.alloc();
                    modelRenderData.model = model;
                    modelRenderData.tex = model.tex == null ? sceneVehicle.modelInfo.get(0).tex : model.tex;
                    modelRenderData.boneCoords.clear();
                    UI3DScene.SceneVehicleModelInfo parentModelInfo = sceneVehicle.getModelInfoForPart(scriptPart.parent);
                    Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();
                    this.initTransform(
                        sceneVehicle,
                        parentModelInfo.getAnimationPlayer(),
                        parentModelInfo.modelScript,
                        modelInfo.modelScript,
                        scriptModel.attachmentNameParent,
                        scriptModel.attachmentNameSelf,
                        attachmentXfrm
                    );
                    parentRenderData.xfrm.mul(attachmentXfrm, modelRenderData.xfrm);
                    float scale1 = scriptModel.scale;
                    float scale2 = modelInfo.modelScript.scale;
                    boolean bIgnoreVehicleScale = scriptModel.ignoreVehicleScale;
                    float scale3 = bIgnoreVehicleScale ? 1.5F / sceneVehicle.script.getModelScale() : 1.0F;
                    modelRenderData.xfrm.scale(scale1 * scale2 * scale3);
                    ModelInstanceRenderData.postMultiplyMeshTransform(modelRenderData.xfrm, model.mesh);
                    UI3DScene.releaseMatrix4f(attachmentXfrm);
                    this.models.add(modelRenderData);
                }
            }
        }

        void initTransform(
            UI3DScene.SceneVehicle sceneVehicle,
            AnimationPlayer animationPlayer,
            ModelScript parentModelScript,
            ModelScript modelScript,
            String m_attachmentNameParent,
            String m_attachmentNameSelf,
            Matrix4f m_transform
        ) {
            m_transform.identity();
            Matrix4f attachmentXfrm = UI3DScene.allocMatrix4f();
            ModelAttachment parentAttachment = parentModelScript.getAttachmentById(m_attachmentNameParent);
            if (parentAttachment == null) {
                parentAttachment = sceneVehicle.script.getAttachmentById(m_attachmentNameParent);
            }

            if (parentAttachment != null) {
                ModelInstanceRenderData.makeBoneTransform(animationPlayer, parentAttachment.getBone(), m_transform);
                m_transform.scale(1.0F / parentModelScript.scale);
                ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
                m_transform.mul(attachmentXfrm);
            }

            ModelAttachment selfAttachment = modelScript.getAttachmentById(m_attachmentNameSelf);
            if (selfAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }

                m_transform.mul(attachmentXfrm);
            }

            UI3DScene.releaseMatrix4f(attachmentXfrm);
        }

        @Override
        void release() {
            s_pool.release(this);
        }
    }

    private final class VehicleSceneModelCamera extends UI3DScene.SceneModelCamera {
        private VehicleSceneModelCamera() {
            Objects.requireNonNull(UI3DScene.this);
            super();
        }

        @Override
        public void Begin() {
            UI3DScene.StateData stateData = UI3DScene.this.stateDataRender();
            GL11.glViewport(
                UI3DScene.this.getAbsoluteX().intValue(),
                Core.getInstance().getScreenHeight() - UI3DScene.this.getAbsoluteY().intValue() - UI3DScene.this.getHeight().intValue(),
                UI3DScene.this.getWidth().intValue(),
                UI3DScene.this.getHeight().intValue()
            );
            PZGLUtil.pushAndLoadMatrix(5889, stateData.projection);
            Matrix4f matrix4f = UI3DScene.allocMatrix4f();
            matrix4f.set(stateData.modelView);
            matrix4f.mul(this.renderData.transform);
            PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
            UI3DScene.releaseMatrix4f(matrix4f);
            GL11.glDepthRange(0.0, 1.0);
            GL11.glDepthMask(true);
        }

        @Override
        public void End() {
            PZGLUtil.popMatrix(5889);
            PZGLUtil.popMatrix(5888);
        }
    }

    private static enum View {
        Left,
        Right,
        Top,
        Bottom,
        Front,
        Back,
        UserDefined;
    }
}
