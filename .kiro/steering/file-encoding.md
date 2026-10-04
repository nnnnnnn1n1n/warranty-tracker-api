---
inclusion: always
name: file-encoding-rule
description: Project-wide file encoding rule for UTF-8 without BOM
---

# File Encoding Rule

**MANDATORY**: All text files created or modified by Kiro must use **UTF-8 without BOM** (Byte Order Mark).

## Scope

This rule applies to all text-based project files, including:
- Java source files (.java)
- Markdown (.md)
- YAML (.yml, .yaml)
- JSON (.json)
- SQL (.sql)
- Properties (.properties)
- OpenAPI/Swagger (.yaml, .json)
- All other text-based project files

## Mandatory Rules

### Creating and Modifying Files

- **Create files with UTF-8 without BOM**: All new files must be written in UTF-8 without BOM encoding
- **Modify files with UTF-8 without BOM**: When modifying existing files, preserve or convert to UTF-8 without BOM
- **Never write files as UTF-8 with BOM**: BOM (Byte Order Mark) is forbidden. The encoding MUST be UTF-8 without BOM, not UTF-8 with BOM.
- **Never use other Unicode encodings**: Do NOT use UTF-16, UTF-16 LE, UTF-16 BE, UTF-32, or any other Unicode encoding variant.
- **Do not rely on defaults**: Do not rely on the default encoding of PowerShell, shell commands, editors, libraries, or file-writing APIs. Explicitly specify UTF-8 without BOM.
- **Specify encoding programmatically**: When writing a file programmatically (via tools, APIs, or scripts), explicitly specify UTF-8 without BOM as the target encoding.

### Verification and Compliance

- **Verify after writing**: Before considering a file-writing operation complete, verify that the resulting file is actually UTF-8 without BOM.
- **Preserve existing encoding**: Do not convert an existing UTF-8 file to UTF-16, UTF-8 with BOM, or another encoding unless the task explicitly requires an encoding conversion.
- **Preserve existing content**: When modifying a file, preserve the existing content and encoding unless the task explicitly requires an encoding change.

### Handling Encoding Problems

- **Stop on detection**: If an encoding problem is detected, STOP and report the problem instead of repeatedly rewriting, restoring, compiling, or testing the file.
- **No workarounds**: Do not use Git restore, Gradle clean, compilation, or other unrelated operations as a workaround for an encoding problem unless explicitly requested by the user.

### Clarification

The required encoding is exactly **UTF-8 without BOM**. Do not interpret "UTF-8" as "UTF-8 with BOM".
