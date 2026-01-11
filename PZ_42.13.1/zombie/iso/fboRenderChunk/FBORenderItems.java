// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.IModelCamera;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.TextureDraw;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;

public final class FBORenderItems {
    private static FBORenderItems instance;
    private final ArrayList<FBORenderItems.RenderJob> renderJobs = new ArrayList<>();
    private final ObjectPool<FBORenderItems.RenderJob> jobPool = new ObjectPool<>(FBORenderItems.RenderJob::new);
    private final FBORenderItems.ChunkCamera chunkCamera = new FBORenderItems.ChunkCamera();

    public static FBORenderItems getInstance() {
        if (instance == null) {
            instance = new FBORenderItems();
        }

        return instance;
    }

    public void render(int renderChunkIndex, IsoTrap trap) {
        InventoryItem item = trap.getItem();
        if (ItemModelRenderer.itemHasModel(item)) {
            FBORenderItems.RenderJob renderJob = this.jobPool.alloc();
            renderJob.init(renderChunkIndex, trap);
            this.renderJobs.add(renderJob);
        }
    }

    public void render(int renderChunkIndex, IsoWorldInventoryObject worldInventoryObject) {
        InventoryItem item = worldInventoryObject.getItem();
        if (ItemModelRenderer.itemHasModel(item)) {
            FBORenderItems.RenderJob renderJob = this.jobPool.alloc();
            renderJob.init(renderChunkIndex, worldInventoryObject);
            this.renderJobs.add(renderJob);
        }
    }

    public void update() {
        for (int i = 0; i < this.renderJobs.size(); i++) {
            FBORenderItems.RenderJob job = this.renderJobs.get(i);
            if (job.getObject().getObjectIndex() == -1) {
                job.done = 1;
            }

            if (job.getObject().getRenderSquare() == null) {
                job.done = 1;
            }

            if (job.done != 1 || job.renderRefCount <= 0) {
                if (job.done == 1 && job.renderRefCount == 0) {
                    this.renderJobs.remove(i--);
                    this.jobPool.release(job);
                } else {
                    FBORenderChunk renderChunk = FBORenderChunkManager.instance.chunks.get(job.renderChunkIndex);
                    if (renderChunk == null) {
                        job.done = 1;
                    } else if (job.playerIndex == IsoCamera.frameState.playerIndex) {
                        ItemModelRenderer.RenderStatus status = job.renderMain();
                        if (status == ItemModelRenderer.RenderStatus.Loading) {
                            boolean playerIndex = true;
                        } else if (status == ItemModelRenderer.RenderStatus.Ready) {
                            job.renderRefCount++;
                            if (!FBORenderChunkManager.instance.toRenderThisFrame.contains(renderChunk)) {
                                FBORenderChunkManager.instance.toRenderThisFrame.add(renderChunk);
                            }

                            int playerIndex = IsoCamera.frameState.playerIndex;
                            SpriteRenderer.instance.glDoEndFrame();
                            SpriteRenderer.instance.glDoStartFrameFlipY(renderChunk.w, renderChunk.h, 1.0F, playerIndex);
                            renderChunk.beginMainThread(false);
                            SpriteRenderer.instance.drawGeneric(job);
                            renderChunk.endMainThread();
                            SpriteRenderer.instance.glDoEndFrame();
                            SpriteRenderer.instance
                                .glDoStartFrame(
                                    Core.getInstance().getScreenWidth(),
                                    Core.getInstance().getScreenHeight(),
                                    Core.getInstance().getZoom(playerIndex),
                                    playerIndex
                                );
                        } else {
                            job.done = 1;
                        }
                    }
                }
            }
        }
    }

    public void Reset() {
        this.jobPool.forEach(FBORenderItems.RenderJob::Reset);
        this.jobPool.clear();
        this.renderJobs.clear();
    }

    public IModelCamera getCamera() {
        return this.chunkCamera;
    }

    public void setCamera(FBORenderChunk renderChunk, float x, float y, float z, Vector3f rotate) {
        this.chunkCamera.set(renderChunk, x, y, z, 0.0F);
        this.chunkCamera.angle.set(rotate);
    }

    private static final class ChunkCamera extends FBORenderChunkCamera {
        final Vector3f angle = new Vector3f();

