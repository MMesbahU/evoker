import java.io.IOException;

public abstract class RemoteBinaryData extends BinaryData{

    DataClient dc;

    RemoteBinaryData(DataClient dc, int numInds, MarkerData md, String collection){
        super (numInds,md,collection);
        this.dc = dc;
    }

    public void checkFile(String filename, byte[] headers) throws IOException{
        //TODO: check the magic numbers
    	String fileSizeString = dc.getFTP().stat(filename).getSize().toString();
    	Integer fileSize = Integer.parseInt(fileSizeString);
    	// check the size of the bnt or bed file using the default format header sizes
    	if (fileSize != (numSNPs*bytesPerRecord) + headers.length){
    		// check the size of the bnt or bed file using the Oxford format header sizes
    		if (fileSize != (numSNPs*bytesPerRecord) + 8){
    			throw new IOException(filename + " is not properly formatted.\n(Incorrect length.)");
    		}
    	}
    }        
}
