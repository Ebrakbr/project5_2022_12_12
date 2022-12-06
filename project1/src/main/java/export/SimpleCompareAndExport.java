package export;

import com.inet.pdfc.PDFComparer;
import com.inet.pdfc.error.PdfcException;
import com.inet.pdfc.presenter.DifferencesPDFPresenter;
import util.SampleUtil;

import java.io.File;

/**
 * A simple Sample for export to pdf file the comparing between 2 PDF Files
 *
 * Expects 2 arguments: the paths of the PDF files
 */
public class SimpleCompareAndExport{

    /**
     * A simple Sample for export to pdf file the comparing between 2 PDF Files
     * @param args Expects 2 arguments: the paths of the PDF files
     */
    public static void main( String[] args ) {
        SampleUtil.filterServerPlugins();
        File[] files = getFileOfArguments( args );

        //Use the current i-net PDFC configuration. If no configuration has been previously set then the default configuration will be used.
        DifferencesPDFPresenter differencesPDFPresenter = new DifferencesPDFPresenter( files[0].getParentFile() );
        PDFComparer pdfComparer = new PDFComparer().addPresenter( differencesPDFPresenter );
        try {
            pdfComparer.compare( files[1], files[0] );
            SampleUtil.showPresenterError( pdfComparer );
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
            throw new IllegalArgumentException( "Usage: SimpleCompareAndExport <PDF-File1> <PDF-File2>" );
        }
        return new File[]{ SampleUtil.checkAndGetFile( args[0] ), SampleUtil.checkAndGetFile( args[1] )};
    }
}
