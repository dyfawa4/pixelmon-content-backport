import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import local.pixelmon.backport.client.RaidCatchClock;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Pure Java arithmetic regression checks and actual target bytecode validation. */
public final class MergedPatchVerification {
    private static final String TARGET = "com/pixelmonmod/pixelmon/client/gui/raids/RaidCatchScreen";

    public static void main(String[] args) throws Exception {
        for (int fps : new int[] {30, 60, 120, 144, 240, 418, 480, 500, 1000, 2000}) {
            checkPacing(fps, 0);
            checkPacing(fps, -500);
        }
        RaidCatchClock clock = new RaidCatchClock();
        check(clock.advance(0, 2L) == 0, "two milliseconds preserve a fraction");
        check(clock.advance(0, 1L) == 1, "fraction contributes on the next frame");
        clock.reset();
        check(clock.advance(0, 1L) == 0, "phase reset discards previous fraction");
        clock.reset();
        check(clock.advance(7, 0L) == 7, "zero elapsed time");
        check(clock.advance(7, -10L) == 7, "clock rollback cannot rewind animation");
        clock.reset();
        check(clock.advance(Integer.MAX_VALUE - 1, 10L) == Integer.MAX_VALUE, "integer saturation");
        check(clock.advance(0, Long.MAX_VALUE) == Integer.MAX_VALUE, "large delta without long overflow");
        clock.reset();
        int jittered = 0;
        long totalMillis = 0;
        long[] deltas = {0, 1, 2, 3, 17, 0, 200, 1, 6, 33};
        for (int i = 0; i < 1000; i++) {
            long delta = deltas[i % deltas.length];
            jittered = clock.advance(jittered, delta);
            totalMillis += delta;
        }
        check(jittered == totalMillis * 2L / 5L, "jitter and pauses conserve elapsed time");
        int original = 0;
        for (int frame = 0; frame < 5000; frame++) original = (int) (original + 2L / 2.5D);
        check(original == 0, "original formula reproduces high-FPS stall");
        System.out.println("PASS: frame pacing, fractional carry, resets, clock rollback, overflow, original stall");
        verifyTarget(args[0]);
    }

    private static void checkPacing(int fps, int initial) {
        RaidCatchClock clock = new RaidCatchClock();
        int ticker = initial;
        long previous = 0;
        for (int frame = 1; frame <= fps * 10; frame++) {
            long now = frame * 1000L / fps;
            ticker = clock.advance(ticker, now - previous);
            previous = now;
            check(ticker == initial + now * 2L / 5L, "frame-independent progress at " + fps + " FPS");
        }
        check(ticker == initial + 4000, "ten-second animation progress at " + fps + " FPS");
    }

    private static void verifyTarget(String jarPath) throws Exception {
        ClassNode target = new ClassNode();
        try (JarFile jar = new JarFile(jarPath);
             InputStream input = jar.getInputStream(jar.getJarEntry(TARGET + ".class"))) {
            new ClassReader(input).accept(target, 0);
        }
        MethodNode render = find(target, "func_230430_a_", "(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V");
        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (AbstractInsnNode node : render.instructions.toArray()) if (node.getOpcode() >= 0) instructions.add(node);
        List<Integer> writes = new ArrayList<>();
        int clockCalls = 0;
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode node = instructions.get(i);
            if (node instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) node;
                if (field.getOpcode() == Opcodes.PUTFIELD && field.owner.equals(TARGET)
                    && field.name.equals("ticker") && field.desc.equals("I")) writes.add(i);
            }
            if (node instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) node;
                if (call.owner.equals(TARGET) && call.name.equals("getMillis") && call.desc.equals("()J")) clockCalls++;
            }
        }
        check(writes.size() == 2, "render must have exactly two ticker writes");
        check(clockCalls == 1, "render must sample elapsed time exactly once");
        check(instructions.get(writes.get(0) - 1).getOpcode() == Opcodes.ICONST_0, "first ticker write is the shake reset");
        int write = writes.get(1);
        check(instructions.get(write - 1).getOpcode() == Opcodes.D2I, "second write truncates animation time");
        check(instructions.get(write - 2).getOpcode() == Opcodes.DADD, "original ticker addition");
        check(instructions.get(write - 3).getOpcode() == Opcodes.DDIV, "original rate division");
        AbstractInsnNode scale = instructions.get(write - 4);
        check(scale instanceof LdcInsnNode && Double.valueOf(2.5D).equals(((LdcInsnNode) scale).cst), "original 2.5 time scale");
        check(instructions.get(write - 5).getOpcode() == Opcodes.L2D, "elapsed millisecond conversion");
        check(instructions.get(write - 6).getOpcode() == Opcodes.LLOAD, "elapsed delta local");
        check(instructions.get(write - 7).getOpcode() == Opcodes.I2D, "current ticker conversion");
        AbstractInsnNode current = instructions.get(write - 8);
        check(current instanceof FieldInsnNode && current.getOpcode() == Opcodes.GETFIELD
            && ((FieldInsnNode) current).name.equals("ticker") && ((FieldInsnNode) current).owner.equals(TARGET), "current ticker source");
        MethodNode millis = find(target, "getMillis", "()J");
        int returns = 0;
        for (AbstractInsnNode node : millis.instructions.toArray()) if (node.getOpcode() == Opcodes.LRETURN) returns++;
        check(returns == 2, "getMillis must have exactly two return injection sites");
        MethodNode setShakes = find(target, "setShakes", "(IZ)V");
        int starts = 0;
        for (AbstractInsnNode node : setShakes.instructions.toArray()) {
            if (node instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) node;
                if (field.getOpcode() == Opcodes.PUTFIELD && field.owner.equals(TARGET)
                    && field.name.equals("ticker") && field.desc.equals("I")) starts++;
            }
        }
        check(starts == 1, "catch start has exactly one ticker reset");
        check(target.fields.stream().anyMatch(field -> field.name.equals("ticker") && field.desc.equals("I")), "shadow field");
        System.out.println("PASS: actual Pixelmon target descriptors and all four injection selectors");
    }

    private static MethodNode find(ClassNode target, String name, String desc) {
        return target.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(desc))
            .findFirst().orElseThrow(() -> new IllegalStateException("Unsupported target: " + name + desc));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
