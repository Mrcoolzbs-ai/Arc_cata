import com.kostya.cataclysm.ImpactDynamics;
public class ImpactDynamicsTest {
 static int checks;
 static void require(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 public static void main(String[] a){
  double[] radii={48,80,112,144,168};double last=0;
  for(int i=0;i<5;i++){
   double p=ImpactDynamics.power(i+1,radii[i],1);
   require(p>last,"first-five power must rise");last=p;
   System.out.printf("tier=%d power=%.3f initialShake=%.3f%n",i+1,p,ImpactDynamics.shake(0,p,52));
   for(double scale:new double[]{.25,.5,1,2,4,100,Double.NaN}){
    double power=ImpactDynamics.power(i+1,radii[i],scale);
    require(Double.isFinite(power)&&power>=.35&&power<=3.8,"bounded power");
    double previous=Double.MAX_VALUE;
    for(double t=-5;t<=105;t+=.25){
     double shake=ImpactDynamics.shake(t,power,52),glitch=ImpactDynamics.glitch(t);
     require(Double.isFinite(shake)&&shake>=0&&shake<=3.8,"bounded shake");
     require(glitch>=0&&glitch<=1,"bounded glitch");
     if(t<0||t>=100)require(shake==0,"shake outside window");
     if(t<0||t>=26)require(glitch==0,"glitch outside window");
     if(t>=0){require(shake<=previous,"shake must decay");previous=shake;}
    }
   }
   require(ImpactDynamics.shake(0,p,320)<ImpactDynamics.shake(0,p,20),"distance attenuation");
  }
  require(ImpactDynamics.glitch(26)==0&&ImpactDynamics.glitch(0)==1,"impact endpoints");
  System.out.println("IMPACT_DYNAMICS_PASS checks="+checks);
 }
}
