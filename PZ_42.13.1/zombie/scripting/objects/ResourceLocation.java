// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.Objects;
import zombie.UsedFromLua;

@UsedFromLua
public final class ResourceLocation {
    public static final String DEFAULT_NAMESPACE = "base";
    private final String namespace;
    private final String path;
    private final String id;

    public ResourceLocation(String namespace, String path) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        } else if (path != null && !path.isEmpty()) {
            this.namespace = namespace.toLowerCase();
            this.path = path.toLowerCase();
            this.id = this.namespace + ":" + this.path;
        } else {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
    }

    public static ResourceLocation of(String id) {
        if (id != null && !id.isEmpty()) {
            String[] parts = id.split(":", 2);
            return parts.length == 1 ? new ResourceLocation("base", parts[0]) : new ResourceLocation(parts[0], parts[1]);
        } else {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof ResourceLocation resourceLocation)
                ? false
                : this.namespace.equals(resourceLocation.namespace) && this.path.equals(resourceLocation.path);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.namespace, this.path);
    }
}
