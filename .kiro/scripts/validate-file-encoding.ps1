#!/usr/bin/env pwsh
<#
.SYNOPSIS
Validates that a file is UTF-8 without BOM.

.DESCRIPTION
Inspects the actual bytes of a file to verify:
- No BOM (Byte Order Mark) is present at the start of the file
- No UTF-16, UTF-32, or other non-UTF-8 encoding signatures are detected
- All remaining bytes form valid UTF-8 sequences

This script reads raw bytes from disk. It does NOT rely on PowerShell's
default text encoding or on how any editor displays the file.

.PARAMETER FilePath
The path to the file to validate.

.NOTES
Exit codes:
  0 = Valid (UTF-8 without BOM)
  1 = Invalid encoding detected (error reported to stderr)
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
    # 3. Empty file is valid UTF-8 without BOM
    # ---------------------------------------------------------------------------
    if ($allBytes.Length -eq 0) {
        exit 0
    }

    # ---------------------------------------------------------------------------
    # 4. BOM / encoding-signature detection (checked against first 4 bytes)
    # ---------------------------------------------------------------------------
    $b0 = $allBytes[0]
    $b1 = if ($allBytes.Length -gt 1) { $allBytes[1] } else { -1 }
    $b2 = if ($allBytes.Length -gt 2) { $allBytes[2] } else { -1 }
    $b3 = if ($allBytes.Length -gt 3) { $allBytes[3] } else { -1 }

    # UTF-32 BE: 00 00 FE FF  (must be checked before UTF-16 BE)
    if ($b0 -eq 0x00 -and $b1 -eq 0x00 -and $b2 -eq 0xFE -and $b3 -eq 0xFF) {
        [Console]::Error.WriteLine("ERROR: Invalid encoding in file: $FilePath")
        [Console]::Error.WriteLine("       Detected : UTF-32 BE (BOM: 00 00 FE FF)")
        [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
        exit 1
    }

    # UTF-32 LE: FF FE 00 00  (must be checked before UTF-16 LE)
    if ($b0 -eq 0xFF -and $b1 -eq 0xFE -and $b2 -eq 0x00 -and $b3 -eq 0x00) {
        [Console]::Error.WriteLine("ERROR: Invalid encoding in file: $FilePath")
        [Console]::Error.WriteLine("       Detected : UTF-32 LE (BOM: FF FE 00 00)")
        [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
        exit 1
    }

    # UTF-16 BE: FE FF
    if ($b0 -eq 0xFE -and $b1 -eq 0xFF) {
        [Console]::Error.WriteLine("ERROR: Invalid encoding in file: $FilePath")
        [Console]::Error.WriteLine("       Detected : UTF-16 BE (BOM: FE FF)")
        [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
        exit 1
    }

    # UTF-16 LE: FF FE
    if ($b0 -eq 0xFF -and $b1 -eq 0xFE) {
        [Console]::Error.WriteLine("ERROR: Invalid encoding in file: $FilePath")
        [Console]::Error.WriteLine("       Detected : UTF-16 LE (BOM: FF FE)")
        [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
        exit 1
    }

    # UTF-8 with BOM: EF BB BF
    if ($b0 -eq 0xEF -and $b1 -eq 0xBB -and $b2 -eq 0xBF) {
        [Console]::Error.WriteLine("ERROR: Invalid encoding in file: $FilePath")
        [Console]::Error.WriteLine("       Detected : UTF-8 with BOM (BOM: EF BB BF)")
        [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
        exit 1
    }

    # ---------------------------------------------------------------------------
    # 5. Strict UTF-8 byte-sequence validation
    #
    #    Walk every byte and verify that multi-byte sequences follow the
    #    UTF-8 specification exactly:
    #
    #      1-byte  (U+0000..U+007F):  0xxxxxxx
    #      2-byte  (U+0080..U+07FF):  110xxxxx 10xxxxxx
    #      3-byte  (U+0800..U+FFFF):  1110xxxx 10xxxxxx 10xxxxxx
    #      4-byte  (U+10000..U+10FFFF): 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
    #
    #    Additionally rejected:
    #      - Overlong encodings
    #      - Surrogates (U+D800..U+DFFF)
    #      - Codepoints above U+10FFFF
    #      - Continuation byte without a leading byte
    #      - Leading byte without enough continuation bytes
    # ---------------------------------------------------------------------------
    $i = 0
    $len = $allBytes.Length

    while ($i -lt $len) {
        $byte = $allBytes[$i]

        if ($byte -le 0x7F) {
            # Single-byte sequence (ASCII): always valid
            $i++
            continue
        }

        # Determine expected sequence length from the leading byte
        if (($byte -band 0xE0) -eq 0xC0) {
            # 2-byte sequence: leading byte 110xxxxx
            $seqLen = 2
            $minCodepoint = 0x80   # overlong guard: must be >= U+0080
        }
        elseif (($byte -band 0xF0) -eq 0xE0) {
            # 3-byte sequence: leading byte 1110xxxx
            $seqLen = 3
            $minCodepoint = 0x800  # overlong guard: must be >= U+0800
        }
        elseif (($byte -band 0xF8) -eq 0xF0) {
            # 4-byte sequence: leading byte 11110xxx
            $seqLen = 4
            $minCodepoint = 0x10000 # overlong guard: must be >= U+10000
        }
        else {
            # 0x80..0xBF: unexpected continuation byte
            # 0xF8..0xFF: invalid leading bytes in UTF-8
            [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
            [Console]::Error.WriteLine("       Invalid byte 0x$("{0:X2}" -f $byte) at offset $i")
            [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
            exit 1
        }

        # Check we have enough bytes remaining
        if (($i + $seqLen) -gt $len) {
            [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
            [Console]::Error.WriteLine("       Truncated multi-byte sequence at offset $i (need $seqLen bytes)")
            [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
            exit 1
        }

        # Validate each continuation byte (10xxxxxx)
        for ($j = 1; $j -lt $seqLen; $j++) {
            $cont = $allBytes[$i + $j]
            if (($cont -band 0xC0) -ne 0x80) {
                [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
                [Console]::Error.WriteLine("       Invalid continuation byte 0x$("{0:X2}" -f $cont) at offset $($i + $j)")
                [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
                exit 1
            }
        }

        # Decode the codepoint to check for overlong encodings and surrogates
        $codepoint = 0
        switch ($seqLen) {
            2 { $codepoint = (($allBytes[$i] -band 0x1F) -shl 6)  -bor ($allBytes[$i+1] -band 0x3F) }
            3 { $codepoint = (($allBytes[$i] -band 0x0F) -shl 12) -bor (($allBytes[$i+1] -band 0x3F) -shl 6) -bor ($allBytes[$i+2] -band 0x3F) }
            4 { $codepoint = (($allBytes[$i] -band 0x07) -shl 18) -bor (($allBytes[$i+1] -band 0x3F) -shl 12) -bor (($allBytes[$i+2] -band 0x3F) -shl 6) -bor ($allBytes[$i+3] -band 0x3F) }
        }

        # Overlong encoding
        if ($codepoint -lt $minCodepoint) {
            [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
            [Console]::Error.WriteLine("       Overlong encoding at offset $i (codepoint U+$("{0:X4}" -f $codepoint) encoded in $seqLen bytes)")
            [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
            exit 1
        }

        # Surrogate codepoints U+D800..U+DFFF are illegal in UTF-8
        if ($codepoint -ge 0xD800 -and $codepoint -le 0xDFFF) {
            [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
            [Console]::Error.WriteLine("       Surrogate codepoint U+$("{0:X4}" -f $codepoint) at offset $i is not valid in UTF-8")
            [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
            exit 1
        }

        # Codepoints above U+10FFFF are outside Unicode range
        if ($codepoint -gt 0x10FFFF) {
            [Console]::Error.WriteLine("ERROR: File is not valid UTF-8: $FilePath")
            [Console]::Error.WriteLine("       Codepoint U+$("{0:X4}" -f $codepoint) at offset $i exceeds maximum Unicode value (U+10FFFF)")
            [Console]::Error.WriteLine("       Required : UTF-8 without BOM")
            exit 1
        }

        $i += $seqLen
    }

    # All bytes validated successfully
    exit 0
}
catch {
    [Console]::Error.WriteLine("ERROR: Could not read file: $FilePath")
    [Console]::Error.WriteLine("       $($_.Exception.Message)")
    exit 2
}
