package live.supeer.metropolis.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import live.supeer.metropolis.Metropolis;

@CommandAlias("here")
public class CommandHere extends BaseCommand {
    public static Metropolis plugin;

    @Default
    public void onHere() {
        // Code here
    }
}
