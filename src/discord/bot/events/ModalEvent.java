package discord.bot.events;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ModalEvent implements EventWrapper {

    ModalInteractionEvent event;

    public ModalEvent(ModalInteractionEvent event) {
        this.event = event;
    }

    @Override
    public ReplyCallbackAction reply(@NotNull String content) { return event.reply(content); }

    @Override
    public User getUser() { return event.getUser(); }

    @Override
    public MessageChannelUnion getChannel() { return event.getChannel(); }

    @Override
    public String getModalId() { return event.getModalId(); }

    @Override
    public List<ModalMapping> getValues() { return event.getValues(); }
}
