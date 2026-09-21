package fr.pineapple.minigolems.entity.ai;

import fr.pineapple.minigolems.entity.MiniGolem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Le travail du golem : rassembler ce qui traine en double.
 *
 * <p>Il cherche un objet present dans deux coffres a la fois et le deplace vers celui qui en
 * contient deja le plus. Le sens du deplacement est ce qui rend le tri stable : en allant toujours
 * du petit tas vers le grand, les piles finissent regroupees au lieu de faire des allers-retours.
 * Un objet present dans un seul coffre n'est jamais touche.</p>
 */
public class SortChestsGoal extends Goal {
	private static final int SEARCH_RADIUS = 8;
	private static final int VERTICAL_RADIUS = 3;
	private static final int MAX_CARRIED = 16;
	private static final double REACH_SQR = 4.0D;
	private static final double SPEED = 0.9D;

	/** Un transfert a effectuer : prendre {@code item} en {@code source} pour le porter en {@code target}. */
	private record Move(BlockPos source, BlockPos target, ItemStack item) {}

	private final MiniGolem golem;

	@Nullable
	private Move move;

	private int repathCooldown;
	private int idleCooldown;

	public SortChestsGoal(MiniGolem golem) {
		this.golem = golem;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.carried().isEmpty()) {
			return true;
		}

		// Le balayage parcourt un volume : le faire a chaque tick serait du gaspillage.
		if (this.idleCooldown > 0) {
			this.idleCooldown--;
			return false;
		}

