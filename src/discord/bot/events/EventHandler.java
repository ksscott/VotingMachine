package discord.bot.events;

import discord.bot.ButtonWrapper;
import discord.bot.ModalWrapper;
import discord.bot.SlashCommand;
import elections.games.Game;
import main.Session;
import model.Option;
import model.vote.Vote;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static discord.bot.CommandDataInitializers.*;

/**
 * An implementation of {@link java.util.function.BiConsumer} that doesn't handle its own exceptions.
 * Useful for declaring throwing lambda expressions.
 * <p>
 * Example EventHandler lambda expression:
 * <code>(event, session) -> { event.reply("Command received").queue(); }</code>
 */
public interface EventHandler {
    void accept(@NotNull EventWrapper event, @NotNull Session session) throws Exception;

    //region SlashCommands

    EventHandler NEW_POLL_HANDLER = (event, session) -> {
        String promptString = "";
        boolean stored = true;
        try {
            OptionMapping storedBool = event.getOption("stored-candidates");
            OptionMapping prompt = event.getOption("prompt");
            promptString = prompt == null ? "" : prompt.getAsString();
            if (storedBool != null) stored = storedBool.getAsBoolean();
        } catch (UnsupportedOperationException e) { /* NO OP */ }

        Set<Option> options = null;
        if (!stored) {
            // Default to list of Games to play
            // Other behavior is possible here, including starting an empty election
            options = Game.shortList().stream()
                    .map(Game::getTitle)
                    .map(Option::new)
                    .collect(Collectors.toSet());
        }
        session.startElection(promptString, options);

        StringJoiner joiner = new StringJoiner("\n");

        joiner.add("New poll started!");
        if (!promptString.equals("")) joiner.add(promptString);
        joiner.add("Type /vote to vote for a list of options.");
        joiner.add("Type /rate to build a vote by rating one option at a time.");
        joiner.add("Type /candidates to see a list of all candidates.");

        event.reply(joiner.toString())
                .addActionRow(
                        ButtonWrapper.VIEW_CANDIDATES.button(),
                        ButtonWrapper.PAST_VOTES.button(),
                        ButtonWrapper.PICK.button(),
                        ButtonWrapper.PLAY.button(),
                        ButtonWrapper.HELP.button()
                )
                .queue();
    };

    EventHandler PAST_VOTES_HANDLER = (event, session) -> {
        boolean result;
        String name = event.getSubcommandName();
        if (name == null) name = "";

        switch (name) {
            case TOGGLE_NAME -> result = session.toggleIncludeShadow();
            case TOGGLE_ON_NAME -> result = session.setIncludeShadow(true);
            case TOGGLE_OFF_NAME -> result = session.setIncludeShadow(false);
            default -> {
                event.reply("Unknown subcommand executed").queue();
                return;
            }
        }

        String username = event.getUser().getEffectiveName();
        event.reply(username + " toggled " + (result ? "ON" : "OFF") + " past vote weights.").queue();
    };

