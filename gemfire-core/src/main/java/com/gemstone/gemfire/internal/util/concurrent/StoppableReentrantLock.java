/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.util.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.gemstone.gemfire.CancelCriterion;
import com.gemstone.gemfire.internal.Assert;

/**
 * Instances of {@link java.util.concurrent.locks.Lock}
 * that respond to cancellations
 * 
 * @author jpenney
 */
public class StoppableReentrantLock {
  /**
   * the underlying lock
   */
  private final ReentrantLock lock;
  
  /**
   * This is how often waiters will wake up to check for cancellation
   */
  private static final long RETRY_TIME = 15 * 1000; // milliseconds

  /**
   * the cancellation criterion
   */
  private final CancelCriterion stopper;
  
  /**
   * Create a new instance with the given cancellation criterion
   * @param stopper the cancellation criterion
   */
  public StoppableReentrantLock(CancelCriterion stopper) {
    Assert.assertTrue(stopper != null);
    this.lock = new ReentrantLock();
    this.stopper = stopper;
    }

  /**
   * Create a new instance with given fairness and cancellation criterion
   * @param fair whether to be fair
   * @param stopper the cancellation criterion
   */
  public StoppableReentrantLock(boolean fair, CancelCriterion stopper) {
    Assert.assertTrue(stopper != null);
    this.stopper = stopper;
    this.lock = new ReentrantLock();
    }


  public void lock() {
    for (;;) {
      boolean interrupted = Thread.interrupted();
      try {
        lockInterruptibly();
        break;
      }
      catch (InterruptedException e) {
        interrupted = true;
      }
      finally {
        if (interrupted) Thread.currentThread().interrupt();
      }
    } // for
  }

  /**
   * @throws InterruptedException
   */
  public void lockInterruptibly() throws InterruptedException {
    for (;;) {
      stopper.checkCancelInProgress(null);
      if (lock.tryLock(RETRY_TIME, TimeUnit.MILLISECONDS))
        break;
    }
  }

  /**
   * @return true if the lock is acquired
   */
  public boolean tryLock() {
    stopper.checkCancelInProgress(null);
    return lock.tryLock();
  }

  /**
   * @param timeoutMs how long to wait in milliseconds
   * @return true if the lock was acquired
   * @throws InterruptedException
   */
  public boolean tryLock(long timeoutMs) throws InterruptedException {
    stopper.checkCancelInProgress(null);
    return lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
  }

  public void unlock() {
    lock.unlock();
  }

  /**
   * @return the new stoppable condition
   */
  public StoppableCondition newCondition() {
    return new StoppableCondition(lock.newCondition(), stopper);
  }
  
  /**
   * @return true if it is held
   */
  public boolean isHeldByCurrentThread() {
    return lock.isHeldByCurrentThread();
  }
}
