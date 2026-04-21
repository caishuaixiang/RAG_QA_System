import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
// 🔥 把 @ 改成相对路径！！！
import { userApi } from '../api/user'
import Cookies from 'js-cookie'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(Cookies.get('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  const role = computed(() => userInfo.value?.role || 0)
  const isAdmin = computed(() => role.value === 1)

  // 方法
  async function login(loginForm) {
    try {
      const res = await userApi.login(loginForm)
      if (res.code === 200) {
        token.value = res.data.token
        userInfo.value = res.data.user
        Cookies.set('token', res.data.token, { expires: 7 })
        localStorage.setItem('userInfo', JSON.stringify(res.data.user))
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message || '登录失败' }
    }
  }

  async function register(registerForm) {
    try {
      const res = await userApi.register(registerForm)
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message || '注册失败' }
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    Cookies.remove('token')
    localStorage.removeItem('userInfo')
  }

  async function getUserInfo() {
    try {
      const res = await userApi.getUserInfo(userInfo.value?.id)
      if (res.code === 200) {
        userInfo.value = res.data
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
      return res
    } catch (error) {
      return { code: 500, message: error.message }
    }
  }

  async function updateUserInfo(data) {
    try {
      const res = await userApi.updateUser(userInfo.value?.id, data)
      if (res.code === 200) {
        userInfo.value = { ...userInfo.value, ...data }
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  async function changePassword(data) {
    try {
      const res = await userApi.changePassword(userInfo.value?.id, data)
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  async function deleteAccount() {
    try {
      const res = await userApi.deleteUser(userInfo.value?.id)
      if (res.code === 200) {
        logout()
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }

  function checkAuth() {
    const savedToken = Cookies.get('token')
    const savedUserInfo = localStorage.getItem('userInfo')
    if (savedToken && savedUserInfo) {
      token.value = savedToken
      userInfo.value = JSON.parse(savedUserInfo)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    nickname,
    role,
    isAdmin,
    login,
    register,
    logout,
    getUserInfo,
    updateUserInfo,
    changePassword,
    deleteAccount,
    checkAuth
  }
})