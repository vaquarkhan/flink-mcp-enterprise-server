package io.github.vaquarkhan.flinkmcp.observability;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Metrics {

    private static final int RESERVOIR = 1024;

    private final ConcurrentHashMap<String, AtomicLong> allowedByTool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> deniedByCode = new ConcurrentHashMap<>();
    private final AtomicLong totalCalls = new AtomicLong();
    private final AtomicLong bytesIn = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();
    private final ConcurrentHashMap<String, LatencyRing> latencyByTool = new ConcurrentHashMap<>();
    private final long startNanos = System.nanoTime();

    public void recordAllowed(String tool, long latencyMs) {
        totalCalls.incrementAndGet();
        allowedByTool.computeIfAbsent(tool, k -> new AtomicLong()).incrementAndGet();
        latencyByTool.computeIfAbsent(tool, k -> new LatencyRing()).add(latencyMs);
    }

    public void recordDenied(String tool, String code) {
        totalCalls.incrementAndGet();
        deniedByCode.computeIfAbsent(code, k -> new AtomicLong()).incrementAndGet();
    }

    public void addBytesIn(long n) {
        bytesIn.addAndGet(n);
    }

    public void addBytesOut(long n) {
        bytesOut.addAndGet(n);
    }

    public long getBytesIn() {
        return bytesIn.get();
    }

    public long getBytesOut() {
        return bytesOut.get();
    }

    public String toJson() {
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long uptime = (System.nanoTime() - startNanos) / 1_000_000L;
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"total_calls\":").append(totalCalls.get()).append(',');
        sb.append("\"uptime_ms\":").append(uptime).append(',');
        sb.append("\"bandwidth\":{");
        sb.append("\"bytes_in\":").append(bytesIn.get()).append(',');
        sb.append("\"bytes_out\":").append(bytesOut.get());
        sb.append("},");
        sb.append("\"jvm\":{");
        sb.append("\"heap_used_bytes\":").append(mem.getHeapMemoryUsage().getUsed()).append(',');
        sb.append("\"heap_max_bytes\":").append(mem.getHeapMemoryUsage().getMax()).append(',');
        sb.append("\"available_processors\":").append(Runtime.getRuntime().availableProcessors());
        sb.append("},");
        sb.append("\"allowed_by_tool\":{");
        appendLongMap(sb, allowedByTool);
        sb.append("},");
        sb.append("\"denied_by_code\":{");
        appendLongMap(sb, deniedByCode);
        sb.append("},");
        sb.append("\"latency_ms_by_tool\":{");
        boolean first = true;
        for (Map.Entry<String, LatencyRing> e : latencyByTool.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            long[] p = e.getValue().percentiles();
            sb.append('"').append(jsonEsc(e.getKey())).append("\":{");
            sb.append("\"p50\":").append(p[0]).append(',');
            sb.append("\"p95\":").append(p[1]).append(',');
            sb.append("\"p99\":").append(p[2]);
            sb.append('}');
        }
        sb.append("}");
        sb.append('}');
        return sb.toString();
    }

    public String toPrometheus() {
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        StringBuilder sb = new StringBuilder();
        sb.append("flink_mcp_calls_total ").append(totalCalls.get()).append('\n');
        sb.append("flink_mcp_bytes_in_total ").append(bytesIn.get()).append('\n');
        sb.append("flink_mcp_bytes_out_total ").append(bytesOut.get()).append('\n');
        sb.append("flink_mcp_heap_used_bytes ").append(mem.getHeapMemoryUsage().getUsed()).append('\n');
        for (Map.Entry<String, AtomicLong> e : allowedByTool.entrySet()) {
            sb.append("flink_mcp_tool_allowed_total{tool=\"").append(promEsc(e.getKey())).append("\"} ")
                    .append(e.getValue().get()).append('\n');
        }
        for (Map.Entry<String, AtomicLong> e : deniedByCode.entrySet()) {
            sb.append("flink_mcp_denied_total{code=\"").append(promEsc(e.getKey())).append("\"} ")
                    .append(e.getValue().get()).append('\n');
        }
        for (Map.Entry<String, LatencyRing> e : latencyByTool.entrySet()) {
            long p99 = e.getValue().percentiles()[2];
            sb.append("flink_mcp_tool_latency_ms{tool=\"").append(promEsc(e.getKey()))
                    .append("\",quantile=\"0.99\"} ").append(p99).append('\n');
        }
        return sb.toString();
    }

    private static void appendLongMap(StringBuilder sb, Map<String, AtomicLong> map) {
        boolean first = true;
        for (Map.Entry<String, AtomicLong> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(jsonEsc(e.getKey())).append("\":").append(e.getValue().get());
        }
    }

    private static String jsonEsc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String promEsc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class LatencyRing {
        private final long[] buf = new long[RESERVOIR];
        private int size;
        private int idx;

        synchronized void add(long v) {
            buf[idx] = v;
            idx = (idx + 1) % RESERVOIR;
            if (size < RESERVOIR) {
                size++;
            }
        }

        synchronized long[] percentiles() {
            if (size == 0) {
                return new long[]{0, 0, 0};
            }
            long[] copy = Arrays.copyOf(buf, size);
            Arrays.sort(copy);
            return new long[]{
                    percentile(copy, 0.50),
                    percentile(copy, 0.95),
                    percentile(copy, 0.99)
            };
        }

        private static long percentile(long[] sorted, double p) {
            int i = (int) Math.ceil(p * sorted.length) - 1;
            if (i < 0) {
                i = 0;
            }
            if (i >= sorted.length) {
                i = sorted.length - 1;
            }
            return sorted[i];
        }
    }
}
