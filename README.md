# Gaming Soul (v0.16)

## v0.16 — Chevalier : nouveaux graphismes + personnalisation
- **Rendu refait** (`KnightRender.kt`) : décor gothique en 3 couches de parallaxe (tours, dômes, viaducs, montagnes, dunes, grottes selon le monde), halo et faisceaux de lumière, brume qui dérive, particules lumineuses, lampadaires qui vacillent, fougères / cristaux, pavés texturés avec contours encrés, stalactites, vignette sombre.
- **Contours noirs** autour du chevalier, des ennemis et des coffres ; halos sur projectiles, coffres rares, boss, porte (arche gothique).
- **Nouveau chevalier** (original) avec animations : respiration, course, saut, coup d'épée avec traînée lumineuse, ombre au sol.
- **Personnalisation** (Chevalier → 🎨 Style) avec aperçu animé (touche l'aperçu : repos / course / attaque) : 5 casques, 4 panaches, 4 capes, 3 épaulières, 4 emblèmes, 6 couleurs au choix parmi 16 (armure, liseré, cape, panache, yeux, lame), bouton Aléatoire et Par défaut. Sauvegardé sur le téléphone (`KSkin` dans `KnightData.kt`).

## v0.15 — Chevalier : aventure de 520 niveaux
- Nouveau mode **⚔️ Chevalier** (Arcade → Chevalier) : plateformer 2D de côté (façon Mario / Hollow Knight), 13 mondes × 40 niveaux = **520 niveaux**, boss tous les 10 niveaux.
- Ennemis (gelée, chauve-souris, squelette, archer, brute, mage, spectre) qui apparaissent peu à peu et deviennent plus forts (PV, dégâts, élites).
- **Coffres** (bois / argent / or) : pièces, potions, équipement (arme, armure, bottes, charme ; 5 raretés) ou pouvoirs.
- **6 classes** (Chevalier, Berserker, Paladin, Assassin, Mage-lame, Lancier).
- **Gacha de 50 pouvoirs** (10 types × 5 éléments), set de 3 pouvoirs personnalisable, doublons = niveau du pouvoir.
- **Sons et musique** : chaque niveau a sa propre musique générée (ambiance du monde + mélodie du niveau, boss plus rapides) et 13 effets sonores, réglables dans la pause. Code : `KnightAudio.kt`.
- Forge : améliorations permanentes (PV, dégâts). Code : `KnightData.kt`, `KnightEngine.kt`, `KnightMenu.kt`, `KnightScreen.kt`.

## Mise à jour v0.14 — Vue 3D + dribbles de foot
- **Vue 3D façon FIFA Mobile** (foot et basket) : caméra en perspective depuis le bord du terrain, côté lointain plus étroit, joueurs et ballon mis à l'échelle selon la profondeur (debout, un peu plus grands au premier plan). Réglage : *Pause → Vue : 3D / 2D (dessus)*, mémorisé. Le volley reste en vue du dessus. Code : `Arcade3D.kt` (`Persp`), `Px` (mode au sol / debout), `SceneCache.draw` (terrain recopié ligne par ligne). Force de la perspective : `Persp.C`, taille des joueurs : `Persp.BB`.
- **Dribbles de foot** : avec le ballon, bouton **TACLE** = dribble (joystick de côté = coupure, avant = roulette, arrière/neutre = drag-back). Pas de tacle possible pendant le dribble, défenseurs proches parfois déséquilibrés. Les bots dribblent aussi quand ils sont pressés. Sans le ballon, TACLE reste la glissade.

## Mise à jour basket (dribbles + vols des bots)
- **Dribbles** : avec la balle, bouton **DÉF** = feinte. Joystick de côté = **crossover**, vers l'avant = **demi-tour**, vers l'arrière ou neutre = **step-back** (bonus de +14 % au tir qui suit). Les défenseurs proches peuvent être pris à contre-pied (étourdis ~0,4 s) ; pendant la feinte, la balle ne peut pas être volée.
- **Les ennemis volent la balle** : portée 15 (avant 11), tentatives bien plus fréquentes, 28 % → 62 % de réussite selon le niveau (avant 10 % → 38 %), bonus par derrière ou si tu cours vite. Message « BALLE VOLÉE ! ». Les bots font aussi des feintes quand ils sont pressés.
- Code : `dribbleMove`, `stealBall`, `actBasketAi` (`ArcadeEngine.kt`) ; poses `CROSSOVER` / `SPIN` / `STEPBACK` (`ArcadeRender.kt`).

## Nouveautés v0.13 — Jeu plus lent, animations d'action, résolution ×2
- **Foot et basket ralentis** (environ 30 % : joueurs, ballon, IA et gardien) pour avoir le temps de réagir. Le chrono du match reste en temps réel ; le volley n'a pas changé. Réglage : `gameSpeed` dans `ArcadeEngine.kt` (0.70 foot, 0.72 basket, 1 = vitesse d'origine).
- **Une animation par action et par sport** :
  - Foot : frappe (jambe armée + bras de balancier), passe du plat du pied, tacle glissé (corps au ras du sol, puis relevé), gardien : arrêt à deux mains, repoussé du poing, lancer par-dessus l'épaule.
  - Basket : dribble (la main suit le rebond), tir en suspension (ballon au-dessus de la tête, puis poignet qui claque), passe à deux mains, vol (balayage du bras), contre (bras tendus en l'air).
  - Volley : réception (bras joints, genoux fléchis), passe (mains au front), smash (bras armé puis frappe, plus haut saut), service, plongeon.
  - Fin de point : l'équipe qui marque saute les bras en l'air, l'autre baisse la tête. Étincelle d'impact sur chaque geste, poussière derrière le tacle.
- **Résolution ×2 pour les joueurs et les ballons** : grille au quart de pixel (16× plus de cellules par sprite qu'avant la v0.9), contour noir plus fin, coins arrondis, ombrage plus doux, membres inclinés lissés, ballon dessiné sur 28×28 avec coutures plus fines, positions au quart de pixel (mouvements plus fluides). Anneaux et pointillés de visée plus fins.
- Aperçu des gestes générés avec le moteur de rendu : `Anim` (types) et `poseFor` (poses) dans `ArcadeEngine.kt` / `ArcadeRender.kt`.

## Nouveautés v0.10 — Décors, terrains, joueurs, gardien
- **15 décors** : un par sport et par difficulté (Foot : rue → stade de légende · Basket : playground → finale dorée · Volley : plage → finale mondiale), avec foule, panneaux LED, projecteurs, écrans géants, feux d'artifice, flashs, vagues…
- **Terrains plus détaillés** (demi-pixels, dessinés une seule fois puis mis en cache) : pelouse texturée selon la division, arcs de corner et de surface, points de penalty, filets à mailles, drapeaux ; parquet / bitume avec lignes, raquettes, tir à 3 points, paniers détaillés ; sable, herbe, bois, PVC pour le volley.
- **Joueurs plus détaillés** : plis du maillot, poignets, marque, oreilles, dents, protège-tibias, crampons, gants du gardien, bracelets (basket), genouillères (volley).
- **Foot** : bouton **⇄** pour changer de joueur quand tu n'as pas la balle (joueurs triés du plus proche au plus loin, puis le **gardien**).
- **Gardien jouable** : joystick = déplacement · TIR = plongeon (ou dégagement long s'il tient le ballon) · PASSE = relance vers un coéquipier. Retour automatique au joueur de champ si le jeu s'éloigne.
- **Guidage du ballon** : conduite assistée (le ballon reste devant tes pieds quand tu cours avec), passes qui s'orientent vers le receveur, anneau blanc sous le coéquipier visé.

Bibliothèque de jeux pour Android + bouton flottant, viseur et Helper ChatGPT.
Package inchangé (`com.nexus.gamelibrary`) : la mise à jour s'installe par-dessus l'ancienne version.

## Nouveautés v0.9 — Arcade : menus, skins, tutoriel
- **Haute définition** : joueurs, ballons et effets dessinés sur une grille 2× plus fine (4× plus de pixels par sprite), coiffures, maillots à motifs, numéros dans le dos, confettis après un but.
- **Un menu par mode** (Foot / Volley / Basket) : Carrière, Match rapide (Entraînement, Facile, Normal, Difficile, Expert ; 3 durées), Tutoriel, Mon style.
- **Mon style** : coiffure, couleurs, maillot + motif, short, accessoire, numéro, ballon, marqueur du joueur piloté, traînée du ballon, avec aperçu animé.
- **Cosmétiques débloqués par les étoiles** (total des 3 carrières) ; le déblocage est annoncé en fin de match.
- **Tutoriel** par sport (affiché au premier lancement de chaque sport, relisible à tout moment) + conseils à l'écran en mode Entraînement.
- **Volley corrigé** : le ballon reste dans la main du serveur jusqu'au service (plus de point perdu pendant que tu attends), service automatique après 7 s, repère au sol de l'impact du ballon, appui un peu trop tôt mémorisé, portée de frappe plus généreuse.

### Mise à jour v0.9.1
- **Nouveau design des joueurs** : style « chibi » plus détaillé, contour noir et ombrage automatiques, yeux avec reflet, sourcils, joues, nez, bouche, col de maillot, écusson, short à bandes, chaussettes à liseré, chaussures avec semelle, bras levés en tir / saut / plongeon.
- **Vol au basket facilité** : portée presque doublée, 72 % de réussite (plus si tu viens par derrière), contre plus facile, recharge réduite, et maintenir DÉF près du porteur tente le vol automatiquement.

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
