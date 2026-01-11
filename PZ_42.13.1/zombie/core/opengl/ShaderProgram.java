// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.lwjglx.BufferUtils;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.SystemDisabler;
import zombie.ZomboidFileSystem;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.util.list.PZArrayUtil;

public final class ShaderProgram {
    private int shaderId;
    private final String name;
    private final boolean isStatic;
    private final boolean isInstanced;
    private final ArrayList<ShaderUnit> vertexUnits = new ArrayList<>();
    private final ArrayList<ShaderUnit> fragmentUnits = new ArrayList<>();
    private final HashMap<String, PredicatedFileWatcher> fileWatchers = new HashMap<>();
    private boolean sourceFilesChanged;
    private boolean compileFailed;
    private final HashMap<String, ShaderProgram.Uniform> uniformsByName = new HashMap<>();
    private final ArrayList<IShaderProgramListener> onCompiledListeners = new ArrayList<>();
    private final int[] uvScaleUniforms = new int[10];
    public final Matrix4f modelView = new Matrix4f();
    public final Matrix4f projection = new Matrix4f();
    private static final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    private ShaderProgram(String name, boolean isStatic, boolean isInstanced) {
        this.name = name;
        this.isStatic = isStatic;
        this.isInstanced = isInstanced;
    }

    public String getName() {
        return this.name;
    }

    public void addCompileListener(IShaderProgramListener listener) {
        if (!this.onCompiledListeners.contains(listener)) {
            this.onCompiledListeners.add(listener);
        }
    }

    public void removeCompileListener(IShaderProgramListener listener) {
        this.onCompiledListeners.remove(listener);
    }

    private void invokeProgramCompiledEvent() {
        this.Start();
        this.uvScaleUniforms[0] = GL20.glGetUniformLocation(this.shaderId, "UVScale");

        for (int i = 1; i < this.uvScaleUniforms.length; i++) {
            this.uvScaleUniforms[i] = GL20.glGetUniformLocation(this.shaderId, "UVScale" + i);
        }

        this.End();
        if (!this.onCompiledListeners.isEmpty()) {
            for (IShaderProgramListener listener : new ArrayList<>(this.onCompiledListeners)) {
                listener.callback(this);
            }
        }
    }

    /**
     * Compiles or re-compiles this program.
     */
    public void compile() {
        this.sourceFilesChanged = false;
        this.compileFailed = false;
        if (this.isCompiled()) {
            this.destroy();
        }

        this.modelView.identity();
        this.projection.identity();
        String name = this.getName();
        if (DebugLog.isEnabled(DebugType.Shader)) {
            DebugLog.Shader.debugln(name + (this.isStatic ? "(Static)" : "") + (this.isInstanced ? "(Instanced)" : ""));
        }

        this.shaderId = GL20.glCreateProgram();
        if (this.shaderId == 0) {
            DebugLog.Shader.error("Failed to create Shader: " + name + " could not create new Shader Program ID.");
        } else {
            this.addShader(this.getRootVertFileName(), ShaderUnit.Type.Vert);
            this.addShader(this.getRootFragFileName(name), ShaderUnit.Type.Frag);
            this.registerFileWatchers();
            if (!this.compileAllShaderUnits()) {
                this.compileFailed = true;
                this.destroy();
            } else if (!this.attachAllShaderUnits()) {
                this.compileFailed = true;
                this.destroy();
            } else {
                this.registerFileWatchers();
                GL20.glLinkProgram(this.shaderId);
                if (GL20.glGetProgrami(this.shaderId, 35714) == 0) {
                    this.compileFailed = true;
                    DebugLog.Shader.error("Failed to link new Shader Program:" + name + " bStatic:" + this.isStatic + " bInstanced:" + this.isInstanced);
                    DebugLog.Shader.error(getLogInfo(this.shaderId));
                    this.destroy();
                } else {
                    GL20.glValidateProgram(this.shaderId);
                    if (GL20.glGetProgrami(this.shaderId, 35715) == 0) {
                        this.compileFailed = true;
                        DebugLog.Shader.error("Failed to validate Shader Program:" + name + " bStatic:" + this.isStatic + " bInstanced:" + this.isInstanced);
                        DebugLog.Shader.error(getLogInfo(this.shaderId));
                        this.destroy();
                    } else {
                        ShaderPrograms.getInstance().registerProgram(this);
                        this.onCompileSuccess();
                    }
                }
            }
        }
    }

