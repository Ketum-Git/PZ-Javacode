// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ScriptModule;

public abstract class ScriptBucket<E extends BaseScriptObject> {
    private static String currentScriptObject;
    private final HashMap<String, ScriptBucket.LoadData<E>> loadData = new HashMap<>();
    protected final ArrayList<String> loadFiles = new ArrayList<>();
    protected final HashSet<String> dotInName = new HashSet<>();
    protected final ArrayList<E> scriptList = new ArrayList<>();
    protected final Map<String, E> scriptMap;
    protected final ScriptModule module;
    protected final ScriptType scriptType;
    private boolean reload;
    protected boolean hasLoadErrors;
    private boolean verbose;

    public static final String getCurrentScriptObject() {
        return currentScriptObject;
    }

    public ScriptBucket(ScriptModule module, ScriptType scriptType) {
        this(module, scriptType, new HashMap<>());
    }

    public ScriptBucket(ScriptModule module, ScriptType scriptType, Map<String, E> customMap) {
        this.module = module;
        this.scriptType = scriptType;
        if (!(this instanceof ScriptBucket.Template) && scriptType.isTemplate()) {
            throw new RuntimeException("ScriptType '" + scriptType + "' should not be template!");
        } else {
            if (customMap != null) {
                this.scriptMap = customMap;
            } else {
                this.scriptMap = new HashMap<>();
            }
        }
    }

    public ScriptType getScriptType() {
        return this.scriptType;
    }

    protected void setReload(boolean reload) {
        this.reload = reload;
    }

