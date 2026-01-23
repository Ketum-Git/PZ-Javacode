// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.resources.ResourcesScript;
import zombie.ui.ObjectTooltip;
import zombie.util.list.PZUnmodifiableList;

@DebugClassFields
@UsedFromLua
public class Resources extends Component {
    public static final String defaultGroup = "resources";
    private static final List<Resource> _emptyResources = PZUnmodifiableList.wrap(new ArrayList<>());
    private final ArrayList<Resource> resources = new ArrayList<>();
    private final Map<String, Resource> idToResourceMap = new HashMap<>();
    private final ArrayList<ResourceGroup> namedGroups = new ArrayList<>();
    private final Map<String, ResourceGroup> namedGroupMap = new HashMap<>();
    private final EnumBitStore<ResourceChannel> inputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private final EnumBitStore<ResourceChannel> outputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private final ArrayList<ResourceGroup> channelGroups = new ArrayList<>();
    private final Map<ResourceChannel, ResourceGroup> inputChannelMap = new HashMap<>();
    private final Map<ResourceChannel, ResourceGroup> outputChannelMap = new HashMap<>();
    private final List<Resource> immutableResources = PZUnmodifiableList.wrap(this.resources);
    private boolean dirty;

    private Resources() {
        super(ComponentType.Resources);
    }

    boolean isDirty() {
        return this.dirty;
    }

    void setDirty() {
        if (!GameClient.client) {
            this.dirty = true;
        }
    }

