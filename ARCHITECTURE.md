# SCAIP server lab setup

## Flow

Public IP (dynamic)
    ↓
Kamailio TLS 5061 — TLS termination; receives SIP MESSAGE, forwards to backend
    ↓
Java SCAIP backend 5062 (UDP/TCP) — SIP stack receives MESSAGE, logs body, responds 200 OK
    ↓
Log files — SCAIP message bodies (e.g. for Apache log viewer / experimental use)

