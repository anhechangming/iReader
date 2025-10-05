<template>
  <div class="bookshelf">
    <!-- 顶部导航 -->
    <header class="header">
      <el-input
        v-model="keyword"
        placeholder="搜索我的书架"
        clearable
        class="search-box"
        @keyup.enter="fetchBooks"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <div class="nav-right">
        <el-button type="text" @click="goHome">首页</el-button>
        <el-dropdown>
          <span class="el-dropdown-link">
            <img class="avatar" src="https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAEDINho4O3zb7IlJpv-ubh9RS5dt4IrgwAC8RsAAmykCFfh2rkqr_C9ATYE.jpg" alt="头像" />
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="fetchBooks">我的书架</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 书架展示区 -->
    <main class="shelf">
      <div
        v-for="book in books"
        :key="book.id"
        class="book-card"
      >
        <div class="book-cover-container" @click="openBook(book)">
          <img
            class="book-cover"
            :src="book.cover || defaultCover"
            alt="封面"
          />
          <el-button 
            class="remove-btn" 
            icon="Delete" 
            circle 
            size="mini"
            @click.stop="removeFromShelf(book.id)"
          ></el-button>
        </div>
        <p class="book-title">{{ book.title }}</p>
        <p class="book-author">{{ book.author || '未知作者' }}</p>
        <p class="book-type" v-if="book.fileType">
          {{ book.fileType.toUpperCase() }}
        </p>
      </div>

      <!-- +号卡片 -->
      <div class="book-card add-card" @click="triggerUpload">
        <span class="plus">+</span>
        <p>添加书籍</p>
      </div>

      <!-- 隐藏文件上传 -->
      <input
        type="file"
        ref="fileInput"
        accept=".txt,.pdf,.epub"
        style="display: none"
        @change="handleFileUpload"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElButton } from "element-plus";
import { Search, Delete } from "@element-plus/icons-vue";
import axios from "axios";
import { useRouter } from "vue-router"; // 引入路由

const router = useRouter(); // 初始化路由实例
const keyword = ref("");
const books = ref([]);
const defaultCover = "https://img.remit.ee/api/file/BQACAgUAAyEGAASHRsPbAAEDIOJo4O9zDdgnvgoGG1E_MkcsvoT-5gAC_BsAAmykCFeauimPTBUAARA2BA.png";
const fileInput = ref(null);

// 从本地存储获取用户信息
const getUserId = () => {
  const user = localStorage.getItem("userId");
  if (!user) {
    return null; // 或者直接 return {}
  }
  try {
    return JSON.parse(user);
  } catch (e) {
    console.error("JSON 解析失败:", e);
    return null;
  }
};


const userId = ref(getUserId());

// 获取书架书籍
const fetchBooks = async () => {
  // 检查登录状态
  if (!userId.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }

  try {
    const { data } = await axios.get("/api/bookshelf/list", {
      params: { 
        userId: userId.value, 
        keyword: keyword.value 
      },
    });
    books.value = data || [];
  } catch (e) {
    ElMessage.error("获取书籍失败：" + (e.response?.data || e.message));
  }
};




onMounted(fetchBooks);

// 打开书籍 - 修复问题1
const openBook = async (book) => {
  try {
    // 直接跳转到阅读器，不做存在性检查
    router.push({
      path: `/reader/${book.id}`,
      query: { title: book.title }
    });
  } catch (err) {
    const errorData = err.response?.data 
      ? JSON.stringify(err.response.data) 
      : err.message;
    ElMessage.error('打开书籍失败：' + errorData);
  }
};

// 触发文件上传
const triggerUpload = () => {
  if (!userId.value) {
    ElMessage.warning('请先登录');
    router.push('/login');
    return;
  }
  
  // 重置input值，解决连续上传同一文件不触发change事件的问题
  if (fileInput.value) {
    fileInput.value.value = '';
  }
  fileInput.value?.click();
};

// 处理文件上传
const handleFileUpload = async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  const ext = file.name.split('.').pop().toLowerCase();
  if (!['txt', 'pdf', 'epub'].includes(ext)) {
    ElMessage.error('仅支持 txt / epub / pdf 格式的文件');
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  formData.append("userId", userId.value);

  try {
    const { data } = await axios.post("/api/bookshelf/uploadAndParse", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        const percent = Math.round((progressEvent.loaded / progressEvent.total) * 100);
        console.log(`上传进度: ${percent}%`);
      }
    });
    ElMessage.success(data || "上传成功");
    fetchBooks(); // 重新获取书架列表
  } catch (err) {
    const errorMsg = err.response?.data || err.message;
    ElMessage.error("上传失败：" + errorMsg);
  }
};

// 从书架移除书籍
const removeFromShelf = async (bookId) => {
  try {
    const { data } = await axios.delete("/api/bookshelf/remove", {
      params: { 
        userId: userId.value, 
        bookId 
      }
    });
    ElMessage.success(data || "移除成功");
    fetchBooks(); // 重新获取书架列表
  } catch (e) {
    ElMessage.error("移除失败：" + (e.response?.data || e.message));
  }
};

// 跳转到首页 - 修复问题2
const goHome = () => {
  router.push('/'); // 使用路由跳转首页
};

// 退出登录 - 修复问题3
const logout = () => {
  // 1. 清除本地存储的用户信息
  localStorage.removeItem('userInfo');
  localStorage.removeItem('token');
  
  // 2. 重置用户ID
  userId.value = 0;
  
  // 3. 显示成功消息
  ElMessage.success("退出成功");
  
  // 4. 跳转到登录页
  router.push('/login');
};
</script>

<style scoped>
/* 样式保持不变 */
.bookshelf {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-box {
  width: 300px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
}

.shelf {
  margin-top: 30px;
  display: flex;
  flex-wrap: wrap;
  gap: 25px;
}

.book-card {
  width: 120px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
}

.book-card:hover {
  transform: translateY(-5px);
}

.book-cover-container {
  position: relative;
}

.book-cover {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #fff;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  display: none;
}

.book-cover-container:hover .remove-btn {
  display: block;
}

.book-title {
  font-size: 14px;
  margin: 6px 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-author {
  font-size: 12px;
  color: gray;
  margin: 0 0 4px;
}

.book-type {
  font-size: 11px;
  color: #666;
  margin: 0;
  background: #f5f5f5;
  border-radius: 3px;
  padding: 1px 0;
}

.add-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 2px dashed #ccc;
  border-radius: 4px;
  height: 160px;
  transition: all 0.2s;
}

.add-card:hover {
  border-color: #409eff;
  color: #409eff;
}

.plus {
  font-size: 32px;
  color: #666;
  margin-bottom: 8px;
}

.add-card:hover .plus {
  color: #409eff;
}
</style>