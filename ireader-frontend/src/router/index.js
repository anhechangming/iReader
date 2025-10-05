import { createRouter, createWebHistory } from "vue-router";
import LoginRegister from "../views/LoginRegister.vue";
import Home from "../views/Home.vue";
import Reader from '../views/Reader.vue';
import Bookshelf from '../views/Bookshelf.vue';

const routes = [
  { path: "/", redirect: "/login" },
  { path: "/login", component: LoginRegister },
  { path: "/home", component: Home, meta: { requiresAuth: true } },
  { path: '/reader/:id', name: 'Reader', component: Reader, props: true },
  { path: '/bookshelf', name: 'Bookshelf', component: Bookshelf, props: true },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});



// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token") || sessionStorage.getItem("token");
  if (!token && to.path !== "/login") {
    next({ path: "/login" });
  } else if (token && to.path === "/login") {
    next({ path: "/home" });
  } else {
    next();
  }
});

export default router;
