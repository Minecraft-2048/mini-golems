package fr.pineapple.minigolems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.pineapple.minigolems.entity.MiniGolem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Le golem est taille dans son bloc, au sens propre.
 *
 * <p>Chaque face reprend la portion de texture qu'elle aurait si la creature avait ete decoupee
 * dans une pile de ces blocs : un texel du golem fait exactement un texel du bloc, et sa position
 * est respectee. Un golem de gazon porte donc l'herbe sur la tete et la terre sur les jambes, comme
 * la face nord du bloc dont il sort. Les coordonnees suivent la convention que Minecraft applique a
 * un modele de bloc qui omet son {@code uv}.</p>
 *
 * <p>Un modele classique ne convenait pas : il fixe ses coordonnees de texture a la construction,
 * alors qu'ici la texture n'est connue qu'au moment du rendu et change d'un golem a l'autre. Chaque
 * face est donc emise directement, en pointant sur la tuile du bloc dans l'atlas — le meme sprite
 * que Minecraft utilise pour ses particules de casse.</p>
 *
 * <p>Les mesures sont en texels (un seizieme de bloc), comme dans les modeles du jeu, et converties
 * au dernier moment. Elles sont relevees dans le modele du golem de cuivre de la 26.2.</p>
 */
public class MiniGolemRenderer extends EntityRenderer<MiniGolem> {
	/** Un seizieme de bloc, l'unite des modeles Minecraft. */
	private static final float P = 1.0F / 16.0F;

	/** Cote d'une tuile de texture, en texels. */
	private static final float TILE = 16.0F;

	/**
	 * Les yeux, seule partie qui n'emprunte pas la texture du bloc. La texture fait 8 x 5 texels,
	 * exactement la face avant de la tete : un pixel des yeux fait un pixel du bloc, comme le reste.
	 */
	private static final ResourceLocation EYES =
			new ResourceLocation("minigolems", "textures/entity/eyes.png");

