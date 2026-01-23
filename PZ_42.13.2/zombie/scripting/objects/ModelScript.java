// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@UsedFromLua
public final class ModelScript extends BaseScriptObject implements IModelAttachmentOwner {
    public static final String DEFAULT_SHADER_NAME = "basicEffect";
    public String fileName;
    public String name;
    public String meshName;
    public String textureName;
    public String shaderName;
    public boolean isStatic = true;
    public float scale = 1.0F;
    public final ArrayList<ModelAttachment> attachments = new ArrayList<>();
    public HashMap<String, ModelAttachment> attachmentById = new HashMap<>();
    public boolean invertX;
    public String postProcess;
    public Model loadedModel;
    public final ArrayList<AnimBoneWeight> boneWeights = new ArrayList<>();
    public String animationsMesh;
    public int cullFace = -1;
    private static final HashSet<String> reported = new HashSet<>();

    public ModelScript() {
        super(ScriptType.Model);
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.fileName = scriptMgr.currentFileName;
        this.name = name;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Block child : block.children) {
            if ("attachment".equals(child.type)) {
                this.LoadAttachment(child);
            }
        }

        boolean bUndoCoreScale = false;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("mesh".equalsIgnoreCase(k)) {
                this.meshName = v;
            } else if ("scale".equalsIgnoreCase(k)) {
                this.scale = Float.parseFloat(v);
            } else if ("shader".equalsIgnoreCase(k)) {
                this.shaderName = v;
            } else if ("static".equalsIgnoreCase(k)) {
                this.isStatic = Boolean.parseBoolean(v);
            } else if ("texture".equalsIgnoreCase(k)) {
                this.textureName = v;
            } else if ("invertX".equalsIgnoreCase(k)) {
                this.invertX = Boolean.parseBoolean(v);
            } else if ("cullFace".equalsIgnoreCase(k)) {
                if (CullFace.BACK.toString().equalsIgnoreCase(v)) {
                    this.cullFace = 1029;
                } else if (CullFace.FRONT.toString().equalsIgnoreCase(v)) {
                    this.cullFace = 1028;
                } else if (CullFace.NONE.toString().equalsIgnoreCase(v)) {
                    this.cullFace = 0;
                }
            } else if ("postProcess".equalsIgnoreCase(k)) {
                this.postProcess = v;
            } else if ("undoCoreScale".equalsIgnoreCase(k)) {
                bUndoCoreScale = Boolean.parseBoolean(v);
            } else if ("boneWeight".equalsIgnoreCase(k)) {
                String[] ss1 = v.split("\\s+");
                if (ss1.length == 2) {
                    AnimBoneWeight boneWeight = new AnimBoneWeight(ss1[0], PZMath.tryParseFloat(ss1[1], 1.0F));
                    boneWeight.includeDescendants = false;
                    this.boneWeights.add(boneWeight);
                }
            } else if ("animationsMesh".equalsIgnoreCase(k)) {
                this.animationsMesh = StringUtils.discardNullOrWhitespace(v);
            }
        }

        if (bUndoCoreScale) {
            this.scale *= 0.6666667F;
        }
    }

    private ModelAttachment LoadAttachment(ScriptParser.Block block) {
        ModelAttachment attachment = this.getAttachmentById(block.id);
        if (attachment == null) {
            attachment = new ModelAttachment(block.id.intern());
            attachment.setOwner(this);
            this.attachments.add(attachment);
            this.attachmentById.put(attachment.getId(), attachment);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim().intern();
            String v = value.getValue().trim().intern();
            if ("bone".equals(k)) {
                attachment.setBone(v);
            } else if ("offset".equals(k)) {
                this.LoadVector3f(v, attachment.getOffset());
            } else if ("rotate".equals(k)) {
                this.LoadVector3f(v, attachment.getRotate());
            } else if ("scale".equals(k)) {
                attachment.setScale(PZMath.tryParseFloat(v, 1.0F));
            }
        }

        return attachment;
    }

    private void LoadVector3f(String s, Vector3f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
    }

    public String getName() {
        return this.name;
    }

    public String getFullType() {
        return this.getModule().name + "." + this.name;
    }

    public String getMeshName() {
        return this.meshName;
    }

    public String getTextureName() {
        return StringUtils.isNullOrWhitespace(this.textureName) ? this.meshName : this.textureName;
    }

    public String getTextureName(boolean allowNull) {
        return StringUtils.isNullOrWhitespace(this.textureName) && !allowNull ? this.meshName : this.textureName;
    }

    public String getShaderName() {
        return StringUtils.isNullOrWhitespace(this.shaderName) ? "basicEffect" : this.shaderName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getAttachmentCount() {
        return this.attachments.size();
    }

    public ModelAttachment getAttachment(int index) {
        return this.attachments.get(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        return this.attachmentById.get(id);
    }

    public ModelAttachment addAttachment(ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(attach);
        this.attachmentById.put(attach.getId(), attach);
        return attach;
    }

    public ModelAttachment removeAttachment(ModelAttachment attach) {
        attach.setOwner(null);
        this.attachments.remove(attach);
        this.attachmentById.remove(attach.getId());
        return attach;
    }

    public ModelAttachment addAttachmentAt(int index, ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(index, attach);
        this.attachmentById.put(attach.getId(), attach);
        return attach;
    }

    public ModelAttachment removeAttachment(int index) {
        ModelAttachment attachment = this.attachments.remove(index);
        this.attachmentById.remove(attachment.getId());
        attachment.setOwner(null);
        return attachment;
    }

    public void scaleAttachmentOffset(float scale) {
        for (int i = 0; i < this.getAttachmentCount(); i++) {
            ModelAttachment attachment = this.getAttachment(i);
            attachment.getOffset().mul(scale);
        }
    }

    @Override
    public void beforeRenameAttachment(ModelAttachment attachment) {
        this.attachmentById.remove(attachment.getId());
    }

    @Override
    public void afterRenameAttachment(ModelAttachment attachment) {
        this.attachmentById.put(attachment.getId(), attachment);
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    @Override
    public void reset() {
        this.invertX = false;
        this.name = null;
        this.meshName = null;
        this.textureName = null;
        this.shaderName = null;
        this.isStatic = true;
        this.scale = 1.0F;
        this.boneWeights.clear();
        this.cullFace = -1;
    }

    private static void checkMesh(String object, String meshName) {
        if (!StringUtils.isNullOrWhitespace(meshName)) {
            String lower = meshName.toLowerCase(Locale.ENGLISH);
            if (!ZomboidFileSystem.instance.activeFileMap.containsKey("media/models_x/" + lower + ".fbx")
                && !ZomboidFileSystem.instance.activeFileMap.containsKey("media/models_x/" + lower + ".x")
                && !ZomboidFileSystem.instance.activeFileMap.containsKey("media/models/" + lower + ".txt")) {
                reported.add(meshName);
                DebugLog.Script.warn("no such mesh \"" + meshName + "\" for " + object);
            }
        }
    }

    private static void checkTexture(String object, String textureName) {
        if (!GameServer.server) {
            if (!StringUtils.isNullOrWhitespace(textureName)) {
                String lower = textureName.toLowerCase(Locale.ENGLISH);
                if (!ZomboidFileSystem.instance.activeFileMap.containsKey("media/textures/" + lower + ".png")) {
                    reported.add(textureName);
                    DebugLog.Script.warn("no such texture \"" + textureName + "\" for " + object);
                }
            }
        }
    }

    private static void check(String object, String model) {
        check(object, model, null);
    }

    private static void check(String object, String model, String clothingItem) {
        if (!StringUtils.isNullOrWhitespace(model)) {
            if (!reported.contains(model)) {
                ModelScript modelScript = ScriptManager.instance.getModelScript(model);
                if (modelScript == null) {
                    reported.add(model);
                    DebugLog.Script.warn("no such model \"" + model + "\" for " + object);
                } else {
                    checkMesh(modelScript.getFullType(), modelScript.getMeshName());
                    if (StringUtils.isNullOrWhitespace(clothingItem)) {
                        checkTexture(modelScript.getFullType(), modelScript.getTextureName());
                    }
                }
            }
        }
    }

    public static void ScriptsLoaded() {
        reported.clear();

        for (Item item : ScriptManager.instance.getAllItems()) {
            item.resolveModelScripts();
            check(item.getFullName(), item.getStaticModel());
            check(item.getFullName(), item.getWeaponSprite());
            check(item.getFullName(), item.worldStaticModel, item.getClothingItem());
            if (item.isItemType(ItemType.FOOD)) {
                String staticModel = item.getStaticModel();
                if (!StringUtils.isNullOrWhitespace(staticModel)) {
                    ModelScript modelScript = ScriptManager.instance.getModelScript(staticModel);
                    if (modelScript != null && modelScript.getAttachmentCount() != 0) {
                        ModelScript modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Burnt");
                        if (modelScript2 != null) {
                            checkTexture(modelScript2.getName(), modelScript2.textureName);
                        }

                        if (modelScript2 != null && modelScript2.getAttachmentCount() != modelScript.getAttachmentCount()) {
                            DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
                        }

                        modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Cooked");
                        if (modelScript2 != null) {
                            checkTexture(modelScript2.getName(), modelScript2.textureName);
                        }

                        if (modelScript2 != null && modelScript2.getAttachmentCount() != modelScript.getAttachmentCount()) {
                            DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
                        }

                        modelScript2 = ScriptManager.instance.getModelScript(staticModel + "Rotten");
                        if (modelScript2 != null) {
                            checkTexture(modelScript2.getName(), modelScript2.textureName);
                        }

                        if (modelScript2 != null && modelScript2.getAttachmentCount() != modelScript.getAttachmentCount()) {
                            DebugLog.Script.warn("different number of attachments on %s and %s", modelScript.name, modelScript2.name);
                        }
                    }
                }
            }
        }
    }
}
