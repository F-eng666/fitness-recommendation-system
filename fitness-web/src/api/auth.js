import request from '@/utils/request'

export function loginApi(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function registerApi(data) {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}

export function getUserInfoApi() {
  return request({
    url: '/auth/info',
    method: 'get'
  })
}

export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function sendResetCode(phone) {
  return request({
    url: '/auth/send-code',
    method: 'post',
    data: { phone }
  })
}

export function resetPassword(phone, code, newPassword) {
  return request({
    url: '/auth/reset-password',
    method: 'post',
    data: { phone, code, newPassword }
  })
}
