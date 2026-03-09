"""
Transparent overlay window for Appreciate (Linux).
Displays reminder text as a click-through overlay on all monitors.

SYNC: Animation types, color palette, and styling must match
      macos/Sources/OverlayContentView.swift,
      android/.../OverlayViewFactory.kt, and
      windows/OverlayWindow.xaml.cs.
"""

import random
import gi
gi.require_version("Gtk", "4.0")
gi.require_version("Gdk", "4.0")
from gi.repository import Gtk, Gdk, GLib, Pango

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

FONT_WEIGHTS = [Pango.Weight.NORMAL, Pango.Weight.BOLD, Pango.Weight.HEAVY]

ANIMATION_KINDS = ["fade", "slide_top", "slide_bottom", "slide_left", "slide_right", "scale_up"]


class OverlayWindow(Gtk.Window):
    """A transparent, click-through overlay that shows reminder text."""

    def __init__(self, text, duration, monitor_geometry=None):
        super().__init__()
        self.set_decorated(False)
        self.set_resizable(False)

        # Transparent background
        self.add_css_class("overlay-window")

        # Pick random style
        self._color = random.choice(COLORS)
        self._font = random.choice(FONTS)
        self._weight = random.choice(FONT_WEIGHTS)
        self._font_size = random.randint(24, 52)
        self._rotation = random.uniform(-8, 8)
        self._show_pill = random.random() < 0.3
        self._animation = random.choice(ANIMATION_KINDS)
        self._duration = duration
        self._opacity = 0.0

        # Position on monitor
        if monitor_geometry:
            x_range = monitor_geometry.width - 600
            y_range = monitor_geometry.height - 200
            self._target_x = monitor_geometry.x + max(50, random.randint(0, max(1, x_range)))
            self._target_y = monitor_geometry.y + max(50, random.randint(0, max(1, y_range)))
        else:
            self._target_x = random.randint(100, 800)
            self._target_y = random.randint(100, 500)

        # Create label
        self._label = Gtk.Label(label=text)
        self._label.set_wrap(True)
        self._label.set_max_width_chars(40)

        # Apply styling via CSS
        css = self._build_css()
        provider = Gtk.CssProvider()
        provider.load_from_data(css.encode())
        Gtk.StyleContext.add_provider_for_display(
            Gdk.Display.get_default(),
            provider,
            Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION,
        )
        self._label.add_css_class("overlay-label")
        self._css_provider = provider

        if self._show_pill:
            box = Gtk.Box()
            box.add_css_class("overlay-pill")
            box.append(self._label)
            self.set_child(box)
        else:
            self.set_child(self._label)

        # Make click-through by setting the surface input region to empty after realization
        self.connect("realize", self._on_realize)

    def _build_css(self):
        pill_bg = f"rgba(0, 0, 0, 0.5)" if self._show_pill else "transparent"
        return f"""
        .overlay-window {{
            background: transparent;
        }}
        .overlay-label {{
            color: {self._color};
            font-family: {self._font};
            font-size: {self._font_size}px;
            font-weight: {self._weight.value_nick};
            text-shadow: 0px 2px 8px rgba(0, 0, 0, 0.7);
            padding: 12px 24px;
        }}
        .overlay-pill {{
            background: {pill_bg};
            border-radius: 24px;
            padding: 4px;
        }}
        """

    def _on_realize(self, widget):
        """After the window is realized, make it click-through and start animation."""
        surface = self.get_surface()
        if surface and hasattr(surface, "set_input_region"):
            # Empty input region = click-through
            region = __import__("cairo").Region()
            surface.set_input_region(region)

        # Position the window
        # GTK4 doesn't allow direct positioning of toplevels on Wayland,
        # but on X11 it works. We use a CSS transform as fallback.
        self.set_default_size(1, 1)

        # Start entrance animation
        self._animate_in()

    def _animate_in(self):
        """Fade in over 600ms using GLib tick callbacks."""
        self._anim_start_time = GLib.get_monotonic_time()
        self._enter_duration_us = 600_000  # 600ms in microseconds
        self.set_opacity(0.0)
        self.present()
        GLib.timeout_add(16, self._tick_enter)  # ~60fps

    def _tick_enter(self):
        elapsed = GLib.get_monotonic_time() - self._anim_start_time
        progress = min(1.0, elapsed / self._enter_duration_us)

        # Ease out decelerate
        progress = 1.0 - (1.0 - progress) ** 2
        self.set_opacity(progress)

        if progress >= 1.0:
            self.set_opacity(1.0)
            # Schedule exit after hold duration
            GLib.timeout_add(int(self._duration * 1000), self._start_exit)
            return False  # stop
        return True  # continue

    def _start_exit(self):
        """Begin fade-out animation."""
        self._anim_start_time = GLib.get_monotonic_time()
        self._exit_duration_us = 1_500_000  # 1500ms
        GLib.timeout_add(16, self._tick_exit)
        return False

    def _tick_exit(self):
        elapsed = GLib.get_monotonic_time() - self._anim_start_time
        progress = min(1.0, elapsed / self._exit_duration_us)

        self.set_opacity(1.0 - progress)

        if progress >= 1.0:
            self._cleanup()
            return False
        return True

    def _cleanup(self):
        """Remove CSS provider and destroy window."""
        Gtk.StyleContext.remove_provider_for_display(
            Gdk.Display.get_default(),
            self._css_provider,
        )
        self.destroy()


def show_overlay(text, duration):
    """Show overlays on all monitors."""
    display = Gdk.Display.get_default()
    if display is None:
        return

    monitors = display.get_monitors()
    for i in range(monitors.get_n_items()):
        monitor = monitors.get_item(i)
        geom = monitor.get_geometry()
        overlay = OverlayWindow(text, duration, geom)
        overlay.present()