    EventHandler PICK_HANDLER = (event, session) -> {
        Set<Option> winners = session.pickWinner();
        Set<Game> winningGames = winners
                .stream()
                .map(Option::name)
                .map(Game::interpret) // FIXME make this agnostic to Games
                .map(o -> o.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String winnersString = winners
                .stream()
                .map(option -> "**" + option.name() + "**")
                .sorted()
                .collect(Collectors.joining(", and "));

        int numVoters = session.numVoters();
        String warning = winningGames
                .stream()
                .filter(game -> game.getMaxPlayers() < numVoters && game.getMaxPlayers() > 0)
                .map(game -> "\n*Warning:* " + game.getTitle() + " has a maximum number of players of " + game.getMaxPlayers())
                .collect(Collectors.joining());

        event.reply("The winner is: " + winnersString + warning).queue();
        File resultsFile = Paths.get("./data/flowplot.png").toFile(); // FIXME hard coded
        event.getChannel().sendFiles(FileUpload.fromData(resultsFile)).queue();
    };

    EventHandler PLAY_HANDLER = (event, session) -> event.replyModal(ModalWrapper.PLAY.modal()).queue();

    EventHandler RIG_HANDLER = (event, session) -> {
        String gameString = event.getOption("game").getAsString();
        Option option = session.interpret(gameString).orElse(null);
        if (option == null) {
            event.reply("Try again, Mr. Trump").setEphemeral(true).queue();
            return;
        }

        String username = event.getUser().getEffectiveName();
        event.reply(username + " has rigged the vote for: " + option.name()).queue();
    };

    EventHandler GAMES_LIST_HANDLER = (event, session) -> {
        String stringList = session.getOptions()
                .stream()
                .map(Option::name)
                .sorted()
                .collect(Collectors.joining("\n"));
        event.reply(stringList).setEphemeral(true).queue();
    };

    EventHandler CANDIDATES_HANDLER = (event, session) -> {
        String name = event.getSubcommandName();
        if (name == null) name = "";

        switch (name) {
            case VIEW_NAME -> {
                String stringList = session.getOptions()
                        .stream()
                        .map(Option::name)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                event.reply("Candidates:\n"+stringList).setEphemeral(true).queue();
            }
            case SAVE_NAME -> {
                session.storeCandidates();
                event.reply("Stored this election's candidates for future use").queue();
            }
            case SUGGEST_NAME -> {
                event.replyModal(ModalWrapper.SUGGEST.modal()).queue();
            }
            default -> {
                event.reply("Unknown subcommand executed").queue();
                return;
            }
        }
    };

    EventHandler VOTE_HANDLER = (event, session) -> {
        String username = event.getUser().getEffectiveName();
        List<Option> gamesList = event.getOptions()
                .stream()
                .map(OptionMapping::getAsString)
                .map(session::interpret)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        session.addVote(username, gamesList);

        String namesList = "";
        int i=1;
        for (Option option : gamesList) {
            namesList += i++ + ". " + option.name() + "\n";
        }
        event.reply(username + " submitted a ranked vote for:\n" + String.join("\n", namesList))/*.setEphemeral(true)*/.queue();
    };

    EventHandler RATE_HANDLER = (event, session) -> {
        String username = event.getUser().getEffectiveName();
        String gameString = event.getOption("candidate").getAsString();
        Option option = session.interpret(gameString).orElse(null);
        if (option == null) {
            event.reply("Game not recognized: " + gameString).setEphemeral(true).queue();
            return;
        }
        Integer rating = event.getOption("rating").getAsInt();

        session.rate(username, option, rating);

        event.reply(username + " rated option: " + option.name() + " -> " + rating)/*.setEphemeral(true)*/.queue();
    };

    EventHandler VETO_HANDLER = (event, session) -> {
        String gameString = event.getOption("game").getAsString();
        Option option = session.interpret(gameString).orElse(null);
        if (option == null) {
            event.reply("Game not recognized").setEphemeral(true).queue();
            return;
        }

        String username = event.getUser().getEffectiveName();
        boolean isVetoed = session.veto(username, option);
        String vetoed = isVetoed ? " vetoed" : " UN-vetoed";
        event.reply(username + vetoed + " the game: " + option.name()).queue();
    };

    EventHandler CURRENT_VOTE_HANDLER = (event, session) -> {
        String username = event.getUser().getEffectiveName();

        String message;

        String name = event.getSubcommandName();
        if (name == null) name = "";

        switch (name) {
            case VIEW_NAME, WAT_NAME -> {
                Vote vote = session.getVote(username);
                if (vote != null) {
                    message = "Your current vote is:\n" + vote;
                } else {
                    message = "You haven't cast a vote in the current election. \n" +
                            "Type /vote to vote for a list of games. \n" +
                            "Type /rate to build a vote by rating one game at a time.";
                }
            }
            case CLEAR_NAME -> {
                session.clearCurrentVote(username);
                message = "Cleared current vote for " + username;
            }
            default -> {
                event.reply("Unknown subcommand executed").queue();
                return;
            }
        }


        event.reply(message).setEphemeral(true).queue();
    };

    EventHandler DEFAULT_VOTE_HANDLER = (event, session) -> {
        String username = event.getUser().getEffectiveName();

        String message;

        String name = event.getSubcommandName();
        if (name == null) name = "";

        switch (name) {
            case SAVE_NAME -> {
                session.saveDefaultVote(username);
                message = "Default vote saved for " + username;
            }
            case LOAD_NAME -> {
                session.loadDefaultVote(username);
                message = "Default vote loaded for " + username;
            }
            case CLEAR_NAME -> {
                session.clearDefaultVote(username);
                message = "Cleared default vote for " + username;
            }
            default -> {
                event.reply("Unknown subcommand executed").queue();
                return;
            }
        }

        event.reply(message).queue();
    };

    EventHandler HELP_HANDLER = (event, session) -> {
        String message = Arrays.stream(SlashCommand.values())
                .map(command -> "/"+command.slashText+" - "+command.description)
                .collect(Collectors.joining("\n"));
        event.reply(message).setEphemeral(true).queue();
    };

    //endregion

    //region Buttons

    EventHandler PAST_VOTES_BUTTON_HANDLER = (event, session) -> { // FIXME duplicated
        boolean result = session.toggleIncludeShadow();
        String username = event.getUser().getEffectiveName();
        event.reply(username + " toggled " + (result ? "ON" : "OFF") + " past vote weights.").queue();
    };

    EventHandler CANDIDATES_VIEW_BUTTON_HANDLER = (event, session) -> { // FIXME duplicated
        String stringList = session.getOptions()
                .stream()
                .map(Option::name)
                .sorted()
                .collect(Collectors.joining("\n"));
        event.reply("Candidates:\n"+stringList).setEphemeral(true).queue();
    };

    //endregion

    //region Modals

    EventHandler SUGGEST_MODAL_HANDLER = (event, session) -> {
        String username = event.getUser().getEffectiveName();
        String gameString = event.getValues().get(0).getAsString();

        session.suggest(new Option(gameString));

        event.reply(":right_arrow: " + username + " suggested: " + gameString).queue();
    };

    EventHandler PLAY_MODAL_HANDLER = (event, session) -> {
        String gameString = event.getValues().get(0).getAsString();
        Option game = session.interpret(gameString).orElse(null);
        if (game == null) {
            event.reply("Game not recognized").setEphemeral(true).queue();
            return;
        }
        session.recordUnspentVotes(game);

        // FIXME There's a bug in here somewhere: "Error: null"

        event.reply("Recorded winner of the last election: " + game.name()).queue();
    };

    //endregion
}
