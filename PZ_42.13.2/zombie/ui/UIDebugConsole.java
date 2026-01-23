// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.KahluaException;
import se.krka.kahlua.vm.LuaClosure;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.input.Mouse;

@UsedFromLua
public final class UIDebugConsole extends NewWindow {
    public static UIDebugConsole instance;
    ScrollBar scrollBarV;
    UITextBox2 outputLog;
    public UITextBox2 commandLine;
    boolean resizing;
    boolean resizeWidth;
    boolean resizeHeight;
    int resizeStartX;
    int resizeStartY;
    int resizeStartWidth;
    int resizeStartHeight;
    UITextBox2 autosuggest;
    String consoleVersion = "v1.1.0";
    int inputlength;
    private final ArrayList<String> previous = new ArrayList<>();
    private final ArrayList<Method> globalLuaMethods = new ArrayList<>();
    public int previousIndex;
    Method prevSuggestion;
    String[] availableCommands = new String[]{"?", "help", "commands", "clr", "AddInvItem", "SpawnZombie"};
    String[] availableCommandsHelp = new String[]{
        "'?' - Shows available commands",
        "'help' - Shows available commands",
        "'commands' - Shows available commands",
        "'clr' - Clears the command log",
        "'AddInvItem' - Adds an item to player inventory. USAGE - AddInvItem 'ItemName' [ammount]",
        "'SpawnZombie' - Spawn a zombie at a map location. USAGE - SpawnZombie X,Y,Z (integers)"
    };
    public boolean debounceUp;
    public boolean debounceDown;
    private static final Object outputLock = "DebugConsole Output Lock";
    private static final ByteBuffer outputBB = ByteBuffer.allocate(8192);
    private static boolean outputChanged;
    private static CharsetDecoder outputDecoder;
    private static char[] outputChars;
    private static CharBuffer outputCharBuf;

