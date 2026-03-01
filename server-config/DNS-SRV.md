# DNS SRV records for SCAIP

SCAIP is SIP-based. Clients can discover the SIP server using DNS SRV records so they don’t need to hardcode the port.

## Domain and ports

- **Domain:** `scaip.syntilio.com` (or your deployed hostname)
- **SIP (TCP):** port **5060**
- **SIPS (TLS):** port **5061**

Ensure the hostname has an **A** (or **AAAA**) record pointing to the server’s IP before adding SRV records.

## SRV records to add

Use your DNS provider’s UI or API to create these. Replace `scaip.syntilio.com` with your hostname if different.

| Type | Name | Value (priority, weight, port, target) |
|------|------|----------------------------------------|
| SRV | `_sip._tcp.scaip.syntilio.com` | `0 0 5060 scaip.syntilio.com.` |
| SRV | `_sips._tcp.scaip.syntilio.com` | `0 0 5061 scaip.syntilio.com.` |

- **Priority** `0`: higher priority (lower number = preferred).
- **Weight** `0`: no weighting between records of same priority.
- **Port:** 5060 for SIP/TCP, 5061 for SIPS/TLS.
- **Target:** must end with a dot (`.`), and must resolve to an A/AAAA record (usually the same hostname).

## Example (BIND-style)

```text
; SCAIP SIP over TCP (no TLS)
_sip._tcp.scaip.syntilio.com.  300  IN  SRV  0  0  5060  scaip.syntilio.com.

; SCAIP SIP over TLS (SIPS)
_sips._tcp.scaip.syntilio.com. 300  IN  SRV  0  0  5061  scaip.syntilio.com.
```

TTL `300` (5 minutes) is optional; adjust as needed.

## Provider-specific notes

- **Cloudflare:** Create SRV record; “Name” is `_sip._tcp.scaip` (without the domain suffix); “Target” is `scaip.syntilio.com`; “Port” 5060; Priority 0, Weight 0.
- **Route53:** Record type SRV; “Value” like `0 0 5060 scaip.syntilio.com.`
- **Hetzner DNS:** Add SRV record with service `_sip`, protocol `_tcp`, name `scaip.syntilio.com` (or leave for apex), priority 0, weight 0, port 5060, target `scaip.syntilio.com`.

## Verification

```bash
# SIP over TCP (should show port 5060)
dig SRV _sip._tcp.scaip.syntilio.com +short

# SIPS over TLS (should show port 5061)
dig SRV _sips._tcp.scaip.syntilio.com +short
```

Example output:

```text
0 0 5060 scaip.syntilio.com.
0 0 5061 scaip.syntilio.com.
```

Clients that support SIP SRV (RFC 3263) can then connect to `scaip.syntilio.com` without specifying the port; they will look up `_sip._tcp.<domain>` or `_sips._tcp.<domain>` and use the returned port.
