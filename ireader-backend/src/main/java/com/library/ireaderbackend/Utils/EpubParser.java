package com.library.ireaderbackend.Utils;

import com.library.ireaderbackend.entity.BookContent;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * EpubParser 修正版（封面路径统一处理）
 */
public class EpubParser {
    public static List<BookContent> parseEpubFile(File epubFile, Long bookId, String staticImgDir) throws Exception {
        System.out.println("[EPUB] 开始解析文件: " + epubFile.getAbsolutePath() + " bookId=" + bookId);
        List<BookContent> chapters = new ArrayList<>();
        String webBase = "/static/book/" + bookId + "/"; // 前端访问基准路径

        try (InputStream in = new FileInputStream(epubFile)) {
            Book epub = new EpubReader().readEpub(in);
            System.out.println("[EPUB] 成功读取 EPUB: " + epub.getTitle());

            // 1) 导出所有资源（图片）
            int imgCount = 0;
            for (Resource r : epub.getResources().getAll()) {
                String href = r.getHref();
                if (href == null) continue;
                String lower = href.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                        || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) {

                    // 所有图片统一导出到 Images/ 下
                    String outPath = "Images/" + new File(href).getName();
                    File outFile = new File(staticImgDir, outPath.replace("/", File.separator));
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(r.getData());
                        imgCount++;
                        System.out.println("[EPUB] 导出图片: " + outFile.getAbsolutePath());
                    } catch (Exception e) {
                        System.out.println("[EPUB] 导出图片失败: " + outFile.getAbsolutePath() + " -> " + e.getMessage());
                    }
                }
            }
            System.out.println("[EPUB] 共导出图片: " + imgCount);

            // 2) 构建 TOC href -> title 映射
            Map<String, String> href2title = new HashMap<>();
            buildHrefTitleMap(epub.getTableOfContents().getTocReferences(), href2title);
            Map<String, String> normalizedHref2Title = new HashMap<>();
            for (Map.Entry<String, String> e : href2title.entrySet()) {
                normalizedHref2Title.put(normalizePath(e.getKey()), e.getValue());
            }

            // 3) 处理 spine（章节）
            int order = 1;
            boolean skipFirstSpine = true; // 跳过第一章（封面）
            for (SpineReference spRef : epub.getSpine().getSpineReferences()) {
                if (spRef.getResource() == null) continue;

                if (skipFirstSpine) {
                    skipFirstSpine = false;
                    continue;
                }

                Resource res = spRef.getResource();
                String href = res.getHref();
                String normalizedHref = normalizePath(href);
                String encoding = res.getInputEncoding() == null ? Charset.forName("UTF-8").name() : res.getInputEncoding();
                String rawHtml = new String(res.getData(), encoding);

                Document doc = Jsoup.parse(rawHtml);

                // 替换 img src
                for (Element img : doc.select("img")) {
                    String src = img.attr("src");
                    if (src == null || src.trim().isEmpty()) continue;
                    String lowSrc = src.toLowerCase();
                    if (lowSrc.startsWith("http://") || lowSrc.startsWith("https://") || lowSrc.startsWith("data:")) continue;
                    String fileName = new File(src).getName();
                    String newWebPath = webBase + "Images/" + fileName;
                    img.attr("src", newWebPath);
                }

                // 获取章节标题
                String title = normalizedHref2Title.get(normalizedHref);
                if (title == null || title.trim().isEmpty()) {
                    String htmlTitle = doc.title();
                    if (htmlTitle != null && !htmlTitle.trim().isEmpty()) title = htmlTitle.trim();
                    else {
                        Element h1 = doc.selectFirst("h1");
                        if (h1 != null && !h1.text().trim().isEmpty()) title = h1.text().trim();
                    }
                }
                if (title == null || title.trim().isEmpty()) title = extractFileName(href);

                BookContent bc = new BookContent();
                bc.setBookId(bookId);
                bc.setChapterTitle(title);
                bc.setChapterContent(doc.html());
                bc.setChapterOrder(order++);  // 这里保证编号从 1 开始
                chapters.add(bc);

                System.out.println("[EPUB] 章节 " + (order-1) + ": " + title);
            }

            // 4) 处理封面图片（单独 export）
            if (epub.getCoverImage() != null) {
                Resource coverRes = epub.getCoverImage();
                File coverFile = new File(staticImgDir, "Images/cover.jpg");
                File parent = coverFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(coverFile)) {
                    fos.write(coverRes.getData());
                    System.out.println("[EPUB] 导出封面图片: " + coverFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println("[EPUB] 导出封面失败: " + e.getMessage());
                }
            }

