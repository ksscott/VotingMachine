package discord.bot;

import main.Session;
import model.Option;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Supplier;

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
            (event, session) -> {
                String username = event.getUser().getEffectiveName();
                String gameString = event.getValues().get(0).getAsString();

                session.suggest(new Option(gameString));

                event.reply(":right_arrow: " + username + " suggested: " + gameString).queue();
            }),
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
            (event, session) -> {
                String gameString = event.getValues().get(0).getAsString();
                Option game = session.interpret(gameString).orElse(null);
                if (game == null) {
                    event.reply("Game not recognized").setEphemeral(true).queue();
                    return;
                }
                try {
                    session.recordUnspentVotes(game);
                } catch (IOException e) {
                    event.reply("Command encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                    return;
                }

                // FIXME There's a bug in here somewhere: "Error: null"

                event.reply("Recorded winner of the last election: " + game.name()).queue();
            }),
    ;

    public final String id;
    private final Supplier<Modal> modalMaker;
    private final EventHandler<ModalInteractionEvent> eventHandler;

    ModalWrapper(String id, Supplier<Modal> modalMaker, EventHandler<ModalInteractionEvent> eventHandler) {
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
                    modal.eventHandler.accept(event, session);
                } catch (Exception e) {
                    event.reply("Modal encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }
}
