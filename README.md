# Gaming Soul (v0.8)

Bibliothèque de jeux pour Android + bouton flottant, viseur et Helper ChatGPT.
Package inchangé (`com.nexus.gamelibrary`) : la mise à jour s'installe par-dessus l'ancienne version.

## Nouveautés v0.8 — Moteur Arcade amélioré
- **Les bots se démarquent** au lieu de courir tous vers le ballon : un seul presseur par équipe, les autres cherchent un espace libre (loin des adversaires, ligne de passe dégagée, pas collés entre eux) et se replacent en défense (marquage côté but, couverture). Les passes des bots ciblent le coéquipier le mieux démarqué.
- **Foot 4 contre 4 avec gardien** : le gardien ne se pilote pas ; il se place sur la ligne ballon → but, lit les tirs (temps de réaction selon le niveau), plonge, capte ou repousse, sort sur les ballons lents et relance vers un coéquipier démarqué. Bouton **TACLE** : glissade (vers l'adversaire qui a le ballon s'il y en a un près de toi) ; un tacle raté ou dans le vide te laisse au sol un instant.
- **Défense au basket** : défense individuelle avec aide côté raquette, un presseur sur le porteur, **tir armé** (le tireur saute 0,3 s) pendant lequel un défenseur proche peut **contrer** (bouton TIR), vols de balle, **interceptions** de passes avec jet de chance, et bouton **DÉF** (maintenir) : défense assistée qui te place entre ton adversaire et le panier.
- **Volley en 3 touches** : réception → passe → attaque (même joueur pas deux fois de suite, 4ᵉ touche impossible). Compteur de touches affiché sous le score. Joystick vers le filet + FRAPPE = renvoi direct. Le contrôle passe tout seul au prochain joueur qui doit toucher.
- **Cercle de timing pour le smash** : à la 3ᵉ touche un cercle se resserre autour du ballon ; appuie sur FRAPPE quand il touche l'anneau doré (vert = smash parfait : rapide et précis ; blanc = trop tôt / trop tard : smash mou ou lob). Les bots smashent avec un timing plus ou moins bon selon leur niveau.
- Le troisième bouton (TACLE en foot, DÉF en basket) apparaît à gauche de PASSE.

## Nouveautés v0.7 — Arcade refaite
- **Vue du dessus en 2D** pour les trois sports, avec des **joueurs en pixel art plus détaillés** (12×16 : cheveux, visage, maillot avec numéro, short, chaussettes, chaussures ; 3 orientations — face, dos, profil — et animation de course).
- **Jeu en équipe avec des bots** : foot 3 contre 3, volley 2 contre 2, basket 3 contre 3. Tu pilotes un joueur (marqué d'une flèche dorée), il change automatiquement vers celui qui est le plus proche de l'action ; tes coéquipiers et les adversaires sont des bots.
- **Aide à la visée** : le tir de foot cherche un coin du but, le tir de basket est assisté (jauge de réussite affichée au-dessus du joueur), la frappe de volley choisit la zone libre du camp adverse (le joystick oriente : côté, court / long), les passes ciblent le coéquipier le mieux placé. Un pointillé doré montre la cible.
- **Commandes** : joystick tactile à gauche, boutons à droite (TIR + PASSE en foot et basket ; FRAPPE en volley ; en basket, TIR sert aussi à voler la balle en défense).
- **Trois carrières séparées** : Football, Volley, Basket, chacune avec ses 15 matchs (5 divisions), ses adversaires, ses étoiles et son bouton de réinitialisation. Depuis l'onglet Arcade, on choisit d'abord la carrière.
- Les anciennes étoiles (v0.6) ne sont pas reprises : les carrières ont changé, la sauvegarde repart de zéro.

## Nouveautés v0.6
- **Onglet Arcade** : mini-jeux en pixel art, 100 % hors ligne (aucun wifi, aucune permission).
  - 3 sports : **volley**, **foot**, **basket** (1 contre 1 contre l'ordinateur).
  - **Carrière** : 5 divisions (Quartier → Légende), 15 niveaux, l'IA devient de plus en plus forte.
  - 1 à 3 étoiles par niveau selon l'écart de score ; un niveau se débloque quand le précédent est gagné.
  - Commandes tactiles : ◀ ▶ pour bouger, ▲ pour sauter, bouton d'action pour frapper / smasher / tirer.
  - La progression est sauvegardée sur le téléphone.
- La barre du bas a un 6ᵉ onglet (« Bibliothèque » y est abrégé en « Biblio », « Paramètres » en « Réglages »).

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
