// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.shader;

import java.nio.FloatBuffer;
import org.joml.Math;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjglx.opengl.Display;
import zombie.SystemDisabler;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.DefaultShader;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.ShaderProgram;
import zombie.core.rendering.InstancedBuffer;
import zombie.core.rendering.ShaderBufferData;
import zombie.core.rendering.ShaderPropertyBlock;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.iso.IsoMovingObject;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.vehicles.BaseVehicle;

public final class Shader {
    private int hueChange;
    private int lightingAmount;
    private int mirrorXId;
    private int transformMatrixId;
    final String name;
    private final ShaderProgram shaderProgram;
    private int matrixId;
    private int light0Direction;
    private int light0Colour;
    private int light1Direction;
    private int light1Colour;
    private int light2Direction;
    private int light2Colour;
    private int light3Direction;
    private int light3Colour;
    private int light4Direction;
    private int light4Colour;
    private int tintColour;
    private int texture0;
    private int texturePainColor;
    private int textureRust;
    private int textureRustA;
    private int textureMask;
    private int textureLights;
    private int textureDamage1Overlay;
    private int textureDamage1Shell;
    private int textureDamage2Overlay;
    private int textureDamage2Shell;
    private int textureUninstall1;
    private int textureUninstall2;
    private int textureLightsEnables1;
    private int textureLightsEnables2;
    private int textureDamage1Enables1;
    private int textureDamage1Enables2;
    private int textureDamage2Enables1;
    private int textureDamage2Enables2;
    private int matBlood1Enables1;
    private int matBlood1Enables2;
    private int matBlood2Enables1;
    private int matBlood2Enables2;
    private int alpha;
    private int textureReflectionA;
    private int textureReflectionB;
    private int reflectionParam;
    public int boneIndicesAttrib;
    public int boneWeightsAttrib;
    private int uvScale;
    private int finalScale;
    final boolean isStatic;
    final boolean instanced;
    public int instancedDataAttrib = -1;
    public static final int INSTANCE_MAX = 128;
    public InstancedBuffer instancedData;
    private static FloatBuffer floatBuffer;
    private static final int MAX_BONES = 64;
    private static final ThreadLocal<Vector3f> tempVec3f = ThreadLocal.withInitial(Vector3f::new);
    private final FloatBuffer floatBuffer2 = BufferUtils.createFloatBuffer(16);

    public Shader(String name, boolean isStatic, boolean instanced) {
        this.name = name;
        this.shaderProgram = ShaderProgram.createShaderProgram(name, isStatic, instanced, false);
        this.shaderProgram.addCompileListener(this::onProgramCompiled);
        this.isStatic = isStatic;
        this.compile();
        this.instanced = this.instancedDataAttrib >= 0;
    }

