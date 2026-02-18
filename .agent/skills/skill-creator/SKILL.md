---
name: skill-creator
description: Guide for creating effective skills for Claude. Use this when the user wants to create a new skill, improve an existing skill, or understand skill design patterns. Helps structure skill content for maximum effectiveness while minimizing context window usage.
license: Complete terms in LICENSE.txt
---

# Skill Creator

This skill guides the creation of effective skills for Claude. A skill is a folder containing a `SKILL.md` file with YAML frontmatter and markdown instructions.

## Core Principles

1. **Conciseness**: Skills should be as short as possible while remaining effective. Every token counts against the context window.
2. **Appropriate Freedom**: Give Claude room to apply judgment rather than over-specifying.
3. **Clear Triggers**: The description field determines when the skill activates - make it specific.

## Anatomy of a Skill

```
my-skill/
  SKILL.md           # Required: Main instructions
  scripts/           # Optional: Helper scripts
  references/        # Optional: Examples, templates
  assets/            # Optional: Images, data files
```

### SKILL.md Structure

```yaml
---
name: skill-name
description: When and how this skill should be used
---

# Skill Title

Brief overview of what the skill does.

## Key Concepts
- Core ideas the skill relies on
- Important constraints or requirements

## Workflow
1. Step one
2. Step two

## Examples
...

## Common Pitfalls
...
```

## Design Patterns

### Progressive Disclosure
Don't load everything upfront. Use `--help` patterns for scripts, reference files only when needed.

```markdown
**Always run scripts with `--help` first** to see usage options.
Only read source files when customization is necessary.
```

### Decision Trees
Help Claude choose the right approach based on context:

```markdown
User task -> Is it X?
  ├─ Yes -> Approach A
  └─ No -> Approach B
```

### Bundled Scripts as Black Boxes
Complex logic should live in scripts, not in the SKILL.md:

```markdown
Use `scripts/helper.py` for complex processing.
Run with `--help` for options.
```

## Step-by-Step Creation Process

1. **Identify the Use Case**: What specific task does this skill address?
2. **Study Existing Examples**: Look at similar skills for patterns
3. **Draft Minimal SKILL.md**: Start with just the essentials
4. **Add Scripts if Needed**: Extract complexity into helper scripts
5. **Test and Iterate**: Try the skill, refine based on results
6. **Package**: Ensure all dependencies are included

## Best Practices

- **Be specific in descriptions**: "Create Excel spreadsheets" is better than "Work with files"
- **Use examples sparingly**: One good example beats three mediocre ones
- **Avoid redundancy**: Don't repeat what Claude already knows
- **Keep instructions actionable**: Focus on what to do, not background theory
- **Test edge cases**: Skills should handle unusual inputs gracefully

## Anti-Patterns to Avoid

- **Over-specification**: Don't try to cover every possible scenario
- **Excessive examples**: Context is precious, use it wisely
- **Embedding large files**: Reference external files instead
- **Generic descriptions**: These cause skills to activate incorrectly
