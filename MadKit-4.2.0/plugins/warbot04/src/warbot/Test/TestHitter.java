package warbot.Test;

import java.util.Random;

import warbot.kernel.Brain;
import warbot.kernel.Percept;
import warbot.kernel.WarbotMessage;

public class TestHitter extends Brain{
	String groupName = "warbot-";
	String roleName = "Hitter";

	final int tempsMax = 10;
	int temps = tempsMax; // variable permettant de garder la meme direction
							// pendant tempsMax it�rations

	Percept myhome = null;
	double homeX = 0;
	double homeY = 0;

	final int maxStep = 8;
	int step = 0;
	boolean sendAlive = false;

	public TestHitter() {
	}

	public void activate() {
		groupName = groupName + getTeam();
		randomHeading();
		println("Hitter Test op�rationnel");
		createGroup(false, groupName, null, null);
		requestRole(groupName, roleName, null);
		requestRole(groupName, "mobile", null);
	}
	
	public void end()
	{
	    broadcast(groupName, "Home", Constants.MSG_HITTERDEAD);
	    println("ID Dead (me-hitter) : "+getAddress().getLocalID());
	}

	public void desobstination() {
		//
		if (!isMoving()) // blocage
		{
			if (temps != tempsMax) {
				temps = tempsMax;
			}
			randomHeading();
			temps--;
			move();
			return;
		}
		if (temps < tempsMax && temps > 0) // direction al�atoire a d�j� �t�
											// prise
		{
			temps--;
			move();
			return;
		}
		if (temps == 0) // temps de d�sobstination �coul�
		{
			temps = tempsMax;
		}
	}

