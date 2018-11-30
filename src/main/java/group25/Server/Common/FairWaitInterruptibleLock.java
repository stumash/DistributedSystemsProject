package group25.Server.Common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class FairWaitInterruptibleLock {
    private Deque<Integer> xids;
    private HashMap<Integer, Semaphore> xidToSemaphore;

    public FairWaitInterruptibleLock() {
        xids = new ArrayDeque<Integer>();
        xidToSemaphore = new HashMap<>();
    }

    public boolean lock(int xid) {
        synchronized(this) {
            xids.addLast(xid);
            xidToSemaphore.put(xid, new Semaphore(1));

            try {
                if (xids.peekFirst() != xid) {// not first in line
                    xidToSemaphore.get(xid).acquire();// next acquire() fails
                }
            } catch (InterruptedException e) {
                // do nothing, can't fail
            }
        }

        try {
            xidToSemaphore.get(xid).acquire();
            synchronized(this) {
                if (!xidToSemaphore.containsKey(xid)) { // you have interrupted from waiting on the lock
                    return false; // so you fail to acquire the lock
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return true;
    }

    public boolean unlock(int xid) {
        synchronized(this) {
            xids.removeFirst(); // remove yourself from queue
            xidToSemaphore.remove(xid); // and from map of semaphores
            int nextXid = xids.peekFirst(); // then get the next xid
            xidToSemaphore.get(nextXid).release(); // and wake him up
            return true; // release successful
        }
    }

    public boolean interruptWaiter(int xid) {
        synchronized(this) {
            if (xids.peekFirst() == xid) {
                return false;
            }

            boolean found = xids.remove(xid);
            if (!found) {
                return false;
            }

            Semaphore sem = xidToSemaphore.get(xid);
            xidToSemaphore.remove(xid);
            sem.release();
        }
        return true;
    }

    public int getLockOwner() {
        synchronized(this) {
            if (xids.size() != 0) {
                return xids.peekFirst();
            } else {
                return -1;
            }
        }
    }
}