# Account Management Module

This folder contains the Report 4 diagrams for Module 2 - Account Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-05` View profile
  - `FE-06` Change basic profile
  - `FE-07` Change password
  - `FE-08` View barcode
  - `FE-09` View history of activities
  - `FE-10` View account setting
  - `FE-11` Turn on or turn off notification
  - `FE-12` Turn on or turn off AI suggestion
  - `FE-13` Turn on or turn off HCE feature
  - `FE-14` View booking restriction status by reputation
- Class diagram for the Account Management Module

## Actor Scope

- `FE-05` to `FE-07`: `Admin`, `Librarian`, `Student`, `Teacher`
- `FE-08` to `FE-14`: `Student`, `Teacher`

## Sequence Diagram Convention

- Step numbering is written explicitly in each message.
- `alt` branches are used for different outcomes or platform-specific flows.
- The `Users` column follows the Module 1 convention for `mermaid.ai` rendering.
- Numbering continues correctly after each `alt` block.
