package local.pixelmon.backport.client;

/** Optional detector markers can suppress just their world-space guide line. */
public interface DetectorLineVisibility {
    boolean backport$hideLine();
}
