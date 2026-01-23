// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import zombie.debug.DebugLog;
import zombie.entity.util.BitSet;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.list.PZUnmodifiableList;

public class TaggedObjectManager<T extends TaggedObjectManager.TaggedObject> {
    private static final String defaultTag = "untagged".toLowerCase();
    private final HashMap<String, Integer> tagStringToIndexMap = new HashMap<>();
    private final HashMap<Integer, String> tagIndexToStringMap = new HashMap<>();
    private final HashMap<String, List<T>> tagToObjectListMap = new HashMap<>();
    private final List<String> registeredTags = new ArrayList<>();
    private final List<String> registeredTagsView = PZUnmodifiableList.wrap(this.registeredTags);
    private final HashMap<String, TaggedObjectManager.TagGroup<T>> tagGroupMap = new HashMap<>();
    private final List<TaggedObjectManager.TagGroup<T>> tagGroups = new ArrayList<>();
    private final HashMap<String, String> tagsStringAliasMap = new HashMap<>();
    private final List<T> emptyTagObjects = new ArrayList<>();
    private final List<String> tempStringList = new ArrayList<>();
    private final TaggedObjectManager.BackingListProvider<T> backingListProvider;
    private boolean verbose;
    private boolean warnNonPreprocessedNewTag = true;

    public TaggedObjectManager(TaggedObjectManager.BackingListProvider<T> backingListProvider) {
        this.backingListProvider = Objects.requireNonNull(backingListProvider);
        this.registerTag(defaultTag, false);
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setWarnNonPreprocessedNewTag(boolean b) {
        this.warnNonPreprocessedNewTag = b;
    }

    public boolean isWarnNonPreprocessedNewTag() {
        return this.warnNonPreprocessedNewTag;
    }

    public void clear() {
        this.tagStringToIndexMap.clear();
        this.tagIndexToStringMap.clear();
        this.registeredTags.clear();
        this.tagGroupMap.clear();
        this.tagGroups.clear();
        this.tagsStringAliasMap.clear();
    }

    public void setDirty() {
        for (int i = 0; i < this.tagGroups.size(); i++) {
            this.tagGroups.get(i).dirty = true;
        }
    }

    public List<String> getRegisteredTags() {
        return this.registeredTagsView;
    }

    public void getRegisteredTagGroups(ArrayList<String> list) {
        for (Entry<String, TaggedObjectManager.TagGroup<T>> entry : this.tagGroupMap.entrySet()) {
            list.add(entry.getKey());
        }
    }

    public void registerObjectsFromBackingList() {
        this.registerObjectsFromBackingList(false);
    }

    public void registerObjectsFromBackingList(boolean clear) {
        if (this.verbose) {
            DebugLog.General.println("Registering objects from backing list...");
        }

        if (clear) {
            this.clear();
            this.registerTag(defaultTag, false);
        }

        List<T> list = this.backingListProvider.getTaggedObjectList();

        for (int i = 0; i < list.size(); i++) {
            T taggedObject = list.get(i);
            this.registerObject(taggedObject, false);
        }
    }

    public void registerObject(T taggedObject, boolean bSetDirty) {
        if (this.verbose) {
            DebugLog.General.println("register tagged object: " + taggedObject);
        }

        taggedObject.getTagBits().clear();
        List<String> tags = taggedObject.getTags();

        for (int i = 0; i < tags.size(); i++) {
            this.registerTag(tags.get(i), bSetDirty);
        }

        if (tags.isEmpty()) {
            int categoryIndex = this.tagStringToIndexMap.get(defaultTag);
            taggedObject.getTagBits().set(categoryIndex);
            this.tagToObjectListMap.get(defaultTag).add(taggedObject);
        } else {
            for (int i = 0; i < tags.size(); i++) {
                String tag = this.sanitizeTag(tags.get(i));
                int categoryIndex = this.tagStringToIndexMap.get(tag);
                taggedObject.getTagBits().set(categoryIndex);
                this.tagToObjectListMap.get(tag).add(taggedObject);
            }
        }

        if (bSetDirty) {
            this.setDirty();
        }
    }

    private String sanitizeTag(String tag) {
        return tag != null ? tag.trim().toLowerCase() : tag;
    }

    private int registerTag(String tag, boolean bSetDirty) {
        tag = this.sanitizeTag(tag);
        if (!this.registeredTags.contains(tag)) {
            if (this.verbose) {
                DebugLog.General.println("register new tag: " + tag);
            }

            int bitIndex = this.registeredTags.size() + 1;
            this.registeredTags.add(tag);
            this.tagStringToIndexMap.put(tag, bitIndex);
            this.tagIndexToStringMap.put(bitIndex, tag);
            this.tagToObjectListMap.put(tag, new ArrayList<>());
            if (bSetDirty) {
                this.setDirty();
            }

            return bitIndex;
        } else {
            return this.registeredTags.indexOf(tag) + 1;
        }
    }

    public List<T> getListForTag(String tag) {
        List<T> taggedObjects = this.tagToObjectListMap.get(this.sanitizeTag(tag));
        return taggedObjects != null ? taggedObjects : this.emptyTagObjects;
    }

    public List<T> getListForTag(int tagBitIndex) {
        String tag = this.tagIndexToStringMap.get(tagBitIndex);
        return tag != null ? this.getListForTag(tag) : this.emptyTagObjects;
    }

    public List<T> queryTaggedObjects(String tagQueryString) {
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            DebugLog.General.warn("manager-> returning empty list for: " + tagQueryString);
            return this.emptyTagObjects;
        } else {
            TaggedObjectManager.TagGroup<T> tagGroup = this.tagGroupMap.get(tagQueryString);
            if (tagGroup != null) {
                if (this.verbose) {
                    DebugLog.General.println("manager-> returning cached list for: " + tagQueryString);
                }

                return tagGroup.getUpdatedClientView();
            } else {
                String alias = this.tagsStringAliasMap.get(tagQueryString);
                if (alias != null) {
                    tagGroup = this.tagGroupMap.get(alias);
                    if (tagGroup != null) {
                        if (this.verbose) {
                            DebugLog.General.println("manager-> returning cached list for alias '" + alias + "', cache: " + tagQueryString);
                        }

                        return tagGroup.getUpdatedClientView();
                    }
                }

                String formattedQueryStr = this.formatQueryString(tagQueryString);
                tagGroup = this.tagGroupMap.get(formattedQueryStr);
                if (tagGroup != null) {
                    if (this.verbose) {
                        DebugLog.General.println("manager-> created new alias '" + tagQueryString + "' for: " + formattedQueryStr);
                    }

                    this.tagsStringAliasMap.put(tagQueryString, formattedQueryStr);
                    return tagGroup.getUpdatedClientView();
                } else {
                    String[] whitelist = this.readWhitelist(formattedQueryStr);
                    String[] blacklist = this.readBlackList(formattedQueryStr);
                    BitSet whitelistBits = this.createTagBits(whitelist);
                    BitSet blacklistBits = this.createTagBits(blacklist);
                    boolean hasWhiteList = whitelistBits.notEmpty();
                    boolean hasBlackList = blacklistBits.notEmpty();
                    if (!hasWhiteList && !hasBlackList) {
                        DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
                        return this.emptyTagObjects;
                    } else {
                        tagGroup = new TaggedObjectManager.TagGroup<>(this, whitelistBits, blacklistBits);
                        this.populateTagGroupList(tagGroup, false);
                        this.tagGroupMap.put(formattedQueryStr, tagGroup);
                        this.tagGroups.add(tagGroup);
                        if (this.verbose) {
                            DebugLog.General.println("manager-> created new set for: " + formattedQueryStr);
                        }

                        return tagGroup.getUpdatedClientView();
                    }
                }
            }
        }
    }

