# RAG智能问答系统前端

基于 Vue 3 + Element Plus 构建的RAG智能问答系统前端项目。

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vue Router 4** - Vue.js官方路由
- **Pinia** - Vue.js状态管理库
- **Element Plus** - Vue 3组件库
- **Axios** - HTTP客户端
- **Vite** - 下一代前端构建工具
- **Sass** - CSS预处理器

## 功能特性

### 用户管理
- 用户登录/注册
- 个人信息管理
- 密码修改
- 账号注销

### 知识库管理
- 知识库列表展示
- 新建/编辑/删除知识库
- 知识库分类管理
- 知识库统计信息

### 文档管理
- 文档上传（支持PDF、Word、Excel、PPT、TXT等格式）
- 文档列表展示
- 文档内容预览
- 文档切片查看
- 文档删除

### 智能问答
- 基于知识库的智能问答
- 答案溯源信息展示
- 问答历史记录
- 多知识库切换

## 项目结构

```
RAGFrontend/
├── public/                 # 静态资源
├── src/
│   ├── api/               # API接口
│   │   ├── user.js        # 用户相关API
│   │   ├── knowledge.js   # 知识库API
│   │   ├── document.js    # 文档API
│   │   └── qa.js          # 问答API
│   ├── assets/            # 资源文件
│   ├── components/        # 公共组件
│   ├── layouts/           # 布局组件
│   ├── router/            # 路由配置
│   ├── stores/            # 状态管理
│   ├── styles/            # 样式文件
│   ├── utils/             # 工具函数
│   ├── views/             # 页面组件
│   │   ├── dashboard/     # 首页
│   │   ├── login/         # 登录页
│   │   ├── register/      # 注册页
│   │   ├── knowledge/     # 知识库管理
│   │   ├── document/      # 文档管理
│   │   ├── qa/            # 智能问答
│   │   ├── history/       # 问答历史
│   │   ├── profile/       # 个人中心
│   │   └── error/         # 错误页面
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── index.html             # HTML模板
├── package.json           # 项目配置
├── vite.config.js         # Vite配置
└── README.md              # 项目说明
```

## 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

### 安装依赖

```bash
cd RAGFrontend
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 生产构建

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 配置说明

### API代理配置

在 `vite.config.js` 中配置后端API代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 环境变量

创建 `.env.development` 和 `.env.production` 文件配置环境变量：

```
VITE_API_BASE_URL=/api
```

## 页面截图

### 登录页面
![登录页面](docs/login.png)

### 首页
![首页](docs/dashboard.png)

### 知识库管理
![知识库管理](docs/knowledge.png)

### 智能问答
![智能问答](docs/qa.png)

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 许可证

MIT License