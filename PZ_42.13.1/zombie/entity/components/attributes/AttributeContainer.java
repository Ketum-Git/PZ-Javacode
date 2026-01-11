// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.attributes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.assoc.AssocArray;
import zombie.entity.util.enums.IOEnum;
import zombie.iso.IsoObject;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.attributes.AttributesScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class AttributeContainer extends Component {
    private short maxAttributeId = -1;
    private final AssocArray<AttributeType, AttributeInstance<?, ?>> attributes = new AssocArray<>();
    public static final short STORAGE_SIZE = 64;
    private static final byte SAVE_EMPTY = 0;
    private static final byte SAVE_COMPRESSED = 1;
    private static final byte SAVE_UNCOMPRESSED_8 = 8;
    private static final byte SAVE_UNCOMPRESSED_16 = 16;

    private AttributeContainer() {
        super(ComponentType.Attributes);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        AttributesScript attributesScript = (AttributesScript)componentScript;
        if (attributesScript.getTemplateContainer() != null) {
            Copy(attributesScript.getTemplateContainer(), this);
        } else {
            DebugLog.General.error("Unable to create AttributeContainer from script: " + componentScript.getName());
        }
    }

    @Override
    public String toString() {
        boolean ownerIso = this.owner != null && this.owner instanceof IsoObject;
        String attributesStr = "";

        for (int i = 0; i < this.attributes.size(); i++) {
            attributesStr = attributesStr + this.attributes.getKey(i).toString() + ";";
        }

        return "AttributeContainer [owner = "
            + (this.owner != null ? this.owner.toString() : "null")
            + ", iso = "
            + ownerIso
            + ", attributes = "
            + attributesStr
            + "]";
    }

    public int size() {
        return this.attributes.size();
    }

    public void forEach(BiConsumer<AttributeType, AttributeInstance> action) {
        this.attributes.forEach(action);
    }

    public boolean contains(AttributeType type) {
        return this.attributes.containsKey(type);
    }

    public void remove(AttributeType type) {
        this.removeAndRelease(type);
    }

    private AttributeInstance removeAndRelease(AttributeType type) {
        AttributeInstance attribute = this.attributes.remove(type);
        if (attribute != null) {
            attribute.release();
        }

        return attribute;
    }

    protected AttributeInstance<?, ?> getOrAdd(AttributeType type) {
        AttributeInstance<?, ?> attribute = this.attributes.get(type);
        if (attribute == null) {
            attribute = AttributeFactory.Create(type);
            this.attributes.put(type, attribute);
            if (attribute.getType().id() > this.maxAttributeId) {
                this.maxAttributeId = attribute.getType().id();
            }
        }

        return attribute;
    }

    public boolean add(AttributeType type) {
        if (!this.contains(type)) {
            AttributeInstance attribute = AttributeFactory.Create(type);
            this.attributes.put(type, attribute);
            if (attribute.getType().id() > this.maxAttributeId) {
                this.maxAttributeId = attribute.getType().id();
            }
        }

        return false;
    }

    public final boolean putFromScript(AttributeType type, String scriptVal) {
        AttributeInstance attribute = this.getOrAdd(type);
        return attribute.setValueFromScriptString(scriptVal);
    }

    public final <E extends Enum<E> & IOEnum> void put(AttributeType.Enum<E> type, E value) {
        AttributeInstance<?, ?> attribute = this.getOrAdd(type);
        ((AttributeInstance.Enum)attribute).setValue(value);
    }

    public final <E extends Enum<E> & IOEnum> void set(AttributeType.Enum<E> type, E value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Enum)attribute).setValue(value);
        }
    }

    public final <E extends Enum<E> & IOEnum> E get(AttributeType.Enum<E> type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return (E)((AttributeInstance.Enum)attribute).getValue();
        }
    }

    public final <E extends Enum<E> & IOEnum> E get(AttributeType.Enum<E> type, E defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return (E)(attribute != null ? ((AttributeInstance.Enum)attribute).getValue() : defaultTo);
    }

    public final <E extends Enum<E> & IOEnum> void put(AttributeType.EnumSet<E> type, EnumSet<E> value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.EnumSet)attribute).setValue(value);
    }

    public final <E extends Enum<E> & IOEnum> void set(AttributeType.EnumSet<E> type, EnumSet<E> value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.EnumSet)attribute).setValue(value);
        }
    }

    public final <E extends Enum<E> & IOEnum> EnumSet<E> get(AttributeType.EnumSet<E> type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.EnumSet)attribute).getValue();
        }
    }

    public final <E extends Enum<E> & IOEnum> void put(AttributeType.EnumStringSet<E> type, EnumStringObj<E> value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.EnumStringSet)attribute).setValue(value);
    }

    public final <E extends Enum<E> & IOEnum> void set(AttributeType.EnumStringSet<E> type, EnumStringObj<E> value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.EnumStringSet)attribute).setValue(value);
        }
    }

    public final <E extends Enum<E> & IOEnum> EnumStringObj<E> get(AttributeType.EnumStringSet<E> type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.EnumStringSet)attribute).getValue();
        }
    }

    public final void put(AttributeType.String type, String value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.String)attribute).setValue(value);
    }

    public final void set(AttributeType.String type, String value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.String)attribute).setValue(value);
        }
    }

    public final String get(AttributeType.String type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.String)attribute).getValue();
        }
    }

    public final String get(AttributeType.String type, String defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.String)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Bool type, boolean value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Bool)attribute).setValue(value);
    }

    public final void set(AttributeType.Bool type, boolean value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Bool)attribute).setValue(value);
        }
    }

    public final boolean get(AttributeType.Bool type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Bool)attribute).getValue();
        }
    }

    public final boolean get(AttributeType.Bool type, boolean defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Bool)attribute).getValue() : defaultTo;
    }

    public final void putFloatValue(AttributeType.Numeric type, float value) {
        AttributeInstance attribute = this.getOrAdd(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Numeric)attribute).fromFloat(value);
        }
    }

    public final void setFloatValue(AttributeType.Numeric type, float value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Numeric)attribute).fromFloat(value);
        }
    }

    public final float getFloatValue(AttributeType.Numeric type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Numeric)attribute).floatValue();
        }
    }

    public final float getFloatValue(AttributeType.Numeric type, float defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Numeric)attribute).floatValue() : defaultTo;
    }

    public final void put(AttributeType.Float type, float value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Float)attribute).setValue(value);
    }

    public final void set(AttributeType.Float type, float value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Float)attribute).setValue(value);
        }
    }

    public final float get(AttributeType.Float type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Float)attribute).getValue();
        }
    }

    public final float get(AttributeType.Float type, float defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Float)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Double type, double value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Double)attribute).setValue(value);
    }

    public final void set(AttributeType.Double type, double value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Double)attribute).setValue(value);
        }
    }

    public final double get(AttributeType.Double type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Double)attribute).getValue();
        }
    }

    public final double get(AttributeType.Double type, double defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Double)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Byte type, byte value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Byte)attribute).setValue(value);
    }

    public final void set(AttributeType.Byte type, byte value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Byte)attribute).setValue(value);
        }
    }

    public final byte get(AttributeType.Byte type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Byte)attribute).getValue();
        }
    }

    public final byte get(AttributeType.Byte type, byte defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Byte)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Short type, short value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Short)attribute).setValue(value);
    }

    public final void set(AttributeType.Short type, short value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Short)attribute).setValue(value);
        }
    }

    public final short get(AttributeType.Short type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Short)attribute).getValue();
        }
    }

    public final short get(AttributeType.Short type, short defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Short)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Int type, int value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Int)attribute).setValue(value);
    }

    public final void set(AttributeType.Int type, int value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Int)attribute).setValue(value);
        }
    }

    public final int get(AttributeType.Int type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Int)attribute).getValue();
        }
    }

    public final int get(AttributeType.Int type, int defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute != null ? ((AttributeInstance.Int)attribute).getValue() : defaultTo;
    }

    public final void put(AttributeType.Long type, long value) {
        AttributeInstance attribute = this.getOrAdd(type);
        ((AttributeInstance.Long)attribute).setValue(value);
    }

    public final void set(AttributeType.Long type, long value) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            ((AttributeInstance.Long)attribute).setValue(value);
        }
    }

    public final long get(AttributeType.Long type) {
        AttributeInstance attribute = this.attributes.get(type);
        if (attribute == null) {
            throw new UnsupportedOperationException("Container does not contain attribute '" + type + "'.");
        } else {
            return ((AttributeInstance.Long)attribute).getValue();
        }
    }

    public final long get(AttributeType.Long type, long defaultTo) {
        AttributeInstance attribute = this.attributes.get(type);
        return attribute == null ? ((AttributeInstance.Long)attribute).getValue() : defaultTo;
    }

    public AttributeType getKey(int index) {
        return this.attributes.getKey(index);
    }

    public AttributeInstance getAttribute(int index) {
        return this.attributes.getValue(index);
    }

    public AttributeInstance getAttribute(AttributeType type) {
        return this.attributes.get(type);
    }

    private void recalculateMaxId() {
        this.maxAttributeId = -1;
        if (this.attributes.size() > 0) {
            for (int i = 0; i < this.attributes.size(); i++) {
                AttributeType type = this.attributes.getKey(i);
                if (type.id() > this.maxAttributeId) {
                    this.maxAttributeId = type.id();
                }
            }
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.clear();
    }

    public void clear() {
        if (!this.attributes.isEmpty()) {
            for (int i = 0; i < this.attributes.size(); i++) {
                this.attributes.getValue(i).release();
            }
        }

        this.attributes.clear();
        this.maxAttributeId = -1;
    }

    public static void Copy(AttributeContainer source, AttributeContainer target) {
        target.clear();

        for (int i = 0; i < source.attributes.size(); i++) {
            AttributeInstance attribute = source.attributes.getValue(i);
            target.attributes.put(attribute.getType(), attribute.copy());
        }

        target.maxAttributeId = source.maxAttributeId;
    }

    public static void Merge(AttributeContainer source, AttributeContainer target) {
        for (int i = 0; i < source.attributes.size(); i++) {
            AttributeInstance attribute = source.attributes.getValue(i);
            if (target.attributes.containsKey(attribute.getType())) {
                AttributeInstance targAttribute = target.attributes.remove(attribute.getType());
                targAttribute.release();
            }

            target.attributes.put(attribute.getType(), attribute.copy());
        }

        target.maxAttributeId = (short)Math.max(target.maxAttributeId, source.maxAttributeId);
    }

    public AttributeContainer copy() {
        AttributeContainer other = (AttributeContainer)ComponentType.Attributes.CreateComponent();
        Copy(this, other);
        return other;
    }

    public boolean isIdenticalTo(AttributeContainer other) {
        if (this.size() == 0 && other.size() == 0) {
            return true;
        } else if (this.size() != other.size()) {
            return false;
        } else {
            for (int i = 0; i < this.attributes.size(); i++) {
                AttributeInstance attribute = this.attributes.getValue(i);
                AttributeInstance attributeOther = other.attributes.get(attribute.getType());
                if (attributeOther == null || !attribute.equalTo(attributeOther)) {
                    return false;
                }
            }

            return true;
        }
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        switch (type) {
            default:
                return false;
        }
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 240);
    }

    @Override
    public void save(ByteBuffer output) {
        if (this.maxAttributeId == -1) {
            output.put((byte)0);
        } else {
            byte idBitSize = (byte)(this.maxAttributeId > 127 ? 16 : 8);
            int storages = 1 + this.maxAttributeId / 64;
            int attrCount = this.attributes.size();
            int STORAGE_BYTES = 8;
            if (attrCount * idBitSize + 16 > storages * 64) {
                output.put((byte)1);
                output.put((byte)storages);
                int headersStartPos = output.position();

                for (int i = 0; i < storages; i++) {
                    output.putLong(0L);
                }

                for (int i = 0; i < this.attributes.size(); i++) {
                    AttributeInstance attribute = this.attributes.getValue(i);
                    int id = attribute.getType().id();
                    int headerPos = headersStartPos + 8 * (id / 64);
                    int bitIndex = id % 64;
                    long bit = 1L << bitIndex;
                    int curPos = output.position();
                    output.position(headerPos);
                    long header = output.getLong();
                    header |= bit;
                    output.position(headerPos);
                    output.putLong(header);
                    output.position(curPos);
                    attribute.save(output);
                }
            } else {
                output.put(idBitSize);
                output.putShort((short)attrCount);

                for (int i = 0; i < this.attributes.size(); i++) {
                    AttributeInstance attribute = this.attributes.getValue(i);
                    if (idBitSize == 8) {
                        output.put((byte)attribute.getType().id());
                    } else {
                        output.putShort(attribute.getType().id());
                    }

                    attribute.save(output);
                }
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.clear();
        byte saved_mode = input.get();
        if (saved_mode != 0) {
            int STORAGE_BYTES = 8;
            if (saved_mode == 1) {
                int storages = input.get();
                if (storages == 0) {
                    return;
                }

                int headersStartPos = input.position();
                int saveBlockPos = headersStartPos + storages * 8;

                for (short headerIndex = 0; headerIndex < storages; headerIndex++) {
                    input.position(headersStartPos + headerIndex * 8);
                    long header = input.getLong();
                    input.position(saveBlockPos);
                    long bit = 1L;

                    for (short bitIndex = 0; bitIndex < 64; bitIndex++) {
                        if ((header & bit) == bit) {
                            short id = (short)(headerIndex * 64 + bitIndex);
                            AttributeType type = Attribute.TypeFromId(id);
                            if (type == null) {
                                throw new IOException("Unable to read attribute type.");
                            }

                            AttributeInstance attribute = this.getOrAdd(type);
                            if (attribute != null) {
                                attribute.load(input);
                            }
                        }

                        bit <<= 1;
                    }

                    saveBlockPos = input.position();
                }
            } else {
                int attrCount = input.getShort();

                for (int i = 0; i < attrCount; i++) {
                    AttributeType typex;
                    if (saved_mode == 8) {
                        typex = Attribute.TypeFromId(input.get());
                    } else {
                        typex = Attribute.TypeFromId(input.getShort());
                    }

                    if (typex == null) {
                        throw new IOException("Unable to read attribute type.");
                    }

                    AttributeInstance attribute = this.getOrAdd(typex);
                    if (attribute != null) {
                        attribute.load(input);
                    }
                }
            }
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            if (this.size() > 0) {
                ArrayList<AttributeInstance> list = new ArrayList<>();

                for (int i = 0; i < this.size(); i++) {
                    AttributeInstance attr = this.getAttribute(i);
                    if (!attr.isHiddenUI()) {
                        list.add(attr);
                    }
                }

                if (DebugOptions.instance.tooltipAttributes.getValue()) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    Color c = Colors.CornFlowerBlue;
                    item.setLabel("[Debug Begin Attributes]", c.r, c.g, c.b, 1.0F);
                }

                list.sort(Comparator.comparing(AttributeInstance::getNameUI));

                for (AttributeInstance attribute : list) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    item.setLabel(attribute.getNameUI() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (attribute.isDisplayAsBar()) {
                        float f = attribute.getDisplayAsBarUnit();
                        item.setProgress(f, 0.0F, 0.6F, 0.0F, 0.7F);
                        if (DebugOptions.instance.tooltipAttributes.getValue()) {
                            item = layout.addItem();
                            item.setLabel("*" + attribute.getNameUI() + ":", 0.5F, 0.5F, 0.5F, 1.0F);
                            item.setValue(attribute.stringValue(), 0.5F, 0.5F, 0.5F, 1.0F);
                        }
                    } else {
                        item.setValue(attribute.stringValue(), 1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }

                if (DebugOptions.instance.tooltipAttributes.getValue()) {
                    list.clear();

                    for (int ix = 0; ix < this.size(); ix++) {
                        AttributeInstance attr = this.getAttribute(ix);
                        if (attr.isHiddenUI()) {
                            list.add(attr);
                        }
                    }

                    if (!list.isEmpty()) {
                        ObjectTooltip.LayoutItem item = layout.addItem();
                        Color c = Colors.CornFlowerBlue;
                        item.setLabel("[Debug Hidden Attributes]", c.r, c.g, c.b, 1.0F);

                        for (AttributeInstance attributex : list) {
                            item = layout.addItem();
                            item.setLabel(attributex.getNameUI() + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                            if (attributex.isDisplayAsBar()) {
                                float f = attributex.getDisplayAsBarUnit();
                                item.setProgress(f, 0.0F, 0.6F, 0.0F, 0.7F);
                                if (DebugOptions.instance.tooltipAttributes.getValue()) {
                                    item = layout.addItem();
                                    item.setLabel("*" + attributex.getNameUI() + ":", 0.5F, 0.5F, 0.5F, 1.0F);
                                    item.setValue(attributex.stringValue(), 0.5F, 0.5F, 0.5F, 1.0F);
                                }
                            } else {
                                item.setValue(attributex.stringValue(), 1.0F, 1.0F, 1.0F, 1.0F);
                            }
                        }
                    }

                    ObjectTooltip.LayoutItem item = layout.addItem();
                    Color c = Colors.CornFlowerBlue;
                    item.setLabel("[Debug End Attributes]", c.r, c.g, c.b, 1.0F);
                }
            }
        }
    }
}
