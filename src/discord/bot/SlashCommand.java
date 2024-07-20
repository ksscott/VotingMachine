package discord.bot;

import elections.games.Game;
import main.Session;
import model.Option;
import model.vote.Vote;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static discord.bot.CommandDataInitializers.*;

public enum SlashCommand {

    //region Commands

    //region Session Management

    NEW_POLL("new", "Begins a new election",
            data -> {
                data.addOption(OptionType.STRING, "prompt", "Write a prompt for this poll");
                data.addOption(OptionType.BOOLEAN, "stored-candidates", "Use stored candidates from a previous poll; defaults to True");
                return data;
            },
            (event, session) -> {
                OptionMapping storedBool = event.getOption("stored-candidates");
                OptionMapping prompt = event.getOption("prompt");
                String promptString = prompt == null ? "" : prompt.getAsString();

                Set<Option> options = null;
                if (storedBool != null && !storedBool.getAsBoolean()) {
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

                event.reply(joiner.toString()).queue();
            }),
    PAST_VOTES("past-votes", "Set whether vote weights from past elections are included in this election",
            ADD_TOGGLES,
            (event, session) -> {
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
            }),
    PICK("pick", "Tally votes and pick the winning game(s)",
            (event, session) -> {
                Set<Option> winners = session.pickWinner();
                Set<Game> winningGames = winners
                        .stream()
                        .map(Option::name)
                        .map(Game::interpret) // FIXME make this agnostic to Games
                        .map(Optional::orElseThrow)
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
            }),
    PLAY("play", "Use this command only once when a game is chosen to record unspent vote weights",
            data -> data.addOption(OptionType.STRING, "game", "The winning game to play", true),
            (event, session) -> {
                String gameString = event.getOption("game").getAsString();
                Option game = session.interpret(gameString).orElse(null);
                if (game == null) {
                    event.reply("Game not recognized").setEphemeral(true).queue();
                    return;
                }
                session.recordUnspentVotes(game);

                // FIXME There's a bug in here somewhere: "Error: null"

                event.reply("Recorded winner of the last election: " + game.name()).queue();
            }),
    RIG("rig", "Cause a game to automatically win the election",
            data -> data.addOption(OptionType.STRING, "game", "The automatically winning game", true),
            (event, session) -> {
                String gameString = event.getOption("game").getAsString();
                Option option = session.interpret(gameString).orElse(null);
                if (option == null) {
                    event.reply("Try again, Mr. Trump").setEphemeral(true).queue();
                    return;
                }

                String username = event.getUser().getEffectiveName();
                event.reply(username + " has rigged the vote for: " + option.name()).queue();
            }),

    //endregion

    //region Candidates

    GAMES_LIST("games", "Lists out the games that we can play",
            (event, session) -> {
                String stringList = session.getOptions()
                        .stream()
                        .map(Option::name)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                event.reply(stringList).setEphemeral(true).queue();
            }),
    CANDIDATES("candidates", "Lists out the candidates in this election",
            (event, session) -> {
                String stringList = session.getOptions()
                        .stream()
                        .map(Option::name)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                event.reply("Candidates:\n"+stringList).setEphemeral(true).queue();
            }),
    STORE_CANDIDATES("store-candidates", "Stores the candidates in this election for future elections",
            (event, session) -> {
                session.storeCandidates();
                event.reply("Stored this election's candidates for future use").queue();
            }),
    SUGGEST("suggest", "Add an option to this election",
            (event, session) -> {
                Modal modal = suggestionModal();

                event.replyModal(modal).queue();
            }),

    //endregion

    //region Voting

    VOTE("vote", "Submit a vote for what game(s) to play",
            data -> {
                data.addOption(OptionType.STRING, "first", "Favorite choice of a game to play", true);
                data.addOption(OptionType.STRING, "second", "Second-favorite choice of a game to play");
                data.addOption(OptionType.STRING, "third", "Third-favorite choice of a game to play");
                data.addOption(OptionType.STRING, "fourth", "Fourth-favorite choice of a game to play");
                data.addOption(OptionType.STRING, "fifth", "Fifth-favorite choice of a game to play");
                return data;
            },
            (event, session) -> {
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
            }),
    RATE("rate", "Build a weighted vote one candidate at at time; accepts integers",
            data -> {
                data.addOption(OptionType.STRING, "candidate", "Candidate to rate", true);
                data.addOption(OptionType.INTEGER, "rating", "Integer rating for this candidate", true);
                return data;
            },
            (event, session) -> {
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
            }),
    VETO("veto", "Cause a game to automatically lose the election",
            data -> data.addOption(OptionType.STRING, "game", "The game to forbid", true),
            (event, session) -> {
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
            }),

    //endregion

    //region Current Vote

    CURRENT_VOTE("current-vote", "View or erase your current vote in this election",
            data -> data.addSubcommands(VIEW_CURR, CLEAR_CURR, WAT),
            (event, session) -> {
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
            }),

    //endregion

    //region Default Vote

    DEFAULT_VOTE("default-vote", "Record your current vote as your default preferred vote for future elections",
            data -> data.addSubcommands(SAVE_DEFAULT, LOAD_DEFAULT, CLEAR_DEFAULT),
            (event, session) -> {
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
            }),

    //endregion

    HELP("help", "Lists out available commands",
            (event, session) -> {
                String message = Arrays.stream(values())
                        .map(command -> "/"+command.slashText+" - "+command.description)
                        .collect(Collectors.joining("\n"));
                event.reply(message).setEphemeral(true).queue();
            }),
    ;

    //endregion

    @NotNull
    private static Modal suggestionModal() {
        TextInput input = TextInput.create("suggestion", "Suggestion", TextInputStyle.SHORT)
                .setPlaceholder("Enter suggestion name here")
                .setMinLength(1)
                .setMaxLength(30)
                .build();

        return Modal.create("suggest", "Suggest")
                .addActionRow(input)
                .build();
    }

    public final String slashText;
    public final String description;
    private final UnaryOperator<SlashCommandData> optionsOperator;
    private final EventHandler eventHandler;

    SlashCommand(String slashText,
                 String description,
                 UnaryOperator<SlashCommandData> optionsOperator,
                 EventHandler eventHandler) {
        this.slashText = slashText;
        this.description = description;
        this.optionsOperator = optionsOperator;
        this.eventHandler = eventHandler;
    }

    SlashCommand(String slashText,
                 String description,
                 EventHandler eventHandler) {
        this(slashText, description, NO_OP, eventHandler);
    }

    public SlashCommandData addOptions(SlashCommandData data) {
        return optionsOperator.apply(data);
    }

    public static void handle(SlashCommandInteractionEvent event, Session session) {
        for (SlashCommand command : values()) {
            if (command.slashText.equalsIgnoreCase(event.getName())) {
                try {
                    command.eventHandler.accept(event, session);
                } catch (Exception e) {
                    event.reply("Command encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }

    /**
     * An implementation of {@link java.util.function.BiConsumer} that doesn't handle its own exceptions.
     * Useful for declaring throwing lambda expressions.
     * <p>
     * Example EventHandler lambda expression:
     * <code>(event, session) -> { event.reply("Command received").queue(); }</code>
     */
    public interface EventHandler {
        void accept(SlashCommandInteractionEvent event, Session session) throws Exception;
    }
}
