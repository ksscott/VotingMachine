package discord.bot;

import elections.games.Game;
import main.Session;
import model.Option;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public enum ButtonWrapper {
    PAST_VOTES("past-votes", "Toggle past votes", ButtonStyle.SECONDARY,
            (event, session) -> {
                boolean result = session.toggleIncludeShadow();
                String username = event.getUser().getEffectiveName();
                event.reply(username + " toggled " + (result ? "ON" : "OFF") + " past vote weights.").queue();
            }),
    PICK("pick", "Pick winner(s)", ButtonStyle.SECONDARY,
            (event, session) -> { // TODO duplicated with code in SlashCommand
                Set<Option> winners;
                try {
                    winners = session.pickWinner();
                } catch (IOException e) {
                    event.reply("Command encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                Set<Game> winningGames = winners
                        .stream()
                        .map(Option::name)
                        .map(Game::interpret) // FIXME make this agnostic to Games
                        .map(o -> o.orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                String winnersString = winners
                        .stream()
                        .map(option -> "**" + option.name() + "**")
                        .sorted()
                        .collect(Collectors.joining(", and "));

                int numVoters = session.numVoters();
                String warning = winningGames
                        .stream()
                        .filter(game -> game.getMaxPlayers() < numVoters && game.getMaxPlayers() > 0)
                        .map(game -> "\n*Warning:* " + game.getTitle() + " has a maximum number of players of " + game.getMaxPlayers())
                        .collect(Collectors.joining());

                event.reply("The winner is: " + winnersString + warning).queue();
                File resultsFile = Paths.get("./data/flowplot.png").toFile(); // FIXME hard coded
                event.getChannel().sendFiles(FileUpload.fromData(resultsFile)).queue();
            }),
    PLAY("play", "Record played game", ButtonStyle.SECONDARY,
            (event, session) -> {
                event.replyModal(ModalWrapper.PLAY.modal()).queue();
            }),
    ;

    public final String id;
    private final String label;
    private final ButtonStyle style;
    private final EventHandler<ButtonInteractionEvent> eventHandler;

    ButtonWrapper(String id, String label, ButtonStyle style, EventHandler<ButtonInteractionEvent> eventHandler) {
        this.id = id;
        this.label = label;
        this.style = style;
        this.eventHandler = eventHandler;
    }

    public Button button() {
        return Button.of(style, id, label);
    }

    public static void handle(@NotNull ButtonInteractionEvent event, @NotNull Session session) {
        for (ButtonWrapper button : values()) {
            if (button.id.equalsIgnoreCase(event.getComponentId())) {
                try {
                    button.eventHandler.accept(event, session);
                } catch (Exception e) {
                    event.reply("Button encountered an error: \n" + e.getMessage()).setEphemeral(true).queue();
                }
                break;
            }
        }
    }
}
