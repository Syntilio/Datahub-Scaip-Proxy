# SCAIP protocol (XML, ACK/NACK)

SCAIP (Social Care Alarm Internet Protocol) is a SIP-based protocol for alarm and notification events, standardised in **SIS SS 91100:2014** and **CENELEC 50134-9**. Messages are carried in the body of SIP MESSAGE requests/responses and use **XML**.

## Transport

- **SIP MESSAGE** over UDP or TCP (TLS optional at edge).
- Request: client → server. Response: server → client (same transaction); **200 OK** with body indicating **ACK** (accepted) or **NACK** (rejected).

## Incoming message (alarm/event)

The body of the SIP MESSAGE is XML (`Content-Type: application/xml`) with a root element and standard fields.

### Standard fields (CENELEC / SCAIP)

| Field             | Required | Description |
|-------------------|----------|-------------|
| `controllerId`    | Yes      | Identifier of the SCAIP controller. |
| `deviceId`        | Yes      | Identifier of the device triggering the alarm. |
| `deviceType`      | Yes      | Type of device (e.g. fall sensor, panic button, fixedTrigger). |
| `statusCode`      | Yes      | Current status (e.g. alarm, normal, lowBattery, restoral). |
| `deviceComponent` | No       | Component that raised the alarm (e.g. button1, unspecified). |
| `location`        | No       | Fixed location (e.g. kitchen) or GPS. |
| `priority`        | No       | Alarm priority 0–9 (9 = highest). |

### Example request body

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <controllerId>+123456</controllerId>
  <deviceId>001d940cb800</deviceId>
  <deviceType>fixedTrigger</deviceType>
  <deviceComponent>unspecified</deviceComponent>
  <statusCode>alarm</statusCode>
  <location>kitchen</location>
  <priority>5</priority>
</scaip>
```

## Response (ACK / NACK)

The server responds with **SIP 200 OK** and a body that indicates acceptance or rejection.

### ACK (accepted)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <result>ACK</result>
</scaip>
```

### NACK (rejected)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <result>NACK</result>
  <reason>Missing required element: deviceId</reason>
</scaip>
```

- **ACK**: message was valid and accepted.
- **NACK**: message was invalid or could not be processed; `reason` may describe the error.

## References

- SIS SS 91100:2014 — Social care alarm internet protocol (SCAIP).
- CENELEC EN 50134-9 — Alarm systems – Social alarm systems.
- [iotcomms.io SCAIP / SIP Message](https://iotcomms.io/developer/documentation/apis/sip-message) — Implementation notes and field mapping.
