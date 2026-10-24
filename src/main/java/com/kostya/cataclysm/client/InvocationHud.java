package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Screen-anchored ritual aperture. Geometry stays at the margins; captions never shake. */
public final class InvocationHud {
    private static BufferBuilder mesh;private static Matrix4f matrix;
    private static void line(double x,double y,double xx,double yy,double width,int color){
        double dx=xx-x,dy=yy-y,len=Math.hypot(dx,dy);if(len<.001)return;
        double nx=-dy/len*width*.5,ny=dx/len*width*.5;
        vertex(x+nx,y+ny,color);vertex(x-nx,y-ny,color);vertex(xx-nx,yy-ny,color);
        vertex(x+nx,y+ny,color);vertex(xx-nx,yy-ny,color);vertex(xx+nx,yy+ny,color);
    }
    private static void vertex(double x,double y,int c){mesh.addVertex(matrix,(float)x,(float)y,0).setColor(c);}
    private static void arc(double x,double y,double radius,double a,double b,double width,int c){
        int n=Math.max(8,(int)((b-a)*radius/4));for(int i=0;i<n;i++){
            double u=a+(b-a)*i/n,v=a+(b-a)*(i+1)/n;
            line(x+Math.cos(u)*radius,y+Math.sin(u)*radius,x+Math.cos(v)*radius,y+Math.sin(v)*radius,width,c);
        }
    }
    private static void diamond(double x,double y,double r,int c){
        line(x,y-r,x+r,y,.7,c);line(x+r,y,x,y+r,.7,c);line(x,y+r,x-r,y,.7,c);line(x-r,y,x,y-r,.7,c);
    }
    public static void draw(GuiGraphics g,ClientEffects.Fx f,float age,int bar){
        var mc=Minecraft.getInstance();int w=g.guiWidth(),h=g.guiHeight();
        double fade=ClientEffects.smooth(Math.min(age/18.,(f.spell.duration-age)/32.));
        if(fade<.02)return;
        int color=((int)(fade*145)<<24)|f.spell.color,dim=((int)(fade*58)<<24)|f.spell.color;
        double r=Math.min(w*.19,(h-2*bar)*.30),time=age/20.;
        g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);RenderSystem.setShaderColor(1,1,1,1);
        matrix=g.pose().last().pose();mesh=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION_COLOR);
        // Four incomplete astrolabes. The central 60% is deliberately clear.
        for(int corner=0;corner<4;corner++){
            boolean right=corner%2==1,bottom=corner>=2;double x=right?w-8:8,y=bottom?h-bar-9:bar+9;
            double a=bottom?(right?Math.PI:Math.PI*1.5):(right?Math.PI*.5:0),b=a+Math.PI*.5;
            arc(x,y,r,a+.10,b-.10,.65,color);arc(x,y,r*.88,a+.15,b-.15,.45,dim);
            arc(x,y,r*.62,a+.2,b-.2,.55,dim);
            for(int k=0;k<=16;k++){
                double t=a+.12+(Math.PI*.5-.24)*k/16.,len=k%4==0?7:3;
                line(x+Math.cos(t)*(r-len),y+Math.sin(t)*(r-len),x+Math.cos(t)*(r+2),y+Math.sin(t)*(r+2),.6,k%4==0?color:dim);
            }
            double moving=a+.25+(Math.PI*.5-.5)*(.5+.5*Math.sin(time*.32+corner));
            diamond(x+Math.cos(moving)*r,y+Math.sin(moving)*r,2.3,color);
        }
        for(int side=0;side<2;side++){
            double x=side==0?9:w-9,y=h*.5;
            line(x,bar+r+18,x,y-17,.6,dim);line(x,y+17,x,h-bar-r-18,.6,dim);
            diamond(x,y,5,color);diamond(x,y,10,dim);
            line(x-3,y-24,x+3,y-24,.7,color);line(x-3,y+24,x+3,y+24,.7,color);
        }
        BufferUploader.drawWithShader(mesh.buildOrThrow());mesh=null;RenderSystem.enableCull();
        // A large, stable chapter caption inside the letterbox, with a protected dark backing.
        String phase=age<65?"awaken":age<f.spell.impact*.65?"manifest":age<f.spell.impact?"release":"aftermath";
        Component text=Component.translatable("hud.arcane_cataclysm.phase."+phase);
        float scale=Math.min(1.35f,(w-148f)/Math.max(1,mc.font.width(text)));
        int tw=(int)(mc.font.width(text)*scale),cx=w/2,y=Math.max(3,(bar-13)/2);
        double onset=switch(phase){case "awaken"->0;case "manifest"->65;case "release"->f.spell.impact*.65;default->f.spell.impact;};
        double pulse=1-ClientEffects.smooth((age-onset)/24.);
        g.fill(cx-tw/2-14,0,cx+tw/2+14,bar,0xff000000);
        g.fill(cx-tw/2-9,bar-2,cx+tw/2+9,bar-1,((int)(fade*(155+80*pulse))<<24)|f.spell.color);
        g.pose().pushPose();g.pose().translate(cx-tw/2f,y,0);g.pose().scale(scale,scale,1);
        g.drawString(mc.font,text,0,0,((int)(fade*255)<<24)|0xf3f5ff,true);g.pose().popPose();
        // Stable index in the black bar, away from the chapter and the skip hint.
        String index=String.format(java.util.Locale.ROOT,"%02d / XV",f.spell.tier());
        g.drawString(mc.font,index,w-14-mc.font.width(index),Math.max(5,bar/2-4),0xffd5dde8,false);
        double intro=ClientEffects.smooth((age-8)/14.)*(1-ClientEffects.smooth((age-83)/22.));
        if(intro>.015){int a=(int)(255*intro),x=24,yy=h-bar-(w<380?36:63);
            g.fill(x,yy-7,x+25,yy-5,(a<<24)|f.spell.color);
            g.drawString(mc.font,Component.translatable("hud.arcane_cataclysm.art",f.spell.tier()),x,yy,(a<<24)|0xb9cadb,true);
            float titleScale=Math.min(1.5f,(w-56f)/Math.max(1,mc.font.width(f.spell.title())));
            g.pose().pushPose();g.pose().translate(x,yy+14,0);g.pose().scale(titleScale,titleScale,1);
            g.drawString(mc.font,f.spell.title(),0,0,(a<<24)|0xf3f5ff,true);g.pose().popPose();
        }
    }
    private InvocationHud(){}
}
