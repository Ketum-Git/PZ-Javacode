// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map.Entry;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.shader.Shader;

public class ShaderPropertyBlock {
    private final HashMap<String, ShaderParameter> parameters = new HashMap<>();
    private Shader shader;
    private ByteBuffer data;

    public void SetShader(Shader _shader) {
        if (_shader != this.shader) {
            for (Entry<String, ShaderParameter> kvp : this.parameters.entrySet()) {
                kvp.getValue().offset = -1;
            }

            if (_shader != null && _shader.isInstanced()) {
                if (this.shader != null) {
                    MemoryUtil.memRealloc(this.data, _shader.instancedData.GetBufferData().GetSize());
                } else {
                    this.data = MemoryUtil.memAlloc(_shader.instancedData.GetBufferData().GetSize());
                }

                ShaderBufferData bufferData = _shader.instancedData.GetBufferData();

                for (Entry<String, ShaderParameter> kvp : bufferData.parameters.entrySet()) {
                    this.parameters.put(kvp.getKey(), new ShaderParameter(kvp.getValue()));
                }
            } else if (this.data != null) {
                MemoryUtil.memFree(this.data);
                this.data = null;
            }

            this.shader = _shader;
        }
    }

    public void StoreProperties() {
        for (Entry<String, ShaderParameter> kvp : this.parameters.entrySet()) {
            kvp.getValue().WriteToBuffer(this.data, 0);
        }
    }

    public void CopyToInstanced(InstancedBuffer target) {
        target.data.put(this.data);
    }

    public ShaderParameter GetParameter(String name) {
        return this.parameters.getOrDefault(name, null);
    }

    public void CopyParameters(ShaderPropertyBlock from) {
        for (Entry<String, ShaderParameter> kvp : from.parameters.entrySet()) {
            ShaderParameter param = this.GetParameter(kvp.getKey());
            if (param == null) {
                param = new ShaderParameter(kvp.getKey(), false);
                this.parameters.put(param.name, param);
            }

            param.Copy(kvp.getValue(), true, false);
        }
    }

