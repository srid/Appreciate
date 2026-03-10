"""
Transparent overlay window for Appreciate (Linux).
Displays reminder text as a click-through overlay on all monitors.

SYNC: Animation types, color palette, and styling must match
      macos/Sources/OverlayContentView.swift,
      macos/Sources/OverlayManager.swift,
      android/.../OverlayViewFactory.kt, and
      windows/OverlayWindow.xaml.cs.
"""

import random
import math
import gi
gi.require_version("Gtk", "4.0")
gi.require_version("Gdk", "4.0")
from gi.repository import Gtk, Gdk, GLib, Pango, Gsk, Graphene

# SYNC: HSV color palette — keep in sync across all platforms
COLORS = [
    "#FF6B6B", "#FF8E72", "#FFA07A", "#FFD700", "#FFEB3B",
    "#4ECDC4", "#45B7D1", "#5BF0A5", "#7C4DFF", "#B388FF",
    "#FF80AB", "#EA80FC", "#82B1FF", "#80D8FF", "#A7FFEB",
    "#F4FF81", "#FFD180", "#FF9E80",
]

FONTS = [
    "Sans", "Serif", "Monospace",
    "Ubuntu", "DejaVu Sans", "Liberation Sans",
    "Cantarell", "Noto Sans",
]

# SYNC: Font weights — match macOS .light,.regular,.medium,.semibold,.bold,.heavy
FONT_WEIGHTS = [
    Pango.Weight.LIGHT, Pango.Weight.NORMAL, Pango.Weight.MEDIUM,
    Pango.Weight.SEMIBOLD, Pango.Weight.BOLD, Pango.Weight.HEAVY,
]

# SYNC: Animation kinds — match macOS AnimationKind
ANIMATION_KINDS = [
    "fade", "slide_top", "slide_bottom", "slide_left", "slide_right",
    "scale_up",
]


class OverlayStyle:
    """Randomized style, computed once and shared across all monitors."""

    def __init__(self):
        self.color = random.choice(COLORS)
        self.font = random.choice(FONTS)
        self.weight = random.choice(FONT_WEIGHTS)
        self.font_size = random.randint(36, 72)       # SYNC: match macOS 36...72
        self.rotation = random.uniform(-3, 3)          # SYNC: match macOS ±3°
        self.shadow_opacity = random.uniform(0.4, 0.7) # SYNC: match macOS
        self.animation = random.choice(ANIMATION_KINDS)
        self.show_pill = random.random() < 0.3
        self.pill_dark = random.random() < 0.5         # SYNC: black or white pill
        self.pill_opacity = random.uniform(0.5, 0.85)


