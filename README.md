# Mini Golems — Minecraft 1.20.1 (MinecraftForge)

Pose n'importe quel bloc plein — sable, pierre, laine, cuivre — et coiffe-le d'une **Tête de mini
golem**. Comme pour un golem de neige, la tête se pose en dernier, et les deux blocs sont consommés.
Ce qui se lève prend la couleur et la texture du bloc utilisé.

Publié sur Modrinth : https://modrinth.com/mod/mini-golems

## Son travail : ranger les coffres

Le golem cherche un objet présent dans **deux coffres à la fois** et le porte du petit tas vers le
grand. Un objet qui n'existe que dans un seul coffre n'est jamais touché — les piles ne peuvent donc
que se regrouper, jamais s'éparpiller.

Portée : 8 blocs autour de lui, 3 en hauteur ; il porte 16 objets à la fois.

## Note sur les influences

Le principe « le matériau décide de la créature » vient d'Extra Golems, cité en commentaire dans le
code. La silhouette et le métier reprennent le **golem de cuivre vanilla** (mise à jour Copper Age).
Aucun code ni ressource tiers : tout est écrit de zéro dans `fr.pineapple.minigolems`.

## Compiler

```
./gradlew build
```

## Licence

MIT — voir [LICENSE](LICENSE).
