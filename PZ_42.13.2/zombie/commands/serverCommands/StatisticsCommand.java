// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.network.statistics.StatisticManager;
import zombie.network.statistics.data.Statistic;

@CommandName(name = "stats")
@CommandArgs(required = "(.+)", optional = "(.+)")
@CommandHelp(helpText = "UI_ServerOptionDesc_Statistics")
@RequiredCapability(requiredCapability = Capability.GetStatistic)
public class StatisticsCommand extends CommandBase {
    public StatisticsCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    @Override
    protected String Command() {
        if (this.getCommandArgsCount() == 1) {
            String application = this.getCommandArg(0);
            if ("list".equals(application)) {
                StringBuilder stringBuilder = new StringBuilder();

                for (Statistic statistic : StatisticManager.getInstance()) {
                    stringBuilder.append(statistic.getName()).append(", ");
                }

                return stringBuilder.toString();
            }

            if ("version".equals(application)) {
                return Core.getInstance().getVersion();
            }
        } else if (this.getCommandArgsCount() == 2) {
            String applicationx = this.getCommandArg(0);
            String counter = this.getCommandArg(1);
            Statistic statistic = StatisticManager.getInstance().get(applicationx);
            if (statistic != null) {
                String value = null;

                value = switch (counter) {
                    case "list" -> statistic.getList();
                    case "all" -> statistic.getAll();
                    default -> statistic.getValue(counter);
                };
                if (value != null) {
                    return value;
                }
            }
        }

        return this.getHelp();
    }
}
