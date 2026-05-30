# GeoTracker

## Description du projet

GeoTracker est une application Android developpee en Java permettant de collecter la position geographique d'un appareil via le GPS et de la transmettre en temps reel vers un serveur distant. Le serveur, developpe en PHP avec une base de donnees MySQL, stocke chaque position recue. Une deuxieme activite affiche toutes les positions enregistrees sous forme de marqueurs sur une carte interactive OpenStreetMap. L'application illustre la communication complete entre une application mobile, un backend PHP et une base de donnees, avec affichage cartographique des donnees collectees.

---

## Architecture generale

Le projet est compose de deux parties communicant via le protocole HTTP.

La partie serveur contient une base de donnees MySQL, des classes PHP organisees en couches separees, un script d'insertion des positions et un script de recuperation des positions au format JSON.

La partie mobile contient deux activites : `MainActivity` qui gere le GPS et l'envoi des donnees, et `MapsActivity` qui recupere les positions du serveur et les affiche sur une carte.

---

## Structure du projet serveur

```
localisation/
├── classe/
│   └── Position.php
├── connexion/
│   └── Connexion.php
├── dao/
│   └── IDao.php
├── service/
│   └── PositionService.php
├── createPosition.php
└── showPositions.php
```

---

## Base de donnees

La base de donnees se nomme `localisation` et contient une table `position` avec les champs suivants :

- `id` : identifiant auto-incremente
- `latitude` : coordonnee nord-sud de type DOUBLE
- `longitude` : coordonnee est-ouest de type DOUBLE
- `date_position` : date et heure d'enregistrement de type DATETIME
- `imei` : identifiant unique de l'appareil emetteur de type VARCHAR

---

## Fonctionnement du serveur PHP

**Classe Position**

La classe `Position` represente l'objet metier manipule cote serveur. Chaque instance correspond a une ligne de la table `position`. Les attributs sont prives et accessibles via des getters et setters conformement aux principes de l'encapsulation orientee objet.

**Classe Connexion**

La classe `Connexion` centralise la connexion a la base de donnees via PDO avec le mode d'erreur configure en exception. Elle met la connexion a disposition des autres classes via la methode `getConnexion()`.

**Interface IDao**

L'interface `IDao` definit le contrat CRUD standard : `create`, `update`, `delete`, `getById` et `getAll`. Elle rend l'architecture extensible et coherente.

**Classe PositionService**

La classe `PositionService` implemente `IDao` et fournit deux methodes actives. La methode `create()` insere une position en base via une requete SQL preparee avec des points d'interrogation comme placeholders. La methode `getAll()` retourne toutes les positions triees par date decroissante. Les noms de table et de colonnes sont entoures de backticks pour eviter les conflits avec les mots reserves MySQL.

**Script createPosition.php**

Ce script constitue le point d'entree pour l'insertion. Il accepte uniquement les requetes POST, valide la presence des quatre champs requis, instancie un objet `Position` et appelle le service pour l'insertion. Il retourne une reponse JSON detaillee incluant le statut, un message, les coordonnees enregistrees et l'adresse IP du client.

**Script showPositions.php**

Ce script retourne toutes les positions stockees en base au format JSON via une requete GET. La reponse inclut un indicateur de succes, le nombre total de positions et le tableau des enregistrements. Il est appele par `MapsActivity` pour alimenter la carte en marqueurs.

---

## Fonctionnement de l'application Android

**Permissions**

Cinq permissions sont declarees dans le manifeste : `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`, `ACCESS_NETWORK_STATE` et `WRITE_EXTERNAL_STORAGE`. Le trafic HTTP non chiffre est autorise via `android:usesCleartextTraffic="true"` pour permettre la communication avec le serveur local.

**Identifiant de l'appareil**

L'application utilise `Settings.Secure.ANDROID_ID` comme identifiant unique de l'appareil, ce qui fonctionne sur toutes les versions d'Android sans permission speciale. Cette approche remplace l'IMEI traditionnel, inaccessible depuis Android 10 pour les applications tierces.

