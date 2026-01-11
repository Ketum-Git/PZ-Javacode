// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;

public class StringDictionary {
    private static final ArrayList<StringDictionary.StringRegister> registers = new ArrayList<>();
    private static final int MaxStringLen = 64;
    public static final StringDictionary.StringRegister Generic = addScriptRegister(new StringDictionary.StringRegister(64, "Generic"));

    private StringDictionary() {
    }

    private static StringDictionary.StringRegister addScriptRegister(StringDictionary.StringRegister register) {
        if (!registers.contains(register)) {
            registers.add(register);
        }

        return register;
    }

    protected static void StartScriptLoading() {
        for (StringDictionary.StringRegister register : registers) {
            register.onStartScriptLoading();
        }
    }

    protected static void reset() {
        for (StringDictionary.StringRegister register : registers) {
            register.reset();
        }
    }

    protected static void parseRegisters() throws WorldDictionaryException {
        for (int i = 0; i < registers.size(); i++) {
            StringDictionary.StringRegister register = registers.get(i);
            if (!GameClient.client) {
                register.parseLoadList();
            } else {
                register.parseLoadListClient();
            }
        }
    }

    protected static void saveAsText(FileWriter w, String padding) throws IOException {
        for (int i = 0; i < registers.size(); i++) {
            StringDictionary.StringRegister register = registers.get(i);
            register.saveAsText(w, padding);
        }
    }

    protected static void saveToByteBuffer(ByteBuffer bb) throws IOException {
        bb.putInt(registers.size());

        for (StringDictionary.StringRegister register : registers) {
            GameWindow.WriteString(bb, register.name);
            register.save(bb);
        }
    }

    protected static void loadFromByteBuffer(ByteBuffer bb, int Version) throws IOException {
        int size = bb.getInt();

        for (int i = 0; i < size; i++) {
            String registerName = GameWindow.ReadString(bb);
            boolean found = false;

            for (StringDictionary.StringRegister register : registers) {
                if (register.name.equals(registerName)) {
                    register.load(bb, Version);
                    found = true;
                    break;
                }
            }

            if (!found) {
                StringDictionary.StringRegister.loadEmpty(bb, Version);
                DebugLog.General.debugln("ScriptRegister not found or deprecated = " + registerName);
            }
        }
    }

    public static class StringRegister {
        private final Map<String, DictionaryStringInfo> stringToIdMap = new HashMap<>();
        private final Map<Short, DictionaryStringInfo> idToStringMap = new HashMap<>();
        private final Map<String, DictionaryStringInfo> loadList = new HashMap<>();
        private final String name;
        private short nextId;
        protected final int maxLen;

        protected StringRegister(int maxLen, String name) {
            if (name == null) {
                throw new RuntimeException("Name cannot be null.");
            } else {
                this.name = name;
                this.maxLen = maxLen;
            }
        }

        public void saveString(ByteBuffer output, String string) {
            DictionaryStringInfo info = this.getInfoForName(string);
            if (info != null) {
                output.put((byte)1);
                output.putShort(info.registryId);
            } else {
                DebugLog.General.warn("Unable to save string from register: " + string);
                output.put((byte)0);
                GameWindow.WriteString(output, string);
            }
        }

        public String loadString(ByteBuffer input, int WorldVersion) {
            if (input.get() == 1) {
                short id = input.getShort();
                DictionaryStringInfo info = this.getInfoForID(id);
                if (info != null) {
                    return info.string;
                } else {
                    if (Core.debug) {
                        DebugLog.General.warn("Unable to load string with id: " + id);
                    }

                    return null;
                }
            } else {
                return GameWindow.ReadString(input);
            }
        }

        public String get(short id) {
            DictionaryStringInfo info = this.idToStringMap.get(id);
            return info != null ? info.string : null;
        }

        public short getIdFor(String name) {
            DictionaryStringInfo info = this.stringToIdMap.get(name);
            return info != null ? info.registryId : -1;
        }

        public boolean isRegistered(String string) {
            if (WorldDictionary.isAllowScriptItemLoading()) {
                return string != null ? this.loadList.containsKey(string) : false;
            } else {
                if (string != null) {
                    DictionaryStringInfo info = this.stringToIdMap.get(string);
                    if (info != null && info.string.equals(string) && info.isLoaded()) {
                        return true;
                    }
                }

                return false;
            }
        }

        public DictionaryStringInfo getInfoForName(String name) {
            return this.stringToIdMap.get(name);
        }

