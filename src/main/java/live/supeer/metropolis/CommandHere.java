package live.supeer.metropolis;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;

@CommandAlias("here")
public class CommandHere extends BaseCommand {
    static Metropolis plugin;

    @Default
    public void onHere() {
        // Code here
    }
}
