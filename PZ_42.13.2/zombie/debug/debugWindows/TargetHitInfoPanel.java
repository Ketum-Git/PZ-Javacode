// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.combat.CombatConfig;
import zombie.combat.CombatConfigKey;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.RagdollBodyPart;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.network.fields.hit.HitInfo;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.vehicles.BaseVehicle;

public class TargetHitInfoPanel extends PZDebugWindow {
    private IsoGameCharacter isoGameCharacter;
    private final CombatManager combatManager = CombatManager.getInstance();
    private CombatConfig combatConfig;
    public PZArrayList<HitInfo> hitInfoList = new PZArrayList<>(HitInfo.class, 8);

    public void setIsoGameCharacter(IsoGameCharacter isoGameCharacter) {
        this.isoGameCharacter = isoGameCharacter;
    }

    @Override
    public String getTitle() {
        return "Target Hit Info";
    }

    private void displayBaseVehicleRow(BaseVehicle baseVehicle, HandWeapon weapon, HitInfo hitInfo) {
        BallisticsController ballisticsController = this.isoGameCharacter.getBallisticsController();
        float max = weapon.getMaxSightRange(this.isoGameCharacter);
        float min = weapon.getMinSightRange(this.isoGameCharacter);
        int id = -1;
        int columnIndex = 0;
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(columnIndex++);
        int var13 = baseVehicle.getId();
        ImGui.text(Integer.toString(var13));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text("Vehicle");
        ImGui.tableSetColumnIndex(columnIndex++);
        float dist = PZMath.sqrt(hitInfo.distSq);
        float chance = 0.0F;
        if (dist < min) {
            if (dist > this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
                chance -= (dist - min) * 3.0F;
            }
        } else if (dist >= max) {
            chance -= (dist - max) * 3.0F;
        } else {
            float diff = (max - min) * 0.5F;
            chance += this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS) * (1.0F - Math.abs((dist - min - diff) / diff));
        }

