package com.kostya.cataclysm.world;
import java.util.*;
/** Pure scheduling rules, independent of live server/world objects. */
public final class EndSchedule {
 public static final List<Integer> WAVES=List.of(180,340,420,500,580,840);
 public static Map<UUID,Integer> assign(Collection<UUID> players,UUID caster,long seed){
  var ids=new ArrayList<UUID>(new LinkedHashSet<>(players));ids.remove(caster);Collections.shuffle(ids,new Random(seed));
  var groups=new LinkedHashMap<UUID,Integer>();for(int i=0;i<ids.size();i++)groups.put(ids.get(i),i%6);groups.put(caster,5);return groups;
 }
 private EndSchedule(){}
}
