# 🏋️ FitPro — 智能健身推荐系统

基于 Spring Boot + Vue 3 的全栈健身推荐管理平台，集成 DeepSeek AI 生成个性化健身与饮食计划，通过用户行为分析与标签画像实现智能推荐。

---

## ✨ 功能特性

### 👤 用户端
- **账号系统** — 注册 / 登录 / 找回密码 / 个人资料管理
- **场馆浏览** — 搜索、筛选、查看场馆详情与设施
- **课程预订** — 浏览课程、预约报名、查看订单
- **教练查阅** — 教练列表、详情、查看评价
- **🤖 AI 健身计划** — 接入 DeepSeek 大模型，根据身高/体重/目标/器材生成个性化训练计划与饮食建议
- **🎯 智能推荐** — 基于用户行为（浏览/收藏/报名/评价）和标签画像，推荐课程与教练
- **社区互动** — 文章发布、评论、点赞
- **饮食管理** — AI 饮食计划 + 饮食记录
- **打卡签到** — 课程/场馆打卡记录
- **收藏夹** — 场馆/课程/教练/文章收藏
- **虚拟钱包** — 余额管理、交易记录

### 🔧 场馆管理端
- 仪表盘数据概览
- 课程发布与管理
- 教练管理
- 订单管理
- 评价管理（含回复）

### 🛡️ 系统管理端
- 仪表盘与数据统计
- 用户管理
- 场馆审核（审核/通过/拒绝）
- 课程管理
- 教练管理
- 订单管理
- 评价管理
- 公告管理

---

## 🧱 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 2.7.18 |
| **ORM** | MyBatis-Plus | 3.5.5 |
| **安全认证** | Spring Security + JWT (jjwt) | 0.9.1 |
| **数据库** | MySQL | — |
| **AI 服务** | DeepSeek Chat API | deepseek-chat |
| **工具库** | Hutool 5.8, FastJSON 2, Lombok | — |
| **前端框架** | Vue 3 | 3.4 |
| **构建工具** | Vite | 5.1 |
| **路由** | Vue Router | 4.3 |
| **状态管理** | Pinia | 2.1 |
| **UI 组件库** | Element Plus | 2.5 |
| **HTTP 客户端** | Axios | 1.6 |
| **Markdown 渲染** | Marked | 11.0 |

---

## 📁 项目结构

```
健身推荐系统/
├── fitness-server/            # Spring Boot 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/fitness/
│       │   ├── common/        # 通用类（Result、异常处理、分页）
│       │   ├── config/        # 配置（CORS、安全、MyBatis、DeepSeek）
│       │   ├── controller/    # 控制器（15个模块）
│       │   ├── dto/           # 数据传输对象
│       │   ├── entity/        # 实体类
│       │   ├── enums/         # 枚举类
│       │   ├── mapper/        # MyBatis Mapper 接口
│       │   ├── security/      # JWT 过滤器与 Token 提供器
│       │   └── service/       # 服务层接口与实现
│       └── resources/
│           ├── application.yml  # 应用配置
│           ├── init.sql         # 数据库初始化脚本
│           └── mapper/          # MyBatis XML
├── fitness-web/               # Vue 3 前端
│   ├── src/
│   │   ├── api/               # API 接口封装（15个模块）
│   │   ├── router/            # 路由配置
│   │   ├── store/             # Pinia 状态管理
│   │   ├── utils/             # 工具函数（Auth、Axios）
│   │   └── views/             # 页面组件
│   │       ├── admin/         # 管理后台
│   │       ├── coach/         # 教练模块
│   │       ├── community/     # 社区模块
│   │       ├── course/        # 课程模块
│   │       ├── diet/          # 饮食模块
│   │       ├── fitness/       # 健身推荐模块
│   │       ├── system/        # 系统管理
│   │       ├── user/          # 用户中心
│   │       └── venue/         # 场馆模块
│   └── package.json
└── fitness.sql                # 数据库完整建表脚本
```

---

## 🚀 本地开发

### 环境要求

- **JDK** 11+
- **Maven** 3.6+
- **Node.js** 18+
- **MySQL** 8.0+

### 1. 数据库初始化

```bash
# 创建数据库并导入表结构
mysql -u root -p < fitness.sql
```

### 2. 后端启动

```bash
cd fitness-server

# 设置环境变量（替换为真实值）
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_jwt_secret
export DEEPSEEK_API_KEY=your_deepseek_api_key

# 编译并启动
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8081`

### 3. 前端启动

```bash
cd fitness-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端默认运行在 `http://localhost:3000`，API 请求自动代理到后端 `8081` 端口。

### 4. 访问系统

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |
| 普通用户 | 通过注册页面创建 | — |

---

## ⚙️ 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_PASSWORD` | MySQL 数据库密码 | `123456` |
| `JWT_SECRET` | JWT 签名密钥 | `changeme` |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | — |

> 🛡️ **生产环境务必设置强密码和密钥，切勿使用默认值！**

---

## 📡 API 概览

| 模块 | 端点前缀 | 说明 |
|------|---------|------|
| 认证 | `/api/auth/**` | 登录、注册、密码修改 |
| 用户 | `/api/user/**` | 用户信息、资料管理 |
| 场馆 | `/api/venue/**` | 场馆列表、详情、审核 |
| 课程 | `/api/course/**` | 课程浏览、预约 |
| 教练 | `/api/coach/**` | 教练信息 |
| 评价 | `/api/review/**` | 评价发表、回复 |
| AI 计划 | `/api/ai/**` | AI 生成健身/饮食计划 |
| 推荐 | `/api/recommend/**` | 智能推荐 |
| 社区 | `/api/article/**` | 文章 CRUD、评论、点赞 |
| 收藏 | `/api/favorite/**` | 收藏管理 |
| 打卡 | `/api/check-in/**` | 签到打卡 |
| 钱包 | `/api/wallet/**` | 余额、交易记录 |
| 订单 | `/api/order/**` | 课程订单 |
| 公告 | `/api/announcement/**` | 系统公告 |
| 管理 | `/api/admin/**` | 后台管理接口 |

---

## 🧠 推荐算法

系统采用**混合推荐策略**：

1. **基于内容的推荐** — 根据用户标签（偏好/目标/水平）匹配课程和教练
2. **协同过滤** — 基于用户行为（浏览、收藏、报名、评价）计算相似度
3. **行为权重** — 不同行为赋予不同权重（报名 > 收藏 > 浏览）
4. **标签画像** — 自动更新用户标签，持续优化推荐结果

---

## 📄 License

MIT License

---

> Made with ❤️ by F-eng666
