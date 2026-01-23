// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.debug.DebugOptions;
import zombie.network.GameClient;
import zombie.util.list.PZUnmodifiableList;

public class ResourceGroup {
    protected static final ConcurrentLinkedDeque<ResourceGroup> pool = new ConcurrentLinkedDeque<>();
    private boolean dirty;
    private String name;
    private final ArrayList<Resource> resources = new ArrayList<>();
    private final List<Resource> immutableResources = PZUnmodifiableList.wrap(this.resources);

    protected static ResourceGroup allocAnonymous() {
        ResourceGroup o = pool.poll();
        if (o == null) {
            o = new ResourceGroup();
        }

        return o;
    }

    protected static ResourceGroup alloc(String name) {
        ResourceGroup o = pool.poll();
        if (o == null) {
            o = new ResourceGroup();
        }

        o.name = Objects.requireNonNull(name);
        return o;
    }

    protected static void release(ResourceGroup group) {
        group.reset();
        if (!DebugOptions.instance.checks.objectPoolContains.getValue() || !pool.contains(group)) {
            pool.offer(group);
        }
    }

    private ResourceGroup() {
    }

    public boolean isDirty() {
        return this.dirty;
    }

    void setDirty() {
        if (!GameClient.client) {
            this.dirty = true;
        }
    }

    protected void resetDirty() {
        this.dirty = false;
    }

    public List<Resource> getResources() {
        return this.immutableResources;
    }

    public String getName() {
        return this.name;
    }

    protected int size() {
        return this.resources.size();
    }

    protected Resource get(int index) {
        return this.resources.get(index);
    }

    protected void add(Resource resource) {
        this.resources.add(resource);
    }

    protected boolean remove(Resource resource) {
        return this.resources.remove(resource);
    }

    private void reset() {
        this.resources.clear();
        this.name = null;
        this.dirty = false;
    }

    public Resource get(String name_id) {
        if (name_id != null) {
            for (int i = 0; i < this.resources.size(); i++) {
                Resource resource = this.resources.get(i);
                if (resource != null && resource.getId() != null && resource.getId().equalsIgnoreCase(name_id)) {
                    return resource;
                }
            }
        }

        return null;
    }

    public int getIndex(Resource resource) {
        return this.resources.indexOf(resource);
    }

    public List<Resource> getResources(List<Resource> list, ResourceIO io) {
        return this.getResources(this.resources, list, io, ResourceType.Any, null, true);
    }

    public List<Resource> getResources(List<Resource> list, ResourceType type) {
        return this.getResources(this.resources, list, ResourceIO.Any, type, null, true);
    }

    public List<Resource> getResources(List<Resource> list, ResourceIO io, ResourceChannel channel) {
        return this.getResources(this.resources, list, io, ResourceType.Any, channel, true);
    }

    public List<Resource> getResources(List<Resource> list, ResourceChannel channel) {
        return this.getResources(this.resources, list, ResourceIO.Any, ResourceType.Any, channel, true);
    }

    public List<Resource> getResources(List<Resource> list, ResourceIO io, ResourceType type) {
        return this.getResources(this.resources, list, io, type, null, true);
    }

    private List<Resource> getResources(List<Resource> sources, List<Resource> list, ResourceIO io, ResourceType type, ResourceChannel channel, boolean clear) {
        if (clear && !list.isEmpty()) {
            list.clear();
        }

        for (int i = 0; i < sources.size(); i++) {
            Resource resource = sources.get(i);
            if (resource != null
                && (io == ResourceIO.Any || resource.getIO() == io)
                && (type == ResourceType.Any || resource.getType() == type)
                && (channel == null || resource.getChannel() == channel)) {
                list.add(resource);
            }
        }

        return list;
    }
}
