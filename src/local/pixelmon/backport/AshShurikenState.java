package local.pixelmon.backport;
/** Per-instance state: native Attack equality compares moves, not their owners. */
public interface AshShurikenState {
    Integer backport$previousPP();
    void backport$previousPP(Integer value);
}
