package warbot.Test;

import java.util.ArrayList;
import java.util.List;

import warbot.kernel.*;

public class CMExplorer extends Brain {
	String groupName = "warbot-";
	String roleName = "Explorer";

	int memeDirection = 30;
	int tempsMax = 8;
	int temps = tempsMax; // variable permettant de garder la meme direction
							// pendant tempsMax it�rations
	Percept myhome = null;
	double homeX = 0;
	double homeY = 0;
	int energ = 0;
	List<double[]> tabfoodtake = new ArrayList<double[]>();
	List<double[]> tabfoodtakebyme = new ArrayList<double[]>();

	public CMExplorer() {
	}

	public void activate() {
		groupName = groupName + getTeam(); // -> warbot-CM
		randomHeading(); // direction al�atoire
		println("Explorateur Test op�rationnel");
		createGroup(false, groupName, null, null);
		requestRole(groupName, roleName, null);
		requestRole(groupName, "mobile", null);
		this.setUserMessage((new Integer(this.bagSize())).toString());
		energ = getEnergyLevel();
	}

	public void decompteTemps() {
		temps--;
	}

	int takeFood(Food p) {
		if (distanceTo(p) < 2) {
			if (Math.random() < .1 || getEnergyLevel() < energ / 2) {
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

	void dropallpers() {
		for (int i = bagSize() - 1; i >= 0; i--) {
			drop(i);
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
				double[] d = new double[2];
				d[0] = Double.parseDouble(messCourant.getArg1());
				d[1] = Double.parseDouble(messCourant.getArg2());
				if (!tabfoodtakebyme.contains(d))
					tabfoodtake.add(d);
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

		Percept[] objetsPercus = getPercepts(); // entit�s dans le p�rim�tre de
												// perception
		for (int i = 0; i < objetsPercus.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept objetCourant = objetsPercus[i];
			if (objetCourant.getTeam().equals(getTeam())
					&& objetCourant.getPerceptType().equals("Home")) // si objet courant = base alli�
			{
				myhome = objetCourant;
				break;
			}
			if (myhome == objetCourant)
				break;
		}

		if (myhome != null && distanceTo(myhome) < 2 && bagSize() > 0) {
			setUserMessage("drop all");
			dropAll();
			setUserMessage((new Integer(this.bagSize())).toString());
			return;
		} else if (bagSize() == getBagCapacity()) {
			if(!isMoving())
				randomHeading();
			else
				setHeading(towards(homeX, homeY));
			setUserMessage("going to base");
			move();
			return;
		}

		// 1. Si base ennemie trouv�e : broadcast
		for (int i = 0; i < objetsPercus.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept objetCourant = objetsPercus[i];
			if (!objetCourant.getTeam().equals(getTeam())
					&& objetCourant.getPerceptType().equals("Home")) // si objet
																		// courant
																		// =
																		// base
																		// ennemie
			{
				ennemiX = Double.toString(objetCourant.getX());
				ennemiY = Double.toString(objetCourant.getY());
				broadcast(groupName, "Launcher", chaineAtak, ennemiX, ennemiY);
			}
		}

		// 2. Si attaqu� : demande d'aide sans argument
		if (getShot()) {
			println("explorer attaqu�");
			broadcast(groupName, "Launcher", chaineAide, "0", "0");
		}

		// 3. si rocketlauncher ennemi rep�r�
		for (int i = 0; i < objetsPercus.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept objetCourant = objetsPercus[i];
			if (!objetCourant.getTeam().equals(getTeam())
					&& objetCourant.getPerceptType().equals("RocketLauncher")) // si
																				// RocketLauncher
																				// ennemi
			{
				// strat�gie d'�vitement
				positionEnnemiX = objetCourant.getX(); // abscisse de l'ennemi
				positionEnnemiY = objetCourant.getY(); // ordonn�e de l'ennemi
				ennemiX = Double.toString(objetCourant.getX());
				ennemiY = Double.toString(objetCourant.getY());
				// println("launcher ennemi rep�r�");
				// println(ennemiX);
				// println(ennemiY);
				broadcast(groupName, "Launcher", chaineAide, ennemiX, ennemiY);
				for (int j = 0; j < objetsPercus.length; j++) // pour toutes les
																// entit�s
																// per�ues...
				{
					Percept objetCourant2 = objetsPercus[j];
					if ((getHeading()
							- (towards(objetCourant2.getX(),
									objetCourant.getY())) >= 20)
							&& (objetCourant2.getPerceptType()
									.equals("Obstacle")))
					// si d�tecte un obstacle et que cet obstacle est dans le
					// champ de la direction de l'agent
					{
						if (temps == tempsMax) // si on vient de rep�rer
												// l'obstacle
						{
							decompteTemps();
							setHeading(towards(-positionEnnemiX / 2,
									-positionEnnemiY)); // on �vite ET l'ennemi,
														// Et l'obstacle
						} else {
							if (temps == 0) {
								temps = tempsMax;
							} else // temps entre 8 et 0
							{
								decompteTemps();
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
		for (int i = 0; i < objetsPercus.length; i++) // pour toutes les entit�s
														// per�ues...
		{
			Percept objetCourant = objetsPercus[i];
			if (objetCourant.getPerceptType().equals("Food")) // si nourriture
			{
				double[] d = new double[2];
				d[0] = objetCourant.getX();
				d[1] = objetCourant.getY();
				if (bagSize() != getBagCapacity() && !tabfoodtake.contains(d)) {
					int j = takeFood((Food) objetCourant);
					if (j == 0)
						setUserMessage((new Integer(this.bagSize())).toString());
					else if (j == 1)
						setUserMessage("eating");
					else
						setUserMessage("Going to food");
					broadcast(groupName, "Explorer", "TAKEFOOD",
							Double.toString(objetCourant.getX()),
							Double.toString(objetCourant.getY()));
					tabfoodtakebyme.add(d);
					return;
				}
			}
		}

		// 6. d�placement al�atoire (randomHeading au d�but)
		temps = tempsMax; // remise � 0 du compteur : on est hors de risque...
		move();
		return;
	}
}