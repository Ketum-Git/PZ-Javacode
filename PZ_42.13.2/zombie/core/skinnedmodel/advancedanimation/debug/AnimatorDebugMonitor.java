// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class AnimatorDebugMonitor {
    public static AnimatorDebugMonitor instance;
    private IsoGameCharacter targetIsoGameCharacter;
    private static final ArrayList<String> knownVariables = new ArrayList<>();
    private static boolean knownVarsDirty;
    private String currentState = "null";
    private AnimatorDebugMonitor.MonitoredLayer[] monitoredLayers;
    private final HashMap<String, AnimatorDebugMonitor.MonitoredVar> monitoredVariables = new HashMap<>();
    private final ArrayList<String> customVariables = new ArrayList<>();
    private final LinkedList<AnimatorDebugMonitor.MonitorLogLine> logLines = new LinkedList<>();
    private final Queue<AnimatorDebugMonitor.MonitorLogLine> logLineQueue = new LinkedList<>();
    private boolean floatsListDirty;
    private boolean hasFilterChanges;
    private boolean hasLogUpdates;
    private String logString = "";
    private static final int maxLogSize = 1028;
    private static final int maxOutputLines = 128;
    private static final int maxFloatCache = 1024;
    private final ArrayList<Float> floatsOut = new ArrayList<>();
    private AnimatorDebugMonitor.MonitoredVar selectedVariable;
    private int tickCount;
    private boolean doTickStamps;
    private static final int tickStampLength = 10;
    private static final Color col_curstate = Colors.Cyan;
    private static final Color col_layer_nodename = Colors.CornFlowerBlue;
    private static final Color col_layer_activated = Colors.DarkTurquoise;
    private static final Color col_layer_deactivated = Colors.Orange;
    private static final Color col_track_activated = Colors.SandyBrown;
    private static final Color col_track_deactivated = Colors.Salmon;
    private static final Color col_node_activated = Colors.Pink;
    private static final Color col_node_deactivated = Colors.Plum;
    private static final Color col_var_activated = Colors.Chartreuse;
    private static final Color col_var_changed = Colors.LimeGreen;
    private static final Color col_var_deactivated = Colors.Gold;
    private static final String TAG_VAR = "[variable]";
    private static final String TAG_LAYER = "[layer]";
    private static final String TAG_NODE = "[active_nodes]";
    private static final String TAG_TRACK = "[anim_tracks]";
    private final boolean[] logFlags;

    public IsoGameCharacter getTarget() {
        return this.targetIsoGameCharacter;
    }

    public void setTarget(IsoGameCharacter isoGameCharacter) {
        this.targetIsoGameCharacter = isoGameCharacter;
    }

    public AnimatorDebugMonitor(IsoGameCharacter chr) {
        if (instance != this) {
            instance = this;
            this.targetIsoGameCharacter = chr;
        }

        this.logFlags = new boolean[AnimatorDebugMonitor.LogType.MAX.value()];
        this.logFlags[AnimatorDebugMonitor.LogType.DEFAULT.value()] = true;

        for (int i = 0; i < this.logFlags.length; i++) {
            this.logFlags[i] = true;
        }

        for (int i = 0; i < 1024; i++) {
            this.floatsOut.add(0.0F);
        }

        this.initCustomVars();
        if (chr != null && chr.advancedAnimator != null) {
            for (String v : chr.advancedAnimator.debugGetVariables()) {
                registerVariable(v);
            }
        }
    }

    private void initCustomVars() {
        this.addCustomVariable("aim");
        this.addCustomVariable("bdead");
        this.addCustomVariable("bfalling");
        this.addCustomVariable("baimatfloor");
        this.addCustomVariable("battackfrombehind");
        this.addCustomVariable("attacktype");
        this.addCustomVariable("bundervehicle");
        this.addCustomVariable("reanimatetimer");
        this.addCustomVariable("isattacking");
        this.addCustomVariable("canclimbdownrope");
        this.addCustomVariable("frombehind");
        this.addCustomVariable("fallonfront");
        this.addCustomVariable("hashitreaction");
        this.addCustomVariable("hitreaction");
        this.addCustomVariable("collided");
        this.addCustomVariable("collidetype");
        this.addCustomVariable("intrees");
    }

    public void addCustomVariable(String var) {
        String v = var.toLowerCase();
        if (!this.customVariables.contains(v)) {
            this.customVariables.add(v);
        }

        registerVariable(var);
    }

    public void removeCustomVariable(String var) {
        String v = var.toLowerCase();
        this.customVariables.remove(v);
    }

    public void setFilter(int index, boolean b) {
        if (index >= 0 && index < AnimatorDebugMonitor.LogType.MAX.value()) {
            this.logFlags[index] = b;
            this.hasFilterChanges = true;
        }
    }

    public boolean getFilter(int index) {
        return index >= 0 && index < AnimatorDebugMonitor.LogType.MAX.value() ? this.logFlags[index] : false;
    }

    public boolean isDoTickStamps() {
        return this.doTickStamps;
    }

    public void setDoTickStamps(boolean doTickStamps) {
        if (this.doTickStamps != doTickStamps) {
            this.doTickStamps = doTickStamps;
            this.hasFilterChanges = true;
        }
    }

    private void queueLogLine(String str) {
        this.addLogLine(AnimatorDebugMonitor.LogType.DEFAULT, str, null, true);
    }

    private void queueLogLine(String str, Color col) {
        this.addLogLine(AnimatorDebugMonitor.LogType.DEFAULT, str, col, true);
    }

    private void queueLogLine(AnimatorDebugMonitor.LogType t, String str, Color col) {
        this.addLogLine(t, str, col, true);
    }

    private void addLogLine(String str) {
        this.addLogLine(AnimatorDebugMonitor.LogType.DEFAULT, str, null, false);
    }

    private void addLogLine(String str, Color col) {
        this.addLogLine(AnimatorDebugMonitor.LogType.DEFAULT, str, col, false);
    }

    private void addLogLine(String str, Color col, boolean queue) {
        this.addLogLine(AnimatorDebugMonitor.LogType.DEFAULT, str, col, queue);
    }

    private void addLogLine(AnimatorDebugMonitor.LogType t, String str, Color col) {
        this.addLogLine(t, str, col, false);
    }

    private void addLogLine(AnimatorDebugMonitor.LogType t, String str, Color col, boolean queue) {
        AnimatorDebugMonitor.MonitorLogLine log = new AnimatorDebugMonitor.MonitorLogLine();
        log.line = str;
        log.color = col;
        log.type = t;
        log.tick = this.tickCount;
        if (queue) {
            this.logLineQueue.add(log);
        } else {
            this.log(log);
        }
    }

    private void log(AnimatorDebugMonitor.MonitorLogLine l) {
        this.logLines.addFirst(l);
        if (this.logLines.size() > 1028) {
            this.logLines.removeLast();
        }

        this.hasLogUpdates = true;
    }

    private void processQueue() {
        while (!this.logLineQueue.isEmpty()) {
            AnimatorDebugMonitor.MonitorLogLine l = this.logLineQueue.poll();
            this.log(l);
        }
    }

    private void preUpdate() {
        for (Entry<String, AnimatorDebugMonitor.MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            entry.getValue().updated = false;
        }

        for (int index = 0; index < this.monitoredLayers.length; index++) {
            AnimatorDebugMonitor.MonitoredLayer l = this.monitoredLayers[index];
            l.updated = false;

            for (Entry<String, AnimatorDebugMonitor.MonitoredNode> entry : l.activeNodes.entrySet()) {
                entry.getValue().updated = false;
            }

            for (Entry<String, AnimatorDebugMonitor.MonitoredTrack> entry : l.animTracks.entrySet()) {
                entry.getValue().updated = false;
            }
        }
    }

    private void postUpdate() {
        for (Entry<String, AnimatorDebugMonitor.MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            if (entry.getValue().active && !entry.getValue().updated) {
                this.addLogLine(
                    AnimatorDebugMonitor.LogType.VAR,
                    "[variable] : removed -> '" + entry.getKey() + "', last value: '" + entry.getValue().value + "'.",
                    col_var_deactivated
                );
                entry.getValue().active = false;
            }
        }

        for (int index = 0; index < this.monitoredLayers.length; index++) {
            AnimatorDebugMonitor.MonitoredLayer l = this.monitoredLayers[index];

            for (Entry<String, AnimatorDebugMonitor.MonitoredNode> entryx : l.activeNodes.entrySet()) {
                if (entryx.getValue().active && !entryx.getValue().updated) {
                    this.addLogLine(
                        AnimatorDebugMonitor.LogType.NODE,
                        "[layer][" + l.index + "] [active_nodes] : deactivated -> '" + entryx.getValue().name + "'.",
                        col_node_deactivated
                    );
                    entryx.getValue().active = false;
                }
            }

            for (Entry<String, AnimatorDebugMonitor.MonitoredTrack> entryxx : l.animTracks.entrySet()) {
                if (entryxx.getValue().active && !entryxx.getValue().updated) {
                    this.addLogLine(
                        AnimatorDebugMonitor.LogType.TRACK,
                        "[layer][" + l.index + "] [anim_tracks] : deactivated -> '" + entryxx.getValue().name + "'.",
                        col_track_deactivated
                    );
                    entryxx.getValue().active = false;
                }
            }

            if (l.active && !l.updated) {
                this.addLogLine(
                    AnimatorDebugMonitor.LogType.LAYER, "[layer][" + index + "] : deactivated (last animstate: '" + l.nodeName + "').", col_layer_deactivated
                );
                l.active = false;
            }
        }
    }

    public void update(IsoGameCharacter chr, AnimLayer[] Layers) {
        if (chr != null) {
            this.ensureLayers(Layers);
            this.preUpdate();

            for (IAnimationVariableSlot entry : chr.getGameVariables()) {
                this.updateVariable(entry.getKey(), entry.getValueString());
            }

            for (String var : this.customVariables) {
                String val = chr.getVariableString(var);
                if (val != null) {
                    this.updateVariable(var, val);
                }
            }

            this.updateCurrentState(chr.getCurrentState() == null ? "null" : chr.getCurrentState().getClass().getSimpleName());

            for (int l = 0; l < Layers.length; l++) {
                if (Layers[l] != null) {
                    this.updateLayer(l, Layers[l]);
                }
            }

            this.postUpdate();
            this.processQueue();
            this.tickCount++;
        }
    }

    private void updateCurrentState(String state) {
        if (!this.currentState.equals(state)) {
            this.queueLogLine("Character.currentState changed from '" + this.currentState + "' to: '" + state + "'.", col_curstate);
            this.currentState = state;
        }
    }

    private void updateLayer(int index, AnimLayer layer) {
        AnimatorDebugMonitor.MonitoredLayer mLayer = this.monitoredLayers[index];
        String nodename = layer.getDebugNodeName();
        if (!mLayer.active) {
            mLayer.active = true;
            this.queueLogLine(AnimatorDebugMonitor.LogType.LAYER, "[layer][" + index + "] activated -> animstate: '" + nodename + "'.", col_layer_activated);
        }

        if (!mLayer.nodeName.equals(nodename)) {
            this.queueLogLine(
                AnimatorDebugMonitor.LogType.LAYER,
                "[layer][" + index + "] changed -> animstate from '" + mLayer.nodeName + "' to: '" + nodename + "'.",
                col_layer_nodename
            );
            mLayer.nodeName = nodename;
        }

        for (LiveAnimNode an : layer.getLiveAnimNodes()) {
            this.updateActiveNode(mLayer, an.getSourceNode().name);
        }

        if (layer.getAnimationTrack() != null) {
            for (AnimationTrack anmt : layer.getAnimationTrack().getTracks()) {
                if (anmt.getLayerIdx() == index) {
                    this.updateAnimTrack(mLayer, anmt.getName(), anmt.getBlendWeight());
                }
            }
        }

        mLayer.updated = true;
    }

    private void updateActiveNode(AnimatorDebugMonitor.MonitoredLayer l, String name) {
        AnimatorDebugMonitor.MonitoredNode node = l.activeNodes.get(name);
        if (node == null) {
            node = new AnimatorDebugMonitor.MonitoredNode();
            node.name = name;
            l.activeNodes.put(name, node);
        }

        if (!node.active) {
            node.active = true;
            this.queueLogLine(AnimatorDebugMonitor.LogType.NODE, "[layer][" + l.index + "] [active_nodes] : activated -> '" + name + "'.", col_node_activated);
        }

        node.updated = true;
    }

    private void updateAnimTrack(AnimatorDebugMonitor.MonitoredLayer l, String name, float blendDelta) {
        AnimatorDebugMonitor.MonitoredTrack track = l.animTracks.get(name);
        if (track == null) {
            track = new AnimatorDebugMonitor.MonitoredTrack();
            track.name = name;
            track.blendDelta = blendDelta;
            l.animTracks.put(name, track);
        }

        if (!track.active) {
            track.active = true;
            this.queueLogLine(AnimatorDebugMonitor.LogType.TRACK, "[layer][" + l.index + "] [anim_tracks] : activated -> '" + name + "'.", col_track_activated);
        }

        if (track.blendDelta != blendDelta) {
            track.blendDelta = blendDelta;
        }

        track.updated = true;
    }

    private void updateVariable(String key, String val) {
        AnimatorDebugMonitor.MonitoredVar var = this.monitoredVariables.get(key);
        boolean newvar = false;
        if (var == null) {
            var = new AnimatorDebugMonitor.MonitoredVar();
            this.monitoredVariables.put(key, var);
            newvar = true;
        }

        if (!var.active) {
            var.active = true;
            var.key = key;
            var.value = val;
            this.queueLogLine(AnimatorDebugMonitor.LogType.VAR, "[variable] : added -> '" + key + "', value: '" + val + "'.", col_var_activated);
            if (newvar) {
                registerVariable(key);
            }
        } else if (val == null) {
            if (var.isFloat) {
                var.isFloat = false;
                this.floatsListDirty = true;
            }

            var.value = null;
        } else if (var.value == null || !var.value.equals(val)) {
            try {
                float f = Float.parseFloat(val);
                var.logFloat(f);
                if (!var.isFloat) {
                    var.isFloat = true;
                    this.floatsListDirty = true;
                }
            } catch (NumberFormatException var6) {
                if (var.isFloat) {
                    var.isFloat = false;
                    this.floatsListDirty = true;
                }
            }

            if (!var.isFloat) {
                this.queueLogLine(
                    AnimatorDebugMonitor.LogType.VAR,
                    "[variable] : updated -> '" + key + "' changed from '" + var.value + "' to: '" + val + "'.",
                    col_var_changed
                );
            }

            var.value = val;
        }

        var.updated = true;
    }

    private void buildLogString() {
        ListIterator<AnimatorDebugMonitor.MonitorLogLine> li = this.logLines.listIterator(0);
        int outputLines = 0;
        int indexStart = 0;

        while (li.hasNext()) {
            AnimatorDebugMonitor.MonitorLogLine log = li.next();
            indexStart++;
            if (this.logFlags[log.type.value()]) {
                if (++outputLines >= 128) {
                    break;
                }
            }
        }

        if (indexStart == 0) {
            this.logString = "";
        } else {
            li = this.logLines.listIterator(indexStart);
            StringBuilder s = new StringBuilder();
            String prefix = " <TEXT> ";
            String suffix = " <LINE> ";

            while (li.hasPrevious()) {
                AnimatorDebugMonitor.MonitorLogLine log = li.previous();
                if (this.logFlags[log.type.value()]) {
                    s.append(" <TEXT> ");
                    if (this.doTickStamps) {
                        s.append("[");
                        s.append(String.format("%010d", log.tick));
                        s.append("]");
                    }

                    if (log.color != null) {
                        s.append(" <RGB:");
                        s.append(log.color.r);
                        s.append(",");
                        s.append(log.color.g);
                        s.append(",");
                        s.append(log.color.b);
                        s.append("> ");
                    }

                    s.append(log.line);
                    s.append(" <LINE> ");
                }
            }

            this.logString = s.toString();
            this.hasLogUpdates = false;
            this.hasFilterChanges = false;
        }
    }

    public boolean IsDirty() {
        return this.hasLogUpdates || this.hasFilterChanges;
    }

    public String getLogString() {
        if (this.hasLogUpdates || this.hasFilterChanges) {
            this.buildLogString();
        }

        return this.logString;
    }

    public boolean IsDirtyFloatList() {
        return this.floatsListDirty;
    }

    public ArrayList<String> getFloatNames() {
        this.floatsListDirty = false;
        ArrayList<String> names = new ArrayList<>();

        for (Entry<String, AnimatorDebugMonitor.MonitoredVar> entry : this.monitoredVariables.entrySet()) {
            if (entry.getValue().isFloat) {
                names.add(entry.getValue().key);
            }
        }

        Collections.sort(names);
        return names;
    }

    public static boolean isKnownVarsDirty() {
        return knownVarsDirty;
    }

    public static List<String> getKnownVariables() {
        knownVarsDirty = false;
        Collections.sort(knownVariables);
        return knownVariables;
    }

    public void setSelectedVariable(String key) {
        if (key == null) {
            this.selectedVariable = null;
        } else {
            this.selectedVariable = this.monitoredVariables.get(key);
        }
    }

    public String getSelectedVariable() {
        return this.selectedVariable != null ? this.selectedVariable.key : null;
    }

    public float getSelectedVariableFloat() {
        return this.selectedVariable != null ? this.selectedVariable.valFloat : 0.0F;
    }

    public String getSelectedVarMinFloat() {
        return this.selectedVariable != null && this.selectedVariable.isFloat && this.selectedVariable.min != -1.0F ? this.selectedVariable.min + "" : "-1.0";
    }

    public String getSelectedVarMaxFloat() {
        return this.selectedVariable != null && this.selectedVariable.isFloat && this.selectedVariable.max != -1.0F ? this.selectedVariable.max + "" : "1.0";
    }

    public ArrayList<Float> getSelectedVarFloatList() {
        if (this.selectedVariable != null && this.selectedVariable.isFloat) {
            AnimatorDebugMonitor.MonitoredVar v = this.selectedVariable;
            int index = v.index - 1;
            if (index < 0) {
                index = 0;
            }

            float max = v.max - v.min;

            for (int i = 0; i < 1024; i++) {
                float f = (v.floats[index--] - v.min) / max;
                this.floatsOut.set(i, f);
                if (index < 0) {
                    index = v.floats.length - 1;
                }
            }

            return this.floatsOut;
        } else {
            return null;
        }
    }

    public static void registerVariable(String key) {
        if (key != null) {
            key = key.toLowerCase();
            if (!knownVariables.contains(key)) {
                knownVariables.add(key);
                knownVarsDirty = true;
            }
        }
    }

    private void ensureLayers(AnimLayer[] Layers) {
        int size = Layers.length;
        if (this.monitoredLayers == null || this.monitoredLayers.length != size) {
            this.monitoredLayers = new AnimatorDebugMonitor.MonitoredLayer[size];

            for (int i = 0; i < size; i++) {
                this.monitoredLayers[i] = new AnimatorDebugMonitor.MonitoredLayer(i);
            }
        }
    }

    private static enum LogType {
        DEFAULT(0),
        LAYER(1),
        NODE(2),
        TRACK(3),
        VAR(4),
        MAX(5);

        private final int val;

        private LogType(final int value) {
            this.val = value;
        }

        public int value() {
            return this.val;
        }
    }

    private class MonitorLogLine {
        String line;
        Color color;
        AnimatorDebugMonitor.LogType type;
        int tick;

        private MonitorLogLine() {
            Objects.requireNonNull(AnimatorDebugMonitor.this);
            super();
            this.type = AnimatorDebugMonitor.LogType.DEFAULT;
        }
    }

    private class MonitoredLayer {
        int index;
        String nodeName;
        HashMap<String, AnimatorDebugMonitor.MonitoredNode> activeNodes;
        HashMap<String, AnimatorDebugMonitor.MonitoredTrack> animTracks;
        boolean active;
        boolean updated;

        public MonitoredLayer(final int idx) {
            Objects.requireNonNull(AnimatorDebugMonitor.this);
            super();
            this.nodeName = "";
            this.activeNodes = new HashMap<>();
            this.animTracks = new HashMap<>();
            this.index = idx;
        }
    }

    private class MonitoredNode {
        String name;
        boolean active;
        boolean updated;

        private MonitoredNode() {
            Objects.requireNonNull(AnimatorDebugMonitor.this);
            super();
            this.name = "";
        }
    }

    private class MonitoredTrack {
        String name;
        float blendDelta;
        boolean active;
        boolean updated;

        private MonitoredTrack() {
            Objects.requireNonNull(AnimatorDebugMonitor.this);
            super();
            this.name = "";
        }
    }

    private class MonitoredVar {
        String key;
        String value;
        boolean isFloat;
        float valFloat;
        boolean active;
        boolean updated;
        float[] floats;
        int index;
        float min;
        float max;

        private MonitoredVar() {
            Objects.requireNonNull(AnimatorDebugMonitor.this);
            super();
            this.key = "";
            this.value = "";
            this.min = -1.0F;
            this.max = 1.0F;
        }

        public void logFloat(float f) {
            if (this.floats == null) {
                this.floats = new float[1024];
            }

            if (f != this.valFloat) {
                this.valFloat = f;
                this.floats[this.index++] = f;
                if (f < this.min) {
                    this.min = f;
                }

                if (f > this.max) {
                    this.max = f;
                }

                if (this.index >= 1024) {
                    this.index = 0;
                }
            }
        }
    }
}
