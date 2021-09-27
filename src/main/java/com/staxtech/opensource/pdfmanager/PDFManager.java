package com.staxtech.opensource.pdfmanager;
import com.itextpdf.text.DocumentException;
import com.qoppa.pdf.PDFException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class PDFManager {

    private static final String INPUT_FILE =  "src/main/resources/input_files/c.pdf"; // path to file to be READ from;
    private static final String OUTPUT_DIRECTORY =  "src/main/resources/output_files/"; // path to file to be READ from;
    private static final float RESIZE_FACTOR = (float) 0.7;

    public static void main(String[] args) {
        try {
            String FILE_NAME = new File(INPUT_FILE).getName().replace(".pdf", "");
            String FILE_OUTPUT_DIRECTORY = Files.createDirectories(Paths.get(OUTPUT_DIRECTORY + FILE_NAME)).toString() + "/" + FILE_NAME;
            PDFOperations pdfOperations = new PDFOperations(INPUT_FILE,FILE_OUTPUT_DIRECTORY);
            Object convertedFile = ExecuteOperation(pdfOperations,"PDF_TO_PDF_PAGES");
            if(convertedFile.getClass().isArray()) {
                String [] convertedFiles = (String[]) convertedFile;
//                System.out.print(Arrays.toString(convertedFiles));
                for (String file : convertedFiles) {
                    String fileName = new File(file).getName().replaceFirst("[.][^.]+$", "");
                    String fileOutputDir = Files.createDirectories(Paths.get(OUTPUT_DIRECTORY + FILE_NAME)).toString() + "/" + fileName;
                    String optimizedFile = (String) ExecuteOperation(new PDFOperations(file,fileOutputDir),"OPTIMIZE_PDF");
                    System.out.print("Finished optimizing: " + file + " Into " + optimizedFile + "\n");
                }
            } else System.out.print((String) convertedFile);
        } catch (IOException | ParserConfigurationException | DocumentException | PDFException e) {
            e.printStackTrace();
        }
    }

    static Object ExecuteOperation(PDFOperations pdfOperations, String Type) throws IOException, ParserConfigurationException, DocumentException, PDFException {
        switch (Type){
            case "PDF_TO_HTML":
                return pdfOperations.generateHTMLFromPDF();
            case "PDF_TO_IMAGES":
                return pdfOperations.generateImageFromPDF("png");
            case "PDF_TO_TEXT":
                return pdfOperations.generateTextFromPDF();
            case "PDF_TO_WORD":
                return pdfOperations.generateWordFromPDF();
            case "PDF_TO_PDF_PAGES":
                return pdfOperations.splitPDFIntoPages();
            case "PDF_TO_BASE64":
                return pdfOperations.encodePDFToBase64();
            case "OPTIMIZE_PDF":
                return pdfOperations.optimizePDF(RESIZE_FACTOR);
            case "OPTIMIZE_PDF_WITH_PYTHON":
                return pdfOperations.optimizePDFWithPython();
            default:
                return "Passed Type did not match any of the expected types";
        }
    }


}
