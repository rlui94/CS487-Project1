package bufmgr;

/**
 * Chooses which frame to evict from buffer pool.
 */
public class Clock {
    /* number of frames in buffer pool */
    private int numframes;
    /* the currently considered frame */
    private int currframe;

    public Clock(int numframes){
        this.numframes = numframes;
        currframe = -1;
    }

    /**
     * Chooses the victim frame to evict from the buffer pool
     * @param frametab is the frame table we iterate through to find
     *                 the frame to evict
     * @return  value is negative if every page in the buffer
     * pool is pinned, otherwise a positive integer from 1 to numframes-1
     *
     */
    public int pickVictim(FrameDesc[] frametab){
        /* make sure we look at a new frame */
        currframe++;
        /* iterate through n-1 frames twice */
        for(int counter = 0; counter < numframes * 2; counter++){
            /* if invalid, return currframe */
            if(!frametab[currframe].isValid()){
                return currframe;
            }
            /* otherwise if pincount == 0, check the reference bit */
            else if(frametab[currframe].getPincount() == 0){
                if(frametab[currframe].getrefbit()){
                    frametab[currframe].setrefbit(false);
                }
                else{
                    return currframe;
                }
            }
            /* make sure we stay within 0 to n-1 */
            if(currframe < numframes-2){
                currframe++;
            }
            else{
                currframe = 0;
            }
        }
        /* return a negative since we couldn't find an available frame */
        return -1;
    }
}
