// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.util.HashMap;
import java.util.HashSet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;

@UsedFromLua
public class VideoTexture extends Texture {
    private static final HashMap<String, VideoTexture> successfullyLoaded = new HashMap<>();
    private static final HashSet<String> failedToLoad = new HashSet<>();
    protected boolean useAsync = true;
    protected String videoFilename;
    protected int binkId = -1;

    public static VideoTexture getOrCreate(String filename, int width, int height, boolean useAsync) {
        String absPath = ZomboidFileSystem.instance.getMediaPath("videos/" + filename);
        if (failedToLoad.contains(absPath)) {
            return null;
        } else if (successfullyLoaded.containsKey(absPath)) {
            VideoTexture videoTexture = successfullyLoaded.get(absPath);
            if (videoTexture.isDestroyed()) {
                successfullyLoaded.remove(absPath);
            }

            return videoTexture;
        } else {
            VideoTexture videoTexture = new VideoTexture(absPath, width, height, useAsync);
            if (videoTexture.LoadVideoFile()) {
                successfullyLoaded.put(absPath, videoTexture);
                return videoTexture;
            } else {
                DebugLog.log("Unable to load video texture " + absPath + ".");
                failedToLoad.add(absPath);
                RenderThread.queueInvokeOnRenderContext(videoTexture::destroy);
                return null;
            }
        }
    }

    public static VideoTexture getOrCreate(String filename, int width, int height) {
        return getOrCreate(filename, width, height, true);
    }

    private VideoTexture(String filename, int width, int height) {
        super(width, height, 0);
        this.videoFilename = filename;
        this.xStart = 0.0F;
        this.yStart = 0.0F;
        this.xEnd = 1.0F;
        this.yEnd = 1.0F;
    }

    private VideoTexture(String filename, int width, int height, boolean useAsync) {
        super(width, height, 0);
        this.videoFilename = filename;
        this.xStart = 0.0F;
        this.yStart = 0.0F;
        this.xEnd = 1.0F;
        this.yEnd = 1.0F;
        this.useAsync = useAsync;
    }

    public void closeAndDestroy() {
        successfullyLoaded.remove(this.videoFilename);
        failedToLoad.remove(this.videoFilename);
        this.Close();
        if (!this.isDestroyed()) {
            RenderThread.queueInvokeOnRenderContext(this::destroy);
        }
    }

    public boolean LoadVideoFile() {
        if (this.binkId > -1) {
            DebugLog.log("VideoTexture warning - trying to load a video file which has already been loaded.");
        } else {
            this.binkId = this.openVideo(this.videoFilename);
            if (this.binkId > -1 && this.useAsync) {
                this.processFrameAsync(this.binkId);
            }
        }

        DebugLog.log("binkId: " + this.binkId);
        return this.binkId >= 0;
    }

    public void Close() {
        if (this.binkId > -1) {
            this.closeVideo(this.binkId);
            this.binkId = -1;
        }
    }

    protected void RenderFrameAsync() {
        if (this.isReadyForNewFrame(this.binkId)) {
            if (this.processFrameAsyncWait(this.binkId, 1000)) {
                while (this.shouldSkipFrame(this.binkId)) {
                    this.nextFrame(this.binkId);
                    this.processFrameAsync(this.binkId);
                    this.processFrameAsyncWait(this.binkId, -1);
                }

                if (this.isEndOfVideo(this.binkId)) {
                }

                this.nextFrame(this.binkId);
                this.processFrameAsync(this.binkId);
            }

            long frameData = this.getCurrentFrameData(this.binkId);
            RenderThread.queueInvokeOnRenderContext(() -> {
                GL13.glActiveTexture(33984);
                GL11.glBindTexture(3553, Texture.lastTextureID = this.getID());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                GL11.glTexParameteri(3553, 10242, 10496);
                GL11.glTexParameteri(3553, 10243, 10496);
                GL11.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 5121, frameData);
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            });
        }
    }

    public void RenderFrame() {
        if (this.binkId >= 0) {
            if (this.useAsync) {
                this.RenderFrameAsync();
            } else {
                if (this.isReadyForNewFrame(this.binkId)) {
                    this.processFrame(this.binkId);
                    this.nextFrame(this.binkId);

                    while (this.shouldSkipFrame(this.binkId)) {
                        this.processFrame(this.binkId);
                        this.nextFrame(this.binkId);
                    }

                    long frameData = this.getCurrentFrameData(this.binkId);
                    RenderThread.queueInvokeOnRenderContext(() -> {
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, Texture.lastTextureID = this.getID());
                        GL11.glTexParameteri(3553, 10241, 9728);
                        GL11.glTexParameteri(3553, 10240, 9728);
                        GL11.glTexParameteri(3553, 10242, 10496);
                        GL11.glTexParameteri(3553, 10243, 10496);
                        GL11.glTexImage2D(3553, 0, 6408, this.getWidth(), this.getHeight(), 0, 6408, 5121, frameData);
                        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                    });
                }

                if (this.isEndOfVideo(this.binkId)) {
                }
            }
        }
    }

    @Override
    public boolean isValid() {
        return this.binkId >= 0;
    }

    private native int openVideo(String arg0);

    private native boolean isReadyForNewFrame(int arg0);

    private native void processFrame(int arg0);

    private native void processFrameAsync(int arg0);

    private native boolean processFrameAsyncWait(int arg0, int arg1);

    private native void nextFrame(int arg0);

    private native boolean shouldSkipFrame(int arg0);

    private native boolean isEndOfVideo(int arg0);

    private native void closeVideo(int arg0);

    private native long getCurrentFrameData(int arg0);

    private native int getFrameDataSize(int arg0);

    static {
        if (System.getProperty("os.name").contains("OS X")) {
            System.loadLibrary("bink64");
        } else if (System.getProperty("os.name").startsWith("Win")) {
            System.loadLibrary("bink2w64");
            System.loadLibrary("bink64");
        } else {
            System.loadLibrary("Bink2x64");
            System.loadLibrary("bink64");
        }
    }
}
