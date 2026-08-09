// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.WornItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zombie.UsedFromLua;
import zombie.scripting.objects.ItemBodyLocation;

@UsedFromLua
public class BodyLocationGroup {
    private final String id;
    private final List<BodyLocation> locations = new ArrayList<>();

    public BodyLocationGroup(String id) {
        if (id == null) {
            throw new NullPointerException("id is null");
        } else if (id.isEmpty()) {
            throw new IllegalArgumentException("id is empty");
        } else {
            this.id = id;
        }
    }

    public String getId() {
        return this.id;
    }

    public BodyLocation getLocation(ItemBodyLocation itemBodyLocation) {
        for (int i = 0; i < this.locations.size(); i++) {
            BodyLocation location = this.locations.get(i);
            if (location.isId(itemBodyLocation)) {
                return location;
            }
        }

        return null;
    }

    public BodyLocation getOrCreateLocation(ItemBodyLocation itemBodyLocation) {
        BodyLocation bodyLocation = this.getLocation(itemBodyLocation);
        if (bodyLocation == null) {
            bodyLocation = new BodyLocation(this, itemBodyLocation);
            this.locations.add(bodyLocation);
        }

        return bodyLocation;
    }

    public BodyLocation getLocationByIndex(int index) {
        return index >= 0 && index < this.size() ? this.locations.get(index) : null;
    }

    public void moveLocationToIndex(ItemBodyLocation itemBodyLocation, int index) {
        if (index >= 0 && index < this.size()) {
            for (int i = 0; i < this.locations.size(); i++) {
                BodyLocation location = this.locations.get(i);
                if (location.isId(itemBodyLocation)) {
                    this.locations.add(index, this.locations.remove(i));
                }
            }
        }
    }

    public int size() {
        return this.locations.size();
    }

    public void setExclusive(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        BodyLocation second = this.getLocation(secondId);
        first.setExclusive(secondId);
        second.setExclusive(firstId);
    }

    public boolean isExclusive(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        return first.isExclusive(secondId);
    }

    public void setHideModel(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        first.setHideModel(secondId);
    }

    public boolean isHideModel(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        return first.isHideModel(secondId);
    }

    public void setAltModel(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        first.setAltModel(secondId);
    }

    public boolean isAltModel(ItemBodyLocation firstId, ItemBodyLocation secondId) {
        BodyLocation first = this.getLocation(firstId);
        return first.isAltModel(secondId);
    }

    public int indexOf(ItemBodyLocation locationId) {
        for (int i = 0; i < this.locations.size(); i++) {
            BodyLocation location = this.locations.get(i);
            if (location.isId(locationId)) {
                return i;
            }
        }

        return -1;
    }

    public void setMultiItem(ItemBodyLocation locationId, boolean bMultiItem) {
        BodyLocation location = this.getLocation(locationId);
        location.setMultiItem(bMultiItem);
    }

    public boolean isMultiItem(ItemBodyLocation locationId) {
        BodyLocation location = this.getLocation(locationId);
        return location.isMultiItem();
    }

    public List<BodyLocation> getAllLocations() {
        return Collections.unmodifiableList(this.locations);
    }
}
