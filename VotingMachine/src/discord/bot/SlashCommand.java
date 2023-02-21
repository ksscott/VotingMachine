package discord.bot;

import elections.games.Game;
import main.Session;
import model.Option;
import model.EvaluationResult;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.javatuples.*;

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
    RATE("rate", "Build a weighted vote one game at at time; accepts integers",
            data -> {
                data.addOption(OptionType.STRING, "game", "Game to rate", true);
                data.addOption(OptionType.INTEGER, "rating", "Integer rating for this game", true);
                return data;
            },
            (event, session) -> {
                String username = event.getUser().getName();
                String gameString = event.getOption("game").getAsString();
                Game game = Game.interpret(gameString).orElse(null);
                Integer rating = event.getOption("rating").getAsInt();

                try {
                    session.rate(username, game, rating);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }

                event.reply("Rated game: " + game.getTitle() + " -> " + rating)/*.setEphemeral(true)*/.queue();
                event.getChannel().sendMessage(username + " voted.").queue();
            }),
    PICK("pick", "Tally votes and pick the winning game(s)",
            data -> data,
            (event, session) -> {
                List<Triplet<EvaluationResult,Double,Set<Option>>> roundresults = session.pickWinner();

                StringBuffer winnersString = new StringBuffer();
                for (Triplet<EvaluationResult,Double,Set<Option>> result : roundresults) {
                    if (result.getValue0() == EvaluationResult.WINNERS) {
                        winnersString.append("WINNERS(score≤"+result.getValue1().toString()+"): " + result.getValue2().stream().map(Option::name).sorted().collect(Collectors.joining(", and "))+"\n");
                    } else {
                        winnersString.append("ELIMINATED(score<"+result.getValue1().toString()+"): " + result.getValue2().stream().map(Option::name).sorted().collect(Collectors.joining(", and "))+"\n");
                    }
                }

                // String winnersString = session.pickWinner()
                //                 .stream()
                //                 .map(Option::name)
                //                 .sorted()
                //                 .collect(Collectors.joining(", and "));
                event.reply("Election Results:\n" + winnersString).queue();
            }),
    SAVE("save", "Record your current vote as your default preferred vote for future elections",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                try {
                    session.saveDefaultVote(username);
                } catch (Exception e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                event.reply("Default vote saved for " + username).queue();
            }),
    LOAD("load", "Load your default preferred vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                try {
                    session.loadDefaultVote(username);
                } catch (Exception e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                event.reply("Default vote loaded for " + username).queue();
            }),
    CLEAR("clear", "Clear your current vote in this election",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                try {
                    session.clearCurrentVote(username);
                } catch (Exception e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                event.reply("Cleared current vote for " + username).queue();
            }),
    CLEAR_DEFAULT("clear-default", "Clear your recorded default vote",
            data -> data,
            (event, session) -> {
                String username = event.getUser().getName();
                try {
                    session.clearDefaultVote(username);
                } catch (Exception e) {
                    event.reply("Error: " + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                event.reply("Cleared default vote for " + username).queue();
            }),
    HELP("help", "Lists out available commands",
            data -> data,
            (event, session) -> {
                System.out.println("help command");
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
