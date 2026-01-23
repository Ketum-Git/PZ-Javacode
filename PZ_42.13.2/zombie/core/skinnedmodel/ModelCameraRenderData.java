// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.ai.states.PlayerGetUpState;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.popman.ObjectPool;
import zombie.seating.SeatingManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelCameraRenderData extends TextureDraw.GenericDrawer {
    private ModelCamera camera;
    private float angle;
    private boolean useWorldIso;
    private float x;
    private float y;
    private float z;
    private boolean inVehicle;
    public static final ObjectPool<ModelCameraRenderData> s_pool = new ObjectPool<>(ModelCameraRenderData::new);

    public ModelCameraRenderData init(ModelCamera camera, ModelManager.ModelSlot modelSlot) {
        IsoMovingObject object = modelSlot.model.object;
        IsoGameCharacter character = Type.tryCastTo(object, IsoGameCharacter.class);
        this.camera = camera;
        this.x = object.getX();
        this.y = object.getY();
        this.z = object.getZ();
        if (character == null) {
            this.angle = 0.0F;
            this.inVehicle = false;
            this.useWorldIso = !BaseVehicle.renderToTexture;
        } else {
            this.inVehicle = character.isSeatedInVehicle();
            if (this.inVehicle) {
                this.angle = 0.0F;
                BaseVehicle vehicle = character.getVehicle();
                this.x = vehicle.getX();
                this.y = vehicle.getY();
                this.z = vehicle.getZ();
            } else {
                this.angle = character.getAnimationPlayer().getRenderedAngle();
                this.adjustForSittingOnFurniture(character);
            }

            this.useWorldIso = true;
        }

        return this;
    }

    private void adjustForSittingOnFurniture(IsoGameCharacter character) {
        if (character.isSittingOnFurniture()) {
            IsoObject isoObject = character.getSitOnFurnitureObject();
            if (isoObject != null && isoObject.getSprite() != null && isoObject.getSprite().tilesetName != null) {
                IsoDirections sitDir = character.getSitOnFurnitureDirection();
                Vector3f xln = SeatingManager.getInstance().getTranslation(isoObject.getSprite(), sitDir.name(), new Vector3f());
                float sxf = xln.x;
                float syf = xln.y;
                float szf = xln.z;
                float lerp = 1.0F;
                String SitOnFurnitureDirection = character.getVariableString("SitOnFurnitureDirection");
                String animNodeName = "SitOnFurniture" + SitOnFurnitureDirection;
                if (character.isCurrentState(PlayerGetUpState.instance())) {
                    String suffix = SitOnFurnitureDirection;
                    if (character.getVariableBoolean("getUpQuick")) {
                        suffix = SitOnFurnitureDirection + "Quick";
                    }

                    animNodeName = "fromSitOnFurniture" + suffix;
                    lerp = 0.0F;
                }

                float animFraction = SeatingManager.getInstance().getAnimationTrackFraction(character, animNodeName);
                if (animFraction < 0.0F && !character.getVariableBoolean("SitOnFurnitureStarted")) {
                    lerp = 1.0F - lerp;
                }

                if (animFraction >= 0.0F) {
                    if (character.isCurrentState(PlayerGetUpState.instance())) {
                        float lerpStart = 0.48F;
                        float lerpEnd = 0.63F;
                        if (animFraction >= 0.63F) {
                            lerp = 1.0F;
                        } else if (animFraction >= 0.48F) {
                            lerp = (animFraction - 0.48F) / 0.15F;
                        } else {
                            lerp = 0.0F;
                        }

                        lerp = 1.0F - lerp;
                    } else {
                        float lerpStart = 0.27F;
                        float lerpEnd = 0.43F;
                        if (animFraction >= 0.27F && animFraction <= 0.43F) {
                            lerp = (animFraction - 0.27F) / 0.16F;
                        } else if (animFraction >= 0.43F) {
                            lerp = 1.0F;
                        } else {
                            lerp = 0.0F;
                        }
                    }
                }

                this.z = PZMath.lerp(this.z, isoObject.square.z + szf / 2.44949F, lerp);
            }
        }
    }

    public ModelCameraRenderData init(ModelCamera camera, float useangle, boolean useWorldIso, float tx, float ty, float tz, boolean bInVehicle) {
        this.camera = camera;
        this.angle = useangle;
        this.useWorldIso = useWorldIso;
        this.x = tx;
        this.y = ty;
        this.z = tz;
        this.inVehicle = bInVehicle;
        return this;
    }

    @Override
    public void render() {
        this.camera.useAngle = this.angle;
        this.camera.useWorldIso = this.useWorldIso;
        this.camera.x = this.x;
        this.camera.y = this.y;
        this.camera.z = this.z;
        this.camera.inVehicle = this.inVehicle;
        ModelCamera.instance = this.camera;
    }

    @Override
    public void postRender() {
        s_pool.release(this);
    }
}
