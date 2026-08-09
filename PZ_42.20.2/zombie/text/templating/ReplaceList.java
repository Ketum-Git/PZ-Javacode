// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.text.templating;

import java.util.ArrayList;

public class ReplaceList implements IReplace {
    private final ArrayList<String> replacements;

    public ReplaceList() {
        this.replacements = new ArrayList<>();
    }

    public ReplaceList(ArrayList<String> replacements) {
        this.replacements = replacements;
    }

    protected ArrayList<String> getReplacements() {
        return this.replacements;
    }

    @Override
    public String getString() {
        return this.replacements.isEmpty() ? "!ERROR_EMPTY_LIST!" : this.replacements.get(TemplateText.RandNext(this.replacements.size()));
    }
}
