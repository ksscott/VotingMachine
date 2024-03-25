package discord.bot;

import elections.games.Game;
import main.Session;
import model.Option;
import model.vote.Vote;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public enum SlashCommand {
    GAMES_LIST("games", "Lists out the games that we can play",
            data -> data,
            (event, session) -> {
                String stringList = session.getOptions()
                        .stream()
                        .map(Option::name)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                event.reply(stringList).setEphemeral(true).queue();
            }),
    CANDIDATES("candidates", "Lists out the candidates in this election",
            data -> data,
            (event, session) -> {
                String stringList = session.getOptions()
                        .stream()
                        .map(Option::name)
                        .sorted()
                        .collect(Collectors.joining("\n"));
                event.reply("Candidates:\n"+stringList).setEphemeral(true).queue();
            }),
    STORE_CANDIDATES("store-candidates", "Stores the candidates in this election for future elections",
            data -> data,
            (event, session) -> {
                session.storeCandidates();
                event.reply("Stored this election's candidates for future use").queue();
            }),
    SUGGEST("suggest", "Add an option to this election",
            data -> data.addOption(OptionType.STRING, "game", "The game to suggest", true),
            (event, session) -> {
                String gameString = event.getOption("game").getAsString();

                String username = event.getUser().getEffectiveName();
                session.suggest(new Option(gameString));
                event.reply(username + " suggested: " + gameString).queue();
            }),
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
    PICK("pick", "Tally votes and pick the winning game(s)",
            data -> data,
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
    SAVE("save", "Record your current vote as your default preferred vote for future elections",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                session.saveDefaultVote(username);
                event.reply("Default vote saved for " + username).queue();
            }),
    LOAD("load", "Load your default preferred vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                session.loadDefaultVote(username);
                event.reply("Default vote loaded for " + username).queue();
            }),
    CURRENT_VOTE("current-vote", "List your current vote in this election",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                Vote vote = session.getVote(username);
                String message;
                if (vote != null) {
                    message = "Your current vote is:\n" + vote;
                } else {
                    message = "You haven't cast a vote in the current election. \n" +
                            "Type /vote to vote for a list of games. \n" +
                            "Type /rate to build a vote by rating one game at a time.";
                }
                event.reply(message).setEphemeral(true).queue();
            }),
    WAT("wat", "List your current vote in this election",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                Vote vote = session.getVote(username);
                String message;
                if (vote != null) {
                    message = "Your current vote is:\n" + vote;
                } else {
                    message = "You haven't cast a vote in the current election. \n" +
                            "Type /vote to vote for a list of games. \n" +
                            "Type /rate to build a vote by rating one game at a time.";
                }
                event.reply(message).setEphemeral(true).queue();
            }),
    CLEAR("clear", "Clear your current vote in this election",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                session.clearCurrentVote(username);
                event.reply("Cleared current vote for " + username).queue();
            }),
    CLEAR_DEFAULT("clear-default", "Clear your recorded default vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                session.clearDefaultVote(username);
                event.reply("Cleared default vote for " + username).queue();
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
    HELP("help", "Lists out available commands",
            data -> data,
            (event, session) -> {
                String message = Arrays.stream(values())
                        .map(command -> "/"+command.slashText+" - "+command.description)
                        .collect(Collectors.joining("\n"));
                event.reply(message).setEphemeral(true).queue();
            }),
    ;

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
