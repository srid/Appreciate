using System;
using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Effects;

namespace Appreciate
{
    /// <summary>
    /// Transparent, click-through, topmost overlay window with randomized styling.
    /// SYNC: Animation kinds and color palette must match macOS and Android.
    /// </summary>
    public partial class OverlayWindow : Window
    {
        // Win32 interop for click-through
        private const int GWL_EXSTYLE = -20;
        private const int WS_EX_TRANSPARENT = 0x00000020;
        private const int WS_EX_TOOLWINDOW = 0x00000080;

        [DllImport("user32.dll")]
        private static extern int GetWindowLong(IntPtr hwnd, int index);

        [DllImport("user32.dll")]
        private static extern int SetWindowLong(IntPtr hwnd, int index, int newStyle);

        // SYNC: Animation kinds must match macos/Sources/OverlayManager.swift and android/.../OverlayViewFactory.kt
        private enum AnimationKind
        {
            Fade, SlideTop, SlideBottom, SlideLeft, SlideRight, ScaleUp, BlurFade
        }

        // SYNC: Color palette must match macos/Sources/OverlayManager.swift and android/.../OverlayViewFactory.kt
        private static readonly (float H, float S, float V)[] VibrantColors =
        {
            (0.05f, 0.85f, 0.95f), (0.08f, 0.90f, 1.0f), (0.12f, 0.80f, 1.0f),
            (0.95f, 0.70f, 1.0f), (0.55f, 0.75f, 0.95f), (0.50f, 0.60f, 1.0f),
            (0.45f, 0.70f, 0.90f), (0.75f, 0.60f, 0.95f), (0.30f, 0.80f, 0.90f),
            (0.85f, 0.65f, 1.0f), (0.65f, 0.70f, 1.0f), (0.15f, 0.90f, 1.0f),
            (0.35f, 0.95f, 1.0f), (0.80f, 0.80f, 1.0f), (0.00f, 0.80f, 1.0f),
            (0.60f, 0.35f, 1.0f), (0.85f, 0.30f, 1.0f), (0.40f, 0.35f, 0.95f),
        };

        private static readonly string[] FontFamilies = { "Segoe UI", "Georgia", "Consolas", "Calibri" };
        private static readonly AnimationKind[] AnimationKinds = (AnimationKind[])Enum.GetValues(typeof(AnimationKind));

        private readonly float _displayDuration;

        public OverlayWindow(string text, float displayDuration, double screenLeft, double screenTop, double screenWidth, double screenHeight)
        {
            InitializeComponent();

            _displayDuration = displayDuration;

            // Position to fill the given screen
            Left = screenLeft;
            Top = screenTop;
            Width = screenWidth;
            Height = screenHeight;

            ApplyRandomStyle(text);
            Loaded += OnLoaded;
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            // Make click-through
            var hwnd = new WindowInteropHelper(this).Handle;
            var exStyle = GetWindowLong(hwnd, GWL_EXSTYLE);
            SetWindowLong(hwnd, GWL_EXSTYLE, exStyle | WS_EX_TRANSPARENT | WS_EX_TOOLWINDOW);

            AnimateIn();
        }

