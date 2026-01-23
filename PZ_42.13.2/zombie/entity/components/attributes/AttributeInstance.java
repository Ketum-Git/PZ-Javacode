// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.util.enums.IOEnum;

@UsedFromLua
public abstract class AttributeInstance<C extends AttributeInstance<C, T>, T extends AttributeType> {
    protected T type;

    protected AttributeInstance() {
    }

    protected abstract void setType(T var1);

    public final T getType() {
        return this.type;
    }

    public final AttributeValueType getValueType() {
        return this.type.getValueType();
    }

    public final java.lang.String getNameUI() {
        return this.type.getNameUI();
    }

    public final boolean isHiddenUI() {
        return this.type.isHiddenUI();
    }

    public boolean isRequiresValidation() {
        return false;
    }

    public final boolean isReadOnly() {
        return this.type.isReadOnly();
    }

    protected boolean canSetValue() {
        if (this.isReadOnly()) {
            DebugLog.General.error("Trying to set value on a read-only attribute [" + this.toString() + "]");
            return false;
        } else {
            return true;
        }
    }

    public abstract java.lang.String stringValue();

    public abstract boolean setValueFromScriptString(java.lang.String arg0);

    public abstract boolean equalTo(C var1);

    public abstract C copy();

    public boolean isDisplayAsBar() {
        return false;
    }

    public float getDisplayAsBarUnit() {
        return 0.0F;
    }

    public float getFloatValue() {
        return 0.0F;
    }

    public int getIntValue() {
        return 0;
    }

    protected void reset() {
        this.type = null;
    }

    protected abstract void release();

    public abstract void save(ByteBuffer arg0);

    public abstract void load(ByteBuffer arg0);

    @Override
    public java.lang.String toString() {
        return "Attribute."
            + (this.type != null ? this.type : "NOT_SET")
            + " [value = "
            + this.stringValue()
            + ", valueType = "
            + (this.type != null ? this.type.getValueType() : "NOT_SET")
            + ", hidden = "
            + this.isHiddenUI()
            + ", req_val = "
            + this.isRequiresValidation()
            + ", read-only = "
            + this.isReadOnly()
            + "]";
    }

    @UsedFromLua
    public static class Bool extends AttributeInstance<AttributeInstance.Bool, AttributeType.Bool> {
        private boolean value;

