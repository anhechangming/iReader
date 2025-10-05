### nvm 

nvm 是管理node.js版本的工具，nvm list , nvm use 21.7.3, 

### 创建Vue项目

```bash
npm create vite@latest ireader-frontend
```

进入项目目录

```bash
cd ireader-frontend
npm install
npm run dev
```

打开浏览器访问提示的 `http://localhost:5173/` 就能看到 Vue 项目跑起来了。

安装常用依赖

```bash
npm install axios element-plus
```

> `axios`：用来请求后端接口

> `element-plus`：UI 组件库（表单、按钮、导航栏等）

### **Spring Security 依赖**  一般不需要

从截图中的日志 `Using generated security password: d5b8d5eb-9359-44fc-8d1d-ae0f346535b5` 以及你提到的出现登录界面，可以判断是因为项目中引入了 **Spring Security 依赖**，它会自动为应用添加安全认证功能，默认情况下会要求所有请求都经过登录验证。

### 解决方法

如果你只是想测试接口，暂时不需要安全认证，可以通过以下方式关闭 Spring Security 的默认认证：

#### 方式一：在主启动类上排除 Spring Security 自动配置

找到项目的主启动类（比如 `IreaderBackendApplication`），在 `@SpringBootApplication` 注解中添加 `exclude = SecurityAutoConfiguration.class`，示例代码如下：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class IreaderBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(IreaderBackendApplication.class, args);
    }
}
```

#### 方式二：配置 Spring Security 允许接口匿名访问

如果之后需要用到 Spring Security 的部分功能，只是想让 `api/test` 接口无需登录即可访问，可以创建一个 Spring Security 的配置类：

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/test").permitAll() // 允许/api/test接口匿名访问
                .anyRequest().authenticated() // 其他请求需要认证
        );
        return http.build();
    }
}
```

这样处理后，再访问 `api/test` 接口时，就不会再被重定向到登录界面了。