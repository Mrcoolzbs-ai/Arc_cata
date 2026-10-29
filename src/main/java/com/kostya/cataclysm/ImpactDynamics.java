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
        if(after<0||after>=60)return 0;
        double attenuation=Math.min(1,160/Math.max(1,distance));
        // 0.8.1: one hard jolt that dies within a quarter second - the frame stays readable even at full power.
        double spike=Math.exp(-after/(1.5+power*.9));
        double echo=.10*Math.exp(-Math.pow((after-10)/3.,2));
        return power*.45*(spike+echo)*(1-after/60.)*attenuation;
    }
    public static double kick(double after,double power){
        if(after<0||after>=6)return 0;
        return power*Math.exp(-after*.85);
    }
    public static double aberration(double after,double power){
        if(after<0||after>=14)return 0;
        // 0.8.1: a barely-there fringe - the split must never read as fog over the scene.
        return Math.min(1.,(.20+.28*power)*Math.exp(-after/(2.5+power*.8))+.05*Math.exp(-Math.pow((after-6)/2.5,2)));
    }
    public static double glitch(double after,int tier){
        if(after<0||after>=8)return 0;
        // 0.8.1: visibility first - the tear is a two-frame whisper at the strike, never a veil over the frame.
        double escalate=tier>=16?0:.10+.14*(tier-1)/14.;
        return escalate*Math.exp(-after/1.5)*Math.pow(1-after/8,2);
    }
    private ImpactDynamics(){}
}
