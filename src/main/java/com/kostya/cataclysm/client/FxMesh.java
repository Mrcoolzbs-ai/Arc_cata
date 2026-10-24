package com.kostya.cataclysm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/** Small immediate mesh builder. All positions are camera-relative; no texture/atlas dependency. */
public final class FxMesh {
    public static ShaderInstance glow,rune,energy,voidEnergy,surface;
    private static BufferBuilder b;
    private static boolean normals;
    private static Vec3 camera,left,up;
    private static int color;private static float alpha;
    public static void frame(Camera c){camera=c.getPosition();Vector3f l=c.getLeftVector(),u=c.getUpVector();left=new Vec3(l.x,l.y,l.z);up=new Vec3(u.x,u.y,u.z);}
    public static void begin(ShaderInstance shader,float time,int col,int secondary,float a,float x,float y,float z,float w,boolean opaque){
        normals=shader==energy||shader==voidEnergy||shader==surface;color=col;alpha=a;
        RenderSystem.setShader(()->shader);
        shader.safeGetUniform("FxTime").set(time);
        shader.safeGetUniform("FxParams").set(x,y,z,w);
        shader.safeGetUniform("FxColor2").set(((secondary>>16)&255)/255f,((secondary>>8)&255)/255f,(secondary&255)/255f,1f);
        shader.safeGetUniform("ColorModulator").set(1f,1f,1f,1f);
        RenderSystem.enableBlend();
        // Solid cores must occlude their back faces and the far half of orbit disks.
        RenderSystem.depthMask(shader==voidEnergy||(shader==surface&&(x==0||x==6||x==7||x==8)));
        // Core ShaderInstance in 1.21 does not apply JSON blend settings.
        // Explicit state: opaque plasma/void surfaces use alpha, glow meshes use additive.
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,opaque?GL11.GL_ONE_MINUS_SRC_ALPHA:GL11.GL_ONE);
        b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,normals?DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL:DefaultVertexFormat.POSITION_TEX_COLOR);
    }
    public static void tint(int c,float a){color=c;alpha=a;}
    public static void end(){MeshData mesh=b.build();if(mesh!=null)BufferUploader.drawWithShader(mesh);RenderSystem.depthMask(false);b=null;}
    private static void vertex(Vec3 p,float u,float v,Vec3 normal){
        var out=b.addVertex((float)(p.x-camera.x),(float)(p.y-camera.y),(float)(p.z-camera.z)).setUv(u,v).setColor((color>>16)&255,(color>>8)&255,color&255,(int)(Math.max(0,Math.min(1,alpha))*255));
        if(normals)out.setNormal((float)normal.x,(float)normal.y,(float)normal.z);
    }
    public static void quad(Vec3 a,Vec3 c,Vec3 d,Vec3 e,Vec3 n){vertex(a,0,0,n);vertex(c,1,0,n);vertex(d,1,1,n);vertex(e,0,1,n);}
    public static void billboard(Vec3 p,double size){
        Vec3 a=left.scale(size),v=up.scale(size);quad(p.add(a).add(v),p.subtract(a).add(v),p.subtract(a).subtract(v),p.add(a).subtract(v),new Vec3(0,1,0));
    }
    public static void disk(Vec3 p,double radius,double angle){
        Vec3 a=new Vec3(Math.cos(angle)*radius,0,Math.sin(angle)*radius),v=new Vec3(-Math.sin(angle)*radius,0,Math.cos(angle)*radius);
        quad(p.subtract(a).subtract(v),p.add(a).subtract(v),p.add(a).add(v),p.subtract(a).add(v),new Vec3(0,1,0));
    }
    public static void ribbon(Vec3 from,Vec3 to,double width){
        Vec3 axis=to.subtract(from),view=camera.subtract(from),side=axis.cross(view).normalize().scale(width);
        if(side.lengthSqr()<.001)side=left.scale(width);
        quad(from.add(side),from.subtract(side),to.subtract(side),to.add(side),view.normalize());
    }
    public static void sphere(Vec3 p,double radius,int seg,int rings){
        for(int j=0;j<rings;j++)for(int i=0;i<seg;i++){
            double a=i*Math.PI*2/seg,a1=(i+1)*Math.PI*2/seg,t=j*Math.PI/rings,t1=(j+1)*Math.PI/rings;
            Vec3 n0=unit(a,t),n1=unit(a1,t),n2=unit(a1,t1),n3=unit(a,t1);
            // Continuous globe UVs: per-face 0..1 coordinates made hundreds of repeated stripes.
            vertex(p.add(n0.scale(radius)),i/(float)seg,j/(float)rings,n0);
            vertex(p.add(n1.scale(radius)),(i+1)/(float)seg,j/(float)rings,n1);
            vertex(p.add(n2.scale(radius)),(i+1)/(float)seg,(j+1)/(float)rings,n2);
            vertex(p.add(n3.scale(radius)),i/(float)seg,(j+1)/(float)rings,n3);
        }
    }
    private static Vec3 unit(double a,double t){return new Vec3(Math.cos(a)*Math.sin(t),Math.cos(t),Math.sin(a)*Math.sin(t));}
    public static void beam(Vec3 from,Vec3 to,double radius,int seg){
        Vec3 axis=to.subtract(from).normalize(),u=axis.cross(new Vec3(0,1,0)).normalize();if(u.lengthSqr()<.01)u=new Vec3(1,0,0);
        Vec3 v=axis.cross(u).normalize();
        for(int i=0;i<seg;i++){
            double a=i*Math.PI*2/seg,c=(i+1)*Math.PI*2/seg;Vec3 n=u.scale(Math.cos(a)).add(v.scale(Math.sin(a))),n1=u.scale(Math.cos(c)).add(v.scale(Math.sin(c)));
            quad(from.add(n.scale(radius)),from.add(n1.scale(radius)),to.add(n1.scale(radius)),to.add(n.scale(radius)),n.add(n1).normalize());
        }
    }
    public static void blade(Vec3 tip,double length,double yaw){
        Vec3 u=new Vec3(Math.cos(yaw),0,Math.sin(yaw)),v=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        Vec3 base=tip.add(0,length*.78,0),end=tip.add(0,length,0);double w=length*.055;
        quad(tip,base.add(u.scale(w)),end,base.subtract(u.scale(w)),v);
        quad(tip,base.add(v.scale(w*.3)),end,base.subtract(v.scale(w*.3)),u);
        beam(base.add(u.scale(-w*3)),base.add(u.scale(w*3)),w*.22,4);
        beam(base,end.add(0,length*.12,0),w*.35,6);
    }
    public static void box(Vec3 p,double x,double y,double z){
        Vec3 a=p.add(-x,0,-z),b=p.add(x,0,-z),c=p.add(x,0,z),d=p.add(-x,0,z);
        quad(a,b,b.add(0,y,0),a.add(0,y,0),new Vec3(0,0,-1));
        quad(b,c,c.add(0,y,0),b.add(0,y,0),new Vec3(1,0,0));
        quad(c,d,d.add(0,y,0),c.add(0,y,0),new Vec3(0,0,1));
        quad(d,a,a.add(0,y,0),d.add(0,y,0),new Vec3(-1,0,0));
        quad(a.add(0,y,0),b.add(0,y,0),c.add(0,y,0),d.add(0,y,0),new Vec3(0,1,0));
    }
    public static void verticalSeal(Vec3 p,double radius,double yaw){
        Vec3 u=new Vec3(Math.cos(yaw)*radius,0,Math.sin(yaw)*radius),v=new Vec3(0,radius,0);
        quad(p.subtract(u).subtract(v),p.add(u).subtract(v),p.add(u).add(v),p.subtract(u).add(v),new Vec3(-Math.sin(yaw),0,Math.cos(yaw)));
    }
    public static void crystal(Vec3 base,double height,double width,double yaw){
        Vec3 tip=base.add(Math.sin(yaw)*height*.12,height,Math.cos(yaw)*height*.12);
        for(int i=0;i<6;i++){
            double a=yaw+i*Math.PI/3,b=a+Math.PI/3;
            Vec3 l=base.add(Math.cos(a)*width,height*.18,Math.sin(a)*width),r=base.add(Math.cos(b)*width,height*.18,Math.sin(b)*width);
            Vec3 n=new Vec3(Math.cos((a+b)*.5),.2,Math.sin((a+b)*.5)).normalize();
            quad(base,l,tip,r,n);
        }
    }
    public static void orbitRing(Vec3 center,double radius,double tilt,double angle,int segments,double width){
        Vec3 last=null;
        for(int i=0;i<=segments;i++){
            double a=angle+i*Math.PI*2/segments;Vec3 p=center.add(Math.cos(a)*radius,Math.sin(a)*radius*Math.sin(tilt),Math.sin(a)*radius*Math.cos(tilt));
            if(last!=null)ribbon(last,p,width);last=p;
        }
    }
    public static void wall(Vec3 center,double r,double height,int segments){
        for(int i=0;i<segments;i++){
            double a=i*Math.PI*2/segments,c=(i+1)*Math.PI*2/segments;
            Vec3 p=center.add(Math.cos(a)*r,0,Math.sin(a)*r),q=center.add(Math.cos(c)*r,0,Math.sin(c)*r);
            quad(p,q,q.add(0,height,0),p.add(0,height,0),new Vec3(Math.cos(a),0,Math.sin(a)));
        }
    }
    private FxMesh(){}
}