    public ShaderBufferData GetBufferData() {
        return this.instancedData.GetBufferData();
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public boolean isInstanced() {
        return this.instanced;
    }

    public String getName() {
        return this.name;
    }

    public ShaderProgram getShaderProgram() {
        return this.shaderProgram;
    }

    private void onProgramCompiled(ShaderProgram shaderProgram) {
        this.Start();
        int shaderID = this.shaderProgram.getShaderID();
        if (!this.isStatic) {
            this.matrixId = GL20.glGetUniformLocation(shaderID, "MatrixPalette");
        } else {
            this.transformMatrixId = GL20.glGetUniformLocation(shaderID, "transform");
            if (this.transformMatrixId == -1) {
                this.transformMatrixId = GL20.glGetAttribLocation(shaderID, "transform");
            }
        }

        this.hueChange = GL20.glGetUniformLocation(shaderID, "HueChange");
        this.lightingAmount = GL20.glGetUniformLocation(shaderID, "LightingAmount");
        this.light0Colour = GL20.glGetUniformLocation(shaderID, "Light0Colour");
        this.light0Direction = GL20.glGetUniformLocation(shaderID, "Light0Direction");
        this.light1Colour = GL20.glGetUniformLocation(shaderID, "Light1Colour");
        this.light1Direction = GL20.glGetUniformLocation(shaderID, "Light1Direction");
        this.light2Colour = GL20.glGetUniformLocation(shaderID, "Light2Colour");
        this.light2Direction = GL20.glGetUniformLocation(shaderID, "Light2Direction");
        this.light3Colour = GL20.glGetUniformLocation(shaderID, "Light3Colour");
        this.light3Direction = GL20.glGetUniformLocation(shaderID, "Light3Direction");
        this.light4Colour = GL20.glGetUniformLocation(shaderID, "Light4Colour");
        this.light4Direction = GL20.glGetUniformLocation(shaderID, "Light4Direction");
        this.tintColour = GL20.glGetUniformLocation(shaderID, "TintColour");
        this.texture0 = GL20.glGetUniformLocation(shaderID, "Texture0");
        this.texturePainColor = GL20.glGetUniformLocation(shaderID, "TexturePainColor");
        this.textureRust = GL20.glGetUniformLocation(shaderID, "TextureRust");
        this.textureMask = GL20.glGetUniformLocation(shaderID, "TextureMask");
        this.textureLights = GL20.glGetUniformLocation(shaderID, "TextureLights");
        this.textureDamage1Overlay = GL20.glGetUniformLocation(shaderID, "TextureDamage1Overlay");
        this.textureDamage1Shell = GL20.glGetUniformLocation(shaderID, "TextureDamage1Shell");
        this.textureDamage2Overlay = GL20.glGetUniformLocation(shaderID, "TextureDamage2Overlay");
        this.textureDamage2Shell = GL20.glGetUniformLocation(shaderID, "TextureDamage2Shell");
        this.textureRustA = GL20.glGetUniformLocation(shaderID, "TextureRustA");
        this.textureUninstall1 = GL20.glGetUniformLocation(shaderID, "TextureUninstall1");
        this.textureUninstall2 = GL20.glGetUniformLocation(shaderID, "TextureUninstall2");
        this.textureLightsEnables1 = GL20.glGetUniformLocation(shaderID, "TextureLightsEnables1");
        this.textureLightsEnables2 = GL20.glGetUniformLocation(shaderID, "TextureLightsEnables2");
        this.textureDamage1Enables1 = GL20.glGetUniformLocation(shaderID, "TextureDamage1Enables1");
        this.textureDamage1Enables2 = GL20.glGetUniformLocation(shaderID, "TextureDamage1Enables2");
        this.textureDamage2Enables1 = GL20.glGetUniformLocation(shaderID, "TextureDamage2Enables1");
        this.textureDamage2Enables2 = GL20.glGetUniformLocation(shaderID, "TextureDamage2Enables2");
        this.matBlood1Enables1 = GL20.glGetUniformLocation(shaderID, "MatBlood1Enables1");
        this.matBlood1Enables2 = GL20.glGetUniformLocation(shaderID, "MatBlood1Enables2");
        this.matBlood2Enables1 = GL20.glGetUniformLocation(shaderID, "MatBlood2Enables1");
        this.matBlood2Enables2 = GL20.glGetUniformLocation(shaderID, "MatBlood2Enables2");
        this.alpha = GL20.glGetUniformLocation(shaderID, "Alpha");
        this.textureReflectionA = GL20.glGetUniformLocation(shaderID, "TextureReflectionA");
        this.textureReflectionB = GL20.glGetUniformLocation(shaderID, "TextureReflectionB");
        this.reflectionParam = GL20.glGetUniformLocation(shaderID, "ReflectionParam");
        this.uvScale = GL20.glGetUniformLocation(shaderID, "UVScale");
        this.finalScale = GL20.glGetUniformLocation(shaderID, "FinalScale");
        this.shaderProgram.setSamplerUnit("Texture", 0);
        if (this.texture0 != -1) {
            GL20.glUniform1i(this.texture0, 0);
        }

        if (this.textureRust != -1) {
            GL20.glUniform1i(this.textureRust, 1);
        }

        if (this.textureMask != -1) {
            GL20.glUniform1i(this.textureMask, 2);
        }

        if (this.textureLights != -1) {
            GL20.glUniform1i(this.textureLights, 3);
        }

        if (this.textureDamage1Overlay != -1) {
            GL20.glUniform1i(this.textureDamage1Overlay, 4);
        }

        if (this.textureDamage1Shell != -1) {
            GL20.glUniform1i(this.textureDamage1Shell, 5);
        }

        if (this.textureDamage2Overlay != -1) {
            GL20.glUniform1i(this.textureDamage2Overlay, 6);
        }

        if (this.textureDamage2Shell != -1) {
            GL20.glUniform1i(this.textureDamage2Shell, 7);
        }

        if (this.textureReflectionA != -1) {
            GL20.glUniform1i(this.textureReflectionA, 8);
        }

        if (this.textureReflectionB != -1) {
            GL20.glUniform1i(this.textureReflectionB, 9);
        }

        this.mirrorXId = GL20.glGetUniformLocation(shaderID, "MirrorX");
        this.boneIndicesAttrib = GL20.glGetAttribLocation(shaderID, "boneIndices");
        this.boneWeightsAttrib = GL20.glGetAttribLocation(shaderID, "boneWeights");
        if (Display.capabilities.OpenGL43) {
            this.instancedDataAttrib = GL43.glGetProgramResourceIndex(shaderID, 37606, "instancedData");
            this.instancedData = new InstancedBuffer(this, 128);
            this.instancedData.SetBinding(this.instancedDataAttrib);
        }

        this.End();
    }

    private void compile() {
        this.shaderProgram.compile();
    }

    public void setTexture(Texture tex, String unitName, int textureUnit) {
        this.shaderProgram.setValue(unitName, tex, textureUnit);
    }

    private void setUVScale(float su, float sv) {
        if (this.uvScale > 0) {
            this.shaderProgram.setVector2(this.uvScale, su, sv);
        }
    }

    public int getID() {
        return this.shaderProgram.getShaderID();
    }

    public void Start() {
        if (this.getID() != SceneShaderStore.defaultShaderId) {
            DefaultShader.isActive = false;
        }

        this.shaderProgram.Start();
    }

    public void End() {
        this.shaderProgram.End();
    }

    public void startCharacter(ModelSlotRenderData slotData, ModelInstanceRenderData instData) {
        if (this.isStatic) {
            this.setTransformMatrix(instData.xfrm, true);
        } else {
            this.setMatrixPalette(instData.matrixPalette);
        }

        float ambr = instData.ignoreLighting ? 1.0F : slotData.ambientR * 0.45F;
        float ambg = instData.ignoreLighting ? 1.0F : slotData.ambientG * 0.45F;
        float ambb = instData.ignoreLighting ? 1.0F : slotData.ambientB * 0.45F;
        this.setLights(slotData, 5);
        Texture tex = instData.tex != null ? instData.tex : instData.model.tex;
        if (DebugOptions.instance.isoSprite.characterMipmapColors.getValue()) {
            Texture tex2 = tex instanceof SmartTexture smartTexture ? smartTexture.result : tex;
            if (tex2 != null && tex2.getTextureId() != null && tex2.getTextureId().hasMipMaps()) {
                tex = Texture.getEngineMipmapTexture();
            }
        }

        this.setTexture(tex, "Texture", 0);
        if (instData.modelInstance.parent != null) {
            float targetDepth = instData.modelInstance.parent.targetDepth;
            if (!PerformanceSettings.fboRenderChunk) {
                targetDepth = 0.5F;
            }

            this.setTargetDepth(targetDepth);
        } else {
            float targetDepth = instData.modelInstance.targetDepth;
            if (!PerformanceSettings.fboRenderChunk) {
                targetDepth = 0.5F;
            }

            this.setTargetDepth(targetDepth);
        }

        this.setDepthBias(instData.depthBias / 50.0F);
        this.setAmbient(ambr, ambg, ambb);
        this.setLightingAmount(1.0F);
        this.setHueShift(instData.hue);
        this.setTint(instData.tintR, instData.tintG, instData.tintB);
        this.setAlpha(slotData.alpha);
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue() || slotData.debugChunkState) {
            this.setAmbient(1.0F, 1.0F, 1.0F);
        }
    }

