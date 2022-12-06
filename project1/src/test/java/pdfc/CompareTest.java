package pdfc;


import com.inet.pdfc.config.FilePdfSource;
import com.inet.pdfc.error.PdfcException;
import com.inet.pdfc.model.*;
import com.inet.pdfc.plugin.DocumentReader;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.SampleUtil;
import wantedObjects.TrainStation;

import java.io.File;
/*import java.io.FileWriter;
import java.io.IOException;*/
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.abs;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static parser.PDFAnalysis.getFileOfArguments;


public class CompareTest {
    String trainNamePrefix = "RJX";
//    ----------------------------------------------------
    int stationXOffsetInFile_1 = 30;
    int PDF_1StationFontSizeIndex = 3;
    double stationFonToleranceInFile_1 = 0.6;
    double wordsYOffsetInFile_1 = 0.001;
    int stationReplacementValueInFile_1 = 20;
//    ----------------------------------------------------
//    ////////////////////////////////////////////////////
    int stationXOffsetInFile_2 = 30;
    int PDF_2StationFontSizeIndex = 3;
    double stationFonToleranceInFile_2 = 0.6;
    double wordsYOffsetInFile_2 = 0.04;
    int stationReplacementValueInFile_2 = 20;
//    ////////////////////////////////////////////////////
    List<TextElement> TextElementsInFile1 = new ArrayList<>();
    List<TextElement> TextElementsInFile2 = new ArrayList<>();
    List<TrainStation> trainStationsInPdf_1 = new ArrayList<>();
    List<TrainStation> trainStationsInPdf_2 = new ArrayList<>();
    List<TextElement> officialStations = new ArrayList<>();
    FontInfo fontInfo = null;
    static Logger LOGGER = LogManager.getLogger(CompareTest.class);
    float PDF_1StationFontSize = 0;
    float PDF_2StationFontSize = 0;
    String trainNumber;

    @Test
    public void testDifferences() {
        String file1 = "PDF je Zug ROMAN 160_20221211_a5.pdf";
        String file2 = "PDF je Zug TRAKSYS 160_20221211_a5.pdf";
        String file3 = "Stations.pdf";

        SampleUtil.filterServerPlugins();

        File file = getFileOfArguments(new String[]{file1});
        TextElementsInFile1 = readPDFText(file);
        file = getFileOfArguments(new String[]{file2});
        TextElementsInFile2 = readPDFText(file);
        file = getFileOfArguments(new String[]{file3});

        officialStations = readPDFText(file);
        pluralizeStations(officialStations);

        sortTextElements(TextElementsInFile1,wordsYOffsetInFile_1);
        sortTextElements(TextElementsInFile2,wordsYOffsetInFile_2);

        fontInfo = stationFontInfo(officialStations, TextElementsInFile1);

        concatTextElements(TextElementsInFile1,wordsYOffsetInFile_1,stationFonToleranceInFile_1);
        concatTextElements(TextElementsInFile2,wordsYOffsetInFile_2,stationFonToleranceInFile_2);

        if (areBothFilesAboutSameTrain()) {

            PDF_1StationFontSize = StationFontSize(TextElementsInFile1, PDF_1StationFontSizeIndex);
            PDF_2StationFontSize = StationFontSize(TextElementsInFile2, PDF_2StationFontSizeIndex);

            setXPositionForStations(TextElementsInFile1, PDF_1StationFontSize,stationFonToleranceInFile_1,stationReplacementValueInFile_1);
            setXPositionForStations(TextElementsInFile2, PDF_2StationFontSize,stationFonToleranceInFile_2,stationReplacementValueInFile_2);

            sortAfterConcat(TextElementsInFile1, PDF_1StationFontSize,stationFonToleranceInFile_1);
            sortAfterConcat(TextElementsInFile2, PDF_1StationFontSize,stationFonToleranceInFile_1);

            trainStationsInPdf_1 = genStationArray(TextElementsInFile1, PDF_1StationFontSize,stationFonToleranceInFile_1,stationXOffsetInFile_1);
            trainStationsInPdf_2 = genStationArray(TextElementsInFile2, PDF_2StationFontSize,stationFonToleranceInFile_2,stationXOffsetInFile_2);

            printStations();
        } else {
            System.out.println("Trains are Different in pdf files, comparison is not allowed.");
        }

    }

