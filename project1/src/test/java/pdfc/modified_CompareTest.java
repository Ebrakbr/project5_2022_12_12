package pdfc;

import com.inet.pdfc.PDFComparer;
import com.inet.pdfc.config.XMLProfile;
import com.inet.pdfc.error.PdfcException;
import com.inet.pdfc.generator.model.DiffGroup;
import com.inet.pdfc.results.ResultModel;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;


/**
 * A sample for writing JUnit test cases using PDFC
 */
public class modified_CompareTest {

    private PDFComparer pdfComparer;

    @Before
    public void before() {
        pdfComparer = new PDFComparer();
    }
    static Logger LOGGER = LogManager.getLogger(modified_CompareTest.class);
    @Test
    public void testDifferences() throws PdfcException, IOException {
        String file1 = "PDF je Zug ROMAN 160_20221211_a5.pdf";
        String file2 = "PDF je Zug TRAKSYS 160_20221211_a5.pdf";
        String xmlfile = "Continuous_document_profil.xml";
String msg;
        File example1 = new File(file1);
        File example2 = new File(file2);
        File profil = new File(xmlfile);

        ResultModel result = new PDFComparer().setProfile(new XMLProfile(profil)).compare(example1, example2);
        result.getDifferences(true).get(1);
//        InfoData comparisonParameters = result.getComparisonParameters();
//        Assertions.assertEquals(0, result.getDifferencesCount( false ));
//        Assertions.assertEquals(0, result.getDifferencesCount( true ));
//        Assertions.assertEquals(1213, comparisonParameters.getFirstPageCount());
//        Assertions.assertEquals(1213, comparisonParameters.getSecondPageCount());

        for (int i = 0; i < result.getDifferences(true).size(); i++) {
            DiffGroup diff = result.getDifferences(true).get(i);
            if (diff.getAddedElements().size() != 0) {
                msg="\n\n" +
                        "page Number = " + diff.getAddedElements().get(0).getPageIndex() + 1 + "\n" +
                        "dif type = " + diff.getType() + "\n" +
                        "explan = " + diff + "\n";
                LOGGER.info(msg);

            }
        }

    }
}
