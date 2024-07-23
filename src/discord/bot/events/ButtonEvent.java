package discord.bot.events;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

public class ButtonEvent implements EventWrapper {

    private final ButtonInteractionEvent event;

    public ButtonEvent(ButtonInteractionEvent event) {
        this.event = event;
    }

    @Override
    public ReplyCallbackAction reply(@NotNull String content) { return event.reply(content); }

    @Override
    public ModalCallbackAction replyModal(Modal modal) { return event.replyModal(modal); }

    @Override
    public User getUser() { return event.getUser(); }

    @Override
    public MessageChannelUnion getChannel() { return event.getChannel(); }

    @Override
    public String getComponentId() { return event.getComponentId(); }
}
