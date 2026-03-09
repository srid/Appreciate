using System;

namespace Appreciate
{
    /// <summary>
    /// Schedules overlay reminders at random intervals.
    /// </summary>
    public class TimerManager
    {
        private System.Timers.Timer? _timer;
        public event Action? OnTick;

        public void Start()
        {
            ScheduleNext();
        }

        public void Stop()
        {
            _timer?.Stop();
            _timer?.Dispose();
            _timer = null;
        }

        public void ScheduleNext()
        {
            _timer?.Stop();
            _timer?.Dispose();

            var settings = SettingsStore.Instance;
            if (!settings.IsEnabled) return;

            var minMs = settings.MinIntervalMinutes * 60 * 1000;
            var maxMs = settings.MaxIntervalMinutes * 60 * 1000;
            var intervalMs = minMs + Random.Shared.NextDouble() * (maxMs - minMs);

            _timer = new System.Timers.Timer(Math.Max(1000, intervalMs));
            _timer.AutoReset = false;
            _timer.Elapsed += (_, _) =>
            {
                OnTick?.Invoke();
                ScheduleNext();
            };
            _timer.Start();
        }
    }
}