        ImGui.text(Float.toString(PZMath.roundFloat(dist, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(chance, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(baseVehicle.getCurrentSpeedKmHour(), 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        float lightLevel = 0.0F;
        IsoGridSquare isoGridSquare = baseVehicle.getCurrentSquare();
        ImGui.text("null");
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(
            Float.toString(
                PZMath.max(
                    0.0F,
                    this.combatConfig.get(CombatConfigKey.LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY)
                        * (1.0F - 0.0F / this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD))
                )
            )
        );
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text("NA");
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(chance));
    }

    private void displayRow(IsoMovingObject obj, HandWeapon weapon, HitInfo hitInfo) {
        BallisticsController ballisticsController = this.isoGameCharacter.getBallisticsController();
        float max = weapon.getMaxSightRange(this.isoGameCharacter);
        float min = weapon.getMinSightRange(this.isoGameCharacter);
        IsoGameCharacter hitIsoGameCharacter = (IsoGameCharacter)hitInfo.getObject();
        int id = -1;
        int columnIndex = 0;
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(columnIndex++);
        id = obj.getID();
        ImGui.text(Integer.toString(id));
        ImGui.tableSetColumnIndex(columnIndex++);
        if (hitIsoGameCharacter != null) {
            hitIsoGameCharacter.setHitFromBehind(this.isoGameCharacter.isBehind(hitIsoGameCharacter));
            boolean fromBehind = hitIsoGameCharacter.isHitFromBehind();
            ImGui.text(fromBehind ? "Behind" : "Forward");
        } else {
            ImGui.text("NA");
        }

        ImGui.tableSetColumnIndex(columnIndex++);
        float dist = PZMath.sqrt(hitInfo.distSq);
        float chance = 0.0F;
        if (dist < min) {
            if (dist > this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
                chance -= (dist - min) * 3.0F;
            }
        } else if (dist >= max) {
            chance -= (dist - max) * 3.0F;
        } else {
            float diff = (max - min) * 0.5F;
            chance += this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS) * (1.0F - Math.abs((dist - min - diff) / diff));
        }

        ImGui.text(Float.toString(PZMath.roundFloat(dist, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(chance, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        IsoGameCharacter gameCharacter = (IsoGameCharacter)hitInfo.getObject();
        if (gameCharacter != null) {
            ImGui.text(Float.toString(PZMath.roundFloat(gameCharacter.getMovementSpeed() * GameTime.getInstance().getInvMultiplier(), 6)));
        } else {
            ImGui.text("NA");
        }

        int playerNum = ((IsoPlayer)this.isoGameCharacter).getPlayerNum();
        ImGui.tableSetColumnIndex(columnIndex++);
        IsoGridSquare isoGridSquare = obj.getCurrentSquare();
        float lightLevel = 0.0F;
        if (isoGridSquare != null) {
            ImGui.text(Float.toString(PZMath.roundFloat(isoGridSquare.getLightLevel(playerNum), 3)));
            lightLevel = obj.getCurrentSquare().getLightLevel(playerNum);
        } else {
            ImGui.text("null");
        }

        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(
            Float.toString(
                PZMath.max(
                    0.0F,
                    this.combatConfig.get(CombatConfigKey.LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY)
                        * (1.0F - lightLevel / this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD))
                )
            )
        );
        ImGui.tableSetColumnIndex(columnIndex++);
        if (gameCharacter != null) {
            ImGui.text(Float.toString(PZMath.roundFloat(gameCharacter.getHealth(), 3)));
        } else {
            ImGui.text("NA");
        }

        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(chance));
        if (ballisticsController != null) {
            boolean isCameraTarget = ballisticsController.isCachedCameraTarget(id);
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(Boolean.toString(isCameraTarget));
            int targetedBodyPart = RagdollBodyPart.BODYPART_COUNT.ordinal();
            if (isCameraTarget) {
                targetedBodyPart = ballisticsController.getCachedTargetedBodyPart(id);
                String bodyPartName = RagdollBodyPart.values()[targetedBodyPart].name();
                ImGui.tableSetColumnIndex(columnIndex++);
                ImGui.text(bodyPartName);
            }
        }
    }

    private void displayIsoZombieRow(IsoGameCharacter obj, HandWeapon weapon, HitInfo hitInfo) {
        BallisticsController ballisticsController = this.isoGameCharacter.getBallisticsController();
        float max = weapon.getMaxSightRange(this.isoGameCharacter);
        float min = weapon.getMinSightRange(this.isoGameCharacter);
        int id = -1;
        int columnIndex = 0;
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(columnIndex++);
        IsoZombie zombie = (IsoZombie)hitInfo.getObject();
        if (zombie == null) {
            ImGui.text("ID");
        } else {
            id = zombie.getID();
            ImGui.text(Integer.toString(id));
        }

        ImGui.tableSetColumnIndex(columnIndex++);
        zombie.setHitFromBehind(this.isoGameCharacter.isBehind(zombie));
        boolean fromBehind = zombie.isHitFromBehind();
        ImGui.text(fromBehind ? "Behind" : "Forward");
        ImGui.tableSetColumnIndex(columnIndex++);
        float dist = PZMath.sqrt(hitInfo.distSq);
        float chance = 0.0F;
        if (dist < min) {
            if (dist > this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
                chance -= (dist - min) * 3.0F;
            }
        } else if (dist >= max) {
            chance -= (dist - max) * 3.0F;
        } else {
            float diff = (max - min) * 0.5F;
            chance += this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS) * (1.0F - Math.abs((dist - min - diff) / diff));
        }

        ImGui.text(Float.toString(PZMath.roundFloat(dist, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(chance, 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(obj.getMovementSpeed() * GameTime.getInstance().getInvMultiplier(), 6)));
        int playerNum = ((IsoPlayer)this.isoGameCharacter).getPlayerNum();
        ImGui.tableSetColumnIndex(columnIndex++);
        IsoGridSquare isoGridSquare = obj.getCurrentSquare();
        float lightLevel = 0.0F;
        if (isoGridSquare != null) {
            ImGui.text(Float.toString(PZMath.roundFloat(isoGridSquare.getLightLevel(playerNum), 3)));
            lightLevel = obj.getCurrentSquare().getLightLevel(playerNum);
        } else {
            ImGui.text("null");
        }

        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(
            Float.toString(
                PZMath.max(
                    0.0F,
                    this.combatConfig.get(CombatConfigKey.LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY)
                        * (1.0F - lightLevel / this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD))
                )
            )
        );
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(PZMath.roundFloat(obj.getHealth(), 3)));
        ImGui.tableSetColumnIndex(columnIndex++);
        ImGui.text(Float.toString(chance));
        if (ballisticsController != null) {
            boolean isCameraTarget = ballisticsController.isCachedCameraTarget(id);
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(Boolean.toString(isCameraTarget));
            int targetedBodyPart = RagdollBodyPart.BODYPART_COUNT.ordinal();
            if (isCameraTarget) {
                targetedBodyPart = ballisticsController.getCachedTargetedBodyPart(id);
                String bodyPartName = RagdollBodyPart.values()[targetedBodyPart].name();
                ImGui.tableSetColumnIndex(columnIndex++);
                ImGui.text(bodyPartName);
            }
        }
    }

    @Override
    protected void doWindowContents() {
        if (this.isoGameCharacter != null) {
            this.combatConfig = this.combatManager.getCombatConfig();
            BallisticsController ballisticsController = this.isoGameCharacter.getBallisticsController();
            if (ballisticsController != null) {
                if (ImGui.beginTable("Ballistics Controller Targets", 4, 1984)) {
                    ImGui.tableSetupColumn("id");
                    ImGui.tableSetupColumn("X");
                    ImGui.tableSetupColumn("Y");
                    ImGui.tableSetupColumn("Z");
                    ImGui.tableHeadersRow();
                }

                int numberOfTargets = ballisticsController.getCachedNumberOfTargets();
                float[] ballisticsTargets = ballisticsController.getCachedBallisticsTargets();
                int arrayIndex = 0;

                for (int i = 0; i < numberOfTargets; i++) {
                    int columnIndex = 0;
                    arrayIndex = i * 4;
                    int id = (int)ballisticsTargets[arrayIndex++];
                    float x = ballisticsTargets[arrayIndex++];
                    float y = ballisticsTargets[arrayIndex++] / 2.44949F;
                    float z = ballisticsTargets[arrayIndex++];
                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(columnIndex++);
                    ImGui.text(Integer.toString(id));
                    ImGui.tableSetColumnIndex(columnIndex++);
                    ImGui.text(Float.toString(x));
                    ImGui.tableSetColumnIndex(columnIndex++);
                    ImGui.text(Float.toString(y));
                    ImGui.tableSetColumnIndex(columnIndex++);
                    ImGui.text(Float.toString(z));
                }

                ImGui.endTable();
            }

            HandWeapon weapon = this.isoGameCharacter.getUseHandWeapon();
            if (weapon != null) {
                if (ImGui.beginTable("Target Hit Info", 11, 1984)) {
                    ImGui.tableSetupColumn("id");
                    ImGui.tableSetupColumn("Target Facing");
                    ImGui.tableSetupColumn("distance");
                    ImGui.tableSetupColumn("penalty");
                    ImGui.tableSetupColumn("speed");
                    ImGui.tableSetupColumn("light");
                    ImGui.tableSetupColumn("penalty");
                    ImGui.tableSetupColumn("health");
                    ImGui.tableSetupColumn("chance");
                    ImGui.tableSetupColumn("isCameraTarget");
                    ImGui.tableSetupColumn("BodyPart");
                    ImGui.tableHeadersRow();
                    if (weapon == null || this.hitInfoList.isEmpty()) {
                        ImGui.endTable();
                        return;
                    }

                    for (int i = 0; i < this.hitInfoList.size(); i++) {
                        HitInfo hitInfo = this.hitInfoList.get(i);
                        IsoGameCharacter obj = Type.tryCastTo(hitInfo.getObject(), IsoGameCharacter.class);
                        BaseVehicle vehicle = Type.tryCastTo(hitInfo.getObject(), BaseVehicle.class);
                        if (obj != null) {
                            if (obj instanceof IsoZombie) {
                                this.displayIsoZombieRow(obj, weapon, hitInfo);
                            } else {
                                this.displayRow(obj, weapon, hitInfo);
                            }
                        }

                        if (vehicle != null) {
                            this.displayBaseVehicleRow(vehicle, weapon, hitInfo);
                        }
                    }
                }

                ImGui.endTable();
                if (ballisticsController != null) {
                    int numberOfSpreadTargets = ballisticsController.getNumberOfCachedSpreadData();
                    if (numberOfSpreadTargets != 0) {
                        if (ImGui.beginTable("Spread Data", 4, 1984)) {
                            ImGui.tableSetupColumn("id");
                            ImGui.tableSetupColumn("x");
                            ImGui.tableSetupColumn("y");
                            ImGui.tableSetupColumn("z");
                            ImGui.tableHeadersRow();
                            float[] spreadTargets = ballisticsController.getCachedBallisticsTargetSpreadData();
                            int arrayIndex = 0;

                            for (int i = 0; i < numberOfSpreadTargets; i++) {
                                int columnIndex = 0;
                                ImGui.tableNextRow();
                                ImGui.tableSetColumnIndex(columnIndex++);
                                float id = spreadTargets[arrayIndex++];
                                float x = spreadTargets[arrayIndex++];
                                float z = spreadTargets[arrayIndex++] / 2.44949F;
                                float y = spreadTargets[arrayIndex++];
                                if (id != 0.0F) {
                                    ImGui.text(Float.toString(id));
                                    ImGui.tableSetColumnIndex(columnIndex++);
                                    ImGui.text(Float.toString(x));
                                    ImGui.tableSetColumnIndex(columnIndex++);
                                    ImGui.text(Float.toString(z));
                                    ImGui.tableSetColumnIndex(columnIndex++);
                                    ImGui.text(Float.toString(y));
                                }
                            }

                            ImGui.endTable();
                        }
                    }
                }
            }
        }
    }
}
