// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;
import java.util.ArrayList;
import org.lwjglx.opengl.Display;
import zombie.core.Core;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.debugWindows.AchievementPanel;
import zombie.debug.debugWindows.AimPlotter;
import zombie.debug.debugWindows.BallisticsTargetPanel;
import zombie.debug.debugWindows.CombatManagerEditor;
import zombie.debug.debugWindows.Console;
import zombie.debug.debugWindows.FirearmPanel;
import zombie.debug.debugWindows.JavaInspector;
import zombie.debug.debugWindows.LuaPanel;
import zombie.debug.debugWindows.PhysicsHitReactionsPanel;
import zombie.debug.debugWindows.RagdollDebugWindow;
import zombie.debug.debugWindows.RangeWeaponPanel;
import zombie.debug.debugWindows.RegistriesViewer;
import zombie.debug.debugWindows.ScenePanel;
import zombie.debug.debugWindows.StatisticsPanel;
import zombie.debug.debugWindows.TargetHitInfoPanel;
import zombie.debug.debugWindows.TracerEffectsDebugWindow;
import zombie.debug.debugWindows.UIPanel;
import zombie.debug.debugWindows.Viewport;

public class DebugContext {
    public static final float FLT_MIN = Float.MIN_NORMAL;
    public static DebugContext instance = new DebugContext();
    public int dockspaceId;
    public TextureFBO debugViewportTexture;
    public boolean focusedGameViewport;
    private int dockspace;
    private float viewportMouseX;
    private float viewportMouseY;
    private float lastViewportMouseX;
    private float lastViewportMouseY;
    public Viewport viewport;
    private final ArrayList<BaseDebugWindow> windows = new ArrayList<>();
    private final ArrayList<BaseDebugWindow> transientWindows = new ArrayList<>();

    public static boolean isUsingGameViewportWindow() {
        return Core.isUseGameViewport();
    }

    public void initRenderTarget() {
        if (Core.isUseGameViewport()) {
            RenderThread.invokeOnRenderContext(() -> {
                Texture tex = new Texture(Core.width, Core.height, 18);
                this.debugViewportTexture = new TextureFBO(tex);
            });
        }
    }

    public void init() {
        if (Core.isImGui()) {
            this.initRenderTarget();
            this.windows.add(new LuaPanel());
            this.windows.add(new UIPanel());
            this.windows.add(new ScenePanel());
            this.windows.add(new FirearmPanel());
            this.windows.add(new AimPlotter());
            this.windows.add(new RagdollDebugWindow());
            this.windows.add(new PhysicsHitReactionsPanel());
            this.windows.add(new TracerEffectsDebugWindow());
            this.windows.add(new BallisticsTargetPanel());
            this.windows.add(new TargetHitInfoPanel());
            this.windows.add(new RangeWeaponPanel());
            this.windows.add(new StatisticsPanel());
            this.windows.add(new AchievementPanel());
            this.windows.add(new RegistriesViewer());
            this.windows.add(new CombatManagerEditor());
            if (Core.isUseGameViewport()) {
                this.viewport = new Viewport();
                this.windows.add(this.viewport);
            }

            this.windows.add(new Console());
        }
    }

    public void destroy() {
        if (Core.isImGui()) {
            if (Core.isUseGameViewport()) {
                this.debugViewportTexture.destroy();
                this.debugViewportTexture = null;
            }
        }
    }

    public void tick() {
        if (Core.isImGui()) {
            if (Display.inImGuiFrame()) {
                this.doMainMenu();

                for (int i = 0; i < this.windows.size(); i++) {
                    BaseDebugWindow window = this.windows.get(i);
                    if (window.open.get()) {
                        window.endFrameTick();
                    }
                }

                for (int ix = 0; ix < this.transientWindows.size(); ix++) {
                    BaseDebugWindow window = this.transientWindows.get(ix);
                    if (window.open.get()) {
                        window.endFrameTick();
                    }
                }
            }
        }
    }

    private void doMainMenu() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("Windows")) {
                for (int i = 0; i < this.windows.size(); i++) {
                    BaseDebugWindow window = this.windows.get(i);
                    if (ImGui.menuItem(window.getTitle(), null, window.open.get())) {
                        window.open = new ImBoolean(!window.open.get());
                    }
                }

                ImGui.separator();

                for (int ix = 0; ix < this.transientWindows.size(); ix++) {
                    BaseDebugWindow window = this.transientWindows.get(ix);
                    if (ImGui.menuItem(window.getTitle(), null, window.open.get())) {
                        window.open = new ImBoolean(!window.open.get());
                    }
                }

                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }

    public void startDrawing() {
        this.debugViewportTexture.startDrawing(true, false);
    }

    public void endDrawing() {
        this.debugViewportTexture.endDrawing();
    }

    public void tickFrameStart() {
        if (Core.isImGui()) {
            if (Display.inImGuiFrame()) {
                if (!Core.isUseGameViewport()) {
                    this.dockspace = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), 8);
                } else {
                    this.dockspace = ImGui.dockSpaceOverViewport(ImGui.getMainViewport());
                }

                for (int i = 0; i < this.windows.size(); i++) {
                    BaseDebugWindow window = this.windows.get(i);
                    if (window.open.get()) {
                        window.startFrameTick();
                    }
                }

                for (int ix = 0; ix < this.transientWindows.size(); ix++) {
                    BaseDebugWindow window = this.transientWindows.get(ix);
                    if (window.open.get()) {
                        window.startFrameTick();
                    }
                }
            }
        }
    }

    public int getViewportMouseX() {
        return (int)this.viewportMouseX;
    }

    public int getViewportMouseY() {
        return Core.height - (int)this.viewportMouseY;
    }

    public void setViewportX(float viewportMouseX) {
        this.viewportMouseX = viewportMouseX;
    }

    public void setViewportY(float viewportMouseY) {
        this.viewportMouseY = viewportMouseY;
    }

    public BaseDebugWindow getExistingTransientWindow(BaseDebugWindow window) {
        for (int i = 0; i < this.transientWindows.size(); i++) {
            BaseDebugWindow transientWindow = this.transientWindows.get(i);
            if (transientWindow.getTitle().equals(window.getTitle())) {
                return transientWindow;
            }
        }

        return null;
    }

    public void inspectJava(Object obj) {
        if (obj != null) {
            JavaInspector ji = new JavaInspector(obj);
            BaseDebugWindow tr = this.getExistingTransientWindow(ji);
            if (tr == null) {
                this.transientWindows.add(ji);
            } else {
                tr.open = new ImBoolean(true);
            }
        }
    }

    public ArrayList<BaseDebugWindow> getTransientWindows() {
        return this.transientWindows;
    }

    public void closeTransient(BaseDebugWindow window) {
        this.transientWindows.remove(window);
    }

    public ArrayList<BaseDebugWindow> getWindows() {
        return this.windows;
    }
}
