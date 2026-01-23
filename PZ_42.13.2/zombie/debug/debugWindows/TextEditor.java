// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.ImGuiIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;

public class TextEditor extends BaseDebugWindow {
    private File file;
    private boolean dirty;
    private boolean justOpened;
    private final imgui.extension.texteditor.TextEditor textEditor = new imgui.extension.texteditor.TextEditor();
    private boolean isDebounceFkeys;

    public TextEditor() {
        this.textEditor.setShowWhitespaces(false);
    }

    @Override
    public String getTitle() {
        return this.file.getAbsolutePath().substring(this.file.getAbsolutePath().indexOf("media") + 6);
    }

    @Override
    protected void doKeyInput(ImGuiIO io, boolean keyShift, boolean keyCtrl, boolean keyAlt) {
        if (io.getKeysDown(83) && keyCtrl) {
            try {
                this.save();
            } catch (IOException var10) {
                throw new RuntimeException(var10);
            }
        }

        if (io.getKeysDown(299) && !this.isDebounceFkeys) {
            this.textEditor.setExecutingLine(-1);
            this.stepOver();
            this.isDebounceFkeys = true;
        } else if (io.getKeysDown(300) && !this.isDebounceFkeys) {
            this.textEditor.setExecutingLine(-1);
            this.stepInto();
            this.isDebounceFkeys = true;
        } else if (io.getKeysDown(294) && !this.isDebounceFkeys) {
            this.textEditor.setExecutingLine(-1);
            this.cont();
            this.isDebounceFkeys = true;
        } else if (io.getKeysDown(298) && !this.isDebounceFkeys) {
            int c = this.textEditor.getBreakpointCount();
            ArrayList<Integer> breakpoints = new ArrayList<>();

            for (int x = 0; x < c; x++) {
                int l = this.textEditor.getBreakpoint(x);
                breakpoints.add(l);
            }

            int[] breakpointArray = new int[breakpoints.size()];

            for (int i = 0; i < breakpoints.size(); i++) {
                Integer breakpoint = breakpoints.get(i);
                breakpointArray[i] = breakpoint;
            }

            this.setBreakpoints(breakpoints);
            this.isDebounceFkeys = true;
        }

        if (!io.getKeysDown(299) && !io.getKeysDown(300) && !io.getKeysDown(298) && !io.getKeysDown(294)) {
            this.isDebounceFkeys = false;
        } else {
            this.isDebounceFkeys = true;
        }
    }

    protected void setBreakpoints(ArrayList<Integer> breakpoints) {
    }

    protected void cont() {
    }

    protected void stepInto() {
    }

    protected void stepOver() {
    }

    protected void save() throws IOException {
        try (BufferedWriter br = new BufferedWriter(new FileWriter(this.file))) {
            String text = this.textEditor.getText();
            br.write(text);
        }

        this.dirty = false;
    }

    @Override
    protected boolean isWindowFocused() {
        return this.textEditor.isFocused();
    }

    @Override
    protected void doWindowContents() {
        if (this.textEditor.isTextChanged() && !this.justOpened) {
            this.dirty = true;
        }

        this.justOpened = false;
        this.textEditor.render("TextEditor");
        if (ImGui.beginPopupModal("File changed...")) {
            float textSize = ImGui.calcTextSize("Do you want to save file: " + this.getTitle() + "?").x;
            float textSize2 = ImGui.calcTextSize("Yes").x + ImGui.calcTextSize("No").x + ImGui.calcTextSize("Cancel").x;
            textSize2 += 30.0F;
            ImGui.setWindowSize(textSize + 64.0F, 150.0F);
            ImGui.setCursorPosX((ImGui.getWindowSize().x - textSize) * 0.5F);
            ImGui.textUnformatted("Do you want to save file: " + this.getTitle() + "?");
            ImGui.newLine();
            ImGui.separator();
            ImGui.newLine();
            ImGui.setCursorPosX((ImGui.getWindowSize().x - textSize2) / 2.0F);
            if (ImGui.button("Yes")) {
                this.open.set(false);

                try {
                    this.save();
                } catch (IOException var4) {
                    throw new RuntimeException(var4);
                }

                ImGui.closeCurrentPopup();
                DebugContext.instance.closeTransient(this);
            }

            ImGui.sameLine();
            if (ImGui.button("No")) {
                this.open.set(false);
                ImGui.closeCurrentPopup();
                DebugContext.instance.closeTransient(this);
            }

            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Override
    protected void doMenu() {
        if (ImGui.beginMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save")) {
                }

                if (ImGui.menuItem("Save as...")) {
                }

                ImGui.endMenu();
            }

            ImGui.endMenuBar();
        }
    }

    @Override
    protected void onCloseWindow() {
        if (this.dirty) {
            ImGui.openPopup("File changed...");
            this.open.set(true);
        } else {
            DebugContext.instance.closeTransient(this);
        }
    }

    public void load(File file) throws IOException {
        this.file = file;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }

            String everything = sb.toString();
            this.textEditor.setText(everything);
        }

        this.dirty = false;
        this.justOpened = true;
        this.open.set(true);
    }

    public void setExecutingLine(int lineNumber) {
        this.textEditor.setExecutingLine(lineNumber);
        this.textEditor.setCursorPosition(lineNumber - 1, 0);
    }
}