    public boolean isVerbose() {
        return this.verbose || this.scriptType.isVerbose();
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean isHasLoadErrors() {
        return this.hasLoadErrors;
    }

    protected void setLoadError() {
        this.hasLoadErrors = true;
    }

    public ArrayList<E> getScriptList() {
        return this.scriptList;
    }

    public Map<String, E> getScriptMap() {
        return this.scriptMap;
    }

    public void reset() {
        this.loadData.clear();
        this.loadFiles.clear();
        this.dotInName.clear();
        this.scriptList.clear();
        this.scriptMap.clear();
    }

    public abstract E createInstance(ScriptModule var1, String var2, String var3);

    public boolean CreateFromTokenPP(ScriptLoadMode loadMode, String type, String token) {
        try {
            if (this.scriptType.getScriptTag().equals(type)) {
                String[] waypoint = token.split("[{}]");
                String name = waypoint[0];
                name = name.replace(this.scriptType.getScriptTag(), "");
                name = name.trim();
                if (loadMode == ScriptLoadMode.Init && !this.loadFiles.contains(ScriptManager.instance.currentFileName)) {
                    this.loadFiles.add(ScriptManager.instance.currentFileName);
                }

                if (this.loadData.containsKey(name)) {
                    ScriptBucket.LoadData<E> data = this.loadData.get(name);
                    if (loadMode == ScriptLoadMode.Init) {
                        data.reloaded = false;
                        data.script.InitLoadPP(name);
                        data.scriptBodies.add(token);
                        data.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                        ScriptManager.println(this.scriptType, ": Add ScriptBody: '" + data.name + "' " + data.script.debugString());
                    } else if (loadMode == ScriptLoadMode.Reload && this.reload) {
                        if (this.scriptType.hasFlag(ScriptType.Flags.NewInstanceOnReload)) {
                            E script = this.createInstance(this.module, name, token);
                            script.setModule(this.module);
                            script.InitLoadPP(name);
                            data = new ScriptBucket.LoadData<>(script);
                            data.scriptBodies.add(token);
                            data.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                            data.reloaded = true;
                            data.addedOnReload = false;
                            this.loadData.put(data.name, data);
                            return true;
                        }

                        if (!data.reloaded && !data.scriptBodies.isEmpty()) {
                            data.scriptBodies.clear();
                            data.script.resetLoadedScriptBodies();
                        }

                        data.reloaded = true;
                        data.scriptBodies.add(token);
                        data.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                        ScriptManager.println(this.scriptType, ": Reload ScriptBody: '" + data.name + "' " + data.script.debugString());
                    }
                } else if (loadMode != ScriptLoadMode.Init && !this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
                    DebugLog.General.warn("Found new script but was unable to load, possibly due to not being allowed during reload...");
                    DebugLog.log(">>> : Load ScriptBody: '" + name + "', File: " + ScriptManager.instance.currentFileName);
                    if (!this.scriptType.hasFlag(ScriptType.Flags.AllowNewScriptDiscoveryOnReload)) {
                        DebugLog.log(">>> : Discovery of new scripts during reload not allowed for scripts of type: " + this.scriptType);
                        if (Core.debug) {
                            throw new Exception("Not allowed");
                        }
                    }
                } else {
                    E script = this.createInstance(this.module, name, token);
                    script.setModule(this.module);
                    script.InitLoadPP(name);
                    ScriptBucket.LoadData<E> data = new ScriptBucket.LoadData<>(script);
                    data.scriptBodies.add(token);
                    data.script.addLoadedScriptBody(ScriptManager.getCurrentLoadFileMod(), token);
                    data.reloaded = loadMode == ScriptLoadMode.Reload;
                    data.addedOnReload = loadMode == ScriptLoadMode.Reload;
                    this.loadData.put(data.name, data);
                    ScriptManager.println(this.scriptType, ": New ScriptBody: '" + data.name + "' " + data.script.debugString());
                }

                return true;
            }
        } catch (Exception var8) {
            ExceptionLogger.logException(var8);
            this.hasLoadErrors = true;
        }

        return false;
    }

    public void LoadScripts(ScriptLoadMode loadMode) {
        for (ScriptBucket.LoadData<E> data : this.loadData.values()) {
            try {
                currentScriptObject = data.script != null ? data.script.getScriptObjectFullType() : null;
                if (loadMode != ScriptLoadMode.Reload || data.reloaded) {
                    data.reloaded = false;
                    E script = data.script;
                    if (this.isVerbose()) {
                        DebugLog.General.debugln("[" + this.scriptType.getScriptTag() + "] load script = " + script.getScriptObjectName());
                    }

                    if (loadMode == ScriptLoadMode.Reload && this.scriptType.hasFlag(ScriptType.Flags.ResetOnceOnReload)) {
                        script.reset();
                    }

                    int resetStartIndex = loadMode == ScriptLoadMode.Reload ? 0 : 1;

                    for (int index = 0; index < data.scriptBodies.size(); index++) {
                        String body = data.scriptBodies.get(index);

                        try {
                            if (this.scriptType.hasFlag(ScriptType.Flags.ResetExisting) && index >= resetStartIndex) {
                                script.reset();
                            }

                            script.Load(data.name, body);
                            ScriptManager.println(this.scriptType, " - Load: '" + data.name + "' " + data.script);
                        } catch (Exception var13) {
                            if (this.scriptType.hasFlag(ScriptType.Flags.RemoveLoadError)) {
                                DebugLog.log("[" + this.scriptType.getScriptTag() + "] removing script due to load error = " + script.getScriptObjectName());
                                script = null;
                            }

                            this.hasLoadErrors = true;
                            ExceptionLogger.logException(var13);
                            break;
                        }
                    }

                    if (script != null) {
                        if (script.getObsolete()) {
                            DebugLog.Script.debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, obsolete = " + script.getScriptObjectName());
                            continue;
                        }

                        if (!script.isEnabled()) {
                            DebugLog.Script.debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, disabled = " + script.getScriptObjectName());
                            continue;
                        }

                        if (script.isDebugOnly() && !Core.debug) {
                            DebugLog.Script
                                .debugln("[" + this.scriptType.getScriptTag() + "] ignoring script, is debug only = " + script.getScriptObjectName());
                            continue;
                        }

                        script.calculateScriptVersion();
                        if (loadMode == ScriptLoadMode.Init || data.addedOnReload) {
                            this.onScriptLoad(loadMode, script);
                            this.scriptMap.put(script.getScriptObjectName(), script);
                            this.scriptList.add(script);
                            if (script.getScriptObjectName().contains(".")) {
                                this.dotInName.add(script.getScriptObjectName());
                            }
                        } else if (loadMode == ScriptLoadMode.Reload) {
                            this.onScriptLoad(loadMode, script);
                        }
                    }

                    data.addedOnReload = false;
                }
            } catch (Exception var14) {
                ExceptionLogger.logException(var14);
                this.hasLoadErrors = true;
            } finally {
                currentScriptObject = null;
            }
        }
    }

    protected void onScriptLoad(ScriptLoadMode loadMode, E script) {
    }

    protected abstract E getFromManager(String var1);

    protected abstract E getFromModule(String var1, ScriptModule var2);

    public E get(String name) {
        if (name.contains(".") && !this.dotInName.contains(name)) {
            return this.getFromManager(name);
        } else {
            E script = this.scriptMap.get(name);
            if (script != null) {
                return script;
            } else {
                if (this.scriptType.hasFlag(ScriptType.Flags.SeekImports)) {
                    for (int n = 0; n < this.module.imports.size(); n++) {
                        String moduleName = this.module.imports.get(n);
                        ScriptModule module = ScriptManager.instance.getModule(moduleName);
                        script = this.getFromModule(name, module);
                        if (script != null) {
                            return script;
                        }
                    }
                }

                return null;
            }
        }
    }

    private static class LoadData<E extends BaseScriptObject> {
        private final String name;
        private final ArrayList<String> scriptBodies = new ArrayList<>();
        private final E script;
        private boolean reloaded;
        private boolean addedOnReload;

        private LoadData(E script) {
            this.name = script.getScriptObjectName();
            this.script = script;
        }
    }

    public abstract static class Template<E extends BaseScriptObject> extends ScriptBucket<E> {
        public Template(ScriptModule module, ScriptType scriptType) {
            super(module, scriptType);
            if (!scriptType.isTemplate()) {
                throw new RuntimeException("ScriptType '" + scriptType + "' should be template!");
            }
        }

        @Override
        public boolean CreateFromTokenPP(ScriptLoadMode loadMode, String type, String token) {
            try {
                if ("template".equals(type)) {
                    String[] waypoint = token.split("[{}]");
                    String typeAndName = waypoint[0];
                    typeAndName = typeAndName.replace("template", "");
                    String[] split = typeAndName.trim().split("\\s+");
                    if (split.length == 2) {
                        String type1 = split[0].trim();
                        String name = split[1].trim();
                        if (this.scriptType.getScriptTag().equals(type1)) {
                            E script = this.createInstance(this.module, name, token);
                            script.InitLoadPP(name);
                            this.scriptMap.put(script.getScriptObjectName(), script);
                            if (script.getScriptObjectName().contains(".")) {
                                this.dotInName.add(script.getScriptObjectName());
                            }

                            ScriptManager.println(this.scriptType, "Loaded template: " + script.getScriptObjectName());
                            return true;
                        }
                    }
                }
            } catch (Exception var10) {
                ExceptionLogger.logException(var10);
                this.hasLoadErrors = true;
            }

            return false;
        }

        @Override
        public void LoadScripts(ScriptLoadMode loadMode) {
            this.scriptList.clear();
            this.scriptList.addAll(this.scriptMap.values());
        }
    }
}
