import request from '@/utils/request'

export function getArticleList(params) {
  return request({ url: '/article/list', method: 'get', params })
}

export function getArticleDetail(id) {
  return request({ url: `/article/${id}`, method: 'get' })
}

export function createArticle(data) {
  return request({ url: '/article/create', method: 'post', data })
}

export function updateArticle(id, data) {
  return request({ url: `/article/${id}`, method: 'put', data })
}

export function deleteArticle(id) {
  return request({ url: `/article/${id}`, method: 'delete' })
}

export function toggleArticleLike(id) {
  return request({ url: `/article/${id}/like`, method: 'post' })
}

export function getArticleComments(id, params) {
  return request({ url: `/article/${id}/comments`, method: 'get', params })
}

export function createArticleComment(id, data) {
  return request({ url: `/article/${id}/comment`, method: 'post', data })
}

export function getMyArticles(params) {
  return request({ url: '/article/my', method: 'get', params })
}
