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
            PackComboBox.ItemsSource = SettingsStore.PackNames;
            PackComboBox.SelectedItem = s.SelectedPack;

            UpdateLabels();
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
            if (PackComboBox.SelectedItem is string name && name != "Custom")
            {
                SettingsStore.Instance.SelectPack(name);
                ReminderTextBox.Text = SettingsStore.Instance.ReminderText;
            }
        }

        private void OnReminderTextChanged(object sender, TextChangedEventArgs e)
        {
            var s = SettingsStore.Instance;
            s.ReminderText = ReminderTextBox.Text;
            s.CheckCustomPack();
            if (PackComboBox.SelectedItem as string != s.SelectedPack)
            {
                PackComboBox.SelectedItem = s.SelectedPack;
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
