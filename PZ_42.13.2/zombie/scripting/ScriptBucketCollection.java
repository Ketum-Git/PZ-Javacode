// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.debug.DebugLog;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucketCollection<E extends BaseScriptObject> {
    private final ScriptManager scriptManager;
    private final ScriptType scriptType;
    private final HashMap<ScriptModule, ScriptBucket<E>> map = new HashMap<>();
    private final ArrayList<ScriptModule> scriptModules = new ArrayList<>();
    private final ArrayList<ScriptBucket<E>> scriptBuckets = new ArrayList<>();
    private final ArrayList<E> allScripts = new ArrayList<>();
    private final HashMap<String, E> fullTypeToScriptMap = new HashMap<>();
    protected final ArrayList<String> loadFiles = new ArrayList<>();

    public ScriptBucketCollection(ScriptManager scriptManager, ScriptType scriptType) {
        this.scriptManager = scriptManager;
        this.scriptType = scriptType;
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }

    public boolean isTemplate() {
        return this.scriptType.isTemplate();
    }

    public void reset() {
        this.map.clear();
        this.scriptModules.clear();
        this.scriptBuckets.clear();
        this.allScripts.clear();
        this.fullTypeToScriptMap.clear();
        this.loadFiles.clear();
    }

    public boolean hasFullType(String type) {
        return this.fullTypeToScriptMap.containsKey(type);
    }

    public E getFullType(String type) {
        return this.fullTypeToScriptMap.get(type);
    }

    public HashMap<String, E> getFullTypeToScriptMap() {
        return this.fullTypeToScriptMap;
    }

    public void setReloadBuckets(boolean bReload) {
        for (ScriptBucket<?> bucket : this.scriptBuckets) {
            bucket.setReload(bReload);
        }
    }

    public void registerModule(ScriptModule module) {
        this.scriptModules.add(module);
        ScriptBucket<E> bucket = this.getBucketFromModule(module);
        if (this.scriptType != bucket.scriptType) {
            throw new RuntimeException("ScriptType does not match bucket ScriptType");
        } else {
            this.scriptBuckets.add(bucket);
            this.map.put(module, bucket);
        }
    }

    public abstract ScriptBucket<E> getBucketFromModule(ScriptModule arg0);

    public E getScript(String name) {
        if (this.scriptType.hasFlag(ScriptType.Flags.CacheFullType) && name.contains(".") && this.fullTypeToScriptMap.containsKey(name)) {
            return this.fullTypeToScriptMap.get(name);
        } else {
            ScriptModule module;
            if (!name.contains(".")) {
                module = this.scriptManager.getModule("Base");
            } else {
                module = this.scriptManager.getModule(name);
            }

            if (module == null) {
                return null;
            } else {
                ScriptBucket<E> bucket = this.map.get(module);
                return bucket.get(ScriptManager.getItemName(name));
            }
        }
    }

    public ArrayList<E> getAllScripts() {
        if (!this.scriptType.hasFlag(ScriptType.Flags.Clear) && !this.allScripts.isEmpty()) {
            return this.allScripts;
        } else {
            this.allScripts.clear();

            for (int i = 0; i < this.scriptBuckets.size(); i++) {
                ScriptBucket<E> bucket = this.scriptBuckets.get(i);
                if (!bucket.module.disabled) {
                    if (this.scriptType.hasFlag(ScriptType.Flags.FromList)) {
                        this.allScripts.addAll(bucket.scriptList);
                    } else {
                        this.allScripts.addAll(bucket.scriptMap.values());
                    }
                }
            }

            this.onSortAllScripts(this.allScripts);
            return this.allScripts;
        }
    }

    public void onSortAllScripts(ArrayList<E> scripts) {
    }

    public void LoadScripts(ScriptLoadMode loadMode) {
        DebugLog.Script.trace("Load Scripts: " + this.scriptType.toString() + ", loadMode = " + loadMode);

        for (ScriptBucket<E> scriptBucket : this.scriptBuckets) {
            scriptBucket.LoadScripts(loadMode);

            for (String loadFile : scriptBucket.loadFiles) {
                if (!this.loadFiles.contains(loadFile)) {
                    this.loadFiles.add(loadFile);
                }
            }

            for (E script : scriptBucket.getScriptList()) {
                this.fullTypeToScriptMap.put(script.getScriptObjectFullType(), script);
            }
        }
    }

    public void PreReloadScripts() throws Exception {
        ScriptManager.println(this.scriptType, "<- PreReloadScripts ->");

        for (E script : this.getAllScripts()) {
            script.PreReload();
        }

        this.allScripts.clear();
    }

    public void PostLoadScripts(ScriptLoadMode loadMode) throws Exception {
    }

    public boolean hasLoadErrors() {
        return this.hasLoadErrors(false);
    }

    public boolean hasLoadErrors(boolean onlyCritical) {
        for (ScriptBucket<E> bucket : this.scriptBuckets) {
            if (bucket.isHasLoadErrors() && (!onlyCritical || this.scriptType.isCritical())) {
                return true;
            }
        }

        return false;
    }

    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        ScriptManager.println(this.scriptType, "<- OnScriptsLoaded ->");

        for (E script : this.getAllScripts()) {
            script.OnScriptsLoaded(loadMode);
        }
    }

    public void OnLoadedAfterLua() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnLoadedAfterLua ->");

        for (E script : this.getAllScripts()) {
            script.OnLoadedAfterLua();
        }
    }

    public void OnPostTileDefinitions() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnPostTileDefinitions ->");
    }

    public void OnPostWorldDictionaryInit() throws Exception {
        ScriptManager.println(this.scriptType, "<- OnPostWorldDictionaryInit ->");

        for (E script : this.getAllScripts()) {
            script.OnPostWorldDictionaryInit();
        }
    }
}
