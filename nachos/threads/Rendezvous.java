package nachos.threads;

import nachos.machine.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        threadQueues = new HashMap<Integer, ArrayList<KThread>>();
        valueQueues = new HashMap<Integer, ArrayList<Integer>>();
        conditions = new HashMap<Integer, Condition>();
        locks = new HashMap<Integer, Lock>();
        numFound = new HashMap<Integer, Integer>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */
    public int exchange (int tag, int value) {
	    if (!locks.containsKey(tag)) {
            locks.put(tag, new Lock());
        }

        Lock lock = locks.get(tag);

        lock.acquire();

        if (!threadQueues.containsKey(tag)) {
            threadQueues.put(tag, new ArrayList<KThread>());
            valueQueues.put(tag, new ArrayList<Integer>());
            conditions.put(tag, new Condition(lock));
            numFound.put(tag, 1);
        } else {
            numFound.put(tag, numFound.get(tag) + 1);
        }

        ArrayList<KThread> queue = threadQueues.get(tag);
        ArrayList<Integer> values = valueQueues.get(tag);

        queue.add(KThread.currentThread());
        values.add(value);
        
        lock.release();

        lock.acquire();

        while(queue.size() < 2 || numFound.get(tag) >= 3 || (KThread.currentThread() != queue.get(0) && KThread.currentThread() != queue.get(1))) {
            conditions.get(tag).sleep();
        }

        conditions.get(tag).wake();

        int ret = values.get(queue.indexOf(KThread.currentThread()) ^ 1);

        lock.release();

        lock.acquire();

        numFound.put(tag, numFound.get(tag) - 1);

        while(numFound.get(tag) > 0) {
            conditions.get(tag).sleep();
        }
        conditions.get(tag).wake();

        
        queue.remove(0);
        values.remove(0);

        lock.release();

        return ret;
    }

    HashMap<Integer, ArrayList<KThread>> threadQueues;
    HashMap<Integer, ArrayList<Integer>> valueQueues;
    HashMap<Integer, Condition> conditions;
    HashMap<Integer, Lock> locks;
    HashMap<Integer, Integer> numFound;

    // Place Rendezvous test code inside of the Rendezvous class.

    public static void rendezTest1() {
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t1.setName("t1");
        KThread t2 = new KThread( new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
        });
        t2.setName("t2");
    
        t1.fork(); t2.fork();
        // assumes join is implemented correctly
        t1.join(); t2.join();
    }
    
    // Invoke Rendezvous.selfTest() from ThreadedKernel.selfTest()
    public static void selfTest() {
        // place calls to your Rendezvous tests that you implement here
        rendezTest1();
    }

}
