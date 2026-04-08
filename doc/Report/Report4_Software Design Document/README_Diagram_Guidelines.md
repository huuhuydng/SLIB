# Report 4 Diagram Guidelines

This file records the diagram conventions to be used from this point onward in Report 4.

## Sequence Diagram Rules

- Create one Mermaid sequence diagram for each FE.
- Keep message numbering explicit and continuous.
- When using `alt`, continue numbering correctly inside and after each branch.
- Use the actor mapping in [README_Feature_Breakdown.md](/C:/FPT%20University/SP26/SEP490/CODE%20PHI%E1%BB%80N%20VCL/SLIB/doc/Report/Report4_Software%20Design%20Document/README_Feature_Breakdown.md) as the source of truth for who participates in each use case.
- For the next modules, base sequence diagrams and use case reasoning on the actor assignments already approved in the feature breakdown.
- For CRUD-style features or management flows in HCE and NFC, split the feature into subcases using suffixes such as `a`, `b`, `c`.
- Example naming style:
  - `FE-48a_View and update NFC Tag mapping`
  - `FE-48b_Create NFC Tag mapping`
  - `FE-48c_Delete NFC Tag mapping`
- Prefer lifelines that reflect the real architecture flow, for example:
  - User/UI
  - Frontend or Mobile
  - Backend API Controller
  - Service
  - Repository
  - Database or external service

## Class Diagram Rules

- Create one class diagram for each major module.
- Total target: 12 class diagrams for the 12 major modules.
- Place each class diagram inside the corresponding module folder.
- Keep class diagrams focused on the domain and service relationships relevant to that module.

## Folder Usage

- Use each module folder as the main workspace for that module.
- If a module contains smaller management groups, place related files inside the corresponding subfolder.
- Store Mermaid source files in Markdown files unless another format is requested later.