    private void onCompileSuccess() {
        if (this.isCompiled()) {
            this.uniformsByName.clear();
            this.Start();
            int shaderID = this.shaderId;
            int c = GL20.glGetProgrami(shaderID, 35718);
            int shaderSamplers = 0;
            IntBuffer sizeBuffer = MemoryUtil.memAllocInt(1);
            IntBuffer typeBuffer = MemoryUtil.memAllocInt(1);

            for (int i = 0; i < c; i++) {
                String str = GL20.glGetActiveUniform(shaderID, i, 255, sizeBuffer, typeBuffer);
                int loc = GL20.glGetUniformLocation(shaderID, str);
                if (loc != -1) {
                    int size = sizeBuffer.get(0);
                    int type = typeBuffer.get(0);
                    ShaderProgram.Uniform u = new ShaderProgram.Uniform();
                    this.uniformsByName.put(str, u);
                    u.name = str;
                    u.loc = loc;
                    u.size = size;
                    u.type = type;
                    if (DebugLog.isEnabled(DebugType.Shader)) {
                        DebugLog.Shader.debugln(str + ", Loc: " + loc + ", Type: " + type + ", Size: " + size);
                    }

                    if (u.type == 35678) {
                        if (shaderSamplers != 0) {
                            GL20.glUniform1i(u.loc, shaderSamplers);
                        }

                        u.sampler = shaderSamplers++;
                    }
                }
            }

            MemoryUtil.memFree(sizeBuffer);
            MemoryUtil.memFree(typeBuffer);
            this.End();
            PZGLUtil.checkGLError(true);
            this.invokeProgramCompiledEvent();
        }
    }

    private void registerFileWatchers() {
        for (PredicatedFileWatcher w : this.fileWatchers.values()) {
            DebugFileWatcher.instance.remove(w);
        }

        this.fileWatchers.clear();

        for (ShaderUnit shaderUnit : this.vertexUnits) {
            this.registerFileWatcherInternal(shaderUnit.getFileName(), keyEntry -> this.onShaderFileChanged());
        }

        for (ShaderUnit shaderUnit : this.fragmentUnits) {
            this.registerFileWatcherInternal(shaderUnit.getFileName(), keyEntry -> this.onShaderFileChanged());
        }
    }

    private void registerFileWatcherInternal(String fileName, PredicatedFileWatcher.IPredicatedFileWatcherCallback callback) {
        fileName = ZomboidFileSystem.instance.getString(fileName);
        PredicatedFileWatcher watcher = new PredicatedFileWatcher(fileName, callback);
        this.fileWatchers.put(fileName, watcher);
        DebugFileWatcher.instance.add(watcher);
    }

    private void onShaderFileChanged() {
        this.sourceFilesChanged = true;
    }

    boolean isFirstUnit(ShaderUnit unit) {
        ShaderUnit first = unit.getType() == ShaderUnit.Type.Vert ? this.vertexUnits.get(0) : this.fragmentUnits.get(0);
        return unit == first;
    }

    void addCombinedVertexSource(String code) {
        ShaderUnit var10000 = this.vertexUnits.get(0);
        var10000.processedCode = var10000.processedCode + System.lineSeparator() + code;
    }

    void addCombinedFragmentSource(String code) {
        ShaderUnit var10000 = this.fragmentUnits.get(0);
        var10000.processedCode = var10000.processedCode + System.lineSeparator() + code;
    }