    void resetDirty() {
        if (this.dirty) {
            for (int i = 0; i < this.namedGroups.size(); i++) {
                this.namedGroups.get(i).resetDirty();
            }

            for (int i = 0; i < this.resources.size(); i++) {
                this.resources.get(i).resetDirty();
            }

            this.dirty = false;
            this.sendComponentEvent(ComponentEventType.OnContentsChanged);
        }
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        ResourcesScript script = (ResourcesScript)componentScript;
        ArrayList<String> groups = script.getGroupNames();

        for (int i = 0; i < groups.size(); i++) {
            String groupName = groups.get(i);
            ArrayList<ResourceBlueprint> blueprints = script.getBlueprintGroup(groupName);

            for (int k = 0; k < blueprints.size(); k++) {
                this.createResource(groupName, blueprints.get(k));
            }
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.resetResources();
        this.dirty = false;
    }

    private void resetResources() {
        for (int i = 0; i < this.namedGroups.size(); i++) {
            ResourceGroup group = this.namedGroups.get(i);

            for (int k = 0; k < group.getResources().size(); k++) {
                Resource resource = group.getResources().get(k);
                ResourceFactory.releaseResource(resource);
            }

            ResourceGroup.release(group);
        }

        for (int i = 0; i < this.channelGroups.size(); i++) {
            ResourceGroup.release(this.channelGroups.get(i));
        }

        this.resources.clear();
        this.idToResourceMap.clear();
        this.namedGroups.clear();
        this.namedGroupMap.clear();
        this.inputChannels.clear();
        this.outputChannels.clear();
        this.channelGroups.clear();
        this.inputChannelMap.clear();
        this.outputChannelMap.clear();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    protected void onAddedToOwner() {
    }

    @Override
    protected void onRemovedFromOwner() {
        super.onRemovedFromOwner();
    }

    @Override
    protected void onConnectComponents() {
    }

    @Override
    protected void onFirstCreation() {
    }

    public List<Resource> getResources() {
        return this.immutableResources;
    }

    public ResourceGroup getResourceGroup(String name) {
        return this.namedGroupMap.get(name);
    }

    public List<Resource> getResourcesForGroup(String name) {
        ResourceGroup group = this.getResourceGroup(name);
        return group != null ? group.getResources() : null;
    }

    public void createResourceFromSerial(String blueprintSerial) {
        this.createResourceFromSerial("resources", blueprintSerial);
    }

    public void createResourceFromSerial(String groupName, String blueprintSerial) {
        try {
            ResourceBlueprint blueprint = ResourceBlueprint.Deserialize(blueprintSerial);
            this.createResource(groupName, blueprint);
            ResourceBlueprint.release(blueprint);
        } catch (Exception var4) {
            DebugLog.General.warn("ResourceBlueprint serial: " + blueprintSerial);
            var4.printStackTrace();
        }
    }

    public void createResource(ResourceBlueprint blueprint) {
        this.createResource("resources", blueprint);
    }

    public void createResource(String groupName, ResourceBlueprint blueprint) {
        Resource resource = ResourceFactory.createResource(blueprint);
        this.addResourceInternal(groupName, resource);
        if (this.isAddedToEngine()) {
        }
    }

    private void addResourceInternal(String groupName, Resource resource) {
        if (resource == null) {
            DebugLog.General.warn("unable to add resource 'null'");
        } else if (this.idToResourceMap.containsKey(resource.getId())) {
            DebugLog.General.warn("unable to add resource, duplicate ID '" + resource.getId() + "'");
        } else {
            this.resources.add(resource);
            this.idToResourceMap.put(resource.getId(), resource);
            ResourceGroup group = this.namedGroupMap.get(groupName);
            if (group == null) {
                group = ResourceGroup.alloc(groupName);
                this.namedGroups.add(group);
                this.namedGroupMap.put(groupName, group);
            }

            group.add(resource);
            this.addChannelResource(resource);
            resource.setGroup(group);
            resource.setResourcesComponent(this);
        }
    }

    public void removeResourceGroup(String groupName) {
        ResourceGroup group = this.namedGroupMap.remove(groupName);
        this.removeResourceGroup(group);
    }

    public void removeResourceGroup(ResourceGroup group) {
        this.removeResourceGroupInternal(group);
    }

    private void removeResourceGroupInternal(ResourceGroup group) {
        if (group != null) {
            if (group.size() > 0) {
                for (int i = 0; i < group.getResources().size(); i++) {
                    this.removeResourceInternal(group.getResources().get(i), false);
                }
            }

            this.namedGroups.remove(group);
            ResourceGroup.release(group);
        }
    }

    public void removeResource(String resourceID) {
        Resource resource = this.idToResourceMap.get(resourceID);
        this.removeResourceInternal(resource, true);
    }

    public void removeResource(Resource resource) {
        this.removeResourceInternal(resource, true);
    }

    private void removeResourceInternal(Resource resource, boolean removeFromGroup) {
        if (resource != null) {
            if (this.resources.remove(resource)) {
                this.idToResourceMap.remove(resource.getId());
                if (removeFromGroup) {
                    ResourceGroup group = resource.getGroup();
                    if (group.size() > 0) {
                        group.remove(resource);
                    }

                    if (group.size() == 0) {
                        this.removeResourceGroupInternal(group);
                    }
                }

                this.removeChannelResource(resource);
                resource.setGroup(null);
                resource.setResourcesComponent(null);
                ResourceFactory.releaseResource(resource);
            }
        }
    }

    private void addChannelResource(Resource resource) {
        if (resource.getChannel() != ResourceChannel.NO_CHANNEL) {
            if (resource.getIO() == ResourceIO.Input && resource.getIO() == ResourceIO.Output) {
                ResourceIO io = resource.getIO();
                Map<ResourceChannel, ResourceGroup> channelMap = io == ResourceIO.Input ? this.inputChannelMap : this.outputChannelMap;
                EnumBitStore<ResourceChannel> channels = io == ResourceIO.Input ? this.inputChannels : this.outputChannels;
                ResourceGroup group = channelMap.get(resource.getChannel());
                if (group == null) {
                    group = ResourceGroup.allocAnonymous();
                    this.channelGroups.add(group);
                    channelMap.put(resource.getChannel(), group);
                    channels.add(resource.getChannel());
                }

                group.add(resource);
            }
        }
    }

    private void removeChannelResource(Resource resource) {
        if (resource.getChannel() != ResourceChannel.NO_CHANNEL) {
            if (resource.getIO() == ResourceIO.Input && resource.getIO() == ResourceIO.Output) {
                ResourceIO io = resource.getIO();
                Map<ResourceChannel, ResourceGroup> channelMap = io == ResourceIO.Input ? this.inputChannelMap : this.outputChannelMap;
                EnumBitStore<ResourceChannel> channels = io == ResourceIO.Input ? this.inputChannels : this.outputChannels;
                ResourceGroup group = channelMap.get(resource.getChannel());
                if (group != null) {
                    group.remove(resource);
                    if (group.size() == 0) {
                        this.channelGroups.remove(group);
                        channelMap.remove(resource.getChannel());
                        channels.remove(resource.getChannel());
                        ResourceGroup.release(group);
                    }
                }
            }
        }
    }

    public Resource getResource(String name_id) {
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

    public Resource getResource(int index) {
        return index >= 0 && index < this.resources.size() ? this.resources.get(index) : null;
    }

    public int getResourceIndex(Resource resource) {
        return this.resources.indexOf(resource);
    }

    public int getResourceCount() {
        return this.resources.size();
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

    public List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceIO io) {
        return this.getResourcesFromGroup(group, list, io, ResourceType.Any, null, true);
    }

    public List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceType type) {
        return this.getResourcesFromGroup(group, list, ResourceIO.Any, type, null, true);
    }

    public List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceIO io, ResourceChannel channel) {
        return this.getResourcesFromGroup(group, list, io, ResourceType.Any, channel, true);
    }

    public List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceChannel channel) {
        return this.getResourcesFromGroup(group, list, ResourceIO.Any, ResourceType.Any, channel, true);
    }

