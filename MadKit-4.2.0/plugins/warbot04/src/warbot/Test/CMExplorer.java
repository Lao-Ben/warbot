package warbot.Test;

import warbot.kernel.*;

public class CMExplorer extends Brain {
	
	final static int FOOD_DIST_TO_HOME = 10;
	
	String groupName = "warbot-";
	final static String roleName = "Explorer";
	final static int maxStep = 8;
		
	/**
	 * step : used to keep direction during maxStep steps
	 */
	int step = 0;
	
	Percept myhome = null;
	double homeX = 0;
	double homeY = 0;

	public CMExplorer() {}

	public void activate() {
		groupName = groupName + getTeam(); // -> warbot-CM
		randomHeading(); // direction al�atoire
		println("Explorateur Test op�rationnel");
		createGroup(false, groupName, null, null);
		requestRole(groupName, roleName, null);
		requestRole(groupName, "mobile", null);
		this.setUserMessage((new Integer(this.bagSize())).toString());
	}

	int takeFood(Food p) {
		if (distanceTo(p) < 2) {
			if (getEnergyLevel() < getMaximumEnergy() / 2) {
				println(this.getName()+ " -- takeFood -- energy lvl : " + getEnergyLevel() + "/" + getMaximumEnergy());
				eat((Food) p);
				return 1;
			} else {
				take((Food) p);
				return 0;
			}
		} else {
			if (!isMoving()) // cas o� il a blocage : changement de direction
			{
				randomHeading();
			} else // on va vers l'autre
			{
				setHeading(towards(p.getX(), p.getY()));
			}
			move();
			return 2;
		}
	}

	public void doIt() {
		String chaineAide = "HELP-E";
		String chaineAtak = "ATAQ";
		double positionEnnemiX = 0;
		double positionEnnemiY = 0;
		String ennemiX = "";
		String ennemiY = "";
		WarbotMessage messCourant = null;

		if (!isMoving()) // si bloqu� : direction al�atoire
			randomHeading();

		while ((messCourant = readMessage()) != null) {
			// message d'attaque de base ennemie : les coordonn�es sont toujours
			// pr�sentes en argument du message
			if (messCourant.getAct() != null
					&& messCourant.getAct() == "TAKEFOOD") {
			}
			if (messCourant.getAct() != null
					&& messCourant.getAct() == "basepos") {
				homeX = messCourant.getFromX();
				homeY = messCourant.getFromY();
				broadcast(groupName, "Home", "ExplorerAlive");
			}
		}

		/*
		 * if (!isMoving() && myhome != null && bagSize() == getBagCapacity()) {
		 * move(); return; }
		 */

		Percept[] percepts = getPercepts(); // entit�s dans le p�rim�tre de
												// perception
		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept currentPercept = percepts[i];
			if (currentPercept.getTeam().equals(getTeam())
					&& currentPercept.getPerceptType().equals("Home")) // si objet courant = base alli�
			{
				myhome = currentPercept;
				break;
			}
			if (myhome == currentPercept)
				break;
		}

		if (myhome != null && distanceTo(myhome) < 2 && bagSize() > 0) {
			setUserMessage("drop all");
			dropAll();
			setUserMessage((new Integer(this.bagSize())).toString());
			return;
		} else if (isBagFull()) {
			if(!isMoving())
				randomHeading();
			else
				setHeading(towards(homeX, homeY));
			setUserMessage("going to base");
			move();
			return;
		}

		// 1. Si base ennemie trouv�e : broadcast
		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept currentPercept = percepts[i];
			if (!currentPercept.getTeam().equals(getTeam())
					&& currentPercept.getPerceptType().equals("Home")) // si objet
																		// courant
																		// =
																		// base
																		// ennemie
			{
				ennemiX = Double.toString(currentPercept.getX());
				ennemiY = Double.toString(currentPercept.getY());
				broadcast(groupName, "Launcher", chaineAtak, ennemiX, ennemiY);
			}
		}

		// 2. Si attaqu� : demande d'aide sans argument
		if (getShot()) {
			println("explorer attaqu�");
			broadcast(groupName, "Launcher", chaineAide, "0", "0");
		}

		// 3. si rocketlauncher ennemi rep�r�
		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept currentPercept = percepts[i];
			if (!currentPercept.getTeam().equals(getTeam())
					&& currentPercept.getPerceptType().equals("RocketLauncher")) // si
																				// RocketLauncher
																				// ennemi
			{
				// strat�gie d'�vitement
				positionEnnemiX = currentPercept.getX(); // abscisse de l'ennemi
				positionEnnemiY = currentPercept.getY(); // ordonn�e de l'ennemi
				ennemiX = Double.toString(currentPercept.getX());
				ennemiY = Double.toString(currentPercept.getY());
				// println("launcher ennemi rep�r�");
				// println(ennemiX);
				// println(ennemiY);
				broadcast(groupName, "Launcher", chaineAide, ennemiX, ennemiY);
				for (int j = 0; j < percepts.length; j++) // pour toutes les
																// entit�s
																// per�ues...
				{
					Percept currentPercept2 = percepts[j];
					if ((getHeading()
							- (towards(currentPercept2.getX(),
									currentPercept.getY())) >= 20)
							&& (currentPercept2.getPerceptType()
									.equals("Obstacle")))
					// si d�tecte un obstacle et que cet obstacle est dans le
					// champ de la direction de l'agent
					{
						if (step == 0) // si on vient de rep�rer
												// l'obstacle
						{
							step++;
							setHeading(towards(-positionEnnemiX / 2,
									-positionEnnemiY)); // on �vite ET l'ennemi,
														// Et l'obstacle
						} else {
							if (step == maxStep) {
								step = 0;
							} else // 0 < "current step" < maxStep 
							{
								step++;
							}
						}
						move();
						return;
					}
				}
				setHeading(towards(-positionEnnemiX, -positionEnnemiY)); // direction
																			// oppos�e
																			// �
																			// l'ennemi
																			// rep�r�
			}
		}

		// 5. Si nouriture
		for (int i = 0; i < percepts.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept currentPercept = percepts[i];
			
			// if food and not around a home of same team
			if (currentPercept.getPerceptType().equals("Food") && distanceTo(myhome) > FOOD_DIST_TO_HOME)
			{
				println(this.getName()+ " -- doIt -- dist2home : " + distanceTo(myhome));
				if (!isMyBagFull() ) {
					int j = takeFood((Food) currentPercept);
					if (j == 0)
						setUserMessage((new Integer(this.bagSize())).toString());
					else if (j == 1)
						setUserMessage("eating");
					else
						setUserMessage("Going to food");
					broadcast(groupName, "Explorer", "TAKEFOOD",
							Double.toString(currentPercept.getX()),
							Double.toString(currentPercept.getY()));
					return;
				}
			}
		}

		// 6. d�placement al�atoire (randomHeading au d�but)
		step = maxStep; // remise � 0 du compteur : on est hors de risque...
		move();
		return;
	}
}