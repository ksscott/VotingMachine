package discord.bot;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.function.UnaryOperator;

public class CommandDataInitializers {

    public static final UnaryOperator<SlashCommandData> NO_OP = data -> data;

    //region Current Vote

    public static final String VIEW_NAME = "view";
    static final SubcommandData VIEW_CURR = new SubcommandData(VIEW_NAME, "View your current vote in this election");
    public static final String WAT_NAME = "wat";
    static final SubcommandData WAT = new SubcommandData(WAT_NAME, "wat is current vote??");
    public static final String CLEAR_NAME = "clear";
    static final SubcommandData CLEAR_CURR = new SubcommandData(CLEAR_NAME, "Erase your current vote in this election");

    //endregion

    //region Default Vote

    public static final String SAVE_NAME = "save";
    static final SubcommandData SAVE_DEFAULT = new SubcommandData(SAVE_NAME, "Save current vote as default vote");
    public static final String LOAD_NAME = "load";
    static final SubcommandData LOAD_DEFAULT = new SubcommandData(LOAD_NAME, "Load default vote, replacing your current vote");
    static final SubcommandData CLEAR_DEFAULT = new SubcommandData(CLEAR_NAME, "Erase your saved default vote");

    //endregion

    //region Toggle

    public static final String TOGGLE_NAME = "toggle";
    static final SubcommandData TOGGLE = new SubcommandData(TOGGLE_NAME, "toggle");
    public static final String TOGGLE_ON_NAME = "on";
    static final SubcommandData TOGGLE_ON = new SubcommandData(TOGGLE_ON_NAME, "toggle to on");
    public static final String TOGGLE_OFF_NAME = "off";
    static final SubcommandData TOGGLE_OFF = new SubcommandData(TOGGLE_OFF_NAME, "toggle to off");

    public static final UnaryOperator<SlashCommandData> ADD_TOGGLES = data -> {
        data.addSubcommands(TOGGLE, TOGGLE_ON, TOGGLE_OFF);
        return data;
    };

    //endregion

}
