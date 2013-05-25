package warbot.Test;

import warbot.kernel.*;

public class CMHome extends Brain
{
	String groupName="warbot-";
	String roleName="Home";
	
	public CMHome(){}
	
	void eatFood(Food p){
		if(distanceTo(p) < 3){
			eat((Food)p);
		}
	}

	public void activate()
	{
		groupName=groupName+getTeam();  // -> warbot-CM
		println("Base Test opérationnelle");		
		createGroup(false,groupName,null,null);
		requestRole(groupName,roleName,null);
	}

	public void doIt()
	{
		int distanceDetectionHome 		= 200;		// périmètre de prise en compte pour base ennemie
		int distanceDetectionExplorer 	= 150;		// périmètre de prise en compte pour explorer ennemi
		int distanceDetectionLauncher 	= 80*2;		// périmètre de prise en compte pour launcher ennemi
		int distanceSecurite 			= 150;		// périmètre de sécurité
		int nbEnnemisProches 			= 0;
		int nbAmisProches 				= 0;
		int nbExplorer					= 0;
		int nbLauncher					= 0;
		
		String actMessageAideL 			= "HELP-BL";// cas où risque d'attaque ennemie des launchers
		String actMessageAideE 			= "HELP-BE";// cas où risque d'attaque ennemie car explorers rôdent
		String actMessageAttaque 		= "ATAQ";	// cas de détection de la base ennemie
		String argMessageX 				= "";		// pour envoyer position relative en X de l'ennemi
		String argMessageY 				= "";		// pour envoyer position relative en Y de l'ennemi
		String argLauncherX				= "";		// pour envoyer position relative en X de l'ennemi
		String argLauncherY				= "";		// pour envoyer position relative en Y de l'ennemi
		
		WarbotMessage messCourant	= null;
		
		while((messCourant = readMessage())!= null)
		{
			// message d'attaque de base ennemie : les coordonnées sont toujours présentes en argument du message
			if(messCourant.getAct() != null && messCourant.getAct() == "ExplorerAlive")
			{
				nbExplorer++;
			}
			if(messCourant.getAct() != null && messCourant.getAct() == "LauncherAlive")
			{
				nbLauncher++;
			}
		}
		setUserMessage(Integer.toString(nbExplorer));
		
		broadcast(groupName,"Explorer","basepos");
		broadcast(groupName,"Launcher","basepos");
		Percept[]objetsPercus = getPercepts();	// entités dans le périmètre de perception
		
		for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
		{
			Percept objetCourant = objetsPercus[i];
			// on observe les "ennemis"
			if (!objetCourant.getTeam().equals(getTeam()))
			{
				// si la base percoit la base ennemie
				if(objetCourant.getPerceptType().equals("Home") && distanceTo(objetCourant) < distanceDetectionHome)
				{
					argMessageX = Double.toString(objetCourant.getX());	
					argMessageY = Double.toString(objetCourant.getY());
					broadcast(groupName,"Launcher",actMessageAttaque,argMessageX,argMessageY);
//					println("Base ennemie détectée");
				}
				// si la base percoit des explorateurs ennemis
				if(objetCourant.getPerceptType().equals("Explorer") && distanceTo(objetCourant) < distanceDetectionExplorer)
				{
					argMessageX = Double.toString(objetCourant.getX());	
					argMessageY = Double.toString(objetCourant.getY());
					broadcast(groupName,"Launcher",actMessageAideE,argMessageX,argMessageY);
//					println("Explorer ennemi détecté");
				}
				// si la base percoit des launchers ennemis
				if(objetCourant.getPerceptType().equals("RocketLauncher") && distanceTo(objetCourant) < distanceDetectionLauncher)
				{
//					println("Launcher ennemi détecté");
					nbEnnemisProches ++;
					argLauncherX = Double.toString(objetCourant.getX());
					argLauncherY = Double.toString(objetCourant.getY());
				}
			}
			if (objetCourant.getPerceptType().equals("Food")) // si nourriture
			{
				setUserMessage("eating");
				eatFood((Food) objetCourant);
			}
			// on regarde le nombre de nos launchers "proches"
			if (objetCourant.getTeam().equals(getTeam()) && objetCourant.getPerceptType().equals("RocketLauncher") && distanceTo(objetCourant) < distanceSecurite)	
				nbAmisProches ++;
		}
		// appel à l'aide que si nombre launchers ennemis >= nombre launchers amis
		if (nbAmisProches <= nbEnnemisProches && nbEnnemisProches != 0)
		{
//			println("HELP ! nb ennemis : "+nbEnnemisProches);
			broadcast(groupName,"Launcher",actMessageAideL,argLauncherX,argLauncherY);

			if (getResourceLevel() >= 800)
				createAgent("TestRocketLauncher");
			return;
		}
		if (getResourceLevel() >= 800)
		{
			if (nbLauncher > nbExplorer && nbExplorer < 10)
				createAgent("TestExplorer");
			else
				createAgent("TestRocketLauncher");
		}
			
	}
}
