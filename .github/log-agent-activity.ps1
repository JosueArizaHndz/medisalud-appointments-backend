#!/usr/bin/env powershell
<#
.SYNOPSIS
#    Registra inicio de sesión, prompts de usuario y fin de interacción en .github/logs/activity-YYYY-MM-DD.jsonl
    Eventos: SessionStart | UserPromptSubmit | Stop
    Compatible con PowerShell 5.1+ (powershell) y PowerShell 7+ (pwsh).
#>

$DONE = '{"continue":true}'
$LOGS_DIR = [IO.Path]::GetFullPath([IO.Path]::Combine($PSScriptRoot, 'logs'))

# Leer stdin
try {
    $raw = [Console]::In.ReadToEnd()
    $ev = $raw | ConvertFrom-Json
}
catch { Write-Output $DONE; exit 0 }

if ($ev.PSObject.Properties['hookEventName']) { $hookEvent = $ev.hookEventName }
elseif ($ev.PSObject.Properties['hook_event_name']) { $hookEvent = $ev.hook_event_name }
else { $hookEvent = '' }

$now = [DateTime]::UtcNow
$timestamp = $now.ToString('yyyy-MM-ddTHH:mm:ss.') + $now.Millisecond.ToString('000') + 'Z'
$dateStr = $now.ToString('yyyy-MM-dd')

$sessionId = $null
if ($ev.PSObject.Properties['session_id']) { $sessionId = $ev.session_id }
elseif ($ev.PSObject.Properties['sessionId']) { $sessionId = $ev.sessionId }

$entry = $null

if ($hookEvent -eq 'SessionStart') {
    $entry = [ordered]@{
        type      = 'CREACION_SESION'
        sessionId = $sessionId
        startedAt = $timestamp
        cwd       = $ev.cwd
    }
}
elseif ($hookEvent -eq 'UserPromptSubmit') {
    $prompt = $null
    if ($ev.PSObject.Properties['prompt']) { $prompt = $ev.prompt }
    if ($prompt -is [string] -and $prompt.Length -gt 500) {
        $prompt = $prompt.Substring(0, 500)
    }
    $entry = [ordered]@{
        type      = 'INTERACCION'
        sessionId = $sessionId
        timestamp = $timestamp
        prompt    = $prompt
    }
}
elseif ($hookEvent -eq 'Stop') {
    $entry = [ordered]@{
        type      = 'FIN_INTERACCION'
        sessionId = $sessionId
        timestamp = $timestamp
    }
}

if (-not $entry) { Write-Output $DONE; exit 0 }

try {
    if (-not (Test-Path $LOGS_DIR)) {
        New-Item -ItemType Directory -Path $LOGS_DIR -Force | Out-Null
    }
    $logFile = Join-Path $LOGS_DIR "activity-$dateStr.jsonl"
    $line = $entry | ConvertTo-Json -Compress -Depth 5
    [IO.File]::AppendAllText($logFile, "$line`n", [Text.UTF8Encoding]::new($false))
}
catch {
    [Console]::Error.WriteLine("[log-agent-activity] Error: $_")
}

Write-Output $DONE
