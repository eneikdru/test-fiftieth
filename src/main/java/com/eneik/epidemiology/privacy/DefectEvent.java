package com.eneik.epidemiology.privacy;

public class DefectEvent {

    private String streamName;
    private String shiftType;
    private String rootCausePatternId;

    public DefectEvent() {}

    public DefectEvent(String streamName, String shiftType) {
        this.streamName = streamName;
        this.shiftType = shiftType;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getRootCausePatternId() {
        return rootCausePatternId;
    }

    public void setRootCausePatternId(String rootCausePatternId) {
        this.rootCausePatternId = rootCausePatternId;
    }
}
