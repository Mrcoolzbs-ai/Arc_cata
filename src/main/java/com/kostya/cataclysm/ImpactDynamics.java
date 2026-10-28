package com.kostya.cataclysm;

/** Pure, bounded visual response. Never affects server damage, timing or terrain budgets. */
public final class ImpactDynamics {
    public static double power(int tier,double radius,double scale){
        if(!Double.isFinite(scale))scale=1;
        return Math.clamp((.60+.20*Math.clamp(tier,1,16)+.003*Math.max(0,radius))*Math.sqrt(Math.clamp(scale,.25,4)),.35,3.8);
    }
    public static double shake(double after,double power,double distance){
        if(after<0||after>=100)return 0;
        double attenuation=Math.min(1,160/Math.max(1,distance));
        // 0.7.0 punch pass: hard spike that dies within half a second, one late echo, no slow sway tail.
        double spike=Math.exp(-after/(3.5+power*2.));
        double echo=.30*Math.exp(-Math.pow((after-14)/4.5,2));
        return power*(spike+echo)*(1-after/100.)*attenuation;
    }
    public static double kick(double after,double power){
        if(after<0||after>=6)return 0;
        return power*Math.exp(-after*.85);
    }
    public static double aberration(double after,double power){
        if(after<0||after>=80)return 0;
        return Math.min(1.6,(.45+.55*power)*Math.exp(-after/(4.5+power*1.5))+.22*Math.exp(-Math.pow((after-16)/6.,2)));
    }
    public static double glitch(double after){
        if(after<0||after>=26)return 0;
        // Initial fracture, then a smaller echo. Exact zero outside the impact window.
        return Math.min(1,Math.exp(-after/4.2)+.34*Math.exp(-Math.pow((after-7)/2.2,2)))*Math.pow(1-after/26,2);
    }
    private ImpactDynamics(){}
}
