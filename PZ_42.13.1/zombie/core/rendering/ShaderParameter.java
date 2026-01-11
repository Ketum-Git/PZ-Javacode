// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.function.Function;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.debug.DebugLog;

public class ShaderParameter {
    private static final FloatBuffer VectorBuffer = MemoryUtil.memAllocFloat(4);
    private static final FloatBuffer MatrixBuffer = MemoryUtil.memAllocFloat(16);
    public final String name;
    private Object value;
    private Object defaultValue;
    private ShaderParameter.ParameterTypes type;
    public int offset;
    public int length;

    public ShaderParameter(ShaderParameter other) {
        this.name = other.name;
        this.defaultValue = other.defaultValue;
        this.type = other.type;
        this.offset = other.offset;
        this.length = other.length;
        switch (other.type) {
            case Vector2:
                this.value = new Vector2f(other.GetVector2());
                break;
            case Vector3:
                this.value = new Vector3f(other.GetVector3());
                break;
            case Vector4:
                this.value = new Vector4f(other.GetVector4());
                break;
            case Matrix3:
            case Texture:
            case IntArray:
            case FloatArray:
            case Matrix3Array:
            default:
                this.value = other.value;
                break;
            case Matrix4:
                this.value = new Matrix4f(other.GetMatrix4());
                break;
            case Vector2Array:
                this.value = this.CopyArray(Vector2f.class, other.GetVector2Array(), Vector2f::new);
                break;
            case Vector3Array:
                this.value = this.CopyArray(Vector3f.class, other.GetVector3Array(), Vector3f::new);
                break;
            case Vector4Array:
                this.value = this.CopyArray(Vector4f.class, other.GetVector4Array(), Vector4f::new);
                break;
            case Matrix4Array:
                this.value = this.CopyArray(Matrix4f.class, other.GetMatrix4Array(), Matrix4f::new);
        }
    }

    private ShaderParameter(String _name, Object _value, ShaderParameter.ParameterTypes _type) {
        if (_name.startsWith("instancedStructs[0].")) {
            _name = _name.substring("instancedStructs[0].".length());
        }

        if (_name.endsWith("[0]")) {
            _name = _name.substring(0, _name.length() - "[0]".length());
        }

        this.name = _name;
        this.value = _value;
        this.defaultValue = _value;
        this.type = _type;
        this.offset = -1;
        this.length = 1;
    }

