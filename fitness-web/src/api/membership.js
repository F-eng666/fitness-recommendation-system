import request from '@/utils/request'

/**
 * 获取指定场馆的会员卡列表
 */
export function getVenueCards(venueId) {
  return request({ url: '/membership/cards', method: 'get', params: { venueId } })
}

/**
 * 获取首页热门会员卡（各类型推荐）
 */
export function getHotCards() {
  return request({ url: '/membership/cards/hot', method: 'get' })
}

/**
 * 购买会员卡
 * @param {Object} data - { cardId, venueId }
 */
export function purchaseCard(data) {
  return request({ url: '/membership/purchase', method: 'post', data })
}

/**
 * 获取我的会员卡
 */
export function getMyCards(params) {
  return request({ url: '/membership/my', method: 'get', params })
}

/**
 * 保存场馆的会员卡设置（管理员）
 */
export function saveVenueCards(data) {
  return request({ url: '/membership/admin/venue-cards', method: 'post', data })
}
