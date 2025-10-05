package com.library.ireaderbackend.controller;

import com.library.ireaderbackend.entity.Annotation;
import com.library.ireaderbackend.service.AnnotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annotations")
public class AnnotationController {

    @Autowired
    private AnnotationService annotationService;

    // 新增
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Annotation annotation) {
        annotationService.addAnnotation(annotation);
        return ResponseEntity.ok(Map.of("success", true, "id", annotation.getId()));
    }

    // 查询某本书所有批注
    @GetMapping("/book/{bookId}")
    public List<Annotation> getByBook(@PathVariable Long bookId, @RequestParam Long userId) {
        return annotationService.getAnnotationsByBook(userId, bookId);
    }

    // 查询某章节批注
    @GetMapping("/chapter/{bookContentId}")
    public List<Annotation> getByChapter(@PathVariable Long bookContentId, @RequestParam Long userId) {
        return annotationService.getAnnotationsByChapter(userId, bookContentId);
    }

    // 删除
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        annotationService.deleteAnnotation(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "删除成功"));
    }

    // 更新
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Annotation annotation) {
        annotation.setId(id);
        annotationService.updateAnnotation(annotation);
        return ResponseEntity.ok(Map.of("success", true, "message", "更新成功"));
    }
}
