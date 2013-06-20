package warbot.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import madkit.kernel.AgentAddress;

import warbot.kernel.Brain;
import warbot.kernel.Percept;
import warbot.kernel.WarbotMessage;

public class TestHitter extends Brain{
	String groupName = "warbot-";
	String roleName = "Hitter";

	final int tempsMax = 10;
	int temps = tempsMax; // variable permettant de garder la meme direction
							// pendant tempsMax itérations

	private Point2D lastCenter = null;
	
	Percept myhome = null;
	double homeX = 0;
	double homeY = 0;

	final int maxStep = 8;
	int step = 0;
	//boolean sendAlive = false;
	
	int treshold;
	
	static enum HitterRole {
		alone,
		squad_leader,
		squad_left,
		squad_right
	};

	private AgentAddress tmpSquadMember = null;
	private HashMap<String, AgentAddress> squadMembers;
	private Point2D leaderPosition;
	private Double leaderHeading = 0.;
	private int wantingToBeALeader = 0;
	
	private HitterRole role;

	public TestHitter() {
		squadMembers = new HashMap<String, AgentAddress>();
	}

	public void activate() {
		groupName = groupName + getTeam();
		randomHeading();
		println("Hitter Test opérationnel");
		createGroup(false, groupName, null, null);
		requestRole(groupName, roleName, null);
		requestRole(groupName, "mobile", null);
		role = HitterRole.alone;
		leaderPosition = new Point2D.Double(-42, -42);
		treshold = new Random().nextInt(100)-100;
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
		if (temps < tempsMax && temps > 0) // direction aléatoire a déjà été
											// prise
		{
			temps--;
			move();
			return;
		}
		if (temps == 0) // temps de désobstination écoulé
		{
			temps = tempsMax;
		}
	}
	
	private void avoidance(Percept[] percepts) {

		ArrayList<Percept> uncrossables = new ArrayList<Percept>();

		for (int i = 0; i < percepts.length; i++) {
			Percept currentPercept = percepts[i];
			if (currentPercept.getPerceptType().equals("Obstacle"))
			{
				uncrossables.add(currentPercept);
			}
		}
		
		double centerX = 0.;
		double centerY = 0.;
		
		for (int i = 0; i < uncrossables.size(); i++) {
				centerX += uncrossables.get(i).getX();
				centerY += uncrossables.get(i).getY();
		}
		
		if (uncrossables.size() > 0) {
			centerX /= uncrossables.size();
			centerY /= uncrossables.size();

			Point2D center = new Point2D.Double(centerX, centerY);
			
			
			if (lastCenter == null || (Math.abs(lastCenter.getX()) > Math.abs(centerX) || Math.abs(lastCenter.getY()) > Math.abs(centerY)))
			{
				double angle = getAngle(center);
				int n = 8;
				double delta = getHeading() - angle;
				
				println("uncrossables : " + uncrossables.size() + "center : " + center);
				println("heading : " + getHeading() + " angle : " + angle);
				println("delta : " + delta);
				
				delta = (delta < 0) ? 360 + delta : delta;

				println("delta : " + delta);
				
				if (delta < 45) {
					println("<45");
					setHeading(getHeading() + delta / (1 * n));
				} else if (delta > 315) {
					println(">315");
					setHeading(getHeading() - delta / (1 * n));
				} else if(delta < 90) {
					println("<90");
					setHeading(getHeading() + delta / (2 * n));
				} else if (delta > 270) {
					println(">270");
					setHeading(getHeading() - delta / (2 * n));
				} else if (delta < 180) {
					println("<180");
					setHeading(getHeading() + delta / (4 * n));
				} else if (delta > 180) {
					println(">180");
					setHeading(getHeading() - delta / (4 * n));
				} else {
					println("else");
					setHeading(getHeading() + delta / (8 * n));
				}
			}
			lastCenter = center;
		}
	}
	
	public void gestionHit(double directionTir, int tailleTabAmis,
			double[][] tabAmis) // to avoid hit on its friends
	{
		println("gestion du tir");
		println("direction du tir :" + Double.toString(directionTir));
		for (int i = 0; i < tailleTabAmis; i++) {
			println("entrée dans le tableau");
			println("direction de l'ami : "
					+ Double.toString(towards(tabAmis[0][i], tabAmis[1][i])));
			if ((directionTir - towards(tabAmis[0][i], tabAmis[1][i]) > 0 && directionTir
					- towards(tabAmis[0][i], tabAmis[1][i]) < 20)
					|| (towards(tabAmis[0][i], tabAmis[1][i]) - directionTir > 0 && towards(
							tabAmis[0][i], tabAmis[1][i]) - directionTir < 20)
							|| (directionTir - towards(tabAmis[0][i], tabAmis[1][i]) > 340)
							|| (towards(tabAmis[0][i], tabAmis[1][i]) - directionTir > 340)) {
				println("desobstination");
				desobstination();
				return;
			}
		}
		println("pas desobstination");
		Hit(directionTir);
	}
	
