// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.popman.ObjectPool;
import zombie.util.StringUtils;

public final class TransitionNodeProxy {
    public ArrayList<TransitionNodeProxy.NodeLayerPair> allNewNodes = new ArrayList<>();
    public List<TransitionNodeProxy.NodeLayerPair> allOutgoingNodes = new ArrayList<>();
    public List<TransitionNodeProxy.TransitionNodeProxyData> foundTransitions = new ArrayList<>();
    private static final ObjectPool<TransitionNodeProxy.NodeLayerPair> s_nodeLayerPairPool = new ObjectPool<>(TransitionNodeProxy.NodeLayerPair::new);
    private static final ObjectPool<TransitionNodeProxy.TransitionNodeProxyData> s_transitionNodeProxyDataPool = new ObjectPool<>(
        TransitionNodeProxy.TransitionNodeProxyData::new
    );

    public Boolean HasAnyPossibleTransitions() {
        return !this.allNewNodes.isEmpty() && !this.allOutgoingNodes.isEmpty();
    }

    public TransitionNodeProxy.NodeLayerPair allocNodeLayerPair(LiveAnimNode _liveAnimNode, AnimLayer _animLayer) {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean var3 = true;
        }

        return s_nodeLayerPairPool.alloc().set(_liveAnimNode, _animLayer);
    }

    public TransitionNodeProxy.TransitionNodeProxyData allocTransitionNodeProxyData() {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean var1 = true;
        }

        return s_transitionNodeProxyDataPool.alloc().reset();
    }

    public void reset() {
        if (Thread.currentThread() != GameWindow.gameThread) {
            boolean var1 = true;
        }

        s_nodeLayerPairPool.releaseAll(this.allNewNodes);
        this.allNewNodes.clear();
        s_nodeLayerPairPool.releaseAll(this.allOutgoingNodes);
        this.allOutgoingNodes.clear();
        s_transitionNodeProxyDataPool.releaseAll(this.foundTransitions);
        this.foundTransitions.clear();
    }

    public static class NodeLayerPair {
        public LiveAnimNode liveAnimNode;
        public AnimLayer animLayer;

        protected NodeLayerPair() {
        }

        public TransitionNodeProxy.NodeLayerPair set(LiveAnimNode _liveAnimNode, AnimLayer _animLayer) {
            this.liveAnimNode = _liveAnimNode;
            this.animLayer = _animLayer;
            return this;
        }
    }

    public static class TransitionNodeProxyData {
        public LiveAnimNode newAnimNode;
        public LiveAnimNode oldAnimNode;
        public AnimTransition transitionOut;
        public AnimLayer animLayerIn;
        public AnimLayer animLayerOut;

        public Boolean HasValidAnimNodes() {
            return this.newAnimNode != null && this.oldAnimNode != null;
        }

        public Boolean HasValidTransitions() {
            return this.transitionOut != null;
        }

        protected TransitionNodeProxyData() {
        }

        public TransitionNodeProxy.TransitionNodeProxyData reset() {
            this.newAnimNode = null;
            this.oldAnimNode = null;
            this.transitionOut = null;
            this.animLayerIn = null;
            this.animLayerOut = null;
            return this;
        }

        private boolean isUsingAnimNodesDeferredInfo() {
            return this.transitionOut == null || StringUtils.isNullOrWhitespace(this.transitionOut.deferredBoneName);
        }

        public String getDeferredBoneName() {
            return this.isUsingAnimNodesDeferredInfo() ? this.oldAnimNode.getDeferredBoneName() : this.transitionOut.deferredBoneName;
        }

        public BoneAxis getDeferredBoneAxis() {
            return this.isUsingAnimNodesDeferredInfo() ? this.oldAnimNode.getDeferredBoneAxis() : this.transitionOut.deferredBoneAxis;
        }

        public boolean getUseDeferredRotation() {
            return this.isUsingAnimNodesDeferredInfo() ? this.oldAnimNode.getUseDeferredRotation() : this.transitionOut.useDeferedRotation;
        }

        public boolean getUseDeferredMovement() {
            return this.isUsingAnimNodesDeferredInfo() ? this.oldAnimNode.getUseDeferredMovement() : this.transitionOut.useDeferredMovement;
        }

        public float getDeferredRotationScale() {
            return this.isUsingAnimNodesDeferredInfo() ? this.oldAnimNode.getDeferredRotationScale() : this.transitionOut.deferredRotationScale;
        }
    }
}
