# TaskLine visual system

Direction: Apple-inspired hierarchy on Android: 34 sp bold large titles, 17 sp primary text, neutral grouped backgrounds, opaque content surfaces, circular task completion controls, and a floating navigation capsule. Blue identifies actions and selection. Glass-like translucency is reserved for navigation. Android typography, system pickers, back navigation, and accessible 48 dp touch targets remain supported. This is a Compose adaptation, not Apple�s native Liquid Glass renderer.

## Components

- `TaskLineTheme`: neutral light/dark color schemes, blue accents, and an explicit scalable type hierarchy.
- `GlassCard` / `glassBackdrop`: opaque grouped content and a neutral background; GlassPill provides the separate navigation surface.
- `OverviewCard`: compact active/due-today/done counts from current data.
- `TaskCard`: circular completion control, title, project/date metadata, optional description and status, and labeled actions.
- `NavigationGlyph`: scalable Canvas symbols without an additional icon dependency.
- `EmptyPanel`: consistent icon, title, and next-step copy.
- `TaskLinePreviews`: isolated light, dark, and 150% type previews with preview-only sample data.
- `TaskLineWidget`: native RemoteViews surface backed by the same task repository.

Task and project lists scroll; filter chips scroll horizontally; metadata/actions wrap. Visible action labels supplement color and glyphs. The task editor retains native date selection and explicit save/cancel actions.

## Pattern references

The distinction between project context and time-based focus follows the general organizational pattern described by [Things](https://culturedcode.com/things/support/articles/4001304/). Bounded date navigation draws on the planning pattern described by [Todoist](https://www.todoist.com/help/todoist/features/use-the-calendar-layout-in-todoist-lPHRQTu0o). No brand assets or screens were copied.

Widget integration follows [Android’s widget guidance](https://developer.android.com/develop/ui/views/appwidgets).

Apple-inspired material hierarchy follows [Apple’s Materials guidance](https://developer.apple.com/design/human-interface-guidelines/materials). Content cards use opaque surfaces; navigation uses an alpha-composited surface; the Android widget uses a translucent gradient drawable with 32 dp corners and separate day/night colors. Neither implementation claims real backdrop refraction.

