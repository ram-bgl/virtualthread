import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

public class RunMonitor {
    private final Map<Integer, ExecutionTimeframe> taskCompletionTimes = Collections.synchronizedMap(new TreeMap<>());
    private final long tSubmit = currentTimeMillis();

    public Runnable run(int taskId, Runnable runnable) {
        return () -> {
            long tStart = currentTimeMillis();
            String startThreadName = Thread.currentThread().toString();
            runnable.run();
            String endThreadName = Thread.currentThread().toString();
            long tEnd = currentTimeMillis();
            //System.out.println("hop check st= " + startThreadName + " end= "+endThreadName);
            String hop = startThreadName.equals(endThreadName)
                    ? "❌No hop from " + friendlyVTName(startThreadName)
                    : "✅Hopped from " + friendlyVTName(startThreadName) + " to " + friendlyVTName(endThreadName);
            var prev = taskCompletionTimes.put(taskId,
                    new ExecutionTimeframe(tStart - tSubmit, tEnd - tSubmit, '*', hop));
            if (prev != null) {
                throw new IllegalArgumentException("Task ID already exists: " + taskId);
            }
        };
    }

    public void printExecutionTimes() {
        long max = taskCompletionTimes.values().stream().mapToLong(ExecutionTimeframe::end).max()
                .orElseThrow(() -> new IllegalStateException("Tasks finished too fast! Please add more work."));
        double r = 50d / max;
        for (Integer taskId : taskCompletionTimes.keySet()) {
            ExecutionTimeframe t = taskCompletionTimes.get(taskId);
            String spaces = " ".repeat((int) (t.start() * r));
            String action = ("" + t.symbol).repeat((int) ((t.end() - t.start()) * r));
            String trail = " ".repeat(50 - spaces.length() - action.length() - 1);
            System.out.printf("Task %02d: %s%s%s ->> %s%n", taskId, spaces, action, trail, t.hop);
        }
        System.out.printf("Total runtime = %d millis for %d tasks on a machine with %d CPUs%n",
                currentTimeMillis() - tSubmit,
                taskCompletionTimes.size(),
                Runtime.getRuntime().availableProcessors());
    }

    record ExecutionTimeframe(long start, long end, char symbol, String hop) {
    }

    public static String friendlyVTName(String threadToString) {
        Matcher matcher = virtualThreadNamePattern.matcher(threadToString);
        if (!matcher.matches()) {
            System.err.println(
                    "Virtual thread name '" + threadToString + "' does not match the expected pattern");
        }
        return String.format("(VT#%2s<->PT#%2s)", matcher.group(1), matcher.group(3));
    }

    private static Pattern virtualThreadNamePattern = Pattern.compile("VirtualThread\\[#(\\d+)\\]/runnable@ForkJoinPool-(\\d+)-worker-(\\d+)");
}