	/**
	 * set the position of the RocketLauncher depending on his role:
	 * - the squad_left will stay at the left of the leader
	 * - the squad_left will stay at the left of the leader
	 */
	private void followTheLeader() {
		if (leaderPosition.getX() == -42)
			return;
				
		int decal = 50;
		if (role == HitterRole.squad_right)
		{
			decal = -decal;
		}
    	double[] pt = {leaderPosition.getX() - 30, leaderPosition.getY() - decal};
		AffineTransform.getRotateInstance(Math.toRadians(leaderHeading),
										  leaderPosition.getX(),
										  leaderPosition.getY())
										  	.transform(pt, 0, pt, 0, 1);
		setHeading(towards(pt[0], pt[1]));
		setUserMessage((decal == 50) ? "sqad_left" : "squad_right");
	}
	
	public void doIt() {
		// variables pour gestion des messages
		double[][] tabAtaq = new double[4][100]; // tableau des message
													// d'attaque : première
													// colonne contient l'X,
													// deuxième colonne contient
													// l'Y de la base
		double[][] tabHelpBL = new double[4][100]; // tableau des message d'aide
													// venant des bases
													// concernant des launchers
													// : première colonne X de
													// l'ennemi si non vide,
													// deuxième colonne Y de
													// l'ennemi si non vide,
													// troisième colonne X
													// envoyeur, quatrième
													// colonne Y envoyeur
		double[][] tabHelpBE = new double[4][100]; // tableau des message d'aide
													// venant des bases
													// concernant des explorers
													// : première colonne X de
													// l'ennemi si non vide,
													// deuxième colonne Y de
													// l'ennemi si non vide,
													// troisième colonne X
													// envoyeur, quatrième
													// colonne Y envoyeur
		double[][] tabHelpE = new double[4][100]; // tableau des message d'aide
													// venant des explorers :
													// première colonne X de
													// l'ennemi si non vide,
													// deuxième colonne Y de
													// l'ennemi si non vide,
													// troisième colonne X
													// envoyeur, quatrième
													// colonne Y envoyeur
		double[][] tabHelpL = new double[4][100]; // tableau des message d'aide
													// venant des launchers :
													// première colonne X de
													// l'ennemi si non vide,
													// deuxième colonne Y de
													// l'ennemi si non vide,
													// troisième colonne X
													// envoyeur, quatrième
													// colonne Y envoyeur
		int comptAtaq = 0; // compteur du tableau tabAtaq
		int comptHelpBL = 0; // compteur du tableau tabHelpBL
		int comptHelpBE = 0; // compteur du tableau tabHelpBE
		int comptHelpE = 0; // compteur du tableau tabHelpE
		int comptHelpL = 0; // compteur du tableau tabHelpL
		int tailleAtaq = 0; // taille finale des différents tableaux
		int tailleHelpBL = 0; // taille finale des différents tableaux
		int tailleHelpBE = 0; // taille finale des différents tableaux
		int tailleHelpE = 0; // taille finale des différents tableaux
		int tailleHelpL = 0; // taille finale des différents tableaux
		// variable pour gestion des objets perçus
		double[][] tabMyTeam = new double[2][50]; // tableau contenant les
													// coordonnées relatives des
													// membres de notre équipe
		double[] tabLauncher = new double[4]; // tableau contenant les
												// coordonnées relatives,
												// l'énergie et la distance du
												// launcher ennemi ayant le
												// moins d'énergie tout en étant
												// le plus proche
		double[] tabExplorer = new double[4]; // tableau contenant les
												// coordonnées relatives,
												// l'énergie et la distance de
												// l'explorer ennemi ayant le
												// moins d'énergie tout en étant
												// le plus proche
		double[] tabBase = new double[4]; // tableau contenant les coordonnées
											// relatives, l'énergie et la
											// distance de la base ennemie ayant
											// le moins d'énergie tout en étant
											// la plus proche
		double[] tabAttackLeader = new double[2];
		int comptMyTeam = 0; // compteur du tableau tabMyTeam
		int tailleMyTeam = 0; // taille finale du tableau tabMyTeam
		// initialisation de l'énergie et de la distance dans les tableaux des
		// ennemis perçus
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
		int distanceAmis = 80; // distance à respecter pour éviter obstination
								// rejoindre amis (cas Y)
		int distanceMinAmis = 30; // distance au delà de laquelle il ne faut pas
									// s'approcher des amis (cas Y)
		// variables pour l'envoi de messages
		String actMessageAide = "HELP-L"; // cas où risque d'attaque ennemie
		String actMessageAttaque = "ATAQ"; // cas de détection de la base
											// ennemie
		String argMessageX = ""; // pour envoyer position relative en X de
									// l'ennemi
		String argMessageY = ""; // pour envoyer position relative en Y de
									// l'ennemi
		WarbotMessage currentMsg = null;
		// variables diverses
		double directionX = 0;
		double directionY = 0;
		int seuilEnergieBase = 6000; // énergie seuil pour "se défendre" vs
										// "se sacrifier"
		boolean baseAlive = false;
		
		// send messages to form a squad
				if (role == HitterRole.alone) {
					if (!squadMembers.isEmpty() && squadMembers.get("leader") == null)
						squadMembers.clear();
					
					if (squadMembers.isEmpty()) {
						int rand = new Random().nextInt(100);
						//int rand = new Random().nextInt() % 3;

						if (rand < treshold) {
							if (tmpSquadMember == null)
							{
								broadcast(groupName, roleName, Constants.MSG_WANTTOBEALEADER);
								wantingToBeALeader = 3;
								println(this.getName() + " -- broadcast msg : " + Constants.MSG_WANTTOBEALEADER);
							}
						} else if (rand % 2 == 0) {
							broadcast(groupName, roleName, Constants.MSG_WANTTOBEALEFT);
							wantingToBeALeader = 0;
							println(this.getName() + " -- broadcast msg : " + Constants.MSG_WANTTOBEALEFT);
						} else {
							broadcast(groupName, roleName, Constants.MSG_WANTTOBEARIGHT);				
							wantingToBeALeader = 0;
							println(this.getName() + " -- broadcast msg : " + Constants.MSG_WANTTOBEARIGHT);
						}		
					} else {
						int rand = new Random().nextInt() % 2;
						switch (rand) {
						case 0:
							send(squadMembers.get("leader"), Constants.MSG_WANTTOBEALEFT);
							wantingToBeALeader = 0;
							println(this.getName() + " -- broadcast msg2 : " + Constants.MSG_WANTTOBEALEFT + " to " + squadMembers.get("leader"));
							break;
						case 1:
							send(squadMembers.get("leader"), Constants.MSG_WANTTOBEARIGHT);
							wantingToBeALeader = 0;
							println(this.getName() + " -- broadcast msg2 : " + Constants.MSG_WANTTOBEARIGHT+ " to " + squadMembers.get("leader"));
							break;
						}
					}
					treshold++;
				}
		
		Percept EnnemyBase = null;
		Percept EnnemyExplorer = null;
		Percept EnnemyRocketLauncher = null;

		// récupération et classement des messages
				while ((currentMsg = readMessage()) != null && currentMsg.getSender() != getAddress()) {

					if ((role == HitterRole.alone
							|| role == HitterRole.squad_leader)
							&& currentMsg.getAct() != null)
					{
						// message d'attaque de base ennemie : les coordonnées sont toujours
						// présentes en argument du message
						if (currentMsg.getAct() == "ATAQ") {
							tabAtaq[0][comptAtaq] = Double.valueOf(currentMsg.getArg1())
									.doubleValue();
							tabAtaq[1][comptAtaq] = Double.valueOf(currentMsg.getArg2())
									.doubleValue();
							tabAtaq[2][comptAtaq] = currentMsg.getFromX();
							tabAtaq[3][comptAtaq] = currentMsg.getFromY();
							comptAtaq++;
						}
						// message d'aide d'une base concernant les launchers : les
						// coordonnées de l'ennemi sont toujours présentes en argument du
						// message
						if (currentMsg.getAct() == "HELP-BL") {
							tabHelpBL[0][comptHelpBL] = Double
									.valueOf(currentMsg.getArg1()).doubleValue();
							tabHelpBL[1][comptHelpBL] = Double
									.valueOf(currentMsg.getArg2()).doubleValue();
							tabHelpBL[2][comptHelpBL] = currentMsg.getFromX();
							tabHelpBL[3][comptHelpBL] = currentMsg.getFromY();
							comptHelpBL++;
						}
						// message d'aide d'une base concernant les explorers: les
						// coordonnées de l'ennemi sont toujours présentes en argument du
						// message
						if (currentMsg.getAct() == "HELP-BE") {
							tabHelpBE[0][comptHelpBE] = Double
									.valueOf(currentMsg.getArg1()).doubleValue();
							tabHelpBE[1][comptHelpBE] = Double
									.valueOf(currentMsg.getArg2()).doubleValue();
							tabHelpBE[2][comptHelpBE] = currentMsg.getFromX();
							tabHelpBE[3][comptHelpBE] = currentMsg.getFromY();
							comptHelpBE++;
						}
						// message d'aide d'un launcher : les coordonnées de l'ennemi sont
						// toujours présentes en argument du message
						if (currentMsg.getAct() == "HELP-L") {
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
						// message d'aide d'un explorer : pas de coordonnées de l'ennemi en
						// argument si message suite à tir
						if (currentMsg.getAct() == "HELP-E") {
							tabHelpE[0][comptHelpE] = Double.valueOf(currentMsg.getArg1())
									.doubleValue();
							tabHelpE[1][comptHelpE] = Double.valueOf(currentMsg.getArg2())
									.doubleValue();
							tabHelpE[2][comptHelpE] = currentMsg.getFromX();
							tabHelpE[3][comptHelpE] = currentMsg.getFromY();
							comptHelpE++;
						}
						
						if (currentMsg.getAct() == Constants.MSG_BASEPOS) {
							baseAlive = true;
							homeX = currentMsg.getFromX();
							homeY = currentMsg.getFromY();
							// setUserMessage(homeX + " ; " + homeY);
							/*if (!sendAlive)
							{
								broadcast(groupName, "Home", Constants.MSG_LAUNCHERALIVE);
								sendAlive = true;
							}*/
						}

						// squad messages handling
						if (role == HitterRole.alone) {
							if (currentMsg.getAct() == Constants.MSG_WANTTOBEALEADER) {
								if (wantingToBeALeader == 0 && tmpSquadMember == null && squadMembers.isEmpty()) {
									send(currentMsg.getSender(), Constants.MSG_ACCEPTLEADER);
									tmpSquadMember = currentMsg.getSender();
									println(this.getName() + " -- send msg : " + Constants.MSG_ACCEPTLEADER);
								}
							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTLEADER) {
								send(currentMsg.getSender(), Constants.MSG_ACCEPTLEADERACK);
								role = HitterRole.squad_leader;
								println(this.getName() + " -- send msg : " + Constants.MSG_ACCEPTLEADERACK);
								tmpSquadMember = null;
								// propose a left or a right role or both
							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTLEADERACK) {
								squadMembers.put("leader", tmpSquadMember);
								tmpSquadMember = null;
								println(this.getName() + " -- receive msg : " + Constants.MSG_ACCEPTLEADER);
								// confirm a proposed role	
							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTLEFT) {
								send(currentMsg.getSender(), Constants.MSG_ACCEPTLEFTACK);
								squadMembers.put("leader", currentMsg.getSender());
								role = HitterRole.squad_left;
								tmpSquadMember = null;
								println(this.getName() + " -- send msg : " + Constants.MSG_ACCEPTLEFTACK);
							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTRIGHT) {
								send(currentMsg.getSender(), Constants.MSG_ACCEPTRIGHTACK);
								squadMembers.put("leader", currentMsg.getSender());
								role = HitterRole.squad_right;
								tmpSquadMember = null;
							}
						} else if (role == HitterRole.squad_leader) {

							if (currentMsg.getAct() == Constants.MSG_ASKPOSITION) {
								send(currentMsg.getSender(), Constants.MSG_SENDPOSITION, String.valueOf(getHeading()));
								send(currentMsg.getSender(), Constants.MSG_LEADERBASEPOS, String.valueOf(homeX),String.valueOf(homeY));
								//println("send msg : " + Constants.MSG_SENDPOSITION);
							}

							if (currentMsg.getAct() == Constants.MSG_WANTTOBEALEFT) {
								println("tmpSM2 " + tmpSquadMember
										+ " sm get left " + squadMembers.get("left"));
								if (squadMembers.get("left") == null
										&& tmpSquadMember == null) {
									send(currentMsg.getSender(), Constants.MSG_ACCEPTLEFT);
									tmpSquadMember = currentMsg.getSender();
									println(this.getName() + " -- send msg : " + Constants.MSG_ACCEPTLEFT);
								}

							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTLEFTACK) {
								squadMembers.put("left", tmpSquadMember);
								tmpSquadMember = null;
								println(this.getName() + " -- receive msg : " + Constants.MSG_ACCEPTLEFTACK);
							} else if (currentMsg.getAct() == Constants.MSG_WANTTOBEARIGHT) {
								if (squadMembers.get("right") == null
										&& tmpSquadMember == null) {
									send(currentMsg.getSender(), Constants.MSG_ACCEPTRIGHT);
									tmpSquadMember = currentMsg.getSender();
								}

							} else if (currentMsg.getAct() == Constants.MSG_ACCEPTRIGHTACK) {
								squadMembers.put("right", tmpSquadMember);
								tmpSquadMember = null;
							}
						}
					} else 
					{
						if (currentMsg.getAct() != null && currentMsg.getAct() == Constants.MSG_SENDPOSITION) {
							leaderPosition.setLocation(currentMsg.getFromX(), currentMsg.getFromY());
							leaderHeading = Double.parseDouble(currentMsg.getArg1());
							//println(this.getName() + " -- receive msg : " + Constants.MSG_SENDPOSITION);
						}
						if (currentMsg.getAct() != null && currentMsg.getAct() == Constants.MSG_ATTACKLEADER)
						{
							tabAttackLeader[0] = Double.valueOf(currentMsg.getArg1());
							tabAttackLeader[1] = Double.valueOf(currentMsg.getArg2());
						}
						if (currentMsg.getAct() != null && currentMsg.getAct() == Constants.MSG_LEADERBASEPOS)
						{
							homeX = Double.valueOf(currentMsg.getArg1());
							homeY = Double.valueOf(currentMsg.getArg2());
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
		// fin récupération et classement des messages

		// send messages to get leader's position
				if (squadMembers.get("leader") != null) {
					send(squadMembers.get("leader"), Constants.MSG_ASKPOSITION);
					//println(this.getName() + " -- send msg : " + Constants.MSG_ASKPOSITION);
				} else if (wantingToBeALeader > 0)
					wantingToBeALeader--;
				else if (role == HitterRole.alone) {
					tmpSquadMember = null;
				}
		
		// récupération et classement des objets perçus
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

		for (int i = 0; i < percepts.length; i++) // pour toutes les entités
													// perçues...
		{
			Percept objetCourant = percepts[i];
			if (objetCourant.getTeam().equals(getTeam())) // objet de mon équipe
			{
				if (!objetCourant.getPerceptType().equals("Rocket")) {
					tabMyTeam[0][comptMyTeam] = objetCourant.getX();
					tabMyTeam[1][comptMyTeam] = objetCourant.getY();
					comptMyTeam++;
				}
			} else // on ne garde que les ennemis d'énergie minimum les plus
					// près de chaque type
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
						&& distanceTo(objetCourant) < 15
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
		// fin récupération et classement des objets perçus

		// on ordonne les actions principalement en fonction des objets perçus
		// et des messages reçus
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
		// A : une base ennemie est à portée et le launcher est en position de
		// tirer et/ou de se sacrifier
		if (tabBase[2] != 0 && tabBase[3] != 0) {
			// prévient les autres
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
					gestionHit(towards(tabBase[0], tabBase[1]), tailleMyTeam,
							tabMyTeam);
					if (role == HitterRole.squad_leader)
					{
						send(squadMembers.get("left"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
						send(squadMembers.get("right"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
					}
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

		// B : un launcher ennemi est à portée
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
				gestionHit(towards(tabLauncher[0], tabLauncher[1]), tailleMyTeam,
						tabMyTeam);
				if (role == HitterRole.squad_leader)
				{
					send(squadMembers.get("left"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
					send(squadMembers.get("right"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
				}
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

		// C : réception de demande d'aide de la base car launchers ennemis
		// repérés dans zone de sécurité
		if (tailleHelpBL > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			/* il faudrait sélectionner la base la plus rapprochée */
			// arbitrairement on va vers le dernier launcher ayant envoyé un
			// message
			directionX = tabHelpBL[0][tailleHelpBL - 1]
					+ tabHelpBL[2][tailleHelpBL - 1];
			directionY = tabHelpBL[1][tailleHelpBL - 1]
					+ tabHelpBL[3][tailleHelpBL - 1];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// D : réception de demande d'aide d'un launcher attaqué
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

		// E : réception de demande d'aide d'un explorer attaqué
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

		// F : réception d'un message d'attaque de la base ennemie
		if (tailleAtaq > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			directionX = tabAtaq[0][tailleAtaq - 1]
					+ tabAtaq[2][tailleAtaq - 1];
			directionY = tabAtaq[1][tailleAtaq - 1]
					+ tabAtaq[3][tailleAtaq - 1];
			/*
			 * a voir : on ne bouge vers la base uniquement si on n'est pas déjà
			 * dessus
			 */
			setHeading(towards(directionX, directionY));
			move();
			return;
		}

		// W : on a pour objet perçu un explorer
		if (tabExplorer[2] != 0 && tabExplorer[3] != 0) {
			argMessageX = Double.toString(tabExplorer[0]);
			argMessageY = Double.toString(tabExplorer[1]);
			setUserMessage(""+distanceTo(EnnemyExplorer));
			if (distanceTo(EnnemyExplorer) < 10)
			{
				setUserMessage("ATTACK");
				// tire
				gestionHit(towards(tabExplorer[0], tabExplorer[1]), tailleMyTeam,
						tabMyTeam);
				if (role == HitterRole.squad_leader)
				{
					send(squadMembers.get("left"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
					send(squadMembers.get("right"),Constants.MSG_ATTACKLEADER,argMessageX, argMessageY);
				}
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

		// X : réception de demande d'aide de la base car explorers ennemis
		// repérés dans zone de sécurité
		if (tailleHelpBE > 0) {
			desobstination();
			if (temps != tempsMax)
				return;
			/* il faudrait sélectionner la base la plus rapprochée */
			// arbitrairement on va vers le dernier launcher ayant envoyé un
			// message
			directionX = tabHelpBE[0][tailleHelpBE - 1]
					+ tabHelpBE[2][tailleHelpBE - 1];
			directionY = tabHelpBE[1][tailleHelpBE - 1]
					+ tabHelpBE[3][tailleHelpBE - 1];
			setHeading(towards(directionX, directionY));
			move();
			return;
		}
		
		/*if (tabAttackLeader[0] != 0 && tabAttackLeader[1] != 0 && (role == HitterRole.squad_left || role == HitterRole.squad_right))
		{
			// demande aide
			argMessageX = Double.toString(tabAttackLeader[0]);
			argMessageY = Double.toString(tabAttackLeader[1]);
			// tire
			gestionHit(towards(tabAttackLeader[0], tabAttackLeader[1]), tailleMyTeam,
					tabMyTeam);
		}*/

		// Y : rejoindre ami perçu
		for (int i = 0; i < percepts.length; i++) // pour toutes les entités
													// perçues...
		{
			Percept objetCourant = percepts[i];
			if (objetCourant.getTeam().equals(getTeam())
					&& objetCourant.getPerceptType().equals("RocketLauncher")
					&& distanceTo(objetCourant) > distanceAmis
					&& distanceTo(objetCourant) < distanceMinAmis) {
				if (!isMoving()) // cas où il a blocage : changement de
									// direction
				{
					randomHeading();
				} else // on va vers l'autre
				{
					setHeading(towards(objetCourant.getX(), objetCourant.getY())); // récupère
																					// la
																					// direction
																					// de
																					// l'ami
				}
				move();
				return;
			}
		}
		
		avoidance(percepts);
		if (role == HitterRole.alone || role == HitterRole.squad_leader)
			avoidance(percepts);
		
		// Z : déplacement aléatoire
		step = maxStep;
		// Z : déplacement aléatoire
		if (!isMoving())
			randomHeading();
		// Follow the squad leader
		else if (role == HitterRole.squad_left
				|| role == HitterRole.squad_right) {
			followTheLeader();
		}
		move();
		return;
	}
	
	/* Fetches angle relative to screen centre point
	 * where 3 O'Clock is 0 and 12 O'Clock is 270 degrees
	 * 
	 * @param screenPoint
	 * @return angle in degress from 0-360.
	 */
	public double getAngle(Point2D screenPoint)
	{
	    double dx = screenPoint.getX();
	    // Minus to correct for coord re-mapping
	    double dy = -(screenPoint.getY());

	    double inRads = Math.atan2(dy,dx);

	    // We need to map to coord system when 0 degree is at 3 O'clock, 270 at 12 O'clock
	    if (inRads < 0)
	        inRads = Math.abs(inRads);
	    else
	        inRads = 2*Math.PI - inRads;

	    return Math.toDegrees(inRads);
	}
}
