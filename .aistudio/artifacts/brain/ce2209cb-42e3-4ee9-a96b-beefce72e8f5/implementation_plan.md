# Save State Management System & Classic NES Retro Arcade Architecture

This plan transitions the emulator from the temporary PS4 theme to an **Authentic Classic NES Retro Arcade theme** (charcoal, crimson, gold, authentic NES controller) and introduces a robust, local-storage **Save State Management System** with multi-slot visual browsing, timestamps, screenshot previews, and slot management.

---

## 1. Architectural Changes

### A. Delete & Replace PS4 Theme
- **Eliminate PS4 components**: Deprecate `com.example.nes.ui.ps4` (`Ps4Controls`, `Ps4Symbols`, `Ps4Wave`, `Ps4QuickMenu`).
- **Retro Arcade Palette (`Theme.kt` & `Color.kt`)**:
  - `NesCharcoal` (`#1A1A24`): Vintage console chassis dark base
  - `NesConsoleGray` (`#383A48` / `#505364`): Classic NES front-loader brushed plastic
  - `NesButtonRed` (`#D90429` / `#EF233C`): Iconic NES A / B controller button red
  - `NesGold` (`#FFD166`): Retro cartridge seal and golden highlighting
  - `NesRubberGray` (`#1F2022`): SELECT / START rubber pill buttons
- **Authentic NES Virtual Controller**:
  - Cross directional pad (D-Pad) with tactile central pivot
  - Angled red A and B action buttons
  - Dedicated rapid-fire Turbo A and Turbo B buttons
  - Indented SELECT and START rubber pill buttons
  - Quick Save, Quick Load, and State Manager shortcuts
- **Classic Arcade Game Library**:
  - Retro NES Cartridge shelf view with box art, mapper badge, and quick play
  - Persistent watermark `NES emulator By ArDev`

---

## 2. Save State Management System with Local Storage

### A. Room Database Integration & Persistence
- **Entity (`SaveStateEntity`)**:
  - `id`: Auto-generated Primary Key
  - `romId`: Reference to target ROM
  - `slotIndex`: Slot 1 to 10
  - `slotName`: Name or label (e.g. "World 1-2 Boss", "Dungeon Entrance")
  - `timestamp`: Epoch milliseconds for human-readable time ("2026-09-30 02:15 PM")
  - `filePath`: Absolute path in `context.filesDir/savestates/` storing binary hardware snapshot
  - `thumbnailBase64`: Captured RGB preview frame from PPU
- **DAO (`SaveStateDao`)**:
  - `getStatesForRom(romId: Long): Flow<List<SaveStateEntity>>`
  - `getStateBySlot(romId: Long, slotIndex: Int): SaveStateEntity?`
  - `insert(state: SaveStateEntity)`
  - `delete(state: SaveStateEntity)`

### B. State Manager Dialog UI (`SaveStateManagerDialog.kt`)
- Accessible both in-game (via top toolbar or Pause) and from the library.
- Multi-slot grid / list with:
  - Mini snapshot preview / screenshot
  - Slot number and timestamp
  - **Save State** (captures CPU, PPU, APU, RAM, and Mapper state)
  - **Load State** (restores emulator in-place within 1 frame)
  - **Delete State** (with confirmation dialog)
- Watermark displayed in header and footer: `NES emulator By ArDev`.

---

## 3. Implementation Steps

1. **Database & Room Migration**:
   - Add `SaveStateEntity` and `SaveStateDao` into Room Database (`NesDatabase.kt`).
   - Add save/load/delete state operations in `NesRepository.kt` and `EmulatorViewModel.kt`.
2. **Retro Theme & Controller**:
   - Update `Color.kt` and `Theme.kt` with Classic NES Retro Arcade colors.
   - Build `NesController.kt` (authentic NES D-Pad, A/B/Turbo, Select, Start).
   - Build `SaveStateManagerDialog.kt` with multi-slot management, preview images, and delete actions.
3. **Screen Updates**:
   - Refactor `EmulatorScreen.kt` to use the authentic NES controller and state manager dialog.
   - Refactor `LibraryScreen.kt` to classic NES cartridge shelf layout with state management access.
   - Remove unused PS4 theme files from the project.
4. **Verification**:
   - Verify complete compilation via `compile_applet`.
   - Run unit tests to ensure Mappers, CPU, and Save/Load serialization are 100% stable.
