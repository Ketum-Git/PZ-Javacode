// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.types.MapItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.spnetwork.SinglePlayerClient;
import zombie.worldMap.symbols.SymbolSaveData;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

@UsedFromLua
public final class WorldMapClient {
    public static final WorldMapClient instance = new WorldMapClient();
    private ByteBuffer byteBuffer;
    private ByteBufferWriter byteBufferWriter;
    private final TIntObjectHashMap<WorldMapBaseSymbol> symbolById = new TIntObjectHashMap<>();
    private boolean receivedRequestData;
    private boolean worldMapLoaded;

    public static WorldMapClient getInstance() {
        return instance;
    }

    private void checkBuffer() {
        if (this.byteBuffer == null) {
            this.byteBuffer = ByteBuffer.allocate(1048576);
            this.byteBufferWriter = new ByteBufferWriter(this.byteBuffer);
        }
    }

    public void receive(ByteBuffer bb) throws IOException {
        int packet = bb.get();
        switch (packet) {
            case 1:
                this.receiveAddMarker(bb);
                break;
            case 2:
                this.receiveRemoveMarker(bb);
                break;
            case 3:
                this.receiveAddSymbol(bb);
                break;
            case 4:
                this.receiveRemoveSymbol(bb);
                break;
            case 5:
                this.receiveModifySymbol(bb);
                break;
            case 6:
                this.receiveSetPrivateSymbol(bb);
                break;
            case 7:
                this.receiveModifySharing(bb);
        }
    }

    private void receiveAddMarker(ByteBuffer bb) throws IOException {
    }

    private void receiveRemoveMarker(ByteBuffer bb) throws IOException {
    }

