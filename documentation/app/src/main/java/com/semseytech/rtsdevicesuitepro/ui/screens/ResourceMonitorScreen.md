# ResourceMonitorScreen.kt

## What it does
Provides a real-time visualization of the device's hardware performance, including CPU, RAM, and Battery status.

## Why it does it
To help users understand their device's current performance state and identify resource-heavy processes or low-memory conditions.

## How it does it
- **System Sampling**: Periodically queries the Android system for CPU load, available RAM, and battery level.
- **Dynamic Charts**: (Typically) uses custom drawing or specialized components to render real-time performance graphs.
- **Metric Tiles**: Shows key statistics (e.g., "% RAM Used", "CPU Temperature") with color-coded severity indicators (Green for healthy, Red for high load).

## Overall role
A diagnostic dashboard for monitoring hardware health, complementing the file-system-focused tools in the rest of the suite.
