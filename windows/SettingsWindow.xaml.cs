using System.Linq;
using System.Windows;
using System.Windows.Controls;

namespace Appreciate
{
    public partial class SettingsWindow : Window
    {
        public event System.Action? ShowNowRequested;

        public SettingsWindow()
        {
            InitializeComponent();
            LoadSettings();
        }

        private void LoadSettings()
        {
            var s = SettingsStore.Instance;
            ReminderTextBox.Text = s.ReminderText;
            EnabledCheckBox.IsChecked = s.IsEnabled;
            LaunchAtLoginCheckBox.IsChecked = s.LaunchAtLogin;
            MinIntervalSlider.Value = s.MinIntervalMinutes;
            MaxIntervalSlider.Value = s.MaxIntervalMinutes;
            DurationSlider.Value = s.DisplayDurationSeconds;

            // Pack ComboBox
            RefreshPackList();

            UpdateLabels();
        }

        private void RefreshPackList()
        {
            var s = SettingsStore.Instance;
            PackComboBox.ItemsSource = s.PackNames;
            PackComboBox.SelectedItem = s.SelectedPack;
        }

        private void SaveSettings()
        {
            var s = SettingsStore.Instance;
            s.ReminderText = ReminderTextBox.Text;
            s.IsEnabled = EnabledCheckBox.IsChecked == true;
            s.LaunchAtLogin = LaunchAtLoginCheckBox.IsChecked == true;
            s.MinIntervalMinutes = (float)MinIntervalSlider.Value;
            s.MaxIntervalMinutes = (float)MaxIntervalSlider.Value;
            s.DisplayDurationSeconds = (float)DurationSlider.Value;
            s.Save();

            TrayIcon.UpdateAutoStart(s.LaunchAtLogin);
        }

        private void OnSliderChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            UpdateLabels();
        }

        private void UpdateLabels()
        {
            if (MinIntervalLabel == null) return; // Not yet initialized
            MinIntervalLabel.Text = MinIntervalSlider.Value < 1
                ? $"{(int)(MinIntervalSlider.Value * 60)}s"
                : $"{MinIntervalSlider.Value:F1}min";
            MaxIntervalLabel.Text = $"{(int)MaxIntervalSlider.Value}min";
            DurationLabel.Text = $"{DurationSlider.Value:F1}s";
        }

        private void OnShowNow(object sender, RoutedEventArgs e)
        {
            SaveSettings();
            ShowNowRequested?.Invoke();
        }

        private void OnPackChanged(object sender, SelectionChangedEventArgs e)
        {
            if (PackComboBox.SelectedItem is string name)
            {
                var s = SettingsStore.Instance;
                if (name != s.SelectedPack)
                {
                    s.SelectedPack = name;
                    ReminderTextBox.Text = s.ReminderText;
                }
            }
        }

        private void OnReminderTextChanged(object sender, TextChangedEventArgs e)
        {
            SettingsStore.Instance.ReminderText = ReminderTextBox.Text;
        }

        private void OnAddPack(object sender, RoutedEventArgs e)
        {
            // Simple input dialog using a message box workaround
            var dialog = new Window
            {
                Title = "New Pack",
                Width = 300,
                Height = 140,
                WindowStartupLocation = WindowStartupLocation.CenterOwner,
                Owner = this,
                ResizeMode = ResizeMode.NoResize,
                Background = new System.Windows.Media.SolidColorBrush(
                    (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#1E1E1E")),
            };

            var panel = new StackPanel { Margin = new Thickness(16) };
            var label = new TextBlock { Text = "Pack name:", Foreground = System.Windows.Media.Brushes.White, Margin = new Thickness(0, 0, 0, 8) };
            var textBox = new TextBox { Background = new System.Windows.Media.SolidColorBrush(
                (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString("#2D2D2D")),
                Foreground = System.Windows.Media.Brushes.White };
            var okButton = new Button { Content = "Add", Margin = new Thickness(0, 12, 0, 0) };
            okButton.Click += (_, _) =>
            {
                if (SettingsStore.Instance.AddPack(textBox.Text))
                {
                    RefreshPackList();
                    ReminderTextBox.Text = "";
                }
                dialog.Close();
            };
            panel.Children.Add(label);
            panel.Children.Add(textBox);
            panel.Children.Add(okButton);
            dialog.Content = panel;
            dialog.ShowDialog();
        }

        private void OnDeletePack(object sender, RoutedEventArgs e)
        {
            var s = SettingsStore.Instance;
            if (s.DeletePack(s.SelectedPack))
            {
                RefreshPackList();
                ReminderTextBox.Text = s.ReminderText;
            }
        }

        protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
        {
            SaveSettings();
            e.Cancel = true; // Just hide, don't close
            Hide();
        }
    }
}
