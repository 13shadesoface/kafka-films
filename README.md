# Statistiques de vues de films en temps réel


# Contexte

Vous êtes arrivé dans la société KazaaMovies, spécialisée dans le streaming de films à destination des particuliers.

Ils viennent de mettre en place après plusieurs années de développement une dimension sociale à leur offre de streaming, laquelle va vous fournir des informations sur les interactions utilisateurs et leurs vues, mais ils n'ont mis en place que la première partie technique : la récupération des données.

**Votre but: rentre utilisable ces données, lesquelles arrivent dans plusieurs topics Kafka.**

## Source de données

Il y a actuellement deux topics qui vous fournissent des données:

- `views`: il contient des évènements de visionnement d'un film
    - Structure :
        - ID du film
        - Titre du film
        - Catégorie de vue : start_only, half, full

```sql
{
    "id": 1,
    "title": "Kill Bill",
    "view_category": "half"
}
```

- `likes` : il contient les notes données aux films par les utilisateurs
    - On reçoit un évènement par note donnée
    - Structure
        - ID du film
        - Score

```sql
{
    "id": 1,
    "score": 4.8
}
```

L’ID du film vient de TMDB.

[The Movie Database (TMDB)](https://www.themoviedb.org/?language=fr)

# Projet

Il vous est demandé :

1. d’implémenter des statistiques sur ces messages ;
2. de tester unitairement votre topologie pour vous assurer de sa bonne implémentation ;
3. d’exposer ces statistiques via une API REST.

<aside>
💡 Vous serez évalué en contrôle continu sur un rendu des 2 premières étapes, en particulier sur la qualité et couverture des tests unitaires.

</aside>

L’ensemble sera ensuite évalué en soutenance de projet, sur :

- La qualité de votre implémentation (code, algorithmie)
- Son fonctionnement global (réponse aux spécifications ci-dessous)
- Votre compréhension individuelle des concepts employés.

## Fonctionnalités

Les fonctionnalités clés de l’application demandées sont :

- Nombre de vue par film, catégorisées par type de vue, depuis le lancement et sur les 5 dernières minutes :
    - arrêt en début de film (< 10% du film vu)
    - arrêt en milieu de film (< 90% du film vu)
    - film terminé (> 90% du film vu)
- Top 10 des films
    - ayant les meilleurs retours ( score moyen au dessus de 4)
    - ayant les pires retours (score moyen en dessous de 2)
    - ayant le plus de vues
    - ayant le moins de vues

## Exposition

Les fonctionnalités décrites doivent être exposées via une API REST avec le contrat suivant :

- `GET /movies/:id`
    - Donne le nombre de vues et leur distribution pour un ID de film donné

```json
{
  "id": 1,
  "title": "Movie title",
  "total_view_count": 200,
  "stats": {
    "past": {
      "start_only": 100,
      "half": 10,
      "full": 90
    },
    "last_five_minutes": {
      "start_only": 2000,
      "half": 100,
      "full": 90
    }
  }
}
```

- `GET /stats/ten/best/score`
    - Donne les 10 meilleurs films de tous les temps selon leur score moyen, trié par score décroissant

```json
[
  {
		"id": 99,
    "title": "movie title 1",
    "score": 9.98
  },
  {
		"id": 32,
    "title": "movie title 2",
    "score": 9.7
  },
  {
		"id": 42,
    "title": "movie title 3",
    "score": 8.1
  }
]
```

- `GET /stats/ten/best/views`
    - Donne les 10 meilleurs films de tous les temps selon leurs vues, trié par nombre de vues décroissant

```json
[
  {
		"id": 12,
    "title": "movie title 1",
    "views": 3500
  },
  {
		"id": 2,
    "title": "movie title 2",
    "views": 2800
  },
  {
		"id": 1,
    "title": "movie title 3",
    "views": 2778
  }
]
```

- `GET /stats/ten/worst/score`
    - Donne les 10 pires films de tous les temps selon leur score moyen, trié par score croissant

```json
[
  {
		"id": 2,
    "title": "movie title 1",
    "score": 2.1
  },
  {
		"id": 444,
    "title": "movie title 2",
    "score": 2.21
  },
  {
		"id": 100,
    "title": "movie title 3",
    "score": 3
  }
]
```

- `GET /stats/ten/worst/views`
    - Donne les 10 pires films de tous les temps selon leurs vues, trié par nombre de vues croissant

```json
[
  {
		"id": 200,
    "title": "movie title 1",
    "views": 2
  },
  {
		"id": 207,
    "title": "movie title 2",
    "views": 5
  },
  {
		"id": 119,
    "title": "movie title 3",
    "views": 12
  }
]
```

## Scope

Il n'est pas demandé que l'application implémentée pour ces besoins gère le lancement d'instances multiples (ce qui impliquerait la gestion de stores distribués et l'usage de RPC).

Toutefois, un effort fait en ce sens pourra compter comme bonus.

De même, un front faisant usage de cette API et de l’API TMDB compterait comme bonus.

## Environnement

Vous pouvez partir du squelette fourni pour le développement du projet.

https://github.com/nekonyuu/kafka-as-a-datahub-skeletons

Pour l’environnement local de développement, vous devez utiliser ce fichier docker-compose dans votre projet pour démarrer un cluster kafka et un simulateur de données, lequel vous permettra de développer votre projet !

[compose-movies-views.yml](https://prod-files-secure.s3.us-west-2.amazonaws.com/3aa67a0e-7ae5-48a7-babb-fdbcec3d607c/739739da-b1f7-44c8-931b-ccb9c7bc2d2b/compose-movies-views.yml)

## Documentation

- Kafka Stream DSL documentation: https://docs.confluent.io/current/streams/developer-guide/dsl-api.html
- Kafka Stream Scala doc: https://kafka.apache.org/20/documentation/streams/developer-guide/dsl-api.html#scala-dsl
- Kafka Stream Configuration Documentation: https://docs.confluent.io/current/streams/developer-guide/config-streams.html
- Kafka Streams Interactive Queries: https://docs.confluent.io/current/streams/developer-guide/interactive-queries.html
- Akka HTTP (REST API): https://doc.akka.io/docs/akka-http/current/routing-dsl/index.html
- Play JSON: https://www.playframework.com/documentation/2.8.x/ScalaJson