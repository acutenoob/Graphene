package net.carbonmc.graphene;

import net.carbonmc.graphene.async.AsyncSystemInitializer;
import net.carbonmc.graphene.client.ItemCountRenderer;
import net.carbonmc.graphene.client.gui.ClothConfigScreenFactory;
import net.carbonmc.graphene.config.CoolConfig;
import net.carbonmc.graphene.engine.cull.RenderOptimizer;
import net.carbonmc.graphene.events.ModEventHandlers;
import net.carbonmc.graphene.particles.AsyncParticleHandler;
import net.carbonmc.graphene.util.KillMobsCommand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
@Mod(value = "graphene")
public class Graphene {
	public static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "graphene";
	public static final String VERSION = "1.1.0";
	private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
	public static File Graphene_EVENTS_LOG = new File("log/graphene-event-debug.log");
	public Graphene() {

		var bus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CoolConfig.SPEC);
		MinecraftForge.EVENT_BUS.register(this);
		modEventBus.addListener(this::setup);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			MinecraftForge.EVENT_BUS.register(ItemCountRenderer.class);
		});
		MixinBootstrap.init();
		ModEventHandlers.register(modEventBus, forgeEventBus);
		modEventBus.addListener(AsyncSystemInitializer::init);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			AsyncEventSystem.shutdown();
			AsyncParticleHandler.shutdown();
		}));
		LOGGER.info("Initializing Graphene MOD v{}", VERSION);
		LOGGER.info("请确保模组名称前面有英文半角符号的'!',这样模组才会第一个加载！会带来更好的优化！");
		LOGGER.info("CarbonMC玩家QQ交流群：372378451");
		ModLoadingContext.get().registerExtensionPoint(
				ConfigScreenHandler.ConfigScreenFactory.class,
				() -> new ConfigScreenHandler.ConfigScreenFactory(
						(mc, parent) -> ClothConfigScreenFactory.create(parent)
				)
		);

		ModLoadingContext.get().registerExtensionPoint(
				IExtensionPoint.DisplayTest.class,
				() -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true)
		);
	}
	/**
	 * @author Red flag with 5 stars--RedStar
	 * @reason 检测
	 */

	private void setupClient(final FMLClientSetupEvent event) {

		RenderOptimizer.initialize();
		Runtime.getRuntime().addShutdownHook(new Thread(RenderOptimizer::shutdown));
	}

	/**
	 * @author Red flag with 5 stars--RedStar
	 * @reason 避免临时 Vector4f
	 */

	public static int getMaxWorkerThreads() {
		String propValue = System.getProperty("max.worker.threads");
		try {
			if (propValue != null) {
				int value = Integer.parseInt(propValue);
				return Math.max(1, Math.min(value, 32767));
			}
		} catch (NumberFormatException ignored) {}
		return Math.min(32767, Runtime.getRuntime().availableProcessors() * 2);
	}

	private void setup(final FMLCommonSetupEvent event) {

		LOGGER.info("Graphene Mod 初始化完成");
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		event.enqueueWork(() -> {
			AsyncEventSystem.initialize();
			ModEventProcessor.processModEvents();

		});
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
		KillMobsCommand.register(event.getDispatcher());
	}
	@OnlyIn(Dist.CLIENT)
	public static void registerDynamicListener(
			Class<? extends Event> eventType,
			IEventListener listener,
			EventPriority priority,
			boolean receiveCancelled
	) {
		MinecraftForge.EVENT_BUS.addListener(
				priority,
				receiveCancelled,
				eventType,
				event -> {
					try {
						listener.invoke(event);
					} catch (ClassCastException e) {
						LOGGER.error("Event type mismatch for listener", e);
					} catch (Throwable t) {
						LOGGER.error("Error in optimized event handler", t);
						if (CoolConfig.DISABLE_ASYNC_ON_ERROR.get() && eventType.getSimpleName().contains("Async")) {
							LOGGER.warn("Disabling async for event type due to handler error: {}", eventType.getName());
							AsyncEventSystem.registerSyncEvent(eventType);
						}
					}
				}
		);
	}
	public static void executeSafeAsync(Runnable task, String taskName) {
		AsyncEventSystem.executeAsync(
				TickEvent.ServerTickEvent.class,
				() -> {
					try {
						long start = System.currentTimeMillis();
						task.run();
						long duration = System.currentTimeMillis() - start;
						if (duration > 100) {
							LOGGER.debug("Async task '{}' completed in {}ms", taskName, duration);
						}
					} catch (Throwable t) {
						LOGGER.error("Async task '{}' failed", taskName, t);
						throw t;
					}
				}
		);
	}
}