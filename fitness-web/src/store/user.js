import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { loginApi, registerApi, getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(getToken() || '')
  const isLoggedIn = computed(() => !!token.value)

  // 
  const role = computed(() => user.value?.role || '')

  // 
  const isAdmin = computed(() => role.value === 'ADMIN')

  // 
  async function login(loginForm) {
    const res = await loginApi(loginForm)
    if (res.code === 200) {
      token.value = res.data.token
      user.value = res.data.user
      setToken(res.data.token)
      localStorage.setItem('userRole', res.data.user.role)
      localStorage.setItem('userInfo', JSON.stringify(res.data.user))
      return true
    }
    return false
  }

  // 
  async function register(registerForm) {
    const res = await registerApi(registerForm)
    return res
  }

  // 
  async function fetchUserInfo() {
    try {
      const res = await getUserInfoApi()
      if (res.code === 200) {
        user.value = res.data
        localStorage.setItem('userRole', res.data.role)
        localStorage.setItem('userInfo', JSON.stringify(res.data))
      }
    } catch (e) {
      console.error('', e)
    }
  }

  // 
  function restoreLogin() {
    const savedToken = getToken()
    const savedInfo = localStorage.getItem('userInfo')
    if (savedToken && savedInfo) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedInfo)
      } catch (e) {
        // ignore
      }
    }
  }

  // 
  function logout() {
    token.value = ''
    user.value = null
    removeToken()
    localStorage.removeItem('userRole')
    localStorage.removeItem('userInfo')
  }

  // 
  function updateUser(newUser) {
    user.value = { ...user.value, ...newUser }
    localStorage.setItem('userInfo', JSON.stringify(user.value))
  }

  return {
    user,
    token,
    isLoggedIn,
    role,
    isAdmin,
    login,
    register,
    fetchUserInfo,
    restoreLogin,
    logout,
    updateUser
  }
})
