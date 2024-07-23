package discord.bot.events;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface EventWrapper {

    default ReplyCallbackAction reply(@NotNull String content) { throw new UnsupportedOperationException(); }

    default ModalCallbackAction replyModal(Modal modal) { throw new UnsupportedOperationException(); }

    default User getUser() { throw new UnsupportedOperationException(); }

    default MessageChannelUnion getChannel() { throw new UnsupportedOperationException(); }

    //region SlashCommandInteractionEvent specific

    default String getName() { throw new UnsupportedOperationException(); }

    default String getSubcommandName() { throw new UnsupportedOperationException(); }

    default List<OptionMapping> getOptions() { throw new UnsupportedOperationException(); }

    default OptionMapping getOption(String name) { throw new UnsupportedOperationException(); }

    //endregion

    //region ButtonInteractionEvent specific

    default String getComponentId() { throw new UnsupportedOperationException(); }

    //endregion

    //region ModalInteractionEvent specific

    default String getModalId() { throw new UnsupportedOperationException(); }

    default List<ModalMapping> getValues() { throw new UnsupportedOperationException(); }

    //endregion
}
