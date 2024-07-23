package discord.bot;

import discord.bot.events.EventHandler;
import discord.bot.events.ModalEvent;
import main.Session;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static discord.bot.events.EventHandler.PLAY_MODAL_HANDLER;
import static discord.bot.events.EventHandler.SUGGEST_MODAL_HANDLER;

public enum ModalWrapper {
    SUGGEST("suggest",
            () -> {
                TextInput input = TextInput.create("suggestion", "Suggestion", TextInputStyle.SHORT)
                        .setPlaceholder("Enter suggestion name here")
                        .setMinLength(1)
                        .setMaxLength(30)
                        .build();

                return Modal.create("suggest", "Suggest")
                        .addActionRow(input)
                        .build();
            },
            SUGGEST_MODAL_HANDLER),
    PLAY("play",
            () -> {
                TextInput input = TextInput.create("play", "Played Game", TextInputStyle.SHORT)
                        .setPlaceholder("Enter name of the played game")
                        .setMinLength(1)
                        .setMaxLength(30)
                        .build();

                return Modal.create("play", "Record Played Game")
                        .addActionRow(input)
                        .build();
            },
            PLAY_MODAL_HANDLER),
    ;

    public final String id;
    private final Supplier<Modal> modalMaker;
    private final EventHandler eventHandler;

    ModalWrapper(@NotNull String id,
                 @NotNull Supplier<Modal> modalMaker,
                 @NotNull EventHandler eventHandler) {
        this.id = id;
        this.modalMaker = modalMaker;
        this.eventHandler = eventHandler;
    }

    public Modal modal() {
        return modalMaker.get();
    }

    public static void handle(@NotNull ModalInteractionEvent event, @NotNull Session session) {
        for (ModalWrapper modal : values()) {
            if (modal.id.equalsIgnoreCase(event.getModalId())) {
                try {
                    modal.eventHandler.accept(new ModalEvent(event), session);
                } catch (Exception e) {
                    event.reply("Modal encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }
}