    public void SetInt(String name, int value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetInt(value);
        }
    }

    public void SetFloat(String name, float value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetFloat(value);
        }
    }

    public void SetVector2(String name, Vector2f value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector2(value);
        }
    }

    public void SetVector2(String name, float x, float y) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            param.SetVector2(x, y);
        } else {
            Vector2f vec = new Vector2f(x, y);
            param = new ShaderParameter(name, vec);
            this.parameters.put(name, param);
        }
    }

    public void SetVector3(String name, Vector3f value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector3(value);
        }
    }

    public void SetVector3(String name, float x, float y, float z) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            param.SetVector3(x, y, z);
        } else {
            Vector3f vec = new Vector3f(x, y, z);
            param = new ShaderParameter(name, vec);
            this.parameters.put(name, param);
        }
    }

    public void SetVector4(String name, Vector4f value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector4(value);
        }
    }

    public void SetVector4(String name, float x, float y, float z, float w) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            param.SetVector4(x, y, z, w);
        } else {
            Vector4f vec = new Vector4f(x, y, z, w);
            param = new ShaderParameter(name, vec);
            this.parameters.put(name, param);
        }
    }

    public void SetMatrix3(String name, Matrix3f value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetMatrix3(value);
        }
    }

    public Matrix4f SetMatrix4(String name, Matrix4f value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, new Matrix4f(value));
            this.parameters.put(name, param);
        } else {
            param.SetMatrix4(value);
        }

        return value;
    }

    public Matrix4f SetMatrix4(String name, org.joml.Matrix4f value) {
        ShaderParameter param1 = this.parameters.getOrDefault(name, null);
        if (param1 == null) {
            param1 = new ShaderParameter(name, PZMath.convertMatrix(value, new Matrix4f()));
            this.parameters.put(name, param1);
        } else if (param1.GetType() != ShaderParameter.ParameterTypes.Matrix4) {
            param1.SetMatrix4(PZMath.convertMatrix(value, new Matrix4f()));
        } else {
            PZMath.convertMatrix(value, param1.GetMatrix4());
        }

        return param1.GetMatrix4();
    }

    public Matrix4f SetMatrix4(String name, FloatBuffer value) {
        ShaderParameter param1 = this.parameters.getOrDefault(name, null);
        if (param1 == null) {
            Matrix4f matrix = new Matrix4f();
            matrix.load(value);
            param1 = new ShaderParameter(name, matrix);
            this.parameters.put(name, param1);
        } else {
            Matrix4f matrix;
            if (param1.GetType() != ShaderParameter.ParameterTypes.Matrix4) {
                matrix = new Matrix4f();
            } else {
                matrix = param1.GetMatrix4();
            }

            matrix.load(value);
            param1.SetMatrix4(matrix);
        }

        return param1.GetMatrix4();
    }

    public void SetFloatArray(String name, float[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetFloatArray(value);
        }
    }

    public void SetVector2Array(String name, Vector2f[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector2Array(value);
        }
    }

    public void SetVector3Array(String name, Vector3f[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector3Array(value);
        }
    }

    public void SetVector4Array(String name, Vector4f[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetVector4Array(value);
        }
    }

    public void SetMatrix3Array(String name, Matrix3f[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetMatrix3Array(value);
        }
    }

    public void SetMatrix4Array(String name, Matrix4f[] value) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            param = new ShaderParameter(name, value);
            this.parameters.put(name, param);
        } else {
            param.SetMatrix4Array(value);
        }
    }

    public void SetMatrix4Array(String name, FloatBuffer value) {
        int count = value.limit() / 16;
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param == null) {
            Matrix4f[] array = this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, count);

            for (int i = 0; i < count; i++) {
                array[i].load(value);
            }

            param = new ShaderParameter(name, array);
            this.parameters.put(name, param);
        } else {
            Matrix4f[] array;
            if (param.GetType() != ShaderParameter.ParameterTypes.Matrix4Array) {
                array = this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, count);
            } else {
                array = param.GetMatrix4Array();
                if (array.length != count) {
                    array = this.CreateAndFillArray(Matrix4f[].class, Matrix4f.class, count);
                }
            }

            for (int i = 0; i < count; i++) {
                array[i].load(value);
            }

            param.SetMatrix4Array(array);
        }

        value.flip();
    }

    private <T> void SetArrayElement(String name, int index, T value, Class<T[]> token) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            T[] array = (T[])token.cast(param.GetValue());
            array[index] = value;
        }
    }

    public void SetFloatArrayElement(String name, int index, float value) {
        this.SetArrayElement(name, index, value, Float[].class);
    }

    public void SetVector2ArrayElement(String name, int index, Vector2f value) {
        this.SetArrayElement(name, index, value, Vector2f[].class);
    }

    public void SetVector2ArrayElement(String name, int index, float x, float y) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            Vector2f[] array = Vector2f[].class.cast(param.GetValue());
            array[index].set(x, y);
        }
    }

    public void SetVector3ArrayElement(String name, int index, Vector3f value) {
        this.SetArrayElement(name, index, value, Vector3f[].class);
    }

    public void SetVector3ArrayElement(String name, int index, float x, float y, float z) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            Vector3f[] array = Vector3f[].class.cast(param.GetValue());
            array[index].set(x, y, z);
        }
    }

    public void SetVector4ArrayElement(String name, int index, Vector4f value) {
        this.SetArrayElement(name, index, value, Vector4f[].class);
    }

    public void SetVector4ArrayElement(String name, int index, float x, float y, float z, float w) {
        ShaderParameter param = this.parameters.getOrDefault(name, null);
        if (param != null) {
            Vector4f[] array = Vector4f[].class.cast(param.GetValue());
            array[index].set(x, y, z, w);
        }
    }

    public void SetMatrix3ArrayElement(String name, int index, Matrix3f value) {
        this.SetArrayElement(name, index, value, Matrix3f[].class);
    }

    public void SetMatrix4ArrayElement(String name, int index, Matrix4f value) {
        this.SetArrayElement(name, index, value, Matrix4f[].class);
    }

    private <T> T[] CreateAndFillArray(Class<T[]> arrayToken, Class<T> token, int count) {
        T[] array = (T[])arrayToken.cast(Array.newInstance(token, count));

        try {
            Constructor<?> constructor = token.getDeclaredConstructor();

            for (int i = 0; i < count; i++) {
                array[i] = token.cast(constructor.newInstance());
            }
        } catch (Exception var7) {
        }

        return array;
    }
}
