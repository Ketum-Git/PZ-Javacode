// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.core.properties.PropertyContainer;
import zombie.debug.DebugLog;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.Item;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class GameEntityFactory {
    public static void TransferComponents(GameEntity source, GameEntity target) {
        if (source.hasComponents()) {
            try {
                if (source.hasComponent(ComponentType.SpriteConfig)) {
                    SpriteConfig spriteConfig = source.getSpriteConfig();
                    if (spriteConfig.isValidMultiSquare()) {
                        DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                        return;
                    }
                }

                if (source instanceof Moveable moveable
                    && moveable.getSpriteGrid() != null
                    && (moveable.getSpriteGrid().getWidth() > 1 || moveable.getSpriteGrid().getHeight() > 1)) {
                    DebugLog.General.warn("Cannot transfer components for multi-square objects.");
                    return;
                }

                ArrayList<Component> comps = new ArrayList<>();

                for (int i = 0; i < source.componentSize(); i++) {
                    Component component = source.getComponentForIndex(i);
                    comps.add(component);
                }

                for (int i = 0; i < comps.size(); i++) {
                    Component component = comps.get(i);
                    source.removeComponent(component);
                    if (target.hasComponent(component.getComponentType())) {
                        target.releaseComponent(component.getComponentType());
                    }

                    target.addComponent(component);
                }

                comps.clear();
                target.connectComponents();
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            }
        }
    }

    public static void TransferComponent(GameEntity source, GameEntity target, ComponentType componentType) {
        try {
            Component component = source.getComponent(componentType);
            source.removeComponent(component);
            if (target.hasComponent(componentType)) {
                target.releaseComponent(componentType);
            }

            target.addComponent(component);
            target.connectComponents();
        } catch (Exception var4) {
            ExceptionLogger.logException(var4);
        }
    }

    public static void CreateIsoEntityFromCellLoading(IsoObject isoObject) {
        PropertyContainer props = isoObject.getProperties();
        if (props != null && (props.has(IsoFlagType.EntityScript) || props.has("IsMoveAble") && props.has("CustomItem"))) {
            try {
                boolean isEntityScript = props.has(IsoFlagType.EntityScript);
                boolean isCustomItem = props.has("IsMoveAble") && props.has("CustomItem");
                if (isEntityScript && isCustomItem) {
                    DebugLog.General
                        .warn("Entity has custom item '" + props.has("CustomItem") + "' set, and entity script '" + props.get("EntityScriptName") + "' defined");
                }

                if (isCustomItem) {
                    createIsoEntityFromCustomItem(isoObject, props.get("CustomItem"), true);
                } else {
                    createEntity(isoObject, true);
                }
            } catch (Exception var4) {
                ExceptionLogger.logException(var4);
            }
        }
    }

    private static void createIsoEntityFromCustomItem(IsoObject isoObject, String customItem, boolean isFirstTimeCreated) throws Exception {
        Item scriptItem = ScriptManager.instance.FindItem(customItem);
        if (scriptItem != null) {
            createEntity(isoObject, scriptItem, isFirstTimeCreated);
        } else {
            DebugLog.General.warn("Custom '" + customItem + "' item not found.");
        }
    }

    public static void CreateInventoryItemEntity(InventoryItem inventoryItem, Item itemScript, boolean isFirstTimeCreated) {
        if (itemScript != null && itemScript.hasComponents()) {
            try {
                createEntity(inventoryItem, itemScript, isFirstTimeCreated);
            } catch (Exception var4) {
                ExceptionLogger.logException(var4);
            }
        }
    }

    public static void CreateIsoObjectEntity(IsoObject isoObject, GameEntityScript script, boolean isFirstTimeCreated) {
        try {
            createEntity(isoObject, script, isFirstTimeCreated);
        } catch (Exception var4) {
            ExceptionLogger.logException(var4);
        }
    }

    public static void CreateEntityDebugReload(GameEntity entity, GameEntityScript script, boolean isFirstTimeCreated) {
        try {
            createEntity(entity, script, isFirstTimeCreated);
        } catch (Exception var4) {
            ExceptionLogger.logException(var4);
        }
    }

    private static void createEntity(GameEntity entity, boolean isFirstTimeCreated) throws GameEntityException {
        entity = Objects.requireNonNull(entity);
        GameEntityScript script = null;
        if (entity instanceof IsoObject isoObject) {
            PropertyContainer props = isoObject.getProperties();
            if (props != null && props.has(IsoFlagType.EntityScript)) {
                String scriptKey = props.get("EntityScriptName");
                if (scriptKey != null) {
                    script = ScriptManager.instance.getGameEntityScript(scriptKey);
                }

                if (script == null) {
                    throw new GameEntityException("EntityScript not found, script: " + (scriptKey != null ? scriptKey : "unknown"), entity);
                }
            }
        } else {
            if (!(entity instanceof InventoryItem inventoryItem)) {
                if (entity instanceof VehiclePart) {
                    throw new GameEntityException("Not implemented yet");
                }

                throw new GameEntityException("Unsupported entity type.");
            }

            script = inventoryItem.getScriptItem();
        }

        if (script != null) {
            createEntity(entity, script, isFirstTimeCreated);
        }
    }

    private static void createEntity(GameEntity entity, GameEntityScript script, boolean isFirstTimeCreated) throws GameEntityException {
        entity = Objects.requireNonNull(entity);
        script = Objects.requireNonNull(script);
        if (entity.hasComponents()) {
            throw new GameEntityException("Calling CreateEntity on entity that already has components.", entity);
        } else {
            instanceComponents(entity, script);
            EntityScriptInfo scriptComponent = (EntityScriptInfo)ComponentType.Script.CreateComponent();
            scriptComponent.setOriginalScript(script);
            entity.addComponent(scriptComponent);
            entity.connectComponents();
            if (isFirstTimeCreated) {
                entity.onFirstCreation();
            }
        }
    }

    private static void instanceComponents(GameEntity entity, GameEntityScript script) {
        boolean isIso = entity.getGameEntityType() == GameEntityType.IsoObject;
        if (isIso && script.containsComponent(ComponentType.SpriteConfig)) {
            SpriteConfigScript spriteScript = script.getComponentScriptFor(ComponentType.SpriteConfig);
            SpriteConfig spriteConfig = (SpriteConfig)spriteScript.type.CreateComponentFromScript(spriteScript);
            entity.addComponent(spriteConfig);
            boolean isMaster = spriteConfig.isMultiSquareMaster();

            for (int i = 0; i < script.getComponentScripts().size(); i++) {
                ComponentScript componentScript = script.getComponentScripts().get(i);
                if (componentScript.type != ComponentType.SpriteConfig && (isMaster || !componentScript.isoMasterOnly())) {
                    Component component = componentScript.type.CreateComponentFromScript(componentScript);
                    entity.addComponent(component);
                }
            }
        } else {
            for (int ix = 0; ix < script.getComponentScripts().size(); ix++) {
                ComponentScript componentScript = script.getComponentScripts().get(ix);
                Component component = componentScript.type.CreateComponentFromScript(componentScript);
                entity.addComponent(component);
            }
        }
    }

    public static void RemoveComponentType(GameEntity entity, ComponentType componentType) {
        if (entity.hasComponent(componentType)) {
            entity.releaseComponent(componentType);
        }

        entity.connectComponents();
    }

    public static void RemoveComponentTypes(GameEntity entity, EnumSet<ComponentType> componentTypes) {
        for (ComponentType type : componentTypes) {
            if (entity.hasComponent(type)) {
                entity.releaseComponent(type);
            }
        }

        entity.connectComponents();
    }

    public static void RemoveComponent(GameEntity entity, Component component) {
        if (entity.containsComponent(component)) {
            entity.releaseComponent(component);
        }

        entity.connectComponents();
    }

    public static void RemoveComponents(GameEntity entity, Component... components) {
        if (components != null && components.length != 0) {
            for (Component component : components) {
                entity.releaseComponent(component);
            }

            entity.connectComponents();
        }
    }

    public static void AddComponent(GameEntity entity, Component component) {
        AddComponent(entity, true, component);
    }

    public static void AddComponents(GameEntity entity, Component... components) {
        AddComponents(entity, true, components);
    }

    public static void AddComponent(GameEntity entity, boolean replace, Component component) {
        if (entity.hasComponent(component.getComponentType())) {
            if (!replace) {
                return;
            }

            entity.releaseComponent(component.getComponentType());
        }

        entity.addComponent(component);
        entity.connectComponents();
    }

    public static void AddComponents(GameEntity entity, boolean replace, Component... components) {
        if (components != null && components.length != 0) {
            for (Component component : components) {
                if (entity.hasComponent(component.getComponentType())) {
                    if (!replace) {
                        continue;
                    }

                    entity.releaseComponent(component.getComponentType());
                }

                entity.addComponent(component);
            }

            entity.connectComponents();
        }
    }
}
