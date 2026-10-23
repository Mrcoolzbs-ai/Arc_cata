"""Original procedural assets. No reference-mod code, textures or audio are used."""
from pathlib import Path
import json, math, subprocess, wave, tempfile
import numpy as np
from PIL import Image, ImageDraw
root=Path(__file__).resolve().parents[1]/'src/main/resources'
a=root/'assets/arcane_cataclysm'; d=root/'data/arcane_cataclysm'
def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n')
spells=['starfall','heavens_lance','singularity','snow_grave','tempest_wrath','tectonic_rift','solar_nova','thousand_blades','inferno_tide','last_cataclysm']
ru=['Падение звезды','Копьё небес','Сингулярность','Снежная могила','Гнев бури','Разлом титанов','Солнечная нова','Тысяча клинков','Адский прилив','Последний катаклизм']
en=['Starfall','Heaven’s Lance','Singularity','Snow Grave','Tempest’s Wrath','Tectonic Rift','Solar Nova','Thousand Blades','Inferno Tide','The Last Cataclysm']
for lang,names in [('ru_ru',ru),('en_us',en)]:
    russian=lang=='ru_ru'
    current=a/f'lang/{lang}.json'
    obj=json.loads(current.read_text()) if current.exists() else {}
    obj.update({f'spell.arcane_cataclysm.{s}':n for s,n in zip(spells,names)})
    def text(key,r,e):obj[key]=r if russian else e
    text('item.arcane_cataclysm.grimoire_of_cataclysm','Гримуар Катаклизма','Grimoire of Cataclysm')
    text('itemGroup.arcane_cataclysm','Магия Катаклизма','Arcane Cataclysm')
    text('tooltip.arcane_cataclysm.controls','ПКМ — призыв · Shift + ПКМ / колесо — выбор','RMB: cast · Shift + RMB / wheel: select')
    text('tooltip.arcane_cataclysm.warning','Необратимо разрушает мир. Сделайте резервную копию!','Permanent terrain destruction. Back up your world!')
    text('hud.arcane_cataclysm.controls','ПКМ — призыв  ·  Shift + колесо — выбор','RMB: cast  ·  Shift + wheel: select')
    text('hud.arcane_cataclysm.invocation','ЗАПРЕТНОЕ ИСКУССТВО','FORBIDDEN ART')
    text('hud.arcane_cataclysm.skip','%s — пропустить камеру · %s — камера вкл/выкл','%s: skip camera · %s: camera on/off')
    text('message.arcane_cataclysm.permission','Нужны права оператора или творческий режим.','Operator permissions or Creative mode required.')
    text('message.arcane_cataclysm.busy','Мир уже сотрясает катаклизм. Подождите.','The world is already enduring a cataclysm. Wait.')
    text('message.arcane_cataclysm.camera_on','Кинематографическая камера включена','Cinematic camera enabled')
    text('message.arcane_cataclysm.camera_off','Кинематографическая камера выключена','Cinematic camera disabled')
    text('key.arcane_cataclysm.camera','Кинематографическая камера вкл/выкл','Toggle cinematic camera')
    text('key.arcane_cataclysm.skip','Пропустить текущую камеру','Skip current cinematic camera')
    text('key.categories.arcane_cataclysm','Магия Катаклизма','Arcane Cataclysm')
    text('subtitles.arcane_cataclysm.charge','Магия нарастает','Arcane power gathers')
    text('subtitles.arcane_cataclysm.impact','Катаклизм обрушивается','Cataclysm strikes')
    text('subtitles.arcane_cataclysm.rumble','Земля содрогается','Earth trembles')
    text('death.attack.arcane_cataclysm','%1$s исчезает в катаклизме','%1$s was consumed by a cataclysm')
    text('death.attack.arcane_cataclysm.player','%1$s уничтожен катаклизмом %2$s','%1$s was consumed by %2$s’s cataclysm')
    write(a/f'lang/{lang}.json',obj)
