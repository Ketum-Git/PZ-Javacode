// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class AttachmentEditorState extends GameState {
    public static AttachmentEditorState instance;
    private static final String INDENT = "    ";
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;
    private final ArrayList<String> clipNames = new ArrayList<>();

    @Override
    public void enter() {
        instance = this;
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("AttachmentEditorState_InitUI"));
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), this.table);
        }

        this.exit = false;
    }

    @Override
    public void yield() {
        this.restoreGameUI();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
        this.restoreGameUI();
    }

    @Override
    public void render() {
        int playerIndex = 0;
        Core.getInstance().StartFrame(0, true);
        this.renderScene();
        Core.getInstance().EndFrame(0);
        Core.getInstance().RenderOffScreenBuffer();
        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }

        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        if (!this.exit && !GameKeyboard.isKeyPressed(65)) {
            this.updateScene();
            return GameStateMachine.StateAction.Remain;
        } else {
            return GameStateMachine.StateAction.Continue;
        }
    }

    public static AttachmentEditorState checkInstance() {
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new AttachmentEditorState() : instance;
    }

    private void saveGameUI() {
        this.gameUi.clear();
        this.gameUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        this.suspendUi = UIManager.suspend;
        UIManager.suspend = false;
        UIManager.setShowPausedMessage(false);
        UIManager.defaultthread = this.luaEnv.thread;
    }

    private void restoreGameUI() {
        this.selfUi.clear();
        this.selfUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        UIManager.UI.addAll(this.gameUi);
        UIManager.suspend = this.suspendUi;
        UIManager.setShowPausedMessage(true);
        UIManager.defaultthread = LuaManager.thread;
    }

    private void updateScene() {
        ModelManager.instance.update();
        if (GameKeyboard.isKeyPressed(17)) {
            DebugOptions.instance.model.render.wireframe.setValue(!DebugOptions.instance.model.render.wireframe.getValue());
        }
    }

    private void renderScene() {
    }

    private void renderUI() {
        UIManager.render();
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "getClipNames":
                if (this.clipNames.isEmpty()) {
                    for (AnimationClip clip : ModelManager.instance.getAllAnimationClips()) {
                        this.clipNames.add(clip.name);
                    }

                    this.clipNames.sort(Comparator.naturalOrder());
                }

                return this.clipNames;
            case "exit":
                this.exit = true;
                return null;
            default:
                throw new IllegalArgumentException("unhandled \"" + func + "\"");
        }
    }

    public Object fromLua1(String func, Object arg0) {
        byte var4 = -1;
        switch (func.hashCode()) {
            case 1396535690:
                if (func.equals("writeScript")) {
                    var4 = 0;
                }
            default:
                switch (var4) {
                    case 0:
                        ModelScript modelScript = ScriptManager.instance.getModelScript((String)arg0);
                        if (modelScript == null) {
                            throw new NullPointerException("model script \"" + arg0 + "\" not found");
                        }

                        ArrayList<String> tokens = readScript(modelScript.getFileName());

                        try {
                            readScriptNew(modelScript);
                        } catch (Exception var9) {
                            var9.printStackTrace();
                        }

                        if (tokens != null) {
                            String fileName = modelScript.getFileName();
                            if (updateScript(fileName, tokens, modelScript)) {
                                String absolutePath = ZomboidFileSystem.instance.getString(fileName);
                                this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
                            }
                        }

                        return null;
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
                }
        }
    }

    private static String formatFloat(float value) {
        if (!Float.isInfinite(value) && value == Math.floor(value)) {
            return String.format("%.1ff", value);
        } else {
            String valueStr = String.format(Locale.US, "%.4f", value);
            return "%sf".formatted(new BigDecimal(valueStr).stripTrailingZeros().toPlainString());
        }
    }

    public static void readScriptNew(ModelScript script) throws IOException {
        Path path = Path.of("../src/generation/ModelScriptGenerator.java");
        List<String> lines = Files.readAllLines(path);
        StringBuilder sb = new StringBuilder();
        new StringBuilder();
        boolean inModel = false;

        for (int i = 0; i < lines.size(); i++) {
            String originalLine = lines.get(i);
            if (originalLine.trim().startsWith(".add(ModelBuilder.withId(ModelKey")) {
                String type = originalLine.trim().replaceFirst("\\.add\\(ModelBuilder\\.withId\\(ModelKey\\.", "").replaceFirst("\\)", "");
                if (type.equals(script.getName().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())) {
                    inModel = true;
                    System.out.println(" Model line: .(ModelScriptGenerator.java:%s)".formatted(i + 1));
                } else if (inModel) {
                    inModel = false;
                }

                if (inModel) {
                    sb.append(originalLine.trim()).append("\n");
                    sb.append("%s.mesh(\"%s\")\n".formatted("    ".repeat(4), script.getMeshName()));
                    sb.append("%s.texture(\"%s\")\n".formatted("    ".repeat(4), script.getTextureName()));

                    for (int i1 = 0; i1 < script.getAttachmentCount(); i1++) {
                        StringBuilder attachmentSb = new StringBuilder();
                        boolean addedAttachment = false;
                        ModelAttachment attachment = script.getAttachment(i1);
                        attachmentSb.append("%s.addAttachment(attachment(\"%s\")\n".formatted("    ".repeat(4), attachment.getId()));
                        if (attachment.getOffset().x() != 0.0F || attachment.getOffset().y() != 0.0F || attachment.getOffset().z() != 0.0F) {
                            attachmentSb.append(
                                "%s.offset(%s, %s, %s)\n"
                                    .formatted(
                                        "    ".repeat(5),
                                        formatFloat(attachment.getOffset().x()),
                                        formatFloat(attachment.getOffset().y()),
                                        formatFloat(attachment.getOffset().z())
                                    )
                            );
                            addedAttachment = true;
                        }

                        if (attachment.getRotate().x() != 0.0F || attachment.getRotate().y() != 0.0F || attachment.getRotate().z() != 0.0F) {
                            attachmentSb.append(
                                "%s.rotate(%s, %s, %s)\n"
                                    .formatted(
                                        "    ".repeat(5),
                                        formatFloat(attachment.getRotate().x()),
                                        formatFloat(attachment.getRotate().y()),
                                        formatFloat(attachment.getRotate().z())
                                    )
                            );
                            addedAttachment = true;
                        }

                        if (attachment.getScale() != 1.0F) {
                            attachmentSb.append("%s.scale(%s)\n".formatted("    ".repeat(5), formatFloat(attachment.getScale())));
                            addedAttachment = true;
                        }

                        if (attachment.getBone() != null) {
                            attachmentSb.append("%s.bone(\"%s\")\n".formatted("    ".repeat(5), attachment.getBone()));
                            addedAttachment = true;
                        }

                        attachmentSb.append("%s)\n".formatted("    ".repeat(4)));
                        if (addedAttachment) {
                            sb.append((CharSequence)attachmentSb);
                        }
                    }

                    sb.append("%s)".formatted("    ".repeat(3)));
                }
            }
        }

        System.out.println(sb);
    }

    public static ArrayList<String> readScript(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        fileName = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(fileName);

        try (
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
        ) {
            String EOL = System.lineSeparator();

            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(EOL);
            }
        } catch (Throwable var11) {
            ExceptionLogger.logException(var11);
            return null;
        }

        String totalFile = ScriptParser.stripComments(stringBuilder.toString());
        return ScriptParser.parseTokens(totalFile);
    }

    public static boolean updateScript(String fileName, ArrayList<String> tokens, ModelScript modelScript) {
        fileName = ZomboidFileSystem.instance.getString(fileName);

        for (int i = tokens.size() - 1; i >= 0; i--) {
            String token = tokens.get(i).trim();
            int firstOpen = token.indexOf("{");
            int lastClose = token.lastIndexOf("}");
            String header = token.substring(0, firstOpen);
            if (header.startsWith("module")) {
                header = token.substring(0, firstOpen).trim();
                String[] ss = header.split("\\s+");
                String moduleName = ss.length > 1 ? ss[1].trim() : "";
                if (moduleName.equals(modelScript.getModule().getName())) {
                    String body = token.substring(firstOpen + 1, lastClose).trim();
                    ArrayList<String> tokens1 = ScriptParser.parseTokens(body);

                    for (int j = tokens1.size() - 1; j >= 0; j--) {
                        String token1 = tokens1.get(j).trim();
                        if (token1.startsWith("model")) {
                            firstOpen = token1.indexOf("{");
                            header = token1.substring(0, firstOpen).trim();
                            ss = header.split("\\s+");
                            String scriptName = ss.length > 1 ? ss[1].trim() : "";
                            if (scriptName.equals(modelScript.getName())) {
                                token1 = modelScriptToText(modelScript, token1).trim();
                                tokens1.set(j, token1);
                                String EOL = System.lineSeparator();
                                String moduleStr = String.join(EOL + "\t", tokens1);
                                moduleStr = "module " + moduleName + EOL + "{" + EOL + "\t" + moduleStr + EOL + "}" + EOL;
                                tokens.set(i, moduleStr);
                                return writeScript(fileName, tokens);
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static String modelScriptToText(ModelScript modelScript, String token) {
        ScriptParser.Block block = ScriptParser.parse(token);
        block = block.children.get(0);

        for (int i = block.children.size() - 1; i >= 0; i--) {
            ScriptParser.Block block1 = block.children.get(i);
            if ("attachment".equals(block1.type)) {
                block.elements.remove(block1);
                block.children.remove(i);
            }
        }

        for (int ix = 0; ix < modelScript.getAttachmentCount(); ix++) {
            ModelAttachment attach = modelScript.getAttachment(ix);
            ScriptParser.Block block1 = block.getBlock("attachment", attach.getId());
            if (block1 == null) {
                block1 = new ScriptParser.Block();
                block1.type = "attachment";
                block1.id = attach.getId();
                block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", attach.getOffset().x(), attach.getOffset().y(), attach.getOffset().z()));
                block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", attach.getRotate().x(), attach.getRotate().y(), attach.getRotate().z()));
                if (attach.getScale() != 1.0F) {
                    block1.setValue("scale", String.format(Locale.US, "%.4f", attach.getScale()));
                }

                if (attach.getBone() != null) {
                    block1.setValue("bone", attach.getBone());
                }

                block.elements.add(block1);
                block.children.add(block1);
            } else {
                block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", attach.getOffset().x(), attach.getOffset().y(), attach.getOffset().z()));
                block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", attach.getRotate().x(), attach.getRotate().y(), attach.getRotate().z()));
                if (attach.getScale() != 1.0F) {
                    block1.setValue("scale", String.format(Locale.US, "%.4f", attach.getScale()));
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrint(1, stringBuilder, eol);
        return stringBuilder.toString();
    }

    public static boolean writeScript(String fileName, ArrayList<String> tokens) {
        String absolutePath = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(absolutePath);

        try {
            boolean var13;
            try (
                FileWriter fw = new FileWriter(file);
                BufferedWriter br = new BufferedWriter(fw);
            ) {
                DebugLog.General.printf("writing %s\n", fileName);

                for (String token : tokens) {
                    br.write(token);
                }

                var13 = true;
            }

            return var13;
        } catch (Throwable var12) {
            ExceptionLogger.logException(var12);
            return false;
        }
    }
}
