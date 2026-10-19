package local.pixelmon.backport;

import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.config.api.ui.ConfigSetting;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;

/** Allowlisted native configuration objects. Paths are fields, never filesystem paths. */
public final class ConfigEditor {
    private static final Gson JSON=new Gson();
    public static final String[] GROUPS={"general","battle","spawning","breeding","externalMoves","dimensions","raids","structures","quests","commands","storage","graphics"};
    private ConfigEditor(){}
    static Object root(String group){
        if(group.equals("backport"))return BackportConfig.values();
        if(!Arrays.asList(GROUPS).contains(group))throw new IllegalArgumentException("未知配置分类");
        try{return PixelmonConfigProxy.class.getMethod("get"+Character.toUpperCase(group.charAt(0))+group.substring(1)).invoke(null);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("配置尚未加载",e);}
    }
    public static final class Entry {
        final Object owner;final Field field;public final String path,help;public final boolean restart;
        Entry(Object owner,Field field,String path){this.owner=owner;this.field=field;this.path=path;field.setAccessible(true);
            ConfigSetting s=field.getAnnotation(ConfigSetting.class);restart=s!=null&&s.requiresRestart();
            String comment="";for(var a:field.getAnnotations())if(a.annotationType().getSimpleName().equals("Comment"))try{comment=String.valueOf(a.annotationType().getMethod("value").invoke(a));}catch(ReflectiveOperationException ignored){}
            String translated=ConfigLabels.help(path);help=translated.isEmpty()?comment:translated;
        }
        public JsonElement value(){try{return JSON.toJsonTree(field.get(owner),field.getGenericType());}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        public JsonObject describe(){JsonObject j=new JsonObject();j.addProperty("path",path);j.addProperty("label",ConfigLabels.text(path));JsonElement v=value();boolean large=v.toString().length()>2500;j.add("value",large?new JsonPrimitive("此列表较长，请在原配置文件中编辑"):v);j.addProperty("readOnly",large);j.addProperty("help",help.length()>500?help.substring(0,500):help);j.addProperty("restart",restart);j.addProperty("type",field.getType().getSimpleName());if(field.getType().isEnum()){JsonArray options=new JsonArray();for(Object option:field.getType().getEnumConstants())options.add(((Enum<?>)option).name());j.add("options",options);}if(List.class.isAssignableFrom(field.getType()))j.addProperty("itemType",((Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0]).getSimpleName());ConfigSetting s=field.getAnnotation(ConfigSetting.class);if(s!=null){j.addProperty("min",s.minValue());j.addProperty("max",s.maxValue());}return j;}
    }
    private static boolean scalar(Class<?> t){return t.isPrimitive()||t==String.class||t.isEnum()||Number.class.isAssignableFrom(t)||t==Boolean.class;}
    private static boolean supported(Field f){
        if(scalar(f.getType()))return true;
        if(List.class.isAssignableFrom(f.getType())&&f.getGenericType() instanceof ParameterizedType){Type t=((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];return t instanceof Class&&scalar((Class<?>)t);}
        return false;
    }
    private static void walk(Object obj,String prefix,List<Entry> result,int depth) throws IllegalAccessException {
        if(obj==null||depth>8)return;
        for(Field f:obj.getClass().getDeclaredFields()){
            if(Modifier.isStatic(f.getModifiers())||Modifier.isTransient(f.getModifiers())||Modifier.isFinal(f.getModifiers())||f.isSynthetic())continue;
            f.setAccessible(true);String path=prefix+f.getName();
            if(supported(f))result.add(new Entry(obj,f,path));
            else if(f.getType().getName().startsWith("com.pixelmonmod.pixelmon.api.config."))walk(f.get(obj),path+".",result,depth+1);
        }
    }
    public static List<Entry> entries(String group){try{List<Entry> rows=new ArrayList<>();walk(root(group),"",rows,0);rows.sort(Comparator.comparing(e->e.path));return rows;}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    public static void validateObject(Object obj){try{List<Entry> rows=new ArrayList<>();walk(obj,"",rows,0);for(Entry e:rows)parse(e,e.value());}catch(IllegalAccessException e){throw new IllegalArgumentException(e);}}
    private static Object parse(Entry entry,JsonElement value){
        Field f=entry.field;Class<?> t=f.getType();
        if(value==null||value.isJsonNull())throw new IllegalArgumentException("不能为空 / null is not allowed");
        if(t==boolean.class||t==Boolean.class){if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("需要 true 或 false");}
        else if(t==String.class){if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("字符串须使用 JSON 引号");if(value.getAsString().length()>8192)throw new IllegalArgumentException("文字过长");}
        else if(t.isEnum()){if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||Arrays.stream(t.getEnumConstants()).noneMatch(v->((Enum<?>)v).name().equals(value.getAsString())))throw new IllegalArgumentException("可选值: "+Arrays.toString(t.getEnumConstants()));}
        else if(List.class.isAssignableFrom(t)){
            if(!value.isJsonArray()||value.getAsJsonArray().size()>1024)throw new IllegalArgumentException("需要不超过 1024 项的 JSON 列表");
            Class<?> item=(Class<?>)((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0];
            for(JsonElement e:value.getAsJsonArray())if(!e.isJsonPrimitive()||(item==String.class&&!e.getAsJsonPrimitive().isString())||(item==Boolean.class&&!e.getAsJsonPrimitive().isBoolean())||(Number.class.isAssignableFrom(item)&&!e.getAsJsonPrimitive().isNumber()))throw new IllegalArgumentException("列表元素类型不匹配");
        }else {
            if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("需要数字");
            BigDecimal number;try{number=value.getAsBigDecimal();}catch(Exception e){throw new IllegalArgumentException("需要有限数字");}
            if(t==int.class||t==Integer.class)try{number.intValueExact();}catch(ArithmeticException e){throw new IllegalArgumentException("需要 32 位整数");}
            if(t==long.class||t==Long.class)try{number.longValueExact();}catch(ArithmeticException e){throw new IllegalArgumentException("需要 64 位整数");}
            if(!Double.isFinite(number.doubleValue())||(t==float.class&&!Float.isFinite(number.floatValue())))throw new IllegalArgumentException("数字越界");
            ConfigSetting s=f.getAnnotation(ConfigSetting.class);if(s!=null&&(number.compareTo(BigDecimal.valueOf(s.minValue()))<0||number.compareTo(BigDecimal.valueOf(s.maxValue()))>0))throw new IllegalArgumentException("范围: "+s.minValue()+" ~ "+s.maxValue());
        }
        try{return JSON.fromJson(value,f.getGenericType());}catch(Exception e){throw new IllegalArgumentException("配置值格式错误",e);}
    }
    public static synchronized String save(String group,String path,JsonElement expected,JsonElement value) throws Exception {
        Entry entry=entries(group).stream().filter(e->e.path.equals(path)).findFirst().orElseThrow(()->new IllegalArgumentException("未知配置项"));
        JsonElement before=entry.value();if(!before.equals(expected))throw new IllegalStateException("配置已被其他人修改，请刷新后再保存");
        Object parsed=parse(entry,value),old=entry.field.get(entry.owner);entry.field.set(entry.owner,parsed);
        try{
            if(group.equals("backport"))BackportConfig.save();
            else {
                Object config=root(group);Field ref=com.pixelmonmod.pixelmon.api.config.api.yaml.AbstractYamlConfig.class.getDeclaredField("config");ref.setAccessible(true);
                @SuppressWarnings("unchecked") var live=(info.pixelmon.repack.org.spongepowered.reference.ValueReference<Object,?>)ref.get(config);
                if(!live.setAndSave(config))throw new java.io.IOException("原生配置保存失败");
            }
        }catch(Exception e){entry.field.set(entry.owner,old);throw e;}
        if(group.equals("graphics")&&!entry.restart)ConfigLiveUpdate.graphics();
        return entry.restart?"已保存；此项需要重启游戏或服务器":"已保存并更新当前配置；实时读取项立即生效，生成／战斗初始化项在下一次创建时生效";
    }
    public static JsonObject page(String group,String search,int page){
        return page(group,search,page,8);
    }
    public static JsonObject page(String group,String search,int page,int size){
        size=Math.max(1,Math.min(8,size));
        String query=search.toLowerCase(Locale.ROOT);List<Entry> all=new ArrayList<>();for(Entry e:entries(group))if((e.path+" "+ConfigLabels.text(e.path)+" "+e.help).toLowerCase(Locale.ROOT).contains(query))all.add(e);
        int pages=Math.max(1,(all.size()+size-1)/size),index=Math.max(0,Math.min(pages-1,page));JsonObject result=new JsonObject();result.addProperty("pages",pages);result.addProperty("page",index);JsonArray rows=new JsonArray();
        for(int i=index*size;i<Math.min(all.size(),index*size+size);i++)rows.add(all.get(i).describe());result.add("entries",rows);return result;
    }
}
