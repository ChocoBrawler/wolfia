/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.commands.debug;

import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.game.definitions.Games;

/**
 * Created by napster on 28.05.17.
 */
public class RestartCommand extends BaseCommand implements IOwnerRestricted {

    public RestartCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    private boolean reminded = false;

    @Override
    public String help() {
        return "Restart the bot.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {

        if (Wolfia.isShuttingDown()) {
            commandInfo.replyWithName(String.format("restart has been queued already! **%s** games still running.",
                    Games.getRunningGamesCount()));
            return false;
        }

        if (!this.reminded) {
            Wolfia.handleOutputMessage(commandInfo.event.getTextChannel(),
                    "%s, you have fucked up in the past so here's a reminder:" +
                            "\n - Did you update the config files?" +
                            "\n - Any database migration necessary/implemented?" +
                            "\n - Did you actually upload the updated code?" +
                            "\nJust run the command again if you're sure you have done everything." +
                            "\n\n_Yours, %s_", commandInfo.event.getAuthor().getAsMention(), commandInfo.event.getJDA().getSelfUser().getName());
            this.reminded = true;
            return false;
        }

        Wolfia.handleOutputMessage(true, commandInfo.event.getTextChannel(),
                "%s, **%s** games are still running. Will restart as soon as they are over.",
                commandInfo.event.getAuthor().getAsMention(), Games.getRunningGamesCount());

        new Thread(() -> Wolfia.shutdown(Wolfia.EXIT_CODE_RESTART), "shutdown-thread").start();
        return true;
    }
}
