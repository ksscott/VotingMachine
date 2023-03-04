package discord.bot;

import elections.games.Game;
import main.Session;
import model.Option;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public enum SlashCommand {
//    PING("ping", "Calculate ping of the bot", (event) -> {
//        long time = System.currentTimeMillis();
//        event.reply("Pong!").setEphemeral(true) // reply or acknowledge
//                .flatMap(v ->
//                        event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
//                ).queue();
//    }),
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
    SUGGEST("suggest", "Add an option to this election",
            data -> data.addOption(OptionType.STRING, "game", "The game to suggest", true),
            (event, session) -> {
                String gameString = event.getOption("game").getAsString();

                String username = event.getUser().getName();
                session.suggest(new Option(gameString));
                event.reply(username + " suggested the game: " + gameString).queue();
            }),
    NEW_POLL("new", "Begins a new election",
            data -> data,
            (event, session) -> {
                session.startElection();

                String message = "New poll started! \n" +
                        "Type /vote to vote for a list of games. \n" +
                        "Type /rate to build a vote by rating one game at a time. \n" +
                        "Type /games to see a list of options.";

                event.reply(message).queue();
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
                String username = event.getUser().getName();
                List<Game> gamesList = event.getOptions()
                        .stream()
                        .map(OptionMapping::getAsString)
                        .map(Game::interpret)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                session.addVote(username, gamesList);

                String namesList = gamesList.stream().map(Game::getTitle).collect(Collectors.joining("\n"));
                event.reply("Voted for game: " + String.join("\n", namesList))/*.setEphemeral(true)*/.queue();
                event.getChannel().sendMessage(username + " voted.").queue();
            }),
    RATE("rate", "Build a weighted vote one game at at time; accepts integers",
            data -> {
                data.addOption(OptionType.STRING, "game", "Game to rate", true);
                data.addOption(OptionType.INTEGER, "rating", "Integer rating for this game", true);
                return data;
            },
            (event, session) -> {
                String username = event.getUser().getName();
                String gameString = event.getOption("game").getAsString();
                Option option = session.interpret(gameString).orElse(null);
                Integer rating = event.getOption("rating").getAsInt();

                session.rate(username, option, rating);

                event.reply("Rated game: " + option.name() + " -> " + rating)/*.setEphemeral(true)*/.queue();
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

                String username = event.getUser().getName();
                session.veto(username, option);
                event.reply(username + " vetoed the game: " + option.name()).queue();
            }),
    PICK("pick", "Tally votes and pick the winning game(s)",
            data -> data,
            (event, session) -> {
                String winnersString = session.pickWinner()
                                .stream()
                                .map(Option::name)
                                .sorted()
                                .collect(Collectors.joining(", and "));
                event.reply("The winner is: " + winnersString).queue();
            }),
    SAVE("save", "Record your current vote as your default preferred vote for future elections",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                session.saveDefaultVote(username);
                event.reply("Default vote saved for " + username).queue();
            }),
    LOAD("load", "Load your default preferred vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                session.loadDefaultVote(username);
                event.reply("Default vote loaded for " + username).queue();
            }),
    CLEAR("clear", "Clear your current vote in this election",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                session.clearCurrentVote(username);
                event.reply("Cleared current vote for " + username).queue();
            }),
    CLEAR_DEFAULT("clear-default", "Clear your recorded default vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                session.clearDefaultVote(username);
                event.reply("Cleared default vote for " + username).queue();
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
