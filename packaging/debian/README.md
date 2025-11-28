## Debian packaging for truth-core-service.deb

### Package contents
- `/usr/local/bin/truth_core` (binary, provide ahead of time, built via `cargo build --release --bin truth_core_server --features desktop`)
- `/etc/truth-core/config.yml` (sample configuration)
- `/etc/systemd/system/truth-core.service` (systemd unit)
- System user `truthd` (no login, no home)
- `postinst` / `prerm` / `postrm` scripts (create user, enable/start/stop/purge)

### Installation
```
sudo dpkg -i truth-core-service.deb
```

### Service control
```
sudo systemctl status truth-core.service
sudo systemctl restart truth-core.service
sudo systemctl stop truth-core.service
```

### Notes
- User `truthd` is created without home or shell (hardened service account).
- Auto-start and restart handled through systemd.
- Constraint: no modification/logging of identities or acknowledgments—service is limited to auto-start, restart, and clean event propagation.
- To remove with purge: `sudo dpkg --purge truth-core-service`

_Version: v1.0.0_
