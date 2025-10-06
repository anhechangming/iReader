<template>
  <div :class="['reader-page', { night: nightMode }]">
    <!-- 顶部：左侧 加入书架/返回，中心书名，右侧：首页|我的书架|头像 -->
    <header class="reader-top">
      <div class="left">
        <button class="btn-link" @click="goBack">← 返回</button>
        <button class="btn-primary" @click="addToShelf" :disabled="addingShelf">
          {{ inShelf ? '已加入书架' : (addingShelf ? '添加中...' : '加入书架') }}
        </button>
      </div>

      <div class="center">
        <div class="book-title">{{ bookTitle }}</div>
      </div>

      <div class="right">
        <a class="nav-link" @click.prevent="goHome">书城</a>
        <a class="nav-link" @click.prevent="goShelf">我的书架</a>
        <div class="avatar-wrapper" @click="toggleAvatarMenu">
          <img class="avatar" :src="avatarUrl" alt="avatar" />
          <div v-if="avatarMenu" class="avatar-menu">
            <div class="avatar-item" @click="goShelf">我的书架</div>
            <div class="avatar-item" @click="logout">退出登录</div>
          </div>
        </div>
      </div>
    </header>

    <div class="reader-body">
      <!-- 目录侧栏 -->
      <aside class="toc" v-show="showToc">
        <div class="toc-header">目录</div>
        <div class="toc-list">
          <div
            v-for="ch in chapters"
            :key="ch.id"
            :class="['toc-item', { active: ch.chapterOrder === currentOrder }]"
            @click="loadChapter(ch.chapterOrder)"
          >
            <span class="order">{{ ch.chapterOrder }}</span>
            <span class="title">{{ trimTitle(ch.chapterTitle) }}</span>
          </div>
        </div>
      </aside>

      <!-- 正文区 -->
      <section class="content" ref="contentWrap">
        <div v-if="loading" class="loader">加载中……</div>

        <div v-else class="chapter-wrap" :style="{ fontSize: fontSize + 'px', lineHeight: lineHeight }">
          <!-- 显示 HTML 内容（BookContent.chapterContent） -->
          <div
            v-if="currentChapter"
            ref="contentEl"
            class="chapter-body"
            v-html="chapterHtml"
            @mouseup="onMouseUpInContent"
          ></div>

          <div v-else class="empty-tip">章节内容为空</div>
        </div>

        <!-- 底部翻页栏 -->
        <div class="pager">
          <button class="pager-btn" @click="prevChapter" :disabled="currentOrder <= 1">上一章</button>
          <span class="pager-info">{{ currentOrder }} / {{ chapters.length }}</span>
          <button class="pager-btn" @click="nextChapter" :disabled="currentOrder >= chapters.length">下一章</button>
        </div>
      </section>

      <!-- 批注侧栏 -->
      <aside class="annotation-drawer" v-show="showAnnotationPanel">
        <div class="drawer-header">
          批注
          <button class="small" @click="refreshAnnotations" title="刷新">⟳</button>
        </div>

        <div class="ann-list">
          <div v-if="annotationsForBook.length === 0" class="empty">暂无批注</div>
          <div v-for="a in annotationsForBook" :key="a.id" class="ann-item">
            <div class="ann-meta">
              <div>
                <small class="ann-type">{{ a.type }}</small>
                <small v-if="a.chapterOrder"> · 第{{ a.chapterOrder }}章</small>
              </div>
              <div class="ann-actions">
                <button class="btn tiny" @click="jumpToAnnotation(a)">跳转</button>
                <button class="btn tiny danger" @click="deleteAnnotation(a.id)">删除</button>
              </div>
            </div>
            <div class="ann-body">
              <div v-if="a.textContent" class="ann-text">「{{ a.textContent }}」</div>
              <div v-else class="ann-text muted">（书签）</div>
            </div>
          </div>
        </div>

        <div class="ann-actions">
          <textarea v-model="noteText" placeholder="在此添加笔记（不选中文本也可以）"></textarea>
          <div class="row">
            <input type="color" v-model="defaultColor" title="高亮颜色" />
            <button class="btn primary" @click="saveNote">保存笔记</button>
          </div>
        </div>
      </aside>
    </div>

    <!-- 右侧悬浮工具（圆形） -->
    <nav class="float-toolbar">
      <button class="tool" title="目录" @click="toggleToc">☰</button>
      <button class="tool" title="批注" @click="toggleAnnotationPanel">💬</button>
      <button class="tool" title="夜间/日间" @click="toggleNight">{{ nightMode ? '🌤' : '🌙' }}</button>
      <button class="tool" title="添加书签" @click="addBookmark">🔖</button>
      <div class="tool tool-font">
        <button @click="decreaseFont">A-</button>
        <button @click="increaseFont">A+</button>
      </div>
    </nav>

    <!-- 选中文字后的弹出框（高亮/批注） -->
    <div v-if="showPopup" :style="popupStyle" class="selection-popup" ref="popup">
      <div class="popup-row">
        <button class="btn small" @click="confirmHighlight">高亮</button>
        <button class="btn small" @click="confirmNote">批注</button>
        <button class="btn small" @click="cancelSelection">取消</button>
      </div>
      <textarea v-model="popupNote" placeholder="（可选）添加笔记"></textarea>
      <div class="row">
        <input type="color" v-model="popupColor" />
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ReaderView',
  data() {
    return {
      // book & chapters
      bookId: null,
      bookTitle: '',
      chapters: [],
      currentChapter: null,
      currentOrder: 0,
      chapterHtml: '',
      originalChapterHtml: '',

      // UI state
      showToc: false,
      showAnnotationPanel: false,
      loading: false,
      nightMode: false,
      fontSize: 18,
      lineHeight: 1.9,
      avatarMenu: false,
      avatarUrl: 'https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAEDINho4O3zb7IlJpv-ubh9RS5dt4IrgwAC8RsAAmykCFfh2rkqr_C9ATYE.jpg',
      inShelf: false,
      addingShelf: false,

      // annotations
      annotationsForBook: [],
      noteText: '',
      defaultColor: '#ffd54f',

      // selection popup
      showPopup: false,
      popupX: 0,
      popupY: 0,
      popupNote: '',
      popupColor: '#ffd54f',
      selectedText: '',
      selectedStart: null,
      selectedEnd: null,
      selectedRange: null,

      // local user
      userId: null
    };
  },

  computed: {
    popupStyle() {
      return { left: this.popupX + 'px', top: this.popupY + 'px', zIndex: 3000 };
    }
  },

  mounted() {
    this.bookId = Number(this.$route.params.id || this.$route.query.bookId);
    this.userId = Number(localStorage.getItem('userId')) || (JSON.parse(localStorage.getItem('user') || '{}').id) || 1;
    this.fetchBookInfo();
    this.fetchChapters();
    this.loadAnnotations();
    const savedFont = Number(localStorage.getItem('reader_fontSize'));
    if (savedFont) this.fontSize = savedFont;
    const savedNight = localStorage.getItem('reader_night') === '1';
    this.nightMode = savedNight;
    document.addEventListener('click', this.onGlobalClick);
  },

  beforeUnmount() {
    document.removeEventListener('click', this.onGlobalClick);
  },

  methods: {
    // navigation & top actions
    goBack() { this.$router.back(); },
    goHome() { this.$router.push('/'); },
    goShelf() { this.$router.push('/bookshelf'); },
    toggleAvatarMenu() { this.avatarMenu = !this.avatarMenu; },
    logout() { localStorage.removeItem('token'); localStorage.removeItem('userId'); this.$router.push('/login'); },
async addToShelf() {
  const userId = this.userId;
  if (!userId) { 
    alert('请先登录'); 
    this.$router.push('/login'); 
    return; 
  }
  if (this.inShelf) {
    alert('这本书已经在你的书架中了');
    return;
  }

  try {
    this.addingShelf = true;
    await axios.post(`http://localhost:8080/api/bookshelf/add`, null, { 
      params: { userId, bookId: this.bookId } 
    });
    this.inShelf = true;
    alert('成功加入书架');
  } catch (err) {
    console.error('加入书架失败', err);

    // 判断后端返回
    if (err.response && err.response.status === 409) {
      alert('这本书已经在你的书架中了');
      this.inShelf = true;
    } else if (err.response && err.response.data && err.response.data.message) {
      alert(err.response.data.message);
    } else {
      alert('加入书架失败，请稍后重试');
    }
  } finally {
    this.addingShelf = false;
  }
},


    // load book & chapters
    async fetchBookInfo() {
      try {
        const res = await axios.get(`http://localhost:8080/api/book/detail/${this.bookId}`);
        this.bookTitle = res.data?.title || '';
      } catch (e) {
        console.warn('获取书信息失败', e);
      }
    },

    async fetchChapters() {
      try {
        this.loading = true;
        const res = await axios.get(`http://localhost:8080/api/book/content/${this.bookId}`);
        this.chapters = res.data || [];
        if (this.chapters.length > 0) {
          const firstOrder = this.chapters[0].chapterOrder;
          this.loadChapter(firstOrder);
        }
      } catch (e) {
        console.error('获取章节失败', e);
      } finally {
        this.loading = false;
      }
    },

    async loadChapter(order) {
      if (!order) return;
      try {
        this.loading = true;
        const res = await axios.get(`http://localhost:8080/api/book/content/${this.bookId}/chapter/${order}`);
        this.currentChapter = res.data;
        this.currentOrder = Number(order);
        this.chapterHtml = res.data?.chapterContent || '<p>(本章无内容)</p>';
        this.originalChapterHtml = this.chapterHtml;
        this.$nextTick(() => {
          this.clearAllHighlights();
          this.renderHighlightsForCurrentChapter();
          if (this.$refs.contentWrap && this.$refs.contentWrap.scrollTo) {
            this.$refs.contentWrap.scrollTo({ top: 0, behavior: 'auto' });
          }
        });
      } catch (e) {
        console.error('加载章节失败', e);
      } finally {
        this.loading = false;
      }
    },

    prevChapter() { if (this.currentOrder > 1) this.loadChapter(this.currentOrder - 1); },
    nextChapter() { if (this.currentOrder < this.chapters.length) this.loadChapter(this.currentOrder + 1); },
    toggleToc() { this.showToc = !this.showToc; },
    toggleAnnotationPanel() { this.showAnnotationPanel = !this.showAnnotationPanel; },

    // font & theme
    increaseFont() { this.fontSize = Math.min(30, this.fontSize + 1); localStorage.setItem('reader_fontSize', this.fontSize); },
    decreaseFont() { this.fontSize = Math.max(12, this.fontSize - 1); localStorage.setItem('reader_fontSize', this.fontSize); },
    toggleNight() { this.nightMode = !this.nightMode; localStorage.setItem('reader_night', this.nightMode ? '1' : '0'); },

    trimTitle(t) { if (!t) return ''; return t.length > 80 ? t.slice(0, 80) + '...' : t; },

    // annotations backend
    async loadAnnotations() {
      try {
        const res = await axios.get(`http://localhost:8080/api/annotations/book/${this.bookId}`, {
          params: { userId: this.userId }
        });
        this.annotationsForBook = res.data || [];
      } catch (e) {
        console.error('加载批注失败', e);
      }
    },
    async refreshAnnotations() {
      await this.loadAnnotations();
      this.clearAllHighlights();
      this.renderHighlightsForCurrentChapter();
    },
    async deleteAnnotation(id) {
      if (!confirm('确认删除该批注？')) return;
      try {
        await axios.delete(`http://localhost:8080/api/annotations/${id}`);
        this.annotationsForBook = this.annotationsForBook.filter(a => a.id !== id);
        this.clearAllHighlights();
        this.renderHighlightsForCurrentChapter();
      } catch (e) {
        console.error('删除失败', e);
        alert('删除失败');
      }
    },

    // save a simple NOTE from annotation panel (not selection)
    async saveNote() {
      if (!this.noteText || !this.noteText.trim()) { alert('笔记不能为空'); return; }
      const payload = {
        userId: this.userId,
        bookId: this.bookId,
        bookContentId: this.currentChapter?.id,
        chapterOrder: this.currentChapter?.chapterOrder,
        type: 'NOTE',
        textContent: this.noteText.trim()
      };
      try {
        await axios.post('http://localhost:8080/api/annotations/add', payload);
        this.noteText = '';
        await this.loadAnnotations();
        alert('笔记已保存');
      } catch (e) {
        console.error('保存笔记失败', e);
        alert('保存失败');
      }
    },

    // add a bookmark by creating annotation with type BOOKMARK
    async addBookmark() {
      if (!this.currentChapter) { alert('请先打开章节'); return; }
      const payload = {
        userId: this.userId,
        bookId: this.bookId,
        bookContentId: this.currentChapter.id,
        chapterOrder: this.currentChapter.chapterOrder,
        type: 'BOOKMARK'
      };
      try {
        await axios.post('http://localhost:8080/api/annotations/add', payload);
        await this.loadAnnotations();
        alert('书签已保存');
      } catch (e) {
        console.error('保存书签失败', e);
        alert('保存书签失败');
      }
    },

    // ---------------- selection handling (for highlight & note) ----------------
    onMouseUpInContent(event) {
      const sel = window.getSelection();
      if (!sel || sel.isCollapsed) {
        return;
      }
      const range = sel.getRangeAt(0);
      const contentEl = this.$refs.contentEl;
      if (!contentEl) return;
      if (!contentEl.contains(range.commonAncestorContainer)) {
        return;
      }

      // compute offsets relative to contentEl
      const offsets = this.getOffsetsFromRange(range, contentEl);
      if (!offsets || offsets.end <= offsets.start) {
        return;
      }

      // position popup near selection but avoid right-side tool/annotation-drawer
      const rect = range.getBoundingClientRect();
      const popupWidth = 260;
      const popupHeight = 150;
      // compute a safe right bound (consider float toolbar & annotation drawer)
      const rightReserved = this.showAnnotationPanel ? 380 : 120; // 380 for drawer + margin, else toolbar width approx 120
      const rightBound = window.innerWidth - rightReserved;
      let x = rect.left + window.scrollX;
      if (x + popupWidth > rightBound) {
        x = Math.max(10, rightBound - popupWidth - 8);
      }
      let y = rect.top + window.scrollY - 48;
      if (y < 60) y = rect.bottom + window.scrollY + 6; // if too close to top, show below selection
      // avoid bottom overflow
      const pageBottom = window.scrollY + window.innerHeight;
      if (y + popupHeight > pageBottom) {
        y = Math.max(60, pageBottom - popupHeight - 20);
      }

      this.popupX = x;
      this.popupY = y;

      this.selectedText = offsets.text;
      this.selectedStart = offsets.start;
      this.selectedEnd = offsets.end;
      this.selectedRange = range.cloneRange();

      this.popupNote = '';
      this.popupColor = this.defaultColor || '#ffd54f';
      this.showPopup = true;
    },

    cancelSelection() {
      this.showPopup = false;
      try { window.getSelection().removeAllRanges(); } catch (e) {}
      this.selectedText = '';
      this.selectedStart = null;
      this.selectedEnd = null;
      this.selectedRange = null;
    },

    // immediate wrap using saved range -> instant feedback, then POST
    wrapRangeWithSpan(range, tempId, color) {
      try {
        const span = document.createElement('span');
        span.className = 'annotation-highlight';
        span.dataset.tempId = tempId;
        span.style.backgroundColor = color || '#ffd54f';
        span.style.padding = '0 2px';
        span.style.borderRadius = '2px';
        const frag = range.extractContents();
        span.appendChild(frag);
        range.insertNode(span);
        // collapse selection
        try { window.getSelection().removeAllRanges(); } catch (e) {}
        return span;
      } catch (e) {
        console.error('wrapRangeWithSpan error', e);
        return null;
      }
    },

    async confirmHighlight() {
      if (!this.selectedText) { alert('请先选中文本'); return; }
      // make a temp id and immediately wrap selection to show highlight
      const tempId = 'tmp-' + Date.now();
      let insertedSpan = null;
      if (this.selectedRange) {
        insertedSpan = this.wrapRangeWithSpan(this.selectedRange, tempId, this.popupColor);
      }

      // prepare payload (use saved offsets if present)
      const payload = {
        userId: this.userId,
        bookId: this.bookId,
        bookContentId: this.currentChapter?.id,
        chapterOrder: this.currentChapter?.chapterOrder,
        type: 'HIGHLIGHT',
        textContent: this.selectedText,
        color: this.popupColor || this.defaultColor,
        startOffset: this.selectedStart,
        endOffset: this.selectedEnd
      };

      try {
        const res = await axios.post('http://localhost:8080/api/annotations/add', payload);
        const newId = res.data && (res.data.id || res.data && res.data.id);
        // update temp span dataset to real id
        if (newId) {
          // find the temp span (either insertedSpan or query by data-temp-id)
          let span = insertedSpan;
          if (!span) {
            span = this.$refs.contentEl?.querySelector(`[data-temp-id="${tempId}"]`);
          }
          if (!span) {
            // alternative search: find first span with innerText matching selectedText (best-effort)
            const nodes = this.$refs.contentEl?.querySelectorAll('.annotation-highlight') || [];
            for (const s of nodes) {
              if (!s.dataset.annoId && s.innerText && s.innerText.trim() === this.selectedText.trim()) {
                span = s;
                break;
              }
            }
          }
          if (span) {
            span.dataset.annoId = newId;
            delete span.dataset.tempId;
          }
        }
        await this.loadAnnotations();
        this.clearAllHighlights();
        this.renderHighlightsForCurrentChapter();
        this.cancelSelection();
      } catch (e) {
        console.error('保存高亮失败', e);
        // rollback visual if we inserted a temp span
        if (insertedSpan && insertedSpan.parentNode) {
          insertedSpan.parentNode.replaceChild(document.createTextNode(insertedSpan.innerText), insertedSpan);
        }
        alert('保存高亮失败');
      }
    },

    async confirmNote() {
      if (!this.popupNote || !this.popupNote.trim()) { alert('请输入批注内容'); return; }
      const payload = {
        userId: this.userId,
        bookId: this.bookId,
        bookContentId: this.currentChapter?.id,
        chapterOrder: this.currentChapter?.chapterOrder,
        type: 'NOTE',
        textContent: (this.selectedText ? (this.selectedText + ' | ') : '') + this.popupNote.trim(),
        startOffset: this.selectedStart,
        endOffset: this.selectedEnd,
        color: this.popupColor
      };
      try {
        await axios.post('http://localhost:8080/api/annotations/add', payload);
        await this.loadAnnotations();
        this.cancelSelection();
      } catch (e) {
        console.error('保存批注失败', e);
        alert('保存批注失败');
      }
    },

    // rendering highlights (on load)
    clearAllHighlights() {
      if (this.$refs.contentEl && this.originalChapterHtml != null) {
        this.$refs.contentEl.innerHTML = this.originalChapterHtml;
      }
    },

    renderHighlightsForCurrentChapter() {
      if (!this.$refs.contentEl || !this.currentChapter) return;
      const arr = this.annotationsForBook.filter(a => a.bookContentId === this.currentChapter.id && a.type === 'HIGHLIGHT');
      arr.forEach(a => {
        if (a.startOffset != null && a.endOffset != null) {
          this.wrapOffsetsWithSpan(a.startOffset, a.endOffset, a.id, a.color || '#ffd54f');
        } else if (a.textContent) {
          this.highlightByText(a.textContent, a.id, a.color || '#ffd54f');
        }
      });
    },

    wrapOffsetsWithSpan(startChar, endChar, annotationId, color) {
      const root = this.$refs.contentEl;
      if (!root) return;
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
      const textNodes = [];
      let node;
      while ((node = walker.nextNode())) textNodes.push(node);
      let charCount = 0;
      let startNode = null, startOffsetInNode = 0;
      let endNode = null, endOffsetInNode = 0;
      for (let i = 0; i < textNodes.length; i++) {
        const tn = textNodes[i];
        const len = tn.nodeValue.length;
        if (startNode == null && charCount + len >= startChar) {
          startNode = tn;
          startOffsetInNode = startChar - charCount;
        }
        if (endNode == null && charCount + len >= endChar) {
          endNode = tn;
          endOffsetInNode = endChar - charCount;
          break;
        }
        charCount += len;
      }
      if (!startNode || !endNode) return;
      const range = document.createRange();
      range.setStart(startNode, startOffsetInNode);
      range.setEnd(endNode, endOffsetInNode);
      const frag = range.extractContents();
      const span = document.createElement('span');
      span.className = 'annotation-highlight';
      span.dataset.annoId = annotationId;
      span.style.backgroundColor = color || '#ffd54f';
      span.style.padding = '0 2px';
      span.style.borderRadius = '2px';
      span.appendChild(frag);
      range.insertNode(span);
    },

    highlightByText(text, annotationId, color) {
      if (!text) return;
      const root = this.$refs.contentEl;
      if (!root) return;
      const plain = root.innerText || root.textContent || '';
      const idx = plain.indexOf(text);
      if (idx < 0) return;
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
      let charCount = 0;
      let tn;
      while ((tn = walker.nextNode())) {
        const nextCount = charCount + tn.nodeValue.length;
        if (idx >= charCount && idx < nextCount) {
          const startInNode = idx - charCount;
          const endInNode = startInNode + text.length;
          const range = document.createRange();
          range.setStart(tn, startInNode);
          range.setEnd(tn, endInNode);
          const frag = range.extractContents();
          const span = document.createElement('span');
          span.className = 'annotation-highlight';
          span.dataset.annoId = annotationId;
          span.style.background = color;
          span.style.padding = '0 2px';
          span.style.borderRadius = '2px';
          span.appendChild(frag);
          range.insertNode(span);
          break;
        }
        charCount = nextCount;
      }
    },

    // offsets helper
    getOffsetsFromRange(range, rootEl) {
      try {
        const walker = document.createTreeWalker(rootEl, NodeFilter.SHOW_TEXT, null);
        const textNodes = [];
        let n;
        while ((n = walker.nextNode())) textNodes.push(n);

        let charCount = 0;
        let foundStart = false, startIndex = -1;
        for (let i = 0; i < textNodes.length; i++) {
          const tn = textNodes[i];
          if (tn === range.startContainer) {
            startIndex = charCount + range.startOffset;
            foundStart = true;
            break;
          }
          charCount += tn.nodeValue.length;
        }

        charCount = 0;
        let foundEnd = false, endIndex = -1;
        for (let i = 0; i < textNodes.length; i++) {
          const tn = textNodes[i];
          if (tn === range.endContainer) {
            endIndex = charCount + range.endOffset;
            foundEnd = true;
            break;
          }
          charCount += tn.nodeValue.length;
        }

        if (!foundStart || !foundEnd) {
          const selText = range.toString();
          const plain = rootEl.innerText || rootEl.textContent || '';
          const idx = plain.indexOf(selText);
          if (idx < 0) return null;
          return { start: idx, end: idx + selText.length, text: selText };
        }
        const text = range.toString();
        return { start: startIndex, end: endIndex, text };
      } catch (e) {
        console.error('计算选区offset失败', e);
        return null;
      }
    },

    // jump to annotation
    jumpToAnnotation(a) {
      if (!a) return;
      if (a.bookContentId && a.bookContentId !== this.currentChapter?.id) {
        this.loadChapter(a.chapterOrder || this.getChapterOrderById(a.bookContentId));
        this.$nextTick(() => {
          setTimeout(() => {
            const el = this.$refs.contentEl?.querySelector(`[data-anno-id="${a.id}"]`);
            if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }, 400);
        });
      } else {
        this.$nextTick(() => {
          const el = this.$refs.contentEl?.querySelector(`[data-anno-id="${a.id}"]`);
          if (el) {
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
            el.style.transition = 'box-shadow 0.4s';
            el.style.boxShadow = '0 0 8px rgba(255,200,0,0.9)';
            setTimeout(() => { el.style.boxShadow = ''; }, 700);
          }
        });
      }
    },

    getChapterOrderById(bookContentId) {
      const c = this.chapters.find(x => x.id === bookContentId);
      return c ? c.chapterOrder : null;
    },

    // global click handler
    onGlobalClick(e) {
      if (!this.$el.contains(e.target)) {
        this.avatarMenu = false;
        this.showPopup = false;
      }
    }
  }
};
</script>