    private BitSet createTagBits(String[] tags) {
        return this.createTagBits(tags, true);
    }

    private BitSet createTagBits(String[] tags, boolean registerNewTags) {
        BitSet categoryBits = new BitSet();
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                String tag = tags[i];
                Integer bitIndex = this.tagStringToIndexMap.get(tag);
                if (bitIndex != null) {
                    categoryBits.set(bitIndex);
                } else if (registerNewTags) {
                    if (this.warnNonPreprocessedNewTag) {
                        DebugLog.General.warn("manager-> new tag discovered that was not preprocessed, tag: " + tag);
                    }

                    int newBitIndex = this.registerTag(tag, true);
                    categoryBits.set(newBitIndex);
                }
            }
        }

        return categoryBits;
    }

    private List<T> populateTagGroupList(TaggedObjectManager.TagGroup<T> tagGroup, boolean clear) {
        return this.populateTaggedObjectList(tagGroup.whitelist, tagGroup.blacklist, tagGroup.list, clear);
    }

    private List<T> populateTaggedObjectList(BitSet whitelistBits, BitSet blacklistBits, List<T> listToPopulate, boolean clear) {
        return this.populateTaggedObjectList(whitelistBits, blacklistBits, listToPopulate, null, clear);
    }

    private List<T> populateTaggedObjectList(BitSet whitelistBits, BitSet blacklistBits, List<T> listToPopulate, List<T> sources, boolean clear) {
        if (clear && !listToPopulate.isEmpty()) {
            listToPopulate.clear();
        }

        List<T> taggedObjects;
        if (sources != null) {
            taggedObjects = sources;
        } else {
            taggedObjects = this.backingListProvider.getTaggedObjectList();
        }

        boolean hasWhiteList = whitelistBits.notEmpty();
        boolean hasBlackList = blacklistBits.notEmpty();

        for (int i = 0; i < taggedObjects.size(); i++) {
            T taggedObject = taggedObjects.get(i);
            if ((!hasWhiteList || taggedObject.getTagBits().intersects(whitelistBits))
                && (!hasBlackList || !taggedObject.getTagBits().intersects(blacklistBits))) {
                listToPopulate.add(taggedObject);
            }
        }

        return listToPopulate;
    }

    public List<T> filterList(String tagQueryString, List<T> listToPopulate, List<T> sourceList, boolean clearList) {
        if (clearList) {
            listToPopulate.clear();
        }

        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            if (this.verbose) {
                DebugLog.General.warn("manager-> query string empty, returning input list for: " + tagQueryString);
            }

            return listToPopulate;
        } else {
            tagQueryString = this.formatQueryString(tagQueryString);
            String[] whitelist = this.readWhitelist(tagQueryString);
            String[] blacklist = this.readBlackList(tagQueryString);
            BitSet whitelistBits = this.createTagBits(whitelist, false);
            BitSet blacklistBits = this.createTagBits(blacklist, false);
            boolean hasWhiteList = whitelistBits.notEmpty();
            boolean hasBlackList = blacklistBits.notEmpty();
            if (!hasWhiteList && !hasBlackList) {
                if (this.verbose) {
                    DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
                }

                return listToPopulate;
            } else {
                List<T> taggedObjects;
                if (sourceList != null) {
                    taggedObjects = sourceList;
                } else {
                    taggedObjects = this.backingListProvider.getTaggedObjectList();
                }

                for (int i = 0; i < taggedObjects.size(); i++) {
                    T taggedObject = taggedObjects.get(i);
                    if ((!hasWhiteList || taggedObject.getTagBits().intersects(whitelistBits))
                        && (!hasBlackList || !taggedObject.getTagBits().intersects(blacklistBits))) {
                        listToPopulate.add(taggedObject);
                    }
                }

                return listToPopulate;
            }
        }
    }

    public List<T> populateList(String tagQueryString, List<T> listToPopulate, List<T> sourceList, boolean clearList) {
        if (clearList) {
            listToPopulate.clear();
        }

        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            if (this.verbose) {
                DebugLog.General.warn("manager-> query string empty, returning input list for: " + tagQueryString);
            }

            return listToPopulate;
        } else {
            tagQueryString = this.formatQueryString(tagQueryString);
            String[] whitelist = this.readWhitelist(tagQueryString);
            String[] blacklist = this.readBlackList(tagQueryString);
            BitSet whitelistBits = this.createTagBits(whitelist);
            BitSet blacklistBits = this.createTagBits(blacklist);
            boolean hasWhiteList = whitelistBits.notEmpty();
            boolean hasBlackList = blacklistBits.notEmpty();
            if (!hasWhiteList && !hasBlackList) {
                DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
                return listToPopulate;
            } else {
                return this.populateTaggedObjectList(whitelistBits, blacklistBits, listToPopulate, sourceList, clearList);
            }
        }
    }

    public String formatAndRegisterQueryString(String tagQueryString) {
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            throw new IllegalArgumentException("Key is null or whitespace.");
        } else if (this.tagGroupMap.containsKey(tagQueryString)) {
            return tagQueryString;
        } else {
            tagQueryString = this.formatQueryString(tagQueryString);
            if (!this.tagGroupMap.containsKey(tagQueryString)) {
                this.queryTaggedObjects(tagQueryString);
            }

            return tagQueryString;
        }
    }

    public String formatQueryString(String tagQueryString) {
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            return tagQueryString;
        } else {
            String[] whitelist = this.readWhitelist(tagQueryString);
            String[] blacklist = this.readBlackList(tagQueryString);
            StringBuilder sb = new StringBuilder();
            this.tempStringList.clear();
            if (whitelist != null) {
                for (int i = 0; i < whitelist.length; i++) {
                    this.tempStringList.add(this.sanitizeTag(whitelist[i]));
                }

                Collections.sort(this.tempStringList);

                for (int i = 0; i < this.tempStringList.size(); i++) {
                    if (i == 0) {
                        sb.append(this.tempStringList.get(i));
                    } else {
                        sb.append(";").append(this.tempStringList.get(i));
                    }
                }
            }

            this.tempStringList.clear();
            if (blacklist != null) {
                sb.append("-");

                for (int ix = 0; ix < blacklist.length; ix++) {
                    this.tempStringList.add(this.sanitizeTag(blacklist[ix]));
                }

                Collections.sort(this.tempStringList);

                for (int ix = 0; ix < this.tempStringList.size(); ix++) {
                    if (ix == 0) {
                        sb.append(this.tempStringList.get(ix));
                    } else {
                        sb.append(";").append(this.tempStringList.get(ix));
                    }
                }
            }

            return sb.toString();
        }
    }

    private String[] readWhitelist(String tagQueryString) {
        if (!tagQueryString.contains("-")) {
            return tagQueryString.split(";");
        } else if (tagQueryString.startsWith("-")) {
            return null;
        } else {
            String[] sp = tagQueryString.split("-");
            return sp[0].split(";");
        }
    }

    private String[] readBlackList(String tagQueryString) {
        if (!tagQueryString.contains("-")) {
            return null;
        } else if (tagQueryString.startsWith("-")) {
            String s = tagQueryString.substring(1);
            return s.split(";");
        } else {
            String[] sp = tagQueryString.split("-");
            return sp[1].split(";");
        }
    }

    public void debugPrint() {
        this.debugPrint(null);
    }

    public void debugPrint(ArrayList<String> lines) {
        this.debugLog("[TaggedObjectManager]", lines);
        this.debugLog("{", lines);
        this.debugLog("[registeredTags]", lines);
        this.debugLog("{", lines);

        for (int i = 0; i < this.registeredTags.size(); i++) {
            String s = this.registeredTags.get(i);
            this.debugLog("  " + i + " = " + s, lines);
        }

        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagStringToIndexMap]", lines);
        this.debugLog("{", lines);

        for (Entry<String, Integer> entry : this.tagStringToIndexMap.entrySet()) {
            this.debugLog("  " + entry.getKey() + " = " + entry.getValue(), lines);
        }

        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagIndexToStringMap]", lines);
        this.debugLog("{", lines);

        for (Entry<Integer, String> entry : this.tagIndexToStringMap.entrySet()) {
            this.debugLog("  " + entry.getKey() + " = " + entry.getValue(), lines);
        }

        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagToObjectListMap]", lines);
        this.debugLog("{", lines);

        for (Entry<String, List<T>> entry : this.tagToObjectListMap.entrySet()) {
            this.debugLog("  " + entry.getKey(), lines);
            this.debugLog("  {", lines);

            for (T obj : entry.getValue()) {
                if (obj instanceof BaseScriptObject baseScriptObject) {
                    this.debugLog("    " + baseScriptObject.getScriptObjectFullType(), lines);
                } else {
                    this.debugLog("    " + obj, lines);
                }
            }

            this.debugLog("  }", lines);
        }

        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagStringAliasMap]", lines);
        this.debugLog("{", lines);

        for (Entry<String, String> entry : this.tagsStringAliasMap.entrySet()) {
            this.debugLog("  " + entry.getKey() + " = " + entry.getValue(), lines);
        }

        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagGroupMap]", lines);
        this.debugLog("{", lines);

        for (Entry<String, TaggedObjectManager.TagGroup<T>> entry : this.tagGroupMap.entrySet()) {
            this.debugLog("  [" + entry.getKey() + "]", lines);
            this.debugLog("  {", lines);
            this.debugLog("    whitelist = " + this.getBitSetString(entry.getValue().whitelist), lines);
            this.debugLog("    blacklist = " + this.getBitSetString(entry.getValue().blacklist), lines);
            this.debugLog("    [objects]", lines);
            this.debugLog("    {", lines);

            for (T objx : entry.getValue().list) {
                if (objx instanceof BaseScriptObject baseScriptObject) {
                    this.debugLog("      " + baseScriptObject.getScriptObjectFullType(), lines);
                } else {
                    this.debugLog("      " + objx, lines);
                }
            }

            this.debugLog("    }", lines);
            this.debugLog("  }", lines);
        }

        this.debugLog("}", lines);
        this.debugLog("}", lines);
    }

    private String getBitSetString(BitSet bitSet) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        boolean prepend = false;

        for (int i = 0; i < bitSet.length(); i++) {
            if (bitSet.get(i)) {
                if (prepend) {
                    sb.append(", ");
                }

                sb.append(this.tagIndexToStringMap.get(i));
                sb.append("(");
                sb.append(i);
                sb.append(")");
                prepend = true;
            }
        }

        sb.append(" }");
        return sb.toString();
    }

    private void debugLog(String s, ArrayList<String> lines) {
        if (lines != null) {
            lines.add(s);
        } else {
            DebugLog.log(s);
        }
    }

    public interface BackingListProvider<T extends TaggedObjectManager.TaggedObject> {
        List<T> getTaggedObjectList();
    }

    private static class TagGroup<T extends TaggedObjectManager.TaggedObject> {
        private final TaggedObjectManager<T> manager;
        private final BitSet whitelist;
        private final BitSet blacklist;
        private final List<T> list = new ArrayList<>();
        private final List<T> clientView;
        private boolean dirty;

        private TagGroup(TaggedObjectManager<T> manager, BitSet whitelist, BitSet blacklist) {
            this.manager = manager;
            this.whitelist = whitelist;
            this.blacklist = blacklist;
            this.clientView = PZUnmodifiableList.wrap(this.list);
        }

        private List<T> getUpdatedClientView() {
            if (this.dirty) {
                this.manager.populateTaggedObjectList(this.whitelist, this.blacklist, this.list, true);
                this.dirty = false;
            }

            return this.clientView;
        }
    }

    public interface TaggedObject {
        List<String> getTags();

        BitSet getTagBits();
    }
}