        @Override
        public void Begin() {
            super.Begin();
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.peek();
            MODELVIEW.rotate((float) -Math.PI, 0.0F, 1.0F, 0.0F);
            MODELVIEW.rotate(this.angle.x * (float) (Math.PI / 180.0), 1.0F, 0.0F, 0.0F);
            MODELVIEW.rotate(this.angle.y * (float) (Math.PI / 180.0), 0.0F, 1.0F, 0.0F);
            MODELVIEW.rotate(this.angle.z * (float) (Math.PI / 180.0), 0.0F, 0.0F, 1.0F);
        }

        @Override
        public void End() {
            super.End();
        }
    }

    private static final class RenderJob extends TextureDraw.GenericDrawer {
        int playerIndex;
        int renderChunkIndex;
        int done;
        int renderRefCount;
        IsoTrap trap;
        IsoWorldInventoryObject worldInventoryObject;
        final ItemModelRenderer[] renderer = new ItemModelRenderer[3];
        boolean rendered;

        RenderJob() {
            for (int i = 0; i < this.renderer.length; i++) {
                this.renderer[i] = new ItemModelRenderer();
            }
        }

        void init(int renderChunkIndex, IsoWorldInventoryObject worldInventoryObject) {
            this.playerIndex = IsoCamera.frameState.playerIndex;
            this.renderChunkIndex = renderChunkIndex;
            this.done = 0;
            this.renderRefCount = 0;
            this.worldInventoryObject = worldInventoryObject;
            this.trap = null;
            this.rendered = false;
        }

        void init(int renderChunkIndex, IsoTrap trap) {
            this.playerIndex = IsoCamera.frameState.playerIndex;
            this.renderChunkIndex = renderChunkIndex;
            this.done = 0;
            this.renderRefCount = 0;
            this.worldInventoryObject = null;
            this.trap = trap;
            this.rendered = false;
        }

        IsoObject getObject() {
            return (IsoObject)(this.trap != null ? this.trap : this.worldInventoryObject);
        }

        ItemModelRenderer.RenderStatus renderMain() {
            if (this.trap != null) {
                IsoGridSquare square = this.trap.getSquare();
                IsoGridSquare renderSquare = this.trap.getRenderSquare();
                boolean bRenderToChunkTexture = true;
                int stateIndex = SpriteRenderer.instance.getMainStateIndex();
                return this.renderer[stateIndex]
                    .renderMain(this.trap.getItem(), square, renderSquare, square.x + 0.5F, square.y + 0.5F, square.z, 0.0F, -1.0F, true);
            } else {
                IsoGridSquare square = this.worldInventoryObject.getSquare();
                IsoGridSquare renderSquare = this.worldInventoryObject.getRenderSquare();
                boolean bRenderToChunkTexture = true;
                int stateIndex = SpriteRenderer.instance.getMainStateIndex();
                return this.renderer[stateIndex]
                    .renderMain(
                        this.worldInventoryObject.getItem(),
                        square,
                        renderSquare,
                        square.x + this.worldInventoryObject.xoff,
                        square.y + this.worldInventoryObject.yoff,
                        square.z + this.worldInventoryObject.zoff,
                        0.0F,
                        -1.0F,
                        true
                    );
            }
        }

        @Override
        public void render() {
            if (this.done != 1) {
                SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
                FBORenderChunk renderChunk = renderState.cachedRenderChunkIndexMap.get(this.renderChunkIndex);
                if (renderChunk != null) {
                    int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
                    ItemModelRenderer renderer = this.renderer[stateIndex];
                    FBORenderItems.instance.chunkCamera.set(renderChunk, renderer.x, renderer.y, renderer.z, 0.0F);
                    FBORenderItems.instance.chunkCamera.angle.set(renderer.angle);
                    renderer.DoRender(FBORenderItems.instance.chunkCamera, true, renderChunk.highRes);
                    if (renderer.isRendered()) {
                        this.rendered = true;
                    }
                }
            }
        }

        @Override
        public void postRender() {
            if (this.rendered) {
                this.rendered = false;
                this.done = 1;
            }

            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            this.renderer[stateIndex].reset();
            this.renderRefCount--;
        }

        void Reset() {
        }
    }
}
