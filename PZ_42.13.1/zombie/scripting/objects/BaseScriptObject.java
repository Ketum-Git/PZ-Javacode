// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugIgnoreField;
import zombie.debug.objects.DebugNonRecursive;
import zombie.iso.Vector3;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;
import zombie.util.hash.PZHash;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
@UsedFromLua
public abstract class BaseScriptObject {
    @DebugIgnoreField
    private final ArrayList<String> loadedScriptBodies = new ArrayList<>(0);
    @DebugIgnoreField
    private ArrayList<String> linesCache;
    private final ScriptType scriptObjectType;
    @DebugNonRecursive
    private ScriptModule module;
    private String scriptObjectName;
    @DebugNonRecursive
    private BaseScriptObject parentScript;
    private String fullTypeCache;
    private boolean fullTypeDirty = true;
    private long scriptVersion = -1L;
    protected boolean enabled = true;
    protected boolean debugOnly;

    protected BaseScriptObject(ScriptType type) {
        this.scriptObjectType = type;
    }

    public final String debugString() {
        return "[type="
            + this.scriptObjectType
            + ", module="
            + (this.module != null ? this.module.getName() : "null")
            + ", name="
            + this.scriptObjectName
            + ", fulltype="
            + this.getScriptObjectFullType()
            + "]";
    }

    @Deprecated
    public void getVersion(IVersionHash hash) {
        throw new RuntimeException("Not implemented. class = " + this.getClass().getSimpleName());
    }

    public long getScriptVersion() {
        return this.scriptVersion;
    }

    public void calculateScriptVersion() {
        int len = this.getScriptObjectFullType().length();

        for (int i = 0; i < this.loadedScriptBodies.size(); i++) {
            String body = this.loadedScriptBodies.get(i);
            len += body.length();
        }

        StringBuilder sb = new StringBuilder(len);
        sb.append(this.getScriptObjectFullType());

        for (int i = 0; i < this.loadedScriptBodies.size(); i++) {
            String body = this.loadedScriptBodies.get(i);
            sb.append(body);
        }

        this.scriptVersion = PZHash.murmur_64(sb.toString());
    }

    public ScriptModule getModule() {
        return this.module;
    }

    public void setModule(ScriptModule module) {
        this.module = module;
        this.fullTypeDirty = true;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public final boolean isDebugOnly() {
        return this.debugOnly;
    }

    public final void setParent(BaseScriptObject parent) {
        this.parentScript = parent;
        if (parent != null) {
            this.setModule(parent.getModule());
        }
    }

    public final BaseScriptObject getParent() {
        return this.parentScript;
    }

    public final ScriptType getScriptObjectType() {
        return this.scriptObjectType;
    }

    public final String getScriptObjectName() {
        return this.scriptObjectName;
    }

    public final String getScriptObjectFullType() {
        if (!this.fullTypeDirty && this.fullTypeCache != null) {
            return this.fullTypeCache;
        } else {
            this.fullTypeDirty = false;
            String moduleName = this.module != null && this.module.name != null ? this.module.getName() : null;
            String scriptName = this.scriptObjectName != null ? this.scriptObjectName : null;
            if (moduleName != null && scriptName != null) {
                this.fullTypeCache = moduleName + "." + scriptName;
                return this.fullTypeCache;
            } else {
                throw new RuntimeException("[" + this.scriptObjectType + "] Module or name missing, module: " + moduleName + ", script: " + scriptName);
            }
        }
    }

    public final void resetLoadedScriptBodies() {
        this.loadedScriptBodies.clear();
    }

    public final void addLoadedScriptBody(String modId, String body) {
        this.loadedScriptBodies.add(modId);
        this.loadedScriptBodies.add(body);
    }

    public final ArrayList<String> getLoadedScriptBodies() {
        return this.loadedScriptBodies;
    }

    public final int getLoadedScriptBodyCount() {
        return this.loadedScriptBodies.size();
    }

    public boolean getObsolete() {
        return false;
    }

    public void InitLoadPP(String name) {
        this.scriptObjectName = name;
        this.fullTypeDirty = true;
    }

    public final void LoadCommonBlock(String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
    }

    public final void LoadCommonBlock(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("enabled")) {
                    this.enabled = val.equalsIgnoreCase("true");
                } else if (key.equalsIgnoreCase("debugOnly")) {
                    this.debugOnly = val.equalsIgnoreCase("true");
                }
            }
        }
    }

    public void Load(String name, String body) throws Exception {
        if (Core.debug) {
            throw new RuntimeException("Load(name,totalFile) not overridden. [" + this.getClass().getSimpleName() + "]");
        } else {
            DebugLog.General.warn("Load(name,totalFile) not overridden. [" + this.getClass().getSimpleName() + "]");
        }
    }

    public void PreReload() {
    }

    public void reset() {
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    public void OnLoadedAfterLua() throws Exception {
    }

    public void OnPostWorldDictionaryInit() throws Exception {
    }

    public ArrayList<String> getScriptLines() {
        if (this.linesCache == null) {
            this.linesCache = new ArrayList<>();
            this.getAllScriptLines(this.linesCache);
        }

        return this.linesCache;
    }

    public final ArrayList<String> getAllScriptLines(ArrayList<String> list) {
        if (this.loadedScriptBodies.size() < 2) {
            return list;
        } else {
            for (int i = this.loadedScriptBodies.size() - 2; i >= 0; i -= 2) {
                list.add("/* SCRIPT BODY: " + this.loadedScriptBodies.get(i) + " */");
                String[] r_lines = this.loadedScriptBodies.get(i + 1).split("\\r?\\n");
                int emptyLines = 0;

                for (String s : r_lines) {
                    if (StringUtils.isNullOrWhitespace(s)) {
                        emptyLines++;
                    } else {
                        emptyLines = 0;
                    }

                    if (emptyLines < 2) {
                        list.add(s);
                    }
                }
            }

            return list;
        }
    }

    public final ArrayList<String> getBodyScriptLines(int bodyIndex, ArrayList<String> list) {
        bodyIndex = bodyIndex * 2 + 1;
        if (bodyIndex >= 0 && bodyIndex < this.loadedScriptBodies.size()) {
            String[] r_lines = this.loadedScriptBodies.get(bodyIndex).split("\\r?\\n");
            int emptyLines = 0;

            for (String s : r_lines) {
                if (StringUtils.isNullOrWhitespace(s)) {
                    emptyLines++;
                } else {
                    emptyLines = 0;
                }

                if (emptyLines < 2) {
                    list.add(s);
                }
            }
        }

        return list;
    }

    protected void LoadVector3(String s, Vector3 v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
    }
}
