import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '../stores/user'

NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/register/index.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'knowledge/list',
        name: 'KnowledgeList',
        component: () => import('../views/knowledge/list.vue'),
        meta: { title: '知识库列表' }
      },
      {
        path: 'knowledge/create',
        name: 'KnowledgeCreate',
        component: () => import('../views/knowledge/create.vue'),
        meta: { title: '新建知识库' }
      },
      {
        path: 'knowledge/detail/:id',
        name: 'KnowledgeDetail',
        component: () => import('../views/knowledge/detail.vue'),
        meta: { title: '知识库详情' }
      },
      {
        path: 'document/list',
        name: 'DocumentList',
        component: () => import('../views/document/list.vue'),
        meta: { title: '文档列表' }
      },
      {
        path: 'document/upload',
        name: 'DocumentUpload',
        component: () => import('../views/document/upload.vue'),
        meta: { title: '上传文档' }
      },
      {
        path: 'qa',
        name: 'QA',
        component: () => import('../views/qa/index.vue'),
        meta: { title: '智能问答' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('../views/history/index.vue'),
        meta: { title: '问答历史' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/index.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()

  const userStore = useUserStore()
  const token = userStore.token || localStorage.getItem('token')

  // 如果未登录且访问需要登录的页面，跳转到登录页
  if (!token && !['/login', '/register'].includes(to.path)) {
    next('/login?redirect=' + encodeURIComponent(to.fullPath))
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router