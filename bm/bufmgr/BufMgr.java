package bufmgr;

import global.GlobalConst;
import global.Minibase;
import global.Page;
import global.PageId;

import java.util.HashMap;

/**
 * <h3>Minibase Buffer Manager</h3>
 * The buffer manager manages an array of main memory pages.  The array is
 * called the buffer pool, each page is called a frame.  
 * It provides the following services:
 * <ol>
 * <li>Pinning and unpinning disk pages to/from frames
 * <li>Allocating and deallocating runs of disk pages and coordinating this with
 * the buffer pool
 * <li>Flushing pages from the buffer pool
 * <li>Getting relevant data
 * </ol>
 * The buffer manager is used by access methods, heap files, and
 * relational operators.
 */
public class BufMgr implements GlobalConst {
  /** Number of frames allowed */
  private int maxframes;
  /** Number of current valid frames in buffer pool */
  private int currframes;
  /** Array of pages in the current buffer pool */
  private Page[] bufferpool;
  /** Array of frame descriptions */
  private FrameDesc[] frametab;
  /** HashMap mapping page numbers to frame descriptions */
  private HashMap<Integer, FrameDesc> framedir;
  /** Clock for replacement policy */
  private Clock clock;

  /**
   * Constructs a buffer manager by initializing member data.  
   * 
   * @param numframes number of frames in the buffer pool
   */
  public BufMgr(int numframes) {
    maxframes = numframes;
    currframes = 0;
    bufferpool = new Page[numframes];
    frametab = new FrameDesc[numframes];
    for(int i = 0; i < numframes; i++){
      bufferpool[i] = new Page();
      frametab[i] = new FrameDesc();
      frametab[i].setBploc(i);
    }
    framedir = new HashMap<>(numframes);
    clock = new Clock(numframes);

  } // public BufMgr(int numframes)

  /**
   * The result of this call is that disk page number pageno should reside in
   * a frame in the buffer pool and have an additional pin assigned to it, 
   * and mempage should refer to the contents of that frame. <br><br>
   * 
   * If disk page pageno is already in the buffer pool, this simply increments 
   * the pin count.  Otherwise, this<br> 
   * <pre>
   * 	uses the replacement policy to select a frame to replace
   * 	writes the frame's contents to disk if valid and dirty
   * 	if (contents == PIN_DISKIO)
   * 		read disk page pageno into chosen frame
   * 	else (contents == PIN_MEMCPY)
   * 		copy mempage into chosen frame
   * 	[omitted from the above is maintenance of the frame table and hash map]
   * </pre>		
   * @param pageno identifies the page to pin
   * @param mempage An output parameter referring to the chosen frame.  If
   * contents==PIN_MEMCPY it is also an input parameter which is copied into
   * the chosen frame, see the contents parameter. 
   * @param contents Describes how the contents of the frame are determined.<br>  
   * If PIN_DISKIO, read the page from disk into the frame.<br>  
   * If PIN_MEMCPY, copy mempage into the frame.<br>  
   * If PIN_NOOP, copy nothing into the frame - the frame contents are irrelevant.<br>
   * Note: In the cases of PIN_MEMCPY and PIN_NOOP, disk I/O is avoided.
   * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned.
   * @throws IllegalStateException if all pages are pinned (i.e. pool is full)
   */
  public void pinPage(PageId pageno, Page mempage, int contents) {
      /* Does the page exist already in the buffer pool? */
    if(framedir.containsKey(pageno.hashCode())) {
      if(framedir.get(pageno.hashCode()).isValid()){
        if(contents == PIN_MEMCPY && framedir.get(pageno.hashCode()).isValid() && framedir.get(pageno.hashCode()).getPincount() > 0){
          throw new IllegalStateException("contents==PIN_MEMCPY and page is pinned");
        }
        framedir.get(pageno.hashCode()).incPincount();
      }
    }
    else {
      /* choose victim */
      int victim = clock.pickVictim(frametab);
      if (victim < 0){
        throw new IllegalStateException("All pages are pinned");
      }
      else{
        replacePage(victim, pageno, mempage, contents);
      }
    }


  } // public void pinPage(PageId pageno, Page page, int contents)
  
  /**
   * Unpins a disk page from the buffer pool, decreasing its pin count.
   * 
   * @param pageno identifies the page to unpin
   * @param dirty UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherwise
   * @throws IllegalArgumentException if the page is not in the buffer pool
   *  or not pinned
   */
  public void unpinPage(PageId pageno, boolean dirty) {

    if(!framedir.containsKey(pageno.hashCode()) || (framedir.get(pageno.hashCode()).isValid() && framedir.get(pageno.hashCode()).getPincount() < 1)){
        throw new IllegalArgumentException("Page is not in buffer pool or not pinned");
    }
    else if(framedir.get(pageno.hashCode()).isValid()){
      framedir.get(pageno.hashCode()).decPincount();
      if(framedir.get(pageno.hashCode()).getPincount() < 1){
        framedir.get(pageno.hashCode()).setrefbit(true);
      }
      if(dirty == UNPIN_DIRTY){
        framedir.get(pageno.hashCode()).setDirtyBit(true);
      }
    }

  } // public void unpinPage(PageId pageno, boolean dirty)
  
