package me.manolopro3333.custompanels.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import me.manolopro3333.custompanels.executes.AbrirGuiTest;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.io.File;

@Mod.EventBusSubscriber
public class PanelCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_PANELES = (ctx, builder) -> {
        for (String p : CrearPanelCommand.getPanelesExistentes(ctx.getSource())) {
            if (p.startsWith(builder.getRemainingLowerCase())) builder.suggest(p);
        }
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void register(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(
                Commands.literal("panel")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.argument("nombre", StringArgumentType.word())
                                .suggests(SUGGEST_PANELES)
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "nombre");
                                    CommandSourceStack src = ctx.getSource();
                                    File panelDir = CrearPanelCommand.getPanelDirectory(src.getLevel());
                                    File file = new File(panelDir, name + ".yml");

                                    if (!file.exists()) {
                                        src.sendFailure(Component.literal("§cNo existe un panel llamado '" + name + "'."));
                                        return 0;
                                    }

                                    ServerPlayer player = src.getPlayerOrException();
                                    Vec3 pos = src.getPosition();
                                    AbrirGuiTest.execute(player.level(), pos.x, pos.y, pos.z, player, name);
                                    src.sendSuccess(() -> Component.literal("§aAbriendo panel '" + name + "'…"), false);
                                    return 1;
                                })
                        )
        );
    }
}
