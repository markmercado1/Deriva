package com.deriva.goals;

/**
 * Read-only source consulted by the capture loop. It hides whether goals live in
 * SQLite or in memory, so CaptureSession is not coupled to the UI or storage.
 */
public interface PassiveGoalProvider {
    PassiveGoalSnapshot snapshot();
}
