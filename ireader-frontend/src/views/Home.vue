<template>
  <div class="home">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="nav-left">
        <router-link to="/bookshelf" class="nav-item">我的书架</router-link>
      </div>
      <div class="nav-right">
        <!-- 头像 -->
        <img
          src="https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAEDINho4O3zb7IlJpv-ubh9RS5dt4IrgwAC8RsAAmykCFfh2rkqr_C9ATYE.jpg"
          alt="avatar"
          class="avatar"
          @click="toggleMenu"
        />
        <!-- 下拉菜单 -->
        <div v-if="menuVisible" class="dropdown-menu">
          <div class="menu-item" @click="goToShelf">我的书架</div>
          <div class="menu-item" @click="logout">退出登录</div>
        </div>
      </div>
    </header>

<!-- 搜索栏区域 -->
<div class="search-section">
  <h1 class="title">书城</h1>
  <div class="search-bar">
    <input
      type="text"
      v-model="searchKeyword"
      placeholder="搜索书名或作者"
      @keyup.enter="searchBooks"
    />
    <button class="search-btn" @click="searchBooks">搜索</button>
  </div>
</div>

    <!-- 书籍列表 -->
    <!-- Home.vue 片段 -->
<main class="book-list">
  <div 
    v-for="book in books" 
    :key="book.id" 
    class="book-card"
    @click="goToReader(book.id)"   
>
    <img :src="book.cover" alt="封面" class="book-cover" />
    <div class="book-info">
      <h3 class="book-title">{{ book.title }}</h3>
      <p class="book-author">{{ book.author }}</p>
    </div>
  </div>
</main>

  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "Home",
  data() {
    return {
      books: [],
      menuVisible: false,
      searchKeyword: "",
    };
  },
  methods: {
    async fetchBooks(keyword = "") {
      try {
        const response = await axios.get("http://localhost:8080/api/book/list", {
          params: { keyword },
        });
        this.books = response.data;
      } catch (error) {
        console.error("获取书籍失败:", error);
      }
    },
    searchBooks() {
      this.fetchBooks(this.searchKeyword);
    },
    toggleMenu() {
      this.menuVisible = !this.menuVisible;
    },
    goToShelf() {
      this.$router.push("/bookshelf");
    },
    logout() {
      localStorage.removeItem("token");
      this.$router.push("/login");
    },
    goToReader(bookId) {
    this.$router.push(`/reader/${bookId}`);
  }
  },
  mounted() {
    this.fetchBooks();
  },
};
</script>

<style scoped>
/* 顶部导航栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 40px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: relative;
}

.nav-left .nav-item {
  margin-right: 20px;
  color: #1a73e8;
  text-decoration: none;
  font-size: 14px;
}

.nav-left .nav-item:hover {
  text-decoration: underline;
}

/* 头像样式 */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
}

.nav-right {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 48px;
  right: 0;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 120px;
  z-index: 10;
}

.menu-item {
  padding: 10px;
  font-size: 14px;
  text-align: center;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

/* 整个搜索区域 */
.search-section {
  text-align: center;
  padding: 30px 0;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

/* 标题样式 */
.search-section .title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #333;
}

/* 搜索框居中 */
.search-bar {
  display: flex;
  justify-content: center; /* 横向居中 */
  align-items: center;
  gap: 10px;
}

.search-bar input {
  width: 400px; /* 固定宽度 */
  padding: 10px 15px;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
}

.search-bar input:focus {
  border-color: #1a73e8;
}

.search-btn {
  padding: 8px 18px;
  font-size: 14px;
  border: none;
  border-radius: 20px;
  background-color: #1a73e8;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-btn:hover {
  background-color: #1669c1;
}


/* 书籍列表 */
.book-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, 160px);
  gap: 24px;
  padding: 24px 40px;
  background: #fafafa;
}

.book-card {
  background: #fff;
  border-radius: 6px;
  padding: 10px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.book-cover {
  width: 100px;
  height: 140px;
  object-fit: cover;
  margin-bottom: 8px;
}

.book-title {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.book-author {
  font-size: 12px;
  color: #666;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 40px;
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.search-bar input {
  flex: 1;
  max-width: 600px;
  padding: 10px 15px;
  font-size: 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
}

.search-bar input:focus {
  border-color: #1a73e8;
}

.search-btn {
  padding: 8px 18px;
  font-size: 14px;
  border: none;
  border-radius: 20px;
  background-color: #1a73e8;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-btn:hover {
  background-color: #1669c1;
}

</style>
