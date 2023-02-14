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
import java.util.function.BiConsumer;
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
            List<Game> games = Game.shortList();
            String stringList = games
                    .stream()
                    .map(Game::getTitle)
                    .sorted()
                    .collect(Collectors.joining("\n"));
            event.reply(stringList).setEphemeral(true) // reply or acknowledge
                    .queue();
        }),
    NEW_POLL("new", "Begins a new election",
            data -> data,
            (event, session) -> {
                session.startElection();

                String message = "New poll started! Type /vote to vote. Type '/games' to see a list of options.";

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

                try {
                    session.addVote(username, gamesList);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }

                String namesList = gamesList.stream().map(Game::getTitle).collect(Collectors.joining("\n"));
                event.reply("Voted for game: " + String.join("\n", namesList))/*.setEphemeral(true)*/.queue();
                event.getChannel().sendMessage(username + " voted.").queue();
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
    private final BiConsumer<SlashCommandInteractionEvent, Session> eventHandler;

    SlashCommand(String slashText,
                 String description,
                 UnaryOperator<SlashCommandData> optionsOperator,
                 BiConsumer<SlashCommandInteractionEvent, Session> eventHandler) {
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
                command.eventHandler.accept(event, session);
                break;
            }
        }
    }
}
