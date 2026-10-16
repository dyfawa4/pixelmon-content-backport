package local.pixelmon.backport;

import java.util.*;
import java.util.function.Function;

/** Exact names only; ambiguous translated names never pick an arbitrary object. */
public final class EditorNameAliases {
    private EditorNameAliases(){}
    public static String key(String value){
        return value==null?"":value.replaceAll("(?i)§[0-9a-fk-or]","").trim()
            .toLowerCase(Locale.ROOT).replace('é','e').replaceAll("[\\s_]+","");
    }
    public static <T> String resolve(String input,Collection<T> values,Function<T,String> id,Function<T,String> label){
        String query=key(input);if(query.isEmpty())return input;
        // Explicit internal names take precedence over a translated alias.
        String direct=match(query,values,id,id);if(direct!=null)return direct;
        String translated=match(query,values,id,label);return translated==null?input:translated;
    }
    private static <T> String match(String query,Collection<T> values,Function<T,String> id,Function<T,String> label){
        String found=null;for(T value:values){
            if(value==null || !query.equals(key(label.apply(value))))continue;
            String candidate=id.apply(value);if(candidate==null)continue;
            if(found!=null&&!found.equals(candidate))return null;found=candidate;
        }return found;
    }
    public static boolean prefix(String query,String id,String label){
        String text=key(query);return key(id).startsWith(text)||key(label).startsWith(text);
    }
    public static <T> boolean ambiguous(String input,Collection<T> values,Function<T,String> id,Function<T,String> label){
        String query=key(input);Set<String> matches=new HashSet<>();
        for(T value:values){if(query.equals(key(id.apply(value))))return false;if(query.equals(key(label.apply(value))))matches.add(id.apply(value));}
        return matches.size()>1;
    }
}
