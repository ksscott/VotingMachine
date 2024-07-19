package discord.bot;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.function.UnaryOperator;

public class CommandDataInitializers {

    public static final UnaryOperator<SlashCommandData> NO_OP = data -> data;

    //region Toggle

    public static final String TOGGLE_NAME = "toggle";
    public static final String TOGGLE_ON_NAME = "on";
    public static final String TOGGLE_OFF_NAME = "off";

    static final SubcommandData TOGGLE = new SubcommandData(TOGGLE_NAME, "toggle");
    static final SubcommandData TOGGLE_ON = new SubcommandData(TOGGLE_ON_NAME, "toggle to on");
    static final SubcommandData TOGGLE_OFF = new SubcommandData(TOGGLE_OFF_NAME, "toggle to off");

    public static final UnaryOperator<SlashCommandData> ADD_TOGGLES = data -> {
        data.addSubcommands(TOGGLE, TOGGLE_ON, TOGGLE_OFF);
        return data;
    };

    //endregion

}
