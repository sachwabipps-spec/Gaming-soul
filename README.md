# Gaming Soul (v0.5)

Bibliothèque de jeux pour Android + bouton flottant, viseur et Helper ChatGPT.
Package inchangé (`com.nexus.gamelibrary`) : la mise à jour s'installe par-dessus l'ancienne version.

## Nouveautés v0.5
- **Position du viseur** : décalage horizontal + vertical (curseurs) et croix de flèches ↑ ← ◎ → ↓ (aussi dans le bouton flottant, onglet Viseur ; maintenir = défilement, ◎ = recentrer).

## Nouveautés v0.4
- **Viseur façon Valorant** : couleur (8 couleurs + #RRGGBB), contours, point central, lignes intérieures et extérieures, décalage vertical. Écran *Paramètres → Viseur* avec aperçu en direct.
- **Photo de fond floue** (*Paramètres → Photo de fond*) : boutons et cartes deviennent translucides. Réglage du flou.
- **Onglet Helper** : tu écris ta question, elle part vers l'appli ChatGPT, puis tu colles la réponse dans Gaming Soul (historique enregistré). Aucune clé API.
- **« Boutique » renommé « Applis »**.
- **Bouton flottant à 3 onglets** : Capture (texte proposé + suggestions), Helper, Viseur (afficher / masquer).
- **Nouveau logo** (anneau + flamme) et nom **Gaming Soul**.
- L'APK s'appelle **gaming-soul.apk**.

## Compiler
GitHub : onglet Actions → *Build APK* → télécharger l'artefact `gaming-soul`.
Android Studio : *Build → Build APK(s)* (fichier `gaming-soul-debug.apk`).

## Premier lancement
1. Autorise « Afficher par-dessus les autres applis » (bulle + viseur).
2. À chaque capture, Android demande « Commencer l'enregistrement ou la diffusion ? » : appuie sur Commencer.
