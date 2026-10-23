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
        return power*Math.exp(-after/(9+power*5))*Math.pow(1-after/100,2)*attenuation;
    }
    public static double glitch(double after){
        if(after<0||after>=26)return 0;
        // Initial fracture, then a smaller echo. Exact zero outside the impact window.
        return Math.min(1,Math.exp(-after/6)+.28*Math.exp(-Math.pow((after-7)/2.4,2)))*Math.pow(1-after/26,2);
    }
    private ImpactDynamics(){}
}
