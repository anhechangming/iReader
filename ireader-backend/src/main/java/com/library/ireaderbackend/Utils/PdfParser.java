package com.library.ireaderbackend.Utils;

import com.library.ireaderbackend.entity.BookContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 兼容 PDFBox 2.0.x 的 PDF 电子书解析工具类
 * 解决类型转换、方法访问权限问题
 */
public class PdfParser {

    // 章节标题正则（覆盖多级标题）
    private static final String CHAPTER_REGEX =
            "^(第[零一二三四五六七八九十百千万0-9]{1,5}[章回节卷部集篇]|([0-9]+\\.){1,3}[0-9]+\\s+.*|附录[ABCDEFGHIJKLMNOPQRSTUVWXYZ].*)$";

    // 最小章节内容长度（过滤空章节）
    private static final int MIN_CHAPTER_CONTENT_LEN = 50;

    /**
     * 解析 PDF 为电子书章节列表
     */
    public static List<BookContent> parseEbook(File file, Long bookId) throws IOException {
        List<BookContent> chapters = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(file)) {
            // 1. 优先按 PDF 原生目录（Outline）解析
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            if (outline != null && outline.getFirstChild() != null) {
                System.out.println("[PDF解析] 发现原生目录，按目录层级解析");
                parseByOutline(doc, outline.getFirstChild(), chapters, bookId, 1);
            }
            // 2. 无原生目录时，按页面文本正则切分
            else {
                System.out.println("[PDF解析] 无原生目录，按文本结构正则切分");
                parseByTextStructure(doc, chapters, bookId);
            }
            // 3. 过滤无效章节
            filterInvalidChapters(chapters);
        }
        return chapters;
    }

    /**
     * 按 PDF 原生目录（Outline）解析（修复类型转换和权限问题）
     */
    private static void parseByOutline(PDDocument doc, PDOutlineItem outlineItem,
                                       List<BookContent> chapters, Long bookId, int order) throws IOException {
        if (outlineItem == null) return;

        // 1. 获取当前目录项对应的页面（修复 PDDestination 类型转换问题）
        PDDestination destination = outlineItem.getDestination();
        if (destination == null) {
            // 处理通过动作关联的目录（避免调用 getAction() 导致的权限问题）
            System.out.println("[PDF解析] 跳过无直接目标的目录项：" + outlineItem.getTitle());
            return;
        }

        // 确保目标是页面类型（避免类型转换错误）
        if (!(destination instanceof PDPageDestination)) {
            System.out.println("[PDF解析] 跳过非页面目标的目录项：" + outlineItem.getTitle());
            return;
        }
        PDPageDestination pageDestination = (PDPageDestination) destination;
        PDPage startPage = pageDestination.getPage();
        if (startPage == null) {
            System.out.println("[PDF解析] 跳过无效页面的目录项：" + outlineItem.getTitle());
            return;
        }

        // 2. 定位章节起始页和结束页
        int startPageNum = doc.getPages().indexOf(startPage) + 1; // 页码从1开始
        int endPageNum = getOutlineEndPage(doc, outlineItem);

        // 3. 提取当前章节文本
        PDFTextStripper stripper = new CustomTextStripper();
        stripper.setStartPage(startPageNum);
        stripper.setEndPage(endPageNum);
        String chapterContent = stripper.getText(doc).trim();

        // 4. 构建章节对象（简化层级计算，避免 getParent() 权限问题）
        String chapterTitle = outlineItem.getTitle().trim();
        // 注：因 getParent() 非公开，此处简化层级展示（如需多级目录需升级 PDFBox 版本）
        BookContent chapter = new BookContent();
        chapter.setBookId(bookId);
        chapter.setChapterTitle(chapterTitle);
        chapter.setChapterContent(chapterContent);
        chapter.setChapterOrder(order);
        chapters.add(chapter);
        System.out.printf("[PDF解析] 目录章节：%d. %s（第%d-%d页）%n", order, chapterTitle, startPageNum, endPageNum);

        // 5. 递归处理子目录和同级目录
        parseByOutline(doc, outlineItem.getFirstChild(), chapters, bookId, order);
        parseByOutline(doc, outlineItem.getNextSibling(), chapters, bookId, order + 1);
    }

    /**
     * 按文本结构正则切分（无目录时使用）
     */
    private static void parseByTextStructure(PDDocument doc, List<BookContent> chapters, Long bookId) throws IOException {
        PDFTextStripper stripper = new CustomTextStripper();
        stripper.setSortByPosition(true); // 按位置排序，避免文本乱序

        // 逐页提取文本
        StringBuilder fullText = new StringBuilder();
        int totalPages = doc.getNumberOfPages();
        for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
            stripper.setStartPage(pageNum);
            stripper.setEndPage(pageNum);
            fullText.append(stripper.getText(doc)).append("\n");
        }

        // 按行分割并匹配章节标题
        String[] lines = fullText.toString().split("\r?\n");
        StringBuilder currentContent = new StringBuilder();
        String currentTitle = "序章";
        int order = 1;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.length() < 2) {
                currentContent.append(line).append("\n");
                continue;
            }

            // 匹配章节标题，切分章节
            if (trimmedLine.matches(CHAPTER_REGEX)) {
                if (currentContent.length() > 0) {
                    addChapter(chapters, bookId, currentTitle, currentContent.toString(), order++);
                    currentContent.setLength(0);
                }
                currentTitle = trimmedLine;
            }
            currentContent.append(line).append("\n");
        }

        // 保存最后一章
        if (currentContent.length() > 0) {
            addChapter(chapters, bookId, currentTitle, currentContent.toString(), order);
        }
    }

    // ------------------------------ 工具方法（修复权限和类型问题） ------------------------------

    /**
     * 获取目录项的结束页码（避免依赖 getNextSibling() 的复杂逻辑）
     */
    private static int getOutlineEndPage(PDDocument doc, PDOutlineItem currentItem) {
        PDOutlineItem nextSibling = currentItem.getNextSibling();
        if (nextSibling != null) {
            try {
                PDDestination nextDest = nextSibling.getDestination();
                if (nextDest instanceof PDPageDestination) {
                    PDPage nextPage = ((PDPageDestination) nextDest).getPage();
                    if (nextPage != null) {
                        return doc.getPages().indexOf(nextPage); // 下一页的前一页
                    }
                }
            } catch (IOException e) {
                System.out.println("[PDF解析] 获取目录目标页面失败，跳过该目录项：" + e.getMessage());
            }
        }
        return doc.getNumberOfPages(); // 最后一个目录项，结束页为总页数
    }


    /**
     * 构建章节对象并添加到列表
     */
    private static void addChapter(List<BookContent> chapters, Long bookId,
                                   String title, String content, int order) {
        String trimmedContent = content.trim();
        if (trimmedContent.length() < MIN_CHAPTER_CONTENT_LEN) {
            System.out.println("[PDF解析] 跳过过短章节：" + title);
            return;
        }

        BookContent chapter = new BookContent();
        chapter.setBookId(bookId);
        chapter.setChapterTitle(title);
        chapter.setChapterContent(trimmedContent);
        chapter.setChapterOrder(order);
        chapters.add(chapter);
    }

    /**
     * 过滤无效章节
     */
    private static void filterInvalidChapters(List<BookContent> chapters) {
        chapters.removeIf(chapter -> {
            String content = chapter.getChapterContent().trim();
            return content.isEmpty() || content.length() < MIN_CHAPTER_CONTENT_LEN;
        });
        // 重新排序章节序号
        for (int i = 0; i < chapters.size(); i++) {
            chapters.get(i).setChapterOrder(i + 1);
        }
    }

    /**
     * 自定义文本提取器（优化换行和文本顺序）
     */
    private static class CustomTextStripper extends PDFTextStripper {
        public CustomTextStripper() throws IOException {
            super();
            this.setSortByPosition(true); // 按位置排序，解决文本乱序
            this.setLineSeparator("\n");
            this.setWordSeparator(" ");
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            super.writeString(text, textPositions);
        }
    }
}