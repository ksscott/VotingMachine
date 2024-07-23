package discord.bot;

import discord.bot.events.EventHandler;
import discord.bot.events.SlashEvent;
import main.Session;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

import static discord.bot.CommandDataInitializers.*;
import static discord.bot.events.EventHandler.*;

public enum SlashCommand {

    //region Commands

    //region Session Management

    NEW_POLL("new", "Begins a new election",
            data -> {
                data.addOption(OptionType.STRING, "prompt", "Write a prompt for this poll");
                data.addOption(OptionType.BOOLEAN, "stored-candidates", "Use stored candidates from a previous poll; defaults to True");
                return data;
            },
            NEW_POLL_HANDLER),
    PAST_VOTES("past-votes", "Set whether vote weights from past elections are included in this election",
            ADD_TOGGLES,
            PAST_VOTES_HANDLER),
    PICK("pick", "Tally votes and pick the winning game(s)",
            PICK_HANDLER),
    PLAY("play", "Use this command only once when a game is chosen to record unspent vote weights",
            PLAY_HANDLER),
    RIG("rig", "Cause a game to automatically win the election",
            data -> data.addOption(OptionType.STRING, "game", "The automatically winning game", true),
            RIG_HANDLER),

    //endregion

    //region Candidates

    GAMES_LIST("games", "Lists out the games that we can play",
            GAMES_LIST_HANDLER),
    CANDIDATES("candidates", "Lists out the candidates in this election",
            data -> data.addSubcommands(VIEW_CANDIDATES, SAVE_CANDIDATES, SUGGEST_CANDIDATES),
            CANDIDATES_HANDLER),

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
            VOTE_HANDLER),
    RATE("rate", "Build a weighted vote one candidate at at time; accepts integers",
            data -> {
                data.addOption(OptionType.STRING, "candidate", "Candidate to rate", true);
                data.addOption(OptionType.INTEGER, "rating", "Integer rating for this candidate", true);
                return data;
            },
            RATE_HANDLER),
    VETO("veto", "Cause a game to automatically lose the election",
            data -> data.addOption(OptionType.STRING, "game", "The game to forbid", true),
            VETO_HANDLER),

    //endregion

    //region Current Vote

    CURRENT_VOTE("current-vote", "View or erase your current vote in this election",
            data -> data.addSubcommands(VIEW_CURR, CLEAR_CURR, WAT),
            CURRENT_VOTE_HANDLER),

    //endregion

    //region Default Vote

    DEFAULT_VOTE("default-vote", "Record your current vote as your default preferred vote for future elections",
            data -> data.addSubcommands(SAVE_DEFAULT, LOAD_DEFAULT, CLEAR_DEFAULT),
            DEFAULT_VOTE_HANDLER),

    //endregion

    HELP("help", "Lists out available commands",
            HELP_HANDLER),
    ;

    //endregion

    public final String slashText;
    public final String description;
    private final UnaryOperator<SlashCommandData> optionsOperator;
    private final EventHandler eventHandler;

    SlashCommand(@NotNull String slashText,
                 @NotNull String description,
                 @NotNull UnaryOperator<SlashCommandData> optionsOperator,
                 @NotNull EventHandler eventHandler) {
        this.slashText = slashText;
        this.description = description;
        this.optionsOperator = optionsOperator;
        this.eventHandler = eventHandler;
    }

    SlashCommand(@NotNull String slashText,
                 @NotNull String description,
                 @NotNull EventHandler eventHandler) {
        this(slashText, description, NO_OP, eventHandler);
    }

    public SlashCommandData addOptions(SlashCommandData data) {
        return optionsOperator.apply(data);
    }

    public static void handle(@NotNull SlashCommandInteractionEvent event, @NotNull Session session) {
        for (SlashCommand command : values()) {
            if (command.slashText.equalsIgnoreCase(event.getName())) {
                try {
                    command.eventHandler.accept(new SlashEvent(event), session);
                } catch (Exception e) {
                    event.reply("Command encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }
}
