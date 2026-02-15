---
name: xlsx
description: Create and edit spreadsheet files (.xlsx, .xlsm, .csv, .tsv). Use this skill when the user asks to create, modify, or analyze Excel spreadsheets, CSV files, or tabular data. Handles financial models, data analysis, formatting, and formula creation.
license: Complete terms in LICENSE.txt
---

# Excel/Spreadsheet Processing

This skill handles creation and modification of spreadsheet files including .xlsx, .xlsm, .csv, and .tsv formats.

## Output Requirements

All spreadsheets must:
- Use professional fonts (Calibri, Arial, or similar)
- Have zero formula errors (#REF!, #VALUE!, #NAME?, etc.)
- Include proper number formatting
- Be well-organized with clear headers

## Color Conventions (Financial Models)

Follow industry-standard color coding:
- **Blue text**: Input cells (hardcoded values)
- **Black text**: Formulas and calculations
- **Green text**: Links to other sheets/files
- **Red text**: Error checks or warnings

## Number Formatting Rules

- **Currency**: Use accounting format with 2 decimals
- **Percentages**: Display as % with appropriate decimals
- **Dates**: Use consistent date format (YYYY-MM-DD or regional)
- **Large numbers**: Use thousands separators

## Workflow: Reading Excel Files

```python
import pandas as pd

# Read specific sheet
df = pd.read_excel('file.xlsx', sheet_name='Sheet1')

# Read all sheets
all_sheets = pd.read_excel('file.xlsx', sheet_name=None)
```

## Workflow: Creating Excel Files

```python
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment

# Create workbook
wb = Workbook()
ws = wb.active

# Add data
ws['A1'] = 'Header'
ws['A1'].font = Font(bold=True)

# Save
wb.save('output.xlsx')
```

## Workflow: Editing Existing Files

```python
from openpyxl import load_workbook

wb = load_workbook('existing.xlsx')
ws = wb.active

# Modify cells
ws['B2'] = 'New Value'

# Add formula
ws['C2'] = '=A2+B2'

wb.save('existing.xlsx')
```

## Formula Best Practices

**Always use Excel formulas over hardcoded values when possible:**
- Use `=SUM()`, `=AVERAGE()`, `=VLOOKUP()`, etc.
- Reference cells instead of typing values
- Use named ranges for clarity

**After adding formulas, recalculate:**
```bash
python scripts/recalc.py file.xlsx
```

## Common Operations

### Conditional Formatting
```python
from openpyxl.formatting.rule import ColorScaleRule

rule = ColorScaleRule(
    start_type='min', start_color='FF0000',
    end_type='max', end_color='00FF00'
)
ws.conditional_formatting.add('A1:A10', rule)
```

### Data Validation
```python
from openpyxl.worksheet.datavalidation import DataValidation

dv = DataValidation(type="list", formula1='"Yes,No,Maybe"')
ws.add_data_validation(dv)
dv.add('B2:B100')
```

### Charts
```python
from openpyxl.chart import BarChart, Reference

chart = BarChart()
data = Reference(ws, min_col=2, min_row=1, max_col=3, max_row=10)
chart.add_data(data, titles_from_data=True)
ws.add_chart(chart, 'E1')
```

## CSV/TSV Handling

```python
import pandas as pd

# Read
df = pd.read_csv('file.csv')
df = pd.read_csv('file.tsv', sep='\t')

# Write
df.to_csv('output.csv', index=False)
df.to_csv('output.tsv', sep='\t', index=False)
```

## Error Handling

Always validate data before processing:
```python
# Check for empty values
if df.isnull().any().any():
    print("Warning: Contains empty cells")

# Verify numeric columns
df['Amount'] = pd.to_numeric(df['Amount'], errors='coerce')
```
