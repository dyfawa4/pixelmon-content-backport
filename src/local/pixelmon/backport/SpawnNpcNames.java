package local.pixelmon.backport;

/** Change only the final formatted label, never candidate keys, filters or percentages. */
public final class SpawnNpcNames {
    private static final com.google.gson.JsonObject NAMES=Content.read("backport/spawn-npc-names.json");
    private SpawnNpcNames(){}
    public static String display(String line){
        for(var e:NAMES.entrySet())
            line=line.replace("\u00a76"+e.getKey()+"\u00a77:","\u00a76"+e.getValue().getAsString()+"\u00a77:");
        return line;
    }
}
