package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	
	Queue<KThread> queue;
	Queue<Long> waitTime;
	int counter;
	
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {

		queue = new LinkedList<KThread>();
		waitTime = new LinkedList<Long>();

		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {

		long curr = Machine.timer().getTime();
		KThread.currentThread().yield();
		for (int i = 0; i < queue.size(); i++) {
			KThread thread = queue.poll();
			Long time = waitTime.poll();
			if (time <= curr) {
				thread.ready();
			} else {
				queue.add(thread);
				waitTime.add(time);
			}
		}
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {

		boolean status = Machine.interrupt().disable();

		queue.add(KThread.currentThread());
		waitTime.add((Long) (x + Machine.timer().getTime()));

		KThread.sleep();

		Machine.interrupt().restore(status);
	}

        /**
	 * Cancel any timer set by <i>thread</i>, effectively waking
	 * up the thread immediately (placing it in the scheduler
	 * ready set) and returning true.  If <i>thread</i> has no
	 * timer set, return false.
	 * 
	 * <p>
	 * @param thread the thread whose timer should be cancelled.
	 */
    public boolean cancel(KThread thread) {

		boolean cancel = false;

		for (int i = 0; i < queue.size(); i++) {
			KThread t = queue.poll();
			Long time = waitTime.poll();
			if (t == thread) {
				t.ready();
				cancel = true;
			} else {
				queue.add(t);
				waitTime.add(time);
			}
		}

		return (cancel == true);
	}

	public static void alarmTest1() {
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;
	
		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil (d);
			t1 = Machine.timer().getTime();
			System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}
}