            System.out.println("[EPUB] 共解析章节数: " + chapters.size());
        }

        return chapters;
    }


    public static List<BookContent> parseEpubToChapters(String classpathResource, Long bookId, String staticImgDir) throws Exception {
        System.out.println("[EPUB] 开始解析: " + classpathResource + " bookId=" + bookId);
        List<BookContent> chapters = new ArrayList<>();
        String webBase = "/static/book/" + bookId + "/"; // 前端访问基准路径

        try (InputStream in = EpubParser.class.getResourceAsStream(classpathResource)) {
            if (in == null) throw new RuntimeException("[EPUB] 资源未找到: " + classpathResource);

            Book epub = new EpubReader().readEpub(in);
            System.out.println("[EPUB] 成功读取 EPUB: " + epub.getTitle());

            // 1) 导出所有资源（图片）
            int imgCount = 0;
            for (Resource r : epub.getResources().getAll()) {
                String href = r.getHref();
                if (href == null) continue;
                String lower = href.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                        || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) {

                    // 所有图片统一导出到 Images/ 下
                    String outPath = "Images/" + new File(href).getName();
                    File outFile = new File(staticImgDir, outPath.replace("/", File.separator));
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(r.getData());
                        imgCount++;
                        System.out.println("[EPUB] 导出图片: " + outFile.getAbsolutePath());
                    } catch (Exception e) {
                        System.out.println("[EPUB] 导出图片失败: " + outFile.getAbsolutePath() + " -> " + e.getMessage());
                    }
                }
            }
            System.out.println("[EPUB] 共导出图片: " + imgCount);


            // 2) 构建 TOC href -> title 映射
            Map<String, String> href2title = new HashMap<>();
            buildHrefTitleMap(epub.getTableOfContents().getTocReferences(), href2title);
            Map<String, String> normalizedHref2Title = new HashMap<>();
            for (Map.Entry<String, String> e : href2title.entrySet()) {
                normalizedHref2Title.put(normalizePath(e.getKey()), e.getValue());
            }

            // 3) 处理 spine（章节）
            int order = 1;
            boolean skipFirstSpine = true; // 跳过第一章（封面）
            for (SpineReference spRef : epub.getSpine().getSpineReferences()) {
                if (spRef.getResource() == null) continue;

                if (skipFirstSpine) {
                    skipFirstSpine = false;
                    continue;
                }

                Resource res = spRef.getResource();
                String href = res.getHref();
                String normalizedHref = normalizePath(href);
                String encoding = res.getInputEncoding() == null ? Charset.forName("UTF-8").name() : res.getInputEncoding();
                String rawHtml = new String(res.getData(), encoding);

                Document doc = Jsoup.parse(rawHtml);

                // 替换 img src
                for (Element img : doc.select("img")) {
                    String src = img.attr("src");
                    if (src == null || src.trim().isEmpty()) continue;
                    String lowSrc = src.toLowerCase();
                    if (lowSrc.startsWith("http://") || lowSrc.startsWith("https://") || lowSrc.startsWith("data:")) continue;
                    String fileName = new File(src).getName();
                    String newWebPath = webBase + "Images/" + fileName;
                    img.attr("src", newWebPath);
                }

                // 获取章节标题
                String title = normalizedHref2Title.get(normalizedHref);
                if (title == null || title.trim().isEmpty()) {
                    String htmlTitle = doc.title();
                    if (htmlTitle != null && !htmlTitle.trim().isEmpty()) title = htmlTitle.trim();
                    else {
                        Element h1 = doc.selectFirst("h1");
                        if (h1 != null && !h1.text().trim().isEmpty()) title = h1.text().trim();
                    }
                }
                if (title == null || title.trim().isEmpty()) title = extractFileName(href);

                BookContent bc = new BookContent();
                bc.setBookId(bookId);
                bc.setChapterTitle(title);
                bc.setChapterContent(doc.html());
                bc.setChapterOrder(order++);  // 这里保证编号从 1 开始
                chapters.add(bc);

                System.out.println("[EPUB] 章节 " + (order-1) + ": " + title);
            }


            // 4) 处理封面图片（单独 export）
            if (epub.getCoverImage() != null) {
                Resource coverRes = epub.getCoverImage();
                File coverFile = new File(staticImgDir, "Images/cover.jpg");
                File parent = coverFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(coverFile)) {
                    fos.write(coverRes.getData());
                    System.out.println("[EPUB] 导出封面图片: " + coverFile.getAbsolutePath());
                }
            }

            System.out.println("[EPUB] 共解析章节数: " + chapters.size());
        }

        return chapters;
    }

    // 构建 TOC 映射
    private static void buildHrefTitleMap(List<TOCReference> refs, Map<String, String> map) {
        if (refs == null) return;
        for (TOCReference ref : refs) {
            if (ref == null) continue;
            if (ref.getResource() != null && ref.getTitle() != null && !ref.getTitle().isBlank()) {
                String href = ref.getResource().getHref();
                if (href != null && !href.isBlank()) map.put(href, ref.getTitle());
            }
            if (ref.getChildren() != null && !ref.getChildren().isEmpty()) buildHrefTitleMap(ref.getChildren(), map);
        }
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        String p = path.replace("\\", "/").trim();
        int q = p.indexOf('?'); if (q > -1) p = p.substring(0, q);
        int h = p.indexOf('#'); if (h > -1) p = p.substring(0, h);

        Deque<String> stack = new ArrayDeque<>();
        for (String part : p.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            else if (part.equals("..")) { if (!stack.isEmpty()) stack.removeLast(); }
            else stack.addLast(part);
        }
        return String.join("/", stack);
    }

    private static String extractFileName(String href) {
        if (href == null) return "未知章节";
        int idx = href.lastIndexOf('/');
        return idx >= 0 ? href.substring(idx + 1) : href;
    }
}