    private boolean compileAllShaderUnits() {
        if (ShaderUnit.combineShaderSources) {
            ShaderUnit first = this.vertexUnits.get(0);
            if (first != null && !first.isCompiled() && !first.compile()) {
                DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to compile: " + first.getFileName());
                return false;
            } else {
                first = this.fragmentUnits.get(0);
                if (first != null && !first.isCompiled() && !first.compile()) {
                    DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to compile: " + first.getFileName());
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            for (ShaderUnit unit : this.getShaderUnits()) {
                if (!unit.isCompiled() && !unit.compile()) {
                    DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to compile: " + unit.getFileName());
                    return false;
                }
            }

            return true;
        }
    }

    private boolean attachAllShaderUnits() {
        if (ShaderUnit.combineShaderSources) {
            ShaderUnit first = this.vertexUnits.get(0);
            if (first != null && !first.isAttached() && !first.attach()) {
                DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to compile: " + first.getFileName());
                return false;
            } else {
                first = this.fragmentUnits.get(0);
                if (first != null && !first.isAttached() && !first.attach()) {
                    DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to compile: " + first.getFileName());
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            for (ShaderUnit unit : this.getShaderUnits()) {
                if (!unit.attach()) {
                    DebugLog.Shader.error("Failed to create Shader: " + this.getName() + " Shader unit failed to attach: " + unit.getFileName());
                    return false;
                }
            }

            return true;
        }
    }

    private ArrayList<ShaderUnit> getShaderUnits() {
        ArrayList<ShaderUnit> shaderUnits = new ArrayList<>();
        PZArrayUtil.addAll(shaderUnits, this.vertexUnits);
        PZArrayUtil.addAll(shaderUnits, this.fragmentUnits);
        return shaderUnits;
    }

    private String getRootVertFileName() {
        String stat = this.isStatic ? "_static" : "";
        String inst = this.isInstanced ? "_instanced" : "";
        return "media/shaders/" + this.getName() + stat + inst + ".vert";
    }

    private String getRootFragFileName(String name) {
        String inst = this.isInstanced ? "_instanced" : "";
        return "media/shaders/" + name + inst + ".frag";
    }

    public ShaderUnit addShader(String fileName, ShaderUnit.Type unitType) {
        ShaderUnit shader = this.findShader(fileName, unitType);
        if (shader != null) {
            return shader;
        } else {
            ArrayList<ShaderUnit> shaderList = this.getShaderList(unitType);
            shader = new ShaderUnit(this, fileName, unitType);
            shaderList.add(shader);
            return shader;
        }
    }

    private ArrayList<ShaderUnit> getShaderList(ShaderUnit.Type unitType) {
        return unitType == ShaderUnit.Type.Vert ? this.vertexUnits : this.fragmentUnits;
    }

    private ShaderUnit findShader(String fileName, ShaderUnit.Type unitType) {
        ArrayList<ShaderUnit> shaderList = this.getShaderList(unitType);
        ShaderUnit foundUnit = null;

        for (ShaderUnit unit : shaderList) {
            if (unit.getFileName().equals(fileName)) {
                foundUnit = unit;
                break;
            }
        }

        return foundUnit;
    }

    public static ShaderProgram createShaderProgram(String name, boolean isStatic, boolean isInstanced, boolean compile) {
        ShaderProgram newProgram = new ShaderProgram(name, isStatic, isInstanced);
        if (compile) {
            newProgram.compile();
        }

        return newProgram;
    }

    /**
     * Creates a vertex shader unit.
     *  Deprecated: Use ShaderProgram.createShaderProgram instead.
     */
    @Deprecated
    public static int createVertShader(String fileName) {
        ShaderUnit shader = new ShaderUnit(null, fileName, ShaderUnit.Type.Vert);
        shader.compile();
        return shader.getGLID();
    }

    /**
     * Creates a fragment shader unit.
     *  Deprecated: Use ShaderProgram.createShaderProgram instead.
     */
    @Deprecated
    public static int createFragShader(String fileName) {
        ShaderUnit shader = new ShaderUnit(null, fileName, ShaderUnit.Type.Frag);
        shader.compile();
        return shader.getGLID();
    }

    public static void printLogInfo(int obj) {
        int length = GL20.glGetShaderi(obj, 35716);
        if (length > 1) {
            String out = GL20.glGetShaderInfoLog(obj, length);
            DebugLog.Shader.debugln(":\n" + out);
        }
    }

    public static String getLogInfo(int obj) {
        return GL20.glGetProgramInfoLog(obj, GL20.glGetProgrami(obj, 35716));
    }

    public boolean isCompiled() {
        return this.shaderId != 0;
    }

    public void destroy() {
        if (this.shaderId == 0) {
            this.vertexUnits.clear();
            this.fragmentUnits.clear();
        } else {
            try {
                DebugLog.Shader.debugln(this.getName());

                for (ShaderUnit unit : this.vertexUnits) {
                    unit.destroy();
                }

                this.vertexUnits.clear();

                for (ShaderUnit unit : this.fragmentUnits) {
                    unit.destroy();
                }

                this.fragmentUnits.clear();
                GL20.glDeleteProgram(this.shaderId);
                PZGLUtil.checkGLError(true);
            } finally {
                ShaderPrograms.getInstance().unregisterProgram(this);
                this.vertexUnits.clear();
                this.fragmentUnits.clear();
                this.shaderId = 0;
            }
        }
    }

    public int getShaderID() {
        if (!this.compileFailed && !this.isCompiled() || this.sourceFilesChanged) {
            RenderThread.invokeOnRenderContext(this::compile);
        }

        return this.shaderId;
    }

    public void Start() {
        ShaderHelper.glUseProgramObjectARB(this.getShaderID());
    }

    public void End() {
        ShaderHelper.glUseProgramObjectARB(SceneShaderStore.defaultShaderId);
    }

    public void setSamplerUnit(String loc, int textureUnit) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35678);
        if (u != null) {
            u.sampler = textureUnit;
            GL20.glUniform1i(u.loc, textureUnit);
        }
    }

    public void setValueColor(String loc, int rgba) {
        float byteToFloat = 0.003921569F;
        this.setVector4(
            loc, 0.003921569F * (rgba >> 24 & 0xFF), 0.003921569F * (rgba >> 16 & 0xFF), 0.003921569F * (rgba >> 8 & 0xFF), 0.003921569F * (rgba & 0xFF)
        );
    }

    public void setValueColorRGB(String loc, int rgb) {
        this.setValueColor(loc, rgb & 0xFF);
    }

    public void setValue(String loc, float val) {
        ShaderProgram.Uniform u = this.getUniform(loc, 5126);
        if (u != null) {
            GL20.glUniform1f(u.loc, val);
        }
    }

    public void setValue(String loc, int val) {
        ShaderProgram.Uniform u = this.getUniform(loc, 5124);
        if (u != null) {
            GL20.glUniform1i(u.loc, val);
        }
    }

    public void setValue(String loc, Vector3 val) {
        this.setVector3(loc, val.x, val.y, val.z);
    }

    public void setValue(String loc, Vector2 val) {
        this.setVector2(loc, val.x, val.y);
    }

    public void setVector2(String loc, float val_x, float val_y) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35664);
        if (u != null) {
            this.setVector2(u.loc, val_x, val_y);
        }
    }

