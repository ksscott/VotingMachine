package discord.bot.events;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlashEvent implements EventWrapper {

    private final SlashCommandInteractionEvent event;

    public SlashEvent(SlashCommandInteractionEvent event) {
        this.event = event;
    }

    @Override
    public ReplyCallbackAction reply(@NotNull String content) { return event.reply(content); }

    @Override
    public ModalCallbackAction replyModal(Modal modal) { return event.replyModal(modal); }

    @Override
    public User getUser() { return event.getUser(); }

    @Override
    public String getSubcommandName() { return event.getSubcommandName(); }

    @Override
    public List<OptionMapping> getOptions()  { return event.getOptions(); }

    @Override
    public OptionMapping getOption(String name) { return event.getOption(name); }

    @Override
    public MessageChannelUnion getChannel() { return event.getChannel(); }

    @Override
    public String getName() { return event.getName(); }
}
