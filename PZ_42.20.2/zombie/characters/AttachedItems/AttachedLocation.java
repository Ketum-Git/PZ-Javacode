// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.AttachedItems;

import zombie.UsedFromLua;

@UsedFromLua
public final class AttachedLocation {
    protected final AttachedLocationGroup group;
    protected final String id;
    protected String attachmentName;

    public AttachedLocation(AttachedLocationGroup group, String id) {
        if (id == null) {
            throw new NullPointerException("id is null");
        } else if (id.isEmpty()) {
            throw new IllegalArgumentException("id is empty");
        } else {
            this.group = group;
            this.id = id;
        }
    }

    public void setAttachmentName(String attachmentName) {
        if (this.id == null) {
            throw new NullPointerException("attachmentName is null");
        } else if (this.id.isEmpty()) {
            throw new IllegalArgumentException("attachmentName is empty");
        } else {
            this.attachmentName = attachmentName;
        }
    }

    public String getAttachmentName() {
        return this.attachmentName;
    }

    public String getId() {
        return this.id;
    }
}
