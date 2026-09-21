package fr.pineapple.minigolems.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * La tete du mini-golem.
 *
 * <p>Elle existe pour que le mod cesse de detourner la citrouille sculptee : celle-ci sert deja au
 * vanilla et a Extra Golems, et un joueur qui pose une citrouille ne s'attend pas forcement a voir
 * apparaitre un mini-golem. Avec un bloc dedie, l'intention est explicite et les trois mods ne se
 * disputent plus le meme geste.</p>
 *
 * <p>La construction elle-meme est declenchee a la pose, dans {@code GolemAssembly}.</p>
 */
public class MiniGolemHeadBlock extends HorizontalDirectionalBlock {
	public MiniGolemHeadBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
}