        private void ApplyRandomStyle(string text)
        {
            var rng = Random.Shared;
            var hsv = VibrantColors[rng.Next(VibrantColors.Length)];
            var color = HsvToColor(hsv.H, hsv.S, hsv.V);
            var brush = new SolidColorBrush(color);

            var fontSize = 36 + rng.NextDouble() * 36;
            var fontFamily = new FontFamily(FontFamilies[rng.Next(FontFamilies.Length)]);
            var fontWeight = rng.NextDouble() < 0.5 ? FontWeights.Bold : FontWeights.Normal;
            var rotation = -3 + rng.NextDouble() * 6;
            var showPill = rng.NextDouble() < 0.5;

            // Shadow
            var shadow = new DropShadowEffect
            {
                Color = Colors.Black,
                BlurRadius = 16,
                ShadowDepth = 2,
                Opacity = 0.6
            };

            // Position offset
            var xOffset = (-0.25 + rng.NextDouble() * 0.5) * Width;
            var yOffset = (-0.25 + rng.NextDouble() * 0.5) * Height;

            var transform = new TransformGroup();
            transform.Children.Add(new RotateTransform(rotation));
            transform.Children.Add(new TranslateTransform(xOffset, yOffset));

            if (showPill)
            {
                ReminderText.Text = text;
                ReminderText.FontSize = fontSize;
                ReminderText.FontFamily = fontFamily;
                ReminderText.FontWeight = fontWeight;
                ReminderText.Foreground = brush;
                ReminderText.Effect = shadow;

                PillBorder.Background = rng.NextDouble() < 0.5
                    ? new SolidColorBrush(Color.FromArgb(100, 0, 0, 0))
                    : new SolidColorBrush(Color.FromArgb(40, 255, 255, 255));
                PillBorder.RenderTransform = transform;
                PillBorder.RenderTransformOrigin = new Point(0.5, 0.5);
                PillBorder.Visibility = Visibility.Visible;

                ReminderTextNoPill.Visibility = Visibility.Collapsed;
            }
            else
            {
                ReminderTextNoPill.Text = text;
                ReminderTextNoPill.FontSize = fontSize;
                ReminderTextNoPill.FontFamily = fontFamily;
                ReminderTextNoPill.FontWeight = fontWeight;
                ReminderTextNoPill.Foreground = brush;
                ReminderTextNoPill.Effect = shadow;
                ReminderTextNoPill.RenderTransform = transform;
                ReminderTextNoPill.RenderTransformOrigin = new Point(0.5, 0.5);

                PillBorder.Visibility = Visibility.Collapsed;
                ReminderTextNoPill.Visibility = Visibility.Visible;
            }
        }

        private void AnimateIn()
        {
            var rng = Random.Shared;
            var kind = AnimationKinds[rng.Next(AnimationKinds.Length)];
            var target = PillBorder.Visibility == Visibility.Visible
                ? (FrameworkElement)PillBorder
                : ReminderTextNoPill;
            var scaleBreathing = rng.NextDouble() < 0.5;
            var targetScale = 1.0 + rng.NextDouble() * 0.08;

            // Fade in
            var fadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(600));
            target.Opacity = 0;

            switch (kind)
            {
                case AnimationKind.SlideTop:
                    AddSlideAnimation(target, 0, -400, 0, 0, fadeIn);
                    break;
                case AnimationKind.SlideBottom:
                    AddSlideAnimation(target, 0, 400, 0, 0, fadeIn);
                    break;
                case AnimationKind.SlideLeft:
                    AddSlideAnimation(target, -600, 0, 0, 0, fadeIn);
                    break;
                case AnimationKind.SlideRight:
                    AddSlideAnimation(target, 600, 0, 0, 0, fadeIn);
                    break;
                case AnimationKind.ScaleUp:
                    AddScaleAnimation(target, 0.3, 1.0, fadeIn);
                    break;
                default: // Fade, BlurFade
                    target.BeginAnimation(OpacityProperty, fadeIn);
                    break;
            }

            // Schedule breathing + exit
            var holdMs = Math.Max(0, (_displayDuration - 2f)) * 1000;
            var dispatcherTimer = new System.Windows.Threading.DispatcherTimer();
            dispatcherTimer.Interval = TimeSpan.FromMilliseconds(600 + holdMs);
            dispatcherTimer.Tick += (_, _) =>
            {
                dispatcherTimer.Stop();
                AnimateOut(target);
            };
            dispatcherTimer.Start();

