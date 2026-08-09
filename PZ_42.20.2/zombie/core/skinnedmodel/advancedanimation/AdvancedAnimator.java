// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.GlobalAnimEvent;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

/**
 * Created by LEMMYMAIN on 26/01/2015.
 */
public final class AdvancedAnimator implements IAnimEventCallback {
    private IAnimatable character;
    public AnimationSet animSet;
    private final Set<IAnimEventCallback> animCallbackHandlers = new HashSet<>();
    private AnimLayer rootLayer;
    private final List<SubLayerSlot> subLayers = new ArrayList<>();
    private AnimState rootState;
    private final List<AnimState> subStates = new ArrayList<>();
    public static float motionScale = 0.76F;
    public static float rotationScale = 0.76F;
    private static AnimatorDebugMonitor debugMonitor;
    private static long animSetModificationTime = -1L;
    private static long actionGroupModificationTime = -1L;
    private final AnimationVariableWhileAliveFlagsContainer setFlagCounters = new AnimationVariableWhileAliveFlagsContainer();
    private static String checksum = "";
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
        DebugType.General.println("DebugFileWatcher Hit. ActionGroups: " + entryKey);
        actionGroupModificationTime = System.currentTimeMillis() + 1000L;
    }

    private static void onAnimSetsRefreshTriggered(String entryKey) {
        DebugType.General.println("DebugFileWatcher Hit. AnimSets: " + entryKey);
        animSetModificationTime = System.currentTimeMillis() + 1000L;
    }

    public static void checkModifiedFiles() {
        if (animSetModificationTime != -1L && animSetModificationTime < System.currentTimeMillis()) {
            DebugType.General.println("Refreshing AnimSets.");
            animSetModificationTime = -1L;
            LoadDefaults();
            LuaManager.GlobalObject.refreshAnimSets(true);
        }

        if (actionGroupModificationTime != -1L && actionGroupModificationTime < System.currentTimeMillis()) {
            DebugType.General.println("Refreshing action groups.");
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
            DebugType.General.printException(var3, LogSeverity.Error, "Exception thrown. %s", var3);
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
            debug.append("SubLayer: ").append(i).append("\n");
            debug.append(slot.animLayer.GetDebugString()).append("\n");
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

    public void init(IAnimatable character, String animationSetName) {
        this.character = character;
        this.rootLayer = AnimLayer.alloc(character, this);
        if (character instanceof IAnimEventCallback animEventListener) {
            this.addAnimCallback(animEventListener);
        }

        if (!StringUtils.isNullOrWhitespace(animationSetName)) {
            this.setAnimSet(AnimationSet.GetAnimationSet(animationSetName, false));
        }
    }

    public void setAnimSet(AnimationSet aset) {
        this.animSet = aset;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        this.invokeAnimEvent(sender, track, event);
    }

    private void invokeAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        for (IAnimEventCallback callback : this.animCallbackHandlers) {
            callback.OnAnimEvent(sender, track, event);
        }
    }

    public void invokeGlobalAnimEvent(GlobalAnimEvent event) {
        if (this.isRecording()) {
            this.logGlobalAnimEvent(event);
        }

        this.invokeAnimEvent(null, null, event.getAnimEvent());
    }

    private void logGlobalAnimEvent(GlobalAnimEvent evt) {
        this.character.getAnimationRecorder().logGlobalAnimEvent(this.character, evt);
    }

    public String getCurrentStateName() {
        return this.rootLayer == null ? null : this.rootLayer.getCurrentStateName();
    }

    public boolean containsState(String stateName) {
        return this.animSet != null && this.animSet.containsState(stateName);
    }

    private SubLayerSlot findSubLayerWithState(List<SubLayerSlot> subLayers, AnimState state) {
        SubLayerSlot foundSubLayerSlot = null;
        int i = 0;

        for (int subStateCount = subLayers.size(); i < subStateCount; i++) {
            SubLayerSlot subLayerSlot = subLayers.get(i);
            if (subLayerSlot.isNextOrCurrentState(state)) {
                foundSubLayerSlot = subLayerSlot;
                break;
            }
        }

        return foundSubLayerSlot;
    }

    private SubLayerSlot findInactiveSubLayerWithDesiredLayer(List<SubLayerSlot> subLayers, int desiredLayer) {
        SubLayerSlot foundSubLayerSlot = null;

        for (int i = subLayers.size() - 1; i >= 0; i--) {
            SubLayerSlot subLayerSlot = subLayers.get(i);
            if (!subLayerSlot.shouldBeActive && subLayerSlot.desiredLayer == desiredLayer) {
                foundSubLayerSlot = subLayerSlot;
                break;
            }
        }

        return foundSubLayerSlot;
    }

    public void setState(String stateName) {
        this.setState(stateName, PZArrayList.emptyList());
    }

    public void setState(String stateName, List<String> subStateNames) {
        if (this.animSet == null) {
            DebugType.Animation.error("(%s) Cannot set state. AnimSet is null.", stateName);
        } else {
            this.rootState = this.animSet.GetState(stateName);
            PZArrayUtil.copy(this.subStates, subStateNames, this.animSet::GetState);
            this.setState(this.rootState, this.subStates);
        }
    }

    public void setState(AnimState rootState, List<AnimState> subStates) {
        if (!this.isCurrentState(rootState, subStates)) {
            if (!this.rootLayer.isCurrentState(rootState)) {
                this.rootLayer.transitionTo(rootState);
            }

            this.cleanUpEmptyLayers(this.subLayers);
            PZArrayUtil.forEach(this.subLayers, SubLayerSlot::clearShouldBeActiveFlag);

            for (int iSubStateIdx = 0; iSubStateIdx < subStates.size(); iSubStateIdx++) {
                SubLayerSlot existingSlot = this.findSubLayerWithState(this.subLayers, subStates.get(iSubStateIdx));
                if (existingSlot != null) {
                    existingSlot.shouldBeActive = true;
                    existingSlot.desiredLayer = iSubStateIdx;
                }
            }

            for (int iSubStateIdxx = 0; iSubStateIdxx < subStates.size(); iSubStateIdxx++) {
                AnimState subState = subStates.get(iSubStateIdxx);
                SubLayerSlot existingSlot = this.findSubLayerWithState(this.subLayers, subState);
                if (existingSlot == null) {
                    SubLayerSlot existingInactiveSlot = this.findInactiveSubLayerWithDesiredLayer(this.subLayers, iSubStateIdxx);
                    if (existingInactiveSlot != null) {
                        existingInactiveSlot.setNextTransitionTo(subState);
                        existingInactiveSlot.shouldBeActive = true;
                    } else {
                        SubLayerSlot newSlot = SubLayerSlot.alloc(this.character, this);
                        newSlot.setNextTransitionTo(subState);
                        newSlot.shouldBeActive = true;
                        newSlot.desiredLayer = iSubStateIdxx;
                        this.subLayers.add(newSlot);
                    }
                }
            }

            PZArrayUtil.sort(this.subLayers, SubLayerSlot::compare);
            this.ensureLayerParents(this.subLayers);
            PZArrayUtil.forEach(this.subLayers, SubLayerSlot::applyNextTransition);
            this.cleanUpEmptyLayers(this.subLayers);
            if (DebugType.AnimationLayers.isEnabled(LogSeverity.Debug)) {
                DebugType.AnimationLayers.debugln("States");
                DebugType.AnimationLayers.debugln("+++ rootState: %s", rootState.name);
                PZArrayUtil.forEach(subStates, subStatex -> DebugType.AnimationLayers.debugln("+++ subState: %s", subStatex.name));
                DebugType.AnimationLayers.debugln("------------------------------------------------");
                DebugType.AnimationLayers.debugln("Layers");
                DebugType.AnimationLayers.debugln("*** rootLayer: %s", this.rootLayer.getCurrentStateName());
                PZArrayUtil.forEach(
                    this.subLayers,
                    subSlot -> DebugType.AnimationLayers
                        .debugln(
                            "*** Layer %d. DesiredLayer: %d State: %s ActiveNodes: %d",
                            subSlot.animLayer.getDepth(),
                            subSlot.desiredLayer,
                            subSlot.animLayer.getCurrentStateName(),
                            subSlot.animLayer.getLiveAnimNodes().size()
                        )
                );
                DebugType.AnimationLayers.debugln("------------------------------------------------");
            }
        }
    }

    private boolean isCurrentState(AnimState rootState, List<AnimState> subStates) {
        if (!this.rootLayer.isCurrentState(rootState)) {
            return false;
        } else {
            int iMaxDesiredLayer = 0;

            for (int iLayerSlotIdx = 0; iLayerSlotIdx < this.subLayers.size(); iLayerSlotIdx++) {
                SubLayerSlot existingSlot = this.subLayers.get(iLayerSlotIdx);
                if (existingSlot.shouldBeActive) {
                    if (existingSlot.desiredLayer >= subStates.size()) {
                        return false;
                    }

                    if (!existingSlot.isNextOrCurrentState(subStates.get(existingSlot.desiredLayer))) {
                        return false;
                    }

                    iMaxDesiredLayer = PZMath.max(existingSlot.desiredLayer, iMaxDesiredLayer);
                }
            }

            return iMaxDesiredLayer == subStates.size();
        }
    }

    private void ensureLayerParents(List<SubLayerSlot> subLayerSlots) {
        AnimLayer parentLayer = this.rootLayer;

        for (int isSubState = 0; isSubState < subLayerSlots.size(); isSubState++) {
            SubLayerSlot subLayerSlot = subLayerSlots.get(isSubState);
            subLayerSlot.setParentLayer(parentLayer);
            parentLayer = subLayerSlot.animLayer;
        }
    }

    public void update(float deltaT) {
        try (GameProfiler.ProfileArea var2 = GameProfiler.getInstance().profile("AdvancedAnimator.Update")) {
            this.updateInternal(deltaT);
        }
    }

    private void updateInternal(float deltaT) {
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
                    this.rootLayer.Update(deltaT);

                    for (int i = 0; i < this.subLayers.size(); i++) {
                        SubLayerSlot subLayer = this.subLayers.get(i);
                        subLayer.update(deltaT);
                    }

                    this.cleanUpEmptyLayers(this.subLayers);
                    if (debugMonitor != null && this.character instanceof IsoGameCharacter isoGameCharacter) {
                        if (debugMonitor.getTarget() != this.character) {
                            return;
                        }

                        int count = 1 + this.subLayers.size();
                        AnimLayer[] layers = new AnimLayer[count];
                        layers[0] = this.rootLayer;

                        for (int i = 0; i < this.subLayers.size(); i++) {
                            SubLayerSlot subLayer = this.subLayers.get(i);
                            layers[1 + i] = subLayer.animLayer;
                        }

                        debugMonitor.update(isoGameCharacter, layers);
                    }
                }
            }
        }
    }

    private void cleanUpEmptyLayers(List<SubLayerSlot> subLayers) {
        for (int i = 0; i < subLayers.size(); i++) {
            SubLayerSlot subLayer = subLayers.get(i);
            if (!subLayer.shouldBeActive && subLayer.animLayer.isStateless() && !subLayer.hasRunningAnims()) {
                subLayers.remove(i--);
                Pool.tryRelease(subLayer);
            }
        }

        this.ensureLayerParents(subLayers);
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
            DebugType.AnimationDetailed.debugln("************* New Nodes *************");

            for (int i = 0; i < proxy.allNewNodes.size(); i++) {
                DebugType.AnimationDetailed.debugln("  %s", proxy.allNewNodes.get(i).liveAnimNode.getName());
            }

            DebugType.AnimationDetailed.debugln("************* Out Nodes *************");

            for (int i = 0; i < proxy.allOutgoingNodes.size(); i++) {
                DebugType.AnimationDetailed.debugln("  %s", proxy.allOutgoingNodes.get(i).liveAnimNode.getName());
            }

            DebugType.AnimationDetailed.debugln("*************************************");
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

            for (int j = 0; j < proxy.allOutgoingNodes.size(); j++) {
                TransitionNodeProxy.NodeLayerPair fromNodePair = proxy.allOutgoingNodes.get(j);
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
                        DebugType.AnimationDetailed
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
                boolean targFound = false;

                for (CharacterActionAnims act : CharacterActionAnims.values()) {
                    boolean isTarg = act == CharacterActionAnims.None;
                    String actname;
                    if (isTarg) {
                        actname = target;
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
                        DebugType.General.warn("WARNING: did not find node with condition 'PerformingAction = %s' in player/actions/", actname);
                    }
                }

                if (targFound) {
                    DebugType.Animation.debugln("SUCCESS - Current 'actions' TargetNode: '%s' was found.", target);
                } else {
                    DebugType.Animation.debugln("FAIL - Current 'actions' TargetNode: '%s' not found.", target);
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

    public boolean isRecording() {
        return this.character != null && this.character.isAnimationRecorderActive();
    }

    public void incrementWhileAliveFlag(AnimationVariableReference variableReference, boolean whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.incrementWhileAliveFlag(variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", variableReference, stillAliveCounter);
        variableReference.setVariable(this.getCharacter(), stillAliveCounter > 0 ? whileAliveValue : !whileAliveValue);
    }

    public void decrementWhileAliveFlag(AnimationVariableReference variableReference, boolean whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.decrementWhileAliveFlag(variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", variableReference, stillAliveCounter);
        variableReference.setVariable(this.getCharacter(), stillAliveCounter > 0 ? whileAliveValue : !whileAliveValue);
    }

    public void addAnimCallback(IAnimEventCallback callback) {
        this.animCallbackHandlers.add(callback);
    }

    public static List<String> searchFolders(final URI base, Path pathDir) throws IOException {
        final List<String> files = new ArrayList<>();
        Files.walkFileTree(pathDir, new FileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().toLowerCase().endsWith(".xml")) {
                    String relPath = ZomboidFileSystem.instance.getRelativeFile(base, file.toAbsolutePath().toString());
                    files.add(relPath.toLowerCase(Locale.ENGLISH));
                }

                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                DebugType.General.printException(exc, LogSeverity.Error);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    DebugType.General.printException(exc, LogSeverity.Error);
                }

                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private static List<String> collectBaseGameFiles() {
        List<String> files = new ArrayList<>();

        try {
            File animSetDir = ZomboidFileSystem.instance.getMediaFile("AnimSets");
            File actionGroupsDir = ZomboidFileSystem.instance.getMediaFile("actiongroups");
            files.addAll(searchFolders(ZomboidFileSystem.instance.base.lowercaseUri, animSetDir.toPath()));
            files.addAll(searchFolders(ZomboidFileSystem.instance.base.lowercaseUri, actionGroupsDir.toPath()));
        } catch (IOException var3) {
            ExceptionLogger.logException(var3);
        }

        files.sort(String.CASE_INSENSITIVE_ORDER);
        return files;
    }

    private static List<String> loadModMedia(String modDir) {
        List<String> files = new ArrayList<>();
        if (modDir == null) {
            return files;
        } else {
            try {
                File modDirFile = new File(modDir);
                URI lowercaseURI = new File(modDirFile.getCanonicalFile().getPath().toLowerCase(Locale.ENGLISH)).toURI();
                File canonicalMedia = ZomboidFileSystem.instance.getCanonicalFile(modDirFile, "media");
                File canonicalAnimSets = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "AnimSets");
                File canonicalActionGroups = ZomboidFileSystem.instance.getCanonicalFile(canonicalMedia, "actiongroups");
                files.addAll(searchFolders(lowercaseURI, canonicalAnimSets.toPath()));
                files.addAll(searchFolders(lowercaseURI, canonicalActionGroups.toPath()));
            } catch (IOException var7) {
                ExceptionLogger.logException(var7);
            }

            return files;
        }
    }

    private static List<String> collectModFiles() throws Exception {
        List<String> files = new ArrayList<>();
        List<String> modFiles = new ArrayList<>();

        for (String s : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(s);
            if (mod != null) {
                modFiles.addAll(loadModMedia(mod.getCommonDir()));
                modFiles.addAll(loadModMedia(mod.getVersionDir()));
                modFiles.sort(String.CASE_INSENSITIVE_ORDER);
                files.addAll(modFiles);
                modFiles.clear();
            }
        }

        return files;
    }

    private static void buildChecksum(List<String> gameFiles) throws Exception {
        Set<String> done = new HashSet<>();

        for (String s : gameFiles) {
            if (!done.contains(s)) {
                done.add(s);
                String absPath = ZomboidFileSystem.instance.getAbsolutePath(s);
                if (absPath == null) {
                    throw new IllegalStateException("couldn't find \"" + s + "\"");
                }

                NetChecksum.checksummer.addFile(s, absPath);
            }
        }
    }

    private static void finalizeChecksum() {
        checksum = NetChecksum.checksummer.checksumToString();
        NetChecksum.GroupOfFiles.finishChecksum();
    }

    public static void load() throws Exception {
        if (GameServer.server || GameClient.client) {
            NetChecksum.checksummer.reset();
            List<String> gameFiles = collectBaseGameFiles();
            gameFiles.addAll(collectModFiles());
            buildChecksum(gameFiles);
            finalizeChecksum();
        }
    }

    public static String getChecksum() {
        return checksum;
    }
}
