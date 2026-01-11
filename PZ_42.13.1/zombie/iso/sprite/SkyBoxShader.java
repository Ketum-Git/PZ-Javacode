// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

final class SkyBoxShader extends Shader {
    private int skyBoxTime;
    private int skyBoxParamCloudCount;
    private int skyBoxParamCloudSize;
    private int skyBoxParamSunLight;
    private int skyBoxParamSunColor;
    private int skyBoxParamSkyHColour;
    private int skyBoxParamSkyLColour;
    private int skyBoxParamCloudLight;
    private int skyBoxParamStars;
    private int skyBoxParamFog;
    private int skyBoxParamWind;

    public SkyBoxShader(String name) {
        super(name);
    }

    @Override
    public void startRenderThread(TextureDraw texd) {
        SkyBox sb = SkyBox.getInstance();
        GL20.glUniform1f(this.skyBoxTime, sb.getShaderTime());
        GL20.glUniform1f(this.skyBoxParamCloudCount, sb.getShaderCloudCount());
        GL20.glUniform1f(this.skyBoxParamCloudSize, sb.getShaderCloudSize());
        GL20.glUniform3f(this.skyBoxParamSunLight, sb.getShaderSunLight().x, sb.getShaderSunLight().y, sb.getShaderSunLight().z);
        GL20.glUniform3f(this.skyBoxParamSunColor, sb.getShaderSunColor().r, sb.getShaderSunColor().g, sb.getShaderSunColor().b);
        GL20.glUniform3f(this.skyBoxParamSkyHColour, sb.getShaderSkyHColour().r, sb.getShaderSkyHColour().g, sb.getShaderSkyHColour().b);
        GL20.glUniform3f(this.skyBoxParamSkyLColour, sb.getShaderSkyLColour().r, sb.getShaderSkyLColour().g, sb.getShaderSkyLColour().b);
        GL20.glUniform1f(this.skyBoxParamCloudLight, sb.getShaderCloudLight());
        GL20.glUniform1f(this.skyBoxParamStars, sb.getShaderStars());
        GL20.glUniform1f(this.skyBoxParamFog, sb.getShaderFog());
        GL20.glUniform3f(this.skyBoxParamWind, sb.getShaderWind().x, sb.getShaderWind().y, sb.getShaderWind().z);
    }

    @Override
    public void onCompileSuccess(ShaderProgram shaderProgram) {
        int shaderID = this.getID();
        this.skyBoxTime = GL20.glGetUniformLocation(shaderID, "SBTime");
        this.skyBoxParamCloudCount = GL20.glGetUniformLocation(shaderID, "SBParamCloudCount");
        this.skyBoxParamCloudSize = GL20.glGetUniformLocation(shaderID, "SBParamCloudSize");
        this.skyBoxParamSunLight = GL20.glGetUniformLocation(shaderID, "SBParamSunLight");
        this.skyBoxParamSunColor = GL20.glGetUniformLocation(shaderID, "SBParamSunColour");
        this.skyBoxParamSkyHColour = GL20.glGetUniformLocation(shaderID, "SBParamSkyHColour");
        this.skyBoxParamSkyLColour = GL20.glGetUniformLocation(shaderID, "SBParamSkyLColour");
        this.skyBoxParamCloudLight = GL20.glGetUniformLocation(shaderID, "SBParamCloudLight");
        this.skyBoxParamStars = GL20.glGetUniformLocation(shaderID, "SBParamStars");
        this.skyBoxParamFog = GL20.glGetUniformLocation(shaderID, "SBParamFog");
        this.skyBoxParamWind = GL20.glGetUniformLocation(shaderID, "SBParamWind");
    }
}
