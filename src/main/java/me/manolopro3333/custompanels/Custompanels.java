package me.manolopro3333.custompanels;

import com.mojang.logging.LogUtils;
import me.manolopro3333.custompanels.client.ClientRegistry;
import me.manolopro3333.custompanels.command.CrearPanelCommand;
import me.manolopro3333.custompanels.command.EditPanelCommand;
import me.manolopro3333.custompanels.command.PanelCommand;
import me.manolopro3333.custompanels.init.ModMenus;
import me.manolopro3333.custompanels.network.TestButtonMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Custompanels.MODID)
public class Custompanels {
    public static final String MODID = "custompanels";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL_VERSION = "1";

    @SuppressWarnings("removal")
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "main"))
            .networkProtocolVersion(() -> "1.0")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();

    private static int messageID = 0;
    @SuppressWarnings("removal")
    public Custompanels() {


        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();



        // Registro de sistemas
        ModMenus.REGISTRY.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        // Registrar eventos del bus de Forge
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Registrar mensajes de red
            registerMessages();

            LOGGER.info("Configuración común completada");
        });
    }

    private void registerMessages() {
        addNetworkMessage(
                TestButtonMessage.class,
                TestButtonMessage::buffer,
                TestButtonMessage::new,
                TestButtonMessage::handler
        );
        TestButtonMessage.registerMessage(PACKET_HANDLER);
        LOGGER.info("Mensajes de red registrados");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("CustomPanel inicializado en el servidor");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PanelCommand.register(event);
        CrearPanelCommand.register(event);
        EditPanelCommand.register(event);
        LOGGER.info("Comandos registrados");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.init(event);
            LOGGER.info("Configuración del cliente completada");
        }
    }

    public static <T> void addNetworkMessage(Class<T> messageType,
                                             BiConsumer<T, FriendlyByteBuf> encoder,
                                             Function<FriendlyByteBuf, T> decoder,
                                             BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID++, messageType, encoder, decoder, messageConsumer);
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
            workQueue.forEach(work -> {
                work.setValue(work.getValue() - 1);
                if (work.getValue() == 0)
                    actions.add(work);
            });
            actions.forEach(e -> e.getKey().run());
            workQueue.removeAll(actions);
        }
    }


}
