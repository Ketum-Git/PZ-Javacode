// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import org.w3c.dom.Element;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.CharacterActionAnims;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.Pool;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

/**
 * Created by LEMMYMAIN on 26/01/2015.
 */
public final class AdvancedAnimator implements IAnimEventCallback {
    private IAnimatable character;
    public AnimationSet animSet;
    public final ArrayList<IAnimEventCallback> animCallbackHandlers = new ArrayList<>();
    private AnimLayer rootLayer;
    private final List<SubLayerSlot> subLayers = new ArrayList<>();
    public static float motionScale = 0.76F;
    public static float rotationScale = 0.76F;
    private static AnimatorDebugMonitor debugMonitor;
    private static long animSetModificationTime = -1L;
    private static long actionGroupModificationTime = -1L;
    private final AnimationVariableWhileAliveFlagsContainer setFlagCounters = new AnimationVariableWhileAliveFlagsContainer();
    private AnimationPlayerRecorder recorder;
    private final TransitionNodeProxy transitionNodeProxy = new TransitionNodeProxy();

    public static void systemInit() {
        DebugFileWatcher.instance
            .add(new PredicatedFileWatcher("media/AnimSets", AdvancedAnimator::isAnimSetFilePath, AdvancedAnimator::onAnimSetsRefreshTriggered));
        DebugFileWatcher.instance
            .add(new PredicatedFileWatcher("media/actiongroups", AdvancedAnimator::isActionGroupFilePath, AdvancedAnimator::onActionGroupsRefreshTriggered));
        LoadDefaults();
    }

