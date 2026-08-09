import request from '@/utils/request'

export function getRecommendedCourses(limit = 10) {
  return request({ url: '/recommend/courses', method: 'get', params: { limit } })
}

export function getRecommendedCoaches(limit = 5) {
  return request({ url: '/recommend/coaches', method: 'get', params: { limit } })
}

export function recordBehavior(data) {
  return request({ url: '/recommend/behavior', method: 'post', data })
}
