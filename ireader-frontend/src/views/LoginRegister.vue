<template>
  <div class="wrapper">
    <div :class="['container', { 'is-register': !isLogin }]">
      <!-- 登录表单 -->
      <div class="form a-form">
        <h2 class="title">登录</h2>
        <input v-model="form.phone" type="text" placeholder="请输入手机号" />
        <input v-model="form.password" type="password" placeholder="请输入密码" />
        <button @click="submitLogin">登录</button>
        <p class="switch-text">
          还没有账号？
          <a @click="toggleMode">立即注册</a>
        </p>
      </div>

      <!-- 注册表单 -->
      <div class="form b-form">
        <h2 class="title">注册</h2>
        <input v-model="form.phone" type="text" placeholder="请输入手机号" />
        <input v-model="form.password" type="password" placeholder="请输入密码" />
        <input v-model="form.confirmPassword" type="password" placeholder="请确认密码" />
        <button @click="submitRegister">注册</button>
        <p class="switch-text">
          已有账号？
          <a @click="toggleMode">立即登录</a>
        </p>
      </div>

      <!-- 切换背景 -->
      <div class="switch">
        <div class="switch__circle"></div>
        <div class="switch__container switch1" v-if="isLogin">
          <h2 class="switch__title">Hello Friend!</h2>
          <p class="switch__description">输入信息即可注册账户</p>
        </div>
        <div class="switch__container switch2" v-else>
          <h2 class="switch__title">Welcome Back!</h2>
          <p class="switch__description">请登录以继续使用系统</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";

const router = useRouter();
const isLogin = ref(true);
const form = reactive({
  phone: "",
  password: "",
  confirmPassword: "",
});

function toggleMode() {
  isLogin.value = !isLogin.value;
}

async function submitLogin() {
  if (!form.phone || !form.password) {
    alert("请输入手机号和密码");
    return;
  }
  try {
    const res = await axios.post("/api/user/login", {
      phone: form.phone,
      password: form.password,
    });
    if (res.data.code === 200) {
  const { token, user } = res.data.data;
    console.log("登录返回数据：", res.data.data);

  // 保存 token 和 user 信息
  localStorage.setItem("token", token);
  localStorage.setItem("user", JSON.stringify(user));
  localStorage.setItem("userId", user.id);

  alert("登录成功");
  router.push("/home");
}
else {
      alert(res.data.msg || "登录失败");
    }
  } catch (err) {
    alert("请求出错，请检查后端是否启动");
  }
}

async function submitRegister() {
  if (!form.phone || !form.password || !form.confirmPassword) {
    alert("请完整填写注册信息");
    return;
  }
  if (form.password !== form.confirmPassword) {
    alert("两次密码输入不一致");
    return;
  }
  try {
    const res = await axios.post("/api/user/register", {
      phone: form.phone,
      password: form.password,
    });
    if (res.data.code === 200) {
      alert("注册成功，请登录");
      isLogin.value = true;
    } else {
      alert(res.data.msg || "注册失败");
    }
  } catch (err) {
    alert("请求出错，请检查后端是否启动");
  }
}
</script>

<style scoped>
/* 页面容器 */
.wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #ecf0f3;
  font-family: "Montserrat", sans-serif;
}
.container {
  position: relative;
  width: 800px;
  height: 500px;
  border-radius: 12px;
  box-shadow: 10px 10px 20px #d1d9e6, -10px -10px 20px #ffffff;
  overflow: hidden;
  transition: all 0.6s ease-in-out;
  background: #ecf0f3;
}
.container::after {
  content: "";
  position: absolute;
  top: 0;
  left: 50%;
  width: 1px;
  height: 100%;
  background: rgba(0,0,0,0.06);
  box-shadow: 0 0 12px rgba(0,0,0,0.04);
}
.form {
  position: absolute;
  top: 50%;
  width: min(360px, 40%);
  left: 50%;
  transform: translate(-50%, -50%);
  transition: left 0.6s cubic-bezier(0.4,0,0.2,1), opacity 0.5s ease;
  box-sizing: border-box;
}
.a-form { left: 25%; opacity: 1; pointer-events: auto; z-index: 2; }
.b-form { left: 125%; opacity: 0; pointer-events: none; z-index: 1; }
.container.is-register .a-form { left: -25%; opacity: 0; pointer-events: none; }
.container.is-register .b-form { left: 75%; opacity: 1; pointer-events: auto; z-index: 2; }
.title { font-size: 28px; font-weight: bold; margin-bottom: 20px; text-align: center; color: #333; }
input { display: block; width: 100%; padding: 12px; margin: 12px 0; border: none; border-radius: 8px; outline: none; background: #ecf0f3; box-shadow: inset 2px 2px 5px #d1d9e6, inset -2px -2px 5px #fff; }
button { margin-top: 20px; padding: 12px; width: 100%; background: #4b70e2; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 16px; box-shadow: 5px 5px 10px #d1d9e6, -5px -5px 10px #fff; }
button:hover { background: #3a5ad9; }
.switch-text { text-align: center; margin-top: 10px; font-size: 14px; }
.switch-text a { color: #4b70e2; cursor: pointer; }
.switch { position: absolute; right: 0; top: 0; width: 50%; height: 100%; background: #ecf0f3; box-shadow: -5px 0 15px rgba(0,0,0,0.1); transition: all 0.6s ease-in-out; z-index: 1; }
.container.is-register .switch { left: 0; right: auto; box-shadow: 5px 0 15px rgba(0,0,0,0.08); }
.switch__circle { position: absolute; width: 400px; height: 400px; border-radius: 50%; background: #ecf0f3; box-shadow: inset 8px 8px 15px #d1d9e6, inset -8px -8px 15px #fff; top: -120px; right: -120px; transition: all 0.6s ease-in-out; }
.container.is-register .switch__circle { right: auto; left: -120px; }
.switch1,.switch2 { position: absolute; width: 80%; left: 10%; padding: 40px; top: 20%; transition: opacity 0.4s ease, transform 0.6s cubic-bezier(0.4,0,0.2,1); }
.switch2 { opacity: 0; transform: translateX(100%); }
.container.is-register .switch2 { opacity: 1; transform: translateX(0); }
.container.is-register .switch1 { opacity: 0; transform: translateX(-100%); }
.switch__title { font-size: 22px; font-weight: bold; color: #333; }
.switch__description { font-size: 14px; color: #666; margin: 20px 0; }
</style>
