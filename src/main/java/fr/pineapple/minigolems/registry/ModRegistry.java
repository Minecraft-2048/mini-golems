package fr.pineapple.minigolems.registry;

import fr.pineapple.minigolems.MiniGolemsMod;
import fr.pineapple.minigolems.block.MiniGolemHeadBlock;
import fr.pineapple.minigolems.entity.MiniGolem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
	private ModRegistry() {}

	public static final DeferredRegister<Block> BLOCKS =
			DeferredRegister.create(ForgeRegistries.BLOCKS, MiniGolemsMod.MOD_ID);
	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, MiniGolemsMod.MOD_ID);
	public static final DeferredRegister<EntityType<?>> ENTITIES =
			DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MiniGolemsMod.MOD_ID);

	/** La tete a poser sur le bloc de son choix pour lui donner vie. */
	public static final RegistryObject<Block> MINI_GOLEM_HEAD = BLOCKS.register("mini_golem_head",
			() -> new MiniGolemHeadBlock(BlockBehaviour.Properties.copy(Blocks.CARVED_PUMPKIN)));

	public static final RegistryObject<Item> MINI_GOLEM_HEAD_ITEM = ITEMS.register("mini_golem_head",
			() -> new BlockItem(MINI_GOLEM_HEAD.get(), new Item.Properties()));

	public static final RegistryObject<EntityType<MiniGolem>> MINI_GOLEM = ENTITIES.register("mini_golem",
			() -> EntityType.Builder.of(MiniGolem::new, MobCategory.MISC)
					.sized(0.9F, 1.5F)
					.clientTrackingRange(10)
					.build(MiniGolemsMod.MOD_ID + ":mini_golem"));

	public static void register(IEventBus bus) {
		BLOCKS.register(bus);
		ITEMS.register(bus);
		ENTITIES.register(bus);
	}
}