            // Scale breathing
            if (scaleBreathing)
            {
                var scaleTransform = new ScaleTransform(1, 1);
                if (target.RenderTransform is TransformGroup group)
                {
                    group.Children.Add(scaleTransform);
                }
                else
                {
                    var newGroup = new TransformGroup();
                    if (target.RenderTransform != null)
                        newGroup.Children.Add(target.RenderTransform);
                    newGroup.Children.Add(scaleTransform);
                    target.RenderTransform = newGroup;
                    target.RenderTransformOrigin = new Point(0.5, 0.5);
                }

                var breathe = new DoubleAnimation(1, targetScale, TimeSpan.FromMilliseconds(1500))
                {
                    AutoReverse = true,
                    RepeatBehavior = RepeatBehavior.Forever,
                    EasingFunction = new SineEase()
                };
                scaleTransform.BeginAnimation(ScaleTransform.ScaleXProperty, breathe);
                scaleTransform.BeginAnimation(ScaleTransform.ScaleYProperty, breathe);
            }
        }

        private void AnimateOut(FrameworkElement target)
        {
            var fadeOut = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(1500));
            fadeOut.Completed += (_, _) => Close();
            target.BeginAnimation(OpacityProperty, fadeOut);
        }

        private void AddSlideAnimation(FrameworkElement target, double fromX, double fromY, double toX, double toY, DoubleAnimation fadeIn)
        {
            var translate = new TranslateTransform(fromX, fromY);
            if (target.RenderTransform is TransformGroup group)
            {
                // Replace the existing TranslateTransform
                for (int i = 0; i < group.Children.Count; i++)
                {
                    if (group.Children[i] is TranslateTransform)
                    {
                        var existing = (TranslateTransform)group.Children[i];
                        toX += existing.X;
                        toY += existing.Y;
                        fromX += existing.X;
                        fromY += existing.Y;
                        translate = new TranslateTransform(fromX, fromY);
                        group.Children[i] = translate;
                        break;
                    }
                }
            }

            var animX = new DoubleAnimation(fromX, toX, TimeSpan.FromMilliseconds(600))
            {
                EasingFunction = new QuarticEase { EasingMode = EasingMode.EaseOut }
            };
            var animY = new DoubleAnimation(fromY, toY, TimeSpan.FromMilliseconds(600))
            {
                EasingFunction = new QuarticEase { EasingMode = EasingMode.EaseOut }
            };

            translate.BeginAnimation(TranslateTransform.XProperty, animX);
            translate.BeginAnimation(TranslateTransform.YProperty, animY);
            target.BeginAnimation(OpacityProperty, fadeIn);
        }

        private void AddScaleAnimation(FrameworkElement target, double from, double to, DoubleAnimation fadeIn)
        {
            var scaleTransform = new ScaleTransform(from, from);
            if (target.RenderTransform is TransformGroup group)
            {
                group.Children.Add(scaleTransform);
            }
            else
            {
                target.RenderTransform = scaleTransform;
                target.RenderTransformOrigin = new Point(0.5, 0.5);
            }

            var animScale = new DoubleAnimation(from, to, TimeSpan.FromMilliseconds(600))
            {
                EasingFunction = new BackEase { Amplitude = 0.3, EasingMode = EasingMode.EaseOut }
            };

            scaleTransform.BeginAnimation(ScaleTransform.ScaleXProperty, animScale);
            scaleTransform.BeginAnimation(ScaleTransform.ScaleYProperty, animScale);
            target.BeginAnimation(OpacityProperty, fadeIn);
        }

        private static Color HsvToColor(float h, float s, float v)
        {
            float hDeg = h * 360f;
            int hi = (int)(hDeg / 60) % 6;
            float f = hDeg / 60 - (int)(hDeg / 60);
            float p = v * (1 - s);
            float q = v * (1 - f * s);
            float t = v * (1 - (1 - f) * s);

            float r, g, b;
            switch (hi)
            {
                case 0: r = v; g = t; b = p; break;
                case 1: r = q; g = v; b = p; break;
                case 2: r = p; g = v; b = t; break;
                case 3: r = p; g = q; b = v; break;
                case 4: r = t; g = p; b = v; break;
                default: r = v; g = p; b = q; break;
            }

            return Color.FromRgb((byte)(r * 255), (byte)(g * 255), (byte)(b * 255));
        }
    }
}
