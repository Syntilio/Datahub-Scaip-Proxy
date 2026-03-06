package com.syntilio.scaip.domain;

import com.syntilio.scaip.enums.DeviceComponent;
import com.syntilio.scaip.enums.DeviceType;
import com.syntilio.scaip.enums.StatusCode;

/**
 * SCAIP request message (spec format). Use the builder to create alarm, heartbeat,
 * or invalid examples. Required for a valid message: controllerId (cid), deviceType (dty).
 */
public final class ScaipRequest {

    private final String ref;
    private final String ver;
    private final String controllerId;
    private final DeviceType deviceType;
    private final String deviceId;
    private final DeviceComponent deviceComponent;
    private final StatusCode statusCode;
    private final String statusText;
    private final String locationCode;
    private final String locationText;
    private final String priority;

    private ScaipRequest(Builder b) {
        this.ref = b.ref;
        this.ver = b.ver;
        this.controllerId = b.controllerId;
        this.deviceType = b.deviceType;
        this.deviceId = b.deviceId;
        this.deviceComponent = b.deviceComponent;
        this.statusCode = b.statusCode;
        this.statusText = b.statusText;
        this.locationCode = b.locationCode;
        this.locationText = b.locationText;
        this.priority = b.priority;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Example alarm request (cid, dty, did, dco, stc, lco, lte, pri, ref). */
    public static ScaipRequest alarm(String ref, DeviceType deviceType, DeviceComponent deviceComponent, StatusCode statusCode) {
        return builder()
            .ref(ref)
            .ver("01.00")
            .controllerId("+123456")
            .deviceType(deviceType)
            .deviceId("001d940cb800")
            .deviceComponent(deviceComponent)
            .statusCode(statusCode)
            .locationCode("021")
            .locationText("kitchen")
            .priority("0")
            .build();
    }

    /** Example heartbeat / normal (cid, dty, did, stc, ref). */
    public static ScaipRequest heartbeat(String ref, DeviceType deviceType, StatusCode statusCode) {
        return builder()
            .ref(ref)
            .ver("01.00")
            .controllerId("+123456")
            .deviceType(deviceType)
            .deviceId("001d940cb800")
            .statusCode(statusCode)
            .build();
    }

    /** Example invalid request (missing required dty). */
    public static ScaipRequest invalid(StatusCode statusCode) {
        return builder()
            .controllerId("+123456")
            .deviceId("001d940cb800")
            .statusCode(statusCode)
            .build();
    }

    public String getRef() { return ref; }
    public String getVer() { return ver; }
    public String getControllerId() { return controllerId; }
    public DeviceType getDeviceType() { return deviceType; }
    public String getDeviceId() { return deviceId; }
    public DeviceComponent getDeviceComponent() { return deviceComponent; }
    public StatusCode getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public String getLocationCode() { return locationCode; }
    public String getLocationText() { return locationText; }
    public String getPriority() { return priority; }

    public static final class Builder {
        private String ref;
        private String ver;
        private String controllerId;
        private DeviceType deviceType;
        private String deviceId;
        private DeviceComponent deviceComponent;
        private StatusCode statusCode;
        private String statusText;
        private String locationCode;
        private String locationText;
        private String priority;

        public Builder ref(String ref) { this.ref = ref; return this; }
        public Builder ver(String ver) { this.ver = ver; return this; }
        public Builder controllerId(String controllerId) { this.controllerId = controllerId; return this; }
        public Builder deviceType(DeviceType deviceType) { this.deviceType = deviceType; return this; }
        public Builder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public Builder deviceComponent(DeviceComponent deviceComponent) { this.deviceComponent = deviceComponent; return this; }
        public Builder statusCode(StatusCode statusCode) { this.statusCode = statusCode; return this; }
        public Builder statusText(String statusText) { this.statusText = statusText; return this; }
        public Builder locationCode(String locationCode) { this.locationCode = locationCode; return this; }
        public Builder locationText(String locationText) { this.locationText = locationText; return this; }
        public Builder priority(String priority) { this.priority = priority; return this; }

        public ScaipRequest build() {
            return new ScaipRequest(this);
        }
    }
}
