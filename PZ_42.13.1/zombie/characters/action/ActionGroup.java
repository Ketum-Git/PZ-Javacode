// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;

public final class ActionGroup {
    private String name;
    private String initialStateName;
    private final List<ActionState> states = new ArrayList<>();
    private final Map<String, ActionState> stateLookup = new HashMap<>();
    private final Map<Integer, String> stateNameLookup = new HashMap<>();
    private static final Map<String, ActionGroup> s_actionGroupMap = new HashMap<>();

    private void load() {
        String name = this.name;
        if (DebugLog.isEnabled(DebugType.ActionSystem)) {
            DebugLog.ActionSystem.debugln("Loading ActionGroup: " + name);
        }

        File actionGroupFile = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + name + "/actionGroup.xml");
        if (actionGroupFile.exists() && actionGroupFile.canRead()) {
            this.loadGroupData(actionGroupFile);
        }

        File folder = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + name);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File stateFolder : listOfFiles) {
                if (stateFolder.isDirectory()) {
                    ActionState state = this.getOrCreate(stateFolder.getName());
                    String statepath = stateFolder.getPath();
                    state.load(statepath);
                }
            }
        }
    }

    private void loadGroupData(File in_groupDataFile) {
        Document doc;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(in_groupDataFile);
        } catch (SAXException | IOException | ParserConfigurationException var6) {
            DebugLog.ActionSystem.error("Error loading: " + in_groupDataFile.getPath());
            var6.printStackTrace(DebugLog.ActionSystem);
            return;
        }

        doc.getDocumentElement().normalize();
        Element elem = doc.getDocumentElement();
        if (!elem.getNodeName().equals("actiongroup")) {
            DebugLog.ActionSystem
                .error("Error loading: " + in_groupDataFile.getPath() + ", expected root element '<actiongroup>', received '<" + elem.getNodeName() + ">'");
        } else {
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof Element childElem && childElem.getNodeName().equals("initial")) {
                    this.initialStateName = childElem.getTextContent().trim();
                }
            }
        }
    }

    public ActionState addState(ActionState state) {
        if (this.states.contains(state)) {
            DebugLog.ActionSystem.trace("State already added.");
            return state;
        } else {
            state.setParentActionGroup(this);
            this.states.add(state);
            this.stateLookup.put(state.getName().toLowerCase(), state);
            this.stateNameLookup.put(state.getName().hashCode(), state.getName());
            return state;
        }
    }

    public ActionState findState(String in_stateName) {
        return this.stateLookup.get(in_stateName.toLowerCase());
    }

    public ActionState getOrCreate(String in_stateName) {
        in_stateName = in_stateName.toLowerCase();
        ActionState state = this.findState(in_stateName);
        if (state == null) {
            state = this.addState(new ActionState(in_stateName));
        }

        return state;
    }

    public ActionState getInitialState() {
        ActionState state = null;
        if (this.initialStateName != null) {
            state = this.findState(this.initialStateName);
        }

        if (state == null && !this.states.isEmpty()) {
            state = this.states.get(0);
        }

        return state;
    }

    public ActionState getDefaultState() {
        return this.getInitialState();
    }

    public String getName() {
        return this.name;
    }

    public static ActionGroup getActionGroup(String in_groupName) {
        in_groupName = in_groupName.toLowerCase();
        ActionGroup grp = s_actionGroupMap.get(in_groupName);
        if (grp == null && !s_actionGroupMap.containsKey(in_groupName)) {
            grp = new ActionGroup();
            grp.name = in_groupName;
            s_actionGroupMap.put(in_groupName, grp);

            try {
                grp.load();
            } catch (Exception var3) {
                DebugLog.ActionSystem.error("Error loading action group: " + in_groupName);
                var3.printStackTrace(DebugLog.ActionSystem);
            }

            return grp;
        } else {
            return grp;
        }
    }

    public static void reloadAll() {
        for (Entry<String, ActionGroup> entry : s_actionGroupMap.entrySet()) {
            ActionGroup actionGroup = entry.getValue();

            for (ActionState state : actionGroup.states) {
                state.resetForReload();
            }

            actionGroup.load();
        }
    }
}
