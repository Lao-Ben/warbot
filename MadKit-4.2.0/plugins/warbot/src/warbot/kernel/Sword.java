package warbot.kernel;

import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.ImageIcon;

public class Sword extends MovableEntity
{
	protected static ImageIcon rocketGif=null;

    protected int power = 200;

	public Sword(WarbotEnvironment env,double direction)
	{
		super(env,"sword","",1,150,0);
		setSpeed(1);
		setHeading(direction);
		action = MOVE;
        //System.out.println("Rocket created..");
	}

	public Sword()
	{
		setSpeed(1);
        setHeading(90);
		action = MOVE;
        setEnergy(150);
	}

	public Percept makePercept(double dx, double dy, double d){
	   Percept p = super.makePercept(dx,dy,d);
	   p.setPerceptType("Sword");
	   return p;
	}

    ImageIcon getImage()
    {
        return rocketGif;
    }

    void  createDefaultImage()
    {
        if(rocketGif==null)
            rocketGif = new ImageIcon(Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("warbot/kernel/images/"+team+name+".gif")).getScaledInstance(radius*2,radius*2,Image.SCALE_SMOOTH));
    }

    public void doIt(){
        super.doIt();
        if(energy > 0)
            energy--;
    }

    void doAction()
    {
        if(energy>0)
        {
            Entity e = myWorld.authorizeMove(this,newX(),newY());
            if (e!=null)
            {
                e.getMissileShot(power);
                doPhysicalMove();
                delete();
                return;
            }
            else
            {
                moving=true;
                doPhysicalMove();
            }
        }
        else
            delete();
    }


    void update() {
    }

    public void setPower(int pow)
    {
    	power = pow;
    }

    void getMissileShot(int value)
    {
        delete();
    }


}
