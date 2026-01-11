// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

public class ReplaceSingle implements IReplace {
    private String value = "";

    public ReplaceSingle() {
    }

    public ReplaceSingle(String value) {
        this.value = value;
    }

    protected String getValue() {
        return this.value;
    }

    protected void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getString() {
        return this.value;
    }
}
