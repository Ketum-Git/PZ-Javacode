// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetPath;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimState {
    public String name = "";
    public final List<AnimNode> nodes = new ArrayList<>();
    public final List<AnimNode> abstractNodes = new ArrayList<>();
    public int defaultIndex;
    public AnimationSet set;

    public List<AnimNode> getAnimNodes(IAnimationVariableSource varSource, List<AnimNode> nodes) {
        nodes.clear();
        if (this.nodes.size() <= 0) {
            return nodes;
        } else if (DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue()
            && varSource.getVariableBoolean("dbgForceAnim")
            && varSource.isVariable("dbgForceAnimStateName", this.name)) {
            String dbgForceAnimNodeName = varSource.getVariableString("dbgForceAnimNodeName");
            int anIdx = 0;

            for (int nodeCount = this.nodes.size(); anIdx < nodeCount; anIdx++) {
                AnimNode node = this.nodes.get(anIdx);
                if (StringUtils.equalsIgnoreCase(node.name, dbgForceAnimNodeName)) {
                    nodes.add(node);
                    break;
                }
            }

            return nodes;
        } else {
            AnimNode bestNode = null;
            int i = 0;

            for (int nodeCountx = this.nodes.size(); i < nodeCountx; i++) {
                AnimNode node = this.nodes.get(i);
                if (bestNode != null && bestNode.compareSelectionConditions(node) > 0) {
                    break;
                }

                if (node.checkConditions(varSource)) {
                    bestNode = node;
                    nodes.add(node);
                }
            }

            if (!nodes.isEmpty() && DebugOptions.instance.animation.animLayer.logNodeConditions.getValue()) {
                DebugType.Animation
                    .debugln(
                        "%s Nodes passed: %s",
                        this.set.name,
                        PZArrayUtil.arrayToString(nodes, nodex -> String.format("%s: %s", nodex.name, nodex.getConditionsString()), "{ ", " }", "; ")
                    );
            }

            return nodes;
        }
    }

    public static AnimState Parse(String name, String statePath) {
        AnimState state = new AnimState();
        state.name = name;
        DebugType.Animation.debugln("Loading AnimState: %s", name);
        String[] listOfNodeFiles = ZomboidFileSystem.instance.resolveAllFiles(statePath, file -> file.getName().endsWith(".xml"), true);

        for (String nodeFileName : listOfNodeFiles) {
            File nodeFile = new File(nodeFileName);
            String nodeName = nodeFile.getName().split(".xml")[0].toLowerCase();
            DebugType.Animation.debugln("%s -> AnimNode: %s", name, nodeName);
            String absolutePath = ZomboidFileSystem.instance.resolveFileOrGUID(nodeFileName);
            AnimNodeAsset asset = (AnimNodeAsset)AnimNodeAssetManager.instance.load(new AssetPath(absolutePath));
            if (asset.isReady()) {
                AnimNode newNode = asset.animNode;
                newNode.parentState = state;
                state.addNode(newNode);
            }
        }

        return state;
    }

    public void addNode(AnimNode newNode) {
        if (newNode.isAbstract()) {
            this.abstractNodes.add(newNode);
        } else {
            int insertAt = this.nodes.size();

            for (int i = 0; i < this.nodes.size(); i++) {
                AnimNode node = this.nodes.get(i);
                if (newNode.compareSelectionConditions(node) > 0) {
                    insertAt = i;
                    break;
                }
            }

            this.nodes.add(insertAt, newNode);
        }
    }

    @Override
    public String toString() {
        return "AnimState{"
            + this.name
            + ", NodeCount:"
            + this.nodes.size()
            + ", AbstractNodeCount:"
            + this.abstractNodes.size()
            + ", DefaultIndex:"
            + this.defaultIndex
            + "}";
    }

    /**
     * Null-safe function that returns a given state's name.
     *  If null, returns a null
     */
    public static String getStateName(AnimState state) {
        return state != null ? state.name : null;
    }

    protected void clear() {
        this.nodes.clear();
        this.abstractNodes.clear();
        this.set = null;
    }
}