    public List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceIO io, ResourceType type) {
        return this.getResourcesFromGroup(group, list, io, type, null, true);
    }

    private List<Resource> getResourcesFromGroup(String group, List<Resource> list, ResourceIO io, ResourceType type, ResourceChannel channel, boolean clear) {
        List<Resource> sources = _emptyResources;
        if (group != null) {
            ResourceGroup resourceGroup = this.namedGroupMap.get(group);
            if (resourceGroup == null) {
                DebugLog.General.warn("Group '" + group + "' does not exist.");
                return list;
            }

            sources = resourceGroup.getResources();
        }

        return this.getResources(sources, list, io, type, channel, clear);
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
        this.load(input, 241);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.putInt(this.namedGroups.size());

        for (int i = 0; i < this.namedGroups.size(); i++) {
            ResourceGroup group = this.namedGroups.get(i);
            GameWindow.WriteString(output, group.getName());
            output.putInt(group.size());

            for (int k = 0; k < group.getResources().size(); k++) {
                Resource resource = group.getResources().get(k);
                output.put(resource.getType().getId());
                resource.save(output);
            }
        }
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        if (!this.resources.isEmpty()) {
            this.resetResources();
        }

        int size = input.getInt();
        int resourceSize = 0;

        for (int i = 0; i < size; i++) {
            String groupName = GameWindow.ReadString(input);
            resourceSize = input.getInt();

            for (int k = 0; k < resourceSize; k++) {
                ResourceType resourceType = ResourceType.fromId(input.get());
                Resource resource = ResourceFactory.createBlancResource(resourceType);
                resource.load(input, WorldVersion);
                this.addResourceInternal(groupName, resource);
            }
        }
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    @Override
    protected void renderlast() {
    }

    @Override
    public void dumpContentsInSquare() {
        ArrayList<InventoryItem> items = new ArrayList<>();
        ArrayList<Resource> resourceItems = new ArrayList<>();
        this.getResources(resourceItems, ResourceType.Item);

        for (Resource resourceItem : resourceItems) {
            ((ResourceItem)resourceItem).removeAllItems(items);
        }

        for (InventoryItem item : items) {
            this.owner.getSquare().AddWorldInventoryItem(item, 0.0F, 0.0F, 0.0F);
        }
    }

    @Override
    public boolean isNoContainerOrEmpty() {
        ArrayList<Resource> resources = new ArrayList<>();
        this.getResources(resources, ResourceType.Item);

        for (Resource resource : resources) {
            if (!resource.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
