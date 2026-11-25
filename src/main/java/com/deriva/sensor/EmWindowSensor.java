package com.deriva.sensor;

/** Port for reading the current foreground window. Implementations live in adapters. */
@FunctionalInterface
public interface EmWindowSensor {
    EmActivitySample sample();
}
