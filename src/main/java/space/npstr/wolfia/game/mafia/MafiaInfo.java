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

package space.npstr.wolfia.game.mafia;

import net.dv8tion.jda.core.Permission;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.game.CharakterSetup;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.definitions.Scope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by napster on 02.07.17.
 * <p>
 * Static information about the mafia game
 */
public class MafiaInfo implements GameInfo {

    @Override
    public List<GameMode> getSupportedModes() {
        final List<GameMode> supportedModes = new ArrayList<>();
        supportedModes.add(GameMode.LITE); //village vs wolfs, there is a seer in the game
//        supportedModes.add(GameMode.PURE); //no power roles, just wolfs vs villagers
        return supportedModes;
    }

    @Override
    public GameMode getDefaultMode() {
        return GameMode.LITE;
    }

    @Override
    public Map<Permission, Scope> getRequiredPermissions(final GameMode mode) {
        final Map<Permission, Scope> requiredPermissions = new LinkedHashMap<>();
        requiredPermissions.put(Permission.MESSAGE_EMBED_LINKS, Scope.CHANNEL);
        requiredPermissions.put(Permission.MESSAGE_EXT_EMOJI, Scope.CHANNEL);
        requiredPermissions.put(Permission.MESSAGE_ADD_REACTION, Scope.CHANNEL);
        requiredPermissions.put(Permission.MESSAGE_HISTORY, Scope.CHANNEL);
        switch (mode) {
            case LITE:
            case PURE:
                requiredPermissions.put(Permission.MESSAGE_MANAGE, Scope.CHANNEL); //to delete reactions
                requiredPermissions.put(Permission.MANAGE_PERMISSIONS, Scope.CHANNEL);
            default:
                break;
        }
        return requiredPermissions;
    }

    @Override
    public String getAcceptablePlayerNumbers(final GameMode mode) {
        return "9+";
    }

    @Override
    public boolean isAcceptablePlayerCount(final int playerCount, final GameMode mode) {
        if (Config.C.isDebug && playerCount == 4) return true;
        return 8 < playerCount;
    }

    /**
     * @return a character setup for the provided mode and player count, never null
     */
    @Override
    public CharakterSetup getCharacterSetup(final GameMode mode, final int playerCount) {

        if (Config.C.isDebug && playerCount == 4) return new CharakterSetup()
                .addRoleAndAlignment(Alignments.VILLAGE, Roles.COP, 3)
                .addRoleAndAlignment(Alignments.WOLF, Roles.VANILLA);

        if (!isAcceptablePlayerCount(playerCount, mode)) {
            throw new IllegalArgumentException(String.format(
                    "There is no possible character setup for the provided player count %s in this game %s mode %s",
                    playerCount, Games.POPCORN.textRep, mode.textRep)
            );
        }
//        final Supplier<IllegalArgumentException> t = () -> new IllegalArgumentException(String.format(
//                "There is no character set up for game %s, mode %s and player count %s",
//                Games.MAFIA.textRep, mode.name(), playerCount));

        if (mode == GameMode.LITE) {
            //source: https://weebs.are-la.me/be281b.png
            //https://docs.google.com/spreadsheets/d/1IYcGg4GK9na9JGk-mqlJ7Zfbsah0BJKxiN3pKPJpCPU/edit#gid=0
            final int cops = Math.max(1, (int) Math.floor(playerCount / 10.0));
            final int mafias = (int) Math.floor((playerCount - cops) / 4.0);
            final int towns = playerCount - (cops + mafias);

            return new CharakterSetup()
                    .addRoleAndAlignment(Alignments.VILLAGE, Roles.COP, cops)
                    .addRoleAndAlignment(Alignments.VILLAGE, Roles.VANILLA, towns)
                    .addRoleAndAlignment(Alignments.WOLF, Roles.VANILLA, mafias);
        } else {
            throw new IllegalArgumentException("Unsupported game mode: " + mode.textRep);
        }
    }

    @Override
    public String textRep() {
        return Games.MAFIA.textRep;
    }

    @Override
    public Games getGameType() {
        return Games.MAFIA;
    }
}
