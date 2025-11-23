# Install and start Windows service using WinSW
# WinSW 3.0+ supports user services (no admin required if user service mode is supported)

$serviceConfig = "truth-core-server.xml"
$winswExe = "winsw.exe"

if (Test-Path $winswExe) {
    # Install the service
    & $winswExe install $serviceConfig
    
    # Start the service
    & $winswExe start $serviceConfig
    
    Write-Host "truth-core-server service has been installed and started"
    Write-Host "Manage with: winsw.exe [start|stop|restart|status] $serviceConfig"
} else {
    Write-Error "WinSW executable not found: $winswExe"
    exit 1
}
