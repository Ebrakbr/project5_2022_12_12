package differences;

import com.inet.pdfc.PDFComparer;
import com.inet.pdfc.error.PdfcException;
import com.inet.pdfc.generator.message.InfoData;
import com.inet.pdfc.results.ResultModel;
import util.SampleUtil;

import java.io.File;

/**
 * A sample to show the difference of the number of pages between 2 PDF files.
 *
 * Expects 2 arguments: the paths of the PDF files
 *
 */
public class NumOfDifferencePageNumber{

    /**
     * A sample to show the difference of the number of pages between 2 PDF files.
     * @param args Expects 2 arguments: the paths of the PDF files
     */
    public static void main( String[] args ) {
        SampleUtil.filterServerPlugins();
        File[] files = getFileOfArguments( args );
        PDFComparer pdfComparer = new PDFComparer();

        try ( ResultModel result = pdfComparer.compare( files[0], files[1] ) ){
            InfoData comparisonParameters = result.getComparisonParameters();

            int differencePageNumber = comparisonParameters.getFirstPageCount() - comparisonParameters.getSecondPageCount();
            System.out.println( "difference in number of pages = " + differencePageNumber );
        } catch( PdfcException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Get 2 files that are to be checked
     *
     * @param args the arguments
     * @return 2 Files
     */
    public static File[] getFileOfArguments(final String[] args){
        if (args == null || args.length != 2) {
            throw new IllegalArgumentException( "Usage: NumOfDifferencePageNumber <PDF-File1> <PDF-File2>" );
        }
        return new File[]{ SampleUtil.checkAndGetFile( args[0] ), SampleUtil.checkAndGetFile( args[1] )};
    }
}