    private void setLights(ModelSlotRenderData slotData, int lightCount) {
        for (int i = 0; i < lightCount; i++) {
            ModelInstance.EffectLight el = slotData.effectLights[i];
            if (GameServer.server && ServerGUI.isCreated()) {
                el.r = el.g = el.b = 1.0F;
            }

            this.setLight(i, el.x, el.y, el.z, el.r, el.g, el.b, el.radius, slotData.animPlayerAngle, slotData.x, slotData.y, slotData.z, slotData.object);
        }
    }

    public void updateAlpha(IsoGameCharacter chr, int playerIndex) {
        if (chr != null) {
            this.setAlpha(chr.getAlpha(playerIndex));
        }
    }

    public void setAlpha(float alpha) {
        GL20.glUniform1f(this.alpha, alpha);
    }

    public void setScale(float scale) {
        this.shaderProgram.setValue("FinalScale", scale);
    }

    public void updateParams() {
    }

    public void setMatrixPalette(Matrix4f[] skin) {
        if (!this.isStatic) {
            if (floatBuffer == null) {
                floatBuffer = BufferUtils.createFloatBuffer(1024);
            }

            floatBuffer.clear();

            for (Matrix4f aSkin : skin) {
                aSkin.store(floatBuffer);
            }

            floatBuffer.flip();
            GL20.glUniformMatrix4fv(this.matrixId, true, floatBuffer);
        }
    }

