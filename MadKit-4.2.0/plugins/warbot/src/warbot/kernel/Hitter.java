package warbot.kernel;

import java.awt.Point;

public class Hitter extends BasicBody{
	final static protected int maximumRocket = 30;
	protected int rocketNb=30;
	final protected static int SHOOT=10; //ACTION DESCRIPTION
	final protected static int BUILD_ROCKET=11; //ACTION DESCRIPTION
	final protected static int HIT=12; //ACTION DESCRIPTION
	private double rocketDirection=0;

public Hitter(WarbotEnvironment env,Brain b,String team)
{
	super(env,b,"hitter",team,15,8000,45);
	setSpeed(1);
	rocketNb = maximumRocket;
}

public Hitter()
{
    super();
	setDetectingRange(45);
	setSpeed(1);
	maximumEnergy=8000;
	rocketNb = maximumRocket;
}

public Percept makePercept(double dx, double dy, double d){
	   Percept p = super.makePercept(dx,dy, d);
	   p.setPerceptType("RocketLauncher");
	   return p;
}

////////////////////////////////////     PARTIAL BODY INTERFACE 2 function
public void launchRocket(double direction)
{
	rocketDirection=direction;
	action=SHOOT;
}

public void Hit(double direction)
{
	rocketDirection=direction;
	action=HIT;
}

public void buildRocket()
{
	action = BUILD_ROCKET;
}

public int getRocketNb(){ return rocketNb;}

public void reloadRocket(){ rocketNb = maximumRocket; }

int rocketWaitMax=3;
int rocketWait=rocketWaitMax;

//////////////////////////////////////  internal methods
void tryShoot()
{
	if((rocketNb>0) && (rocketWait <= 0))
	{
		rocketNb--;
		//Rocket r=new Rocket(myWorld,rocketDirection);

        getStructure().getAgent().doCommand(new SEdit.NewNodeCommand("Rocket",new Point(0,0)));
        Rocket r = (Rocket)((WarbotStructure)getStructure()).getLastEntity();
        r.setTeam(team);
        r.setHeading(rocketDirection);
        r.setXY( (radius+r.getRadius()+1)*r.getCosAlpha()+x,(radius+r.getRadius()+1)*r.getSinAlpha()+y);

        rocketWait=rocketWaitMax; // waiting time to be able to shoot again
		//System.err.println(toString());
		/*System.err.println();
		System.err.println(toString());
		System.err.println("xmoi="+ x +" ymoi= "+y);
		System.err.println("x="+ ((radius*2+r.getRadius()+1)*r.getCosAlpha()+x)+" y= "+( (radius*2+r.getRadius()+1)*r.getSinAlpha()+y));
		System.err.println();*/
		//myWorld.addEntity(r);
	}
}

void tryHit()
{
	if(rocketWait <= 0)
	{
		//Rocket r=new Rocket(myWorld,rocketDirection);

        getStructure().getAgent().doCommand(new SEdit.NewNodeCommand("Rocket",new Point(0,0)));
        Rocket r = (Rocket)((WarbotStructure)getStructure()).getLastEntity();
        r.setTeam(team);
        r.setHeading(rocketDirection);
        r.setXY( (radius+r.getRadius()+1)*r.getCosAlpha()+x,(radius+r.getRadius()+1)*r.getSinAlpha()+y);

        rocketWait=rocketWaitMax; // waiting time to be able to shoot again
		//System.err.println(toString());
		/*System.err.println();
		System.err.println(toString());
		System.err.println("xmoi="+ x +" ymoi= "+y);
		System.err.println("x="+ ((radius*2+r.getRadius()+1)*r.getCosAlpha()+x)+" y= "+( (radius*2+r.getRadius()+1)*r.getSinAlpha()+y));
		System.err.println();*/
		//myWorld.addEntity(r);
	}
}

void tryBuildRocket()
{
	rocketNb++;
	if(rocketNb > maximumRocket)
		rocketNb = maximumRocket;
}

/////////////////////////////////	ACTION MECHANISM

void doIt()
{
	super.doIt();
    rocketWait--;
    if (rocketWait < 0) rocketWait=0;
}

void doAction()
{
    //if (getBrain() != null) getBrain().println("trying: " + ACTIONS[action]);
	switch(action)
	{
		case SHOOT:tryShoot();break;
		case HIT:tryHit();break;
  		case BUILD_ROCKET:tryBuildRocket();break;
        default:
        //System.out.println("try to do super : "+action);
        super.doAction();
	}
}
}
