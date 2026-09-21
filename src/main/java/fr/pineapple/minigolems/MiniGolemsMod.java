package fr.pineapple.minigolems;

import fr.pineapple.minigolems.entity.MiniGolem;
import fr.pineapple.minigolems.registry.ModRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MiniGolemsMod.MOD_ID)
public class MiniGolemsMod {
	public static final String MOD_ID = "minigolems";
	public static final Logger LOGGER = LoggerFactory.getLogger("Mini Golems");

	public MiniGolemsMod() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		ModRegistry.register(modBus);
		modBus.addListener(this::createAttributes);
		modBus.addListener(this::addToCreativeTab);

		MinecraftForge.EVENT_BUS.register(new fr.pineapple.minigolems.event.GolemAssembly());
	}

	private void addToCreativeTab(final net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			event.accept(ModRegistry.MINI_GOLEM_HEAD_ITEM);
		}
	}

	private void createAttributes(final EntityAttributeCreationEvent event) {
		event.put(ModRegistry.MINI_GOLEM.get(), MiniGolem.createAttributes().build());
	}
}