    public void setMatrixPalette(FloatBuffer matrixPalette) {
        this.setMatrixPalette(matrixPalette, true);
    }

    public void setMatrixPalette(FloatBuffer matrixPalette, boolean transpose) {
        if (!this.isStatic) {
            GL20.glUniformMatrix4fv(this.matrixId, transpose, matrixPalette);
        }
    }

    public void setMatrixPalette(org.joml.Matrix4f[] skin) {
        if (!this.isStatic) {
            if (floatBuffer == null) {
                floatBuffer = BufferUtils.createFloatBuffer(1024);
            }

            floatBuffer.clear();

            for (org.joml.Matrix4f aSkin : skin) {
                aSkin.get(floatBuffer);
                floatBuffer.position(floatBuffer.position() + 16);
            }

            floatBuffer.flip();
            GL20.glUniformMatrix4fv(this.matrixId, true, floatBuffer);
        }
    }

    public void setTint(float x, float y, float z) {
        GL20.glUniform3f(this.tintColour, x, y, z);
    }

    public void setTextureRustA(float a) {
        GL20.glUniform1f(this.textureRustA, a);
    }

    public void setTexturePainColor(float x, float y, float z, float a) {
        GL20.glUniform4f(this.texturePainColor, x, y, z, a);
    }

    public void setTexturePainColor(org.joml.Vector3f vec, float a) {
        GL20.glUniform4f(this.texturePainColor, vec.x(), vec.y(), vec.z(), a);
    }

    public void setTexturePainColor(Vector4f vec) {
        GL20.glUniform4f(this.texturePainColor, vec.x(), vec.y(), vec.z(), vec.w());
    }

    public void setReflectionParam(float timesOfDay, float refWindows, float refBody) {
        GL20.glUniform3f(this.reflectionParam, timesOfDay, refWindows, refBody);
    }

    public void setTextureUninstall1(float[] matrix4f) {
        this.setMatrix(this.textureUninstall1, matrix4f);
    }

    public void setTextureUninstall2(float[] matrix4f) {
        this.setMatrix(this.textureUninstall2, matrix4f);
    }

    public void setTextureLightsEnables1(float[] matrix4f) {
        this.setMatrix(this.textureLightsEnables1, matrix4f);
    }

    public void setTextureLightsEnables2(float[] matrix4f) {
        this.setMatrix(this.textureLightsEnables2, matrix4f);
    }

    public void setTextureDamage1Enables1(float[] matrix4f) {
        this.setMatrix(this.textureDamage1Enables1, matrix4f);
    }

    public void setTextureDamage1Enables2(float[] matrix4f) {
        this.setMatrix(this.textureDamage1Enables2, matrix4f);
    }

    public void setTextureDamage2Enables1(float[] matrix4f) {
        this.setMatrix(this.textureDamage2Enables1, matrix4f);
    }

    public void setTextureDamage2Enables2(float[] matrix4f) {
        this.setMatrix(this.textureDamage2Enables2, matrix4f);
    }

    public void setMatrixBlood1(float[] matrix1, float[] matrix2) {
        if (this.matBlood1Enables1 != -1 && this.matBlood1Enables2 != -1) {
            this.setMatrix(this.matBlood1Enables1, matrix1);
            this.setMatrix(this.matBlood1Enables2, matrix2);
        }
    }

    public void setMatrixBlood2(float[] matrix1, float[] matrix2) {
        if (this.matBlood2Enables1 != -1 && this.matBlood2Enables2 != -1) {
            this.setMatrix(this.matBlood2Enables1, matrix1);
            this.setMatrix(this.matBlood2Enables2, matrix2);
        }
    }

    public void setShaderAlpha(float a) {
        GL20.glUniform1f(this.alpha, a);
    }

