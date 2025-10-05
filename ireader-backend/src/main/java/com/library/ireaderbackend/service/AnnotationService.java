package com.library.ireaderbackend.service;

import com.library.ireaderbackend.entity.Annotation;
import com.library.ireaderbackend.mapper.AnnotationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnnotationService {
    @Autowired
    private AnnotationMapper annotationMapper;

    public int addAnnotation(Annotation annotation) {
        return annotationMapper.insert(annotation);
    }

    public List<Annotation> getAnnotationsByBook(Long userId, Long bookId) {
        return annotationMapper.findByBook(userId, bookId);
    }

    public List<Annotation> getAnnotationsByChapter(Long userId, Long bookContentId) {
        return annotationMapper.findByChapter(userId, bookContentId);
    }

    public int deleteAnnotation(Long id) {
        return annotationMapper.delete(id);
    }

    public int updateAnnotation(Annotation annotation) {
        return annotationMapper.update(annotation);
    }
}