    private boolean areBothFilesAboutSameTrain() {
        for (TextElement textInFile1 : TextElementsInFile1) {
            if (!textInFile1.getText().startsWith(trainNamePrefix)) continue;
            for (TextElement textInFile2 : TextElementsInFile2) {
                if (!textInFile2.getText().startsWith(trainNamePrefix)) continue;
                if (textInFile1.getText().replaceAll(" ", "").
                        equals(textInFile2.getText().replaceAll(" ", ""))) {
                    trainNumber = textInFile1.getText();
                    return true;

                }
            }
        }
        return false;
    }

    private FontInfo stationFontInfo(List<TextElement> officialStations, List<TextElement> textElementsInFile1) {
        for (TextElement station : officialStations) {
            for (TextElement element : textElementsInFile1) {
                if (Objects.equals(station.getText(), element.getText())) return element.getFontInfo();
            }
        }
        return null;
    }

    private void pluralizeStations(List<TextElement> officialStations) {
        String pageNumber;
        String xOfCode;
        String yOfCode;
        String pageNumberInPositionList;
        String xInPositionList;
        String yInPositionList;
        ArrayList<String> positionOfCodeColumn = new ArrayList<>();

        List<TextElement> localOfficialStations = new ArrayList<>(officialStations);

        for (TextElement element : localOfficialStations) {
            if (element.getText().toLowerCase().replaceAll(" ", "").equals("code")) {
                pageNumber = element.getElementID().toString().split(",")[0];
                xOfCode = String.valueOf(element.getX());
                yOfCode = String.valueOf(element.getY());
                positionOfCodeColumn.add(pageNumber + "," + xOfCode + "," + yOfCode);
            }
        }
        for (TextElement element : localOfficialStations) {
            pageNumber = element.getElementID().toString().split(",")[0];
            if (Integer.parseInt(pageNumber) < 5 || element.getText().replaceAll(" ", "").equals("")) {
                officialStations.remove(element);
                continue;
            }
            xOfCode = String.valueOf(element.getX());
            yOfCode = String.valueOf(element.getY());
            for (String position : positionOfCodeColumn) {
                pageNumberInPositionList = position.split(",")[0];
                xInPositionList = position.split(",")[1];
                yInPositionList = position.split(",")[2];
                if (!pageNumber.equals(pageNumberInPositionList)) continue;
                if (Double.parseDouble(yOfCode) <= Double.parseDouble(yInPositionList) + 0.04) {
                    officialStations.remove(element);
                    continue;
                }
                if (abs(Double.parseDouble(xOfCode) - Double.parseDouble(xInPositionList)) < 5) {
                    officialStations.remove(element);
                }
            }
        }
    }

    private void sortAfterConcat(List<TextElement> elements, float fontSize,double stationFonTolerance) {
        TextElement tempElement;
        for (int i = 0; i < elements.size() - 1; i++) {
            if (!elements.get(i).getElementID().toString().split(",")[0]
                    .equals(elements.get(i + 1).getElementID().toString().split(",")[0])) {
                continue;
            }
            if (elements.get(i).getFontSize() >= fontSize + stationFonTolerance) {
                continue;
            }
            if (abs(elements.get(i + 1).getFontSize() - fontSize) > stationFonTolerance) {
                continue;
            }
            if (elements.get(i + 1).getY() - elements.get(i).getY() < 0.002) {
                tempElement = elements.get(i);
                elements.set(i, elements.get(i + 1));
                elements.set(i + 1, tempElement);
            }
        }
    }

    private List<TextElement> readPDFText(File file) {
        List<TextElement> TextElementsInFile = new ArrayList<>();
        try (Document document = DocumentReader.getInstance().readDocument(new FilePdfSource(file))) {
            int index = 0;
            EnumerationProgress pages = document.getPages(null, index);
            while (pages.hasMoreElements()) {
                Page page = pages.nextElement();
                List<DrawableElement> list = page.getElementList().getList();
                for (DrawableElement drawableElement : list) {
                    if (ElementType.Text.equals(drawableElement.getType())) {
                        TextElementsInFile.add((TextElement) drawableElement);
                    }
                }
            }
        } catch (PdfcException e) {
            e.printStackTrace();
        }
        return TextElementsInFile;
    }

