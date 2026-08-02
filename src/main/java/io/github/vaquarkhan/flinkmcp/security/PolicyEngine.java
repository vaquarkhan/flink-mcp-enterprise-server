package io.github.vaquarkhan.flinkmcp.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Viquar Khan
 */
public final class PolicyEngine {

    private final boolean failClosed;
    private final List<Pattern> denyTools;
    private final List<Pattern> denyJobs;

    private PolicyEngine(boolean failClosed, List<Pattern> denyTools, List<Pattern> denyJobs) {
        this.failClosed = failClosed;
        this.denyTools = denyTools;
        this.denyJobs = denyJobs;
    }

    public static PolicyEngine load(String policyFile) {
        if (policyFile == null || policyFile.isBlank()) {
            return new PolicyEngine(false, List.of(), List.of());
        }
        try {
            List<String> lines = Files.readAllLines(Path.of(policyFile));
            List<Pattern> tools = new ArrayList<>();
            List<Pattern> jobs = new ArrayList<>();
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("deny tool ")) {
                    tools.add(globToRegex(line.substring("deny tool ".length()).trim()));
                } else if (line.startsWith("deny job ")) {
                    jobs.add(globToRegex(line.substring("deny job ".length()).trim()));
                }
            }
            return new PolicyEngine(false, List.copyOf(tools), List.copyOf(jobs));
        } catch (IOException e) {
            return new PolicyEngine(true, List.of(), List.of());
        }
    }

    public boolean allows(String tool, String jobId) {
        if (failClosed) {
            return false;
        }
        for (Pattern p : denyTools) {
            if (p.matcher(tool).matches()) {
                return false;
            }
        }
        if (jobId != null && !jobId.isBlank()) {
            for (Pattern p : denyJobs) {
                if (p.matcher(jobId).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                sb.append(".*");
            } else if (c == '?') {
                sb.append('.');
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString());
    }
}
