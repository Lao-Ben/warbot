package warbot.Test;

import warbot.kernel.*;


public class CMExplorer extends Brain {

	/**
	 * if the explorer is less than FOOD_DIST_TO_HOME from a home, it doesn't
	 * eat/take food
	 */
	final static int FOOD_DIST_TO_HOME = 20;

	static enum ExplorerRole {
		explore,
		collect,
		all
	};
	
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
	
	ExplorerRole role;

	Boolean takingFood = false;
	String foodIdToTake = null;
	
	boolean sendAlive = false;
	
	public CMExplorer() {
	}

	public void activate() {
		groupName = groupName + getTeam();
		randomHeading();
		println(this.getName() + " -- activate");
		createGroup(false, groupName, null, null);
		requestRole(groupName, roleName, null);
		requestRole(groupName, "mobile", null);
		this.setUserMessage((new Integer(this.bagSize())).toString());
		role = ExplorerRole.explore;
	}

	int takeFood(Food p) {
		if (distanceTo(p) < 2) {
			if (getEnergyLevel() < getInitialEnergyLevel() / 2) {
				println(this.getName() + " -- takeFood -- energy lvl : "
						+ getEnergyLevel() + "/" + getMaximumEnergy());
				eat((Food) p);
				return 1;
			} else {
				take((Food) p);
				return 0;
			}
		} else {
			if (!isMoving()) // if got stuck
			{
				randomHeading();
			} else // go to food
			{
				setHeading(towards(p.getX(), p.getY()));
			}
			move();
			return 2;
		}
	}
	
	/**
	 * Percept food entities
	 * 
	 * 1. Percept food
	 * 2. send message to homes if food percepted
	 * 
	 * TODO: improve it (all home's foodlists are filled with the same elements)
	 * 		should have a Master Home that contains the "blackboard"
	 */
	private void perceptFood() {
		Percept[] percepts = getPercepts();
		for (int i = 0; i < percepts.length; i++) {
			Percept currentPercept = percepts[i];
			// if food and not around a home of same team
			if (currentPercept.getPerceptType().equals("Food")) {	
				if (role == ExplorerRole.collect
					&& String.valueOf(System.identityHashCode(currentPercept)) == foodIdToTake) {
					if (takeFood((Food) currentPercept) < 2)
					{
						role = ExplorerRole.explore;
						foodIdToTake = null;
					}
				}
				
				//println(this.getName() + " -- doIt -- broadcast food to take : "	
				//		+ currentPercept.getX()
				//		+ ","
				//		+ currentPercept.getY());
				broadcast(groupName, "Home", Constants.MSG_FOODFOUND, new String[] {
						Double.toString(currentPercept.getX()),
						Double.toString(currentPercept.getY()),
						String.valueOf(System.identityHashCode(currentPercept))});
			}
		}
	}

	public void end()
	{
	    broadcast(groupName, "Home", Constants.MSG_EXPLORERDEAD);
	    println("ID Dead (me-explorer) : "+getAddress().getLocalID());
	}
	
