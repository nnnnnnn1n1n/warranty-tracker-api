#!/usr/bin/env pwsh
<#
.SYNOPSIS
Removes UTF-8 BOM from a file if present.

.DESCRIPTION
Inspects the first bytes of a file. If a UTF-8 BOM (EF BB BF) is detected,
removes only those 3 bytes and writes the remaining bytes back to the file.

No re-encoding occurs. File content (bytes 3 onwards) is preserved exactly.
If no BOM is present, the file is left unchanged.

.PARAMETER FilePath
The path to the file to fix.

.NOTES
Exit codes:
  0 = Success (BOM removed or file was already clean)
  1 = File has invalid encoding detected (not attempted to fix)
  2 = File not found or unreadable
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$FilePath
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# 1. File existence check
# ---------------------------------------------------------------------------
if (-not (Test-Path -Path $FilePath -PathType Leaf)) {
    [Console]::Error.WriteLine("ERROR: File not found: $FilePath")
    exit 2
}

try {
    # ---------------------------------------------------------------------------
    # 2. Read ALL raw bytes from the file
    # ---------------------------------------------------------------------------
    $allBytes = [System.IO.File]::ReadAllBytes($FilePath)

    # ---------------------------------------------------------------------------
    # 3. Empty file is valid (nothing to fix)
    # ---------------------------------------------------------------------------
    if ($allBytes.Length -eq 0) {
        exit 0
    }

    # ---------------------------------------------------------------------------
    # 4. Check for UTF-8 BOM at the beginning
    # ---------------------------------------------------------------------------
    $b0 = $allBytes[0]
    $b1 = if ($allBytes.Length -gt 1) { $allBytes[1] } else { -1 }
    $b2 = if ($allBytes.Length -gt 2) { $allBytes[2] } else { -1 }

    # UTF-8 with BOM: EF BB BF
    if ($b0 -eq 0xEF -and $b1 -eq 0xBB -and $b2 -eq 0xBF) {
        # Remove only the BOM (first 3 bytes)
        if ($allBytes.Length -gt 3) {
            $cleanBytes = $allBytes[3..($allBytes.Length - 1)]
        }
        else {
            # File contains only the BOM bytes
            $cleanBytes = @()
        }

        # Write cleaned bytes back to file
        [System.IO.File]::WriteAllBytes($FilePath, $cleanBytes)
        exit 0
    }

    # ---------------------------------------------------------------------------
    # 5. If we reach here, file has no UTF-8 BOM (nothing to do)
    # ---------------------------------------------------------------------------
    exit 0
}
catch {
    [Console]::Error.WriteLine("ERROR: Could not process file: $FilePath")
    [Console]::Error.WriteLine("       $($_.Exception.Message)")
    exit 2
}