    public UIDebugConsole(int x, int y) {
        super(x, y, 10, 10, true);
        this.resizeToFitY = false;
        this.visible = true;
        instance = this;
        this.width = PZMath.max(640, Core.getInstance().getScreenWidth() / 3);
        int fontHgt = TextManager.instance.getFontHeight(UIFont.DebugConsole);
        int numLines = 11;
        int EdgeSize = 5;
        this.outputLog = new UITextBox2(
            UIFont.DebugConsole,
            5,
            this.titleMiddle.getHeight() + fontHgt,
            (int)this.width - 10,
            fontHgt * 11 + 10,
            "Project Zomboid - "
                + Core.getInstance().getVersion()
                + "\nDebug Console - "
                + this.consoleVersion
                + "\n(C) Indie Stone Studios 2025\n---------------------------------------------------------------------------------------------------------------------------\n\n",
            true
        );
        this.outputLog.multipleLine = true;
        this.outputLog.alwaysPaginate = false;
        this.outputLog.setWrapLines(false);
        this.commandLine = new UIDebugConsole.CommandEntry(
            UIFont.DebugConsole, 5, (int)(this.outputLog.getY() + this.outputLog.getHeight()) + fontHgt, (int)this.width - 10, fontHgt + 10, "", true
        );
        this.commandLine.isEditable = true;
        this.commandLine.textEntryMaxLength = 256;
        int autoSuggestHeight = fontHgt + 10;
        int autoSuggestY = (int)(this.outputLog.getY() + this.outputLog.getHeight()) - autoSuggestHeight;
        this.autosuggest = new UITextBox2(UIFont.DebugConsole, 5, autoSuggestY, 15, autoSuggestHeight, "", true);
        this.autosuggest.frame.color.set(1.0F, 1.0F, 1.0F, 1.0F);
        this.height = (int)(this.commandLine.getY() + this.commandLine.getHeight()) + 6;
        this.scrollBarV = new ScrollBar(
            "UIDebugConsoleScrollbar",
            null,
            (int)(this.outputLog.getX() + this.outputLog.getWidth()) - 14,
            this.outputLog.getY().intValue() + 4,
            this.outputLog.getHeight().intValue() - 8,
            true
        );
        this.scrollBarV.SetParentTextBox(this.outputLog);
        this.AddChild(this.outputLog);
        this.AddChild(this.scrollBarV);
        this.AddChild(this.commandLine);
        this.AddChild(this.autosuggest);
        this.InitSuggestionEngine();
        if (Core.debug) {
            BaseLib.setPrintCallback(this::SpoolText);
        }
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseDown(x, y);
            if (this.scrollBarV.isPointOver(this.getAbsoluteX() + x, this.getAbsoluteY() + y)) {
                return Boolean.FALSE;
            } else if (!this.moving && (x >= this.getWidth() - 10.0 || y >= this.getHeight() - 10.0)) {
                this.resizing = true;
                this.resizeWidth = x >= this.getWidth() - 10.0;
                this.resizeHeight = y >= this.getHeight() - 10.0;
                this.resizeStartX = Mouse.getXA();
                this.resizeStartY = Mouse.getYA();
                this.resizeStartWidth = this.getWidth().intValue();
                this.resizeStartHeight = this.getHeight().intValue();
                this.setCapture(true);
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseUp(x, y);
            if (this.resizing) {
                this.resizing = false;
                this.setCapture(false);
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }

    @Override
    public void onMouseUpOutside(double x, double y) {
        this.onMouseUp(x, y);
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseMove(dx, dy);
            if (this.resizing) {
                if (this.resizeWidth) {
                    int dx1 = Mouse.getXA() - this.resizeStartX;
                    this.setNewSize(this.resizeStartWidth + dx1, this.getHeight().intValue());
                }

                if (this.resizeHeight) {
                    int dy1 = Mouse.getYA() - this.resizeStartY;
                    this.setNewSize(this.getWidth().intValue(), this.resizeStartHeight + dy1);
                }

                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }

    private void setNewSize(int newWidth, int newHeight) {
        newWidth = PZMath.clamp(newWidth, 640, Core.getInstance().getScreenWidth() - 50);
        newHeight = PZMath.clamp(newHeight, 200, Core.getInstance().getScreenHeight() - 50);
        int otherHeight = this.getHeight().intValue() - this.outputLog.getHeight().intValue();
        int outputHeight = newHeight - otherHeight;
        int lineHeight = TextManager.instance.getFontHeight(this.outputLog.font);
        int numLines = (outputHeight - this.outputLog.getInset() * 2) / lineHeight;
        newHeight = otherHeight + numLines * lineHeight + this.outputLog.getInset() * 2;
        int EdgeSize = 5;
        if (newWidth != this.getWidth().intValue()) {
            this.setWidth(newWidth);
            this.outputLog.setWidth(this.getWidth() - 10.0);
            this.outputLog.getFrame().setWidth(this.outputLog.getWidth());
            this.scrollBarV.setX((int)(this.outputLog.getX() + this.outputLog.getWidth()) - 14);
            this.commandLine.setWidth(this.getWidth() - 10.0);
            this.commandLine.getFrame().setWidth(this.commandLine.getWidth());
        }

        if (newHeight != this.getHeight().intValue()) {
            int fontHgt = TextManager.instance.getFontHeight(UIFont.DebugConsole);
            this.setHeight(newHeight);
            this.commandLine.setY(this.getHeight().intValue() - 6 - this.commandLine.getHeight());
            this.commandLine.getFrame().setY(0.0);
            this.outputLog.setHeight(this.commandLine.getY() - fontHgt - this.outputLog.getY());
            this.outputLog.getFrame().setHeight(this.outputLog.getHeight());
            this.outputLog.update();
            this.scrollBarV.setHeight(this.outputLog.getHeight().intValue() - 8);
            this.scrollBarV.update();
            this.scrollBarV.scrollToBottom();
            this.autosuggest.setY(this.outputLog.getY() + this.outputLog.getHeight() - this.autosuggest.getHeight());
        }
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            super.render();
            this.DrawTextCentre(UIFont.DebugConsole, "Command Console", this.getWidth() / 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);
            this.DrawText(UIFont.DebugConsole, "Output Log", 7.0, this.titleMiddle.getHeight(), 0.7F, 0.7F, 1.0, 1.0);
            this.DrawText(UIFont.DebugConsole, "Lua Command Line", 7.0, this.outputLog.getY() + this.outputLog.getHeight() + 1.0, 0.7F, 0.7F, 1.0, 1.0);
            if (this.resizing || this.isMouseOver() && !this.scrollBarV.isMouseOver() && !this.scrollBarV.isBeingDragged()) {
                int mx = Mouse.getXA() - this.getAbsoluteX().intValue();
                int my = Mouse.getYA() - this.getAbsoluteY().intValue();
                int titleBarHgt = 18;
                if (this.resizing && this.resizeWidth || !this.resizing && mx >= this.getWidth().intValue() - 10 && my >= 18) {
                    double rgb = this.resizing ? 1.0 : 0.66;
                    this.DrawTextureScaledColor(null, this.getWidth() - 10.0, 0.0, 10.0, this.getHeight(), rgb, rgb, rgb, 0.66);
                }

                if (this.resizing && this.resizeHeight || !this.resizing && my >= this.getHeight().intValue() - 10) {
                    double rgb = this.resizing ? 1.0 : 0.66;
                    this.DrawTextureScaledColor(null, 0.0, this.getHeight() - 10.0, this.getWidth(), 10.0, rgb, rgb, rgb, 0.66);
                }
            }
        }
    }

    @Override
    public void update() {
        if (this.isVisible()) {
            this.setNewSize(this.getWidth().intValue(), this.getHeight().intValue());
            this.handleOutput();
            super.update();
            if (this.commandLine.getText().length() != this.inputlength && !this.commandLine.getText().isEmpty()) {
                this.inputlength = this.commandLine.getText().length();
                String[] cmdLine = this.commandLine.getText().split(":");
                String input = "";
                if (cmdLine.length > 0) {
                    input = cmdLine[cmdLine.length - 1];
                    if (cmdLine[cmdLine.length - 1].isEmpty() && this.autosuggest.isVisible()) {
                        this.autosuggest.setVisible(false);
                        return;
                    }
                }

                Method m = null;
                if (cmdLine.length > 1 && cmdLine[0].indexOf(")") > 0 && !cmdLine[cmdLine.length - 1].contains("(")) {
                    ArrayList<Method> methods = new ArrayList<>(this.globalLuaMethods);

                    for (int i = 0; i < cmdLine.length; i++) {
                        String s = cmdLine[i];
                        if (s.indexOf(")") > 0) {
                            s = s.split("\\(", 0)[0];

                            for (Method func : methods) {
                                if (func.getName().equals(s)) {
                                    methods.clear();

                                    for (Class<?> parent = func.getReturnType(); parent != null; parent = parent.getSuperclass()) {
                                        for (Method m2 : parent.getDeclaredMethods()) {
                                            if (Modifier.isPublic(m2.getModifiers())) {
                                                methods.add(m2);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    m = this.SuggestionEngine(input, methods);
                } else if (cmdLine.length == 1) {
                    m = this.SuggestionEngine(input);
                }

                String returnType = "void";
                if (m != null) {
                    if (!m.getReturnType().toString().equals("void")) {
                        String[] str = m.getReturnType().toString().split("\\.");
                        returnType = str[str.length - 1];
                    }

                    if (!this.autosuggest.isVisible()) {
                        this.autosuggest.setVisible(true);
                    }

                    this.autosuggest.SetText("<" + returnType + "> " + m.getName());
                    this.autosuggest.setX(5 * this.commandLine.getText().length());
                    this.autosuggest
                        .setWidth(this.autosuggest.getInset() * 2 + TextManager.instance.MeasureStringX(this.autosuggest.font, this.autosuggest.text));
                    this.autosuggest.frame.width = this.autosuggest.getWidth().floatValue();
                }
            } else if (this.commandLine.getText().isEmpty() && this.autosuggest.isVisible()) {
                this.autosuggest.setVisible(false);
            }
        }
    }

    public void ProcessCommand() {
        if (this.commandLine.internalText != null) {
            String ProcessString = this.commandLine.internalText;
            this.commandLine.internalText = "";
            ProcessString = ProcessString.trim();
            String[] Command = ProcessString.split(" ");
            Command[0] = Command[0].trim();
            if (this.previous.isEmpty() || !ProcessString.equals(this.previous.get(this.previous.size() - 1))) {
                this.previous.add(ProcessString);
            }

            this.previousIndex = this.previous.size();
            this.commandLine.doingTextEntry = true;
            Core.currentTextEntryBox = this.commandLine;
            if ("clear".equals(ProcessString)) {
                this.outputLog.textChanged = true;
                this.outputLog.clearInput();
            } else {
                if (DebugOptions.instance.uiDebugConsoleEchoCommand.getValue()) {
                    this.SpoolText("[USER] - \"" + ProcessString + "\".");
                }

                try {
                    LuaClosure closure = LuaCompiler.loadstring(ProcessString, "console", LuaManager.env);
                    LuaReturn var4 = LuaManager.caller.protectedCall(LuaManager.thread, closure);
                } catch (KahluaException var5) {
                    this.SpoolText(var5.getMessage());
                } catch (Exception var6) {
                    Logger.getLogger(UIDebugConsole.class.getName()).log(Level.SEVERE, null, var6);
                }
            }
        }
    }

    void historyPrev() {
        this.previousIndex--;
        if (this.previousIndex < 0) {
            this.previousIndex = 0;
        }

        if (this.previousIndex >= 0 && this.previousIndex < this.previous.size()) {
            this.commandLine.SetText(this.previous.get(this.previousIndex));
        }
    }

    void historyNext() {
        this.previousIndex++;
        if (this.previousIndex >= this.previous.size()) {
            this.previousIndex = this.previous.size() - 1;
        }

        if (this.previousIndex >= 0 && this.previousIndex < this.previous.size()) {
            this.commandLine.SetText(this.previous.get(this.previousIndex));
        }
    }

    public void onOtherKey(int key) {
        switch (key) {
            case 15:
                if (this.prevSuggestion != null) {
                    String[] cmdLine = this.commandLine.getText().split(":");
                    StringBuilder output = new StringBuilder();
                    if (cmdLine.length > 0) {
                        cmdLine[cmdLine.length - 1] = this.prevSuggestion.getName();

                        for (int i = 0; i < cmdLine.length; i++) {
                            output.append(cmdLine[i]);
                            if (i != cmdLine.length - 1) {
                                output.append(":");
                            }
                        }
                    }

                    if (this.prevSuggestion.getParameterTypes().length == 0) {
                        this.commandLine.SetText(output + "()");
                    } else {
                        this.commandLine.SetText(output + "(");
                    }
                }
        }
    }

    void ClearConsole() {
        this.outputLog.textChanged = true;
        this.outputLog.SetText("");
        this.UpdateViewPos();
    }

    void UpdateViewPos() {
        this.outputLog.topLineIndex = this.outputLog.lines.size() - this.outputLog.numVisibleLines;
        if (this.outputLog.topLineIndex < 0) {
            this.outputLog.topLineIndex = 0;
        }

        this.scrollBarV.scrollToBottom();
    }

    void SpoolText(String SpoolLine) {
        this.outputLog.textChanged = true;
        this.outputLog.SetText(this.outputLog.text + SpoolLine + "\n");
        this.UpdateViewPos();
    }

    Method SuggestionEngine(String input) {
        return this.SuggestionEngine(input, this.globalLuaMethods);
    }

    Method SuggestionEngine(String input, ArrayList<Method> methods) {
        int lowestCost = 0;
        int cost = 0;
        Method match = null;

        for (Method m : methods) {
            if (match == null) {
                match = m;
                lowestCost = this.levenshteinDistance(input, m.getName());
            } else {
                cost = this.levenshteinDistance(input, m.getName());
                if (cost < lowestCost) {
                    lowestCost = cost;
                    match = m;
                }
            }
        }

        this.prevSuggestion = match;
        return match;
    }

    void InitSuggestionEngine() {
        Class<?> global = LuaManager.GlobalObject.class;
        this.globalLuaMethods.addAll(Arrays.asList(global.getDeclaredMethods()));
    }

    public int levenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int len0 = lhs.length() + 1;
        int len1 = rhs.length() + 1;
        int[] cost = new int[len0];
        int[] newcost = new int[len0];
        int i = 0;

        while (i < len0) {
            cost[i] = i++;
        }

        for (int j = 1; j < len1; j++) {
            newcost[0] = j;

            for (int ix = 1; ix < len0; ix++) {
                int match = lhs.charAt(ix - 1) == rhs.charAt(j - 1) ? 0 : 1;
                int cost_replace = cost[ix - 1] + match;
                int cost_insert = cost[ix] + 1;
                int cost_delete = newcost[ix - 1] + 1;
                newcost[ix] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        return cost[len0 - 1];
    }

    void setSuggestWidth(int width) {
        this.autosuggest.setWidth(width);
        this.autosuggest.frame.width = width;
    }

    public void addOutput(byte[] b, int off, int len) {
        if (len >= 1) {
            synchronized (outputLock) {
                int overflow = len - outputBB.capacity();
                if (overflow > 0) {
                    off += overflow;
                    len -= overflow;
                }

                if (outputBB.position() + len > outputBB.capacity()) {
                    outputBB.clear();
                }

                outputBB.put(b, off, len);
                if (b[off + len - 1] == 10) {
                    outputChanged = true;
                }
            }
        }
    }

    private void handleOutput() {
        synchronized (outputLock) {
            if (outputChanged) {
                outputChanged = false;

                try {
                    if (outputDecoder == null) {
                        outputDecoder = Charset.forName("UTF-8")
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE);
                    }

                    outputDecoder.reset();
                    int numBytes = outputBB.position();
                    outputBB.flip();
                    int maxChars = (int)((double)numBytes * outputDecoder.maxCharsPerByte());
                    if (outputChars == null || outputChars.length < maxChars) {
                        int capacity = (maxChars + 128 - 1) / 128 * 128;
                        outputChars = new char[capacity];
                        outputCharBuf = CharBuffer.wrap(outputChars);
                    }

                    outputCharBuf.clear();
                    CoderResult numChars = outputDecoder.decode(outputBB, outputCharBuf, true);
                    outputBB.clear();
                    String text = new String(outputChars, 0, outputCharBuf.position());
                    this.outputLog.textChanged = true;
                    this.outputLog.SetText(this.outputLog.text + text);
                    int MAX_CHARS = 8192;
                    if (this.outputLog.text.length() > 8192) {
                        int start = this.outputLog.text.length() - 8192;

                        while (start < this.outputLog.text.length() && this.outputLog.text.charAt(start) != '\n') {
                            start++;
                        }

                        this.outputLog.textChanged = true;
                        this.outputLog.SetText(this.outputLog.text.substring(start + 1));
                    }
                } catch (Exception var9) {
                }

                this.UpdateViewPos();
            }
        }
    }

    private class CommandEntry extends UITextBox2 {
        public CommandEntry(final UIFont font, final int x, final int y, final int width, final int height, final String text, final boolean HasFrame) {
            Objects.requireNonNull(UIDebugConsole.this);
            super(font, x, y, width, height, text, HasFrame);
        }

        @Override
        public void onPressUp() {
            UIDebugConsole.this.historyPrev();
        }

        @Override
        public void onPressDown() {
            UIDebugConsole.this.historyNext();
        }

        @Override
        public void onOtherKey(int key) {
            UIDebugConsole.this.onOtherKey(key);
        }
    }
}
