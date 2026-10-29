package com.kostya.cataclysm;

/** Pure, bounded visual response. Never affects server damage, timing or terrain budgets. */
public final class ImpactDynamics {
    public static double power(int tier,double radius,double scale){
        if(!Double.isFinite(scale))scale=1;
        double p=(.60+.30*Math.min(tier,15)+.003*Math.max(0,radius))*Math.sqrt(Math.clamp(scale,.25,4));
        // 0.7.2: tier escalation for spells 1-15; 16 is pure cinema and gets a gentle factor instead.
        return Math.clamp(tier>=16?p*.45:p,.35,4.6);
    }
    public static int hits(int tier){
        if(tier>=16)return 0; // End of All stays pure cinema: one authored moment, no barrage.
        // 0.7.3: anime barrage - the aftermath is a train of strikes, not a single hit.
        return 2+(tier-1)/2;
    }
    public static double sinceHit(double after,int tier){
        if(after<0)return after;
        // 0.7.4: synthetic pacing is only the preview/visual-only fallback; real casts strike from server flush events.
        int n=hits(tier),k=(int)Math.min(after/12.,n);
        return after-k*12.;
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
        if(after<0||after>=24)return 0;
        // 0.7.7: a strike reads as a crisp few-frame fringe, not a multi-second wash.
        return Math.min(1.6,(.45+.55*power)*Math.exp(-after/(3.5+power*1.))+.12*Math.exp(-Math.pow((after-8)/3.,2)));
    }
    public static double glitch(double after,int tier){
        if(after<0||after>=14)return 0;
        // 0.7.7: the tear dies inside half a second - a few-frame accent per strike, not a sustained veil.
        double escalate=tier>=16?0:.40+.60*(tier-1)/14.;
        return escalate*Math.min(1,Math.exp(-after/2.2)+.16*Math.exp(-Math.pow((after-4)/1.8,2)))*Math.pow(1-after/14,2);
    }
    private ImpactDynamics(){}
}
