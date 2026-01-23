// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "fileGuidTable")
public final class FileGuidTable {
    @XmlElement(name = "files")
    public final ArrayList<FileGuidPair> files = new ArrayList<>();
    @XmlTransient
    private final Map<String, String> guidToPath = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    @XmlTransient
    private final Map<String, String> pathToGuid = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public void setModID(String modID) {
        for (FileGuidPair pair : this.files) {
            pair.guid = modID + "-" + pair.guid;
        }
    }

    public void mergeFrom(FileGuidTable other) {
        this.files.addAll(other.files);
    }

    public void loaded() {
        for (FileGuidPair pair : this.files) {
            this.guidToPath.put(pair.guid, pair.path);
            this.pathToGuid.put(pair.path, pair.guid);
        }
    }

    public void clear() {
        this.files.clear();
        this.guidToPath.clear();
        this.pathToGuid.clear();
    }

    public String getFilePathFromGuid(String guid) {
        return this.guidToPath.get(guid);
    }

    public String getGuidFromFilePath(String path) {
        return this.pathToGuid.get(path);
    }
}