    public void setVector3(String loc, float val_x, float val_y, float val_z) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35665);
        if (u != null) {
            this.setVector3(u.loc, val_x, val_y, val_z);
        }
    }

    public void setVector4(String loc, float val_x, float val_y, float val_z, float val_w) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35666);
        if (u != null) {
            this.setVector4(u.loc, val_x, val_y, val_z, val_w);
        }
    }

    public final ShaderProgram.Uniform getUniform(String loc, int type) {
        return this.getUniform(loc, type, false);
    }

    public ShaderProgram.Uniform getUniform(String loc, int type, boolean bWarn) {
        ShaderProgram.Uniform u = this.uniformsByName.get(loc);
        if (u == null) {
            if (bWarn) {
                DebugLog.Shader.warn(loc + " doesn't exist in shader");
            }

            return null;
        } else if (u.type != type) {
            DebugLog.Shader.warn(loc + " isn't of type: " + type + ", it is of type: " + u.type);
            return null;
        } else {
            return u;
        }
    }

    public void setValue(String loc, org.lwjgl.util.vector.Matrix4f matrix4f) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35676);
        if (u != null) {
            this.setTransformMatrix(u.loc, matrix4f);
        }
    }

    public void setValue(String loc, Matrix4f matrix4f) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35676);
        if (u != null) {
            this.setTransformMatrix(u.loc, matrix4f);
        }
    }

    public void setValue(String loc, Texture tex, int samplerUnit) {
        ShaderProgram.Uniform u = this.getUniform(loc, 35678);
        if (u != null && tex != null) {
            if (u.sampler != samplerUnit) {
                u.sampler = samplerUnit;
                GL20.glUniform1i(u.loc, u.sampler);
            }

            GL13.glActiveTexture(33984 + u.sampler);
            if (u.sampler < 8) {
                GL11.glEnable(3553);
            }

            int lastTextureID = Texture.lastTextureID;
            tex.bind();
            if (u.sampler > 0) {
                Texture.lastTextureID = lastTextureID;
            }

            if (DebugOptions.instance.checks.boundTextures.getValue()) {
                SpriteRenderer.ringBuffer.debugBoundTexture(tex, 33984 + u.sampler);
            }

            Vector2 uvScale = tex.getUVScale(ShaderProgram.L_setValue.vector2);
            this.setUVScale(samplerUnit, uvScale.x, uvScale.y);
            if (SystemDisabler.doEnableDetectOpenGLErrorsInTexture) {
                PZGLUtil.checkGLErrorThrow("Shader.setValue<Texture> Loc: %s, Tex: %s, samplerUnit: %d", loc, tex, samplerUnit);
            }
        }
    }

    private void setUVScale(int samplerUnit, float x, float y) {
        if (samplerUnit < 0) {
            DebugLog.Shader.error("SamplerUnit out of range: " + samplerUnit);
        } else if (samplerUnit >= this.uvScaleUniforms.length) {
            String unitName = "UVScale";
            if (samplerUnit > 0) {
                unitName = "UVScale" + samplerUnit;
            }

            this.setVector2(unitName, x, y);
        } else {
            int uniformID = this.uvScaleUniforms[samplerUnit];
            if (uniformID >= 0) {
                this.setVector2(uniformID, x, y);
            }
        }
    }

    public void setVector2(int id, float x, float y) {
        GL20.glUniform2f(id, x, y);
    }

    public void setVector3(int id, float x, float y, float z) {
        GL20.glUniform3f(id, x, y, z);
    }

    public void setVector4(int id, float x, float y, float z, float w) {
        GL20.glUniform4f(id, x, y, z, w);
    }

    void setTransformMatrix(int loc, org.lwjgl.util.vector.Matrix4f matrix4f) {
        floatBuffer.clear();
        matrix4f.store(floatBuffer);
        floatBuffer.flip();
        GL20.glUniformMatrix4fv(loc, true, floatBuffer);
    }

    void setTransformMatrix(int loc, Matrix4f matrix4f) {
        floatBuffer.clear();
        matrix4f.get(floatBuffer);
        floatBuffer.limit(16);
        GL20.glUniformMatrix4fv(loc, false, floatBuffer);
    }

    private static final class L_setValue {
        static final Vector2 vector2 = new Vector2();
    }

    public static class Uniform {
        public String name;
        public int size;
        public int loc;
        public int type;
        public int sampler;
    }
}
