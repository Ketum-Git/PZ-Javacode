// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import org.w3c.dom.Element;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableEnumParser;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSourceContainer;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.PZForEeachElementXmlParseException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ActionState implements IAnimationVariableSourceContainer {
    private final String name;
    public final ArrayList<ActionTransition> transitions = new ArrayList<>();
    private String[] tags;
    private String[] childTags;
    private ActionGroup parentActionGroup;
    private boolean isGrapplerState;
    private final AnimationVariableSource stateVariables = new AnimationVariableSource();
    private static final Comparator<ActionTransition> transitionComparator = (lhs, rhs) -> rhs.conditionPriority != lhs.conditionPriority
        ? rhs.conditionPriority - lhs.conditionPriority
        : rhs.conditions.size() - lhs.conditions.size();

    public ActionState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getName()).append("\r\n");
        result.append("{").append("\r\n");
        result.append("\t").append("name:").append(this.name).append("\r\n");
        result.append("\t").append("transitions:").append("\r\n");
        result.append("\t{").append("\r\n");

        for (int itransition = 0; itransition < this.transitions.size(); itransition++) {
            result.append(this.transitions.get(itransition).toString("\t")).append(",").append("\r\n");
        }

        result.append("\t}").append("\r\n");
        result.append("\t").append("variables:").append("\r\n");
        result.append("\t{").append("\r\n");

        for (IAnimationVariableSlot slot : this.stateVariables.getGameVariables()) {
            result.append(slot.getKey()).append(":").append(slot.getValueString()).append(",").append("\r\n");
        }

        result.append("\t}").append("\r\n");
        result.append("}");
        return result.toString();
    }

    public final boolean canHaveSubStates() {
        return !PZArrayUtil.isNullOrEmpty(this.childTags);
    }

    public final boolean canBeSubstate() {
        return !PZArrayUtil.isNullOrEmpty(this.tags);
    }

    public final boolean canHaveSubState(ActionState child) {
        return canHaveSubState(this, child);
    }

    public boolean isGrapplerState() {
        return this.isGrapplerState;
    }

    /**
     * Returns TRUE if the supplied child state can be a child of this state.
     *  To determine this, the parent's childStateTags are compared to the child's parentStateTags.
     *  If there is an overlap, the child is compatible with the parent.
     */
    public static boolean canHaveSubState(ActionState parent, ActionState child) {
        if (parent == child) {
            return false;
        } else {
            String[] parentTags = parent.childTags;
            String[] childTags = child.tags;
            return tagsOverlap(parentTags, childTags);
        }
    }

    public static boolean tagsOverlap(String[] parentTags, String[] childTags) {
        if (PZArrayUtil.isNullOrEmpty(parentTags)) {
            return false;
        } else if (PZArrayUtil.isNullOrEmpty(childTags)) {
            return false;
        } else {
            boolean overlapped = false;

            for (int parentIdx = 0; parentIdx < parentTags.length; parentIdx++) {
                String parentTag = parentTags[parentIdx];

                for (int childIdx = 0; childIdx < childTags.length; childIdx++) {
                    String childTag = childTags[childIdx];
                    if (StringUtils.equalsIgnoreCase(parentTag, childTag)) {
                        overlapped = true;
                        break;
                    }
                }
            }

            return overlapped;
        }
    }

    public String getName() {
        return this.name;
    }

    public void load(String stateFolderPath) {
        File folder = new File(stateFolderPath).getAbsoluteFile();
        File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (listOfFiles != null) {
            Collator collator = Collator.getInstance(Locale.ENGLISH);
            Arrays.sort(listOfFiles, (o1, o2) -> collator.compare(o1.getName(), o2.getName()));

            for (File file : listOfFiles) {
                this.parse(file);
            }

            this.sortTransitions();
        }
    }

    public void parse(File file) {
        ArrayList<ActionTransition> loadedTransitions = new ArrayList<>();
        ArrayList<String> tags = new ArrayList<>();
        ArrayList<String> childTags = new ArrayList<>();
        String filePath = file.getPath();

        try {
            Element root = PZXmlUtil.parseXml(filePath);
            if (root.getNodeName().equals("ActionState")) {
                this.parseActionState(root);
                return;
            }

            if (ActionTransition.parse(root, filePath, loadedTransitions)) {
                for (int i = 0; i < loadedTransitions.size(); i++) {
                    if (loadedTransitions.get(i).transitionTo != null && loadedTransitions.get(i).transitionTo.equals(this.getName())) {
                        DebugLog.ActionSystem
                            .warn(
                                "Canceled loading wrong transition from %s to %s in file %s",
                                this.getName(),
                                loadedTransitions.get(i).transitionTo,
                                file.getName()
                            );
                        loadedTransitions.remove(i--);
                    }
                }

                this.transitions.addAll(loadedTransitions);
                if (DebugLog.isEnabled(DebugType.ActionSystem)) {
                    DebugLog.ActionSystem.debugln("Loaded transitions from file: %s", file.getName());
                }

                return;
            }

            if (this.parseTags(root, tags, childTags)) {
                this.tags = PZArrayUtil.concat(this.tags, tags.toArray(new String[0]));
                this.childTags = PZArrayUtil.concat(this.childTags, childTags.toArray(new String[0]));
                if (DebugLog.isEnabled(DebugType.ActionSystem)) {
                    DebugLog.ActionSystem.debugln("Loaded tags from file: %s", filePath);
                }

                return;
            }

            if (DebugLog.isEnabled(DebugType.ActionSystem)) {
                DebugLog.ActionSystem.warn("Unrecognized xml file. It does not appear to be a transition nor a tag(s). %s", filePath);
            }
        } catch (Exception var8) {
            DebugLog.ActionSystem.error("Error loading: " + filePath);
            DebugLog.ActionSystem.error(var8);
        }
    }

    private void parseActionState(Element root) {
        try {
            PZXmlUtil.forEachElement(root, child -> {
                String xmlTag = child.getNodeName();
                if ("isGrapplerState".equalsIgnoreCase(xmlTag)) {
                    this.isGrapplerState = StringUtils.tryParseBoolean(child.getTextContent());
                }

                if ("variables".equalsIgnoreCase(xmlTag)) {
                    this.parseStateVariables(child);
                }
            });
        } catch (PZForEeachElementXmlParseException var3) {
            DebugLog.ActionSystem.printException(var3, "Exception thrown parsing ActionState.", LogSeverity.Error);
        }
    }

    private void parseStateVariables(Element root) {
        PZXmlUtil.forEachElement(
            root,
            child -> {
                String xmlTag = child.getNodeName();
                if ("var".equalsIgnoreCase(xmlTag)) {
                    String varKey = child.getAttribute("key");
                    if (StringUtils.isNullOrWhitespace(varKey)) {
                        DebugLog.ActionSystem
                            .warn("Could not parse ActionState variable: %s. Missing 'key' attribute.", PZXmlUtil.elementToPrettyStringSafe(child));
                        return;
                    }

                    String varType = child.getAttribute("type");
                    String varValue = child.getTextContent();
                    if (StringUtils.isNullOrWhitespace(varType) || "string".equalsIgnoreCase(varType)) {
                        this.stateVariables.setVariable(varKey, varValue);
                    } else if ("bool".equalsIgnoreCase(varType)) {
                        this.stateVariables.setVariable(varKey, StringUtils.tryParseBoolean(varValue));
                    } else if ("float".equalsIgnoreCase(varType)) {
                        this.stateVariables.setVariable(varKey, StringUtils.tryParseFloat(varValue));
                    } else if ("Enum".equalsIgnoreCase(varType)) {
                        String enumType = child.getAttribute("enumType");
                        if (StringUtils.isNullOrWhitespace(enumType)) {
                            DebugLog.ActionSystem
                                .warn("Could not parse ActionState variable: %s. Missing 'enumType' attribute.", PZXmlUtil.elementToPrettyStringSafe(child));
                            return;
                        }

                        this.stateVariables.setVariableEnum(varKey, AnimationVariableEnumParser.tryParse(enumType, varValue));
                    }
                }
            }
        );

        for (IAnimationVariableSlot slot : this.stateVariables.getGameVariables()) {
            slot.setReadOnly(true);
        }
    }

    private boolean parseTags(Element root, ArrayList<String> out_tags, ArrayList<String> out_childTags) {
        out_tags.clear();
        out_childTags.clear();
        if (root.getNodeName().equals("tags")) {
            PZXmlUtil.forEachElement(root, child -> {
                if (child.getNodeName().equals("tag")) {
                    out_tags.add(child.getTextContent());
                }
            });
            return true;
        } else if (root.getNodeName().equals("childTags")) {
            PZXmlUtil.forEachElement(root, child -> {
                if (child.getNodeName().equals("tag")) {
                    out_childTags.add(child.getTextContent());
                }
            });
            return true;
        } else {
            return false;
        }
    }

    public void sortTransitions() {
        this.transitions.sort(transitionComparator);
    }

    public void resetForReload() {
        this.transitions.clear();
        this.tags = null;
        this.childTags = null;
        this.stateVariables.removeAllVariables();
    }

    public void setParentActionGroup(ActionGroup in_parentActionGroup) {
        this.parentActionGroup = in_parentActionGroup;
    }

    public ActionGroup getParentActionGroup() {
        return this.parentActionGroup;
    }

    @Override
    public IAnimationVariableSource getGameVariablesInternal() {
        return this.stateVariables;
    }

    public boolean hasStateVariables() {
        return !this.stateVariables.isEmpty();
    }
}
