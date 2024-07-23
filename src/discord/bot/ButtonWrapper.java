package discord.bot;

import discord.bot.events.ButtonEvent;
import discord.bot.events.EventHandler;
import main.Session;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import static discord.bot.events.EventHandler.*;

public enum ButtonWrapper {
    VIEW_CANDIDATES("candidates-view", "View candidates", CANDIDATES_VIEW_BUTTON_HANDLER),
    PAST_VOTES("past-votes", "Toggle past votes", PAST_VOTES_BUTTON_HANDLER),
    PICK("pick", "Pick winner(s)", PICK_HANDLER),
    PLAY("play", "Record played game", PLAY_HANDLER),
    HELP("help", "Help", HELP_HANDLER),
    ;

    public final String id;
    private final String label;
    private final ButtonStyle style;
    private final EventHandler eventHandler;

    ButtonWrapper(@NotNull String id,
                  @NotNull String label,
                  @NotNull ButtonStyle style,
                  @NotNull EventHandler eventHandler) {
        this.id = id;
        this.label = label;
        this.style = style;
        this.eventHandler = eventHandler;
    }

    ButtonWrapper(@NotNull String id,
                  @NotNull String label,
                  @NotNull EventHandler eventHandler) {
        this(id, label, ButtonStyle.SECONDARY, eventHandler);
    }

    public Button button() {
        return Button.of(style, id, label);
    }

    public static void handle(@NotNull ButtonInteractionEvent event, @NotNull Session session) {
        for (ButtonWrapper button : values()) {
            if (button.id.equalsIgnoreCase(event.getComponentId())) {
                try {
                    button.eventHandler.accept(new ButtonEvent(event), session);
                } catch (Exception e) {
                    event.reply("Button encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }
}
