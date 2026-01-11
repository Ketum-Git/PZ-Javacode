// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.commands.AltCommandArgs;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclesDB2;

@CommandName(name = "addvehicle")
@AltCommandArgs(
    {
            @CommandArgs(required = "([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", argName = "Script Only"),
            @CommandArgs(
                required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(-?\\d+.*\\d*),(-?\\d+.*\\d*),(-?\\d+.*\\d*)"},
                argName = "Script And Coordinate"
            ),
            @CommandArgs(required = {"([a-zA-Z0-9.-]*[a-zA-Z][a-zA-Z0-9_.-]*)", "(.+)"}, argName = "Script And Player")
    }
)
@CommandHelp(helpText = "UI_ServerOptionDesc_AddVehicle")
@RequiredCapability(requiredCapability = Capability.ManipulateVehicle)
public class AddVehicleCommand extends CommandBase {
    public static final String scriptOnly = "Script Only";
    public static final String scriptPlayer = "Script And Player";
    public static final String scriptCoordinate = "Script And Coordinate";

    public AddVehicleCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String scriptName = this.getCommandArg(0);
        VehicleScript vehicleScriptObj = ScriptManager.instance.getVehicle(scriptName);
        if (vehicleScriptObj == null) {
            return "Unknown vehicle script \"" + scriptName + "\"";
        } else {
            scriptName = vehicleScriptObj.getModule().getName() + "." + vehicleScriptObj.getName();
            int x;
            int y;
            int z;
            if (this.argsName.equals("Script And Player")) {
                String user = this.getCommandArg(1);
                IsoPlayer player = GameServer.getPlayerByUserNameForCommand(user);
                if (player == null) {
                    return "User \"" + user + "\" not found";
                }

                x = PZMath.fastfloor(player.getX());
                y = PZMath.fastfloor(player.getY());
                z = PZMath.fastfloor(player.getZ());
            } else if (this.argsName.equals("Script And Coordinate")) {
                x = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(1)));
                y = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(2)));
                z = PZMath.fastfloor(Float.parseFloat(this.getCommandArg(3)));
            } else {
                if (this.connection == null) {
                    return "Pass a username or coordinate";
                }

                String user = this.getExecutorUsername();
                IsoPlayer player = GameServer.getPlayerByUserNameForCommand(user);
                if (player == null) {
                    return "User \"" + user + "\" not found";
                }

                x = PZMath.fastfloor(player.getX());
                y = PZMath.fastfloor(player.getY());
                z = PZMath.fastfloor(player.getZ());
            }

            if (z > 0) {
                return "Z coordinate must be 0 for now";
            } else {
                IsoGridSquare square = ServerMap.instance.getGridSquare(x, y, z);
                if (square == null) {
                    return "Invalid location " + x + "," + y + "," + z;
                } else {
                    BaseVehicle v = new BaseVehicle(IsoWorld.instance.currentCell);
                    v.setScriptName(scriptName);
                    v.setX(x - 1.0F);
                    v.setY(y - 0.1F);
                    v.setZ(z + 0.2F);
                    if (IsoChunk.doSpawnedVehiclesInInvalidPosition(v)) {
                        v.setSquare(square);
                        v.square.chunk.vehicles.add(v);
                        v.chunk = v.square.chunk;
                        v.addToWorld();
                        VehiclesDB2.instance.addVehicle(v);
                        v.setCurrentKey(v.createVehicleKey());
                        v.repair();
                        if (v.getPassengerDoor(0) != null) {
                            v.getPassengerDoor(0).getDoor().setLocked(false);
                        }

                        return "Vehicle spawned";
                    } else {
                        return "ERROR: I can not spawn the vehicle. Invalid position. Try to change position.";
                    }
                }
            }
        }
    }
}