<style scoped>
/* 略：保持之前样式，仅对关键样式作增强（popup z-index，提高 highlight 可视度） */
.reader-page { min-height: 100vh; background: #f7f7f7; color: #1b1b1b; font-family: "Helvetica Neue", Arial, "Microsoft Yahei", sans-serif; display:flex; flex-direction:column; }
.reader-page.night { background: #0d1115; color: #dfe6ee; }
/* ...（其余样式与之前版本保持一致） ... */

/* selection popup 提高 z-index，避免被 toolbar 覆盖 */
.selection-popup { position:absolute; z-index:3000; width:260px; background:#fff; border:1px solid #ddd; box-shadow:0 8px 24px rgba(0,0,0,0.12); padding:8px; border-radius:8px; }
/* highlight style 更加明显 */
.annotation-highlight { background:#ffd54f; padding:0 2px; border-radius:2px; box-decoration-break: clone; }

/* 其余 CSS 保持你之前的样式（省略以保持回复简洁） */
/* ---------------- layout ---------------- */
.reader-page { min-height: 100vh; background: #f7f7f7; color: #1b1b1b; font-family: "Helvetica Neue", Arial, "Microsoft Yahei", sans-serif; display:flex; flex-direction:column; }
.reader-page.night { background: #0d1115; color: #dfe6ee; }

/* top */
.reader-top { height:56px; display:flex; align-items:center; justify-content:space-between; padding:0 18px; background: #fff; border-bottom:1px solid #e6e6e6; }
.reader-page.night .reader-top { background: #07111a; border-bottom-color:#16202a; }
.left, .center, .right { display:flex; align-items:center; gap:8px; }
.left { flex:1; }
.center { flex:1; justify-content:center; }
.right { flex:1; justify-content:flex-end; position:relative; }
.book-title { font-weight:600; font-size:16px; }

/* buttons */
.btn-link { border:none; background:none; color:#1a73e8; cursor:pointer; }
.btn-primary { background:#1a73e8; color:#fff; border:none; padding:6px 10px; border-radius:6px; cursor:pointer; }

/* tools */
.reader-tools { display:flex; justify-content:space-between; align-items:center; padding:8px 18px; background:#fafafa; border-bottom:1px solid #eee; }
.reader-page.night .reader-tools { background:#07111a; border-bottom-color:#16202a; }
.tool { background:none; border:none; cursor:pointer; padding:6px; color:inherit; }
.font-controls { display:inline-flex; align-items:center; gap:8px; }

/* body layout */
.reader-body { display:flex; flex:1; min-height:calc(100vh - 140px); position:relative; }
.toc { width:300px; border-right:1px solid #eee; padding:12px; background:#fff; overflow:auto; }
.reader-page.night .toc { background:#07111a; border-right-color:#12202a; }
.toc-header { font-weight:600; margin-bottom:8px; }
.toc-list { display:flex; flex-direction:column; gap:6px; }
.toc-item { padding:8px; border-radius:6px; cursor:pointer; color:#222; display:flex; gap:8px; align-items:center; }
.reader-page.night .toc-item { color:#cfd8de; }
.toc-item.active { background:#f1f7ff; }
.reader-page.night .toc-item.active { background:#0b2332; }

/* content */
.content { flex:1; overflow:auto; padding:28px 60px; }
.chapter-wrap { max-width:840px; margin:0 auto; text-align:left; }
.chapter-body { background:#fff; padding:32px; box-shadow:0 6px 18px rgba(0,0,0,0.04); border-radius:8px; }
.reader-page.night .chapter-body { background:#071521; box-shadow:none; color:#dfe6ee; }
.chapter-body img { max-width:100%; height:auto; display:block; margin:12px auto; }

/* pager */
.pager { display:flex; justify-content:center; align-items:center; gap:12px; margin:18px 0; }
.pager-btn { padding:8px 14px; border-radius:6px; border:1px solid #ddd; background:#fff; cursor:pointer; }
.reader-page.night .pager-btn { background:#0b1116; border-color:#222; color:#ddd; }

/* annotation drawer */
.annotation-drawer { width:360px; border-left:1px solid #eee; background:#fff; padding:12px; overflow:auto; }
.reader-page.night .annotation-drawer { background:#07111a; border-left-color:#12202a; color:#cfd8de; }
.drawer-header { display:flex; justify-content:space-between; align-items:center; font-weight:600; margin-bottom:8px; }
.ann-list { max-height:60vh; overflow:auto; margin-bottom:10px; }
.ann-item { padding:8px; border-bottom:1px dashed #eee; }
.ann-meta { display:flex; justify-content:space-between; align-items:center; gap:8px; }
.ann-text { margin-top:6px; }

/* float toolbar */
.float-toolbar { position:fixed; right:22px; top:160px; display:flex; flex-direction:column; gap:12px; z-index:50; }
.tool { width:44px; height:44px; border-radius:50%; background:#fff; box-shadow:0 6px 14px rgba(0,0,0,0.08); display:flex; align-items:center; justify-content:center; font-size:16px; border:none; cursor:pointer; }
.tool-font { padding:8px; display:flex; flex-direction:column; gap:6px; }

/* selection popup */
.selection-popup { position:absolute; z-index:999; width:260px; background:#fff; border:1px solid #ddd; box-shadow:0 8px 24px rgba(0,0,0,0.12); padding:8px; border-radius:8px; }
.selection-popup textarea { width:100%; height:60px; margin-top:8px; border:1px solid #eee; padding:6px; border-radius:4px; resize:vertical; }
.selection-popup .row { display:flex; align-items:center; gap:8px; margin-top:8px; }
.btn.small { padding:6px 10px; border-radius:6px; }
.btn.tiny { padding:4px 8px; font-size:12px; }
.btn.danger { background:#ff6b6b; color:#fff; border:none; }
.btn.primary { background:#2f80ed; color:#fff; border:none; padding:8px 12px; border-radius:6px; }

/* highlight style */
.annotation-highlight { background:#ffd54f; padding:0 2px; border-radius:2px; }

/* avatar */
.avatar { width:36px; height:36px; border-radius:50%; object-fit:cover; cursor:pointer; }
.avatar-wrapper { position:relative; }
.avatar-menu { position:absolute; right:0; top:44px; background:#fff; border-radius:6px; box-shadow:0 6px 18px rgba(0,0,0,0.08); width:160px; z-index:30; }
.avatar-item { padding:10px; cursor:pointer; text-align:center; }
.avatar-item:hover { background:#f5f7fb; }
.reader-page.night .avatar-menu { background:#07111a; color:#cfd8de; }

@media (max-width: 1000px) {
  .toc { display:none; }
  .annotation-drawer { display:none; }
  .chapter-body { padding:18px; }
  .float-toolbar { right:8px; top:140px; }
}

</style>
