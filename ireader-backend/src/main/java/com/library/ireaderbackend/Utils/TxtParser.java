package com.library.ireaderbackend.Utils;

import com.library.ireaderbackend.entity.BookContent;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 更鲁棒的 TXT 解析器，针对你提供的样本（如：第一章 前言：出延津记 / 小节 一 二 三）
 * 功能：
 *  - 自动检测编码（UTF-8 / GB18030 / GBK）
 *  - 过滤顶部噪声（下载/版权/简介）
 *  - 识别主章节（第X章 ...）
 *  - 将“一 二 三”类识别为章节内小节（插入为小标题）
 *  - 恢复段落（识别全角缩进、空行；合并被错误换行的句子）
 */
public class TxtParser {

    // 主章节：第X章 + 可带副标题（支持全角空格、半角空格、冒号、破折号等）
    private static final Pattern MAIN_CHAPTER_PATTERN = Pattern.compile(
            "^\\s*(第[零一二三四五六七八九十百千万0-9]{1,6}[章回节卷部集篇])" +      // group1: 第X章
                    "[\\s\\u3000\\-–—:：]*" +                                                // 可选分隔符
                    "(.*)$",                                                                 // group2: 副标题（可空）
            Pattern.CASE_INSENSITIVE
    );

    // 小节编号：单独一行的 一 二 三 等（作为章节内小标题）
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\s*[一二三四五六七八九十百千零]+\\s*$");

