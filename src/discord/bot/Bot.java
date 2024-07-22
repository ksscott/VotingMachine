package discord.bot;

import main.Session;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static Session session;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Include bot token as argument");
            System.exit(1);
        }
        // args[0] would be the token (using an environment variable or config file is preferred for security)
        // We don't need any intents for this bot. Slash commands work without any intents!
        JDA jda = JDABuilder.createLight(args[0], Collections.emptyList())
                .addEventListeners(new Bot())
                .setActivity(Activity.listening("The People's Votes"))
                .build();

        // Sets the global command list to the provided commands (removing all others)
        List<SlashCommandData> commandList = Arrays.stream(SlashCommand.values())
                .map(c -> c.addOptions(Commands.slash(c.slashText, c.description)))
                .collect(Collectors.toList());
        jda.updateCommands().addCommands(commandList).queue();

        session = new Session();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getUser().isBot()) {
            return; // Don't talk with other bots
        } else if (!event.getChannel().getName().equalsIgnoreCase("bot-commands")) {
            return; // Only works in the bot-commands channel
        }

        SlashCommand.handle(event, session);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        ButtonWrapper.handle(event, session);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        ModalWrapper.handle(event, session);
    }

}
