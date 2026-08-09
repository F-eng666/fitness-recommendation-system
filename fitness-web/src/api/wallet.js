import request from '@/utils/request'

export function getMyWallet() {
  return request({ url: '/wallet/my', method: 'get' })
}

export function getWalletTransactions(params) {
  return request({ url: '/wallet/transactions', method: 'get', params })
}

export function rechargeWallet(amount) {
  return request({ url: '/wallet/recharge', method: 'post', params: { amount } })
}
