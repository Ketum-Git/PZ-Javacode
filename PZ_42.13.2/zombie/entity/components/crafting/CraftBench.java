// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.ui.ObjectTooltip;

@UsedFromLua
public class CraftBench extends Component {
    private final EnumBitStore<ResourceChannel> fluidInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private final EnumBitStore<ResourceChannel> energyInputChannels = EnumBitStore.noneOf(ResourceChannel.class);
    private String recipeTagQuery;

    private CraftBench() {
        super(ComponentType.CraftBench);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        CraftBenchScript script = (CraftBenchScript)componentScript;
        this.recipeTagQuery = script.getRecipeTagQuery();
        this.fluidInputChannels.copyFrom(script.getFluidInputChannels());
        this.energyInputChannels.copyFrom(script.getEnergyInputChannels());
    }

    public EnumBitStore<ResourceChannel> getFluidInputChannels() {
        return this.fluidInputChannels;
    }

    public EnumBitStore<ResourceChannel> getEnergyInputChannels() {
        return this.energyInputChannels;
    }

    public String getRecipeTagQuery() {
        return this.recipeTagQuery;
    }

    public void setRecipeTagQuery(String recipeTagQuery) {
        if (this.recipeTagQuery == null || !this.recipeTagQuery.equalsIgnoreCase(recipeTagQuery)) {
            this.recipeTagQuery = recipeTagQuery;
        }
    }

    public List<CraftRecipe> getRecipes() {
        return CraftRecipeManager.queryRecipes(this.recipeTagQuery);
    }

    public ArrayList<Resource> getResources() {
        return new ArrayList<>();
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    @Override
    protected void renderlast() {
    }

    @Override
    protected void reset() {
        super.reset();
        this.recipeTagQuery = null;
        this.fluidInputChannels.clear();
        this.energyInputChannels.clear();
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
    }

    @Override
    protected void onConnectComponents() {
    }

    @Override
    protected void onFirstCreation() {
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
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
        GameWindow.WriteString(output, this.recipeTagQuery);
        this.fluidInputChannels.save(output);
        this.energyInputChannels.save(output);
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.recipeTagQuery = GameWindow.ReadString(input);
        this.fluidInputChannels.load(input);
        this.energyInputChannels.load(input);
    }
}
