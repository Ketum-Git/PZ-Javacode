// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.THashSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.iso.IsoLot;
import zombie.iso.MapFiles;
import zombie.util.PZXmlParserException;
import zombie.vehicles.BaseVehicle;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class WorldMapStreets {
    private static final ClosestPoint s_closestPoint = new ClosestPoint();
    private static final THashSet<WorldMapStreet> tempStreetSet = new THashSet<>();
    private static final ArrayList<WorldMapStreet> tempStreetList = new ArrayList<>();
    public static final HashMap<String, WorldMapStreets> s_fileNameToData = new HashMap<>();
    private static final HashSet<WorldMapStreet> s_streetSet = new HashSet<>();
    private final String relativeFileName;
    private final String absoluteFileName;
    private final ArrayList<WorldMapStreet> streets = new ArrayList<>();
    private final StreetLookup lookup = new StreetLookup();
    final TIntArrayList obscuredCells = new TIntArrayList();
    private final ArrayList<IWorldMapStreetListener> listeners = new ArrayList<>();
    long changeCount;
    boolean dirty;

    public static WorldMapStreets getOrCreateData(String relativeFileName, String absoluteFileName) {
        WorldMapStreets data = s_fileNameToData.get(absoluteFileName);
        if (data == null) {
            data = new WorldMapStreets(relativeFileName, absoluteFileName);
            data.initObscuredCells();
            s_fileNameToData.put(absoluteFileName, data);
            if (Files.exists(Paths.get(absoluteFileName))) {
                try {
                    data.read(absoluteFileName);
                } catch (PZXmlParserException var4) {
                    throw new RuntimeException(var4);
                }
            }
        }

        return data;
    }

    public WorldMapStreets(String relativeFileName, String absoluteFileName) {
        this.relativeFileName = relativeFileName;
        this.absoluteFileName = absoluteFileName;
    }

    public String getRelativeFileName() {
        return this.relativeFileName;
    }

    public String getAbsoluteFileName() {
        return this.absoluteFileName;
    }

    public StreetLookup getLookup() {
        return this.lookup;
    }

    public boolean read(String absoluteFileName) throws PZXmlParserException {
        WorldMapStreetsXML xml = new WorldMapStreetsXML();
        return xml.read(absoluteFileName, this);
    }

    public int getStreetCount() {
        return this.streets.size();
    }

    public WorldMapStreet getStreetByIndex(int index) {
        return this.streets.get(index);
    }

    public void addStreet(WorldMapStreet street) {
        if (!this.streets.contains(street)) {
            this.streets.add(street);
            this.lookup.addStreet(street);
            this.changeCount++;
            this.listeners.forEach(listener -> listener.onAdd(street));
        }
    }

    public void removeStreet(WorldMapStreet street) {
        this.listeners.forEach(listener -> listener.onBeforeRemove(street));
        this.streets.remove(street);
        this.lookup.removeStreet(street);
        this.changeCount++;
        this.listeners.forEach(listener -> listener.onAfterRemove(street));
    }

    public void setDirty(boolean b) {
        this.dirty = b;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public WorldMapStreet splitStreet(WorldMapStreet street, int index) {
        if (this.streets.contains(street) && index > 0 && index < street.getNumPoints()) {
            StreetPoints points = new StreetPoints();

            for (int i = index; i < street.getNumPoints(); i++) {
                points.add(street.getPointX(i), street.getPointY(i));
            }

            this.onBeforeModifyStreet(street);
            index++;

            while (index < street.getNumPoints()) {
                street.removePoint(index);
            }

            this.onAfterModifyStreet(street);
            WorldMapStreet newStreet = new WorldMapStreet(this, street.getTranslatedText(), points);
            newStreet.setWidth(street.getWidth());
            this.streets.add(this.streets.indexOf(street) + 1, newStreet);
            this.lookup.addStreet(newStreet);
            this.listeners.forEach(listener -> listener.onAdd(newStreet));
            return newStreet;
        } else {
            return null;
        }
    }

    int indexOf(WorldMapStreet street) {
        return this.streets.indexOf(street);
    }

    void onBeforeModifyStreet(WorldMapStreet street) {
        this.lookup.removeStreet(street);
        this.changeCount++;
        this.listeners.forEach(listener -> listener.onBeforeModifyStreet(street));
    }

    void onAfterModifyStreet(WorldMapStreet street) {
        this.lookup.addStreet(street);
        this.changeCount++;
        this.listeners.forEach(listener -> listener.onAfterModifyStreet(street));
    }

    void initObscuredCells() {
        for (MapFiles mapFiles : IsoLot.MapFiles) {
            if (this.absoluteFileName.startsWith(mapFiles.mapDirectoryAbsolutePath)) {
                for (int cellY = mapFiles.minCell300Y; cellY <= mapFiles.maxCell300Y; cellY++) {
                    for (int cellX = mapFiles.minCell300X; cellX <= mapFiles.maxCell300X; cellX++) {
                        if (mapFiles.hasCell300(cellX, cellY)) {
                            for (int i = 0; i < mapFiles.priority; i++) {
                                MapFiles mapFiles1 = IsoLot.MapFiles.get(i);
                                if (mapFiles1.hasCell300(cellX, cellY)) {
                                    this.obscuredCells.add(cellX);
                                    this.obscuredCells.add(cellY);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    public void renderStreetLines(UIWorldMap ui, float r, float g, float b, float a, int thickness) {
        this.getStreetsOverlapping(ui, 0.0F, 0.0F, ui.getWidth().floatValue(), ui.getHeight().floatValue(), tempStreetSet);

        for (WorldMapStreet street : tempStreetSet) {
            street.renderLines(ui, r, g, b, a, thickness, null);
        }
    }

    void getStreetsOverlapping(UIWorldMap ui, float uiMinX, float uiMinY, float uiMaxX, float uiMaxY, THashSet<WorldMapStreet> result) {
        result.clear();
        if (!(ui.getAPI().getZoomF() < 13.0F)) {
            float worldX1 = ui.getAPI().uiToWorldX(uiMinX, uiMinY);
            float worldY1 = ui.getAPI().uiToWorldY(uiMinX, uiMinY);
            float worldX2 = ui.getAPI().uiToWorldX(uiMaxX, uiMinY);
            float worldY2 = ui.getAPI().uiToWorldY(uiMaxX, uiMinY);
            float worldX3 = ui.getAPI().uiToWorldX(uiMaxX, uiMaxY);
            float worldY3 = ui.getAPI().uiToWorldY(uiMaxX, uiMaxY);
            float worldX4 = ui.getAPI().uiToWorldX(uiMinX, uiMaxY);
            float worldY4 = ui.getAPI().uiToWorldY(uiMinX, uiMaxY);
            float minX = PZMath.min(worldX1, worldX2, worldX3, worldX4);
            float minY = PZMath.min(worldY1, worldY2, worldY3, worldY4);
            float maxX = PZMath.max(worldX1, worldX2, worldX3, worldX4);
            float maxY = PZMath.max(worldY1, worldY2, worldY3, worldY4);
            minX = PZMath.max(minX, (float)ui.getWorldMap().getMinXInSquares());
            minY = PZMath.max(minY, (float)ui.getWorldMap().getMinYInSquares());
            maxX = PZMath.min(maxX, (float)ui.getWorldMap().getMaxXInSquares());
            maxY = PZMath.min(maxY, (float)ui.getWorldMap().getMaxYInSquares());
            this.lookup.getStreetsOverlapping(PZMath.fastfloor(minX), PZMath.fastfloor(minY), (int)PZMath.ceil(maxX), (int)PZMath.ceil(maxY), result);
        }
    }

    public boolean canPickStreet(UIWorldMap ui, float uiX, float uiY) {
        int worldX = PZMath.fastfloor(ui.getAPI().uiToWorldX(uiX, uiY));
        int worldY = PZMath.fastfloor(ui.getAPI().uiToWorldY(uiX, uiY));
        WorldMapRenderer renderer = ui.getAPI().getRenderer();
        return renderer.getVisited() == null || !renderer.getBoolean("HideUnvisited") || renderer.getVisited().isKnown(worldX, worldY);
    }

    public WorldMapStreet pickStreet(UIWorldMap ui, float uiX, float uiY, float dist, boolean bRender) {
        this.getStreetsOverlapping(ui, uiX - dist, uiY - dist, uiX + dist, uiY + dist, tempStreetSet);
        WorldMapStreet closest = null;
        Vector2f closestPt = BaseVehicle.allocVector2f();
        float closestDistSq = Float.MAX_VALUE;

        for (WorldMapStreet street : tempStreetSet) {
            float distSq = street.getClosestPointOn(ui, uiX, uiY, s_closestPoint);
            float radius = PZMath.max(10.0F, street.getWidth() / 2.0F * ui.getAPI().getWorldScale());
            if (distSq < closestDistSq && distSq < radius * radius) {
                closestDistSq = distSq;
                closest = street;
            }
        }

        BaseVehicle.releaseVector2f(closestPt);
        if (bRender && closest != null) {
            closest.renderIntersections(ui, 1.0F, 0.0F, 0.0F, 1.0F);
        }

        return closest;
    }

    private void renderOverlappedChunks(UIWorldMap ui, WorldMapStreet street) {
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0F);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0F);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0F);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0F);

        for (int cellY = cellY1; cellY <= cellY2; cellY++) {
            for (int cellX = cellX1; cellX <= cellX2; cellX++) {
                StreetLookup.Cell cell = this.lookup.getCell(cellX, cellY);
                if (cell != null) {
                    for (int chunkY = 0; chunkY < 10; chunkY++) {
                        for (int chunkX = 0; chunkX < 10; chunkX++) {
                            StreetLookup.Chunk chunk = cell.getChunk(chunkX, chunkY);
                            if (chunk != null && chunk.contains(street)) {
                                float x1 = ui.getAPI().worldToUIX(cellX * 200 + chunkX * 20, cellY * 200 + chunkY * 20);
                                float y1 = ui.getAPI().worldToUIY(cellX * 200 + chunkX * 20, cellY * 200 + chunkY * 20);
                                float x2 = ui.getAPI().worldToUIX(cellX * 200 + (chunkX + 1) * 20, cellY * 200 + (chunkY + 1) * 20);
                                float y2 = ui.getAPI().worldToUIY(cellX * 200 + (chunkX + 1) * 20, cellY * 200 + (chunkY + 1) * 20);
                                ui.DrawLine(null, x1, y1, x2, y1, 1.0F, 0.0, 0.0, 0.0, 1.0);
                                ui.DrawLine(null, x2, y1, x2, y2, 1.0F, 0.0, 0.0, 0.0, 1.0);
                                ui.DrawLine(null, x2, y2, x1, y2, 1.0F, 0.0, 0.0, 0.0, 1.0);
                                ui.DrawLine(null, x1, y2, x1, y1, 1.0F, 0.0, 0.0, 0.0, 1.0);
                            }
                        }
                    }
                }
            }
        }
    }

    private void calculateIntersections(WorldMapStreet street) {
        street.getIntersections().clear();
        int cellX1 = PZMath.fastfloor(street.getMinX() / 200.0F);
        int cellY1 = PZMath.fastfloor(street.getMinY() / 200.0F);
        int cellX2 = PZMath.fastfloor(street.getMaxX() / 200.0F);
        int cellY2 = PZMath.fastfloor(street.getMaxY() / 200.0F);
        s_streetSet.clear();

        for (int cellY = cellY1; cellY <= cellY2; cellY++) {
            for (int cellX = cellX1; cellX <= cellX2; cellX++) {
                StreetLookup.Cell cell = this.lookup.getCell(cellX, cellY);
                if (cell != null) {
                    for (int chunkY = 0; chunkY < 10; chunkY++) {
                        for (int chunkX = 0; chunkX < 10; chunkX++) {
                            StreetLookup.Chunk chunk = cell.getChunk(chunkX, chunkY);
                            if (chunk != null && chunk.contains(street)) {
                                this.calculateIntersections(street, chunk.streets, chunk.streetCount, s_streetSet);
                            }
                        }
                    }
                }
            }
        }

        street.getIntersections().sort((o1, o2) -> Float.compare(o1.distanceFromStart, o2.distanceFromStart));
    }

    private void calculateIntersections(WorldMapStreet street, WorldMapStreet[] streets, int streetCount, HashSet<WorldMapStreet> done) {
        for (int i = 0; i < streetCount; i++) {
            WorldMapStreet street2 = streets[i];
            if (street != street2 && !done.contains(street2)) {
                done.add(street2);
                street.calculateIntersections(street2);
            }
        }
    }

    public boolean checkForEdits() {
        boolean bEdits = false;

        for (WorldMapStreet street : this.streets) {
            if (this.changeCount != street.changeCount) {
                street.changeCount = this.changeCount;
                street.clipToObscuredCells();
                this.calculateIntersections(street);
                this.initConnectedStreets(street);
                bEdits = true;
            }
        }

        return bEdits;
    }

    public void render(UIWorldMap ui, StreetRenderData renderData) {
        this.checkForEdits();
        this.getStreetsOverlapping(ui, 0.0F, 0.0F, ui.getWidth().floatValue(), ui.getHeight().floatValue(), tempStreetSet);
        tempStreetList.clear();
        tempStreetList.addAll(tempStreetSet);

        for (int i = 0; i < tempStreetList.size(); i++) {
            WorldMapStreet street = tempStreetList.get(i);

            for (int j = 0; j < street.getIntersections().size(); j++) {
                Intersection intersection = street.getIntersections().get(j);
                tempStreetSet.add(intersection.street);
            }
        }

        tempStreetList.clear();
        tempStreetList.addAll(tempStreetSet);
        tempStreetList.sort((o1, o2) -> this.streets.indexOf(o1) - this.streets.indexOf(o2));

        for (int i = 0; i < tempStreetList.size(); i++) {
            WorldMapStreet street = tempStreetList.get(i);
            street.resetIntersectionRenderFlag();
        }

        for (int i = 0; i < tempStreetList.size(); i++) {
            WorldMapStreet street = tempStreetList.get(i);
            street.render(ui, renderData);
        }
    }

    void getConnectedStreets(WorldMapStreet street, THashSet<WorldMapStreet> result) {
        tempStreetSet.clear();
        result.clear();
        this.getConnectedStreets(street, tempStreetSet, result);
    }

    void getConnectedStreets(WorldMapStreet street, THashSet<WorldMapStreet> done, THashSet<WorldMapStreet> result) {
        if (!done.contains(street)) {
            done.add(street);
            result.add(street);
            if (street.connectedToStart != null) {
                this.getConnectedStreets(street.connectedToStart, done, result);
            }

            if (street.connectedToEnd != null) {
                this.getConnectedStreets(street.connectedToEnd, done, result);
            }

            for (Intersection ixn : street.getIntersections()) {
                if (ixn.street.getTranslatedText().equals(street.getTranslatedText())) {
                    if (ixn.segment == 0) {
                        this.getConnectedStreets(ixn.street, done, result);
                    }

                    if (ixn.segment == street.getNumPoints() - 2) {
                        this.getConnectedStreets(ixn.street, done, result);
                    }
                }
            }
        }
    }

    void initConnectedStreets(WorldMapStreet street) {
        street.connectedToStart = this.initConnectedStreets(street, 0);
        street.connectedToEnd = this.initConnectedStreets(street, street.getNumPoints() - 1);
    }

    WorldMapStreet initConnectedStreets(WorldMapStreet street, int pointIndex) {
        float worldX = street.getPointX(pointIndex);
        float worldY = street.getPointY(pointIndex);
        tempStreetSet.clear();
        this.getLookup()
            .getStreetsOverlapping(
                PZMath.fastfloor(worldX - 1.0F),
                PZMath.fastfloor(worldY - 1.0F),
                PZMath.fastfloor(worldX + 1.0F),
                PZMath.fastfloor(worldY + 1.0F),
                tempStreetSet
            );

        for (WorldMapStreet street2 : tempStreetSet) {
            if (street2 != street && street2.getTranslatedText().equals(street.getTranslatedText())) {
                street2.getClosestPointOn(worldX, worldY, s_closestPoint);
                if (s_closestPoint.distSq < 4.0F) {
                    return street2;
                }
            }
        }

        return null;
    }

    public void addListener(IWorldMapStreetListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(IWorldMapStreetListener listener) {
        this.listeners.remove(listener);
    }

    public void combine(WorldMapStreets other) {
        other.checkForEdits();

        for (int i = 0; i < other.streets.size(); i++) {
            WorldMapStreet street = other.streets.get(i);

            for (int j = 0; j < street.splitStreets.size(); j++) {
                WorldMapStreet street1 = street.splitStreets.get(j);
                WorldMapStreet copy = street1.createCopy(this);
                this.addStreet(copy);
            }
        }
    }

    public void clear() {
        WorldMapStreet.s_pool.releaseAll(this.streets);
        this.streets.clear();
        this.dirty = false;
    }
}
