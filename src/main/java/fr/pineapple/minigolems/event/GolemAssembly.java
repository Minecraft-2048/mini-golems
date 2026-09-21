package fr.pineapple.minigolems.event;

import fr.pineapple.minigolems.entity.MiniGolem;
import fr.pineapple.minigolems.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Construction d'un mini-golem : un bloc plein surmonte d'une citrouille sculptee.
 *
 * <p>C'est le principe d'Extra Golems — le materiau decide de la creature — applique a la forme et
 * au metier du golem de cuivre. N'importe quel bloc plein fait l'affaire : sable, pierre, laine,
 * cuivre. Comme dans le vanilla, la tete doit etre posee en dernier, et les deux blocs sont
 * consommes.</p>
 *
 * <p>1.20.1 code en dur ses patrons de golems dans {@code CarvedPumpkinBlock}, sans point
 * d'extension : on ecoute donc la pose de bloc.</p>
 */
public class GolemAssembly {
	/**
	 * Priorite la plus basse par prudence : depuis que le mod a sa propre tete, il n'y a plus de
	 * geste partage avec le vanilla ou Extra Golems, mais laisser les autres passer d'abord ne
	 * coute rien.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.isCanceled() || event.getLevel().isClientSide()
				|| !(event.getLevel() instanceof Level level)) {
			return;
		}

		// Un autre mod a pu retirer la tete entre-temps.
		if (!level.getBlockState(event.getPos()).is(event.getPlacedBlock().getBlock())) {
			return;
		}

		BlockState placed = event.getPlacedBlock();

		// Uniquement notre propre tete : la citrouille sculptee sert deja au vanilla et a Extra
		// Golems, et la detourner rendait les trois mods impossibles a departager.
		if (!placed.is(ModRegistry.MINI_GOLEM_HEAD.get())) {
			return;
		}

		BlockPos pumpkinPos = event.getPos();
		BlockPos basePos = pumpkinPos.below();
		BlockState base = level.getBlockState(basePos);

		if (!isValidMaterial(level, basePos, base)) {
			return;
		}

		Block material = base.getBlock();

		level.removeBlock(pumpkinPos, false);
		level.removeBlock(basePos, false);

		MiniGolem golem = ModRegistry.MINI_GOLEM.get().create(level);

		if (golem == null) {
			return;
		}

		golem.moveTo(basePos.getX() + 0.5D, basePos.getY(), basePos.getZ() + 0.5D,
				level.getRandom().nextFloat() * 360.0F, 0.0F);
		golem.setMaterial(material);

		if (level instanceof ServerLevel serverLevel) {
			golem.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(basePos),
					MobSpawnType.TRIGGERED, null, null);
		}

		level.addFreshEntity(golem);

		SoundType sound = base.getSoundType();
		level.playSound(null, basePos, sound.getPlaceSound(), SoundSource.BLOCKS,
				sound.getVolume(), sound.getPitch());
	}

	/**
	 * Un cube plein et opaque, sans inventaire ni etat a preserver.
	 *
	 * <p>La condition ecarte les escaliers, dalles, verre et coffres : un golem taille dedans
	 * n'aurait pas de texture coherente sur ses six faces, et detruire un conteneur pour en faire
	 * une creature ferait disparaitre son contenu.</p>
	 */
	private static boolean isValidMaterial(Level level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.hasBlockEntity()) {
			return false;
		}

		// Rien qui puisse tomber ou couler pendant l'assemblage.
		if (!state.getFluidState().isEmpty()) {
			return false;
		}

		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			return false;
		}

		return state.isCollisionShapeFullBlock(level, pos) && state.canOcclude();
	}
}