    public void setLight(int index, float x, float y, float z, float r, float g, float b, float rad, float animPlayerAngle, ModelInstance inst) {
        float offsetX = 0.0F;
        float offsetY = 0.0F;
        float offsetZ = 0.0F;
        IsoMovingObject instObject = inst.object;
        if (instObject != null) {
            offsetX = instObject.getX();
            offsetY = instObject.getY();
            offsetZ = instObject.getZ();
        }

        this.setLight(index, x, y, z, r, g, b, rad, animPlayerAngle, offsetX, offsetY, offsetZ, instObject);
    }

    public void setLight(
        int index,
        float x,
        float y,
        float z,
        float r,
        float g,
        float b,
        float rad,
        float animPlayerAngle,
        float offsetX,
        float offsetY,
        float offsetZ,
        IsoMovingObject instObject
    ) {
        PZGLUtil.checkGLError(true);
        int Dir = this.light0Direction;
        int Col = this.light0Colour;
        if (index == 1) {
            Dir = this.light1Direction;
            Col = this.light1Colour;
        }

        if (index == 2) {
            Dir = this.light2Direction;
            Col = this.light2Colour;
        }

        if (index == 3) {
            Dir = this.light3Direction;
            Col = this.light3Colour;
        }

        if (index == 4) {
            Dir = this.light4Direction;
            Col = this.light4Colour;
        }

        if (r + g + b != 0.0F && !(rad <= 0.0F)) {
            Vector3f vec = tempVec3f.get();
            if (!Float.isNaN(animPlayerAngle)) {
                vec.set(x, y, z);
                vec.x -= offsetX;
                vec.y -= offsetY;
                vec.z -= offsetZ;
            } else {
                vec.set(x, y, z);
            }

            float dist = vec.length();
            if (dist < 1.0E-4F) {
                vec.set(0.0F, 0.0F, 1.0F);
            } else {
                vec.normalise();
            }

            if (!Float.isNaN(animPlayerAngle)) {
                float radians = -animPlayerAngle;
                float vx = vec.x;
                float vy = vec.y;
                vec.x = vx * Math.cos(radians) - vy * Math.sin(radians);
                vec.y = vx * Math.sin(radians) + vy * Math.cos(radians);
            }

            float temp = vec.y;
            vec.y = vec.z;
            vec.z = temp;
            if (vec.length() < 1.0E-4F) {
                vec.set(0.0F, 1.0F, 0.0F);
            }

            vec.normalise();
            float del = 1.0F - dist / rad;
            if (del < 0.0F) {
                del = 0.0F;
            }

            if (del > 1.0F) {
                del = 1.0F;
            }

            r *= del;
            g *= del;
            b *= del;
            r = PZMath.clamp(r, 0.0F, 1.0F);
            g = PZMath.clamp(g, 0.0F, 1.0F);
            b = PZMath.clamp(b, 0.0F, 1.0F);
            if (instObject instanceof BaseVehicle) {
                this.doVector3(Dir, -vec.x, vec.y, vec.z);
            } else {
                this.doVector3(Dir, -vec.x, vec.y, vec.z);
            }

            if (instObject instanceof IsoPlayer) {
                boolean var29 = false;
            }

            this.doVector3(Col, r, g, b);
            if (SystemDisabler.doEnableDetectOpenGLErrors) {
                PZGLUtil.checkGLErrorThrow("Shader.setLightInternal.");
            }
        } else {
            this.doVector3(Dir, 0.0F, 1.0F, 0.0F);
            this.doVector3(Col, 0.0F, 0.0F, 0.0F);
        }
    }