write(root/'pack.mcmeta',{'pack':{'pack_format':34,'description':'Arcane Cataclysm — original spell effects'}})
write(d/'damage_type/cataclysm.json',{'message_id':'arcane_cataclysm','scaling':'never','exhaustion':0.1,'effects':'hurt'})
write(root/'data/minecraft/tags/damage_type/bypasses_armor.json',{'replace':False,'values':['arcane_cataclysm:cataclysm']})
write(d/'tags/block/destruction_immune.json',{'replace':False,'values':['minecraft:bedrock','minecraft:barrier','minecraft:end_portal','minecraft:end_portal_frame','minecraft:end_gateway','minecraft:nether_portal','minecraft:command_block','minecraft:chain_command_block','minecraft:repeating_command_block','minecraft:structure_block','minecraft:structure_void','minecraft:jigsaw','minecraft:reinforced_deepslate']})
# Intentional hand-pixelled 32px item icon: midnight leather, aged gold hardware, cyan sigil.
im=Image.new('RGBA',(32,32));q=ImageDraw.Draw(im)
q.polygon([(6,3),(24,1),(28,5),(28,27),(10,31),(4,26),(4,7)],fill='#120e23')
q.polygon([(7,5),(24,3),(26,5),(26,26),(9,29),(6,26)],fill='#593757')
q.polygon([(9,6),(24,4),(24,24),(9,27)],fill='#2b213e')
q.polygon([(6,7),(9,6),(9,27),(6,25)],fill='#8e5a62')
q.polygon([(10,28),(25,25),(27,27),(11,30)],fill='#cfc2a1')
q.line([(12,28),(25,26)],fill='#827662',width=1)
q.line([(10,7),(22,5),(22,23),(11,25),(10,7)],fill='#b4925a',width=1)
for x,y in [(9,6),(23,4),(9,26),(23,24)]:
    q.rectangle((x-1,y-1,x+1,y+1),fill='#d6ad65');q.point((x,y-1),fill='#fff0bb')
q.ellipse((12,10,21,20),outline='#705ca7',width=1)
q.line([(12,13),(21,16),(13,19),(17,10),(20,19),(12,13)],fill='#ad90e3',width=1)
q.polygon([(17,11),(19,15),(17,19),(15,15)],fill='#5ce8f0')
q.line([(17,12),(17,16)],fill='#e3ffff',width=1)
q.rectangle((23,13,27,16),fill='#b69259');q.point((25,14),fill='#f9e2a0')
(a/'textures/item').mkdir(parents=True,exist_ok=True);im.save(a/'textures/item/grimoire_of_cataclysm.png')
im.resize((512,512),Image.Resampling.NEAREST).save(Path(__file__).resolve().parent/'grimoire-preview.png')
write(a/'models/item/grimoire_of_cataclysm.json',{'parent':'minecraft:item/generated','textures':{'layer0':'arcane_cataclysm:item/grimoire_of_cataclysm'},'display':{'thirdperson_righthand':{'rotation':[0,-30,-12],'translation':[0,2,0],'scale':[.85,.85,.85]},'firstperson_righthand':{'rotation':[0,-35,12],'translation':[1,3,1],'scale':[.9,.9,.9]}}})
# Synthesised fantasy audio; bounded peaks, smooth endpoints, no downloaded samples.
sr=24000;rng=np.random.default_rng(8791)
(a/'sounds').mkdir(parents=True,exist_ok=True)
for name,seconds in [('charge',4.8),('impact',4.2),('rumble',6.0)]:
    t=np.arange(int(sr*seconds))/sr;noise=rng.standard_normal(len(t));low=np.convolve(noise,np.ones(180)/180,'same')
    if name=='charge':
        phase=2*np.pi*(65*t+18*t*t);env=np.sin(np.pi*np.clip(t/seconds,0,1))**.6
        signal=(.25*np.sin(phase)+.12*np.sin(phase*1.5)+.10*np.sin(phase*2.01)+low*.6+noise*.02)*env
    elif name=='impact':
        signal=(.55*np.sin(2*np.pi*(48*t-14*np.log1p(t)))+low*2.2)*np.exp(-t*1.1)
        signal+=noise*.35*np.exp(-t*9)+.13*np.sin(2*np.pi*320*t)*np.exp(-t*4)
    else:
        signal=(low*2.7+.14*np.sin(2*np.pi*31*t)+.07*np.sin(2*np.pi*47*t))*np.exp(-t*.45)
    signal*=np.minimum(t/.015,1)*np.minimum((seconds-t)/.2,1);signal/=max(1,np.max(np.abs(signal))/ .9)
    tmp=Path(tempfile.gettempdir())/('arcane_cataclysm_'+name+'.wav')
    with wave.open(str(tmp),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((signal*32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(tmp),'-c:a','libvorbis','-q:a','4',str(a/f'sounds/{name}.ogg')],check=True)
    tmp.unlink()
write(a/'sounds.json',{n:{'subtitle':f'subtitles.arcane_cataclysm.{n}','sounds':[{'name':f'arcane_cataclysm:{n}','stream':False}]} for n in ['charge','impact','rumble']})
print('Generated item icon, RU/EN localization, damage data and 3 original OGG sounds.')