    private void sortTextElements(List<TextElement> LocalTextElements,double wordsYOffset) {
        TextElement tempElement;
        String page_i;
        String page_j;

        for (int i = 0; i < LocalTextElements.size(); i++) {
            page_i = LocalTextElements.get(i).getElementID().toString().split(",")[0];
            for (int j = i + 1; j < LocalTextElements.size(); j++) {
                page_j = LocalTextElements.get(j).getElementID().toString().split(",")[0];
                if (Integer.parseInt(page_j) < Integer.parseInt(page_i)) {
                    tempElement = LocalTextElements.get(i);
                    LocalTextElements.set(i, LocalTextElements.get(j));
                    LocalTextElements.set(j, tempElement);
                }
            }
        }
//       ------------------------------------------------------------
        outerLoop:
        for (int i = 0; i < LocalTextElements.size(); i++) {
            page_i = LocalTextElements.get(i).getElementID().toString().split(",")[0];
            for (int j = i + 1; j < LocalTextElements.size(); j++) {
                page_j = LocalTextElements.get(j).getElementID().toString().split(",")[0];
                if (!page_i.equals(page_j)) continue outerLoop;
                if (LocalTextElements.get(j).getY() < LocalTextElements.get(i).getY() &&
                        abs(LocalTextElements.get(j).getY() - LocalTextElements.get(i).getY()) > wordsYOffset) {
                    tempElement = LocalTextElements.get(i);
                    LocalTextElements.set(i, LocalTextElements.get(j));
                    LocalTextElements.set(j, tempElement);
                }
            }
        }
//       ------------------------------------------------------------
        outerLoop:
        for (int i = 0; i < LocalTextElements.size(); i++) {
            page_i = LocalTextElements.get(i).getElementID().toString().split(",")[0];
            for (int j = i + 1; j < LocalTextElements.size(); j++) {
                page_j = LocalTextElements.get(j).getElementID().toString().split(",")[0];
                if (!page_i.equals(page_j)) continue outerLoop;
                if (abs(LocalTextElements.get(j).getY() - LocalTextElements.get(i).getY()) < wordsYOffset &&
                        LocalTextElements.get(j).getX() < LocalTextElements.get(i).getX()) {
                    tempElement = LocalTextElements.get(i);
                    LocalTextElements.set(i, LocalTextElements.get(j));
                    LocalTextElements.set(j, tempElement);
                }
            }
        }
    }

