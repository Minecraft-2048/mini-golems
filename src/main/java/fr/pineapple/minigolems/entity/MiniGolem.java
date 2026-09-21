package fr.pineapple.minigolems.entity;

import fr.pineapple.minigolems.entity.ai.SortChestsGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Un mini-golem taille dans un bloc.
 *
 * <p>Il retient le bloc dont il est fait : c'est lui qui donne sa texture, ses sons de pas et sa
 * couleur. Son travail est toujours le meme, quel que soit le materiau — consolider les objets
 * eparpilles dans les coffres voisins.</p>
 */
public class MiniGolem extends AbstractGolem {
	/** Le bloc source, transporte par son identifiant : simple a synchroniser et a sauvegarder. */
	private static final EntityDataAccessor<String> DATA_MATERIAL =
			SynchedEntityData.defineId(MiniGolem.class, EntityDataSerializers.STRING);

	public MiniGolem(EntityType<? extends MiniGolem> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return AbstractGolem.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.28D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new SortChestsGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_MATERIAL, BuiltInRegistries.BLOCK.getKey(Blocks.COPPER_BLOCK).toString());
	}

	public void setMaterial(Block block) {
		this.entityData.set(DATA_MATERIAL, BuiltInRegistries.BLOCK.getKey(block).toString());
	}

	public Block getMaterial() {
		ResourceLocation id = ResourceLocation.tryParse(this.entityData.get(DATA_MATERIAL));
		return id == null ? Blocks.COPPER_BLOCK : BuiltInRegistries.BLOCK.get(id);
	}

	public BlockState getMaterialState() {
		return this.getMaterial().defaultBlockState();
	}

	/** Le nom affiche suit le materiau : « Mini-golem de sable » plutot qu'un nom generique. */
	@Override
	public net.minecraft.network.chat.Component getName() {
		if (this.hasCustomName()) {
			return super.getName();
		}

		return net.minecraft.network.chat.Component.translatable(
				"entity.minigolems.mini_golem.of",
				net.minecraft.network.chat.Component.translatable(this.getMaterial().getDescriptionId()));
	}

	/** Les sons de pas viennent du bloc : un golem de sable ne sonne pas comme un golem de pierre. */
	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		SoundType sound = this.getMaterialState().getSoundType();
		this.playSound(sound.getStepSound(), sound.getVolume() * 0.35F, sound.getPitch());
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.getMaterialState().getSoundType().getHitSound();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.getMaterialState().getSoundType().getBreakSound();
	}

	/** Casse, il rend le bloc qui l'a fait. */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		this.spawnAtLocation(new ItemStack(this.getMaterial()));
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("Material", this.entityData.get(DATA_MATERIAL));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);

		if (tag.contains("Material")) {
			this.entityData.set(DATA_MATERIAL, tag.getString("Material"));
		}
	}
}