class OverlayWindow(Gtk.Window):
    """A fullscreen transparent, click-through overlay that shows reminder text."""

    def __init__(self, text, duration, style, monitor_geometry=None):
        super().__init__()
        self.set_decorated(False)
        self.set_resizable(False)
        self._style = style
        self._duration = duration
        self._css_provider = None
        self._content = None

        # SYNC: macOS uses ±15% of screen size as offset from center
        if monitor_geometry:
            self.set_default_size(monitor_geometry.width, monitor_geometry.height)
            cx = monitor_geometry.width // 2
            cy = monitor_geometry.height // 2
            margin_x = cx + int(monitor_geometry.width * random.uniform(-0.15, 0.15))
            margin_y = cy + int(monitor_geometry.height * random.uniform(-0.15, 0.15))
            # Clamp so text stays within screen bounds (10% padding from edges)
            pad_x = int(monitor_geometry.width * 0.1)
            pad_y = int(monitor_geometry.height * 0.1)
            margin_x = max(pad_x, min(margin_x, monitor_geometry.width - pad_x))
            margin_y = max(pad_y, min(margin_y, monitor_geometry.height - pad_y))
        else:
            self.set_default_size(1920, 1080)
            margin_x = random.randint(200, 1200)
            margin_y = random.randint(200, 800)

        # Apply CSS
        css = self._build_css()
        provider = Gtk.CssProvider()
        provider.load_from_data(css.encode())
        Gtk.StyleContext.add_provider_for_display(
            Gdk.Display.get_default(),
            provider,
            Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION,
        )
        self._css_provider = provider
        self.add_css_class("overlay-window")

        # Create label
        label = Gtk.Label(label=text)
        label.add_css_class("overlay-label")
        label.set_wrap(True)
        label.set_max_width_chars(40)

        # Wrap in container for pill background
        if style.show_pill:
            box = Gtk.Box()
            box.add_css_class("overlay-pill")
            box.append(label)
            content = box
        else:
            content = label

        self._content = content

        # Position text within fullscreen window using Gtk.Fixed
        container = Gtk.Fixed()
        container.put(content, margin_x, margin_y)
        self.set_child(container)

        # Click-through
        self.connect("realize", self._on_realize)

        # Track animation state
        self._slide_offset_x = 0.0
        self._slide_offset_y = 0.0
        self._current_scale = 1.0
        self._target_margin_x = margin_x
        self._target_margin_y = margin_y
        self._container = container

    def _build_css(self):
        s = self._style
        pill_color = "rgba(0, 0, 0, {:.2f})".format(s.pill_opacity) if s.pill_dark \
            else "rgba(255, 255, 255, {:.2f})".format(s.pill_opacity * 0.3)
        shadow2_opacity = max(0, s.shadow_opacity - 0.1)
        # Rotation in degrees for Gtk.Fixed transform
        return f"""
        .overlay-window {{
            background: transparent;
        }}
        .overlay-label {{
            color: {s.color};
            font-family: {s.font};
            font-size: {s.font_size}px;
            font-weight: {s.weight.value_nick};
            text-shadow: 2px 2px 8px rgba(0, 0, 0, {s.shadow_opacity:.2f}),
                         0px 0px 16px rgba(0, 0, 0, {shadow2_opacity:.2f});
            padding: 12px 24px;
        }}
        .overlay-pill {{
            background: {pill_color};
            border-radius: 20px;
            padding: 12px 20px;
        }}
        """

    def _on_realize(self, widget):
        """After the window is realized, make it click-through and start animation."""
        surface = self.get_surface()
        if surface and hasattr(surface, "set_input_region"):
            region = __import__("cairo").Region()
            surface.set_input_region(region)

        self._animate_in()

    def _animate_in(self):
        """Entrance animation over ~600ms with per-kind effects."""
        self._anim_start = GLib.get_monotonic_time()
        self._enter_us = 600_000
        self.set_opacity(0.0)

        kind = self._style.animation
        # SYNC: Initial offsets match macOS AnimatingOverlayView.animateIn()
        if kind == "slide_top":
            self._slide_offset_y = -200.0
        elif kind == "slide_bottom":
            self._slide_offset_y = 200.0
        elif kind == "slide_left":
            self._slide_offset_x = -400.0
        elif kind == "slide_right":
            self._slide_offset_x = 400.0
        elif kind == "scale_up":
            self._current_scale = 0.3

        self.present()
        self.fullscreen()
        GLib.timeout_add(16, self._tick_enter)

    def _tick_enter(self):
        elapsed = GLib.get_monotonic_time() - self._anim_start
        t = min(1.0, elapsed / self._enter_us)

        # SYNC: Spring-like ease (macOS uses .spring(response: 0.6, dampingFraction: 0.8))
        t_ease = 1.0 - (1.0 - t) ** 3

        self.set_opacity(t_ease)

        # Animate slide offset back to zero
        kind = self._style.animation
        if kind.startswith("slide_"):
            self._slide_offset_x *= (1.0 - t_ease)
            self._slide_offset_y *= (1.0 - t_ease)
            # Reposition content with slide offset
            new_x = self._target_margin_x + int(self._slide_offset_x)
            new_y = self._target_margin_y + int(self._slide_offset_y)
            self._container.move(self._content, new_x, new_y)
        elif kind == "scale_up":
            self._current_scale = 0.3 + (1.0 - 0.3) * t_ease

        # Apply rotation via widget transform
        self._apply_transform()

        if t >= 1.0:
            self.set_opacity(1.0)
            GLib.timeout_add(int(self._duration * 1000), self._start_exit)
            return False
        return True

    def _start_exit(self):
        """Begin fade-out animation."""
        self._anim_start = GLib.get_monotonic_time()
        self._exit_us = 1_500_000
        GLib.timeout_add(16, self._tick_exit)
        return False

    def _tick_exit(self):
        elapsed = GLib.get_monotonic_time() - self._anim_start
        t = min(1.0, elapsed / self._exit_us)

        self.set_opacity(1.0 - t)

        # SYNC: macOS scaleUp exit goes to 1.5x
        kind = self._style.animation
        if kind == "scale_up":
            self._current_scale = 1.0 + (0.5 * t)
            self._apply_transform()

        if t >= 1.0:
            self._cleanup()
            return False
        return True

    def _apply_transform(self):
        """Apply rotation and scale to the content widget."""
        try:
            rotation_rad = math.radians(self._style.rotation)
            transform = Gsk.Transform.new()
            transform = transform.scale(self._current_scale, self._current_scale)
            transform = transform.rotate(self._style.rotation)
            self._content.set_transform(transform)
        except Exception:
            pass  # Transform not supported, skip gracefully

    def _cleanup(self):
        """Remove CSS provider and destroy window."""
        if self in _active_overlays:
            _active_overlays.remove(self)
        if self._css_provider:
            Gtk.StyleContext.remove_provider_for_display(
                Gdk.Display.get_default(),
                self._css_provider,
            )
        self.destroy()


# Track active overlay windows so we can dismiss them
_active_overlays: list[OverlayWindow] = []


def _dismiss_all():
    """Dismiss any existing overlays (matches macOS OverlayManager.dismiss)."""
    for win in list(_active_overlays):
        win._cleanup()
    _active_overlays.clear()


def show_overlay(text, duration):
    """Show overlays on all monitors with shared randomized style."""
    _dismiss_all()

    display = Gdk.Display.get_default()
    if display is None:
        return

    # SYNC: Shared style across all monitors (matches macOS behavior)
    style = OverlayStyle()

    monitors = display.get_monitors()
    for i in range(monitors.get_n_items()):
        monitor = monitors.get_item(i)
        geom = monitor.get_geometry()
        overlay = OverlayWindow(text, duration, style, geom)
        _active_overlays.append(overlay)
        overlay.present()
