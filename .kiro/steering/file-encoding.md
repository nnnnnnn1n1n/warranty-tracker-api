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

## Requirements

- **Create files with UTF-8 without BOM**: All new files must be written in UTF-8 without BOM encoding
- **Modify files with UTF-8 without BOM**: When modifying existing files, preserve or convert to UTF-8 without BOM
- **Avoid incorrect encodings**: Do NOT use UTF-8 with BOM, UTF-16, or other encodings
- **PowerShell and command-line tools**: When using PowerShell or other file-writing commands, explicitly ensure the output is UTF-8 without BOM

## Verification

Files created by Kiro should not have a BOM prefix (bytes EF BB BF at file start). Text editors and IDEs display file encoding in their settings, allowing verification of correct encoding.
