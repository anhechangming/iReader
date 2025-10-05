import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import axios from "axios";

// 后端地址
axios.defaults.baseURL = "http://localhost:8080";

// 请求拦截器：自动附带 token
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem("token") || sessionStorage.getItem("token");
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = token;
  }
  return config;
});

// 响应拦截器：处理未登录/过期
axios.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err?.response?.status === 401) {
      localStorage.removeItem("token");
      sessionStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

const app = createApp(App);
app.use(router);
app.use(ElementPlus);
app.mount("#app");