	public void doIt() {
		// variables pour gestion des messages
		double[][] tabAtaq = new double[4][100]; // tableau des message
													// d'attaque : premi�re
													// colonne contient l'X,
													// deuxi�me colonne contient
													// l'Y de la base
		double[][] tabHelpBL = new double[4][100]; // tableau des message d'aide
													// venant des bases
													// concernant des launchers
													// : premi�re colonne X de
													// l'ennemi si non vide,
													// deuxi�me colonne Y de
													// l'ennemi si non vide,
													// troisi�me colonne X
													// envoyeur, quatri�me
													// colonne Y envoyeur
		double[][] tabHelpBE = new double[4][100]; // tableau des message d'aide
													// venant des bases
													// concernant des explorers
													// : premi�re colonne X de
													// l'ennemi si non vide,
													// deuxi�me colonne Y de
													// l'ennemi si non vide,
													// troisi�me colonne X
													// envoyeur, quatri�me
													// colonne Y envoyeur
		double[][] tabHelpE = new double[4][100]; // tableau des message d'aide
													// venant des explorers :
													// premi�re colonne X de
													// l'ennemi si non vide,
													// deuxi�me colonne Y de
													// l'ennemi si non vide,
													// troisi�me colonne X
													// envoyeur, quatri�me
													// colonne Y envoyeur
		double[][] tabHelpL = new double[4][100]; // tableau des message d'aide
													// venant des launchers :
													// premi�re colonne X de
													// l'ennemi si non vide,
													// deuxi�me colonne Y de
													// l'ennemi si non vide,
													// troisi�me colonne X
													// envoyeur, quatri�me
													// colonne Y envoyeur
		int comptAtaq = 0; // compteur du tableau tabAtaq
		int comptHelpBL = 0; // compteur du tableau tabHelpBL
		int comptHelpBE = 0; // compteur du tableau tabHelpBE
		int comptHelpE = 0; // compteur du tableau tabHelpE
		int comptHelpL = 0; // compteur du tableau tabHelpL
		int tailleAtaq = 0; // taille finale des diff�rents tableaux
		int tailleHelpBL = 0; // taille finale des diff�rents tableaux
		int tailleHelpBE = 0; // taille finale des diff�rents tableaux
		int tailleHelpE = 0; // taille finale des diff�rents tableaux
		int tailleHelpL = 0; // taille finale des diff�rents tableaux
		// variable pour gestion des objets per�us
		double[][] tabMyTeam = new double[2][50]; // tableau contenant les
													// coordonn�es relatives des
													// membres de notre �quipe
		double[] tabLauncher = new double[4]; // tableau contenant les
												// coordonn�es relatives,
												// l'�nergie et la distance du
												// launcher ennemi ayant le
												// moins d'�nergie tout en �tant
												// le plus proche
		double[] tabExplorer = new double[4]; // tableau contenant les
												// coordonn�es relatives,
												// l'�nergie et la distance de
												// l'explorer ennemi ayant le
												// moins d'�nergie tout en �tant
												// le plus proche
		double[] tabBase = new double[4]; // tableau contenant les coordonn�es
											// relatives, l'�nergie et la
											// distance de la base ennemie ayant
											// le moins d'�nergie tout en �tant
											// la plus proche
		int comptMyTeam = 0; // compteur du tableau tabMyTeam
		int tailleMyTeam = 0; // taille finale du tableau tabMyTeam
		// initialisation de l'�nergie et de la distance dans les tableaux des
		// ennemis per�us
		tabLauncher[2] = 0;
		tabLauncher[3] = 0;
		tabExplorer[2] = 0;
		tabExplorer[3] = 0;
		double[] tabRocket = new double[3];
		tabRocket[0] = 0;
		tabRocket[1] = 0;
		tabRocket[2] = 0;
		tabBase[2] = 0;
		tabBase[3] = 0;
		// variables de gestion des distances
		int distanceEnnemi = 80;
		int distanceAmis = 80; // distance � respecter pour �viter obstination
								// rejoindre amis (cas Y)
		int distanceMinAmis = 30; // distance au del� de laquelle il ne faut pas
									// s'approcher des amis (cas Y)
		// variables pour l'envoi de messages
		String actMessageAide = "HELP-L"; // cas o� risque d'attaque ennemie
		String actMessageAttaque = "ATAQ"; // cas de d�tection de la base
											// ennemie
		String argMessageX = ""; // pour envoyer position relative en X de
									// l'ennemi
		String argMessageY = ""; // pour envoyer position relative en Y de
									// l'ennemi
		WarbotMessage currentMsg = null;
		// variables diverses
		double directionX = 0;
		double directionY = 0;
		int seuilEnergieBase = 6000; // �nergie seuil pour "se d�fendre" vs
										// "se sacrifier"
		boolean baseAlive = false;
		
		
		Percept EnnemyBase = null;
		Percept EnnemyExplorer = null;
		Percept EnnemyRocketLauncher = null;

		// r�cup�ration et classement des messages
		while ((currentMsg = readMessage()) != null) {
			// message d'attaque de base ennemie : les coordonn�es sont toujours
			// pr�sentes en argument du message
			if (currentMsg.getAct() != null && currentMsg.getAct() == "ATAQ") {
				tabAtaq[0][comptAtaq] = Double.valueOf(currentMsg.getArg1())
						.doubleValue();
				tabAtaq[1][comptAtaq] = Double.valueOf(currentMsg.getArg2())
						.doubleValue();
				tabAtaq[2][comptAtaq] = currentMsg.getFromX();
				tabAtaq[3][comptAtaq] = currentMsg.getFromY();
				comptAtaq++;
			}
			// message d'aide d'une base concernant les launchers : les
			// coordonn�es de l'ennemi sont toujours pr�sentes en argument du
			// message
			if (currentMsg.getAct() != null && currentMsg.getAct() == "HELP-BL") {
				tabHelpBL[0][comptHelpBL] = Double
						.valueOf(currentMsg.getArg1()).doubleValue();
				tabHelpBL[1][comptHelpBL] = Double
						.valueOf(currentMsg.getArg2()).doubleValue();
				tabHelpBL[2][comptHelpBL] = currentMsg.getFromX();
				tabHelpBL[3][comptHelpBL] = currentMsg.getFromY();
				comptHelpBL++;
			}
			// message d'aide d'une base concernant les explorers: les
			// coordonn�es de l'ennemi sont toujours pr�sentes en argument du
			// message
			if (currentMsg.getAct() != null && currentMsg.getAct() == "HELP-BE") {
				tabHelpBE[0][comptHelpBE] = Double
						.valueOf(currentMsg.getArg1()).doubleValue();
				tabHelpBE[1][comptHelpBE] = Double
						.valueOf(currentMsg.getArg2()).doubleValue();
				tabHelpBE[2][comptHelpBE] = currentMsg.getFromX();
				tabHelpBE[3][comptHelpBE] = currentMsg.getFromY();
				comptHelpBE++;
			}
			// message d'aide d'un launcher : les coordonn�es de l'ennemi sont
			// toujours pr�sentes en argument du message
			if (currentMsg.getAct() != null && currentMsg.getAct() == "HELP-L") {
				// un launcher ne prend pas en compte ses propres messages
				if (currentMsg.getFromX() != 0 && currentMsg.getFromY() != 0) {
					tabHelpL[0][comptHelpL] = Double.valueOf(
							currentMsg.getArg1()).doubleValue();
					tabHelpL[1][comptHelpL] = Double.valueOf(
							currentMsg.getArg2()).doubleValue();
					tabHelpL[2][comptHelpL] = currentMsg.getFromX();
					tabHelpL[3][comptHelpL] = currentMsg.getFromY();
					comptHelpL++;
				}
			}
			// message d'aide d'un explorer : pas de coordonn�es de l'ennemi en
			// argument si message suite � tir
			if (currentMsg.getAct() != null && currentMsg.getAct() == "HELP-E") {
				tabHelpE[0][comptHelpE] = Double.valueOf(currentMsg.getArg1())
						.doubleValue();
				tabHelpE[1][comptHelpE] = Double.valueOf(currentMsg.getArg2())
						.doubleValue();
				tabHelpE[2][comptHelpE] = currentMsg.getFromX();
				tabHelpE[3][comptHelpE] = currentMsg.getFromY();
				comptHelpE++;
			}
			if (currentMsg.getAct() != null && currentMsg.getAct() == Constants.MSG_BASEPOS) {
				baseAlive = true;
				homeX = currentMsg.getFromX();
				homeY = currentMsg.getFromY();
				if (!sendAlive)
				{
					broadcast(groupName, "Home", Constants.MSG_HITTERALIVE);
					sendAlive = true;
				}
			}
		}
		tailleAtaq = comptAtaq;
		tailleHelpBL = comptHelpBL;
		tailleHelpBE = comptHelpBE;
		tailleHelpE = comptHelpE;
		tailleHelpL = comptHelpL;
		comptAtaq = 0;
		comptHelpBL = 0;
		comptHelpBE = 0;
		comptHelpE = 0;
		comptHelpL = 0;
		// fin r�cup�ration et classement des messages

		// r�cup�ration et classement des objets per�us
		Percept[] percepts = getPercepts(); // entities in the perception radius

		for (int i = 0; i < percepts.length; i++) {
			Percept currentPercept = percepts[i];
			if (myhome == currentPercept)
				break;
			if (currentPercept.getTeam().equals(getTeam())
					&& currentPercept.getPerceptType().equals("Home")) {
				myhome = currentPercept;
				break;
			}
		}

		if (baseAlive && getRocketNumber() == 0) {
			if (myhome != null && distanceTo(myhome) < 2) {
				reloadRocket();
				setHeading(towards(-myhome.getX(), -myhome.getY()));
				return;
			} else {
				if (!isMoving())
					randomHeading();
				else
					setHeading(towards(homeX, homeY));
				move();
				return;
			}
		}

		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
													// per�ues...
		{
			Percept objetCourant = percepts[i];
			if (objetCourant.getTeam().equals(getTeam())) // objet de mon �quipe
			{
				if (!objetCourant.getPerceptType().equals("Rocket")) {
					tabMyTeam[0][comptMyTeam] = objetCourant.getX();
					tabMyTeam[1][comptMyTeam] = objetCourant.getY();
					comptMyTeam++;
				}
			} else // on ne garde que les ennemis d'�nergie minimum les plus
					// pr�s de chaque type
			{
				// ennemi de type base
				if (objetCourant.getPerceptType().equals("Home")) // objet
																	// "ennemi"
																	// de type
																	// base
				{
					if (objetCourant.getEnergy() < tabBase[2]
							|| tabBase[2] == 0) {
						tabBase[0] = objetCourant.getX();
						tabBase[1] = objetCourant.getY();
						tabBase[2] = objetCourant.getEnergy();
						tabBase[3] = objetCourant.getDistance();
						EnnemyBase = objetCourant;
					} else if (objetCourant.getEnergy() == tabBase[2]
							&& objetCourant.getDistance() < tabBase[3]) {
						tabBase[0] = objetCourant.getX();
						tabBase[1] = objetCourant.getY();
						tabBase[2] = objetCourant.getEnergy();
						tabBase[3] = objetCourant.getDistance();
						EnnemyBase = objetCourant;
					}
				}
				// ennemi de type explorer
				if (objetCourant.getPerceptType().equals("Explorer")) // objet
																		// "ennemi"
																		// de
																		// type
																		// explorer
				{
					if (objetCourant.getEnergy() < tabExplorer[2]
							|| tabExplorer[2] == 0) {
						tabExplorer[0] = objetCourant.getX();
						tabExplorer[1] = objetCourant.getY();
						tabExplorer[2] = objetCourant.getEnergy();
						tabExplorer[3] = objetCourant.getDistance();
						EnnemyExplorer = objetCourant;
					} else if (objetCourant.getEnergy() == tabExplorer[2]
							&& objetCourant.getDistance() < tabExplorer[3]) {
						tabExplorer[0] = objetCourant.getX();
						tabExplorer[1] = objetCourant.getY();
						tabExplorer[2] = objetCourant.getEnergy();
						tabExplorer[3] = objetCourant.getDistance();
						EnnemyExplorer = objetCourant;
					}
				}
				// ennemi de type launcher
				if (objetCourant.getPerceptType().equals("RocketLauncher")) // objet
																			// "ennemi"
																			// de
																			// type
																			// launcher
				{
					if (objetCourant.getEnergy() < tabLauncher[2]
							|| tabLauncher[2] == 0) {
						tabLauncher[0] = objetCourant.getX();
						tabLauncher[1] = objetCourant.getY();
						tabLauncher[2] = objetCourant.getEnergy();
						tabLauncher[3] = objetCourant.getDistance();
						EnnemyRocketLauncher = objetCourant;
					} else if (objetCourant.getEnergy() == tabLauncher[2]
							&& objetCourant.getDistance() < tabLauncher[3]) {
						tabLauncher[0] = objetCourant.getX();
						tabLauncher[1] = objetCourant.getY();
						tabLauncher[2] = objetCourant.getEnergy();
						tabLauncher[3] = objetCourant.getDistance();
						EnnemyRocketLauncher = objetCourant;
					}
				}
				// rocket detected
				if (objetCourant.getPerceptType().equals("Rocket")
						&& distanceTo(objetCourant) < 4
						&& !objetCourant.getTeam().equals(getTeam())) {
					if (distanceTo(objetCourant) < tabRocket[2]
							|| tabRocket[2] == 0) {
						tabRocket[0] = objetCourant.getX();
						tabRocket[1] = objetCourant.getY();
						tabRocket[2] = distanceTo(objetCourant);
						// setUserMessage("detect rocket");
					}
				}
			}
		}
		tailleMyTeam = comptMyTeam;
		comptMyTeam = 0;
		// fin r�cup�ration et classement des objets per�us

		// on ordonne les actions principalement en fonction des objets per�us
		// et des messages re�us
		if (tabRocket[2] != 0) {
			setUserMessage("detect rocket");
			for (int j = 0; j < percepts.length; j++) {
				Percept currentPercept2 = percepts[j];
				if ((getHeading()
						- (towards(currentPercept2.getX(), tabRocket[1])) >= 20)
						&& (currentPercept2.getPerceptType().equals("Obstacle"))) {
					if (step == 0) {
						step++;
						setHeading(towards(-tabRocket[0] / 2, -tabRocket[1]));
					} else {
						if (step == maxStep)
							step = 0;
						else
							step++;
					}
					move();
					return;
				}
			}
			int rand = (new Random()).nextInt() % 4;
			if (rand == 0)
				setHeading(towards(tabRocket[0], -tabRocket[1])); // direction
			else if (rand == 1)
				setHeading(towards(-tabRocket[0], tabRocket[1])); // direction
			else if (rand == 2)
				setHeading(towards(-tabRocket[0], -tabRocket[1] / 2)); // direction
			else
				setHeading(towards(-tabRocket[0] / 2, -tabRocket[1])); // direction
			move();
			return;
		}
		// A : une base ennemie est � port�e et le launcher est en position de
		// tirer et/ou de se sacrifier
		if (tabBase[2] != 0 && tabBase[3] != 0) {
			// pr�vient les autres
			argMessageX = Double.toString(tabBase[0]);
			argMessageY = Double.toString(tabBase[1]);
			setUserMessage(""+distanceTo(EnnemyBase));
			if (distanceTo(EnnemyBase) < 10)
			{
				broadcast(groupName, "Launcher", actMessageAttaque, argMessageX,
					argMessageY);
				if ((!getShot()) || (getShot() && tabBase[2] < seuilEnergieBase)) {
					setUserMessage("ATTACK");
					// tire
					Hit(towards(tabBase[0], tabBase[1]));
					// launchRocket(towards(tabBase[0],tabBase[1]));
					return;
				}
			}
			else
			{
				setHeading(towards(tabBase[0], tabBase[1]));
				move();
				return;
			}
		}

		// B : un launcher ennemi est � port�e
		if (tabLauncher[2] != 0 && tabLauncher[3] != 0) {
			// demande aide
			argMessageX = Double.toString(tabLauncher[0]);
			argMessageY = Double.toString(tabLauncher[1]);
			setUserMessage(""+distanceTo(EnnemyRocketLauncher));
			if (distanceTo(EnnemyRocketLauncher) < 10)
			{
				broadcast(groupName, "Launcher", actMessageAide, argMessageX,
						argMessageY);
				setUserMessage("ATTACK");
				// tire
				Hit(towards(tabLauncher[0], tabLauncher[1]));
				// launchRocket(towards(tabLauncher[0],tabLauncher[1]));
				return;
			}
			else
			{
				setHeading(towards(tabLauncher[0], tabLauncher[1]));
				move();
				return;
			}
		}

		// C : r�ception de demande d'aide de la base car launchers ennemis
		// rep�r�s dans zone de s�curit�
		if (tailleHelpBL > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			/* il faudrait s�lectionner la base la plus rapproch�e */
			// arbitrairement on va vers le dernier launcher ayant envoy� un
			// message
			directionX = tabHelpBL[0][tailleHelpBL - 1]
					+ tabHelpBL[2][tailleHelpBL - 1];
			directionY = tabHelpBL[1][tailleHelpBL - 1]
					+ tabHelpBL[3][tailleHelpBL - 1];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// D : r�ception de demande d'aide d'un launcher attaqu�
		if (tailleHelpL > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			int inddist = 0;
			double distmax = 0;
			for (int i = 0; i < tailleHelpL; i++) {
				double dist = distanceToAbsolute(tabHelpL[0][i]
						+ tabHelpL[2][i], tabHelpL[1][i] + tabHelpL[3][i]);
				if (dist < distmax || distmax == 0) {
					distmax = dist;
					inddist = i;
				}
			}
			directionX = tabHelpL[0][inddist] + tabHelpL[2][inddist];
			directionY = tabHelpL[1][inddist] + tabHelpL[3][inddist];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// E : r�ception de demande d'aide d'un explorer attaqu�
		if (tailleHelpE > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			int inddist = 0;
			double distmax = 0;
			for (int i = 0; i < tailleHelpE; i++) {
				double dist = distanceToAbsolute(tabHelpE[0][i]
						+ tabHelpE[2][i], tabHelpE[1][i] + tabHelpE[3][i]);
				if (dist < distmax || distmax == 0) {
					distmax = dist;
					inddist = i;
				}
			}
			directionX = tabHelpE[0][inddist] + tabHelpE[2][inddist];
			directionY = tabHelpE[1][inddist] + tabHelpE[3][inddist];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// F : r�ception d'un message d'attaque de la base ennemie
		if (tailleAtaq > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			directionX = tabAtaq[0][tailleAtaq - 1]
					+ tabAtaq[2][tailleAtaq - 1];
			directionY = tabAtaq[1][tailleAtaq - 1]
					+ tabAtaq[3][tailleAtaq - 1];
			/*
			 * a voir : on ne bouge vers la base uniquement si on n'est pas d�j�
			 * dessus
			 */
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// W : on a pour objet per�u un explorer
		if (tabExplorer[2] != 0 && tabExplorer[3] != 0) {
			setUserMessage(""+distanceTo(EnnemyExplorer));
			if (distanceTo(EnnemyExplorer) < 10)
			{
				setUserMessage("ATTACK");
				// tire
				Hit(towards(tabExplorer[0], tabExplorer[1]));
				// launchRocket(towards(tabExplorer[0],tabExplorer[1]));
				return;
			}
			else
			{
				setHeading(towards(tabExplorer[0], tabExplorer[1]));
				move();
				return;
			}
		}

		// X : r�ception de demande d'aide de la base car explorers ennemis
		// rep�r�s dans zone de s�curit�
		if (tailleHelpBE > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			/* il faudrait s�lectionner la base la plus rapproch�e */
			// arbitrairement on va vers le dernier launcher ayant envoy� un
			// message
			directionX = tabHelpBE[0][tailleHelpBE - 1]
					+ tabHelpBE[2][tailleHelpBE - 1];
			directionY = tabHelpBE[1][tailleHelpBE - 1]
					+ tabHelpBE[3][tailleHelpBE - 1];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// Y : rejoindre ami per�u
		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
													// per�ues...
		{
			Percept objetCourant = percepts[i];
			if (objetCourant.getTeam().equals(getTeam())
					&& objetCourant.getPerceptType().equals("RocketLauncher")
					&& distanceTo(objetCourant) > distanceAmis
					&& distanceTo(objetCourant) < distanceMinAmis) {
				if (!isMoving()) // cas o� il a blocage : changement de
									// direction
				{
					randomHeading();
				} else // on va vers l'autre
				{
					setHeading(towards(objetCourant.getX(), objetCourant.getY())); // r�cup�re
																					// la
																					// direction
																					// de
																					// l'ami
				}
				move();
				return;
			}
		}
		// Z : d�placement al�atoire
		step = maxStep;
		if (getRocketNumber() < 10)
			buildRocket();
		else {
			if (!isMoving())
				randomHeading();
			move();
		}
		return;
	}
}
