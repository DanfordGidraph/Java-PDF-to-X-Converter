package com.staxtech.opensource.pdfmanager;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;
import com.qoppa.pdf.PDFException;
import com.qoppa.pdfOptimizer.OptSettings;
import com.qoppa.pdfOptimizer.OptimizeResults;
import com.qoppa.pdfOptimizer.PDFOptimizer;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.fit.pdfdom.PDFDomTree;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class PDFOperations {
    private static String SOURCE_FILE = "";
    private static String DEST_DIR = "";
    private static String BINARIES_DIR = "src/main/resources/binaries/";

    PDFOperations(String sourceFile, String destPath){
        SOURCE_FILE = sourceFile;
        DEST_DIR = destPath;
    }

    @SuppressWarnings("unused")
    String generateHTMLFromPDF() throws IOException, ParserConfigurationException {
        String file_save_path = DEST_DIR+"_webpage.html";
        PDDocument pdf = PDDocument.load(new File(SOURCE_FILE));
        Writer result = new PrintWriter(file_save_path, "utf-8");
        new PDFDomTree().writeText(pdf, result);
        result.close();
        return file_save_path;
    }

    @SuppressWarnings("unused")
    String [] generateImageFromPDF(String extension) throws IOException {
        PDDocument document = PDDocument.load(new File(SOURCE_FILE));
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        final int numberOfPages = document.getNumberOfPages();
        String[] file_save_paths = new String[numberOfPages];
        for (int page = 0; page < numberOfPages; ++page) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            String file_save_path = String.format(DEST_DIR+"_page_%d.%s", page + 1, extension);
            file_save_paths[page] = file_save_path;
            ImageIOUtil.writeImage(bim, file_save_path, 300);
        }
        document.close();
        return file_save_paths;
    }

    @SuppressWarnings("unused")
    String generateTextFromPDF() throws FileNotFoundException, IOException {
        String file_save_path = DEST_DIR+"_text.txt";
        File f = new File(SOURCE_FILE);
        String parsedText;
        PDFParser parser = new PDFParser(new RandomAccessFile(f, "r"));
        parser.parse();
        COSDocument cosDoc = parser.getDocument();
        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDDocument pdDoc = new PDDocument(cosDoc);
        parsedText = pdfStripper.getText(pdDoc);
        PrintWriter pw = new PrintWriter(file_save_path);
        pw.print(parsedText);
        pw.close();
        return file_save_path;
    }

    @SuppressWarnings("unused")
    String generateWordFromPDF() throws FileNotFoundException, IOException {
        String file_save_path = DEST_DIR+"_word.docx";
        XWPFDocument doc = new XWPFDocument();
        //Get source pdf
        com.itextpdf.text.pdf.PdfReader pdfReader = new com.itextpdf.text.pdf.PdfReader(SOURCE_FILE);
        PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
        for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
            TextExtractionStrategy strategy =
                    parser.processContent(i, new SimpleTextExtractionStrategy());
            String text = strategy.getResultantText();
            XWPFParagraph p = doc.createParagraph();
            XWPFRun run = p.createRun();
            run.setText(text);
            run.addBreak(BreakType.PAGE);
        }
        FileOutputStream out = new FileOutputStream(file_save_path);
        doc.write(out);
        doc.close();
        return file_save_path;
    }

    @SuppressWarnings("unused")
    String[] splitPDFIntoPages () throws IOException {
        File file = new File(SOURCE_FILE);
        PDDocument doc = PDDocument.load(file);
        Splitter splitter = new Splitter();
        List<PDDocument> Pages = splitter.split(doc);
        List<String> file_save_paths = new ArrayList<>();
        int idx = 0;
        for (Iterator<PDDocument> iterator = Pages.listIterator(); iterator.hasNext(); idx++) {
            String file_save_path = DEST_DIR + "_page_" + (idx + 1) + ".pdf";
            file_save_paths.add(file_save_path);
            PDDocument pd = iterator.next();
            pd.save(file_save_path);
        }
        return file_save_paths.toArray(new String[0]);
    }

    @SuppressWarnings("unused")
    String encodePDFToBase64() throws IOException {
        String file_save_path = DEST_DIR + "_base64.txt";
        OutputStream os = Base64.getEncoder().wrap(new FileOutputStream(file_save_path));
        FileInputStream fis = new FileInputStream(SOURCE_FILE);
        byte[] bytes = new byte[1024];
        int read;
        while ((read = fis.read(bytes)) > -1) { os.write(bytes, 0, read); }
        fis.close();
        return file_save_path;
    }

    @SuppressWarnings("unused")
    String optimizePDF(float resizeFactor) throws IOException, DocumentException {
        String file_save_path = DEST_DIR + "_optimized.pdf";
        PdfName key = new PdfName("ITXT_SpecialId");
        PdfName value = new PdfName("123456789");
        // Read the file
        PdfReader reader = new PdfReader(SOURCE_FILE);
        int n = reader.getXrefSize();
        PdfObject object;
        PRStream stream;
        // Look for image and manipulate image stream
        for (int i = 0; i < n; i++) {
            object = reader.getPdfObject(i);
            if (object == null || !object.isStream())
                continue;
            stream = (PRStream)object;
            // if (value.equals(stream.get(key))) {
            PdfObject pdfsubtype = stream.get(PdfName.SUBTYPE);
            System.out.println(stream.type());
            if (pdfsubtype != null && pdfsubtype.toString().equals(PdfName.IMAGE.toString())) {
                PdfImageObject image = new PdfImageObject(stream);
                BufferedImage bi = image.getBufferedImage();
                if (bi == null) continue;
                int width = (int)(bi.getWidth() * resizeFactor);
                int height = (int)(bi.getHeight() * resizeFactor);
                BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                AffineTransform at = AffineTransform.getScaleInstance(resizeFactor, resizeFactor);
                Graphics2D g = img.createGraphics();
                g.drawRenderedImage(bi, at);
                ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
                ImageIO.write(img, "JPG", imgBytes);
                stream.clear();
                stream.setData(imgBytes.toByteArray(), false, PRStream.BEST_COMPRESSION);
                stream.put(PdfName.TYPE, PdfName.XOBJECT);
                stream.put(PdfName.SUBTYPE, PdfName.IMAGE);
                stream.put(key, value);
                stream.put(PdfName.FILTER, PdfName.DCTDECODE);
                stream.put(PdfName.WIDTH, new PdfNumber(width));
                stream.put(PdfName.HEIGHT, new PdfNumber(height));
                stream.put(PdfName.BITSPERCOMPONENT, new PdfNumber(8));
                stream.put(PdfName.COLORSPACE, PdfName.DEVICERGB);
            }
        }
        // Save altered PDF
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(file_save_path));
        stamper.close();
        reader.close();
        return file_save_path;
    }

    String optimizePDFWithPython() throws IOException, PDFException {
        String file_save_path = DEST_DIR + "_py_optimized.pdf";
        PDFOptimizer pdfOptimize = new PDFOptimizer(SOURCE_FILE, null);
        OptSettings options = new OptSettings();
        options.setDiscardAltImages(true);
        options.setDiscardAnnotations(true);
        options.setDiscardBookmarks(true);
        options.setDiscardDocumentInfo(true);
        options.setDiscardFileAttachments(true);
        options.setDiscardFormFields(true);
        options.setDiscardJSActions(true);
        options.setDiscardPageThumbnails(true);
        options.setDiscardXMPData(true);
        options.setDiscardUnusedResources(true);
        options.setDiscardLinks(true);
        options.setClearSignatures(true);
        options.setFlateUncompressedStreams(true);
        options.setMergeDuplicateFonts(true);
        options.setMergeDuplicateImages(true);
        OptimizeResults optimizeResults = pdfOptimize.optimize(options, file_save_path);
        return file_save_path;
    }
}