  /**
   * Allocates a run of new disk pages and pins the first one in the buffer pool.
   * The pin will be made using PIN_MEMCPY.  Watch out for disk page leaks.
   * 
   * @param firstpg input and output: holds the contents of the first allocated page
   * and refers to the frame where it resides
   * @param run_size input: number of pages to allocate
   * @return page id of the first allocated page
   * @throws IllegalArgumentException if firstpg is already pinned
   * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
   */
  public PageId newPage(Page firstpg, int run_size) {

      PageId newId = Minibase.DiskManager.allocate_page(run_size);
      int victim = 0;
      Minibase.DiskManager.read_page(newId, firstpg);
      if(framedir.containsKey(newId.hashCode()) && framedir.get(newId.hashCode()).isValid()) {
        if (framedir.get(newId.hashCode()).getPincount() > 0) {
          throw new IllegalArgumentException("firstpg is already pinned");
        } else {
          victim = framedir.get(newId.hashCode()).getBploc();
        }
      }
      else {
        victim = clock.pickVictim(frametab);
        if (victim < 0) {
          throw new IllegalStateException("All pages are pinned");
        }
      }
      replacePage(victim, newId, firstpg, PIN_MEMCPY);
      return newId;

  } // public PageId newPage(Page firstpg, int run_size)

  /**
   * Deallocates a single page from disk, freeing it from the pool if needed.
   * 
   * @param pageno identifies the page to remove
   * @throws IllegalArgumentException if the page is pinned
   */
  public void freePage(PageId pageno) {

      if(framedir.containsKey(pageno.hashCode()) && framedir.get(pageno.hashCode()).isValid()){
        if(framedir.get(pageno.hashCode()).getPincount() > 0){
          throw new IllegalArgumentException("Page is currently pinned");
        }
        else {
          framedir.get(pageno.hashCode()).setValidBit(false);
        }
      }
      Minibase.DiskManager.deallocate_page(pageno);

  } // public void freePage(PageId firstid)

  /**
   * Write all valid and dirty frames to disk.
   * Note flushing involves only writing, not unpinning or freeing
   * or the like.
   * 
   */
  public void flushAllFrames() {
      for(int i = 0; i < maxframes; i++){
        if(frametab[i].isValid()) {
          Minibase.DiskManager.write_page(new PageId(frametab[i].getPageId()), bufferpool[i]);
        }
      }

  } // public void flushAllFrames()

  /**
   * Write a page in the buffer pool to disk, if dirty.
   * 
   * @throws IllegalArgumentException if the page is not in the buffer pool
   */
  public void flushPage(PageId pageno) {

      if(framedir.containsKey(pageno.hashCode())){
        Minibase.DiskManager.write_page(pageno, bufferpool[framedir.get(pageno.hashCode()).getBploc()]);
      }
      else{
        throw new IllegalArgumentException("Page is not in the buffer pool");
      }

  }

   /**
   * Gets the total number of buffer frames.
   */
  public int getNumFrames() {
      return maxframes;
  }

  /**
   * Gets the total number of unpinned buffer frames.
   */
  public int getNumUnpinned() {
    int count = 0;
      for(int i = 0; i < maxframes; i++){
        if(frametab[i].isValid() && frametab[i].getPincount() == 0){
          count++;
        }
      }
      return count;
  }

  /**
   * Performs all of the replacement when evicting a page and
   * inserting a new page. Cannot fail so checks must be done before
   * replacePage() is called.
   * @param victim  Page number of victim page as integer
   * @param pageno  Page number of new page as PageId
   * @param mempage New page as Page
   * @param contents  Describes how contents of frame are determined
   */
  public void replacePage(int victim, PageId pageno, Page mempage, int contents) {
    int victimId = frametab[victim].getPageId();
    if (frametab[victim].isValid() && frametab[victim].isDirty()) {
      flushPage(new PageId(victimId));
    }
    if(framedir.containsKey(victimId)) {
      framedir.remove(victimId);
    }
    frametab[victim].setFrame(pageno.hashCode());
    framedir.put(pageno.hashCode(), frametab[victim]);
    // deal with contents
    if (contents == PIN_MEMCPY) {
      bufferpool[frametab[victim].getBploc()] = mempage;
    } else if (contents == PIN_DISKIO) {
      Minibase.DiskManager.read_page(pageno, mempage);
      bufferpool[frametab[victim].getBploc()] = mempage;
    }
    /* else contents == PIN_NOOP and we copy nothing */
    frametab[victim].incPincount();
  }

} // public class BufMgr implements GlobalConst
