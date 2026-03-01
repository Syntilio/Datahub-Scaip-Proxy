# SCAIP scaling guide

This document describes how the SCAIP backend is scaled (number of Java instances and memory) and how to change it.

## Overview

- **Kamailio** receives SIP MESSAGE on TCP 5060 / TLS 5061 and load-balances to a set of **Java SCAIP backends** on localhost.
- Each backend is a separate JVM (`scaip@5062`, `scaip@5063`, …), one per port.
- The **dispatcher** list defines how many backends exist and that traffic to them uses TCP.
- Each JVM’s heap is limited so that **N** backends together use a fixed amount of RAM (e.g. 3 GB on a 4 GB host).

## Current defaults

| Setting        | Value | Where configured |
|----------------|--------|-------------------|
| Number of backends | 5  | `server-config/dispatcher.list`, `server-config/bootstrap.sh` |
| Backend ports  | 5062–5066 | Same files |
| Heap per JVM   | 600 MB (`-Xmx600m`) | `server-config/scaip@.service` |
| Total heap     | 3 GB (5 × 600 MB) | For a 4 GB server, ~1 GB left for OS/Kamailio/Apache |

## Changing the number of backends

You need to keep three places in sync.

### 1. Dispatcher list (`server-config/dispatcher.list`)

Add or remove one line per backend. Format:

```text
1 sip:127.0.0.1:PORT;transport=tcp 0 0
```

- Use consecutive ports starting at 5062 (e.g. 5062, 5063, …, 5066 for 5 backends).
- Keep `;transport=tcp` so Kamailio uses TCP to the backends.

Example for **5 backends** (current):

```text
1 sip:127.0.0.1:5062;transport=tcp 0 0
1 sip:127.0.0.1:5063;transport=tcp 0 0
1 sip:127.0.0.1:5064;transport=tcp 0 0
1 sip:127.0.0.1:5065;transport=tcp 0 0
1 sip:127.0.0.1:5066;transport=tcp 0 0
```

### 2. Bootstrap script (`server-config/bootstrap.sh`)

- **Enable/start** only the ports you use. Find the two lines with `systemctl enable` and `systemctl start` and list the same ports, e.g.:

  ```bash
  systemctl enable scaip@5062 scaip@5063 scaip@5064 scaip@5065 scaip@5066
  systemctl start scaip@5062 scaip@5063 scaip@5064 scaip@5065 scaip@5066
  ```

- If you **reduce** the number of backends (e.g. from 10 to 5), keep or add the loop that stops and disables the higher ports so old instances don’t keep running:

  ```bash
  for p in 5067 5068 5069 5070 5071; do systemctl stop scaip@$p 2>/dev/null || true; systemctl disable scaip@$p 2>/dev/null || true; done
  ```

  Adjust the list `5067 5068 …` to any ports you are **removing** (everything above your new highest port).

### 3. Java heap in systemd (`server-config/scaip@.service`)

- Decide total heap for all backends (e.g. 3 GB on a 4 GB machine).
- **Per-JVM heap** = total heap ÷ number of backends.
- Set `-Xmx` (and optionally `-Xms`) in `ExecStart`:

  ```ini
  # Example: 5 backends, 3 GB total → 600m each
  ExecStart=/usr/bin/java -Xmx600m -Xms256m -jar /opt/scaip/runtime/app.jar
  ```

  If you change to 10 backends and still want 3 GB total, use `-Xmx300m` (3000/10). Update the comment in the unit to match.

## Formula

- **Heap per backend:** `total_heap_mb / number_of_backends`
- **Example (4 GB server, 1 GB for OS/rest):** 3 GB for Java → 5 backends → `-Xmx600m` each.

## Deploying changes

1. **Edit** `server-config/dispatcher.list`, `server-config/bootstrap.sh`, and `server-config/scaip@.service` as above.
2. **On the server:** run bootstrap (or at least):
   - Copy `scaip@.service` to `/etc/systemd/system/scaip@.service`.
   - Run `systemctl daemon-reload`.
   - Enable/start the desired `scaip@PORT` units; stop/disable any ports you removed.
3. **Copy** the new `dispatcher.list` to `/etc/kamailio/dispatcher.list`.
4. **Reload** the dispatcher so Kamailio uses the new set:
   - `kamctl dispatcher reload`  
   or restart Kamailio: `systemctl restart kamailio`.

## Checking that it matches

- Backends: `systemctl list-units 'scaip@*'` (only the ports you want should be active).
- Dispatcher: `kamctl dispatcher list` (should list the same ports as in `dispatcher.list`).
- Memory: `ps -o rss,cmd -C java` (RSS per JVM should be in the same order as your `-Xmx`).
