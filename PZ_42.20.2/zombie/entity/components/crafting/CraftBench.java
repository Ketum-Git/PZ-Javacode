// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceChannel;
import zombie.entity.network.EntityPacketType;
import zombie.entity.util.enums.EnumBitStore;
import zombie.network.IConnection;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.crafting.CraftBenchScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;

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
    protected void reset() {
        super.reset();
        this.recipeTagQuery = null;
        this.fluidInputChannels.clear();
        this.energyInputChannels.clear();
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
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
        this.load(input, 249);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        GameWindow.WriteString(output, this.recipeTagQuery);
        this.fluidInputChannels.save(output);
        this.energyInputChannels.save(output);
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.recipeTagQuery = GameWindow.ReadString(input);
        this.fluidInputChannels.load(input);
        this.energyInputChannels.load(input);
    }
}