	public void doIt() {
		String helpStr 				= "HELP-E";
		String attackStr 			= "ATAQ";
		double ennemyPosX 			= 0;
		double ennemyPosY 			= 0;
		String ennemyX 				= "";
		String ennemyY 				= "";
		boolean enn 				= false;
		WarbotMessage currentMsg 	= null;
		boolean baseAlive			= false;
		
		boolean eating = false;
		double foodFoundX = -1;
		double foodFoundY = -1;
		
		if (!isMoving()) // if got stuck
			randomHeading();

		while ((currentMsg = readMessage()) != null) {
			if (currentMsg.getAct() != null) {
				if (currentMsg.getAct() == Constants.MSG_BASEPOS) {
					homeX = currentMsg.getFromX();
					homeY = currentMsg.getFromY();
					if (!sendAlive)
					{
						broadcast(groupName, "Home", Constants.MSG_EXPLORERALIVE);
						sendAlive = true;
					}
					baseAlive = true;
				} else if (currentMsg.getAct() == Constants.MSG_TAKEFOOD) {
					if (!isMyBagFull()) { 
						broadcast(groupName, "Home", Constants.MSG_TAKINGFOOD, currentMsg.getArgN(3));
					}
				} else if (currentMsg.getAct() == Constants.MSG_TAKINGFOODACK) {
					foodFoundX = currentMsg.getFromX() + Double.valueOf(currentMsg.getArg1());
					foodFoundY = currentMsg.getFromY() + Double.valueOf(currentMsg.getArg2());
					foodIdToTake = currentMsg.getArgN(3);
					role = ExplorerRole.collect;
				}
			}
		}

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

		if (baseAlive && myhome != null && distanceTo(myhome) < 2 && bagSize() > 0) {
			setUserMessage("drop all");
			setHeading(towards(homeX, homeY));
			dropAll();
			setUserMessage((new Integer(this.bagSize())).toString());
			return;
		} else if (baseAlive && isBagFull()) {
			if (!isMoving())
				randomHeading();
			else
				setHeading(towards(homeX, homeY));
			setUserMessage("going to base");
			move();
			return;
		}

		// 1. Ennemy's base found => broadcast
		for (int i = 0; i < percepts.length; i++) // pour toutes les entités
													// perçues...
		{
			Percept currentPercept = percepts[i];
			if (!currentPercept.getTeam().equals(getTeam())
					&& currentPercept.getPerceptType().equals("Home")) // si
																		// objet
																		// courant
																		// =
																		// base
																		// ennemie
			{
				ennemyX = Double.toString(currentPercept.getX());
				ennemyY = Double.toString(currentPercept.getY());
				broadcast(groupName, "Launcher", attackStr, ennemyX, ennemyY);
				broadcast(groupName, "Hitter", attackStr, ennemyX, ennemyY);
			}
		}

		// 2. Attacked => ask for help
		if (getShot()) {
			println("explorer attaqué");
			broadcast(groupName, "Launcher", helpStr, "0", "0");
			broadcast(groupName, "Hitter", helpStr, "0", "0");
		}

		// 3. Ennemy rocketlauncher spotted => go away & broadcast
		for (int i = 0; i < percepts.length; i++) // pour toutes les entités
													// perçues...
		{
			Percept currentPercept = percepts[i];
			if (!currentPercept.getTeam().equals(getTeam())
					&& (currentPercept.getPerceptType().equals("RocketLauncher") || 
							currentPercept.getPerceptType().equals("Hitter"))) // si
																					// RocketLauncher
																					// ennemi
			{
				// stratégie d'évitement
				enn = true;
				ennemyPosX = currentPercept.getX(); // abscisse de l'ennemi
				ennemyPosY = currentPercept.getY(); // ordonnée de l'ennemi
				ennemyX = Double.toString(currentPercept.getX());
				ennemyY = Double.toString(currentPercept.getY());
				// println("launcher ennemi repéré");
				// println(ennemyX);
				// println(ennemyY);
				broadcast(groupName, "Launcher", helpStr, ennemyX, ennemyY);
				broadcast(groupName, "Hitter", helpStr, ennemyX, ennemyY);
				for (int j = 0; j < percepts.length; j++) // pour toutes les
															// entités
															// perçues...
				{
					Percept currentPercept2 = percepts[j];
					if ((getHeading()
							- (towards(currentPercept2.getX(),
									currentPercept.getY())) >= 20)
							&& (currentPercept2.getPerceptType()
									.equals("Obstacle")))
					// si détecte un obstacle et que cet obstacle est dans le
					// champ de la direction de l'agent
					{
						if (step == 0) // si on vient de repérer
										// l'obstacle
						{
							step++;
							setHeading(towards(-ennemyPosX / 2, -ennemyPosY)); // on
																				// évite
																				// ET
																				// l'ennemi,
																				// Et
																				// l'obstacle
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
				setHeading(towards(-ennemyPosX, -ennemyPosY)); // direction
																// opposée
																// à
																// l'ennemi
																// repéré
			}
		}

		perceptFood();
		
		// 4. Food percepted
		if (!enn && 
				(role == ExplorerRole.collect || role == ExplorerRole.all) )
		{
			for (int i = 0; i < percepts.length; i++) // pour toutes les entités
			// perçues...
			{
				Percept currentPercept = percepts[i];

				// if food and not around a home of same team
				if (currentPercept.getPerceptType().equals("Food")) {					
					if(eating)
					{	
						//println(this.getName() + " -- doIt -- bag size : " + bagSize() + "/" + getBagCapacity());
						if (bagSize() >= getBagCapacity() - 1)
						{
							println(this.getName() + " -- doIt -- broadcast food to take : "	
								+ currentPercept.getX()
								+ ","
								+ currentPercept.getY());
							// TODO: improve it (all home's foodlists are filled with the same elements)
							broadcast(groupName, "Home", Constants.MSG_FOODFOUND, new String[] {
									Double.toString(currentPercept.getX()),
									Double.toString(currentPercept.getY()),
									String.valueOf(System.identityHashCode(currentPercept))});
						}	
					}
					else if (myhome != null) {
					//	println(this.getName() + " -- doIt -- dist2home : "
					//			+ distanceTo(myhome));
						if (distanceTo(myhome) > FOOD_DIST_TO_HOME) {
							if (!isMyBagFull()) {
								int j = takeFood((Food) currentPercept);
								// if (j == 0)
								setUserMessage((new Integer(this.bagSize()))
										.toString());
								/*
								 * else if (j == 1) setUserMessage("eating");
								 * else setUserMessage("Going to food");
								 */								 
								eating = true;
							}
						}
					} else {
						if (!isMyBagFull()) {
							int j = takeFood((Food) currentPercept);
							// if (j == 0)
							setUserMessage((new Integer(this.bagSize()))
									.toString());
							/*
							 * else if (j == 1) setUserMessage("eating"); else
							 * setUserMessage("Going to food");
							 */
							/*
							 * broadcast(groupName, "Explorer", "TAKEFOOD",
							 * Double.toString(currentPercept.getX()),
							 * Double.toString(currentPercept.getY()));
							 */
							eating = true;
						}
					}
				}
			}
			//println(this.getName() + " -- doIt -- bag size 2 : " + bagSize() + "/" + getBagCapacity());
			if (eating)
				return;
		}
		

		if (!enn && bagSize() > 0 && getEnergyLevel() < getInitialEnergyLevel() / 2) {
			drop(bagSize() - 1);
			return;
		}

		
		// 6. Go to food found by an other explorer
		if (foodFoundX >= 0 && foodFoundY >= 0) {
			setHeading(towards(foodFoundX, foodFoundY));
			println(this.getName() + " -- doIt -- go to found food : " + foodFoundX + "," + foodFoundY);
		}
		
		// 7. Move
		step = maxStep;
		move();
		return;
	}
}