        public DictionaryStringInfo getInfoForID(short id) {
            return this.idToStringMap.get(id);
        }

        public void register(String string) {
            if (string != null) {
                try {
                    if (!WorldDictionary.isAllowScriptItemLoading()) {
                        throw new WorldDictionaryException("Cannot register string at this time.");
                    }

                    if (!this.canRegister(string)) {
                        throw new WorldDictionaryException(this.name + ": Cannot register this string! string = " + string);
                    }

                    if (this.loadList.containsKey(string)) {
                        return;
                    }

                    DictionaryStringInfo info = new DictionaryStringInfo();
                    info.string = string;
                    this.loadList.put(info.string, info);
                } catch (Exception var3) {
                    var3.printStackTrace();
                }
            }
        }

        protected boolean canRegister(String string) throws WorldDictionaryException {
            if (string != null) {
                if (string.length() < this.maxLen) {
                    return true;
                }

                if (Core.debug) {
                    throw new RuntimeException(
                        "String exceeds default max string length, can be adjusted. maxlen = "
                            + this.maxLen
                            + ", strlen = "
                            + string.length()
                            + ", string = "
                            + string
                    );
                }
            }

            return false;
        }

        protected void parseLoadList() throws WorldDictionaryException {
            if (GameClient.client) {
                throw new WorldDictionaryException("Shouldn't be called on client!");
            } else {
                for (Entry<String, DictionaryStringInfo> entry : this.loadList.entrySet()) {
                    entry.getValue().isLoaded = false;
                }

                for (Entry<String, DictionaryStringInfo> entry : this.loadList.entrySet()) {
                    DictionaryStringInfo info = entry.getValue();
                    DictionaryStringInfo stored = this.stringToIdMap.get(info.string);
                    if (stored == null) {
                        if (this.nextId >= 32767) {
                            throw new WorldDictionaryException("Max string ID value reached for " + this.name + "!");
                        }

                        info.registryId = this.nextId++;
                        this.stringToIdMap.put(info.string, info);
                        this.idToStringMap.put(info.registryId, info);
                        info.isLoaded = true;
                        WorldDictionaryLogger.log(new Log.RegisterString(this.name, info.string, info.registryId, true));
                    } else {
                        stored.string = info.string;
                        stored.isLoaded = true;
                    }
                }
            }
        }

        protected void parseLoadListClient() throws WorldDictionaryException {
            if (!GameClient.client) {
                throw new WorldDictionaryException("Should only be called on client!");
            } else {
                for (Entry<String, DictionaryStringInfo> entry : this.stringToIdMap.entrySet()) {
                    DictionaryStringInfo stored = entry.getValue();
                    if (stored.isLoaded) {
                        DictionaryStringInfo info = this.loadList.get(stored.string);
                        if (info == null) {
                            throw new WorldDictionaryException("Missing dictionary string on client: " + stored.string);
                        }

                        stored.string = info.string;
                    }
                }
            }
        }

        protected void reset() {
            this.nextId = 0;
            this.stringToIdMap.clear();
            this.idToStringMap.clear();
        }

        protected void onStartScriptLoading() {
            this.loadList.clear();
        }

        protected void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + this.name + " = {" + System.lineSeparator());
            String p1 = padding + "\t";
            String p2 = p1 + "\t";

            for (Entry<String, DictionaryStringInfo> entry : this.stringToIdMap.entrySet()) {
                w.write(p1 + "{" + System.lineSeparator());
                entry.getValue().saveAsText(w, p2);
                w.write(p1 + "}," + System.lineSeparator());
            }

            w.write(padding + "}," + System.lineSeparator());
        }

        protected void save(ByteBuffer bb) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Save);
            bb.putShort(this.nextId);
            bb.putInt(this.stringToIdMap.size());

            for (Entry<String, DictionaryStringInfo> entry : this.stringToIdMap.entrySet()) {
                entry.getValue().save(bb);
            }

            ByteBlock.End(bb, block);
        }

        protected void load(ByteBuffer bb, int Version) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Load);
            this.nextId = bb.getShort();
            int size = bb.getInt();

            for (int i = 0; i < size; i++) {
                DictionaryStringInfo info = new DictionaryStringInfo();
                info.load(bb, Version);
                this.stringToIdMap.put(info.string, info);
                this.idToStringMap.put(info.registryId, info);
            }

            ByteBlock.End(bb, block);
        }

        private static final void loadEmpty(ByteBuffer bb, int Version) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Load);
            ByteBlock.SkipAndEnd(bb, block);
        }
    }
}