    private void receiveAddSymbol(ByteBuffer bb) throws IOException {
        if (this.receivedRequestData) {
            SymbolSaveData saveData = new SymbolSaveData(241, 2);
            saveData.load(bb);
            WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
            networkInfo.load(bb, 241, 2);
            int symbolType = bb.get();
            WorldMapBaseSymbol symbol;
            if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
                symbol = new WorldMapTextSymbol(MapItem.getSingleton().getSymbols());
            } else {
                if (symbolType != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
                    throw new IOException("unknown map symbol type " + symbolType);
                }

                symbol = new WorldMapTextureSymbol(MapItem.getSingleton().getSymbols());
            }

            symbol.load(bb, saveData);
            symbol.setNetworkInfo(networkInfo);
            this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
            if (this.worldMapLoaded) {
                MapItem.getSingleton().getSymbols().addSymbol(symbol);
            }
        }
    }

    private void receiveRemoveSymbol(ByteBuffer bb) throws IOException {
        if (this.receivedRequestData) {
            int id = bb.getInt();
            WorldMapBaseSymbol symbol = this.symbolById.remove(id);
            if (symbol != null) {
                if (this.worldMapLoaded) {
                    MapItem.getSingleton().getSymbols().removeSymbol(symbol);
                }
            }
        }
    }

    private void receiveModifySymbol(ByteBuffer bb) throws IOException {
        if (this.receivedRequestData) {
            SymbolSaveData saveData = new SymbolSaveData(241, 2);
            saveData.load(bb);
            int id = bb.getInt();
            WorldMapBaseSymbol symbol = this.symbolById.get(id);
            if (symbol != null) {
                symbol.getNetworkInfo().load(bb, 241, 2);
                symbol.load(bb, saveData);
                if (this.worldMapLoaded) {
                    MapItem.getSingleton().getSymbols().invalidateLayout();
                }
            }
        }
    }

    private void receiveSetPrivateSymbol(ByteBuffer bb) throws IOException {
        if (this.receivedRequestData) {
            int id = bb.getInt();
            WorldMapBaseSymbol symbol = this.symbolById.get(id);
            if (symbol != null) {
                this.symbolById.remove(id);
                boolean bAuthorLocal = symbol.isAuthorLocalPlayer();
                symbol.setPrivate();
                if (this.worldMapLoaded && !bAuthorLocal) {
                    MapItem.getSingleton().getSymbols().removeSymbol(symbol);
                }
            }
        }
    }

    private void receiveModifySharing(ByteBuffer bb) throws IOException {
        if (this.receivedRequestData) {
            int id = bb.getInt();
            WorldMapBaseSymbol symbol = this.symbolById.get(id);
            if (symbol != null) {
                symbol.getNetworkInfo().load(bb, 241, 2);
            }
        }
    }

    public void receiveRequestData(ByteBuffer bb) throws IOException {
        this.receivedRequestData = true;
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.load(bb);
        int symbolCount = bb.getInt();

        for (int i = 0; i < symbolCount; i++) {
            WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
            networkInfo.load(bb, 241, 2);
            int symbolType = bb.get();
            WorldMapBaseSymbol symbol;
            if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
                symbol = new WorldMapTextSymbol(MapItem.getSingleton().getSymbols());
            } else {
                if (symbolType != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
                    throw new IOException("unknown map symbol type " + symbolType);
                }

                symbol = new WorldMapTextureSymbol(MapItem.getSingleton().getSymbols());
            }

            symbol.load(bb, saveData);
            symbol.setNetworkInfo(networkInfo);
            this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
        }
    }

    public void worldMapLoaded() {
        this.worldMapLoaded = true;
        WorldMapSymbols symbols = MapItem.getSingleton().getSymbols();
        this.symbolById.forEachValue(symbol -> {
            symbols.addSymbol(symbol);
            return true;
        });
    }

    private void sendPacket(ByteBuffer bb) {
        if (GameServer.server) {
            throw new IllegalStateException("can't call this method on the server");
        } else {
            if (GameClient.client) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                bb.flip();
                b.bb.put(bb);
                PacketTypes.PacketType.WorldMap.send(GameClient.connection);
            } else {
                ByteBufferWriter b = SinglePlayerClient.connection.startPacket();
                bb.flip();
                b.bb.put(bb);
                SinglePlayerClient.connection.endPacketImmediate();
            }
        }
    }

    public void sendShareSymbol(WorldMapBaseSymbol symbol, WorldMapSymbolNetworkInfo networkInfo) {
        if (symbol.isShared()) {
            if (!symbol.getNetworkInfo().equals(networkInfo)) {
                networkInfo.setID(symbol.getNetworkInfo().getID());
                this.checkBuffer();
                this.byteBuffer.clear();
                ByteBufferWriter b = this.byteBufferWriter;
                PacketTypes.PacketType.WorldMap.doPacket(b);
                b.putByte((byte)7);

                try {
                    b.putInt(symbol.getNetworkInfo().getID());
                    networkInfo.save(b.bb);
                    this.sendPacket(this.byteBuffer);
                } catch (IOException var5) {
                    ExceptionLogger.logException(var5);
                }
            }
        } else {
            this.sendAddSymbol(symbol, networkInfo);
        }
    }

    public void sendAddSymbol(WorldMapBaseSymbol symbol, WorldMapSymbolNetworkInfo networkInfo) {
        if (!symbol.isShared()) {
            this.checkBuffer();
            this.byteBuffer.clear();
            ByteBufferWriter b = this.byteBufferWriter;
            PacketTypes.PacketType.WorldMap.doPacket(b);
            b.putByte((byte)3);

            try {
                SymbolSaveData saveData = new SymbolSaveData(241, 2);
                saveData.save(b.bb, symbol);
                networkInfo.save(b.bb);
                b.putByte((byte)symbol.getType().index());
                symbol.save(b.bb, saveData);
                this.sendPacket(this.byteBuffer);
                MapItem.getSingleton().getSymbols().removeSymbol(symbol);
            } catch (IOException var5) {
                ExceptionLogger.logException(var5);
            }
        }
    }

    public void sendModifySymbol(WorldMapBaseSymbol symbol) {
        if (!symbol.isPrivate()) {
            this.checkBuffer();
            this.byteBuffer.clear();
            ByteBufferWriter b = this.byteBufferWriter;
            PacketTypes.PacketType.WorldMap.doPacket(b);
            b.putByte((byte)5);

            try {
                SymbolSaveData saveData = new SymbolSaveData(241, 2);
                saveData.save(b.bb, symbol);
                b.putInt(symbol.getNetworkInfo().getID());
                symbol.save(b.bb, saveData);
                this.sendPacket(this.byteBuffer);
            } catch (IOException var4) {
                ExceptionLogger.logException(var4);
            }
        }
    }

    public void sendSetPrivateSymbol(WorldMapBaseSymbol symbol) {
        if (!symbol.isPrivate()) {
            this.checkBuffer();
            this.byteBuffer.clear();
            ByteBufferWriter b = this.byteBufferWriter;
            PacketTypes.PacketType.WorldMap.doPacket(b);
            b.putByte((byte)6);
            b.putInt(symbol.getNetworkInfo().getID());
            this.sendPacket(this.byteBuffer);
        }
    }

    public void sendRemoveSymbol(WorldMapBaseSymbol symbol) {
        if (!symbol.isPrivate()) {
            this.checkBuffer();
            this.byteBuffer.clear();
            ByteBufferWriter b = this.byteBufferWriter;
            PacketTypes.PacketType.WorldMap.doPacket(b);
            b.putByte((byte)4);
            b.putInt(symbol.getNetworkInfo().getID());
            this.sendPacket(this.byteBuffer);
        }
    }

    public void Reset() {
        this.receivedRequestData = false;
        this.worldMapLoaded = false;
        this.symbolById.forEachValue(symbol -> {
            symbol.release();
            return true;
        });
        this.symbolById.clear();
    }
}