    public ShaderParameter(String _name, boolean _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Bool);
    }

    public ShaderParameter(String _name, int _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Int);
    }

    public ShaderParameter(String _name, int _value, boolean isTexture) {
        this(_name, _value, isTexture ? ShaderParameter.ParameterTypes.Texture : ShaderParameter.ParameterTypes.Int);
    }

    public ShaderParameter(String _name, float _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Float);
    }

    public ShaderParameter(String _name, Vector2f _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector2);
    }

    public ShaderParameter(String _name, Vector3f _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector3);
    }

    public ShaderParameter(String _name, Vector4f _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector4);
    }

    public ShaderParameter(String _name, Matrix3f _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Matrix3);
    }

    public ShaderParameter(String _name, Matrix4f _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Matrix4);
    }

    public ShaderParameter(String _name, int[] _value, boolean isTexture) {
        this(_name, _value, isTexture ? ShaderParameter.ParameterTypes.TextureArray : ShaderParameter.ParameterTypes.IntArray);
    }

    public ShaderParameter(String _name, float[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.FloatArray);
    }

    public ShaderParameter(String _name, Vector2f[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector2Array);

        for (int i = 0; i < _value.length; i++) {
            if (_value[i] == null) {
                _value[i] = new Vector2f();
            }
        }
    }

    public ShaderParameter(String _name, Vector3f[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector3Array);

        for (int i = 0; i < _value.length; i++) {
            if (_value[i] == null) {
                _value[i] = new Vector3f();
            }
        }
    }

    public ShaderParameter(String _name, Vector4f[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Vector4Array);

        for (int i = 0; i < _value.length; i++) {
            if (_value[i] == null) {
                _value[i] = new Vector4f();
            }
        }
    }

    public ShaderParameter(String _name, Matrix3f[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Matrix3Array);

        for (int i = 0; i < _value.length; i++) {
            if (_value[i] == null) {
                _value[i] = new Matrix3f();
            }
        }
    }

    public ShaderParameter(String _name, Matrix4f[] _value) {
        this(_name, _value, ShaderParameter.ParameterTypes.Matrix4Array);

        for (int i = 0; i < _value.length; i++) {
            if (_value[i] == null) {
                _value[i] = new Matrix4f();
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s { %s: %3s }", this.getClass().getSimpleName(), this.name, this.value);
    }

    public ShaderParameter.ParameterTypes GetType() {
        return this.type;
    }

    private <T> T[] CopyArray(Class<T> token, T[] from, Function<T, T> create) {
        T[] to = (T[])Arrays.copyOf(from, from.length);

        for (int i = 0; i < to.length; i++) {
            to[i] = create.apply(from[i]);
        }

        return to;
    }

    public void Copy(ShaderParameter param, boolean copyDefault, boolean matchType) {
        if (this.type == param.type || !matchType) {
            if (copyDefault || this.type != param.type) {
                this.defaultValue = param.defaultValue;
            }

            this.type = param.type;
            this.value = param.value;
        }
    }

    public void ResetValue() {
        this.value = this.defaultValue;
    }

    public int GetSize() {
        return switch (this.type) {
            case Bool, Int, Float, Texture -> 4;
            case Vector2 -> 8;
            case Vector3 -> 12;
            case Vector4 -> 16;
            case Matrix3 -> 36;
            case Matrix4 -> 64;
            case IntArray, FloatArray, TextureArray -> this.length * 4;
            case Vector2Array -> this.length * 8;
            case Vector3Array, Vector4Array -> this.length * 16;
            case Matrix3Array -> this.length * 36;
            case Matrix4Array -> this.length * 64;
            default -> 0;
        };
    }

    public Object GetValue() {
        return this.value;
    }

    public Boolean GetBool() {
        return (Boolean)this.value;
    }

    public int GetInt() {
        return (Integer)this.value;
    }

    public float GetFloat() {
        return (Float)this.value;
    }

    public Vector2f GetVector2() {
        return (Vector2f)this.value;
    }

    public Vector3f GetVector3() {
        return (Vector3f)this.value;
    }

    public Vector4f GetVector4() {
        return (Vector4f)this.value;
    }

    public Matrix3f GetMatrix3() {
        return (Matrix3f)this.value;
    }

    public Matrix4f GetMatrix4() {
        return (Matrix4f)this.value;
    }

    public int GetTexture() {
        return (Integer)this.value;
    }

    public int[] GetIntArray() {
        return (int[])this.value;
    }

    public float[] GetFloatArray() {
        return (float[])this.value;
    }

    public Vector2f[] GetVector2Array() {
        return (Vector2f[])this.value;
    }

    public Vector3f[] GetVector3Array() {
        return (Vector3f[])this.value;
    }

    public Vector4f[] GetVector4Array() {
        return (Vector4f[])this.value;
    }

    public Matrix3f[] GetMatrix3Array() {
        return (Matrix3f[])this.value;
    }

    public Matrix4f[] GetMatrix4Array() {
        return (Matrix4f[])this.value;
    }

    public int[] GetTextureArray() {
        return (int[])this.value;
    }

    public FloatBuffer GetBuffer() {
        return (FloatBuffer)this.value;
    }

    private void SetValue(Object _value, ShaderParameter.ParameterTypes _type) {
        if (_type != this.type) {
            String str = String.format("Changing parameter %s from %s to %s", this.name, this.type, _type);
            DebugLog.Shader.warn(str);
            this.type = _type;
            this.defaultValue = _value;
        }

        this.value = _value;
    }

    public void SetBool(boolean _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Bool);
    }

    public void SetInt(int _value) {
        if (this.type != ShaderParameter.ParameterTypes.Int || this.value == null || (Integer)this.value != _value) {
            this.SetValue(_value, ShaderParameter.ParameterTypes.Int);
        }
    }

    public void SetFloat(float _value) {
        if (this.type != ShaderParameter.ParameterTypes.Float || this.value == null || (Float)this.value != _value) {
            this.SetValue(_value, ShaderParameter.ParameterTypes.Float);
        }
    }

    public void SetVector2(Vector2f vec) {
        this.SetVector2(vec.x, vec.y);
    }

    public void SetVector2(float x, float y) {
        if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector2) {
            Vector2f vec = (Vector2f)this.value;
            vec.set(x, y);
        } else {
            this.SetValue(new Vector2f(x, y), ShaderParameter.ParameterTypes.Vector2);
        }
    }

    public void SetVector3(Vector3f vec) {
        this.SetVector3(vec.x, vec.y, vec.z);
    }

    public void SetVector3(float x, float y, float z) {
        if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector3) {
            Vector3f vec = (Vector3f)this.value;
            vec.set(x, y, z);
        } else {
            this.SetValue(new Vector3f(x, y, z), ShaderParameter.ParameterTypes.Vector3);
        }
    }

    public void SetVector4(Vector4f vec) {
        this.SetVector4(vec.x, vec.y, vec.z, vec.w);
    }

    public void SetVector4(float x, float y, float z, float w) {
        if (this.value != null && this.type == ShaderParameter.ParameterTypes.Vector4) {
            Vector4f vec = (Vector4f)this.value;
            vec.set(x, y, z, w);
        } else {
            this.SetValue(new Vector4f(x, y, z, w), ShaderParameter.ParameterTypes.Vector4);
        }
    }

    public void SetMatrix3(Matrix3f mat) {
        Matrix3f matrix;
        if (this.value != null && this.type == ShaderParameter.ParameterTypes.Matrix3) {
            matrix = (Matrix3f)this.value;
        } else {
            this.SetValue(matrix = new Matrix3f(), ShaderParameter.ParameterTypes.Matrix3);
        }

        matrix.load(mat);
    }

    public void SetMatrix4(Matrix4f mat) {
        Matrix4f matrix;
        if (this.value != null && this.type == ShaderParameter.ParameterTypes.Matrix4) {
            matrix = (Matrix4f)this.value;
        } else {
            this.SetValue(matrix = new Matrix4f(), ShaderParameter.ParameterTypes.Matrix4);
        }

        matrix.load(mat);
    }

    public void SetTexture(int _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Texture);
    }

    public void SetIntArray(int[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.IntArray);
    }

    public void SetFloatArray(float[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.FloatArray);
    }

    public void SetVector2Array(Vector2f[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Vector2Array);
    }

    public void SetVector3Array(Vector3f[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Vector3Array);
    }

    public void SetVector4Array(Vector4f[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Vector4Array);
    }

    public void SetMatrix3Array(Matrix3f[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Matrix3Array);
    }

    public void SetMatrix4Array(Matrix4f[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.Matrix4Array);
    }

    public void SetTextureArray(int[] _value) {
        this.SetValue(_value, ShaderParameter.ParameterTypes.TextureArray);
    }

    private static FloatBuffer StoreVectors(Vector[] vs, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * vs.length);

        for (int i = 0; i < vs.length; i++) {
            vs[i].store(buffer);
        }

        buffer.flip();
        return buffer;
    }

    private void LoadVectors(int program, Vector[] vs, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * vs.length);
        GL43.glGetUniformfv(program, this.offset, buffer);
        buffer.rewind();

        for (int i = 0; i < vs.length; i++) {
            vs[i].load(buffer);
        }

        MemoryUtil.memFree(buffer);
    }

    private static FloatBuffer StoreMatrices(Matrix[] ms, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * ms.length);

        for (int i = 0; i < ms.length; i++) {
            ms[i].store(buffer);
        }

        buffer.flip();
        return buffer;
    }

    private void LoadMatrices(int program, Matrix[] ms, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * ms.length);
        GL43.glGetUniformfv(program, this.offset, buffer);
        buffer.rewind();

        for (int i = 0; i < ms.length; i++) {
            ms[i].load(buffer);
        }

        MemoryUtil.memFree(buffer);
    }

    public void UpdateDefault() {
        this.defaultValue = this.value;
    }

    public void PushUniform() {
        switch (this.type) {
            case Bool:
                GL43.glUniform1i(this.offset, (Boolean)this.value ? 1 : 0);
                break;
            case Int:
            case Texture:
                GL43.glUniform1i(this.offset, (Integer)this.value);
                break;
            case Float:
                GL43.glUniform1f(this.offset, (Float)this.value);
                break;
            case Vector2: {
                Vector2f v = (Vector2f)this.value;
                GL43.glUniform2f(this.offset, v.x, v.y);
                break;
            }
            case Vector3: {
                Vector3f v = (Vector3f)this.value;
                GL43.glUniform3f(this.offset, v.x, v.y, v.z);
                break;
            }
            case Vector4: {
                Vector4f v = (Vector4f)this.value;
                GL43.glUniform4f(this.offset, v.x, v.y, v.z, v.w);
                break;
            }
            case Matrix3:
                ((Matrix3f)this.value).store(MatrixBuffer);
                GL43.glUniformMatrix3fv(this.offset, true, MatrixBuffer.flip());
                MatrixBuffer.limit(MatrixBuffer.capacity());
                break;
            case Matrix4:
                ((Matrix4f)this.value).store(MatrixBuffer);
                GL43.glUniformMatrix4fv(this.offset, true, MatrixBuffer.flip());
                break;
            case IntArray:
            case TextureArray:
                GL43.glUniform1iv(this.offset, (int[])this.value);
                break;
            case FloatArray:
                GL43.glUniform1fv(this.offset, (float[])this.value);
                break;
            case Vector2Array: {
                FloatBuffer buffer = StoreVectors((Vector2f[])this.value, 2);
                GL43.glUniform2fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case Vector3Array: {
                FloatBuffer buffer = StoreVectors((Vector3f[])this.value, 3);
                GL43.glUniform3fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case Vector4Array: {
                FloatBuffer buffer = StoreVectors((Vector3f[])this.value, 4);
                GL43.glUniform4fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case Matrix3Array: {
                FloatBuffer buffer = StoreMatrices((Matrix3f[])this.value, 9);
                GL43.glUniformMatrix3fv(this.offset, true, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case Matrix4Array: {
                FloatBuffer buffer = StoreMatrices((Matrix4f[])this.value, 16);
                GL43.glUniformMatrix4fv(this.offset, true, buffer);
                MemoryUtil.memFree(buffer);
            }
        }
    }

    public void PullUniform(int program) {
        switch (this.type) {
            case Bool:
                this.value = GL43.glGetUniformi(program, this.offset) != 0;
                break;
            case Int:
            case Texture:
                this.value = GL43.glGetUniformi(program, this.offset);
                break;
            case Float:
                this.value = GL43.glGetUniformf(program, this.offset);
                break;
            case Vector2:
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector2f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            case Vector3:
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector3f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            case Vector4:
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector4f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            case Matrix3:
                GL43.glGetUniformfv(program, this.offset, MatrixBuffer);
                MatrixBuffer.rewind();
                ((Matrix3f)this.value).load(MatrixBuffer);
                MatrixBuffer.rewind();
                break;
            case Matrix4:
                GL43.glGetUniformfv(program, this.offset, MatrixBuffer);
                MatrixBuffer.rewind();
                ((Matrix4f)this.value).load(MatrixBuffer);
                MatrixBuffer.rewind();
                break;
            case IntArray:
            case TextureArray:
                GL43.glGetUniformiv(program, this.offset, (int[])this.value);
                break;
            case FloatArray:
                GL43.glGetUniformfv(program, this.offset, (float[])this.value);
                break;
            case Vector2Array:
                this.LoadVectors(program, (Vector2f[])this.value, 2);
                break;
            case Vector3Array:
                this.LoadVectors(program, (Vector3f[])this.value, 3);
                break;
            case Vector4Array:
                this.LoadVectors(program, (Vector4f[])this.value, 4);
                break;
            case Matrix3Array:
                this.LoadMatrices(program, (Matrix3f[])this.value, 9);
                break;
            case Matrix4Array:
                this.LoadMatrices(program, (Matrix4f[])this.value, 16);
        }
    }

    public void PushInstanced(InstancedBuffer buffer, int baseOffset) {
        this.WriteToBuffer(buffer.data, baseOffset);
    }

    public void WriteToBuffer(ByteBuffer buffer, int baseOffset) {
        if (this.offset >= 0 && this.type != ShaderParameter.ParameterTypes.Texture && this.type != ShaderParameter.ParameterTypes.TextureArray) {
            buffer.position(baseOffset + this.offset);
            switch (this.type) {
                case Bool:
                    ShaderBuffer.PushBool(buffer, (Boolean)this.value);
                    break;
                case Int:
                case Texture:
                    ShaderBuffer.PushInt(buffer, (Integer)this.value);
                    break;
                case Float:
                    ShaderBuffer.PushFloat(buffer, (Float)this.value);
                    break;
                case Vector2:
                    ShaderBuffer.PushVector2(buffer, (Vector2f)this.value);
                    break;
                case Vector3:
                    ShaderBuffer.PushVector3(buffer, (Vector3f)this.value);
                    break;
                case Vector4:
                    ShaderBuffer.PushVector4(buffer, (Vector4f)this.value);
                    break;
                case Matrix3:
                    ShaderBuffer.PushMatrix3(buffer, (Matrix3f)this.value);
                    break;
                case Matrix4:
                    ShaderBuffer.PushMatrix4(buffer, (Matrix4f)this.value);
                    break;
                case IntArray:
                case TextureArray:
                    ShaderBuffer.PushIntArray(buffer, (int[])this.value);
                    break;
                case FloatArray:
                    ShaderBuffer.PushFloatArray(buffer, (float[])this.value);
                    break;
                case Vector2Array:
                    ShaderBuffer.PushVector2Array(buffer, (Vector2f[])this.value);
                    break;
                case Vector3Array:
                    ShaderBuffer.PushVector3Array(buffer, (Vector3f[])this.value);
                    break;
                case Vector4Array:
                    ShaderBuffer.PushVector4Array(buffer, (Vector4f[])this.value);
                case Matrix3Array:
                default:
                    break;
                case Matrix4Array:
                    ShaderBuffer.PushMatrix4Array(buffer, (Matrix4f[])this.value);
            }
        }
    }

    public static enum ParameterTypes {
        Bool,
        Int,
        Float,
        Vector2,
        Vector3,
        Vector4,
        Matrix3,
        Matrix4,
        Texture,
        IntArray,
        FloatArray,
        Vector2Array,
        Vector3Array,
        Vector4Array,
        Matrix3Array,
        Matrix4Array,
        TextureArray;
    }
}
