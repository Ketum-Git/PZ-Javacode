// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@CommandName(name = "addxp")
@CommandArgs(required = {"(.+)", "(\\S+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_AddXp")
@RequiredCapability(requiredCapability = Capability.AddXP)
public class AddXPCommand extends CommandBase {
    public AddXPCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        String user = this.getCommandArg(0);
        String perks = this.getCommandArg(1);
        IsoPlayer p = GameServer.getPlayerByUserNameForCommand(user);
        if (p == null) {
            return "No such user";
        } else {
            String username = p.getDisplayName();
            String perk = null;
            float xp = 0.0F;
            String[] perkAndXp = perks.split("=", 2);
            if (perkAndXp.length != 2) {
                return this.getHelp();
            } else {
                perk = perkAndXp[0].trim();
                if (PerkFactory.Perks.FromString(perk) == PerkFactory.Perks.MAX) {
                    String nl = this.connection == null ? "\n" : " LINE ";
                    StringBuilder sb = new StringBuilder();

                    for (int i = 0; i < PerkFactory.PerkList.size(); i++) {
                        if (PerkFactory.PerkList.get(i) != PerkFactory.Perks.Passiv) {
                            sb.append(PerkFactory.PerkList.get(i));
                            if (i < PerkFactory.PerkList.size()) {
                                sb.append(nl);
                            }
                        }
                    }

                    return "List of available perks :" + nl + sb.toString();
                } else {
                    try {
                        xp = Float.parseFloat(perkAndXp[1]);
                    } catch (NumberFormatException var11) {
                        return this.getHelp();
                    }

                    if (p != null) {
                        UdpConnection c = GameServer.getConnectionFromPlayer(p);
                        if (c != null) {
                            INetworkPacket.send(c, PacketTypes.PacketType.AddXP, p, PerkFactory.Perks.FromString(perk), xp);
                            LoggerManager.getLogger("admin").write(this.getExecutorUsername() + " added " + xp + " " + perk + " xp's to " + username);
                            return "Added " + xp + " " + perk + " xp's to " + username;
                        }
                    }

                    return "User " + username + " not found.";
                }
            }
        }
    }
}
