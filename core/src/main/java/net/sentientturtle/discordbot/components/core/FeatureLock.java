package net.sentientturtle.discordbot.components.core;

import org.jetbrains.annotations.NotNull;

/**
 * Class to handle locking of JDA/Bot features that cannot be used by multiple modules at once.
 */
public enum FeatureLock {
    VOICE,
    PRESENCE;

    private static final Class<?>[] locks = new Class<?>[FeatureLock.values().length];

    /**
     * Attempt to lock a feature on behalf of a class.
     * @param lockingClass Class for whom to lock
     * @param feature Feature to lock
     * @return True if lock was (previously) acquired for class, false if feature has been locked by another class
     */
    public static synchronized boolean tryLock(@NotNull Class<?> lockingClass, @NotNull FeatureLock feature) {
        if (locks[feature.ordinal()] == null) {
            locks[feature.ordinal()] = lockingClass;
            return true;
        } else {
            return lockingClass.equals(locks[feature.ordinal()]);
        }
    }

    /**
     * Attempt to lock a feature on behalf of a class, throw if it cannot be locked.
     * @param lockingClass Class for whom to lock
     * @param feature Feature to lock
     * @throws IllegalStateException If lock could not be acquired
     */
    public static synchronized void lockOrThrow(@NotNull Class<?> lockingClass, @NotNull FeatureLock feature) throws IllegalStateException {
        if (!tryLock(lockingClass, feature)) {
            throw new IllegalStateException("Attempted to lock " + feature.name() + " but it was already taken by " + locks[feature.ordinal()]);
        }
    }

    /**
     * Attempt to atomically lock multiple features, either all features will be locked or none will be.
     * Returns true if zero features are specified.
     * @param lockingClass Class for whom to lock
     * @param features Features to lock
     * @return True if all features could be locked, false otherwise
     */
    public static synchronized boolean tryAtomicLock(@NotNull Class<?> lockingClass, @NotNull FeatureLock... features) {
        boolean success = true;
        for (FeatureLock feature : features) {
            success &= tryLock(lockingClass, feature);
        }
        if (!success) tryUnlock(lockingClass, features);
        return success;
    }

    /**
     * Attempt to atomically lock multiple features, either all features will be locked or none will be.
     * No-op if zero features are specified.
     * @param lockingClass Class for whom to lock
     * @param features Features to lock
     * @throws IllegalStateException If the lock on any of the specified features could not be acquired
     */
    public static synchronized void atomicLockOrThrow(@NotNull Class<?> lockingClass, @NotNull FeatureLock... features) throws IllegalStateException {
        try {
            for (FeatureLock feature : features) {
                lockOrThrow(lockingClass, feature);
            }
        } catch (IllegalStateException e) {
            tryUnlock(lockingClass, features);
            throw e;
        }
    }

    /**
     * Attempts to unlock the specified features.
     * Will only unlock features locked on behalf of the specified class.
     * If any specified feature is locked by another class, it will remain locked by that class.
     * No-op if zero features are specified.
     * @param unlockingClass Class for whom to unlock
     * @param features Features to unlock
     */
    public static synchronized void tryUnlock(@NotNull Class<?> unlockingClass, @NotNull FeatureLock... features) {
        int unlocked = 0;
        for (FeatureLock feature : features) {
            if (unlockingClass.equals(locks[feature.ordinal()])) {
                locks[feature.ordinal()] = null;
                unlocked++;
            }
        }
    }

    /**
     * Attempts to unlock the specified features
     * Will raise an exception if any of the specified features are not owned by unlockingClass
     * <br/>
     * Intended for use situations where premature unlocking indicates a fault
     * No-op if zero features are specified.
     * @param unlockingClass Class for whom to unlock
     * @param features Features to unlock
     * @throws IllegalArgumentException If any of the specified
     */
    public static synchronized void unlockOrThrow(@NotNull Class<?> unlockingClass, @NotNull FeatureLock... features) throws IllegalArgumentException {
        for (FeatureLock feature : features) {
            if (unlockingClass.equals(locks[feature.ordinal()])) {
                locks[feature.ordinal()] = null;
            } else {
                throw new IllegalArgumentException("Could not unlock " + feature.name() + " on behalf of " + unlockingClass + " it was instead held by " + locks[feature.ordinal()]);
            }
        }
    }
}
