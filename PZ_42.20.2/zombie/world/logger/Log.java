// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import zombie.world.DictionaryInfo;
import zombie.world.DictionaryScriptInfo;
import zombie.world.ItemInfo;

public class Log {
    public abstract static class BaseItemLog extends Log.BaseLog {
        protected final DictionaryInfo<?> itemInfo;
        protected final boolean isItem;

        public BaseItemLog(DictionaryInfo<?> itemInfo) {
            this.itemInfo = itemInfo;
            this.isItem = this.itemInfo instanceof ItemInfo;
        }

        public final String getTypeTag() {
            return this.isItem ? "item" : "entity";
        }

        @Override
        abstract void saveAsText(FileWriter var1, String var2) throws IOException;

        protected String getItemString() {
            return "fulltype = \""
                + this.itemInfo.getFullType()
                + "\", registeryID = "
                + this.itemInfo.getRegistryID()
                + ", existsVanilla = "
                + this.itemInfo.isExistsAsVanilla()
                + ", isModded = "
                + this.itemInfo.isModded()
                + ", modID = \""
                + this.itemInfo.getModID()
                + "\", obsolete = "
                + this.itemInfo.isObsolete()
                + ", removed = "
                + this.itemInfo.isRemoved()
                + ", isLoaded = "
                + this.itemInfo.isLoaded();
        }
    }

    public abstract static class BaseLog {
        protected boolean ignoreSaveCheck;

        public boolean isIgnoreSaveCheck() {
            return this.ignoreSaveCheck;
        }

        abstract void saveAsText(FileWriter var1, String var2) throws IOException;
    }

    public abstract static class BaseScriptLog extends Log.BaseLog {
        protected final DictionaryScriptInfo<?> info;

        public BaseScriptLog(DictionaryScriptInfo<?> info) {
            this.info = info;
        }

        @Override
        abstract void saveAsText(FileWriter var1, String var2) throws IOException;

        protected String getScriptString() {
            return "name = \""
                + this.info.getName()
                + "\", registeryID = "
                + this.info.getRegistryID()
                + ", version = "
                + this.info.getVersion()
                + ", isLoaded = "
                + this.info.isLoaded();
        }
    }

    public static class Comment extends Log.BaseLog {
        protected String txt;

        public Comment(String txt) {
            this.ignoreSaveCheck = true;
            this.txt = txt;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "-- " + this.txt + System.lineSeparator());
        }
    }

    public static class Info extends Log.BaseLog {
        protected final List<String> mods;
        protected final String timeStamp;
        protected final String saveWorld;
        protected final int worldVersion;
        public boolean hasErrored;

        public Info(String timeStamp, String saveWorld, int worldVersion, List<String> mods) {
            this.ignoreSaveCheck = true;
            this.timeStamp = timeStamp;
            this.saveWorld = saveWorld;
            this.worldVersion = worldVersion;
            this.mods = mods;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{" + System.lineSeparator());
            w.write(padding + "\ttype = \"info\"," + System.lineSeparator());
            w.write(padding + "\ttimeStamp = \"" + this.timeStamp + "\"," + System.lineSeparator());
            w.write(padding + "\tsaveWorld = \"" + this.saveWorld + "\"," + System.lineSeparator());
            w.write(padding + "\tworldVersion = " + this.worldVersion + "," + System.lineSeparator());
            w.write(padding + "\thasErrored = " + this.hasErrored + "," + System.lineSeparator());
            w.write(padding + "\titemMods = {" + System.lineSeparator());

            for (int i = 0; i < this.mods.size(); i++) {
                w.write(padding + "\t\t\"" + this.mods.get(i) + "\"," + System.lineSeparator());
            }

            w.write(padding + "\t}," + System.lineSeparator());
            w.write(padding + "}," + System.lineSeparator());
        }
    }

    public static class ModIDChangedItem extends Log.BaseItemLog {
        protected final String oldModId;
        protected final String newModId;

        public ModIDChangedItem(DictionaryInfo<?> itemInfo, String oldModId, String newModId) {
            super(itemInfo);
            this.oldModId = oldModId;
            this.newModId = newModId;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(
                padding
                    + "{ type = \"modchange_"
                    + this.getTypeTag()
                    + "\", oldModID = \""
                    + this.oldModId
                    + "\", "
                    + this.getItemString()
                    + " }"
                    + System.lineSeparator()
            );
        }
    }

    public static class ObsoleteItem extends Log.BaseItemLog {
        public ObsoleteItem(DictionaryInfo<?> itemInfo) {
            super(itemInfo);
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"obsolete_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
        }
    }

    public static class RegisterItem extends Log.BaseItemLog {
        public RegisterItem(DictionaryInfo<?> itemInfo) {
            super(itemInfo);
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"reg_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
        }
    }

    public static class RegisterObject extends Log.BaseLog {
        protected final String objectName;
        protected final int id;

        public RegisterObject(String objectName, int id) {
            this.objectName = objectName;
            this.id = id;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"reg_obj\", id = " + this.id + ", obj = \"" + this.objectName + "\" }" + System.lineSeparator());
        }
    }

    public static class RegisterScript extends Log.BaseScriptLog {
        public RegisterScript(DictionaryScriptInfo<?> info) {
            super(info);
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"reg_script\", " + this.getScriptString() + " }" + System.lineSeparator());
        }
    }

    public static class RegisterString extends Log.BaseLog {
        protected static final String reg = "reg_str";
        protected static final String unreg = "un_reg_str";
        protected final String registerName;
        protected final String s;
        protected final int id;
        protected final boolean register;

        public RegisterString(String registerName, String s, int id) {
            this(registerName, s, id, true);
        }

        public RegisterString(String registerName, String s, int id, boolean register) {
            this.registerName = registerName;
            this.s = s;
            this.id = id;
            this.register = register;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(
                padding
                    + "{ type = \""
                    + (this.register ? "reg_str" : "un_reg_str")
                    + "\", register = "
                    + this.registerName
                    + ", id = "
                    + this.id
                    + ", value = \""
                    + this.s
                    + "\" }"
                    + System.lineSeparator()
            );
        }
    }

    public static class ReinstateItem extends Log.BaseItemLog {
        public ReinstateItem(DictionaryInfo<?> itemInfo) {
            super(itemInfo);
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"reinstate_" + this.getTypeTag() + "\", " + this.getItemString() + " }" + System.lineSeparator());
        }
    }

    public static class RemovedItem extends Log.BaseItemLog {
        protected final boolean isScriptMissing;

        public RemovedItem(DictionaryInfo<?> itemInfo, boolean isScriptMissing) {
            super(itemInfo);
            this.isScriptMissing = isScriptMissing;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(
                padding
                    + "{ type = \"removed_"
                    + this.getTypeTag()
                    + "\", scriptMissing = "
                    + this.isScriptMissing
                    + ", "
                    + this.getItemString()
                    + " }"
                    + System.lineSeparator()
            );
        }
    }

    public static class VersionChangedScript extends Log.BaseScriptLog {
        private final long oldVersion;

        public VersionChangedScript(DictionaryScriptInfo<?> info, long oldVersion) {
            super(info);
            this.oldVersion = oldVersion;
        }

        @Override
        public void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + "{ type = \"script_version\", old_version = " + this.oldVersion + "," + this.getScriptString() + " }" + System.lineSeparator());
        }
    }
}