    private void printStations() {
        System.out.println(".".repeat(100) + "\nThere is same train in both file (" + trainNumber + ")," +
                "Writing results in C:/PDFC/logs/CompareTest.xml ... \n" + ".".repeat(100));
        StringBuilder msg = new StringBuilder();
        String stationNameInFile1;
        String stationNameInFile2;
        for (TrainStation stationInPdf1 : trainStationsInPdf_1) {
            stationNameInFile1 = stationInPdf1.getName().getText().replaceAll("\\d" + "." + "\\d", "").replaceAll("\\d", "");
            msg.append("\n").append("Station name in pdf_1 = ").
                    append(stationNameInFile1).
                    append(".".repeat(90 - 2 * (24 + stationNameInFile1.length())));
            for (TrainStation stationInPdf2 : trainStationsInPdf_2) {
                stationNameInFile2 = stationInPdf2.getName().getText().replaceAll("\\d" + "." + "\\d", "").replaceAll("\\d", "");
                if (!stationNameInFile1.replace(" ", "").replace(".", "").
                        equals(stationNameInFile2.replace(" ", "").replace(".", ""))) {
                    continue;
                }
                msg.append("Station name in pdf_2 = ").append(stationNameInFile2).append("\n");

                msg.append(genParamMessage(stationInPdf1.getParam1(), stationInPdf2.getParam1(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam2(), stationInPdf2.getParam2(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam3(), stationInPdf2.getParam3(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam4(), stationInPdf2.getParam4(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam5(), stationInPdf2.getParam5(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam6(), stationInPdf2.getParam6(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam7(), stationInPdf2.getParam7(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam8(), stationInPdf2.getParam8(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam9(), stationInPdf2.getParam9(), stationInPdf1, stationInPdf2));
                msg.append(genParamMessage(stationInPdf1.getParam10(), stationInPdf2.getParam10(), stationInPdf1, stationInPdf2));

                msg.append("page in pdf_1 : ").append(Integer.parseInt(stationInPdf1.getName().getElementID().toString().split(",")[0]) + 1);
                msg.append(".".repeat(56));
                msg.append("page in pdf_2 : ").append(Integer.parseInt(stationInPdf2.getName().getElementID().toString().split(",")[0]) + 1);
            }
            LOGGER.info(msg.toString());
            msg = new StringBuilder();
        }
    }

    private float StationFontSize(List<TextElement> TextElementsInFile, int StationFontSizeIndex) {
        float fontSize = 0.0F;
        List<TextElement> uniqueTextBasedOnFont = TextElementsInFile.stream()
                .collect(collectingAndThen(toCollection(() ->
                        new TreeSet<>(comparingDouble(TextElement::getFontSize))), ArrayList::new));

        Comparator<TextElement> comparator = Comparator.comparing(TextElement::getFontSize);

        Stream<TextElement> TextElementStream = uniqueTextBasedOnFont.stream().sorted(comparator);
        List<TextElement> sortedTextElements = TextElementStream.collect(Collectors.toList());

        Collections.reverse(sortedTextElements);

        if(fontInfo != null && fontInfo.equals(sortedTextElements.get(StationFontSizeIndex).getFontInfo())) {
            fontSize = sortedTextElements.get(StationFontSizeIndex).getFontSize();
        } else {
            fontSize = fontSize + sortedTextElements.get(StationFontSizeIndex).getFontSize();
        }
        return fontSize;
    }

    private void concatTextElements(List<TextElement> LocalTextElements,double wordsYOffset,double stationFonTolerance) {
        int spaceCounter;
        String page_i;
        String page_j;

        for (int i = 0; i < LocalTextElements.size(); i++) {
            spaceCounter = 0;
            for (int j = i + 1; j < LocalTextElements.size(); j++) {
                if (!LocalTextElements.get(j).getText().equals(" ")) {
                    spaceCounter = 0;
                } else if (LocalTextElements.get(j).getText().equals(" ")) {
                    spaceCounter++;
                }
                if (spaceCounter > 2) {
                    LocalTextElements.remove(j);
                    continue;
                }
                page_i = LocalTextElements.get(i).getElementID().toString().split(",")[0];
                page_j = LocalTextElements.get(j).getElementID().toString().split(",")[0];
                if (!page_i.equals(page_j) ||
                        abs(LocalTextElements.get(i).getY() - LocalTextElements.get(j).getY()) > wordsYOffset ||
                        abs(LocalTextElements.get(i).getFontSize() - LocalTextElements.get(j).getFontSize()) > stationFonTolerance ||
                        !LocalTextElements.get(i).getFontInfo().equals(LocalTextElements.get(j).getFontInfo())) {
                    continue;
                }

                LocalTextElements.get(i).setText(LocalTextElements.get(i).getText() + LocalTextElements.get(j).getText());
                LocalTextElements.remove(j);
                j -= 1;
            }
        }
    }

    private TextElement findStationParams(TextElement goalParam, TextElement testParam,
                                          TrainStation trainStationsInPdf,double wordsYOffset) {
        TextElement tempPram;
        String testParamText = testParam == null ? "" : testParam.getText();
        double testParamY = testParam == null ? 0 : testParam.getY();

        if (trainStationsInPdf.getName().getText().contains(goalParam.getText())) {
            return goalParam;
        }

        if (trainStationsInPdf.getParam1() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam1().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam1();
                trainStationsInPdf.setParam1(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam1().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam1().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam1().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam1().getText());
                trainStationsInPdf.setParam1(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam2() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam2().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam2();
                trainStationsInPdf.setParam2(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam2().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam2().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam2().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam2().getText());
                trainStationsInPdf.setParam2(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam3() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam3().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam3();
                trainStationsInPdf.setParam3(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam3().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam3().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam3().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam3().getText());
                trainStationsInPdf.setParam3(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam4() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam4().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam4();
                trainStationsInPdf.setParam4(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam4().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam4().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam4().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam4().getText());
                trainStationsInPdf.setParam4(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam5() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam5().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam5();
                trainStationsInPdf.setParam5(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam5().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam5().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam5().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam5().getText());
                trainStationsInPdf.setParam5(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam6() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam6().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam6();
                trainStationsInPdf.setParam6(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam6().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam6().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam6().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam6().getText());
                trainStationsInPdf.setParam6(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam7() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam7().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam7();
                trainStationsInPdf.setParam7(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam7().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam7().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam7().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam7().getText());
                trainStationsInPdf.setParam7(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam8() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam8().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam8();
                trainStationsInPdf.setParam8(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam8().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam8().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam8().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam8().getText());
                trainStationsInPdf.setParam8(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam9() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam9().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam9();
                trainStationsInPdf.setParam9(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam9().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam9().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam9().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam9().getText());
                trainStationsInPdf.setParam9(null);
                return testParam;
            }
        }

        if (trainStationsInPdf.getParam10() != null) {
            if (goalParam.getText().trim().equals(trainStationsInPdf.getParam10().getText())) {
                tempPram = testParam;
                testParam = trainStationsInPdf.getParam10();
                trainStationsInPdf.setParam10(tempPram);
                return testParam;
            }

            if (Objects.equals(testParamText + trainStationsInPdf.getParam10().getText(), goalParam.getText().trim()) ||
                    (abs(testParamY - trainStationsInPdf.getParam10().getY()) < wordsYOffset &&
                            !testParamText.replaceAll(" ", "").equals(trainStationsInPdf.getParam10().getText().replaceAll(" ", "")))) {
                Objects.requireNonNull(testParam).setText(testParam.getText() + trainStationsInPdf.getParam10().getText());
                trainStationsInPdf.setParam10(null);
                return testParam;
            }
        }

        return testParam;
    }

    private List<TrainStation> genStationArray(List<TextElement> inPutList, float fontSize,double stationFonTolerance,double stationXOffset) {
        List<TrainStation> outPutList = new ArrayList<>();
        TrainStation trainStation = new TrainStation();
        TextElement previousStation = null;
        TextElement previousParam = null;
        int paramCounter = 0;
        boolean isStationStarted = false;
        double pageNumberX = 0;
        double xOffset = 0;

        for (TextElement element : inPutList) {
            if (element.getText().matches("- \\d -") ||
                    (element.getText().matches("\u00B1 \\d \u00B1"))) {
                if (pageNumberX != 0 && isStationStarted) {
                    if (element.getX() <= pageNumberX) {
                        xOffset = abs(element.getX() - pageNumberX);
                    } else {
                        xOffset = element.getX() - pageNumberX;
                    }
                }
                pageNumberX = element.getX();
            }
            if (abs(element.getFontSize() - fontSize) < stationFonTolerance &&
                    !(previousStation != null && previousStation.getText().equals(element.getText())) &&
                    !isNumeric(element.getText()) &&
                    isAccepted(element.getText())) {
                isStationStarted = true;
                previousStation = element;
//                element.setText(element.getText().replaceAll("\\d" + "." + "\\d", "").replaceAll("\\d", ""));
                if (trainStation.getName() != null) {
                    outPutList.add(trainStation);
                }
                trainStation = new TrainStation(element);
                if (!thereIsMoreStation(inPutList, inPutList.indexOf(element), fontSize,stationFonTolerance)) {
                    outPutList.add(trainStation);
                }
                xOffset = 0;
                previousParam = null;
                paramCounter = 1;
            }
            if (element.getFontSize() < fontSize && isStationStarted &&
                    isAccepted(element.getText()) &&
                    abs(element.getX() - trainStation.getName().getX()) < stationXOffset + xOffset &&
                    !(previousParam != null && previousParam.getText().equals(element.getText()))) {
                previousParam = element;
                switch (paramCounter) {
                    case 1:
                        trainStation.setParam1(element);
                        break;
                    case 2:
                        trainStation.setParam2(element);
                        break;
                    case 3:
                        trainStation.setParam3(element);
                        break;
                    case 4:
                        trainStation.setParam4(element);
                        break;
                    case 5:
                        trainStation.setParam5(element);
                        break;
                    case 6:
                        trainStation.setParam6(element);
                        break;
                    case 7:
                        trainStation.setParam7(element);
                        break;
                    case 8:
                        trainStation.setParam8(element);
                        break;
                    case 9:
                        trainStation.setParam9(element);
                        break;
                    case 10:
                        trainStation.setParam10(element);
                        break;
                }
                paramCounter++;
            }
        }
        return outPutList;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String genParamMessage(TextElement param_1, TextElement param_2, TrainStation stationInPdf1, TrainStation stationInPdf2) {
        String msg = "";
        if (param_1 != null && param_2 != null) {
            if (!param_1.getText().replaceAll(" ", "").equals(param_2.getText().replaceAll(" ", "")) &&
                    !isCharachtersEqual(param_1.getText(), param_2.getText())) {
                if (param_1.getText().length() <= param_2.getText().length()) {
                    param_1 = findStationParams(param_2, param_1, stationInPdf1,wordsYOffsetInFile_1);
                }
                if (param_1.getText().length() > param_2.getText().length()) {
                    param_2 = findStationParams(param_1, param_2, stationInPdf2,wordsYOffsetInFile_2);
                }
                if (param_1.getText().toLowerCase().contains("(in ") && !param_2.getText().toLowerCase().contains("(in ")) {
                    param_2.setText(param_1.getText());
                }
                if (param_2.getText().toLowerCase().contains("(in ") && !param_1.getText().toLowerCase().contains("(in ")) {
                    param_1.setText(param_2.getText());
                }
            }
            msg = genMsg(param_1, param_2);
        } else if (param_1 == null && param_2 != null) {
            param_1 = findStationParams(param_2, null, stationInPdf1,wordsYOffsetInFile_1);
            if (param_1 != null) {
                msg = genMsg(param_1, param_2);
            } else {
                msg = "*".repeat(8) + ".".repeat(90 - (param_2.getText().length() + 19)) + param_2.getText() + "(Not Equal)" + "\n";
            }
        } else if (param_1 != null) {
            param_2 = findStationParams(param_1, null, stationInPdf2,wordsYOffsetInFile_2);
            if (param_2 != null) {
                msg = genMsg(param_1, param_2);
            } else {
                msg = param_1.getText() + ".".repeat(90 - (param_1.getText().length() + 19)) + "*".repeat(8) + "(Not Equal)" + "\n";
            }
        }
        return msg;
    }

    private String genMsg(TextElement param_1, TextElement param_2) {
        String msg;
        msg = param_1.getText() + ".".repeat(90 - (param_1.getText().length() + param_2.getText().length())) + param_2.getText()
                + ((param_1.getText().trim().equals(param_2.getText().trim()) ||
                isCharachtersEqual(param_1.getText(), param_2.getText())) ? "" : "(Not Equal)") + "\n";
        return msg;

    }

    private boolean isCharachtersEqual(String param_1, String param_2) {
        for (int i = 0; i < param_1.length(); i++) {
            if (!param_2.contains(String.valueOf(param_1.charAt(i))) &&
                    !String.valueOf(param_1.charAt(i)).equals(" ")) {
                return false;
            }
        }
        for (int i = 0; i < param_2.length(); i++) {
            if (!param_1.contains(String.valueOf(param_2.charAt(i))) &&
                    !String.valueOf(param_2.charAt(i)).equals(" ")) {
                return false;
            }
        }
        return true;
    }

    private boolean isAccepted(String input) {
        if (input.equals("Powered by Worldline")) {
            return false;
        }
        if (input.equals("456123456")) {
            return false;
        }
        if (input.equals("EBU")) {
            return false;
        }
        if (input.matches("- \\d -")) {
            return false;
        }
        if (input.matches("± \\d ±")) {
            return false;
        }
        if (input.contains("\u00B1")) {
            return false;
        }
        if (input.equals("Vorbemerkungen")) {
            return false;
        }
        if (input.equals("Funkbereiche")) {
            return false;
        }
        return !input.contains("GSM-R");
    }

    private void setXPositionForStations(List<TextElement> elements, float fontSize,double stationFonTolerance,double stationReplacementValue) {
        double pageHeaderXPosition = 0;

        for (TextElement element : elements) {
            if (element.getText().matches("- \\d -") ||
                    (element.getText().matches("\u00B1 \\d \u00B1"))) {

                pageHeaderXPosition = element.getX();
            }
            if (abs(element.getFontSize() - fontSize) < stationFonTolerance) {
                if (pageHeaderXPosition > element.getX()) {
                    element.setX(pageHeaderXPosition - stationReplacementValue);
                } else {
                    element.setX(pageHeaderXPosition + stationReplacementValue);
                }
            }
        }
    }

    private boolean thereIsMoreStation(List<TextElement> elements, int index, float fontSize,double stationFonTolerance) {
        for (int i = index + 1; i < elements.size(); i++) {
            if (abs(elements.get(i).getFontSize() - fontSize) < stationFonTolerance) {
                return true;
            }
        }
        return false;
    }

    /*private void writeFile(List<TextElement> elements, String pageIndex, String filename) {
        try {
            FileWriter fileWriter = new FileWriter(filename);
            for (TextElement element : elements) {
                if (element.getElementID().toString().split(",")[0].equals(pageIndex)) {
                    fileWriter.write("Text = " + element.getText() + "\n" +
                            "ID = " + element.getElementID() + "\n" +
                            "X = " + element.getX() + "\n" +
                            "Y = " + element.getY() + "\n" +
                            "font-size = " + element.getFontSize() + "\n" +
                            "fontName = " + element.getFontInfo().getFontName() + "\n" +
                            "fontStyle = " + element.getFontInfo().getStyle() +
                            "\n" + ".".repeat(90) + "\n");
                }
            }
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
        }
    }*/
}
