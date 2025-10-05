package com.library.ireaderbackend.service;


import com.library.ireaderbackend.entity.BookContent;
import com.library.ireaderbackend.mapper.BookContentMapper;
import com.library.ireaderbackend.mapper.BookMapper;
import com.library.ireaderbackend.Utils.EpubParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookContentService {

    @Autowired
    private BookContentMapper bookContentMapper;

    @Autowired
    private BookMapper bookMapper;

    /**
     * 获取某书的所有章节；若 DB 中为空则尝试解析 epub 并持久化
     */
    public List<BookContent> getChapters(Long bookId) throws Exception {
        List<BookContent> list = bookContentMapper.findByBookId(bookId);
        if (list == null || list.isEmpty()) {
            // 从 book 表里拿 filePath（比如： books/一句顶一万句 - 刘震云.epub ）
            com.library.ireaderbackend.entity.Book book = bookMapper.findById(bookId);
            if (book == null) throw new RuntimeException("书籍不存在: " + bookId);
            String path = book.getFilePath();
            if (path == null) throw new RuntimeException("书籍文件路径未配置");
            // 确保以 / 开头用于 classpath 资源读取
            String cp = path.startsWith("/") ? path : ("/" + path);
            // 后端存储路径，比如 D:/桌面/iReader/static/book/{bookId}/
            String staticImgDir = "D:/桌面/iReader/static/book/" + bookId + "/";

           List<BookContent> chapters = EpubParser.parseEpubToChapters(cp, bookId,staticImgDir);
            if (!chapters.isEmpty()) {
                bookContentMapper.insertBatch(chapters);
            }
            list = bookContentMapper.findByBookId(bookId);
        }
        return list;
    }

    public BookContent getChapter(Long bookId, Integer order) {
        return bookContentMapper.findByBookIdAndOrder(bookId, order);
    }
    public void saveAll(List<BookContent> contents) {
        if (contents == null || contents.isEmpty()) return;
        bookContentMapper.insertBatch(contents);
    }
}
