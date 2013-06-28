Fonctionnalites ajoutees :

  Collecte de nourriture
    Les explorers ont 2 roles distincts :
    - explorer : il envoie a la base la position de chaque nourriture rencontree au cours de son exploration.
    - collecter : la base indique a certain explorers (en priorite ceux qui ont ete utilise recemment comme collecteur) d'aller chercher de la nourriture a une position donnee. Une fois la nourriture collectee celle ci est apportee a la base (si le sac est plein ou si l'on a plus besoin de ce collecteur).

  Recuperation de munitions
    Les RocketLaunchers peuvent récupérer des munitions en passant près de leur base.

  Deplacement en formation (pour les combatants)
    Les combatants de meme type communiquent entre eux pour se grouper et choisir leur role (leader, left ou right) dans la formation.
    
  Production d'agents
    Génération de nouveaux membres dans l'équipe grâce à la nourriture collectee.

  Esquive des tirs
    Pour chaque entité, nous avons également fait en sorte qu'elle puisse essayer d'esquiver les balles des ennemis.
Nous avons également fait que les entités essaient de ne pas se rentrer dedans.
  
  Attaque au corps a corps pour le RocketLauncher
    En parallèle de ce nouveau type de combattant, nous avons fait en sorte que les RocketLauncher attaquent eux aussi au corps à corps dans le cas où ils n'ont plus de munitions.

  La base envoie également un message aux différents agents en fonction de la situation et en particulier en cas de détection d'un ennemis pour appeler les combattants de son équipe pour se défendre.
  

Modifications de Warbot :

  Limitation du nombre de munitions dont dispose les RocketLaunchers.
  
  Nouveau type de combattant, au corps à corps, ne consommant donc plus de munitions.

  Amelioration du placement des agents generes par une base. Les agents crees ne sont crees sur un emplacement libre autour de la base ou plus tard si aucun emplacement de libre n'est disponible. Les agents n'apparaissent donc plus sur des agents existant.

  Ajout de la possibilite de deposer un objet crossable sur un agent (plus logique et permet de deposer la nourriture sur la base).  