	public MiniGolemRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.45F;
	}

	@Override
	public ResourceLocation getTextureLocation(MiniGolem golem) {
		return InventoryMenu.BLOCK_ATLAS;
	}

	@Override
	public void render(MiniGolem golem, float entityYaw, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int light) {
		BlockState state = golem.getMaterialState();
		BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		TextureAtlasSprite sprite = model.getParticleIcon();

		// Les blocs teintes — herbe, feuillages — ont une texture grise que seule la couleur de
		// biome rend correcte. Sans cette teinte un golem d'herbe serait blanchatre.
		int tint = Minecraft.getInstance().getBlockColors().getColor(state, golem.level(), golem.blockPosition(), 0);
		float red = tint == -1 ? 1.0F : (tint >> 16 & 0xFF) / 255.0F;
		float green = tint == -1 ? 1.0F : (tint >> 8 & 0xFF) / 255.0F;
		float blue = tint == -1 ? 1.0F : (tint & 0xFF) / 255.0F;

		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));

		pose.pushPose();

		float bodyYaw = Mth.rotLerp(partialTick, golem.yBodyRotO, golem.yBodyRot);
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

		// Aucune mise a l'echelle : le jeu rend le golem de cuivre tel quel, et c'est la condition
		// pour que les pixels du golem fassent la meme taille que ceux du bloc.

		// La demarche est deduite de la vitesse reelle plutot que des compteurs d'animation :
		// c'est independant des mappings et suffisant pour un pantin de quelques cubes.
		float speed = (float) golem.getDeltaMovement().horizontalDistance();
		float swing = Mth.cos((golem.tickCount + partialTick) * 0.6F) * Math.min(speed * 14.0F, 1.0F);

		// Jambes courtes et trapues, pivotant a la hanche.
		leg(pose, consumer, sprite, light, red, green, blue, -2.0F, swing);
		leg(pose, consumer, sprite, light, red, green, blue, 2.0F, -swing);

		// Torse. Il ne pivote pas : sa position au repos est sa position tout court.
		box(pose.last(), consumer, sprite, light, red, green, blue,
				-4.0F, 5.0F, -3.0F, 4.0F, 11.0F, 3.0F, 0.0F, 0.0F, 0.0F);

		// Bras longs, en balancier inverse des jambes.
		arm(pose, consumer, sprite, light, red, green, blue, -5.5F, -swing * 0.6F);
		arm(pose, consumer, sprite, light, red, green, blue, 5.5F, swing * 0.6F);

		// Tete, qui suit le regard. C'est elle qui donne sa silhouette au golem de cuivre : large,
		// et surtout tres profonde, d'ou le museau.
		float headYaw = Mth.rotLerp(partialTick, golem.yHeadRotO, golem.yHeadRot) - bodyYaw;
		float headPitch = Mth.lerp(partialTick, golem.xRotO, golem.getXRot());

		pose.pushPose();
		pose.translate(0.0F, 11.0F * P, 0.0F);
		pose.mulPose(Axis.YP.rotationDegrees(-headYaw));
		pose.mulPose(Axis.XP.rotationDegrees(headPitch));

		box(pose.last(), consumer, sprite, light, red, green, blue,
				-4.0F, 0.0F, -5.0F, 4.0F, 5.0F, 5.0F, 0.0F, 11.0F, 0.0F);

		// Le petit mat et sa pointe, signature du golem de cuivre. Ils depassent du bloc : leur
		// matiere vient donc du bas du bloc suivant de la pile.
		box(pose.last(), consumer, sprite, light, red, green, blue,
				-1.0F, 5.0F, -1.0F, 1.0F, 9.0F, 1.0F, 0.0F, 11.0F, 0.0F);
		box(pose.last(), consumer, sprite, light, red, green, blue,
				-2.0F, 9.0F, -2.0F, 2.0F, 13.0F, 2.0F, 0.0F, 11.0F, 0.0F);

		// Les yeux sont poses sur la face avant, un cheveu en avant du cube pour ne pas se battre
		// avec lui dans le tampon de profondeur. Ils sont opaques, pas additifs : ajouter de la
		// lumiere ne se voit pas sur un golem de sable ou de pierre claire, et le regard y
		// disparaissait. La luminosite est forcee au maximum, donc ils brillent quand meme la nuit.
		eyes(pose.last(), buffers.getBuffer(RenderType.entityCutout(EYES)));
		pose.popPose();

		pose.popPose();

		super.render(golem, entityYaw, partialTick, pose, buffers, light);
	}

	private static void leg(PoseStack pose, VertexConsumer consumer, TextureAtlasSprite sprite, int light,
			float r, float g, float b, float x, float swing) {
		pose.pushPose();
		pose.translate(x * P, 5.0F * P, 0.0F);
		pose.mulPose(Axis.XP.rotation(swing));
		box(pose.last(), consumer, sprite, light, r, g, b,
				-2.0F, -5.0F, -2.0F, 2.0F, 0.0F, 2.0F, x, 5.0F, 0.0F);
		pose.popPose();
	}

	private static void arm(PoseStack pose, VertexConsumer consumer, TextureAtlasSprite sprite, int light,
			float r, float g, float b, float x, float swing) {
		pose.pushPose();
		pose.translate(x * P, 11.0F * P, 0.0F);
		pose.mulPose(Axis.XP.rotation(swing));
		box(pose.last(), consumer, sprite, light, r, g, b,
				-1.5F, -10.0F, -2.0F, 1.5F, 0.0F, 2.0F, x, 11.0F, 0.0F);
		pose.popPose();
	}

	/**
	 * Six faces, chacune prenant dans la texture le rectangle qui lui revient.
	 *
	 * <p>Les coordonnees {@code x0..z1} sont locales a la piece — ce sont elles qui tournent avec
	 * elle. {@code offX/offY/offZ} donnent la position de la piece au repos et ne servent qu'a
	 * choisir la portion de texture : la matiere reste attachee au membre quand il bouge, comme si
	 * elle y avait ete taillee.</p>
	 */
	private static void box(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite, int light,
			float r, float g, float b,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float offX, float offY, float offZ) {
		// Position dans le bloc d'origine : le golem est centre sur x = z = 8 et pose sur y = 0,
		// comme s'il avait ete decoupe dans une pile de blocs partant de ses pieds.
		float gx0 = x0 + offX + 8.0F;
		float gx1 = x1 + offX + 8.0F;
		float gy0 = y0 + offY;
		float gy1 = y1 + offY;
		float gz0 = z0 + offZ + 8.0F;
		float gz1 = z1 + offZ + 8.0F;

		// nord (-Z) : vu du nord, +X part vers la gauche, d'ou le u inverse.
		quad(pose, consumer, sprite, light, r, g, b, 0.0F, 0.0F, -1.0F,
				x1, y1, z0, x0, y1, z0, x1, y0, z0,
				TILE - gx1, TILE - gy1, TILE - gx0, TILE - gy0);
		// sud (+Z)
		quad(pose, consumer, sprite, light, r, g, b, 0.0F, 0.0F, 1.0F,
				x0, y1, z1, x1, y1, z1, x0, y0, z1,
				gx0, TILE - gy1, gx1, TILE - gy0);
		// ouest (-X)
		quad(pose, consumer, sprite, light, r, g, b, -1.0F, 0.0F, 0.0F,
				x0, y1, z0, x0, y1, z1, x0, y0, z0,
				gz0, TILE - gy1, gz1, TILE - gy0);
		// est (+X)
		quad(pose, consumer, sprite, light, r, g, b, 1.0F, 0.0F, 0.0F,
				x1, y1, z1, x1, y1, z0, x1, y0, z1,
				TILE - gz1, TILE - gy1, TILE - gz0, TILE - gy0);
		// dessus (+Y)
		quad(pose, consumer, sprite, light, r, g, b, 0.0F, 1.0F, 0.0F,
				x0, y1, z0, x1, y1, z0, x0, y1, z1,
				gx0, gz0, gx1, gz1);
		// dessous (-Y)
		quad(pose, consumer, sprite, light, r, g, b, 0.0F, -1.0F, 0.0F,
				x0, y0, z1, x1, y0, z1, x0, y0, z0,
				gx0, TILE - gz1, gx1, TILE - gz0);
	}

	/**
	 * Une face rectangulaire, decrite par son coin d'origine et les extremites de ses deux aretes.
	 *
	 * <p>Les coordonnees de texture sont en texels et peuvent sortir de la tuile : le mat du golem,
	 * par exemple, depasse le haut de son bloc et emprunte le bas du bloc suivant. Les textures de
	 * blocs se raccordant d'une tuile a l'autre, on ramene les coordonnees dans la tuile et on coupe
	 * la face la ou elle franchit le bord. L'etirer briserait la taille des pixels, ce qui est
	 * justement ce qu'on cherche a garantir. Aucune face ne depassant seize texels, deux morceaux
	 * par axe suffisent.</p>
	 */
	private static void quad(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
			int light, float r, float g, float b, float nx, float ny, float nz,
			float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz,
			float u0, float v0, float u1, float v1) {
		float lu = u1 - u0;
		float lv = v1 - v0;

		if (lu <= 0.0F || lv <= 0.0F) {
			return;
		}

		float uStart = u0 - TILE * Mth.floor(u0 / TILE);
		float vStart = v0 - TILE * Mth.floor(v0 / TILE);

		for (int cu = 0; cu < 2; cu++) {
			float us = cu == 0 ? uStart : 0.0F;
			float ue = cu == 0 ? Math.min(TILE, uStart + lu) : uStart + lu - TILE;

			if (ue <= us) {
				continue;
			}

			float s0 = cu == 0 ? 0.0F : (TILE - uStart) / lu;
			float s1 = cu == 0 ? (ue - uStart) / lu : 1.0F;

			for (int cv = 0; cv < 2; cv++) {
				float vs = cv == 0 ? vStart : 0.0F;
				float ve = cv == 0 ? Math.min(TILE, vStart + lv) : vStart + lv - TILE;

				if (ve <= vs) {
					continue;
				}

				float t0 = cv == 0 ? 0.0F : (TILE - vStart) / lv;
				float t1 = cv == 0 ? (ve - vStart) / lv : 1.0F;

				// L'ordre des sommets decide de la face visible : OpenGL ne dessine que celle dont le
				// contour tourne dans le bon sens, l'autre est eliminee. Avec l'ordre inverse, on ne
				// voyait que l'interieur des cubes — le golem paraissait retourne comme un gant.
				corner(pose, consumer, sprite, light, r, g, b, nx, ny, nz,
						ox, oy, oz, ux, uy, uz, vx, vy, vz, s0, t1, us, ve);
				corner(pose, consumer, sprite, light, r, g, b, nx, ny, nz,
						ox, oy, oz, ux, uy, uz, vx, vy, vz, s1, t1, ue, ve);
				corner(pose, consumer, sprite, light, r, g, b, nx, ny, nz,
						ox, oy, oz, ux, uy, uz, vx, vy, vz, s1, t0, ue, vs);
				corner(pose, consumer, sprite, light, r, g, b, nx, ny, nz,
						ox, oy, oz, ux, uy, uz, vx, vy, vz, s0, t0, us, vs);
			}
		}
	}

	/**
	 * Un sommet, situe par ses deux parametres le long des aretes de la face.
	 *
	 * <p>{@code u} et {@code v} sont en texels dans la tuile ; la conversion vers l'atlas se fait
	 * ici plutot que par {@code sprite.getU}, dont la signature bouge d'une version a l'autre.</p>
	 */
	private static void corner(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
			int light, float r, float g, float b, float nx, float ny, float nz,
			float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz,
			float s, float t, float u, float v) {
		float x = ox + (ux - ox) * s + (vx - ox) * t;
		float y = oy + (uy - oy) * s + (vy - oy) * t;
		float z = oz + (uz - oz) * s + (vz - oz) * t;

		consumer.vertex(pose.pose(), x * P, y * P, z * P)
				.color(r, g, b, 1.0F)
				.uv(sprite.getU0() + (sprite.getU1() - sprite.getU0()) * u / TILE,
						sprite.getV0() + (sprite.getV1() - sprite.getV0()) * v / TILE)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				.normal(pose.normal(), nx, ny, nz)
				.endVertex();
	}

	/** Un quad plaque sur la face avant de la tete, aux dimensions exactes de celle-ci. */
	private static void eyes(PoseStack.Pose pose, VertexConsumer consumer) {
		float x = 4.0F * P;
		float top = 5.0F * P;
		float z = -5.0F * P - 0.001F;

		eyeVertex(pose, consumer, x, 0.0F, z, 0.0F, 1.0F);
		eyeVertex(pose, consumer, -x, 0.0F, z, 1.0F, 1.0F);
		eyeVertex(pose, consumer, -x, top, z, 1.0F, 0.0F);
		eyeVertex(pose, consumer, x, top, z, 0.0F, 0.0F);
	}

	private static void eyeVertex(PoseStack.Pose pose, VertexConsumer consumer,
			float x, float y, float z, float u, float v) {
		consumer.vertex(pose.pose(), x, y, z)
				.color(1.0F, 1.0F, 1.0F, 1.0F)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(15728640)
				.normal(pose.normal(), 0.0F, 0.0F, -1.0F)
				.endVertex();
	}
}
