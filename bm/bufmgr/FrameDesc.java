package bufmgr;

/**
 * Frame description object
 * Contains information on various states associated with a page
 */
public class FrameDesc {
    /** Associated page number */
    private int pagenum;
    /** Dirty bit */
    private boolean isDirty;
    /** Valid bit */
    private boolean isValid;
    /** How many callers have pins on the data */
    private int pincount;
    /** Reference bit is true if page has been referenced recently.
     * Reset to true when pincount is zero.
     */
    private boolean refbit;
    /** Location in buffer pool array */
    private int bploc;

    /**
     * Takes a page number and creates a frame description object
     * @param pagenum   Page number associated with this frame description
     */
    public FrameDesc(int pagenum){
        this.pagenum = pagenum;
        this.isDirty = false;
        this.isValid = true;
        this.pincount = 0;
        this.refbit = true;
        this.bploc = -1;
    }

    /** Set the values of this frame description */
    public void setFrame (int pagenum){
        this.pagenum = pagenum;
        this.isDirty = false;
        this.isValid = true;
        this.pincount = 0;
        this.refbit = true;
        this.bploc = -1;
    }

    /** pincount manipulation */
    public void resetPin(){
        this.pincount = 0;
    }
    public void incPincount(){
        this.pincount++;
    }

    /** Getters */
    public boolean isDirty(){
        return this.isDirty;
    }
    public boolean isValid(){
        return this.isValid;
    }
    public int getPincount(){
        return this.pincount;
    }
    public boolean getrefbit(){
        return this.refbit;
    }
    public int getBploc(){
        return this.bploc;
    }
}
