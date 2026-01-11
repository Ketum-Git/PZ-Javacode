// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugLog;

public class PZGLUtil {
    private static final int SeverityVerbosity = 37191;
    static int test;

    public static void checkGLErrorThrow(String format, Object... args) throws OpenGLException {
        int glErrorCode = GL11.glGetError();
        if (glErrorCode != 0) {
            test++;
            throw new OpenGLException(createErrorMessage(glErrorCode, format, args));
        }
    }

    private static String createErrorMessage(int glErrorCode, String format, Object... args) {
        String nl = System.lineSeparator();
        return "  GL Error code ("
            + glErrorCode
            + ") encountered."
            + nl
            + "  Error translation: "
            + createErrorMessage(glErrorCode)
            + nl
            + "  While performing: "
            + String.format(format, args);
    }

    private static String createErrorMessage(int glErrorCode) {
        String error_string = Util.translateGLErrorString(glErrorCode);
        return error_string + " (" + glErrorCode + ")";
    }

    public static boolean checkGLError(boolean stackTrace) {
        try {
            Util.checkGLError();
            return true;
        } catch (OpenGLException var2) {
            RenderThread.logGLException(var2, stackTrace);
            return false;
        }
    }

    public static void InitGLDebugging() {
        GL43.glEnable(37600);
        GL11.glEnable(33346);
        GL43.glDebugMessageCallback(PZGLUtil::glDebugOutput, 0L);
        GL43.glDebugMessageControl(4352, 4352, 4352, (IntBuffer)null, true);
    }

    private static void glDebugOutput(int source, int type, int id, int severity, int length, long message, long userParam) {
        if (severity <= 37191 && (severity != 33387 || 37191 == severity)) {
            String sourceStr = switch (source) {
                case 33350 -> "API";
                case 33351 -> "Window System";
                case 33352 -> "Shader Compiler";
                case 33353 -> "Third Party";
                case 33354 -> "Application";
                case 33355 -> "Other";
                default -> "";
            };

            String typeStr = switch (type) {
                case 33356 -> "Error";
                case 33357 -> "Deprecated Behaviour";
                case 33358 -> "Undefined Behaviour";
                case 33359 -> "Portability";
                case 33360 -> "Performance";
                case 33361 -> "Other";
                default -> "";
                case 33384 -> "Marker";
                case 33385 -> "Push Group";
                case 33386 -> "Pop Group";
            };

            String severityStr = switch (severity) {
                case 33387 -> "Notification";
                case 37190 -> "High";
                case 37191 -> "Medium";
                case 37192 -> "Low";
                default -> "";
            };
            String messageStr = MemoryUtil.memASCII(message);

            Consumer<String> log = switch (severity) {
                case 37190 -> DebugLog.General::error;
                case 37191, 37192 -> DebugLog.General::warn;
                default -> DebugLog.General::print;
            };

            String prefix = switch (severity) {
                case 37190 -> "ERROR";
                case 37191, 37192 -> "WARN";
                default -> "INFO";
            };
            log.accept(prefix + " : OpenGL: Source: " + sourceStr + " Type: " + typeStr + " Severity: " + severityStr + " Message: " + messageStr);
        }
    }

    public static void printGLState(PrintStream out) {
        int res = GL11.glGetInteger(2979);
        out.println("DEBUG: GL_MODELVIEW_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(2980);
        out.println("DEBUG: GL_PROJECTION_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(2981);
        out.println("DEBUG: GL_TEXTURE_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(2992);
        out.println("DEBUG: GL_ATTRIB_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(2993);
        out.println("DEBUG: GL_CLIENT_ATTRIB_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3381);
        out.println("DEBUG: GL_MAX_ATTRIB_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3382);
        out.println("DEBUG: GL_MAX_MODELVIEW_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3383);
        out.println("DEBUG: GL_MAX_NAME_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3384);
        out.println("DEBUG: GL_MAX_PROJECTION_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3385);
        out.println("DEBUG: GL_MAX_TEXTURE_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3387);
        out.println("DEBUG: GL_MAX_CLIENT_ATTRIB_STACK_DEPTH= " + res);
        res = GL11.glGetInteger(3440);
        out.println("DEBUG: GL_NAME_STACK_DEPTH= " + res);
    }

    public static void loadMatrix(Matrix4f matrix) {
        matrix.get(Model.m_staticReusableFloatBuffer);
        Model.m_staticReusableFloatBuffer.position(16);
        Model.m_staticReusableFloatBuffer.flip();
        GL11.glLoadMatrixf(Model.m_staticReusableFloatBuffer);
    }

    public static void multMatrix(Matrix4f matrix) {
        matrix.get(Model.m_staticReusableFloatBuffer);
        Model.m_staticReusableFloatBuffer.position(16);
        Model.m_staticReusableFloatBuffer.flip();
        GL11.glMultMatrixf(Model.m_staticReusableFloatBuffer);
    }

    public static void loadMatrix(int mode, Matrix4f matrix) {
        GL11.glMatrixMode(mode);
        loadMatrix(matrix);
    }

    public static void multMatrix(int mode, Matrix4f matrix) {
        GL11.glMatrixMode(mode);
        multMatrix(matrix);
    }

    public static void pushAndLoadMatrix(int mode, Matrix4f matrix) {
        MatrixStack matrixStack = mode == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
        Matrix4f m = matrixStack.alloc().set(matrix);
        matrixStack.push(m);
    }

    public static void pushAndMultMatrix(int mode, Matrix4f matrix) {
        MatrixStack matrixStack = mode == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
        if (!matrixStack.isEmpty()) {
            Matrix4f m = matrixStack.alloc().set(matrixStack.peek()).mul(matrix);
            matrixStack.push(m);
        }
    }

    public static void popMatrix(int mode) {
        MatrixStack matrixStack = mode == 5888 ? Core.getInstance().modelViewMatrixStack : Core.getInstance().projectionMatrixStack;
        matrixStack.pop();
    }
}
