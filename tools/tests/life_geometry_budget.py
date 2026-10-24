"""Deterministic geometry-work estimate, NOT timing/FPS or a live-server benchmark.
Compares 0.6.0 formula (commit 3d1a460) with 0.6.1 for flat terrain y=60,
centre y=60, minY=-64/maxY=320, scale=1, radius=352. Material sampling omitted.
"""
import math,json
radius=352;old_samples=new_samples=old_fill=new_fill=columns=0
for x in range(-radius,radius):
 for z in range(-radius,radius):
  dx=x+.5;dz=z+.5;d=math.hypot(dx,dz)
  if d>radius:continue
  u=d/radius;a=math.atan2(dz,dx);raw=int(60-165*(1-u*u));spike=0
  root=abs(math.sin(a*9+d*.017))*d
  if root<5:spike=max(0,60-raw)+int(35*(1-u))
  if u<.1:spike=max(0,60-raw)+int(125*(1-u/.12))
  floor=max(-64,min(318,raw));spike=max(0,min(spike,319-floor));bottom=max(-64,floor-4)
  old_top=min(319,max(floor+spike,floor,130));new_top=max(floor,60)
  old_samples+=old_top-bottom+1;new_samples+=new_top-bottom+1
  old_fill+=max(0,floor+spike-60);columns+=1
print(json.dumps({'baseline_commit':'3d1a460','scope':'deterministic flat-world column-range estimate; not FPS or elapsed time','columns':columns,'old_snapshot_states':old_samples,'new_snapshot_states':new_samples,'old_above_terrain_structure_cells':old_fill,'new_above_terrain_structure_cells':new_fill},indent=2))
assert new_samples<old_samples and new_fill==0 and old_fill>0
