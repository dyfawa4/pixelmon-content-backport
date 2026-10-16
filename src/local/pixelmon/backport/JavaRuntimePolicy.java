package local.pixelmon.backport;

/** This legacy Pixelmon runtime requires Java 11, including its reflection APIs. */
public final class JavaRuntimePolicy {
    private JavaRuntimePolicy(){}
    public static boolean allowed(int major){return major==11;}
    public static boolean allowed(){return allowed(Runtime.version().feature());}
    public static String actual(){return "Java"+Runtime.version().feature();}
}
