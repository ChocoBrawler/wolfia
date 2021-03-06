package space.npstr.wolfia.events;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.db.entities.stats.CommandStats;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import java.util.concurrent.TimeUnit;

/**
 * Listens for commands in a private channels. Current limitation: Playing more than one game there is no way to limit
 * the issuing of a commands that is valid for both games to only one of them.
 */
public class PrivateChannelListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PrivateChannelListener.class);

    private final long userId;
    private final GameCommand allowedCommand;
    private final Game game;

    public PrivateChannelListener(final long userId, final long selfDestructMillis, final Game game, final GameCommand allowedCommand) {
        this.userId = userId;
        this.allowedCommand = allowedCommand;
        this.game = game;

        Wolfia.addEventListener(this);
        Wolfia.executor.schedule(() -> Wolfia.removeEventListener(this), selfDestructMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        final long received = System.currentTimeMillis();

        //ignore any channels what arent the one we are looking for
        if (event.getPrivateChannel() == null || event.getPrivateChannel().getUser().getIdLong() != this.userId) {
            return;
        }

        //ignore messages not starting with the prefix (prefix is accepted case insensitive)
        final String raw = event.getMessage().getRawContent();
        if (!raw.toLowerCase().startsWith(Config.PREFIX.toLowerCase())) {
            return;
        }

        //ignore bot accounts generally
        if (event.getAuthor().isBot()) {
            return;
        }

        //bot should ignore itself
        if (event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        final CommandParser.CommandContainer commandInfo = CommandParser.parse(raw, event, received);

        //todo find a way to unify private channel commands in the CommandHandler
        if (this.allowedCommand.isCommandTrigger(commandInfo.command)) {
            log.info("user {}, channel {}, command {}", event.getAuthor().getIdLong(), event.getChannel().getIdLong(), event.getMessage().getRawContent());
            Wolfia.executor.submit(() -> {
                boolean success = false;
                try {
                    success = this.game.issueCommand(this.allowedCommand, commandInfo);
                } catch (final IllegalGameStateException e) {
                    Wolfia.handleOutputMessage(commandInfo.event.getChannel(), "%s", e.getMessage());
                }
                final long executed = System.currentTimeMillis();
                try {
                    Wolfia.getDbWrapper().persist(new CommandStats(commandInfo, this.allowedCommand.getClass(), executed, success));
                } catch (final DatabaseException e) {
                    log.error("Db blew up saving command stats", e);
                }
            });
        }
    }
}
