// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.LuaCaller;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaThread;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.Clipboard;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.ModelManager;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.gameStates.AttachmentEditorState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.input.GameKeyboard;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class EditVehicleState extends GameState {
    private static final String INDENT = "    ";
    public static EditVehicleState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private String initialScript;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private KahluaTable table;

    public EditVehicleState() {
        instance = this;
    }

    @Override
    public void enter() {
        instance = this;
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("EditVehicleState_InitUI"));
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

    public static EditVehicleState checkInstance() {
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new EditVehicleState() : instance;
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

    public void setScript(String scriptName) {
        if (this.table == null) {
            this.initialScript = scriptName;
        } else {
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("setScript"), this.table, scriptName);
        }
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit":
                this.exit = true;
                return null;
            case "getInitialScript":
                return this.initialScript;
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
                        VehicleScript vehicleScript = ScriptManager.instance.getVehicle((String)arg0);
                        if (vehicleScript == null) {
                            throw new NullPointerException("vehicle script \"" + arg0 + "\" not found");
                        }

                        ArrayList<String> tokens = this.readScript(vehicleScript.getFileName());

                        try {
                            this.readScriptNew(vehicleScript);
                        } catch (Exception var8) {
                            var8.printStackTrace();
                        }

                        if (tokens != null) {
                            this.updateScript(vehicleScript.getFileName(), tokens, vehicleScript);
                        }

                        this.updateModelScripts(vehicleScript);
                        return null;
                    default:
                        throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
                }
        }
    }

    private void readScriptNew(VehicleScript vehicleScript) throws IOException {
        Path path = Path.of("../src/main/java/generation/VehicleScriptGenerator.java");
        List<String> lines = Files.readAllLines(path);
        StringBuilder sb = new StringBuilder();
        new StringBuilder();
        boolean inVehicle = false;
        boolean foundWheel = false;

        for (int i = 0; i < lines.size(); i++) {
            String originalLine = lines.get(i);
            if (originalLine.trim().startsWith(".add(VehicleBuilder.withId(VehicleKey")) {
                String type = originalLine.trim().replaceFirst("\\.add\\(VehicleBuilder\\.withId\\(VehicleKey\\.", "").replaceFirst("\\)", "");
                if (type.equals(vehicleScript.getName().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())) {
                    inVehicle = true;
                    System.out.println(" Vehicle line: .(VehicleScriptGenerator.java:%s)".formatted(i + 1));
                } else if (inVehicle) {
                    inVehicle = false;
                }
            }

            if (inVehicle) {
                sb.append(originalLine).append("\n");
                i = this.removeBlock(originalLine, ".addPhysics(", lines, i, sb);
                i = this.removeBlock(originalLine, ".addArea(", lines, i, sb);
                i = this.removeBlock(originalLine, ".addAttachment(", lines, i, sb);
                i = this.removeBlock(originalLine, ".addPassenger(", lines, i, sb);
                if (originalLine.trim().startsWith(".addModel(model()")) {
                    while (!originalLine.trim().startsWith(")")) {
                        String var19 = lines.get(++i);
                        String var20 = parseFloat("scale", var19, vehicleScript.getModelScale());
                        originalLine = parseFloat(
                            "offset",
                            var20,
                            vehicleScript.getModel().getOffset().x / vehicleScript.getModelScale(),
                            vehicleScript.getModel().getOffset().y / vehicleScript.getModelScale(),
                            vehicleScript.getModel().getOffset().z / vehicleScript.getModelScale()
                        );
                        sb.append(originalLine).append("\n");
                    }
                }

                for (int x = 0; x < vehicleScript.getPartCount(); x++) {
                    VehicleScript.Part part = vehicleScript.getPart(x);

                    for (int j = 0; j < part.getModelCount(); j++) {
                        VehicleScript.Model model = part.getModel(j);
                        if (originalLine.trim().startsWith(".addModel(model(\"%s\"".formatted(model.getId()))) {
                            int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;

                            while (!originalLine.startsWith("%s)".formatted("    ".repeat(indentCount)))) {
                                String var21 = lines.get(++i);
                                String var22 = parseFloat("offset", var21, model.getOffset().x, model.getOffset().y, model.getOffset().z);
                                originalLine = parseFloat("rotate", var22, model.getRotate().x, model.getRotate().y, model.getRotate().z);
                                sb.append(originalLine).append("\n");
                            }
                        }
                    }
                }

                for (int jx = 0; jx < vehicleScript.getWheelCount(); jx++) {
                    VehicleScript.Wheel wheel = vehicleScript.getWheel(jx);
                    if (originalLine.trim()
                        .startsWith(".addWheel(wheel(VehicleWheel.%s".formatted(wheel.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()))) {
                        foundWheel = true;
                        int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;
                        float modelScale = vehicleScript.getModelScale();

                        while (!originalLine.startsWith("%s)".formatted("    ".repeat(indentCount)))) {
                            String var23 = lines.get(++i);
                            originalLine = parseFloat(
                                "offset", var23, wheel.getOffset().x / modelScale, wheel.getOffset().y / modelScale, wheel.getOffset().z / modelScale
                            );
                            sb.append(originalLine).append("\n");
                        }
                    }
                }

                if (originalLine.trim().startsWith(".extents(")) {
                    Vector3f v = vehicleScript.getExtents();
                    originalLine = parseFloat(
                        "extents", originalLine, v.x / vehicleScript.getModelScale(), v.y / vehicleScript.getModelScale(), v.z / vehicleScript.getModelScale()
                    );
                }

                if (originalLine.trim().startsWith(".physicsChassisShape(") && vehicleScript.hasPhysicsChassisShape()) {
                    Vector3f v = vehicleScript.getPhysicsChassisShape();
                    originalLine = parseFloat(
                        "physicsChassisShape",
                        originalLine,
                        v.x / vehicleScript.getModelScale(),
                        v.y / vehicleScript.getModelScale(),
                        v.z / vehicleScript.getModelScale()
                    );
                }

                if (originalLine.trim().startsWith(".centerOfMassOffset(")) {
                    Vector3f v = vehicleScript.getCenterOfMassOffset();
                    originalLine = parseFloat(
                        "centerOfMassOffset",
                        originalLine,
                        v.x / vehicleScript.getModelScale(),
                        v.y / vehicleScript.getModelScale(),
                        v.z / vehicleScript.getModelScale()
                    );
                }

                if (originalLine.trim().startsWith(".shadowExtents(")) {
                    Vector2f v = vehicleScript.getShadowExtents();
                    originalLine = parseFloat("shadowExtents", originalLine, v.x / vehicleScript.getModelScale(), v.y / vehicleScript.getModelScale());
                }

                if (originalLine.trim().startsWith(".shadowOffset(")) {
                    Vector2f v = vehicleScript.getShadowOffset();
                    originalLine = parseFloat("shadowOffset", originalLine, v.x / vehicleScript.getModelScale(), v.y / vehicleScript.getModelScale());
                }

                if (originalLine.startsWith("%s)".formatted("    ".repeat(3)))) {
                    this.addBlocks(sb, originalLine, vehicleScript);
                    break;
                }
            }
        }

        if (!foundWheel) {
            sb.append("\n Might have found extra wheels:\n");

            for (int jxx = 0; jxx < vehicleScript.getWheelCount(); jxx++) {
                VehicleScript.Wheel wheel = vehicleScript.getWheel(jxx);
                sb.append(
                    "%s.addWheel(wheel(VehicleWheel.%s)\n".formatted("    ".repeat(3), wheel.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())
                );
                float modelScale = vehicleScript.getModelScale();
                sb.append("%s.front(%s)\n".formatted("    ".repeat(4), wheel.front));
                sb.append(
                    "%s.offset(%s, %s, %s)\n"
                        .formatted(
                            "    ".repeat(4),
                            formatFloat(wheel.getOffset().x / modelScale),
                            formatFloat(wheel.getOffset().y / modelScale),
                            formatFloat(wheel.getOffset().z / modelScale)
                        )
                );
                sb.append("%s.radius(%s)\n".formatted("    ".repeat(4), formatFloat(wheel.radius / modelScale)));
                sb.append("%s.width(%s)\n".formatted("    ".repeat(4), formatFloat(wheel.width)));
                sb.append("%s)\n".formatted("    ".repeat(3)));
            }
        }

        System.out.println(sb);
        Clipboard.setClipboard(sb.toString());
    }

    private void addBlocks(StringBuilder sb, String originalLine, VehicleScript script) {
        sb.setLength(sb.length() - originalLine.length() - 1);
        this.addPhysicsBlock(sb, script);
        this.addAreaBlock(sb, script);
        this.addAttachmentBlock(sb, script);
        this.addPassengerBlock(sb, script);
        sb.append("%s)".formatted("    ".repeat(3)));
    }

    private void addPassengerBlock(StringBuilder sb, VehicleScript vehicleScript) {
        float modelScale = vehicleScript.getModelScale();

        for (int i = 0; i < vehicleScript.getPassengerCount(); i++) {
            VehicleScript.Passenger pngr = vehicleScript.getPassenger(i);
            sb.append(
                "%s.addPassenger(passenger(VehiclePassenger.%s)\n"
                    .formatted("    ".repeat(4), pngr.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())
            );

            for (VehicleScript.Position posn : pngr.positions) {
                sb.append(
                    "%s.addPosition(position(VehiclePosition.%s)\n"
                        .formatted("    ".repeat(5), posn.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())
                );
                sb.append(
                    "%s.offset(%s, %s, %s)\n"
                        .formatted(
                            "    ".repeat(6),
                            formatFloat(posn.getOffset().x() / modelScale),
                            formatFloat(posn.getOffset().y() / modelScale),
                            formatFloat(posn.getOffset().z() / modelScale)
                        )
                );
                sb.append(
                    "%s.rotate(%s, %s, %s)\n"
                        .formatted(
                            "    ".repeat(6),
                            formatFloat(posn.getRotate().x() / modelScale),
                            formatFloat(posn.getRotate().y() / modelScale),
                            formatFloat(posn.getRotate().z() / modelScale)
                        )
                );
                sb.append("%s)\n".formatted("    ".repeat(5)));
            }

            sb.append("%s)\n".formatted("    ".repeat(4)));
        }
    }

    private void addAttachmentBlock(StringBuilder sb, VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getAttachmentCount(); i++) {
            ModelAttachment attach = vehicleScript.getAttachment(i);
            float modelScale = vehicleScript.getModelScale();
            sb.append(
                "%s.addAttachment(attachment(VehicleAttachment.%s)\n"
                    .formatted("    ".repeat(4), attach.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase())
            );
            sb.append(
                "%s.offset(%s, %s, %s)\n"
                    .formatted(
                        "    ".repeat(5),
                        formatFloat(attach.getOffset().x() / modelScale),
                        formatFloat(attach.getOffset().y() / modelScale),
                        formatFloat(attach.getOffset().z() / modelScale)
                    )
            );
            sb.append(
                "%s.rotate(%s, %s, %s)\n"
                    .formatted("    ".repeat(5), formatFloat(attach.getRotate().x()), formatFloat(attach.getRotate().y()), formatFloat(attach.getRotate().z()))
            );
            if (attach.getBone() != null) {
                sb.append("%s.bone(%s)\n".formatted("    ".repeat(5), attach.getBone()));
            }

            if (attach.getCanAttach() != null) {
                sb.append("%s.canAttach(%s)\n".formatted("    ".repeat(5), PZArrayUtil.arrayToString(attach.getCanAttach(), "\"", "\"", ",")));
            }

            if (attach.getZOffset() != 0.0F) {
                sb.append("%s.zoffset(%s)\n".formatted("    ".repeat(5), formatFloat(attach.getZOffset())));
            }

            if (!attach.isUpdateConstraint()) {
                sb.append("%s.updateconstraint(%s)\n".formatted("    ".repeat(5), "false"));
            }

            sb.append("%s)\n".formatted("    ".repeat(4)));
        }
    }

    private void addAreaBlock(StringBuilder sb, VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getAreaCount(); i++) {
            VehicleScript.Area area = vehicleScript.getArea(i);
            sb.append("%s.addArea(area(VehicleArea.%s)\n".formatted("    ".repeat(4), area.getId().replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase()));
            float scale = vehicleScript.getModelScale();
            sb.append(
                "%s.xywh(%s, %s, %s, %s)\n"
                    .formatted(
                        "    ".repeat(5),
                        formatFloat(area.getX().floatValue() / scale),
                        formatFloat(area.getY().floatValue() / scale),
                        formatFloat(area.getW().floatValue() / scale),
                        formatFloat(area.getH().floatValue() / scale)
                    )
            );
            sb.append("%s)\n".formatted("    ".repeat(4)));
        }
    }

    private void addPhysicsBlock(StringBuilder sb, VehicleScript vehicleScript) {
        float modelScale = vehicleScript.getModelScale();

        for (int i = 0; i < vehicleScript.getPhysicsShapeCount(); i++) {
            VehicleScript.PhysicsShape shape = vehicleScript.getPhysicsShape(i);
            sb.append("%s.addPhysics(physics(\"%s\")\n".formatted("    ".repeat(4), shape.getTypeString()));
            sb.append(
                "%s.offset(%s, %s, %s)\n"
                    .formatted(
                        "    ".repeat(5),
                        formatFloat(shape.getOffset().x() / modelScale),
                        formatFloat(shape.getOffset().y() / modelScale),
                        formatFloat(shape.getOffset().z() / modelScale)
                    )
            );
            if (shape.type == 1) {
                sb.append(
                    "%s.extents(%s, %s, %s)\n"
                        .formatted(
                            "    ".repeat(5),
                            formatFloat(shape.getExtents().x() / modelScale),
                            formatFloat(shape.getExtents().y() / modelScale),
                            formatFloat(shape.getExtents().z() / modelScale)
                        )
                );
                sb.append(
                    "%s.rotate(%s, %s, %s)\n"
                        .formatted("    ".repeat(5), formatFloat(shape.getRotate().x()), formatFloat(shape.getRotate().y()), formatFloat(shape.getRotate().z()))
                );
            }

            if (shape.type == 2) {
                sb.append("%s.radius(%s)\n".formatted("    ".repeat(5), formatFloat(shape.getRadius() / modelScale)));
            }

            if (shape.type == 3) {
                sb.append(
                    "%s.rotate(%s, %s, %s)\n"
                        .formatted("    ".repeat(5), formatFloat(shape.getRotate().x()), formatFloat(shape.getRotate().y()), formatFloat(shape.getRotate().z()))
                );
                sb.append("%s.scale(%s)\n".formatted("    ".repeat(5), formatFloat(shape.getExtents().x() / modelScale)));
                sb.append("%s.physicsShapeScript(%s)\n".formatted("    ".repeat(5), shape.getPhysicsShapeScript()));
            }

            sb.append("%s)\n".formatted("    ".repeat(4)));
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

    private int removeBlock(String originalLine, String type, List<String> lines, int i, StringBuilder sb) {
        if (originalLine.trim().startsWith(type)) {
            int indentCount = (originalLine.length() - originalLine.stripLeading().length()) / 4;
            sb.setLength(sb.length() - originalLine.length() - 1);

            while (!originalLine.startsWith("%s)".formatted("    ".repeat(indentCount)))) {
                originalLine = lines.get(++i);
            }
        }

        return i;
    }

    private static String parseFloat(String type, String originalLine, Float... values) {
        if (originalLine.trim().startsWith(".%s(".formatted(type))) {
            Stream var10000 = Arrays.stream(values).map(EditVehicleState::formatFloat);
            String var4 = "%s";
            String collect = var10000.<CharSequence>map(xva$0 -> "%s".formatted(xva$0)).collect(Collectors.joining(", "));
            originalLine = originalLine.replaceAll("[.]%s[(][^)]*[)]".formatted(type), ".%s(%s)".formatted(type, collect));
        }

        return originalLine;
    }

    private ArrayList<String> readScript(String fileName) {
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
        } catch (Throwable var12) {
            ExceptionLogger.logException(var12);
            return null;
        }

        String totalFile = ScriptParser.stripComments(stringBuilder.toString());
        return ScriptParser.parseTokens(totalFile);
    }

    private void updateScript(String fileName, ArrayList<String> tokens, VehicleScript vehicleScript) {
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
                if (moduleName.equals(vehicleScript.getModule().getName())) {
                    String body = token.substring(firstOpen + 1, lastClose).trim();
                    ArrayList<String> tokens1 = ScriptParser.parseTokens(body);

                    for (int j = tokens1.size() - 1; j >= 0; j--) {
                        String token1 = tokens1.get(j).trim();
                        if (token1.startsWith("vehicle")) {
                            firstOpen = token1.indexOf("{");
                            header = token1.substring(0, firstOpen).trim();
                            ss = header.split("\\s+");
                            String scriptName = ss.length > 1 ? ss[1].trim() : "";
                            if (scriptName.equals(vehicleScript.getName())) {
                                token1 = this.vehicleScriptToText(vehicleScript, token1).trim();
                                tokens1.set(j, token1);
                                String EOL = System.lineSeparator();
                                String moduleStr = String.join(EOL + "\t", tokens1);
                                moduleStr = "module " + moduleName + EOL + "{" + EOL + "\t" + moduleStr + EOL + "}" + EOL;
                                tokens.set(i, moduleStr);
                                this.writeScript(fileName, tokens);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private String vehicleScriptToText(VehicleScript vehicleScript, String token) {
        float scale = vehicleScript.getModelScale();
        ScriptParser.Block block = ScriptParser.parse(token);
        block = block.children.get(0);
        VehicleScript.Model vsm = vehicleScript.getModel();
        ScriptParser.Block block1 = block.getBlock("model", null);
        if (vsm != null && block1 != null) {
            float modelScale = vehicleScript.getModelScale();
            block1.setValue("scale", String.format(Locale.US, "%.4f", modelScale));
            Vector3f v = vehicleScript.getModel().getOffset();
            block1.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", v.x / scale, v.y / scale, v.z / scale));
        }

        ArrayList<ScriptParser.Block> existingPhysicsShapes = new ArrayList<>();

        for (int i = 0; i < block.children.size(); i++) {
            ScriptParser.Block block1x = block.children.get(i);
            if ("physics".equals(block1x.type)) {
                if (existingPhysicsShapes.size() == vehicleScript.getPhysicsShapeCount()) {
                    block.elements.remove(block1x);
                    block.children.remove(i);
                    i--;
                } else {
                    existingPhysicsShapes.add(block1x);
                }
            }
        }

        for (int ix = 0; ix < vehicleScript.getPhysicsShapeCount(); ix++) {
            VehicleScript.PhysicsShape shape = vehicleScript.getPhysicsShape(ix);
            boolean replace = ix < existingPhysicsShapes.size();
            ScriptParser.Block block1x = replace ? existingPhysicsShapes.get(ix) : new ScriptParser.Block();
            block1x.type = "physics";
            block1x.id = shape.getTypeString();
            if (replace) {
                block1x.elements.clear();
                block1x.children.clear();
                block1x.values.clear();
            }

            block1x.setValue(
                "offset",
                String.format(Locale.US, "%.4f %.4f %.4f", shape.getOffset().x() / scale, shape.getOffset().y() / scale, shape.getOffset().z() / scale)
            );
            if (shape.type == 1) {
                block1x.setValue(
                    "extents",
                    String.format(Locale.US, "%.4f %.4f %.4f", shape.getExtents().x() / scale, shape.getExtents().y() / scale, shape.getExtents().z() / scale)
                );
                block1x.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", shape.getRotate().x(), shape.getRotate().y(), shape.getRotate().z()));
            }

            if (shape.type == 2) {
                block1x.setValue("radius", String.format(Locale.US, "%.4f", shape.getRadius() / scale));
            }

            if (shape.type == 3) {
                block1x.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", shape.getRotate().x(), shape.getRotate().y(), shape.getRotate().z()));
                block1x.setValue("physicsShapeScript", shape.getPhysicsShapeScript());
                block1x.setValue("scale", String.format(Locale.US, "%.4f", shape.getExtents().x() / scale));
            }

            if (!replace) {
                block.elements.add(block1x);
                block.children.add(block1x);
            }
        }

        this.removeAttachments(block);

        for (int ix = 0; ix < vehicleScript.getAttachmentCount(); ix++) {
            ModelAttachment attach = vehicleScript.getAttachment(ix);
            this.attachmentToBlock(vehicleScript, attach, block);
        }

        Vector3f v = vehicleScript.getExtents();
        block.setValue("extents", String.format(Locale.US, "%.4f %.4f %.4f", v.x / scale, v.y / scale, v.z / scale));
        if (vehicleScript.hasPhysicsChassisShape()) {
            Vector3f vx = vehicleScript.getPhysicsChassisShape();
            block.setValue("physicsChassisShape", String.format(Locale.US, "%.4f %.4f %.4f", vx.x / scale, vx.y / scale, vx.z / scale));
        }

        Vector3f vx = vehicleScript.getCenterOfMassOffset();
        block.setValue("centerOfMassOffset", String.format(Locale.US, "%.4f %.4f %.4f", vx.x / scale, vx.y / scale, vx.z / scale));
        Vector2f vxx = vehicleScript.getShadowExtents();
        boolean exists = block.getValue("shadowExtents") != null;
        block.setValue("shadowExtents", String.format(Locale.US, "%.4f %.4f", vxx.x / scale, vxx.y / scale));
        if (!exists) {
            block.moveValueAfter("shadowExtents", "centerOfMassOffset");
        }

        Vector2f vxxx = vehicleScript.getShadowOffset();
        exists = block.getValue("shadowOffset") != null;
        block.setValue("shadowOffset", String.format(Locale.US, "%.4f %.4f", vxxx.x / scale, vxxx.y / scale));
        if (!exists) {
            block.moveValueAfter("shadowOffset", "shadowExtents");
        }

        for (int ix = 0; ix < vehicleScript.getAreaCount(); ix++) {
            VehicleScript.Area area = vehicleScript.getArea(ix);
            ScriptParser.Block block1xx = block.getBlock("area", area.getId());
            if (block1xx != null) {
                block1xx.setValue(
                    "xywh", String.format(Locale.US, "%.4f %.4f %.4f %.4f", area.getX() / scale, area.getY() / scale, area.getW() / scale, area.getH() / scale)
                );
            }
        }

        for (int ixx = 0; ixx < vehicleScript.getPartCount(); ixx++) {
            VehicleScript.Part part = vehicleScript.getPart(ixx);
            ScriptParser.Block block1xx = block.getBlock("part", part.getId());
            if (block1xx != null) {
                for (int j = 0; j < part.getModelCount(); j++) {
                    VehicleScript.Model model = part.getModel(j);
                    ScriptParser.Block block2 = block1xx.getBlock("model", model.getId());
                    if (block2 != null) {
                        block2.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", model.offset.x, model.offset.y, model.offset.z));
                        block2.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", model.rotate.x, model.rotate.y, model.rotate.z));
                    }
                }
            }
        }

        for (int ixxx = 0; ixxx < vehicleScript.getPassengerCount(); ixxx++) {
            VehicleScript.Passenger pngr = vehicleScript.getPassenger(ixxx);
            ScriptParser.Block block1xx = block.getBlock("passenger", pngr.getId());
            if (block1xx != null) {
                for (VehicleScript.Position posn : pngr.positions) {
                    ScriptParser.Block block2 = block1xx.getBlock("position", posn.id);
                    if (block2 != null) {
                        block2.setValue(
                            "offset", String.format(Locale.US, "%.4f %.4f %.4f", posn.offset.x / scale, posn.offset.y / scale, posn.offset.z / scale)
                        );
                        block2.setValue(
                            "rotate", String.format(Locale.US, "%.4f %.4f %.4f", posn.rotate.x / scale, posn.rotate.y / scale, posn.rotate.z / scale)
                        );
                    }
                }
            }
        }

        for (int ixxxx = 0; ixxxx < vehicleScript.getWheelCount(); ixxxx++) {
            VehicleScript.Wheel wheel = vehicleScript.getWheel(ixxxx);
            ScriptParser.Block block1xx = block.getBlock("wheel", wheel.getId());
            if (block1xx != null) {
                block1xx.setValue("offset", String.format(Locale.US, "%.4f %.4f %.4f", wheel.offset.x / scale, wheel.offset.y / scale, wheel.offset.z / scale));
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        String eol = System.lineSeparator();
        block.prettyPrint(1, stringBuilder, eol);
        return stringBuilder.toString();
    }

    private void removeAttachments(ScriptParser.Block block) {
        for (int i = block.children.size() - 1; i >= 0; i--) {
            ScriptParser.Block block1 = block.children.get(i);
            if ("attachment".equals(block1.type)) {
                block.elements.remove(block1);
                block.children.remove(i);
            }
        }
    }

    private void attachmentToBlock(VehicleScript vehicleScript, ModelAttachment attach, ScriptParser.Block block) {
        float scale = vehicleScript.getModelScale();
        ScriptParser.Block block1 = block.getBlock("attachment", attach.getId());
        if (block1 == null) {
            block1 = new ScriptParser.Block();
            block1.type = "attachment";
            block1.id = attach.getId();
            block.elements.add(block1);
            block.children.add(block1);
        }

        block1.setValue(
            "offset",
            String.format(Locale.US, "%.4f %.4f %.4f", attach.getOffset().x() / scale, attach.getOffset().y() / scale, attach.getOffset().z() / scale)
        );
        block1.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", attach.getRotate().x(), attach.getRotate().y(), attach.getRotate().z()));
        if (attach.getBone() != null) {
            block1.setValue("bone", attach.getBone());
        }

        if (attach.getCanAttach() != null) {
            block1.setValue("canAttach", PZArrayUtil.arrayToString(attach.getCanAttach(), "", "", ","));
        }

        if (attach.getZOffset() != 0.0F) {
            block1.setValue("zoffset", String.format(Locale.US, "%.4f", attach.getZOffset()));
        }

        if (!attach.isUpdateConstraint()) {
            block1.setValue("updateconstraint", "false");
        }
    }

    private void writeScript(String fileName, ArrayList<String> tokens) {
        String absolutePath = ZomboidFileSystem.instance.getString(fileName);
        File file = new File(absolutePath);

        try (
            FileWriter fw = new FileWriter(file);
            BufferedWriter br = new BufferedWriter(fw);
        ) {
            DebugLog.General.printf("writing %s\n", fileName);

            for (String token : tokens) {
                br.write(token);
            }

            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
        } catch (Throwable var13) {
            ExceptionLogger.logException(var13);
        }
    }

    private void updateModelScripts(VehicleScript vehicleScript) {
        for (int i = 0; i < vehicleScript.getPartCount(); i++) {
            VehicleScript.Part part = vehicleScript.getPart(i);

            for (int j = 0; j < part.getModelCount(); j++) {
                VehicleScript.Model scriptModel = part.getModel(j);
                if (scriptModel.getFile() != null) {
                    ModelScript modelScript = ScriptManager.instance.getModelScript(scriptModel.getFile());
                    if (modelScript != null && modelScript.getAttachmentCount() != 0) {
                        ArrayList<String> tokens = AttachmentEditorState.readScript(modelScript.getFileName());
                        if (tokens != null) {
                            String fileName = modelScript.getFileName();
                            if (AttachmentEditorState.updateScript(fileName, tokens, modelScript)) {
                                String absolutePath = ZomboidFileSystem.instance.getString(fileName);
                                this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("wroteScript"), this.table, absolutePath);
                            }
                        }
                    }
                }
            }
        }
    }

    public static final class LuaEnvironment {
        public J2SEPlatform platform;
        public KahluaTable env;
        public KahluaThread thread;
        public LuaCaller caller;

        public LuaEnvironment(J2SEPlatform platform, KahluaConverterManager converterManager, KahluaTable env) {
            this.platform = platform;
            this.env = env;
            this.thread = LuaManager.thread;
            this.caller = LuaManager.caller;
        }
    }
}
