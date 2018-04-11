package katas.memoryref;


import com.sun.istack.internal.Nullable;

import java.lang.ref.*;
import java.util.HashSet;
import java.util.Set;

public class Main{

    public static final int HOW_MANY = 500_000;

    public static void main(String [] args) throws InterruptedException {
        // byBarbini();
        byVarghese();
    }

    private static void byVarghese() throws InterruptedException {
        HeavyList heavyList = new HeavyList(0, null); // a strong object

        ReferenceQueue<HeavyList> referenceQueue = new ReferenceQueue<HeavyList>();// the ReferenceQueue
        WeakReference<HeavyList> reference = new WeakReference<HeavyList>(heavyList, referenceQueue);

        System.out.println("Any weak references in Q ? " + (referenceQueue.poll() != null));
        heavyList = null;

        System.out.println("Now to call gc...");
        Runtime.getRuntime().gc(); // the object will be cleared here - finalize will be called.
        Reference<? extends HeavyList> refRemoved = referenceQueue.remove();
        System.out.println("Any weak references in Q ? " + (refRemoved != null));
        System.out.println("Is this same as original weak reference ? " + (refRemoved == reference));
        System.out.println(" and heap object is " + refRemoved.get());
    }

    private static void byBarbini() {
        //-XX:+HeapDumpOnOutOfMemoryError -Xmx4096m
        //try with
        // -XX:+UnlockExperimentalVMOptions -XX:G1MaxNewSizePercent=75 -XX:G1NewSizePercent=50 -XX:+UseG1GC
        // or with
        //-XX:+CMSParallelRemarkEnabled, -XX:+UseConcMarkSweepGC, -XX:+UseParNewGC, -XX:ParallelGCThreads=8, -XX:SurvivorRatio=25


        System.out.println("Start!");

        ReferenceQueue<HeavyList> referenceQueue = new ReferenceQueue();

        Set<Reference<HeavyList>> references = new HashSet<>();

        printMem();
        System.out.println("Press ^C to break!");
        System.out.println("\n\nUsed mem");

        long startTime = System.currentTimeMillis();

        allocationLoop(referenceQueue, references, 100);
        System.out.println("Total time of allocation loop " + (System.currentTimeMillis() - startTime));

        System.gc();

        int removed = removeRefsPolledFromReferenceQueue(referenceQueue, references);
        printRefs(references, removed);
    }

    private static void allocationLoop(ReferenceQueue<HeavyList> queue, Set<Reference<HeavyList>> references, int howManyTimes) {
        HeavyList head = new HeavyList(0, null);
        HeavyList oldTail = head;
        for (int i = 0; i < howManyTimes; i++) {

            HeavyList newTail = allocate(HOW_MANY, oldTail);

            createReferencesAndRegisterThemInQueue(queue, references, oldTail);

            deallocateHalf(head);

            int removed = removeRefsPolledFromReferenceQueue(queue, references);

            System.gc();   //uncomment this line to comparing with forced gc
            printRefs(references, removed);

            oldTail = newTail;
        }
        head = null;
        oldTail = null;
    }

    private static void printRefs(Set<Reference<HeavyList>> references, int removed) {
        System.out.println("Used mem " + getUsedMem() + "    Refs removed from ref queue " + removed + "   left " + references.size());
    }

    private static void createReferencesAndRegisterThemInQueue(ReferenceQueue<HeavyList> queue, Set<Reference<HeavyList>> references, HeavyList oldTail) {
        HeavyList curr = oldTail.next;
        while (curr != null) {
//            Reference<HeavyList> reference = new SoftReference<>(curr, queue);
//                Reference<HeavyList> reference = new WeakReference<>(curr, queue);
                Reference<HeavyList> reference = new PhantomReference<>(curr, queue);
            references.add(reference);

            curr = curr.getNext();
        }
    }

    private static long getUsedMem() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static int removeRefsPolledFromReferenceQueue(ReferenceQueue queue, Set<Reference<HeavyList>> references) {
        int removed = 0;
        while (true){
            Reference r = queue.poll();
            if (r == null)
                break;
            references.remove(r);
            removed++;
        }
        return removed;
    }

    private static void deallocateHalf(HeavyList head) {
        HeavyList curr = head;

        while(curr != null){
            curr.dropNext();
            curr = curr.getNext();
        }
    }

    private static void printMem() {
         /* Total number of processors or cores available to the JVM */
        System.out.println("Available processors (cores): " +
                Runtime.getRuntime().availableProcessors());

        /* Total amount of free memory available to the JVM */
        System.out.println("Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        System.out.println("Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently in use by the JVM */
        System.out.println("Total memory (bytes): " +
                Runtime.getRuntime().totalMemory());

    }

    private static HeavyList allocate(int howMany, HeavyList startFrom) {

        HeavyList curr = startFrom;
        for (int i = 0; i < howMany; i++) {
            curr = new HeavyList(i, curr);
        }
        return curr;

    }

    private static int count(HeavyList list) {

        HeavyList curr = list;
        int tot = 0;
        while (curr != null) {
            tot++;
            curr = curr.getNext();
        }
        return tot;

    }

    private static class HeavyList {

        byte[] mega = new byte[1000];
        private HeavyList next = null;

        public HeavyList(int number, @Nullable HeavyList prev) {
            for (int i = 0; i < mega.length; i++) {
                mega[i] = (byte) (number % 256);
            }
            if (prev != null) {
                prev.next = this;
            }
        }

        public HeavyList getNext() {
            return next;
        }

        public HeavyList dropNext(){
            if (next == null || next.next == null)
                return null;
            HeavyList res = next;
            next = next.next;
            return res;
        }
    }

}