    private static boolean isAnimSetFilePath(String path) {
        if (path == null) {
            return false;
        } else if (!path.endsWith(".xml")) {
            return false;
        } else {
            ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

            for (int i = 0; i < modIDs.size(); i++) {
                String modID = modIDs.get(i);
                ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modID);
                if (mod != null
                    && mod.animSetsFile != null
                    && mod.animSetsFile.common.canonicalFile != null
                    && path.startsWith(mod.animSetsFile.common.canonicalFile.getPath())) {
                    return true;
                }

                if (mod != null
                    && mod.animSetsFile != null
                    && mod.animSetsFile.version.canonicalFile != null
                    && path.startsWith(mod.animSetsFile.version.canonicalFile.getPath())) {
                    return true;
                }
            }

            String animSetsPath = ZomboidFileSystem.instance.getAnimSetsPath();
            return path.startsWith(animSetsPath);
        }
    }

    private static boolean isActionGroupFilePath(String path) {
        if (path == null) {
            return false;
        } else if (!path.endsWith(".xml")) {
            return false;
        } else {
            ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();

            for (int i = 0; i < modIDs.size(); i++) {
                String modID = modIDs.get(i);
                ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modID);
                if (mod != null
                    && mod.actionGroupsFile != null
                    && mod.actionGroupsFile.common.canonicalFile != null
                    && path.startsWith(mod.actionGroupsFile.common.canonicalFile.getPath())) {
                    return true;
                }

                if (mod != null
                    && mod.actionGroupsFile != null
                    && mod.actionGroupsFile.version.canonicalFile != null
                    && path.startsWith(mod.actionGroupsFile.version.canonicalFile.getPath())) {
                    return true;
                }
            }

            String actionGroupsPath = ZomboidFileSystem.instance.getActionGroupsPath();
            return path.startsWith(actionGroupsPath);
        }
    }

    private static void onActionGroupsRefreshTriggered(String entryKey) {
        DebugLog.General.println("DebugFileWatcher Hit. ActionGroups: " + entryKey);
        actionGroupModificationTime = System.currentTimeMillis() + 1000L;
    }

    private static void onAnimSetsRefreshTriggered(String entryKey) {
        DebugLog.General.println("DebugFileWatcher Hit. AnimSets: " + entryKey);
        animSetModificationTime = System.currentTimeMillis() + 1000L;
    }

    public static void checkModifiedFiles() {
        if (animSetModificationTime != -1L && animSetModificationTime < System.currentTimeMillis()) {
            DebugLog.General.println("Refreshing AnimSets.");
            animSetModificationTime = -1L;
            LoadDefaults();
            LuaManager.GlobalObject.refreshAnimSets(true);
        }

        if (actionGroupModificationTime != -1L && actionGroupModificationTime < System.currentTimeMillis()) {
            DebugLog.General.println("Refreshing action groups.");
            actionGroupModificationTime = -1L;
            LuaManager.GlobalObject.reloadActionGroups();
        }
    }

    private static void LoadDefaults() {
        try {
            Element rootXml = PZXmlUtil.parseXml("media/AnimSets/Defaults.xml");
            String mx = rootXml.getElementsByTagName("MotionScale").item(0).getTextContent();
            motionScale = Float.parseFloat(mx);
            String r = rootXml.getElementsByTagName("RotationScale").item(0).getTextContent();
            rotationScale = Float.parseFloat(r);
        } catch (PZXmlParserException var3) {
            DebugLog.General.error("Exception thrown: " + var3);
            var3.printStackTrace();
        }
    }

    public String GetDebug() {
        StringBuilder debug = new StringBuilder();
        debug.append("GameState: ");
        if (this.character instanceof IsoGameCharacter character) {
            debug.append(character.getCurrentState() == null ? "null" : character.getCurrentState().getClass().getSimpleName()).append("\n");
        }

        if (this.rootLayer != null) {
            debug.append("Layer: ").append(0).append("\n");
            debug.append(this.rootLayer.GetDebugString()).append("\n");
        }

        for (int i = 0; i < this.subLayers.size(); i++) {
            SubLayerSlot slot = this.subLayers.get(i);
            if (slot.shouldBeActive) {
                debug.append("SubLayer: ").append(i).append("\n");
                debug.append(slot.animLayer.GetDebugString()).append("\n");
            }
        }

        debug.append("Variables:\n");
        debug.append("Weapon: ").append(this.character.getVariableString("weapon")).append("\n");
        debug.append("Aim: ").append(this.character.getVariableString("aim")).append("\n");
        ArrayList<IAnimationVariableSlot> sorted = new ArrayList<>();

        for (IAnimationVariableSlot entry : this.character.getGameVariables()) {
            sorted.add(entry);
        }

        sorted.sort(Comparator.comparing(IAnimationVariableSlot::getKey));

        for (IAnimationVariableSlot entry : sorted) {
            debug.append("  ").append(entry.getKey()).append(" : ").append(entry.getValueString()).append("\n");
        }

        sorted.clear();
        return debug.toString();
    }

    public void OnAnimDataChanged(boolean reload) {
        if (reload && this.character instanceof IsoGameCharacter character) {
            character.getStateMachine().activeStateChanged++;
            character.setDefaultState();
            if (character instanceof IsoZombie) {
                character.setOnFloor(false);
            }

            character.getStateMachine().activeStateChanged--;
        }

        this.setAnimSet(AnimationSet.GetAnimationSet(this.character.GetAnimSetName(), false));
        if (this.character.getAnimationPlayer() != null) {
            this.character.getAnimationPlayer().reset();
        }

        if (this.rootLayer != null) {
            this.rootLayer.reset();
        }

        for (int i = 0; i < this.subLayers.size(); i++) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.reset();
        }
    }

    public void reset() {
        if (this.rootLayer != null) {
            this.rootLayer.reset();
        }

        for (int i = 0; i < this.subLayers.size(); i++) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.reset();
        }
    }

    public void Reload() {
    }

    public void init(IAnimatable character) {
        this.character = character;
        this.rootLayer = AnimLayer.alloc(character, this);
    }

    public void setAnimSet(AnimationSet aset) {
        this.animSet = aset;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        this.invokeAnimEvent(sender, track, event);
    }

    private void invokeAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        for (int i = 0; i < this.animCallbackHandlers.size(); i++) {
            IAnimEventCallback callback = this.animCallbackHandlers.get(i);
            callback.OnAnimEvent(sender, track, event);
        }
    }

    public void invokeGlobalAnimEvent(AnimEvent event) {
        if (this.isRecording()) {
            this.logGlobalAnimEvent(event);
        }

        this.invokeAnimEvent(null, null, event);
    }

    private void logGlobalAnimEvent(AnimEvent evt) {
        AnimationPlayerRecorder recorder = this.recorder;
        recorder.logGlobalAnimEvent(evt);
    }

    public String getCurrentStateName() {
        return this.rootLayer == null ? null : this.rootLayer.getCurrentStateName();
    }

    public boolean containsState(String stateName) {
        return this.animSet != null && this.animSet.containsState(stateName);
    }

    public AnimLayer findLayerWithState(AnimState in_state) {
        if (this.rootLayer.isCurrentState(in_state)) {
            return this.rootLayer;
        } else {
            int i = 0;

            for (int subStateCount = this.subLayers.size(); i < subStateCount; i++) {
                SubLayerSlot subLayerSlot = this.subLayers.get(i);
                if (subLayerSlot.animLayer != null && subLayerSlot.animLayer.isCurrentState(in_state)) {
                    return subLayerSlot.animLayer;
                }
            }

            return null;
        }
    }

    public final void setState(String stateName) {
        this.setState(stateName, PZArrayList.emptyList());
    }

    public void setState(String stateName, List<String> subStateNames) {
        if (this.animSet == null) {
            DebugLog.Animation.error("(" + stateName + ") Cannot set state. AnimSet is null.");
        } else {
            if (!this.animSet.containsState(stateName)) {
                DebugLog.Animation.error("State not found: " + stateName);
            }

            AnimState newState = this.animSet.GetState(stateName);
            AnimLayer sourceLayer = this.findLayerWithState(newState);
            this.rootLayer.transitionTo(newState, sourceLayer);
            List<SubLayerSlot> subLayers = this.subLayers;
            PZArrayUtil.forEach(subLayers, subLayerx -> subLayerx.shouldBeActive = false);
            DebugLog.AnimationDetailed.debugln("*** SetState: <%s>", stateName);
            sourceLayer = this.rootLayer;

            for (int iSubStateNameIdx = 0; iSubStateNameIdx < subStateNames.size(); iSubStateNameIdx++) {
                String subStateName = subStateNames.get(iSubStateNameIdx);
                DebugLog.AnimationDetailed.debugln("  SetSubState: <%s>", subStateName);
                int subLayerIdx = this.getOrCreateSlot(subLayers, sourceLayer, subStateName);
                AnimState subState = this.animSet.GetState(subStateName);
                AnimLayer sourceLayerx = this.findLayerWithState(subState);
                SubLayerSlot subLayer = subLayers.get(subLayerIdx);
                subLayer.setParentLayer(sourceLayer);
                subLayer.transitionTo(subState, sourceLayerx);
                if (subLayerIdx != iSubStateNameIdx) {
                    SubLayerSlot temp = subLayers.get(iSubStateNameIdx);
                    subLayers.set(iSubStateNameIdx, subLayer);
                    subLayers.set(subLayerIdx, temp);
                }

                sourceLayer = subLayer.animLayer;
            }

            if (subStateNames.isEmpty()) {
                DebugLog.AnimationDetailed.debugln("  SetSubState: NoneToSet");
            }

            PZArrayUtil.forEach(subLayers, SubLayerSlot::applyTransition);

            while (!subLayers.isEmpty()) {
                int trailingSlotIdx = subLayers.size() - 1;
                SubLayerSlot trailingSlot = subLayers.get(trailingSlotIdx);
                if (!trailingSlot.isStateless() || trailingSlot.hasRunningAnims()) {
                    break;
                }

                Pool.tryRelease(trailingSlot);
                subLayers.remove(trailingSlotIdx);
            }
        }
    }

    private int getOrCreateSlot(List<SubLayerSlot> in_subLayers, AnimLayer in_parentLayer, String in_animStateName) {
        int foundLayerIdx = -1;
        int i = 0;

        for (int count = in_subLayers.size(); i < count; i++) {
            SubLayerSlot subLayer = in_subLayers.get(i);
            if (subLayer.animLayer.isCurrentState(in_animStateName)) {
                foundLayerIdx = i;
                break;
            }
        }

        if (foundLayerIdx > -1) {
            return foundLayerIdx;
        } else {
            i = 0;

            for (int countx = in_subLayers.size(); i < countx; i++) {
                SubLayerSlot subLayer = in_subLayers.get(i);
                if (subLayer.isStateless()) {
                    foundLayerIdx = i;
                    break;
                }
            }

            if (foundLayerIdx > -1) {
                return foundLayerIdx;
            } else {
                SubLayerSlot newLayer = SubLayerSlot.alloc(in_parentLayer, this.character, this);
                in_subLayers.add(newLayer);
                return in_subLayers.size() - 1;
            }
        }
    }

    public void update(float in_deltaT) {
        try (GameProfiler.ProfileArea ignored = GameProfiler.getInstance().profile("AdvancedAnimator.Update")) {
            this.updateInternal(in_deltaT);
        }
    }

    private void updateInternal(float in_deltaT) {
        if (this.character.getAnimationPlayer() != null) {
            if (this.character.getAnimationPlayer().isReady()) {
                if (this.animSet != null) {
                    if (!this.rootLayer.hasState()) {
                        this.rootLayer.transitionTo(this.animSet.GetState("Idle"), true);
                    }

                    this.rootLayer.updateLiveAnimNodes();

                    for (int i = 0; i < this.subLayers.size(); i++) {
                        SubLayerSlot subLayer = this.subLayers.get(i);
                        subLayer.animLayer.updateLiveAnimNodes();
                    }

                    this.GenerateTransitionData();
                    this.rootLayer.Update(in_deltaT);

                    for (int i = 0; i < this.subLayers.size(); i++) {
                        SubLayerSlot subLayer = this.subLayers.get(i);
                        subLayer.update(in_deltaT);
                    }

                    if (debugMonitor != null && this.character instanceof IsoGameCharacter isoGameCharacter) {
                        if (debugMonitor.getTarget() != this.character) {
                            return;
                        }

                        int count = 1 + this.getActiveSubLayerCount();
                        AnimLayer[] layers = new AnimLayer[count];
                        layers[0] = this.rootLayer;
                        count = 0;

                        for (int i = 0; i < this.subLayers.size(); i++) {
                            SubLayerSlot subLayer = this.subLayers.get(i);
                            if (subLayer.shouldBeActive) {
                                layers[1 + count] = subLayer.animLayer;
                                count++;
                            }
                        }

                        debugMonitor.update(isoGameCharacter, layers);
                    }
                }
            }
        }
    }

    private void GenerateTransitionData() {
        TransitionNodeProxy proxy = this.transitionNodeProxy;
        proxy.reset();
        this.rootLayer.FindTransitioningLiveAnimNode(proxy, true);

        for (int i = 0; i < this.subLayers.size(); i++) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.FindTransitioningLiveAnimNode(proxy, false);
        }

        if (!proxy.allNewNodes.isEmpty() || !proxy.allOutgoingNodes.isEmpty()) {
            DebugLog.AnimationDetailed.debugln("************* New Nodes *************");

            for (int i = 0; i < proxy.allNewNodes.size(); i++) {
                DebugLog.AnimationDetailed.debugln("  %s", proxy.allNewNodes.get(i).liveAnimNode.getName());
            }

            DebugLog.AnimationDetailed.debugln("************* Out Nodes *************");

            for (int i = 0; i < proxy.allOutgoingNodes.size(); i++) {
                DebugLog.AnimationDetailed.debugln("  %s", proxy.allOutgoingNodes.get(i).liveAnimNode.getName());
            }

            DebugLog.AnimationDetailed.debugln("*************************************");
        }

        if (proxy.HasAnyPossibleTransitions()) {
            this.FindTransitionsFromProxy(proxy);
            this.ProcessTransitions(proxy);
        }
    }

    public void FindTransitionsFromProxy(TransitionNodeProxy proxy) {
        for (int i = 0; i < proxy.allNewNodes.size(); i++) {
            TransitionNodeProxy.NodeLayerPair toNodePair = proxy.allNewNodes.get(i);
            AnimNode toNode = toNodePair.liveAnimNode.getSourceNode();

            for (int j = 0; i < proxy.allOutgoingNodes.size(); i++) {
                TransitionNodeProxy.NodeLayerPair fromNodePair = proxy.allOutgoingNodes.get(i);
                if (toNode != fromNodePair.liveAnimNode.getSourceNode()) {
                    AnimTransition animTransition = fromNodePair.liveAnimNode.findTransitionTo(this.character, toNodePair.liveAnimNode.getSourceNode());
                    if (animTransition != null) {
                        TransitionNodeProxy.TransitionNodeProxyData transitionData = proxy.allocTransitionNodeProxyData();
                        transitionData.animLayerIn = toNodePair.animLayer;
                        transitionData.newAnimNode = toNodePair.liveAnimNode;
                        transitionData.animLayerOut = fromNodePair.animLayer;
                        transitionData.oldAnimNode = fromNodePair.liveAnimNode;
                        transitionData.transitionOut = animTransition;
                        proxy.foundTransitions.add(transitionData);
                        DebugLog.AnimationDetailed
                            .debugln(
                                "** NEW ** Anim: <%s>; <%s>; this: <%s>",
                                transitionData.newAnimNode.getName(),
                                transitionData.transitionOut != null ? "true" : "false",
                                this.toString()
                            );
                    }
                }
            }
        }
    }

    public void ProcessTransitions(TransitionNodeProxy proxy) {
        for (int i = 0; i < proxy.foundTransitions.size(); i++) {
            TransitionNodeProxy.TransitionNodeProxyData transition = proxy.foundTransitions.get(i);
            AnimationTrack transitionTrack = transition.animLayerOut.startTransitionAnimation(transition);
            transition.newAnimNode.startTransitionIn(transition.oldAnimNode, transition.transitionOut, transitionTrack);
            transition.oldAnimNode.setTransitionOut(transition.transitionOut);
        }
    }

    public void render() {
        if (this.character.getAnimationPlayer() != null) {
            if (this.character.getAnimationPlayer().isReady()) {
                if (this.animSet != null) {
                    if (this.rootLayer.hasState()) {
                        this.rootLayer.render();
                    }
                }
            }
        }
    }

    public void printDebugCharacterActions(String target) {
        if (this.animSet != null) {
            AnimState state = this.animSet.GetState("actions");
            if (state != null) {
                boolean isTarg = false;
                boolean targFound = false;

                for (CharacterActionAnims act : CharacterActionAnims.values()) {
                    isTarg = false;
                    String actname;
                    if (act == CharacterActionAnims.None) {
                        actname = target;
                        isTarg = true;
                    } else {
                        actname = act.toString();
                    }

                    boolean found = false;

                    for (AnimNode node : state.nodes) {
                        for (AnimCondition con : node.conditions) {
                            if (con.type == AnimCondition.Type.STRING
                                && con.name.equalsIgnoreCase("performingaction")
                                && con.stringValue.equalsIgnoreCase(actname)) {
                                found = true;
                                break;
                            }
                        }

                        if (found) {
                            break;
                        }
                    }

                    if (found) {
                        if (isTarg) {
                            targFound = true;
                        }
                    } else {
                        DebugLog.log("WARNING: did not find node with condition 'PerformingAction = " + actname + "' in player/actions/");
                    }
                }

                if (targFound) {
                    if (DebugLog.isEnabled(DebugType.Animation)) {
                        DebugLog.Animation.debugln("SUCCESS - Current 'actions' TargetNode: '" + target + "' was found.");
                    }
                } else if (DebugLog.isEnabled(DebugType.Animation)) {
                    DebugLog.Animation.debugln("FAIL - Current 'actions' TargetNode: '" + target + "' not found.");
                }
            }
        }
    }

    public ArrayList<String> debugGetVariables() {
        ArrayList<String> vars = new ArrayList<>();
        if (this.animSet != null) {
            for (Entry<String, AnimState> entry : this.animSet.states.entrySet()) {
                AnimState state = entry.getValue();

                for (AnimNode node : state.nodes) {
                    for (AnimCondition con : node.conditions) {
                        if (con.name != null && !vars.contains(con.name.toLowerCase())) {
                            vars.add(con.name.toLowerCase());
                        }
                    }
                }
            }
        }

        return vars;
    }

    public AnimatorDebugMonitor getDebugMonitor() {
        return debugMonitor;
    }

    public void setDebugMonitor(AnimatorDebugMonitor monitor) {
        debugMonitor = monitor;
    }

    public IAnimatable getCharacter() {
        return this.character;
    }

    public void updateSpeedScale(String variable, float newSpeed) {
        if (this.rootLayer != null) {
            List<LiveAnimNode> liveAnimNodes = this.rootLayer.getLiveAnimNodes();

            for (int i = 0; i < liveAnimNodes.size(); i++) {
                LiveAnimNode node = liveAnimNodes.get(i);
                if (node.isActive() && node.getSourceNode() != null && variable.equals(node.getSourceNode().speedScaleVariable)) {
                    node.getSourceNode().speedScale = newSpeed + "";

                    for (int j = 0; j < node.getMainAnimationTracksCount(); j++) {
                        node.getMainAnimationTrackAt(j).setSpeedDelta(newSpeed);
                    }
                }
            }
        }
    }

    /**
     * Returns TRUE if any Actuve Live nodes are an Idle animation.
     *  This is useful when determining if the character is currently Idle.
     *  
     *  eg. For adding variations to standing around, like fidgeting, sneezing, etc.
     */
    public boolean containsAnyIdleNodes() {
        if (this.rootLayer == null) {
            return false;
        } else {
            boolean isIdle = false;
            List<LiveAnimNode> liveAnimNodes = this.rootLayer.getLiveAnimNodes();

            for (int i = 0; i < liveAnimNodes.size() && !isIdle; i++) {
                isIdle = liveAnimNodes.get(i).isIdleAnimActive();
            }

            for (int j = 0; j < this.getSubLayerCount(); j++) {
                AnimLayer subLayer = this.getSubLayerAt(j);
                liveAnimNodes = subLayer.getLiveAnimNodes();

                for (int i = 0; i < liveAnimNodes.size(); i++) {
                    isIdle = liveAnimNodes.get(i).isIdleAnimActive();
                    if (!isIdle) {
                        break;
                    }
                }
            }

            return isIdle;
        }
    }

    public AnimLayer getRootLayer() {
        return this.rootLayer;
    }

    public int getSubLayerCount() {
        return this.subLayers.size();
    }

    public AnimLayer getSubLayerAt(int idx) {
        return this.subLayers.get(idx).animLayer;
    }

    public int getActiveSubLayerCount() {
        int count = 0;

        for (int i = 0; i < this.subLayers.size(); i++) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            if (subLayer.shouldBeActive) {
                count++;
            }
        }

        return count;
    }

    public void setRecorder(AnimationPlayerRecorder recorder) {
        this.recorder = recorder;
    }

    public boolean isRecording() {
        return this.recorder != null && this.recorder.isRecording();
    }

    public void incrementWhileAliveFlag(AnimationVariableReference in_variableReference, boolean in_whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.incrementWhileAliveFlag(in_variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", in_variableReference, stillAliveCounter);
        in_variableReference.setVariable(this.getCharacter(), stillAliveCounter > 0 ? in_whileAliveValue : !in_whileAliveValue);
    }

    public void decrementWhileAliveFlag(AnimationVariableReference in_variableReference, boolean in_whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.decrementWhileAliveFlag(in_variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", in_variableReference, stillAliveCounter);
        in_variableReference.setVariable(this.getCharacter(), stillAliveCounter > 0 ? in_whileAliveValue : !in_whileAliveValue);
    }
}
