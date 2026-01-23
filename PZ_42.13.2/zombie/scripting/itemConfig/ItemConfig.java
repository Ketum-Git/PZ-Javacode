// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeUtil;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.ItemConfigurator;
import zombie.inventory.ItemPickInfo;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.generators.GeneratorBoolAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorEnumStringSetAttribute;
import zombie.scripting.itemConfig.generators.GeneratorFluidContainer;
import zombie.scripting.itemConfig.generators.GeneratorLuaFunc;
import zombie.scripting.itemConfig.generators.GeneratorNumericAttribute;
import zombie.scripting.itemConfig.generators.GeneratorStringAttribute;
import zombie.scripting.itemConfig.script.BucketRootScript;
import zombie.scripting.itemConfig.script.SelectorBucketScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class ItemConfig extends BaseScriptObject {
    public static String errorLine;
    public static String errorBucket;
    public static String errorRoot;
    public static String errorItemConfig;
    public static final String VARIABLE_PREFIX = "$";
    private final ArrayList<String> includes = new ArrayList<>();
    private final HashMap<String, String> variables = new HashMap<>();
    private final HashMap<String, BucketRootScript> rootScripts = new HashMap<>();
    private final ArrayList<BucketRoot> roots = new ArrayList<>();
    private String name;
    private boolean hasBeenParsed;
    private boolean isValid = true;
    private static final ArrayList<RandomGenerator> tempGenerators = new ArrayList<>();

    private static String createErrorString() {
        String e = "[";
        e = e + "itemConfig=" + (errorItemConfig != null ? errorItemConfig : "unknown") + ", ";
        if (errorBucket != null) {
            e = e + "bucket=" + errorBucket + ", ";
        }

        if (errorRoot != null) {
            e = e + "attribute=" + errorRoot + ", ";
        }

        e = e + "line=\"" + (errorLine != null ? errorLine : "null") + "\"]";
        return e + "]";
    }

    private static void WarnOrError(String s) throws ItemConfig.ItemConfigException {
        if (Core.debug) {
            throw new ItemConfig.ItemConfigException(s);
        } else {
            DebugLog.log("RecipeAttributes -> " + s + " \n" + createErrorString());
        }
    }

    public ItemConfig() {
        super(ScriptType.ItemConfig);
    }

    public String getName() {
        return this.name;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void ConfigureEntitySpawned(GameEntity entity, ItemPickInfo pickInfo) {
        if (!this.roots.isEmpty() && this.isValid) {
            for (int i = 0; i < this.roots.size(); i++) {
                BucketRoot root = this.roots.get(i);
                if (root.getBucketSpawn() != null) {
                    root.getBucketSpawn().Resolve(entity, pickInfo);
                }
            }
        }
    }

    public void ConfigureEntityOnCreate(GameEntity entity) {
        if (!this.roots.isEmpty() && this.isValid) {
            for (int i = 0; i < this.roots.size(); i++) {
                BucketRoot root = this.roots.get(i);
                if (root.getBucketOnCreate() != null) {
                    root.getBucketOnCreate().ResolveOnCreate(entity);
                }
            }
        }
    }

    @Override
    public void Load(String name, String totalFile) throws ItemConfig.ItemConfigException {
        this.name = name;
        errorLine = null;
        errorBucket = null;
        errorRoot = null;
        errorItemConfig = this.name;

        try {
            ScriptParser.Block block = ScriptParser.parse(totalFile);
            block = block.children.get(0);
            this.LoadCommonBlock(block);

            for (ScriptParser.BlockElement element : block.elements) {
                if (element.asValue() != null) {
                    String s = element.asValue().string;
                    errorLine = s;
                    if (!StringUtils.isNullOrWhitespace(s)) {
                        if (s.contains("=")) {
                            String[] split = s.split("=");
                            String k = split[0].trim();
                            String v = split[1].trim();
                            if (k.equalsIgnoreCase("include")) {
                                this.includes.add(v);
                            }
                        }

                        errorLine = null;
                    }
                } else {
                    ScriptParser.Block child = element.asBlock();
                    if ("includes".equalsIgnoreCase(child.type)) {
                        for (ScriptParser.Value value : child.values) {
                            String s = value.string;
                            if (!StringUtils.isNullOrWhitespace(s)) {
                                this.includes.add(s.trim());
                            }
                        }
                    } else if ("variables".equalsIgnoreCase(child.type)) {
                        for (ScriptParser.Value valuex : child.values) {
                            String s = valuex.string;
                            if (!StringUtils.isNullOrWhitespace(s)) {
                                this.variables.put(valuex.getKey(), valuex.getValue());
                            }
                        }
                    } else {
                        BucketRootScript rootScript = BucketRootScript.TryLoad(child);
                        if (rootScript != null) {
                            String rootId = rootScript.getType().toString();
                            if (rootScript.getId() != null) {
                                rootId = rootId + ":" + rootScript.getId();
                            }

                            this.rootScripts.put(rootId, rootScript);
                        }
                    }
                }
            }
        } catch (Exception var10) {
            if (!(var10 instanceof ItemConfig.ItemConfigException)) {
                throw new ItemConfig.ItemConfigException(var10.getMessage(), var10);
            }

            throw new ItemConfig.ItemConfigException(var10.getMessage(), var10, false);
        }

        errorLine = null;
        errorBucket = null;
        errorRoot = null;
        errorItemConfig = null;
    }

    @Override
    public void PreReload() {
        this.hasBeenParsed = false;
        this.includes.clear();
        this.variables.clear();
        this.rootScripts.clear();
        this.roots.clear();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        errorItemConfig = this.name;

        try {
            this.Parse(null);
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
            throw new Exception(var3);
        }

        errorItemConfig = null;
    }

    private void Parse(HashSet<String> includeSet) throws ItemConfig.ItemConfigException {
        if (includeSet != null) {
            if (includeSet.contains(this.name)) {
                throw new ItemConfig.ItemConfigException("Circular includes detected.");
            }

            includeSet.add(this.name);
        }

        if (!this.hasBeenParsed) {
            HashMap<String, BucketRootScript> mergedIncludes = null;
            HashMap<String, String> mergedVariables = new HashMap<>();
            if (this.includes != null) {
                for (String include : this.includes) {
                    HashSet<String> set = includeSet != null ? includeSet : new HashSet<>();
                    set.add(this.name);
                    ItemConfig parent = ScriptManager.instance.getItemConfig(include);
                    if (!parent.hasBeenParsed) {
                        parent.Parse(set);
                    }

                    mergedIncludes = MergeRoots(mergedIncludes, parent.rootScripts, true);

                    for (Entry<String, String> entry : parent.variables.entrySet()) {
                        mergedVariables.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            if (mergedIncludes != null) {
                MergeRoots(mergedIncludes, this.rootScripts, true);
            }

            for (Entry<String, String> entry : mergedVariables.entrySet()) {
                if (!this.variables.containsKey(entry.getKey())) {
                    mergedVariables.put(entry.getKey(), entry.getValue());
                }
            }

            this.hasBeenParsed = true;
        }
    }

    private static HashMap<String, BucketRootScript> MergeRoots(
        HashMap<String, BucketRootScript> bucket, HashMap<String, BucketRootScript> bucketAdd, boolean createCopyEntries
    ) {
        HashMap<String, BucketRootScript> merged = new HashMap<>();
        if (bucket == null && bucketAdd == null) {
            return merged;
        } else {
            if (bucket != null && bucketAdd != null) {
                for (Entry<String, BucketRootScript> entry : bucket.entrySet()) {
                    if (!bucketAdd.containsKey(entry.getKey())) {
                        merged.put(entry.getKey(), createCopyEntries ? entry.getValue().copy() : entry.getValue());
                    }
                }

                for (Entry<String, BucketRootScript> entryx : bucketAdd.entrySet()) {
                    merged.put(entryx.getKey(), createCopyEntries ? entryx.getValue().copy() : entryx.getValue());
                }
            } else {
                HashMap<String, BucketRootScript> from = bucket != null ? bucket : bucketAdd;

                for (Entry<String, BucketRootScript> entryx : from.entrySet()) {
                    merged.put(entryx.getKey(), createCopyEntries ? entryx.getValue().copy() : entryx.getValue());
                }
            }

            return merged;
        }
    }

    public void BuildBuckets() {
        errorItemConfig = this.name;

        try {
            this.roots.clear();

            for (Entry<String, BucketRootScript> entry : this.rootScripts.entrySet()) {
                BucketRoot bucketRoot = this.buildBucketRoot(entry.getValue());
                this.roots.add(bucketRoot);
            }
        } catch (Exception var4) {
            DebugLog.log(var4.getMessage());
            var4.printStackTrace();
            this.isValid = false;
        }

        VariableBuilder.clear();
        errorItemConfig = null;
    }

    private BucketRoot buildBucketRoot(BucketRootScript script) throws ItemConfig.ItemConfigException {
        VariableBuilder.setKeys(this.variables);
        if (script.idIsVariable()) {
            String id = VariableBuilder.Build(script.getId());
            script.setId(id);
        }

        BucketRoot root = new BucketRoot(script.getType(), script.getId());
        SelectorBucket spawn = this.buildBucket(script, script.getDefaultBucket());
        root.setBucketSpawn(spawn);
        if (script.getOnCreateBucket() != null) {
            SelectorBucket create = this.buildBucket(script, script.getOnCreateBucket());
            root.setBucketOnCreate(create);
        }

        return root;
    }

    private SelectorBucket buildBucket(BucketRootScript rootScript, SelectorBucketScript bucketScript) throws ItemConfig.ItemConfigException {
        SelectorBucket[] children = null;
        if (!bucketScript.getChildren().isEmpty()) {
            children = new SelectorBucket[bucketScript.getChildren().size()];

            for (int i = 0; i < bucketScript.getChildren().size(); i++) {
                children[i] = this.buildBucket(rootScript, bucketScript.getChildren().get(i));
            }
        }

        int[] selectorIDs = null;
        if (bucketScript.getSelectorString() != null) {
            String selector = bucketScript.getSelectorString();
            if (selector.contains("$")) {
                selector = VariableBuilder.Build(selector);
            }

            String[] ids = null;
            if (bucketScript.getSelectorType().isAllowChaining() && selector.contains("/")) {
                ids = selector.split("/");

                for (int i = 0; i < ids.length; i++) {
                    ids[i] = ids[i].trim();
                }
            } else {
                ids = new String[]{selector};
            }

            selectorIDs = new int[ids.length];

            for (int i = 0; i < ids.length; i++) {
                if (bucketScript.getSelectorType() == SelectorType.Tile) {
                    selectorIDs[i] = ItemConfigurator.GetIdForSprite(ids[i]);
                } else {
                    selectorIDs[i] = ItemConfigurator.GetIdForString(ids[i]);
                }

                if (selectorIDs[i] == -1) {
                    throw new ItemConfig.ItemConfigException("Could not find selectorID for: " + ids[i] + ", in: " + bucketScript.getSelectorString());
                }
            }
        }

        Randomizer randomizer = null;
        if (!bucketScript.getRandomizers().isEmpty()) {
            tempGenerators.clear();

            for (String rand : bucketScript.getRandomizers()) {
                String s = rand;
                if (!StringUtils.isNullOrWhitespace(rand)) {
                    if (rand.contains("$")) {
                        s = VariableBuilder.Build(rand);
                    }

                    if (!StringUtils.isNullOrWhitespace(s)) {
                        errorLine = s;
                        RandomGenerator generator = null;
                        switch (rootScript.getType()) {
                            case Attribute:
                                AttributeType type = Attribute.TypeFromName(rootScript.getId());
                                if (type == null) {
                                    throw new ItemConfig.ItemConfigException(
                                        "Invalid attribute! [itemConfig="
                                            + this.name
                                            + ", attribute="
                                            + (type != null ? type : "null")
                                            + ", attributeString = "
                                            + (rootScript.getId() != null ? rootScript.getId() : "null")
                                            + "]"
                                    );
                                }

                                if (AttributeValueType.IsNumeric(type.getValueType())) {
                                    generator = this.buildNumericGenerator(type, s);
                                } else if (type.getValueType() == AttributeValueType.Boolean) {
                                    generator = this.buildBoolGenerator(type, s);
                                } else if (type.getValueType() == AttributeValueType.String) {
                                    generator = this.buildStringGenerator(type, s);
                                } else if (type.getValueType() == AttributeValueType.Enum) {
                                    generator = this.buildEnumGenerator(type, s);
                                } else if (type.getValueType() == AttributeValueType.EnumSet) {
                                    generator = this.buildEnumSetGenerator(type, s);
                                } else if (type.getValueType() == AttributeValueType.EnumStringSet) {
                                    generator = this.buildEnumStringSetGenerator(type, s);
                                }
                                break;
                            case FluidContainer:
                                generator = this.buildFluidContainerGenerator(rootScript.getId(), s);
                                break;
                            case LuaFunc:
                                generator = this.buildLuaFuncGenerator(s);
                        }

                        if (generator != null) {
                            tempGenerators.add(generator);
                        }

                        errorLine = null;
                    }
                }
            }

            randomizer = new Randomizer(PZArrayUtil.toArray(tempGenerators));
        }

        return new SelectorBucket(selectorIDs, bucketScript, children, randomizer);
    }

    private RandomGenerator buildLuaFuncGenerator(String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        String val = null;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("func")) {
                val = elems[1];
            }
        }

        if (val == null) {
            throw new ItemConfig.ItemConfigException("At least parameter 'func' has to be defined.");
        } else {
            return new GeneratorLuaFunc(val, chance);
        }
    }

    private RandomGenerator buildFluidContainerGenerator(String containerID, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        float min = 0.0F;
        float max = 0.0F;
        boolean hasMax = false;
        ArrayList<Fluid> fluids = new ArrayList<>();
        ArrayList<Float> ratios = new ArrayList<>();
        float val = 0.0F;
        boolean hasVal = false;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("min")) {
                min = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("max")) {
                hasMax = true;
                max = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                hasVal = true;
                val = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("fluid")) {
                String[] fl = elems[1].split(":");
                Fluid fluid = Fluid.Get(fl[0]);
                float ratio = Float.parseFloat(fl[1]);
                if (fluid == null) {
                    throw new ItemConfig.ItemConfigException("Could not find fluid: '" + fl[0] + "'.");
                }

                fluids.add(fluid);
                ratios.add(ratio);
            }
        }

        if (!hasMax && !hasVal) {
            throw new ItemConfig.ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
        } else {
            if (hasVal) {
                min = val;
                max = val;
            }

            if (fluids.isEmpty()) {
                return new GeneratorFluidContainer(containerID, null, null, chance, min, max);
            } else {
                float[] ratiosArr = new float[ratios.size()];

                for (int i = 0; i < ratios.size(); i++) {
                    ratiosArr[i] = ratios.get(i);
                }

                return new GeneratorFluidContainer(containerID, PZArrayUtil.toArray(fluids), ratiosArr, chance, min, max);
            }
        }
    }

    private RandomGenerator buildNumericGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        float min = 0.0F;
        float max = 0.0F;
        boolean hasMax = false;
        float val = 0.0F;
        boolean hasVal = false;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("min")) {
                min = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("max")) {
                hasMax = true;
                max = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                hasVal = true;
                val = Float.parseFloat(elems[1]);
            }
        }

        if (!hasMax && !hasVal) {
            throw new ItemConfig.ItemConfigException("At least one of these parameters: 'max' or 'value', has to be defined.");
        } else {
            if (hasVal) {
                min = val;
                max = val;
            }

            return new GeneratorNumericAttribute(attributeType, chance, min, max);
        }
    }

    private RandomGenerator buildStringGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        String val = null;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                val = elems[1];
            }
        }

        if (val == null) {
            throw new ItemConfig.ItemConfigException("At least parameter 'value' has to be defined.");
        } else {
            return new GeneratorStringAttribute(attributeType, chance, val);
        }
    }

    private RandomGenerator buildBoolGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        boolean hasVal = false;
        boolean val = false;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                val = Boolean.parseBoolean(elems[1]);
                hasVal = true;
            }
        }

        if (!hasVal) {
            throw new ItemConfig.ItemConfigException("At least parameter 'value' has to be defined.");
        } else {
            return new GeneratorBoolAttribute(attributeType, chance, val);
        }
    }

    private RandomGenerator buildEnumGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        String val = null;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                val = elems[1];
            }
        }

        if (val == null) {
            throw new ItemConfig.ItemConfigException("At least parameter 'value' has to be defined.");
        } else {
            return new GeneratorEnumAttribute(attributeType, chance, val);
        }
    }

    private RandomGenerator buildEnumSetGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        String val = null;
        GeneratorEnumSetAttribute.Mode mode = GeneratorEnumSetAttribute.Mode.Set;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                val = elems[1];
            } else if (id.equalsIgnoreCase("mode")) {
                if (elems[1].equalsIgnoreCase("add")) {
                    mode = GeneratorEnumSetAttribute.Mode.Add;
                } else if (elems[1].equalsIgnoreCase("remove")) {
                    mode = GeneratorEnumSetAttribute.Mode.Remove;
                }
            }
        }

        if (val == null) {
            throw new ItemConfig.ItemConfigException("At least parameter 'value' has to be defined.");
        } else {
            String[] values;
            if (val.contains(";")) {
                values = val.split(";");
            } else {
                values = new String[]{val};
            }

            return new GeneratorEnumSetAttribute(attributeType, mode, chance, values);
        }
    }

    private RandomGenerator buildEnumStringSetGenerator(AttributeType attributeType, String s) throws ItemConfig.ItemConfigException {
        String[] params = s.split("\\s+");
        float chance = 1.0F;
        String val = null;
        GeneratorEnumStringSetAttribute.Mode mode = GeneratorEnumStringSetAttribute.Mode.Set;

        for (String param : params) {
            String[] elems = param.split("=");
            String id = elems[0];
            if (id.equalsIgnoreCase("chance")) {
                chance = Float.parseFloat(elems[1]);
            } else if (id.equalsIgnoreCase("value")) {
                val = elems[1];
            } else if (id.equalsIgnoreCase("mode")) {
                if (elems[1].equalsIgnoreCase("add")) {
                    mode = GeneratorEnumStringSetAttribute.Mode.Add;
                } else if (elems[1].equalsIgnoreCase("remove")) {
                    mode = GeneratorEnumStringSetAttribute.Mode.Remove;
                }
            }
        }

        if (val == null) {
            throw new ItemConfig.ItemConfigException("At least parameter 'value' has to be defined.");
        } else {
            String[] valuesEnum = null;
            String[] valuesString = null;
            ArrayList<String> enums = new ArrayList<>();
            ArrayList<String> strings = new ArrayList<>();
            if (val.contains(";")) {
                String[] split = val.split(";");

                for (String str : split) {
                    if (AttributeUtil.isEnumString(str)) {
                        enums.add(str);
                    } else {
                        strings.add(str);
                    }
                }
            } else if (AttributeUtil.isEnumString(val)) {
                enums.add(val);
            } else {
                strings.add(val);
            }

            if (!enums.isEmpty()) {
                valuesEnum = enums.toArray(new String[0]);
            }

            if (!strings.isEmpty()) {
                valuesString = strings.toArray(new String[0]);
            }

            return new GeneratorEnumStringSetAttribute(attributeType, mode, chance, valuesEnum, valuesString);
        }
    }

    public static class ItemConfigException extends Exception {
        public ItemConfigException(String errorMessage) {
            super("RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString());
        }

        public ItemConfigException(String errorMessage, Throwable err) {
            super("RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString(), err);
        }

        public ItemConfigException(String errorMessage, Throwable err, boolean doPrint) {
            super(doPrint ? "RecipeAttributes -> " + errorMessage + " \n" + ItemConfig.createErrorString() : errorMessage, err);
        }
    }
}
