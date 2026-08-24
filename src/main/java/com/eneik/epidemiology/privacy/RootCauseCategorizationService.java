package com.eneik.epidemiology.privacy;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RootCauseCategorizationService {

    private static final Map<String, String> PATTERN_ID_MAP = new HashMap<>();

    static {
        PATTERN_ID_MAP.put("reviewConcerns_8_CONSECUTIVE_SAME_SIDE", "PATTERN_DESIGN_REVIEW_CONCERNS_INVARIANT");
    }

    public String assignRootCausePatternId(String streamName, String shiftType) {
        if (streamName == null || shiftType == null) {
            return "UNCATEGORIZED";
        }
        String key = streamName + "_" + shiftType;
        return PATTERN_ID_MAP.getOrDefault(key, "UNCATEGORIZED");
    }

    public DefectEvent processDefectEvent(DefectEvent event) {
        if (event != null && event.getRootCausePatternId() == null) {
            String patternId = assignRootCausePatternId(event.getStreamName(), event.getShiftType());
            if (!"UNCATEGORIZED".equals(patternId)) {
                event.setRootCausePatternId(patternId);
            }
        }
        return event;
    }
}
