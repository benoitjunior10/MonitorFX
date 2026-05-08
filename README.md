# MonitorFX

> Plateforme de surveillance système en temps réel — construite avec Java 21 et JavaFX.

MonitorFX est une application de bureau qui affiche en temps réel les métriques matérielles (CPU, RAM, uptime) ainsi que la liste complète des processus en cours d'exécution, avec la possibilité de les terminer depuis l'interface.

---

## Fonctionnalités

- **Surveillance CPU & RAM** — taux d'utilisation, mémoire totale/libre/utilisée, nombre de processeurs logiques
- **Informations système** — système d'exploitation, version, architecture, uptime formaté
- **Liste des processus** — PID, nom, utilisateur, heure de démarrage, durée CPU cumulée, état (actif / terminé)
- **Recherche de processus** — filtrage en direct dans la liste
- **Arrêt de processus** — terminaison normale avec fallback forcé, confirmation avant action
- **Rafraîchissement automatique** — métriques toutes les secondes, processus toutes les 4 secondes
- **Rafraîchissement manuel** — boutons dédiés dans l'interface
- **Interface responsive** — s'adapte automatiquement à la résolution de l'écran, taille minimale garantie

---

## Architecture

Le projet suit le pattern **MVC** avec une séparation stricte des responsabilités :

```
com.monitorfx/
├── MonitorFX.java                  # Point d'entrée (Application JavaFX)
├── controller/
│   └── DashboardController.java    # Contrôleur principal — orchestre vue et services
├── model/
│   ├── SystemMetrics.java          # Record immuable des métriques système
│   └── ProcessRecord.java          # Record immuable représentant un processus
├── service/
│   ├── SystemMetricsService.java   # Collecte CPU, RAM, OS via JMX
│   └── ProcessService.java         # Collecte et arrêt de processus via ProcessHandle
├── util/
│   ├── RefreshScheduler.java       # Planificateur de tâches concurrentes (daemon threads)
│   └── Formatters.java             # Utilitaires de formatage (Go, %, durées)
└── view/
    ├── DashboardView.java          # Vue principale JavaFX (tableau de bord)
    ├── MetricCard.java             # Composant carte de métrique réutilisable
    └── MaterialIcon.java           # Icônes SVG Material intégrées
```

**Flux de données :**

```
RefreshScheduler (daemon threads)
        │
        ▼
DashboardController
   ├── SystemMetricsService  →  SystemMetrics  →  DashboardView.updateMetrics()
   └── ProcessService        →  ProcessRecord  →  DashboardView.updateProcesses()
                                                    (via Platform.runLater)
```

---

## Prérequis

| Outil        | Version minimale |
|--------------|-----------------|
| Java (JDK)   | 21              |
| JavaFX SDK   | 21              |
| Maven        | 3.8+            |

> **Note :** JavaFX n'est plus inclus dans le JDK standard depuis Java 11. Il doit être installé séparément ou géré via Maven (dépendance `org.openjfx`).

---

## Installation et lancement

### 1. Cloner ou extraire le projet

```bash
unzip MonitorFX.zip
cd MonitorFX
```

### 2. Compiler

```bash
mvn compile
```

### 3. Lancer l'application

```bash
mvn javafx:run
```

Ou via NetBeans, en utilisant le fichier `nbactions.xml` fourni (action **Run Project**).

---

## Raccourcis et comportements clés

| Action                       | Comportement                                                      |
|------------------------------|-------------------------------------------------------------------|
| Rafraîchir les métriques     | Bouton dédié ou automatique toutes les **1 seconde**              |
| Rafraîchir les processus     | Bouton dédié ou automatique toutes les **4 secondes**             |
| Rechercher un processus      | Saisie dans le champ de recherche — filtrage instantané           |
| Terminer un processus        | Sélectionner un processus → cliquer sur Terminer → confirmer      |
| Fermeture de l'application   | Arrêt propre du planificateur via `Stage.onCloseRequest`          |

---

## Détails techniques

### Collecte des métriques système

`SystemMetricsService` utilise `com.sun.management.OperatingSystemMXBean` (extension JVM privée) pour accéder à la charge CPU de l'ensemble du système (`getCpuLoad()`) et à la mémoire physique réelle (`getTotalMemorySize()`, `getFreeMemorySize()`).

### Gestion des processus

`ProcessService` s'appuie sur l'API standard `java.lang.ProcessHandle` (Java 9+). La terminaison suit une stratégie en deux temps :

1. Tentative d'arrêt normal (`destroy()`) — délai de 2 secondes
2. Si échec : arrêt forcé (`destroyForcibly()`) — délai de 3 secondes

L'application ne peut pas terminer son propre processus (protection explicite).

### Concurrence

`RefreshScheduler` encapsule un `ScheduledExecutorService` à 3 threads daemon nommés (`monitor-worker-N`). Les drapeaux `AtomicBoolean` dans le contrôleur empêchent les exécutions concurrentes redondantes (anti-double-refresh). Toutes les mises à jour de la vue sont redirigées vers le thread JavaFX via `Platform.runLater()`.

---

## Dépendances

| Artifact                       | Version | Rôle                         |
|-------------------------------|---------|------------------------------|
| `org.openjfx:javafx-controls` | 21      | Interface graphique JavaFX   |

---

## Limitations connues

- La collecte CPU (`getCpuLoad()`) utilise une API interne (`com.sun`) non garantie dans les versions futures du JDK.
- L'arrêt de certains processus système peut nécessiter des **privilèges élevés** (administrateur / root).
- Les processus appartenant à d'autres utilisateurs peuvent afficher `N/A` pour le champ utilisateur selon les permissions de l'OS.

---

## Contribution

Ce projet a été développé en équipe dans le cadre d'un projet académique. Chaque module correspond à une responsabilité assignée :

| Module                  | Responsabilité                          |
|-------------------------|-----------------------------------------|
| `SystemMetricsService`  | Surveillance RAM + CPU                  |
| `ProcessService`        | Collecte et arrêt des processus         |
| `RefreshScheduler`      | Concurrence et orchestration            |
| `DashboardView`         | Interface graphique et composants       |

---

## Licence

Projet académique — tous droits réservés aux auteurs.