    public void setLightInst(
        int index,
        float x,
        float y,
        float z,
        float r,
        float g,
        float b,
        float rad,
        float animPlayerAngle,
        float offsetX,
        float offsetY,
        float offsetZ,
        ShaderPropertyBlock properties
    ) {
        if (properties.GetParameter("LightDirection") == null) {
            properties.SetVector3Array("LightDirection", new Vector3f[5]);
        }

        if (properties.GetParameter("LightColour") == null) {
            properties.SetVector3Array("LightColour", new Vector3f[5]);
        }

        if (r + g + b != 0.0F && !(rad <= 0.0F)) {
            Vector3f vec = tempVec3f.get();
            if (!Float.isNaN(animPlayerAngle)) {
                vec.set(x, y, z);
                vec.x -= offsetX;
                vec.y -= offsetY;
                vec.z -= offsetZ;
            } else {
                vec.set(x, y, z);
            }

            float dist = vec.length();
            if (dist < 1.0E-4F) {
                vec.set(0.0F, 0.0F, 1.0F);
            } else {
                vec.normalise();
            }

            if (!Float.isNaN(animPlayerAngle)) {
                float radians = -animPlayerAngle;
                float vx = vec.x;
                float vy = vec.y;
                vec.x = vx * Math.cos(radians) - vy * Math.sin(radians);
                vec.y = vx * Math.sin(radians) + vy * Math.cos(radians);
            }

            float temp = vec.y;
            vec.y = vec.z;
            vec.z = temp;
            if (vec.lengthSquared() < 1.0E-4F) {
                vec.set(0.0F, 1.0F, 0.0F);
            } else {
                vec.normalise();
            }

            float del = Math.clamp(0.0F, 1.0F, 1.0F - dist / rad);
            r *= del;
            g *= del;
            b *= del;
            r = PZMath.clamp(r, 0.0F, 1.0F);
            g = PZMath.clamp(g, 0.0F, 1.0F);
            b = PZMath.clamp(b, 0.0F, 1.0F);
            properties.SetVector3ArrayElement("LightDirection", index, -vec.x, vec.y, vec.z);
            properties.SetVector3ArrayElement("LightColour", index, r, g, b);
        } else {
            properties.SetVector3ArrayElement("LightDirection", index, 0.0F, 1.0F, 0.0F);
            properties.SetVector3ArrayElement("LightColour", index, 0.0F, 0.0F, 0.0F);
        }
    }

    private void doVector3(int id, float x, float y, float z) {
        this.shaderProgram.setVector3(id, x, y, z);
    }

    public void setHueShift(float hue) {
        if (this.hueChange > 0) {
            this.shaderProgram.setValue("HueChange", hue);
        }
    }

    public void setLightingAmount(float lighting) {
        if (this.lightingAmount > 0) {
            this.shaderProgram.setValue("LightingAmount", lighting);
        }
    }

    public void setTargetDepth(float targetDepth) {
        this.shaderProgram.setValue("targetDepth", targetDepth);
    }

    public void setDepthBias(float bias) {
        this.shaderProgram.setValue("DepthBias", bias / 300.0F);
    }

    public void setAmbient(float amb) {
        this.shaderProgram.setVector3("AmbientColour", amb, amb, amb);
    }

    public void setAmbient(float ambr, float ambg, float ambb) {
        this.shaderProgram.setVector3("AmbientColour", ambr, ambg, ambb);
    }

    public void setTransformMatrix(Matrix4f matrix4f, boolean transpose) {
        if (floatBuffer == null) {
            floatBuffer = BufferUtils.createFloatBuffer(1024);
        }

        floatBuffer.clear();
        matrix4f.store(floatBuffer);
        floatBuffer.flip();
        GL20.glUniformMatrix4fv(this.transformMatrixId, transpose, floatBuffer);
    }

    public void StoreMatrix(org.joml.Matrix4f matrix4f) {
        this.floatBuffer2.clear();
        matrix4f.get(this.floatBuffer2);
        this.floatBuffer2.position(16);
        this.floatBuffer2.flip();
    }

    public void setTransformMatrix(org.joml.Matrix4f matrix4f, boolean transpose) {
        if (this.transformMatrixId != -1) {
            this.floatBuffer2.clear();
            matrix4f.get(this.floatBuffer2);
            this.floatBuffer2.position(16);
            this.floatBuffer2.flip();
            GL20.glUniformMatrix4fv(this.transformMatrixId, transpose, this.floatBuffer2);
        }
    }

    public void setMatrix(int location, org.joml.Matrix4f matrix4f) {
        this.floatBuffer2.clear();
        matrix4f.get(this.floatBuffer2);
        this.floatBuffer2.position(16);
        this.floatBuffer2.flip();
        GL20.glUniformMatrix4fv(location, true, this.floatBuffer2);
    }

    public void setMatrix(int location, float[] matrix4f) {
        this.floatBuffer2.clear();
        this.floatBuffer2.put(matrix4f);
        this.floatBuffer2.flip();
        GL20.glUniformMatrix4fv(location, true, this.floatBuffer2);
    }

    public boolean isVehicleShader() {
        return this.textureRust != -1;
    }

    public void setHighResDepthMultiplier(float m) {
        this.getShaderProgram().setValue("HighResDepthMultiplier", m);
    }
}