**Activite principale**

`MainActivity` initialise le `LocationManager` et demande la permission de localisation dynamiquement au demarrage. Une fois la permission accordee, elle s'abonne aux mises a jour GPS avec un intervalle de 60 secondes et une distance minimale de 150 metres. A chaque nouvelle position recue, les quatre valeurs sont affichees dans le panneau de coordonnees, un message Toast est affiche, et la position est envoyee au serveur via une requete POST Volley. Un journal d'activite horodate trace tous les evenements dans l'ordre chronologique inverse. Un bouton permet d'acceder a la carte.

**Envoi des donnees**

La methode `sendPosition()` construit une requete `StringRequest` POST via Volley contenant quatre parametres : latitude, longitude, date au format `yyyy-MM-dd HH:mm:ss` et identifiant de l'appareil. La reponse JSON du serveur est consignee dans le journal d'activite.

**Activite cartographique**

`MapsActivity` affiche une carte OpenStreetMap via la bibliotheque osmdroid centree par defaut sur Casablanca. Au demarrage, elle envoie une requete GET vers `showPositions.php` et parse la reponse JSON. Pour chaque position recue, un marqueur est cree avec un titre numerote et un snippet affichant la date et l'identifiant de l'appareil. La carte est automatiquement animee vers la position la plus recente. Le nombre de marqueurs charges est affiche dans l'en-tete de l'activite.

**Cycle de vie de la carte**

Les methodes `onResume()` et `onPause()` appellent respectivement `mapView.onResume()` et `mapView.onPause()` pour gerer correctement les ressources osmdroid.

---

## Differences par rapport au sujet original

- Utilisation d'OpenStreetMap via osmdroid au lieu de Google Maps, eliminant le besoin d'une cle API
- Identifiant de l'appareil base sur `ANDROID_ID` au lieu de l'IMEI, compatible avec toutes les versions Android
- Journal d'activite horodate tracant tous les evenements GPS et reseau
- Affichage complet des donnees GPS : latitude, longitude, altitude et precision
- Interface organisee en cartes MaterialCardView avec theme vert fonce
- Script `showPositions.php` appele en GET au lieu de POST
- Marqueurs enrichis affichant la date et l'identifiant de l'appareil dans le snippet
- Carte animee automatiquement vers la position la plus recente au chargement
- Compteur de marqueurs affiche dans l'en-tete de la carte
- Autorisation du trafic HTTP via `usesCleartextTraffic` au lieu d'un fichier de configuration reseau separe

---

## Installation et configuration

**Serveur :**
1. Placer le dossier `localisation` dans `C:/xampp/htdocs/`
2. Demarrer Apache et MySQL via le panneau de controle XAMPP
3. Ouvrir phpMyAdmin et executer le script SQL fourni
4. Verifier que la table `position` contient bien les colonnes `id`, `latitude`, `longitude`, `date_position` et `imei`
5. Tester `http://localhost/localisation/showPositions.php` dans le navigateur

**Application Android :**
1. Ouvrir le projet dans Android Studio
2. Laisser Gradle synchroniser les dependances automatiquement
3. Lancer sur un emulateur Android Studio
4. Accepter la permission de localisation au demarrage
5. Simuler une position GPS via Extended Controls puis cliquer Set Location
6. Verifier l'insertion dans phpMyAdmin
7. Appuyer sur View Map pour visualiser les positions sur la carte

---

## Technologies utilisees

- Langage Android : Java
- Environnement : Android Studio
- SDK minimum : API 24 (Android 7.0)
- Bibliotheque reseau : Volley 1.2.1
- Bibliotheque cartographique : osmdroid 6.1.17 avec tuiles OpenStreetMap MAPNIK
- Serveur : PHP avec PDO
- Base de donnees : MySQL via phpMyAdmin
- Composants principaux : LocationManager, LocationListener, ANDROID_ID, StringRequest, JsonObjectRequest, MapView, Marker, MaterialCardView

  