    // header / watermark / 下载说明 等噪声匹配
    private static final Pattern REDUNDANT = Pattern.compile(
            "小说下载|www\\.|http://|https://|bbs\\.|整理|版权所有|ISBN|定价|【作品简介】|作者：|出版社：",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MIN_CHAPTER_LENGTH = 40; // 章节最小长度，过短将合并
    private static final int FALLBACK_CHARS_PER_CHAPTER = 3000; // 无章节标题时按长度拆分

    public static List<BookContent> parseTxt(Path path, Long bookId) throws IOException {
        List<String> raw = readFileWithAutoCharset(path);
        if (raw == null || raw.isEmpty()) throw new IOException("文件内容为空");

        // 标准化行：保留原始换行但把非断行空格统一替换
        List<String> normalized = new ArrayList<>();
        for (String line : raw) {
            if (line == null) line = "";
            // 把不间断空格与常见干扰字符替换为普通空格（但保留全角缩进 U+3000）
            line = line.replace('\u00A0', ' ');
            normalized.add(line);
        }

        // 过滤顶部冗余直到第一个主章节或正文开始（如果没有主章节，则只过滤明显噪声行）
        List<String> filtered = filterHeaderNoise(normalized);

        // 切分章节
        List<BookContent> chapters = splitChapters(filtered);

        // 如果只剩序章且内容很长，按长度分割以避免只出现一个超长序章
        if (chapters.size() == 1 && (chapters.get(0).getChapterTitle() == null || "序章".equals(chapters.get(0).getChapterTitle()))) {
            String body = chapters.get(0).getChapterContent();
            if (body != null && body.length() > FALLBACK_CHARS_PER_CHAPTER) {
                chapters = fallbackSplitByLength(body);
            }
        }

        // 填充 bookId 与 chapterOrder，修剪内容首尾空白
        for (int i = 0; i < chapters.size(); i++) {
            BookContent c = chapters.get(i);
            c.setBookId(bookId);
            c.setChapterOrder(i + 1);
            if (c.getChapterContent() != null) c.setChapterContent(c.getChapterContent().trim());
            if (c.getChapterTitle() == null || c.getChapterTitle().isBlank()) c.setChapterTitle("第" + (i + 1) + "章");
        }
        return chapters;
    }

    // -------------------- 辅助方法 --------------------

    private static List<String> filterHeaderNoise(List<String> lines) {
        List<String> out = new ArrayList<>();
        boolean started = false;
        int i = 0;
        for (; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            // 如果遇到主章节或简介标记，就认为正文开始
            if (MAIN_CHAPTER_PATTERN.matcher(t).matches() || t.startsWith("【作品简介】") || t.startsWith("第一章") || t.startsWith("第一章")) {
                started = true;
                break;
            }
            // 如果行包含明显的噪声则跳过
            if (REDUNDANT.matcher(t).find()) continue;
            // 跳过空白头部
            if (t.isEmpty()) continue;
            // 若到这里说明可能进入正文（但为了保险，我们把这行也当正文）
            // 但如果尚未发现真正的正文，我们仍继续，直到出现主章节或正文明显开始
            // 为了兼容不带 meta 的文件，我们允许直接把剩余都当正文
            // so break and copy all remaining
            break;
        }
        // copy remaining lines from i to end
        for (int j = i; j < lines.size(); j++) out.add(lines.get(j));
        return out;
    }

    private static List<BookContent> splitChapters(List<String> lines) {
        List<BookContent> chapters = new ArrayList<>();
        String curTitle = "序章";
        List<String> curLines = new ArrayList<>();
        boolean foundMain = false;

        for (int idx = 0; idx < lines.size(); idx++) {
            String raw = lines.get(idx);
            String normalized = raw == null ? "" : raw.replace('\u3000', '\u3000'); // 保留全角空格
            String trimmed = normalized.trim();

            // 主章节检测
            Matcher m = MAIN_CHAPTER_PATTERN.matcher(trimmed);
            if (m.matches()) {
                // 保存当前章节
                if (!curLines.isEmpty()) {
                    String content = formatChapterContent(curLines);
                    BookContent bc = new BookContent();
                    bc.setChapterTitle(curTitle);
                    bc.setChapterContent(content);
                    chapters.add(bc);
                    curLines.clear();
                }
                // 构造章节标题（合并主标题与副标题）
                String main = m.group(1) == null ? trimmed : m.group(1).trim();
                String sub = (m.groupCount() >= 2 && m.group(2) != null) ? m.group(2).trim() : "";
                if (!sub.isEmpty()) curTitle = (main + " " + sub).trim();
                else curTitle = main.isEmpty() ? trimmed : main;
                foundMain = true;
                continue; // 跳过该标题行作为正文
            }

            // 小节编号识别：把它插入为章节内部小标题（双换行分隔）
            Matcher s = SECTION_PATTERN.matcher(trimmed);
            if (s.matches() && foundMain) {
                // 将小节编号作为独立小标题加入当前章
                // 先把当前累计段落加入
                if (!curLines.isEmpty()) {
                    curLines.add(""); // 保留一个空行以分段
                }
                curLines.add(trimmed);
                curLines.add(""); // 空行后再开始正文
                continue;
            }

            // 普通正文行，保留原始（等会 format 时合并断行）
            curLines.add(raw);
        }

        // 保存最后一个章节
        if (!curLines.isEmpty() || !"序章".equals(curTitle)) {
            String content = formatChapterContent(curLines);
            BookContent bc = new BookContent();
            bc.setChapterTitle(curTitle);
            bc.setChapterContent(content);
            chapters.add(bc);
        }

        // 如果有太短的章节，尝试合并到上一个
        List<BookContent> filtered = new ArrayList<>();
        for (BookContent c : chapters) {
            String cont = c.getChapterContent() == null ? "" : c.getChapterContent().trim();
            if (cont.length() < MIN_CHAPTER_LENGTH && !filtered.isEmpty()) {
                // 合并到上一章
                BookContent prev = filtered.get(filtered.size() - 1);
                prev.setChapterContent(prev.getChapterContent() + "\n\n" + cont);
            } else {
                filtered.add(c);
            }
        }
        return filtered;
    }

    /**
     * 将章节的原始行恢复成带段落的文本
     * 规则：
     *  - 如果行为空 -> 段落边界
     *  - 如果行以两个或以上全角空格（\u3000\u3000）或制表符开头 -> 认为新段落
     *  - 如果当前行为小节编号（如“一”），已在 splitChapters 中插入为独立行
     *  - 否则将连续的短行合并为同一个段落（中间以空格连接），在遇到句号（。！？）保持自然断句
     */
    private static String formatChapterContent(List<String> rawLines) {
        StringBuilder out = new StringBuilder();
        StringBuilder para = new StringBuilder();

        for (int i = 0; i < rawLines.size(); i++) {
            String raw = rawLines.get(i);
            String line = raw == null ? "" : raw;

            String trimmed = line.trim();
            // 如果行是空行 -> 段落结束
            if (trimmed.isEmpty()) {
                if (para.length() > 0) {
                    out.append(para.toString().trim()).append("\n\n");
                    para.setLength(0);
                } else {
                    // 多个空行只保留一个
                    if (out.length() > 0 && !out.toString().endsWith("\n\n")) out.append("\n\n");
                }
                continue;
            }

            // 如果该行为小节编号（例如 "一"）或看起来像小标题，直接作为独立段落
            if (SECTION_PATTERN.matcher(trimmed).matches()) {
                if (para.length() > 0) {
                    out.append(para.toString().trim()).append("\n\n");
                    para.setLength(0);
                }
                out.append(trimmed).append("\n\n");
                continue;
            }

            // 如果该行以全角缩进（两个或更多全角空格）或 tab 开头 -> 新段落
            if (line.startsWith("\u3000\u3000") || line.startsWith("\t") || line.startsWith("    ")) {
                if (para.length() > 0) {
                    out.append(para.toString().trim()).append("\n\n");
                    para.setLength(0);
                }
                // 把缩进去掉再加入
                out.append(trimmed).append("\n\n");
                continue;
            }

            // 否则：普通行，合并到当前段落
            if (para.length() == 0) {
                para.append(trimmed);
            } else {
                // 如果上一行以中文句号或问号或感叹号等结尾，倾向于新句；但我们仍保持为同段落（不产生额外空行）
                char lastChar = para.charAt(para.length() - 1);
                boolean lastEndPunct = (lastChar == '。' || lastChar == '!' || lastChar == '?' || lastChar == '！' || lastChar == '？' || lastChar == '…');
                if (lastEndPunct) {
                    // 句子已结束，通常下一行可能为缩进或新句，但不强制新段落：我们在句号处插入换行以提升可读性
                    para.append(trimmed);
                } else {
                    // 断行合并，用空格连接，避免单词粘连
                    para.append(" ").append(trimmed);
                }
            }
        }

        // flush last paragraph
        if (para.length() > 0) {
            out.append(para.toString().trim());
        }

        // 清理多余空行
        String result = out.toString().replaceAll("\\n{3,}", "\n\n").trim();
        return result;
    }

    private static List<BookContent> fallbackSplitByLength(String full) {
        List<BookContent> out = new ArrayList<>();
        int len = full.length();
        int idx = 0;
        int part = 1;
        while (idx < len) {
            int end = Math.min(len, idx + FALLBACK_CHARS_PER_CHAPTER);
            // 优先在句号处断开
            int last = Math.max(full.lastIndexOf('。', end), full.lastIndexOf('\n', end));
            if (last > idx + 200) end = last + 1;
            String slice = full.substring(idx, end).trim();
            BookContent bc = new BookContent();
            bc.setChapterTitle("第" + part + "部分");
            bc.setChapterContent(slice);
            out.add(bc);
            idx = end;
            part++;
        }
        return out;
    }

    // -------------------- 文件读取（自动编码识别） --------------------
    private static List<String> readFileWithAutoCharset(Path path) throws IOException {
        List<Charset> charsets = Arrays.asList(StandardCharsets.UTF_8,
                Charset.forName("GB18030"),
                Charset.forName("GBK"));

        for (Charset cs : charsets) {
            try {
                List<String> lines = readFile(path, cs);
                if (looksLikeText(lines)) return lines;
            } catch (MalformedInputException ignored) {
            }
        }
        throw new IOException("无法识别 TXT 文件编码，请尝试用 UTF-8 或 GBK 保存再导入。");
    }

    private static boolean looksLikeText(List<String> lines) {
        // 简单校验：前 10 行不应包含替代字符（��）或大量控制字符
        for (int i = 0; i < Math.min(10, lines.size()); i++) {
            String l = lines.get(i);
            if (l == null) continue;
            if (l.contains("��") || l.contains("�") || l.contains("??")) return false;
            if (l.matches(".*[\\p{Cntrl}&&[^\\r\\n\\t]].*")) return false;
        }
        return true;
    }

    private static List<String> readFile(Path path, Charset charset) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), charset))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (lines.isEmpty() && line.startsWith("\uFEFF")) line = line.substring(1); // BOM
                lines.add(line);
            }
        }
        return lines;
    }
}
