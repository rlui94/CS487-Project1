package bufmgr;
import global.PageId;

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
    private int bploc;

    public FrameDesc(){
        this.pagenum = -1;
        this.isDirty = false;
        this.isValid = false;
        this.pincount = 0;
        this.refbit = false;
        this.bploc = -1;
    }

    /** Set the values of this frame description */
    public void setFrame (int pagenum) {
        this.pagenum = pagenum;
        this.isDirty = false;
        this.isValid = true;
        this.pincount = 0;
        this.refbit = true;
    }

    /** pincount manipulation */
    public void resetPin(){
        this.pincount = 0;
    }
    public void incPincount(){
        this.pincount++;
    }
    public void decPincount(){
        this.pincount--;
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
    public int getPageId(){
        return this.pagenum;
    }
    public int getBploc(){
        return this.bploc;
    }

    /** Setters*/
    public void setrefbit(boolean bit){
        refbit = bit;
    }
    public void setDirtyBit(boolean bit){
        isDirty = bit;
    }
    public void setValidBit(boolean bit){
        isValid = bit;
    }
    public void setBploc(int loc){
        bploc = loc;
    }

}