		this.idleCooldown = 40;
		this.move = this.findMove();
		return this.move != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.move != null || !this.carried().isEmpty();
	}

	@Override
	public void stop() {
		this.move = null;
		this.repathCooldown = 0;
		this.golem.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.repathCooldown > 0) {
			this.repathCooldown--;
		}

		if (this.move == null) {
			return;
		}

		if (this.carried().isEmpty()) {
			this.collect();
		} else {
			this.deliver();
		}
	}

	private void collect() {
		Container source = this.containerAt(this.move.source());

		if (source == null) {
			this.move = null;
			return;
		}

		if (!this.moveTowards(this.move.source())) {
			return;
		}

		ItemStack taken = take(source, this.move.item(), MAX_CARRIED);

		if (taken.isEmpty()) {
			this.move = null;
			return;
		}

		this.golem.setItemInHand(InteractionHand.MAIN_HAND, taken);
		source.setChanged();
	}

	private void deliver() {
		Container target = this.move == null ? null : this.containerAt(this.move.target());

		// La destination a disparu pendant le trajet : on repose la charge plutot que de
		// la garder en main indefiniment.
		if (target == null) {
			this.dropCarriedBack();
			return;
		}

		if (!this.moveTowards(this.move.target())) {
			return;
		}

		ItemStack leftover = insert(target, this.carried());
		this.golem.setItemInHand(InteractionHand.MAIN_HAND, leftover);
		target.setChanged();

		if (leftover.isEmpty()) {
			this.move = null;
		}
	}

	private void dropCarriedBack() {
		if (this.move != null) {
			Container source = this.containerAt(this.move.source());

			if (source != null && this.moveTowards(this.move.source())) {
				this.golem.setItemInHand(InteractionHand.MAIN_HAND, insert(source, this.carried()));
				source.setChanged();
				this.move = null;
			}

			return;
		}

		this.move = null;
	}

	private boolean moveTowards(BlockPos pos) {
		this.golem.getLookControl().setLookAt(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

		if (this.golem.blockPosition().distSqr(pos) <= REACH_SQR) {
			this.golem.getNavigation().stop();
			return true;
		}

		if (this.repathCooldown <= 0) {
			this.golem.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, SPEED);
			this.repathCooldown = 10;
		}

		return false;
	}

	/**
	 * Cherche un objet present dans plusieurs coffres et decide du sens du transfert : du coffre
	 * qui en a le moins vers celui qui en a le plus.
	 */
	@Nullable
	private Move findMove() {
		List<BlockPos> chests = this.nearbyChests();

		for (BlockPos from : chests) {
			Container source = this.containerAt(from);

			if (source == null) {
				continue;
			}

			for (int slot = 0; slot < source.getContainerSize(); slot++) {
				ItemStack stack = source.getItem(slot);

				if (stack.isEmpty()) {
					continue;
				}

				int here = count(source, stack);
				BlockPos best = null;
				int bestCount = here;

				for (BlockPos to : chests) {
					if (to.equals(from)) {
						continue;
					}

					Container target = this.containerAt(to);

					if (target == null || !hasRoomFor(target, stack)) {
						continue;
					}

					int there = count(target, stack);

					if (there > bestCount) {
						bestCount = there;
						best = to;
					}
				}

				if (best != null) {
					return new Move(from, best, stack.copy());
				}
			}
		}

		return null;
	}

	private List<BlockPos> nearbyChests() {
		BlockPos origin = this.golem.blockPosition();
		List<BlockPos> found = new ArrayList<>();

		for (BlockPos pos : BlockPos.betweenClosed(
				origin.offset(-SEARCH_RADIUS, -VERTICAL_RADIUS, -SEARCH_RADIUS),
				origin.offset(SEARCH_RADIUS, VERTICAL_RADIUS, SEARCH_RADIUS))) {
			if (this.golem.level().getBlockEntity(pos) instanceof ChestBlockEntity) {
				found.add(pos.immutable());
			}
		}

		return found;
	}

	@Nullable
	private Container containerAt(BlockPos pos) {
		BlockEntity blockEntity = this.golem.level().getBlockEntity(pos);
		return blockEntity instanceof Container container ? container : null;
	}

	private ItemStack carried() {
		return this.golem.getItemInHand(InteractionHand.MAIN_HAND);
	}

	private static int count(Container container, ItemStack like) {
		int total = 0;

		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack slot = container.getItem(i);

			if (ItemStack.isSameItemSameTags(slot, like)) {
				total += slot.getCount();
			}
		}

		return total;
	}

	private static boolean hasRoomFor(Container container, ItemStack stack) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack slot = container.getItem(i);

			if (slot.isEmpty()) {
				return true;
			}

			if (ItemStack.isSameItemSameTags(slot, stack)
					&& slot.getCount() < Math.min(slot.getMaxStackSize(), container.getMaxStackSize())) {
				return true;
			}
		}

		return false;
	}

	/** Retire jusqu'a {@code max} exemplaires de cet objet et les rend. */
	private static ItemStack take(Container container, ItemStack like, int max) {
		ItemStack carried = ItemStack.EMPTY;

		for (int i = 0; i < container.getContainerSize() && carried.getCount() < max; i++) {
			ItemStack slot = container.getItem(i);

			if (!ItemStack.isSameItemSameTags(slot, like)) {
				continue;
			}

			int wanted = max - carried.getCount();
			ItemStack removed = container.removeItem(i, wanted);

			if (removed.isEmpty()) {
				continue;
			}

			if (carried.isEmpty()) {
				carried = removed;
			} else {
				carried.grow(removed.getCount());
			}
		}

		return carried;
	}

	/** Verse ce qui rentre et rend le reste. */
	private static ItemStack insert(Container container, ItemStack stack) {
		ItemStack remaining = stack.copy();

		for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
			ItemStack slot = container.getItem(i);

			if (slot.isEmpty()) {
				container.setItem(i, remaining.copy());
				return ItemStack.EMPTY;
			}

			if (!ItemStack.isSameItemSameTags(slot, remaining)) {
				continue;
			}

			int room = Math.min(slot.getMaxStackSize(), container.getMaxStackSize()) - slot.getCount();

			if (room <= 0) {
				continue;
			}

			int moved = Math.min(room, remaining.getCount());
			slot.grow(moved);
			remaining.shrink(moved);
		}

		return remaining;
	}
}
