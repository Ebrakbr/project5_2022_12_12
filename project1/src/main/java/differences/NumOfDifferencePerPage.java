package differences;

import com.inet.pdfc.PDFComparer;
import com.inet.pdfc.error.PdfcException;
import com.inet.pdfc.generator.model.DiffGroup;
import com.inet.pdfc.generator.model.Modification;
import com.inet.pdfc.model.PagedElement;
import com.inet.pdfc.results.ResultModel;
import util.SampleUtil;

import java.io.File;
import java.util.List;

/**
 * A Sample that calculates the differences per page.
 * Expects 2 arguments: the paths of the PDF files
 */
public class NumOfDifferencePerPage {

    /**
     * A Sample that calculates the differences per page.
     * @param args Expects 2 arguments: the paths of the PDF files
     */
    public static void main( String[] args ) {
        SampleUtil.filterServerPlugins();
        File[] files = getFileOfArguments( args );

        PDFComparer pdfComparer = new PDFComparer();
        try( ResultModel result = pdfComparer.compare( files[0], files[1] ) ){

            //Array for the result
            int[] changesPerPage = new int[Math.max( result.getPageCount( true ), result.getPageCount( false ) )];

            List<DiffGroup> differences = result.getDifferences( false );
            for( DiffGroup difference: differences ) {
                List<Modification> modifications = difference.getModifications();
                if( modifications != null ) {
                    inner:
                    for( Modification modification: modifications ) {
                        List<PagedElement> affectedElements = modification.getAffectedElements( true );
                        for( PagedElement affectedElement: affectedElements ) {
                            int pageIndex = affectedElement.getPageIndex();
                            changesPerPage[pageIndex] = changesPerPage[pageIndex] + 1;
                            continue inner;

                        }
                        affectedElements = modification.getAffectedElements( false );
                        for( PagedElement affectedElement: affectedElements ) {
                            int pageIndex = affectedElement.getPageIndex();
                            changesPerPage[pageIndex] = changesPerPage[pageIndex] + 1;
                            continue inner;
                        }
                    }
                }
            }

            //output the result
            for( int i = 0; i < changesPerPage.length; i++ ) {
                int i1 = changesPerPage[i];
                System.out.println( "Page #"+ (i + 1) +" has changes: " + i1 );
            }

        } catch( PdfcException e ) {
            e.printStackTrace();
        }
    }

    /**
     * Get 2 files that are to be checked
     * @param args the arguments
     * @return 2 Files
     */
    public static File[] getFileOfArguments( final String[] args ) {
        if( args == null || args.length != 2 ) {
            throw new IllegalArgumentException( "Usage: NumOfDifferencePerPage <PDF-File1> <PDF-File2>" );
        }
        return new File[] { SampleUtil.checkAndGetFile( args[0] ), SampleUtil.checkAndGetFile( args[1] ) };
    }
}