        protected void setType(AttributeType.Bool type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public boolean getValue() {
            return this.value;
        }

        public void setValue(boolean value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return Boolean.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = Boolean.parseBoolean(val);
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Bool other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Bool copy() {
            AttributeInstance.Bool copy = AttributeFactory.AllocAttributeBool();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.put((byte)(this.value ? 1 : 0));
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.get() == 1;
        }
    }

    @UsedFromLua
    public static class Byte extends AttributeInstance.Numeric<AttributeInstance.Byte, AttributeType.Byte> {
        private byte value;

        protected void setType(AttributeType.Byte type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public byte getValue() {
            return this.value;
        }

        public void setValue(byte value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((byte)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Byte.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(java.lang.Byte.parseByte(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Byte other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Byte copy() {
            AttributeInstance.Byte copy = AttributeFactory.AllocAttributeByte();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.put(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.get();
        }
    }

    @UsedFromLua
    public static class Double extends AttributeInstance.Numeric<AttributeInstance.Double, AttributeType.Double> {
        private double value;

        protected void setType(AttributeType.Double type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public double getValue() {
            return this.value;
        }

        public void setValue(double value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            if (this.value < -java.lang.Float.MAX_VALUE || this.value > java.lang.Float.MAX_VALUE) {
                DebugLog.General.error("Attribute '" + this.type + "' double value exceeds float bounds.");
            }

            return (float)this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue(f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Double.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(java.lang.Double.parseDouble(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Double other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Double copy() {
            AttributeInstance.Double copy = AttributeFactory.AllocAttributeDouble();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putDouble(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getDouble();
        }
    }

    @UsedFromLua
    public static class Enum<E extends java.lang.Enum<E> & IOEnum> extends AttributeInstance<AttributeInstance.Enum<E>, AttributeType.Enum<E>> {
        private E value;

        protected void setType(AttributeType.Enum<E> type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public E getValue() {
            return this.value;
        }

        public void setValue(E value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value.toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            this.value = this.type.enumValueFromString(val);
            if (this.value == null) {
                this.value = this.type.getInitialValue();
            }

            return true;
        }

        public boolean equalTo(AttributeInstance.Enum<E> other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Enum<E> copy() {
            AttributeInstance.Enum<E> copy = AttributeFactory.AllocAttributeEnum();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value = null;
        }

        @Override
        public void save(ByteBuffer output) {
            output.put(this.value.getByteId());
        }

        @Override
        public void load(ByteBuffer input) {
            byte id = input.get();
            this.value = this.type.enumValueFromByteID(id);
            if (this.value == null) {
                DebugLog.General.error("Could not load value for Enum attribute '" + this.type + "', setting default.");
                this.value = this.type.getInitialValue();
            }
        }
    }

    @UsedFromLua
    public static class EnumSet<E extends java.lang.Enum<E> & IOEnum> extends AttributeInstance<AttributeInstance.EnumSet<E>, AttributeType.EnumSet<E>> {
        private java.util.EnumSet<E> value;

        protected void setType(AttributeType.EnumSet<E> type) {
            this.type = type;
            this.value = java.util.EnumSet.copyOf(type.getInitialValue());
        }

        public java.util.EnumSet<E> getValue() {
            return this.value;
        }

        public void setValue(java.util.EnumSet<E> value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value.toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                if (!this.value.isEmpty()) {
                    this.value.clear();
                }

                if (val.contains(";")) {
                    java.lang.String[] split = val.split(";");

                    for (java.lang.String s : split) {
                        this.addValueFromString(s);
                    }
                } else {
                    this.addValueFromString(val);
                }

                return true;
            } catch (Exception var7) {
                DebugLog.General.error("Error in script string '" + val + "'");
                var7.printStackTrace();
                return false;
            }
        }

        public void addValueFromString(java.lang.String val) {
            E e = this.type.enumValueFromString(val);
            if (e != null) {
                this.value.add(e);
            } else {
                throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + val + "'.");
            }
        }

        public boolean removeValueFromString(java.lang.String val) {
            E e = this.type.enumValueFromString(val);
            if (e != null) {
                return this.value.remove(e);
            } else {
                throw new NullPointerException("Attribute.EnumSet Cannot read script value '" + val + "'.");
            }
        }

        public void clear() {
            this.value.clear();
        }

        public boolean equalTo(AttributeInstance.EnumSet<E> other) {
            return this.type == other.type ? this.value.equals(other.value) : true;
        }

        public AttributeInstance.EnumSet<E> copy() {
            AttributeInstance.EnumSet<E> copy = AttributeFactory.AllocAttributeEnumSet();
            copy.setType(this.type);
            copy.value.addAll(this.value);
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value = null;
        }

        @Override
        public void save(ByteBuffer output) {
            output.put((byte)this.value.size());

            for (E val : this.value) {
                output.put(val.getByteId());
            }
        }

        @Override
        public void load(ByteBuffer input) {
            if (!this.value.isEmpty()) {
                this.value.clear();
            }

            int size = input.get();

            for (int i = 0; i < size; i++) {
                byte id = input.get();
                E val = this.type.enumValueFromByteID(id);
                if (val != null) {
                    this.value.add(this.type.enumValueFromByteID(id));
                } else {
                    DebugLog.General.error("Could not load value for EnumSet attribute '" + this.type + "'.");
                }
            }
        }
    }

    @UsedFromLua
    public static class EnumStringSet<E extends java.lang.Enum<E> & IOEnum>
        extends AttributeInstance<AttributeInstance.EnumStringSet<E>, AttributeType.EnumStringSet<E>> {
        private final EnumStringObj<E> value = new EnumStringObj<>();

        protected void setType(AttributeType.EnumStringSet<E> type) {
            this.type = type;
            this.value.initialize(type.getEnumClass());
            this.value.addAll(true, type.getInitialValue());
        }

        public EnumStringObj<E> getValue() {
            return this.value;
        }

        public void setValue(EnumStringObj<E> value) {
            if (this.canSetValue()) {
                this.value.addAll(true, value);
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value.toString();
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value.clear();
                if (val.contains(";")) {
                    java.lang.String[] split = val.split(";");

                    for (java.lang.String s : split) {
                        if (AttributeUtil.isEnumString(s)) {
                            this.addEnumValueFromString(s);
                        } else {
                            this.addStringValue(s);
                        }
                    }
                } else if (AttributeUtil.isEnumString(val)) {
                    this.addEnumValueFromString(val);
                } else {
                    this.addStringValue(val);
                }

                return true;
            } catch (Exception var7) {
                var7.printStackTrace();
                return false;
            }
        }

        public void addEnumValueFromString(java.lang.String val) {
            E e = this.type.enumValueFromString(val);
            if (e != null) {
                this.value.add(e);
            } else {
                throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + val + "'.");
            }
        }

        public boolean removeEnumValueFromString(java.lang.String val) {
            E e = this.type.enumValueFromString(val);
            if (e != null) {
                return this.value.remove(e);
            } else {
                throw new NullPointerException("Attribute.EnumSet Cannot read Enum script value '" + val + "'.");
            }
        }

        public void addStringValue(java.lang.String val) {
            this.value.add(val);
        }

        public boolean removeStringValue(java.lang.String val) {
            return this.value.remove(val);
        }

        public void clear() {
            this.value.clear();
        }

        public boolean equalTo(AttributeInstance.EnumStringSet<E> other) {
            return this.type == other.type ? this.value.equals(other.value) : true;
        }

        public AttributeInstance.EnumStringSet<E> copy() {
            AttributeInstance.EnumStringSet<E> copy = AttributeFactory.AllocAttributeEnumStringSet();
            copy.setType(this.type);
            copy.value.initialize(this.type.getEnumClass());
            copy.value.addAll(this.value);
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        protected void reset() {
            super.reset();
            this.value.reset();
        }

        @Override
        public void save(ByteBuffer output) {
            output.put((byte)this.value.getEnumValues().size());

            for (E val : this.value.getEnumValues()) {
                output.put(val.getByteId());
            }

            output.put((byte)this.value.getStringValues().size());

            for (int i = 0; i < this.value.getStringValues().size(); i++) {
                GameWindow.WriteString(output, this.value.getStringValues().get(i));
            }
        }

        @Override
        public void load(ByteBuffer input) {
            if (!this.value.isEmpty()) {
                this.value.clear();
            }

            int size = input.get();

            for (int i = 0; i < size; i++) {
                byte id = input.get();
                E val = this.type.enumValueFromByteID(id);
                if (val != null) {
                    this.value.add(this.type.enumValueFromByteID(id));
                } else {
                    DebugLog.General.error("Could not load value for EnumStringSet attribute '" + this.type + "'.");
                }
            }

            int var6 = input.get();

            for (int ix = 0; ix < var6; ix++) {
                java.lang.String s = GameWindow.ReadString(input);
                this.value.add(s);
            }
        }
    }

    @UsedFromLua
    public static class Float extends AttributeInstance.Numeric<AttributeInstance.Float, AttributeType.Float> {
        private float value;

        protected void setType(AttributeType.Float type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public float getValue() {
            return this.value;
        }

        public void setValue(float value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue(f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Float.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(java.lang.Float.parseFloat(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Float other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Float copy() {
            AttributeInstance.Float copy = AttributeFactory.AllocAttributeFloat();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putFloat(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getFloat();
        }
    }

    @UsedFromLua
    public static class Int extends AttributeInstance.Numeric<AttributeInstance.Int, AttributeType.Int> {
        private int value;

        protected void setType(AttributeType.Int type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public int getValue() {
            return this.value;
        }

        public void setValue(int value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((int)f);
        }

        @Override
        public java.lang.String stringValue() {
            return Integer.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(Integer.parseInt(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Int other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Int copy() {
            AttributeInstance.Int copy = AttributeFactory.AllocAttributeInt();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putInt(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getInt();
        }
    }

    @UsedFromLua
    public static class Long extends AttributeInstance.Numeric<AttributeInstance.Long, AttributeType.Long> {
        private long value;

        protected void setType(AttributeType.Long type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public long getValue() {
            return this.value;
        }

        public void setValue(long value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            return (float)this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((long)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Long.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(java.lang.Long.parseLong(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Long other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Long copy() {
            AttributeInstance.Long copy = AttributeFactory.AllocAttributeLong();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putLong(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getLong();
        }
    }

    @UsedFromLua
    public abstract static class Numeric<C extends AttributeInstance.Numeric<C, T>, T extends AttributeType.Numeric<T, ?>> extends AttributeInstance<C, T> {
        public abstract float floatValue();

        public abstract void fromFloat(float arg0);

        @Override
        public boolean isRequiresValidation() {
            return ((AttributeType.Numeric)this.type).isRequiresValidation();
        }

        @Override
        public boolean isDisplayAsBar() {
            return ((AttributeType.Numeric)this.type).getDisplayAsBar() != Attribute.UI.DisplayAsBar.Never
                ? ((AttributeType.Numeric)this.type).getVars() != null
                : false;
        }

        @Override
        public float getDisplayAsBarUnit() {
            if (((AttributeType.Numeric)this.type).getVars() != null) {
                float min = ((Number)((AttributeType.Numeric)this.type).getVars().min).floatValue();
                float max = ((Number)((AttributeType.Numeric)this.type).getVars().max).floatValue();
                float val = this.floatValue();
                return (val - min) / (max - min);
            } else {
                return 0.0F;
            }
        }

        @Override
        public float getFloatValue() {
            return this.floatValue();
        }

        @Override
        public int getIntValue() {
            return (int)this.floatValue();
        }
    }

    @UsedFromLua
    public static class Short extends AttributeInstance.Numeric<AttributeInstance.Short, AttributeType.Short> {
        private short value;

        protected void setType(AttributeType.Short type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public short getValue() {
            return this.value;
        }

        public void setValue(short value) {
            if (this.canSetValue()) {
                this.value = this.type.validate(value);
            }
        }

        @Override
        public float floatValue() {
            return this.value;
        }

        @Override
        public void fromFloat(float f) {
            this.setValue((short)f);
        }

        @Override
        public java.lang.String stringValue() {
            return java.lang.Short.toString(this.value);
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = this.type.validate(java.lang.Short.parseShort(val));
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.Short other) {
            return this.type == other.type ? this.value == other.value : true;
        }

        public AttributeInstance.Short copy() {
            AttributeInstance.Short copy = AttributeFactory.AllocAttributeShort();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putShort(this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = input.getShort();
        }
    }

    @UsedFromLua
    public static class String extends AttributeInstance<AttributeInstance.String, AttributeType.String> {
        private java.lang.String value;

        protected void setType(AttributeType.String type) {
            this.type = type;
            this.value = type.getInitialValue();
        }

        public java.lang.String getValue() {
            return this.value;
        }

        public void setValue(java.lang.String value) {
            if (this.canSetValue()) {
                this.value = value;
            }
        }

        @Override
        public java.lang.String stringValue() {
            return this.value;
        }

        @Override
        public boolean setValueFromScriptString(java.lang.String val) {
            try {
                this.value = val;
                return true;
            } catch (Exception var3) {
                var3.printStackTrace();
                this.value = this.type.getInitialValue();
                return false;
            }
        }

        public boolean equalTo(AttributeInstance.String other) {
            return this.type == other.type ? this.value.equals(other.value) : true;
        }

        public AttributeInstance.String copy() {
            AttributeInstance.String copy = AttributeFactory.AllocAttributeString();
            copy.setType(this.type);
            copy.value = this.value;
            return copy;
        }

        @Override
        protected void release() {
            AttributeFactory.Release(this);
        }

        @Override
        public void save(ByteBuffer output) {
            GameWindow.WriteString(output, this.value);
        }

        @Override
        public void load(ByteBuffer input) {
            this.value = GameWindow.ReadString(input);
        }
    }
}
