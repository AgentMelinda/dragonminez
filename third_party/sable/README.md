# Sable soft-compat (DragonMineZ)

## License warning (read this)

Upstream [Sable](https://github.com/ryanhcode/sable) is licensed under **PolyForm Shield 1.0.0**.

That license **forbids distributing a product that competes with Sable**. Publishing a redistributed “DMZ Sable fork” jar as a drop-in replacement is almost certainly not allowed.

**What we do instead (legal soft-compat):**

1. Keep official Sable on the server/client as-is.
2. Ship **optional Mixins inside DragonMineZ** (`dragonminez.sable.mixins.json`) that only apply when Sable is present.
3. Use reflective `SableCompat` for coordinate projection (already used by ki/aim/render).

## What the DMZ mixins fix

Stack you hit:

```text
IOException: Received an invalid packet ID: 254
  at SableUDPPacketDecoder.decode
Server UDP channel caught exception
```

Packet **254 (`0xFE`)** is vanilla **legacy server-list ping / query noise** (or other non-Sable UDP) landing on Sable’s datagram port. Upstream throws; that floods logs and can disturb UDP auth/keepalives during join.

DMZ mixins:

| Mixin | Effect |
|-------|--------|
| `SableUDPPacketDecoderMixin` | **Uses Sable’s enum** (`SableUDPPacketType.VALUES.length`). Does not hardcode ids. Only cancels the `new IOException` path after that check fails |
| `SableUDPChannelHandlerServerMixin` | Swallow “invalid packet ID” in `exceptionCaught` |
| `SableUDPChannelHandlerClientMixin` | Same on client |

**Why not a magic max id (e.g. 32)?** Upstream already validates with the enum array length. Re-checking with a constant drifts when Sable adds packet types. Enum/`VALUES.length` is the source of truth.

Config remains available if you still want UDP off entirely:

- Server: `sable-common.toml` → `disable_udp_pipeline = true` and/or `attempt_udp_networking = false`
- Client: `sable-client.toml` → `attempt_udp_networking = false`

## Private source patch (optional, not redistributed)

A mirror of the one-line upstream fix is in `SableUDPPacketDecoder.patch` for **private** builds only. Do **not** publish a competing Sable jar under PolyForm Shield noncompete.

Upstream clone used for review (outside this repo): sibling `../sable-fork` from `https://github.com/ryanhcode/sable`.

## Verify

1. Install DMZ build with the sable mixin config registered in `neoforge.mods.toml`.
2. Keep Sable 2.0.x installed.
3. Join server — log should no longer spam `invalid packet ID: 254`.
4. Confirm ships / sub-levels still move (UDP may still fall back to TCP on bad NAT; that is separate).
