# SCAIP protocol (XML, spec format)

SCAIP (Social Care Alarm Internet Protocol) is a SIP-based protocol for alarm and notification events, standardised in **SIS SS 91100:2014** and **CENELEC 50134-9**. Messages are carried in the body of SIP MESSAGE requests/responses and use **XML**. This implementation follows the format defined in **SPEC.md** (short XML tags and status-number response).

## Transport

- **SIP MESSAGE** over UDP or TCP (TLS optional at edge).
- Request: client → server. Response: server → client (same transaction); **200 OK** with body containing **ref**, **snu** (status number), **ste** (status text).

## Request (incoming message)

The body of the SIP MESSAGE is XML (`Content-Type: application/xml`) with root `<scaip>` and **short tag names** per SPEC.md:

| Tag | Required | Description |
|-----|----------|-------------|
| `ver` | No | Version (default 01.00). |
| `cid` | **Yes** | Controller identifier. |
| `dty` | **Yes** | Device type (see table below). |
| `sco` | No | System config (see table below). |
| `cha` | No | Call handling (see table below). |
| `mty` | No | Message type (see table below). |
| `hbo` | No | Heartbeat options (see table below). |
| `did` | No | Device identifier. |
| `dco` | No | Device component (see SPEC.md for full list). |
| `dte` | No | Device text. |
| `crd` | No | Caller ID (default sip:). |
| `stc` | No | Status code (see table below for common values). |
| `stt` | No | Status text. |
| `pri` | No | Priority (0). |
| `lco` | No | Location code. |
| `lva` | No | Location value. |
| `lte` | No | Location text. |
| `ico` | No | Info code. |
| `ite` | No | Info text. |
| `ame` | No | Additional message. |
| `ref` | No | Reference (echoed in response). |

**System config (`sco`) values:**

| Code | Meaning |
|------|---------|
| `0` | Local unit and controller |
| `1` | Grouped equipment with supervisor off duty |
| `2` | Grouped equipment with supervisor on duty |
| `3` | Grouped equipment with supervisor on duty acting as alarm receiver |

**Call handling (`cha`) values:**

| Code | Meaning |
|------|---------|
| `0` | Outgoing call |
| `1` | Callback |

**Message type (`mty`) values:**

| Code | Meaning     |
|------|-------------|
| `ME` | Message     |
| `RE` | Reset       |
| `IN` | Information |
| `PI` | Heartbeat   |

**Heartbeat options (`hbo`) values:**

| Code  | Meaning      |
|-------|--------------|
| `0`   | Unadjustable |
| `001` | Adjustable   |

**Device type (`dty`) — common values:**

| Code   | Meaning           |
|--------|-------------------|
| `0002` | Local unit and controller |
| `0003` | Personal trigger  |
| `0004` | Fixed trigger    |
| `0005` | Fall detector    |
| `0007` | Panic button    |
| `0010` | Activity detector |
| `0045` | Controller       |
| `0048` | Local unit       |

(Full list in SPEC.md.)

**Status code (`stc`) — common values:**

| Code   | Meaning    |
|--------|------------|
| `0010` | Manual alarm |
| `0070` | Normal state |
| `0071` | Occupied    |
| `0092` | Automatic reset |
| `0093` | Manual reset  |

(Full list in SPEC.md.)

### Example request

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <ver>01.00</ver>
  <cid>+123456</cid>
  <dty>0004</dty>
  <did>001d940cb800</did>
  <dco>007</dco>
  <stc>0010</stc>
  <lco>021</lco>
  <lte>kitchen</lte>
  <pri>0</pri>
  <ref>a1b2c3d4e5f6g7h8</ref>
</scaip>
```

## Response

The server responds with **SIP 200 OK** and a body with spec tags:

| Tag | Description |
|-----|-------------|
| `ref` | Echo of request reference (or empty). |
| `snu` | Status number (see table below). |
| `ste` | Status text (e.g. error reason). |
| `cve` | Optional common version. |
| `mre` | Optional media reply (see table below). |
| `cre` | Optional callhandling reply (see table below). |
| `tnu` | Optional transferred number. |
| `hbi` | Optional heartbeat interval. |

**Media reply (`mre`) values:**

| Code | Meaning           |
|------|-------------------|
| `0`  | No voice call     |
| `1`  | Duplex voice call |
| `2`  | Microphone only   |
| `3`  | Speaker only      |

**Callhandling reply (`cre`) values:**

| Code | Meaning              |
|------|----------------------|
| `61` | Pre-defined receiver |
| `62` | Transferred number   |

### Status number (snu)

| Value | Meaning |
|-------|---------|
| 0 | OK — message accepted |
| 1 | Message too long |
| 2 | Invalid format |
| 3 | Wrong data content |
| 4 | Hold |
| 5 | Not treated |
| 6 | Busy |
| 7 | Mandatory tag missing |

### Example: OK (snu=0)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <ref>a1b2c3d4e5f6g7h8</ref>
  <snu>0</snu>
  <ste></ste>
</scaip>
```

### Example: NACK (snu=7, mandatory tag missing)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scaip>
  <ref></ref>
  <snu>7</snu>
  <ste>Missing required element(s): dty</ste>
</scaip>
```

## References

- SIS SS 91100:2014 — Social care alarm internet protocol (SCAIP).
- CENELEC EN 50134-9 — Alarm systems – Social alarm systems.
